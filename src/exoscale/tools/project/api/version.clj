(ns exoscale.tools.project.api.version
  (:require [clojure.java.io :as io]
            [clojure.tools.deps.alpha.util.dir :as td]
            [exoscale.deps-version :as version]
            [exoscale.tools.project.api.git :as git]))

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
  (version/update-version {:file version-file
                           :key version-key
                           :suffix version-suffix}))

(defn git-count-revs
  "Updates VERSION file with clojure core libs like version scheme"
  [{:as opts :exoscale.project/keys [version-file]
    :or {version-file "VERSION"}}]
  (spit (td/canonicalize (io/file version-file))
        (git/git-count-revs opts)))
