(ns exoscale.tools.project.io
  (:require [babashka.process :as p]))

(defn shell
  "Run shell commands contained in `cmds`. The first unsuccesful exit triggers a system exit."
  [cmds {:keys [dir env]}]
  (try
    (doseq [cmd cmds]
      (-> (p/process ["sh" "-c" cmd] {:out :inherit :err :inherit :dir dir :env env})
          (p/check)))
    (catch Exception _
      ;; At this stage we already printed the relevant error to stdout
      (System/exit 1))))
