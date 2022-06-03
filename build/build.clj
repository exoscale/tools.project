(ns build
  (:require [clojure.tools.build.api :as b]))


(def basis (b/create-basis {:project "deps.edn"}))

(defn aot-compile
  [_]
  (b/compile-clj {:basis basis
                  :src-dirs ["src"]
                  :class-dir "target/classes"
                  :compile-opts {:direct-linking true}}))
