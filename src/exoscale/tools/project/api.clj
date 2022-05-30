(ns exoscale.tools.project.api
  (:require [clojure.tools.build.api :as b]))

(defn create-basis
  ([deps-file]
   (b/create-basis {:project deps-file}))
  ([] (create-basis "deps.edn")))

(defn clean [opts]
  (let [{:as opts :exoscale.project/keys [target-dir]} opts]
    (b/delete {:path target-dir})
    opts))
