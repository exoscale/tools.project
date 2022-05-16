(ns exoscale.tools.project.api.java
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]))

(defn compile [opts]
  (let [{:as opts
         :exoscale.project/keys [basis class-dir java-src-dirs]} opts]
    (b/javac {:basis basis
              :class-dir class-dir
              :src-dirs java-src-dirs})
    opts))

