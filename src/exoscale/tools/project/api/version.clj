(ns exoscale.tools.project.api.version
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [clojure.tools.deps.alpha.util.dir :as td]
            [exoscale.deps-version :as version]))

(defn run-version-fn [{:as opts :exoscale.project/keys [version-fn]}]
  (when-let [f (requiring-resolve (symbol version-fn))]
    (f opts)))

(defn get-version
  [{:as opts :exoscale.project/keys [version-file version-fn version]}]
  (let [version-from-file (some-> version-file version/read-version-file*)]
    (cond
      (string? version) version
      (string? version-from-file) version-from-file
      (qualified-ident? version-fn) (run-version-fn opts))))

(defn remove-snapshot
  [{:as _opts
    :exoscale.project/keys [version-file]
    :or {version-file "VERSION"}}]
  (version/update-version {:file version-file :suffix nil}))

(defn bump-and-snapshot
  [{:as _opts
    :exoscale.project/keys [version-file version-key version-suffix]
    :or {version-file "VERSION"
         version-key :patch
         version-suffix "SNAPSHOT"}}]
  (version/update-version {:file version-file
                           :key version-key
                           :suffix version-suffix}))

(defn git-count-revs
  "To be used via version-fn, returns version as VERSION_TEMPLATE with number of
  commits on current branch replacing the GENERATED_VERSION placeholder"
  [{:as _opts :exoscale.project/keys [version-template-file]
    :or {version-template-file "VERSION_TEMPLATE"}}]
  (str/replace (slurp (td/canonicalize (io/file version-template-file)))
               "GENERATED_VERSION"
               (b/git-count-revs nil)))

(defn epoch
  "To be used via version-fn, returns version as VERSION_TEMPLATE with number of
  seconds since epoch replacing the GENERATED_VERSION placeholder"
  [_opts]
  (.getEpochSecond (java.time.Instant/now)))
