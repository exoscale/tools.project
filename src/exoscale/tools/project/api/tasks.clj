(ns exoscale.tools.project.api.tasks
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.spec.alpha :as s]
            [clojure.tools.deps.alpha.util.dir :as td]
            [clojure.tools.build.api :as tb]
            [exoscale.lingo :as l]))

(def default-tasks
  {:install [#:exoscale.project.task{:run :exoscale.tools.project/install
                                     :for-all [:exoscale.project/libs]}]
   :deploy [#:exoscale.project.task{:run :exoscale.tools.project/deploy
                                    :for-all [:exoscale.project/deployables]}]
   :test []
   :release [#:exoscale.project.task{:shell ["clojure -X:deps-version update-version :file \"VERSION\" :suffix nil"
                                             "export VERSION=$(cat VERSION)"]}
             #:exoscale.project.task{:ref :deploy :for-all [:exoscale.project/libs]}
             #:exoscale.project.task{:ref :uberjar :for-all [:exoscale.project/deployables]}
             #:exoscale.project.task
              {:shell
               ["git config --global --add safe.directory $PWD"
                "git add VERSION && git commit -m \"Version $VERSION\" && git tag -a \"$VERSION\" --no-sign -m \"Release $VERSION\""
                "clojure -X:deps-version update-version :key :patch :file \"VERSION\" :suffix \"SNAPSHOT\""
                "export VERSION=$(cat VERSION)"
                "git add VERSION && git commit -m \"Version $VERSION\" && git pull && git push --follow-tags"]}]})

(defn shell*
  [cmds {:keys [dir env]}]
  (run! (fn [cmd]
          (let [res (apply shell/sh
                           (cond-> ["sh" "-c" cmd]
                             dir
                             (conj :dir dir)
                             env
                             (conj :env env)))]
            (when (pos? (:exit res))
              (throw (ex-info "Command failed to run" (assoc res :cmd cmd))))
            (println (:out res))
            res))
        cmds))

(defmacro with-dir [dir & body]
  `(let [dir# ~dir]
     (binding [tb/*project-root* dir#
               td/*the-dir* (td/as-canonical (io/file dir#))]
       ~@body)))

(defn run*
  [task {:keys [dir] :as opts}]
  (with-dir dir
    ((requiring-resolve (symbol task)) opts)))

(declare exoscale.tools.project.api.tasks/task)

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
      :shell (shell* shell opts)
      :run (run* run opts)
      :ref (exoscale.tools.project.api.tasks/task (assoc opts :id ref)))))

(defn task
  [opts]
   ;; let's assume we have the full env here
  (let [{:as root-deps-edn
         :exoscale.project/keys [tasks]
         :keys [id]} (edn/read-string (slurp (io/file td/*the-dir* "deps.edn")))
        tasks (merge default-tasks tasks)
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
        (run! (fn [dir] (run-task! task {:dir dir}))
              (or (get-in root-deps-edn for-all)
                  (throw (ex-info (format "Missing for-all key %s" for-all)
                                  task))))
        (run-task! task nil)))))

