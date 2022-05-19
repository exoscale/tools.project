(ns exoscale.tools.project.api.tasks
  "Tasks are meant to be used to compose exoscale.tools.project functions and
  potentially run them for a set of things.

  A Task is just a map.
  #:exoscale.project.task{...}

  We have 3 type of tasks for now:

  * :run tasks `#:exoscale.project.task{:run some.ns/fn}` that will trigger an invocation of that task
  * :shell tasks `#:exoscale.project.task{:shell [\"ping foo.com\" \"ping bar.com\"]}` that will trigger an invocation of the shell commands listed in order
  * :ref tasks `#:exoscale.project.task{:ref :something}` that will invoke another task

  Great, but why are we doing this?

  Tasks can be repeated for \"sub modules\" if you specify a :for-all key
  `#:exoscale.project.task{:run some.ns/fn :for-all [:exoscale.project/libs]}`
  the task will then be run for all modules listed under that key in the
  deps.edn file, in (execution) context, in a single process
  potentially. Essentially a declarative version of lein sub that supports
  composability and is not spawning as many processes as we have tasks."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.spec.alpha :as s]
            [clojure.tools.deps.alpha.util.dir :as td]
            [exoscale.tools.project.dir :as dir]
            [clojure.tools.build.api :as tb]
            [exoscale.lingo :as l]))

(def default-tasks
  "Sets some desirable default tasks"
  {:install [#:exoscale.project.task{:run :exoscale.tools.project/install
                                     :for-all [:exoscale.project/libs]}]

   :deploy [#:exoscale.project.task{:run :exoscale.tools.project/deploy
                                    :for-all [:exoscale.project/deployables]}]

   :uberjar [#:exoscale.project.task{:shell
                                     ["mkdir -p resources && echo -n \"$(git rev-parse HEAD)\" > resources/git-version"]
                                     :for-all [:exoscale.project/deployables]}
             #:exoscale.project.task{:run :exoscale.tools.project/uberjar
                                     :for-all [:exoscale.project/deployables]}]

   :compile [#:exoscale.project.task{:run :exoscale.tools.project/compile
                                     :for-all [:exoscale.project/deployables]}
             #:exoscale.project.task{:run :exoscale.tools.project/compile
                                     :for-all [:exoscale.project/libs]}]

   :clean [#:exoscale.project.task{:run :exoscale.tools.project/clean
                                   :for-all [:exoscale.project/deployables]}
           #:exoscale.project.task{:run :exoscale.tools.project/clean
                                   :for-all [:exoscale.project/libs]}]

   :release [#:exoscale.project.task{:run :exoscale.tools.project/version-remove-snapshot}
             #:exoscale.project.task{:ref :deploy :for-all [:exoscale.project/libs]}
             #:exoscale.project.task{:ref :uberjar :for-all [:exoscale.project/deployables]}
             #:exoscale.project.task{:shell
                                     ["git config --global --add safe.directory $PWD"
                                      "git add VERSION"
                                      (str "export VERSION=$(cat VERSION) && "
                                           "git commit -m \"Version $VERSION\" && "
                                           "git tag -a \"$VERSION\" --no-sign -m \"Release $VERSION\"")]}
             #:exoscale.project.task{:run :exoscale.tools.project/version-bump-and-snapshot}
             #:exoscale.project.task
              {:shell
               ["git config --global --add safe.directory $PWD"
                (str "export VERSION=$(cat VERSION) && "
                     "git add VERSION && "
                     "git commit -m \"Version $VERSION\"")
                "git pull && git push --follow-tags"]}]})

(defn shell*
  [cmds {::keys [dir env]}]
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

(defn run*
  [task {::keys [dir] :as opts}]
  (binding [tb/*project-root* dir
            td/*the-dir* (td/as-canonical (io/file dir))]
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
  [{:as task :exoscale.project.task/keys [shell ref run args]}
   opts]
  (let [ret (s/conform :exoscale.project/task task)]
    (case (first ret)
      :shell (shell* shell opts)
      :run (run* run (merge {} args opts))
      :ref (exoscale.tools.project.api.tasks/task (assoc opts :id ref)))))

(defn task
  [opts]
   ;; let's assume we have the full env here
  (let [{:as root-deps-edn
         :exoscale.project/keys [tasks]
         :keys [id]} (edn/read-string (slurp (dir/canonicalize "deps.edn")))
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
        (run! (fn [dir] (run-task! task {::dir (dir/canonicalize dir)}))
              (or (get-in root-deps-edn for-all)
                  (throw (ex-info (format "Missing for-all key %s" for-all)
                                  task))))
        (run-task! task {::dir td/*the-dir*})))))

