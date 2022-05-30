(ns exoscale.tools.project.module
  "All functions in this modules are meant to be used with -T.

  They will trigger invocation of exoscale.tools.project functions with the same
  name or tasks with same id.

  If we can infer that we are in a multi-module setup at the root we will invoke
  the corresponding task, if not we will simply call into the the
  exoscale.tools.project function.

  That essentially means that all fns here as dual purpose depending on which
  directory they are called from"
  (:require [clojure.edn :as edn]
            [exoscale.tools.project :as p]))

;;; module aware commands

(def root-edn
  (delay (edn/read-string (slurp "deps.edn"))))

(defn task-or-tool [opts task-id project-fn]
  (let [redn @root-edn]
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

(defn uberjar
  [opts]
  (task-or-tool opts :uberjar p/uberjar))

(defn deploy
  [opts]
  (task-or-tool opts :deploy p/deploy))

;; for convenience
(def task p/task)
(def outdated p/outdated)
(def merge-deps p/merge-deps)
(def merge-aliases p/merge-aliases)

(defn format-check
  [opts]
  (p/format-check opts)
  (p/task (assoc opts :id :format-check)))

(defn format-fix
  [opts]
  (p/format-fix opts)
  (p/task (assoc opts :id :format-fix)))

(defn lint
  [opts]
  (p/lint opts)
  (p/task (assoc opts :id :lint)))
