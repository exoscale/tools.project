(ns exoscale.tools.project.tasks
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.spec.alpha :as s]
            [clojure.tools.deps.alpha.util.dir :as td]
            [exoscale.lingo :as l]
            [exoscale.tools.project.tasks :as tasks]))

(defn shell*
  [args {:keys [dir env]}]
  (let [res (apply shell/sh
                   (concat ["sh" "-c"]
                           args
                           (cond-> []
                             dir
                             (conj :dir dir)
                             env
                             (conj :env env))))]
    (when (pos? (:exit res))
      (throw (ex-info "Command failed to run" (assoc res :cmd args))))
    (println (:out res))
    res))

(defn run*
  [task opts]
  (td/with-dir (td/as-canonical (io/file (:dir opts)))
    ((requiring-resolve (symbol task)) opts)))

(declare exoscale.tools.project.tasks/task)

(s/def :exoscale.project/tasks
  (s/map-of keyword? (s/coll-of :exoscale.project/task)))

(s/def :exoscale.project/task
  (s/or :ref (s/keys :req [:exoscale.project.task/ref])
        :run (s/keys :req [:exoscale.project.task/run])
        :shell (s/keys :req [:exoscale.project.task/shell])))

(s/def :exoscale.project.task/ref keyword?)
(s/def :exoscale.project.task/run qualified-ident?)
(s/def :exoscale.project.task/shell (s/coll-of string? :min-count 1))

(defn- run-task!
  [{:as task :exoscale.project.task/keys [shell ref run]} opts]
  (let [ret (s/conform :exoscale.project/task task)]
    (case (first ret)
      :shell
      (shell* shell opts)

      :run
      (run* run opts)

      :ref
      (exoscale.tools.project.tasks/task (assoc opts :id ref)))))

(defn task
  [opts]
  ;; let's assume we have the full env here
  (let [{:as root-deps-edn
         :exoscale.project/keys [tasks]
         :keys [id]} (edn/read-string (slurp (io/file td/*the-dir* "deps.edn")))
        task-id (keyword (:id opts))
        task-def (get tasks task-id)]

    ;; validate early
    (when-not (s/valid? :exoscale.project/tasks tasks)
      (l/explain :exoscale.project/tasks tasks)
      (flush)
      (System/exit 1))

    (when-not task
      (println (format "Task '%s' not found" id))
      (System/exit 1))

    (println (format "Starting task %s" task-id))

    (doseq [{:as task :exoscale.project.task/keys [for-all]} task-def]
      (if (seq for-all)
        (run! #(run-task! task {:dir %})
              (or (get-in root-deps-edn for-all)
                  (throw (ex-info (format "Missing for-all key %s" for-all)
                                  task))))
        (run-task! task nil)))))
