(ns exoscale.tools.project.dir
  (:require [clojure.tools.deps.alpha.util.dir :as td]
            [clojure.java.io :as io]))

(defn canonicalize [f]
  (when f
    (str (td/canonicalize (io/file f)))))




