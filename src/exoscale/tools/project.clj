(ns exoscale.tools.project
  "All functions in this modules are meant to be used with -T.

  They will trigger invocation of exoscale.tools.project functions with the same
  name or tasks with same id.

  If we can infer that we are in a multi-module setup at the root we will invoke
  the corresponding task, if not we will simply call into the the
  exoscale.tools.project function.

  That essentially means that all fns here as dual purpose depending on which
  directory they are called from"
  (:refer-clojure :exclude [test])
  (:require [clojure.edn :as edn]
            [exoscale.tools.project.standalone :as ps]))

;;; module aware commands

(def ^:private root-edn
  (delay (edn/read-string (slurp "deps.edn"))))

(defn- task-or-tool [opts task-id project-fn]
  (let [redn @root-edn]
    ;; if it's a multi-module deps.edn with a corresponding task use that
    ;; otherwise fallback on regular project fns
    (if (contains? redn :exoscale.project/modules)
      (ps/task (assoc opts :id task-id))
      (project-fn opts))))

;; Tasks go below

(def add-module
  ps/add-module)

(defn check
  [opts]
  (task-or-tool opts :check ps/check))

(defn clean
  [opts]
  (task-or-tool opts :clean ps/clean))

(defn deploy
  [opts]
  (task-or-tool opts :deploy ps/deploy))

(defn format-check
  [opts]
  (task-or-tool opts :format/check ps/format-check))

(defn format-fix
  [opts]
  (task-or-tool opts :format/fix ps/format-fix))

(def init
  ps/init)

(def info
  ps/info)

(defn install
  [opts]
  (task-or-tool opts :install ps/install))

(defn jar
  [opts]
  (task-or-tool opts :jar ps/jar))

(defn lint
  [opts]
  (task-or-tool opts :lint ps/lint))

(def merge-deps
  ps/merge-deps)

(def merge-aliases
  ps/merge-aliases)

(def outdated
  ps/outdated)

(defn prep
  [opts]
  (task-or-tool opts :prep ps/prep))

(defn release
  [opts]
  (task-or-tool opts :release/modules ps/release))

(defn release-git-count-revs
  [opts]
  (task-or-tool opts :release-git-count-revs/modules ps/release-git-count-revs))

(defn revision-sha
  [opts]
  (task-or-tool opts :revision-sha ps/revision-sha))

(defn prep-self
  [opts]
  (task-or-tool opts :prep-self ps/prep-self))

(def task
  ps/task)

(defn test
  [opts]
  (task-or-tool opts :test ps/test))

(defn uberjar
  [opts]
  (task-or-tool opts :uberjar ps/uberjar))

(def version
  ps/version)
