(ns exoscale.tools.project
  (:refer-clojure :exclude [compile])
  (:require [exoscale.tools.project.api :as api]
            [clojure.spec.alpha :as s]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [exoscale.lingo :as l]))

(def default-opts
  #:exoscale.project{:file "deps.edn"
                     :keypath []
                     :exoscale.deps-version/key :patch
                     :slipset.deps-deploy/exec-args
                     {:repository {"releases" {:url "s3p://exo-artifacts/releases"}}
                      :installer :remote
                      :sign-releases? false}
                     :target-dir "target"
                     :class-dir "target/classes"
                     :javac-opts ["-source" "11" "-target" "11"]
                     :src-dirs ["src" "resources"]
                     :java-src-dirs ["java"]
                     :deps-file "deps.edn"})

(s/def :exoscale.project/lib qualified-ident?)
(s/def :exoscale.project/version string?)
(s/def :exoscale.project/target-dir string?)
(s/def :exoscale.project/class-dir string?)
(s/def :exoscale.project/javac-opts (s/coll-of string?))
(s/def :exoscale.project/src-dirs (s/coll-of string?))
(s/def :exoscale.project/java-src-dirs (s/coll-of string?))
(s/def :exoscale.project/deps-file string?)
(s/def :exoscale.project/subprojects (s/map-of keyword? :exoscale.project/opts))

(s/def :exoscale.project/opts
  (s/keys :req [:exoscale.project/lib]
          :opt [:exoscale.project/version
                :exoscale.project/version-file
                :exoscale.project/target-dir
                :exoscale.project/class-dir
                :exoscale.project/javac-opts
                :exoscale.project/src-dirs
                :exoscale.project/java-src-dirs
                :exoscale.project/deps-file
                :exoscale.project/subprojects]))

(defn read-project
  [{:as _opts :exoscale.project/keys [file keypath]}]
  (try (some-> file
               slurp
               edn/read-string
               (get-in keypath))
       (catch java.io.FileNotFoundException _fnf)))

(defn assoc-version
  [{:as opts :exoscale.project/keys [version-file]}]
  (cond-> opts
    (some? version-file)
    (assoc :exoscale.project/version (slurp version-file))))

(defn read-subproject
  [project-def]
  (if (string? project-def)
    ;; XXX: should keypath be supported here too?
    (read-project #:exoscale.project{:file project-def :keypath []}) 
    project-def))

(defn add-subprojects
  [{:exoscale.project/keys [subprojects] :as opts}]
  (assoc opts :exoscale.project/subprojects
         (reduce-kv #(assoc %1 %2 (read-subproject %3)) {} subprojects)))

(defn into-opts [opts]
  (let [opts (-> default-opts
                 (merge (read-project (into default-opts opts)) opts)
                 assoc-version
                 add-subprojects)]
    (when-not (s/valid? :exoscale.project/opts opts)
      (let [msg (format "Invalid exoscale.project configuration in %s"
                        (some-> (-> opts
                                    :exoscale.project/file
                                    io/file
                                    .getCanonicalPath)))]
        (binding [*out* *err*]
          (println msg)
          (l/explain :exoscale.project/opts opts {:colors? true})
          (flush)
          (System/exit 1))))
    opts))

(defn describe
  [opts]
  (-> opts
      into-opts
      api/describe))

(defn clean [opts]
  (-> opts into-opts api/clean))

(defn compile [opts]
  (-> opts
      into-opts
      api/compile))

(defn jar [opts]
  (-> opts
      into-opts
      api/clean
      api/jar))

(defn uberjar
  [opts]
  (-> opts
      into-opts
      api/uberjar))

(defn install
  [opts]
  (-> opts
      into-opts
      api/install))

(defn deploy
  [opts]
  (-> opts
      into-opts
      api/deploy))
