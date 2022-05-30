(ns exoscale.tools.project.api.tasks
  "Tasks are meant to be used to compose exoscale.tools.project functions and
  potentially run them for a set of things.

  A Task is just a map.
  {...}

  We have 3 type of tasks for now:

  * :run tasks `{:run some.ns/fn}` that will trigger an invocation of that task
  * :shell tasks `{:shell [\"ping foo.com\" \"ping bar.com\"]}` that will trigger an invocation of the shell commands listed in order
  * :ref tasks `{:ref :something}` that will invoke another task

  Great, but why are we doing this?

  Tasks can be repeated for several \"sub projects\" if you specify a :for-all key
  `#:exoscale.project.task{:run some.ns/fn :for-all [:exoscale.project/modules]}`
  the task will then be run for all modules listed under that key in the
  deps.edn file, in (execution) context, in a single process
  potentially. Essentially a declarative version of lein sub that supports
  composability and is not spawning as many processes as we have tasks.

  Filtering on the list of target modules can be performed:

      {:run some.ns/fn :for-all [:exoscale.project/modules]
       :when :exoscale.project/should-run?}

  Inverted predicate filter is also supported:

      {:run some.ns/fn :for-all [:exoscale.project/modules]
       :unless :exoscale.project/bypass?}
  "

  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.tools.build.api :as tb]
            [clojure.tools.deps.alpha.util.dir :as td]
            [exoscale.lingo :as l]
            [exoscale.tools.project.dir :as dir]
            [exoscale.tools.project.io :as pio]))

(def default-tasks
  "Sets some desirable default tasks"
  {:install [{:run :exoscale.tools.project.standalone/install
              :for-all [:exoscale.project/modules]}]

   :deploy [{:run :exoscale.tools.project.standalone/deploy
             :unless :exoscale.project/prevent-deploy?
             :for-all [:exoscale.project/modules]}]

   :uberjar [{:run :exoscale.tools.project.standalone/uberjar
              :when :exoscale.project/uberjar?
              :for-all [:exoscale.project/modules]}]

   :clean [{:run :exoscale.tools.project.standalone/clean
            :for-all [:exoscale.project/modules]}]

   :check [{:shell ["clojure -X:project:test exoscale.tools.project.standalone/standalone-check"]}]

   :check/all [{:ref :check :for-all [:exoscale.project/modules]}]

   :test [{:run :exoscale.tools.project.standalone/test
           :for-all [:exoscale.project/modules]
           :unless :exoscale.project/bypass-test?}]

   :format/check [{:run :exoscale.tools.project.standalone/format-check
                   :for-all [:exoscale.project/modules]}]

   :format/fix [{:run :exoscale.tools.project.standalone/format-fix
                 :for-all [:exoscale.project/modules]}]

   :lint [{:run :exoscale.tools.project.standalone/lint
           :for-all [:exoscale.project/modules]}]

   :revision-sha [{:run :exoscale.tools.project.standalone/revision-sha
                   :for-all [:exoscale.project/modules]}]

   :release [{:run :exoscale.tools.project.standalone/version-remove-snapshot}
             {:ref :deploy}
             {:ref :uberjar}
             {:run :exoscale.tools.project.standalone/git-commit-version}
             {:run :exoscale.tools.project.standalone/git-tag-version}
             {:run :exoscale.tools.project.standalone/version-bump-and-snapshot}
             {:run :exoscale.tools.project.standalone/git-commit-version}
             {:run :exoscale.tools.project.standalone/git-push}]})

(defn shell*
  [cmds {::keys [dir env]}]
  (pio/shell cmds {:dir dir :env env}))

(defn run*
  [task {::keys [dir] :as opts}]
  (binding [tb/*project-root* dir
            td/*the-dir* (td/as-canonical (io/file dir))]
    ((requiring-resolve (symbol task)) opts)))

(declare exoscale.tools.project.api.tasks/task)

(s/def :exoscale.project/tasks
  (s/map-of keyword? (s/coll-of :exoscale.project/task)))

(s/def :exoscale.project/task
  (s/or :ref (s/keys :req-un [:exoscale.project.task/ref])
        :run (s/keys :req-un [:exoscale.project.task/run])
        :shell (s/keys :req-un [:exoscale.project.task/shell])))

(s/def :exoscale.project.task/ref keyword?)
(s/def :exoscale.project.task/run qualified-ident?)
(s/def :exoscale.project.task/shell (s/coll-of string? :min-count 1))

(defn- run-task!
  [{:as task :keys [shell ref run args]}
   {::keys [dir] :as opts}]
  (let [ret (s/conform :exoscale.project/task task)]
    (case (first ret)
      :shell (shell* shell opts)
      :run (run* run (merge {} args opts))
      :ref (binding [td/*the-dir* dir]
             (exoscale.tools.project.api.tasks/task (assoc opts :id ref) opts)))))

(defn- relevant-dir?
  [task dir]
  (let [subproject-edn (edn/read-string (slurp (str dir "/deps.edn")))]
    (boolean
     (if (some? (:when task))
       (get subproject-edn (:when task))
       (not (get subproject-edn (:unless task)))))))

(defn- has-dir-filter?
  [task]
  (or (some? (:when task))
      (some? (:unless task))))

(defn- task-relevant-dirs
  "Process for-all statement, accounting for `:exoscale.project/when` predicate.
   The predicate can be given a default value with `:exoscale.project/default`"
  [deps-edn {:keys [for-all] :as task}]
  (let [dirs (or (get-in deps-edn for-all)
                 (throw (ex-info (format "Missing for-all key %s" for-all)
                                 task)))]
    (cond->> dirs
      (some? (has-dir-filter? task))
      (filter (partial relevant-dir? task)))))

(defn task
  [opts args]
   ;; let's assume we have the full env here
  (let [{:as task-deps-edn
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
    (flush)

    (doseq [{:as task :keys [for-all]} task-def
            :let [task (vary-meta task assoc :exoscale.tools.project.api.tasks/task-deps-edn task-deps-edn)]]
      (if (seq for-all)
        (doseq [dir (task-relevant-dirs task-deps-edn task)]
          (run-task! task (merge args {::dir (dir/canonicalize dir)})))
        (run-task! task (merge args {::dir td/*the-dir*}))))))
