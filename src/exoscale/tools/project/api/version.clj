(ns exoscale.tools.project.api.version
  (:require [exoscale.deps-version :as version]))

(defn get-version
  [{:as _opts :exoscale.project/keys [version-file version]}]
  (or version
      (some-> version-file version/read-version-file*)))

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
  {:file version-file
   :key version-key
   :suffix version-suffix})
