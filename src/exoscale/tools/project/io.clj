(ns exoscale.tools.project.io
  (:require [clojure.tools.build.tasks.process :as p]))

(defn- check
  [{:keys [exit] :as res}]
  (when-not (zero? exit)
    (throw (ex-info "non zero exit" {})))
  res)

(defn shell
  "Run shell commands contained in `cmds`. The first unsuccesful exit triggers a system exit."
  [cmds {:keys [dir env out]}]
  (try
    (last
     (for [cmd cmds]
       (-> (p/process {:command-args (if (vector? cmd) cmd ["sh" "-c" cmd])
                       :dir dir
                       :env env
                       :out (or out :inherit)})
           (check))))
    (catch Exception _
      ;; At this stage we already printed the relevant error to stdout
      (System/exit 1))))
