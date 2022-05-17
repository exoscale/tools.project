(ns exoscale.tools.project.module
  (:refer-clojure :exclude [compile])
  (:require [clojure.edn :as edn]
            [exoscale.tools.project :as p]))

;;; module aware commands

(def root-edn
  (delay (edn/read-string (slurp "deps.edn"))))

(defn task-or-tool [opts task-id project-fn]
  (let [redn @root-edn]
    (prn task-id redn (get-in redn [:exoscale.project/tasks task-id]))

    ;; if it's a multi-module deps.edn with a corresponding task use that
    ;; otherwise fallback on regular project fns
    (if (or (contains? redn :exoscale.project/libs)
            (contains? redn :exoscale.project/deployables))
      (p/task (assoc opts :id task-id))
      (project-fn opts))))

(defn install
  [opts]
  (task-or-tool opts :install p/install))

(defn release
  [opts]
  (task-or-tool opts :release p/release))

(defn clean
  [opts]
  (task-or-tool opts :clean p/clean))

(defn compile
  [opts]
  (task-or-tool opts :compile p/compile))

(defn uberjar
  [opts]
  (task-or-tool opts :uberjar p/uberjar))

(defn deploy
  [opts]
  (task-or-tool opts :deploy p/deploy))
