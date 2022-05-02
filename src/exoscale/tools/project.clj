(ns exoscale.tools.project
  (:refer-clojure :exclude [compile])
  (:require [exoscale.tools.project.api :as api]
            [exoscale.tools.project.path :as path]
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
(s/def :exoscale.project/prevent (s/coll-of #{:uberjar :jar :deploy :install :compile}))

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

(defn assoc-relative-version
  [file-location {:as opts :exoscale.project/keys [version-file]}]
  (cond-> opts
    (some? version-file)
    (assoc :exoscale.project/version
           (slurp (path/sibling file-location version-file)))))

(defn read-subproject
  [project-def]
  (if (string? project-def)
    ;; XXX: should keypath be supported here too?
    (assoc-relative-version
     project-def
     (read-project #:exoscale.project{:file project-def :keypath []}))
    (assoc-version project-def)))

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


(defn subproject-run
  [{:exoscale.project/keys [subprojects] :as opts} f]
  (update opts :subprojects
          (fn [sps]
            (reduce-kv #(assoc %1 %2 (f %3)) {} sps))))

(defn pprint
  [opts]
  (-> opts
      into-opts
      api/pprint))

(defn clean [opts]
  (-> opts
      into-opts
      (subproject-run api/clean)
      api/clean))

(defn compile [opts]
  (-> opts
      into-opts
      (subproject-run api/compile)
      api/compile))

(defn jar [opts]
  (-> (clean opts)
      (subproject-run api/jar)
      api/jar))

(defn uberjar
  [opts]
  (-> (clean opts)
      (subproject-run api/uberjar)
      api/uberjar))

(defn install
  [opts]
  (-> opts
      into-opts
      (subproject-run api/install)
      api/install))

(defn deploy
  [opts]
  (-> opts
      into-opts
      (subproject-run api/deploy)
      api/deploy))

(defn release
  [opts]
  (-> opts
      into-opts
      (subproject-run api/release)
      api/release))

(defn shell
  [opts]
  (-> opts
      into-opts
      api/shell))
