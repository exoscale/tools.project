(ns exoscale.tools.project.api.jar
  (:require [clojure.tools.build.api :as b]
            [exoscale.tools.project.api :as api]
            [exoscale.tools.project.dir :as dir]
            [clojure.tools.deps.alpha.util.dir :as td]
            [clojure.tools.build.api :as tb]))

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

(defn jar [opts]
  (let [{:exoscale.project/keys [_env lib _version _version-file class-dir src-dirs basis jar-file deps-file]
         :as opts} opts]
    (api/clean opts)
    (let [version (api/get-version opts)
          deps-file (dir/canonicalize deps-file)
          basis (or basis (api/create-basis deps-file))
          jar-file (dir/canonicalize (or jar-file (jar-file* (name lib) version)))
          class-dir (dir/canonicalize class-dir)
          src-dirs (map dir/canonicalize src-dirs)]

      (println "Writing pom.xml")
      (b/write-pom {:basis basis
                    :class-dir class-dir
                    :lib lib
                    :src-dirs src-dirs
                    :version version})
      (println "Copying src-dirs: " src-dirs)
      (b/copy-dir {:src-dirs src-dirs
                   :target-dir class-dir})
      (println "Creating jar:" jar-file)
      (b/jar {:class-dir class-dir
              :jar-file jar-file})
      (into opts
            #:exoscale.project{:basis basis
                               :version version
                               :jar-file jar-file}))))

(defn uberjar
  [opts]
  (let [{:exoscale.project/keys [_env lib _version _version-file main
                                 src-dirs class-dir basis uberjar-file deps-file]
         :as opts} opts
        _ (api/clean opts)
        version (api/get-version opts)
        deps-file (dir/canonicalize deps-file)
        basis (or basis (api/create-basis deps-file))
        uber-file (dir/canonicalize (or uberjar-file (uberjar-file* (name lib) version)))
        src-dirs (map dir/canonicalize src-dirs)
        class-dir (dir/canonicalize class-dir)]

    (println "Copying src-dirs")
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir class-dir})
    (println "Compiling" src-dirs)
    (b/compile-clj {:basis basis
                    :class-dir class-dir
                    :src-dirs src-dirs})
    (println "Creating uberjar: " uber-file)
    (b/uber {:basis basis
             :class-dir class-dir
             :main main
             :uber-file uber-file})
    (into opts
          #:exoscale.project{:basis basis
                             :version version
                             :uberjar-file uber-file})))
