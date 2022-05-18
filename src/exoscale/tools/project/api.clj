(ns exoscale.tools.project.api
  (:refer-clojure :exclude [compile])
  (:require [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [exoscale.deps-version :as version]))

(defn create-basis
  ([deps-file]
   (b/create-basis {:project deps-file}))
  ([] (create-basis "deps.edn")))

(defn get-version
  [{:as _opts :exoscale.project/keys [version-file version]}]
  (or version
      (some-> version-file version/read-version-file* str/trim)))

(defn clean [opts]
  (let [{:as opts :exoscale.project/keys [target-dir]} opts]
    (b/delete {:path target-dir})
    opts))


