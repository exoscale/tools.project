(ns exoscale.tools.project.api.java
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]
            [exoscale.tools.project.dir :as dir]))

(defn compile [opts]
  (let [{:as opts
         :exoscale.project/keys [basis class-dir java-src-dirs]} opts
        class-dir (dir/canonicalize class-dir)
        java-src-dirs (map dir/canonicalize java-src-dirs)]
    (b/javac {:basis basis
              :class-dir class-dir
              :src-dirs java-src-dirs})
    (assoc opts
           :exoscale.project/class-dir class-dir
           :exoscale.project/java-src-dirs java-src-dirs)))
