(ns exoscale.tools.project.io
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn cmd!
  [cmd {:keys [dir env]}]
  (let [pb (ProcessBuilder. ^java.util.List cmd)]
    (println :cmd (str/join " " cmd))
    (when dir
      (.directory pb (io/file dir)))
    (when env
      (let [pb-env (.environment pb)]
        (run! (fn [[k v]] (.put pb-env k v)) env)))
    (let [proc (.start pb)
          exit (.waitFor proc)
          out (slurp (.getInputStream proc))
          err (slurp (.getErrorStream proc))]
      (cond-> {:exit exit}
        out (assoc :out out)
        err (assoc :err err)))))

(defn shell
  [cmds {:keys [_dir _env] :as opts}]
  (run! (fn [cmd]
          (let [res (cmd! ["sh" "-c" cmd] opts)]
            (when (pos? (:exit res))
              (throw (ex-info "Command failed to run" (assoc res :cmd cmd))))
            (println (:out res))
            res))
        cmds))
