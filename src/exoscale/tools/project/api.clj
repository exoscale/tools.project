(ns exoscale.tools.project.api
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]
            [clojure.java.shell :as sh]
            [puget.printer :as pp]
            [clojure.tools.cli.api :as td]

            [deps-deploy.deps-deploy :as dd]))

(defn create-basis
  ([deps-file]
   (b/create-basis {:project deps-file}))
  ([] (create-basis "deps.edn")))

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

(defn get-version
  [{:as _opts :exoscale.project/keys [version-file version]}]
  (or version
      (some-> version-file slurp)))

(defn pprint
  [opts]
  (doto opts pp/cprint))

(defn clean [opts]
  (let [{:as opts :exoscale.project/keys [target-dir]} opts]
    (b/delete {:path target-dir})
    opts))

(defn compile [opts]
  (let [{:as opts
         :exoscale.project/keys [basis class-dir java-src-dirs]} opts]
    (b/javac {:basis basis
              :class-dir class-dir
              :src-dirs java-src-dirs})
    opts))

(defn jar [opts]
  (let [{:exoscale.project/keys [_env lib _version _version-file class-dir src-dirs basis jar-file deps-file]
         :as opts} opts]
    (let [version (get-version opts)
          basis (or basis (create-basis deps-file))
          jar-file (or jar-file (jar-file* (name lib) version))]
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
  [{:exoscale.project/keys [_env lib _version _version-file main src-dirs class-dir
                            basis uberjar-file deps-file prevent]
    :as opts}]
  (if-not (contains? (set prevent) :uberjar)
    (let [_ (println "building uberjar for:" (name lib))
          version (get-version opts)
          basis (or basis (create-basis deps-file))
          uber-file (or uberjar-file (uberjar-file* (name lib) version))]
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
                               :uberjar-file uber-file}))
    opts))

(defn install
  [opts]
  (let [opts (jar opts)]
    (td/mvn-install {:jar (:exoscale.project/jar-file opts)})
    opts))

;; temporary until we have something more official, hence why its keys are ns'ed
(defn deploy
  [opts]
  (let [{:as opts
         :exoscale.project/keys [lib target-dir jar-file prevent]
         :slipset.deps-deploy/keys [exec-args]} (jar opts)]
    (if-not (contains? (set prevent) :deploy)
      (do
        (dd/deploy (into {:artifact jar-file
                          :pom-file (format "%s/classes/META-INF/maven/%s/pom.xml"
                                            target-dir
                                            lib)}
                         exec-args))
        opts)
      opts)))

(defn- clean-task-opts
  [opts]
  (-> opts
      (dissoc :exoscale.project/version-file)
      (update :exoscale.project/subprojects
              (fn [sp]
                (reduce-kv #(assoc %1 %2 (dissoc %3 :exoscale.project/version-file))
                           {}
                           sp)))))

(defn- perform-release-task
  [opts task]
  (let [[sym arg] task
        f         (requiring-resolve sym)]
    (f (merge (clean-task-opts opts) arg))))

(defn shell
  [{:as                    opts
    :exoscale.project/keys [command]}]
  (prn command)
  (when (some? shell)
    (let [res (apply sh/sh (concat command [:in "" :out-enc "UTF-8"]))]
      (when-not (zero? (:exit res))
        (binding [*out* *err*]
          (println "could not run shell command:" shell)
          (println (:out res)))
        (System/exit 1))
      (println (:out res))))
  opts)

(defn release
  [{:as                    opts
    :exoscale.project/keys [release-tasks prevent]}]
  (cond
    (contains? (set prevent) :release)
    opts

    (some? release-tasks)
    (reduce perform-release-task opts release-tasks)

    :else
    opts))
