(ns exoscale.tools.project.io
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]))

(defn shell
  [cmds {:keys [dir env]}]
  (run! (fn [cmd]
          (let [res (apply shell/sh
                           (cond-> (str/split cmd #"\s+")
                             dir
                             (conj :dir dir)
                             env
                             (conj :env env)))]
            (when (pos? (:exit res))
              (throw (ex-info "Command failed to run" (assoc res :cmd cmd))))
            (println (:out res))
            res))
        cmds))

