(ns exoscale.tools.project.api.jar
  (:require [clojure.tools.build.api :as b]
            [exoscale.tools.project.api.version :as v]
            [exoscale.tools.project.dir :as dir]))

(defn- create-basis
  ([deps-file]
   (b/create-basis {:project deps-file}))
  ([]
   (create-basis "deps.edn")))

(defn jar-file*
  ([lib version]
   (jar-file* "target" lib version))
  ([path lib version]
   (format "%s/%s-%s.jar" path lib version)))

(defn uberjar-file*
  ([lib version]
   (uberjar-file* "target" lib version))
  ([path lib version]
   (format "%s/%s-%s-standalone.jar" path lib version)))

(defn- lifted-basis
  "This creates a basis where source deps have their primary
  external dependencies lifted to the top-level, such as is
  needed by Polylith and possibly other monorepo setups."
  [deps-file]
  (let [default-libs (:libs (create-basis deps-file))
        source-dep? #(not (:mvn/version (get default-libs %)))
        lifted-deps
        (reduce-kv (fn [deps lib {:keys [dependents] :as coords}]
                     (cond-> deps
                       (and (contains? coords :mvn/version)
                            (some source-dep? dependents))
                       (assoc lib (select-keys coords [:mvn/version :exclusions]))))
                   {}
                   default-libs)]
    (-> (b/create-basis {:extra {:deps lifted-deps}})
        (update :libs #(into {} (filter (comp :mvn/version val)) %)))))

(defn- jar-opts
  "Provide sane defaults for jar/uber tasks.
  :lib is required, :version is optional for uber, everything
  else is optional."
  [opts]
  (let [version (v/get-version opts)
        {:exoscale.project/keys [jar-transitive? basis resource-dirs src-dirs deps-file]
         :as opts}
        (into opts
              #:exoscale.project{:version version
                                 :jar-file (dir/canonicalize
                                            (or (:exoscale.project/jar-file opts)
                                                (jar-file* (name (:exoscale.project/lib opts))
                                                           version)))
                                 :class-dir (dir/canonicalize (:exoscale.project/class-dir opts))
                                 :deps-file (:exoscale.project/deps-file opts)
                                 :src-dirs (map dir/canonicalize (:exoscale.project/src-dirs opts))})
        _ (when jar-transitive?
            (assert (nil? basis) ":transitive cannot be true when :basis is provided"))
        basis (if jar-transitive?
                (lifted-basis deps-file)
                (or basis (create-basis deps-file)))
        directory? #(let [f (java.io.File. %)]
                      (and (.exists f) (.isDirectory f)))]
    (assoc opts
           :exoscale.project/basis basis
           :exoscale.project/src+dirs (if jar-transitive?
                                        (filter directory? (:classpath-roots basis))
                                        (into src-dirs
                                              (or resource-dirs ["resources"]))))))

(defn jar [opts]
  (let [{:exoscale.project/keys [_env lib version _version-file class-dir src-dirs src+dirs
                                 basis jar-file]
         :as opts} (jar-opts opts)]
    (println "Writing pom.xml")
    (b/write-pom {:basis basis
                  :class-dir class-dir
                  :lib lib
                  :src-dirs src-dirs
                  :version version})
    (println "Copying src-dirs: " src-dirs)
    (b/copy-dir {:src-dirs src+dirs
                 :target-dir class-dir})
    (println "Creating jar:" jar-file)
    (b/jar {:class-dir class-dir
            :jar-file jar-file})
    opts))

(defn uberjar
  [opts]
  (let [{:exoscale.project/keys [_env lib _version _version-file main compile-opts
                                 src-dirs class-dir basis uberjar-file uber-opts deps-file]
         :as opts} opts
        version (v/get-version opts)
        deps-file (dir/canonicalize deps-file)
        basis (or basis (create-basis deps-file))
        uber-file (dir/canonicalize (or uberjar-file (uberjar-file* (name lib) version)))
        src-dirs (map dir/canonicalize src-dirs)
        class-dir (dir/canonicalize class-dir)]

    (println "Copying src-dirs")
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir class-dir})
    (println "Writing pom.xml")
    (b/write-pom {:basis basis
                  :class-dir class-dir
                  :lib lib
                  :src-dirs src-dirs
                  :version version})
    (println "Compiling" src-dirs)
    (b/compile-clj (cond-> {:basis basis
                            :class-dir class-dir
                            :src-dirs src-dirs}
                     (some? compile-opts)
                     (assoc :compile-opts compile-opts)))
    (println "Creating uberjar: " uber-file)
    (b/uber (merge uber-opts {:basis basis
                              :class-dir class-dir
                              :main main
                              :uber-file uber-file}))
    (into opts
          #:exoscale.project{:basis basis
                             :version version
                             :uberjar-file uber-file
                             :uber-opts uber-opts})))
