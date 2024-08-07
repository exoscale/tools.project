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
             :when :exoscale.project/deploy?
             :for-all [:exoscale.project/modules]}]

   :jar [{:run :exoscale.tools.project.standalone/jar
          :for-all [:exoscale.project/modules]}]

   :uberjar [{:run :exoscale.tools.project.standalone/uberjar
              :when :exoscale.project/uberjar?
              :for-all [:exoscale.project/modules]}]

   :clean [{:run :exoscale.tools.project.standalone/clean
            :for-all [:exoscale.project/modules]}]

   :check [{:run :exoscale.tools.project.standalone/check
            :for-all [:exoscale.project/modules]}]

   :test [{:run :exoscale.tools.project.standalone/test
           :for-all [:exoscale.project/modules]
           :unless :exoscale.project/bypass-test?}]

   :format/check [{:run :exoscale.tools.project.standalone/format-check
                   :for-all [:exoscale.project/modules]}]

   :format/fix [{:run :exoscale.tools.project.standalone/format-fix
                 :for-all [:exoscale.project/modules]}]

   :lint [{:run :exoscale.tools.project.standalone/lint
           :for-all [:exoscale.project/modules]}]

   :prep [{:run :exoscale.tools.project.standalone/prep
           :for-all [:exoscale.project/modules]
           :when :exoscale.project/needs-prep?}]

   :revision-sha [{:run :exoscale.tools.project.standalone/revision-sha
                   :for-all [:exoscale.project/modules]}]

   :release/single [{:run :exoscale.tools.project.standalone/version-remove-snapshot}
                    {:run :exoscale.tools.project.standalone/deploy}
                    {:run :exoscale.tools.project.standalone/git-commit-version}
                    {:run :exoscale.tools.project.standalone/git-tag-version}
                    {:run :exoscale.tools.project.standalone/version-bump-and-snapshot}
                    {:run :exoscale.tools.project.standalone/git-commit-version}
                    {:run :exoscale.tools.project.standalone/git-push}]

   :release/modules [{:run :exoscale.tools.project.standalone/version-remove-snapshot}
                     {:ref :deploy}
                     {:run :exoscale.tools.project.standalone/git-commit-version}
                     {:run :exoscale.tools.project.standalone/git-tag-version}
                     {:run :exoscale.tools.project.standalone/version-bump-and-snapshot}
                     {:run :exoscale.tools.project.standalone/git-commit-version}
                     {:run :exoscale.tools.project.standalone/git-push}]

   :release+tag/single
   [{:run :exoscale.tools.project.standalone/deploy}
    {:run :exoscale.tools.project.standalone/git-tag-version}
    {:run :exoscale.tools.project.standalone/git-push}]

   :release+tag/modules
   [{:ref :deploy}
    {:run :exoscale.tools.project.standalone/git-tag-version}
    {:run :exoscale.tools.project.standalone/git-push}]

   :prep-self [{:run :exoscale.tools.project.standalone/prep-self
                :for-all [:exoscale.project/modules]
                :when :deps/prep-lib}]})

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

(defn- filter-dir?
  [task sub-edn]
  (let [when' (if-let [when-k (:when task)]
                (get sub-edn when-k)
                true)
        unless' (get sub-edn (:unless task))]
    (and (not unless')
         when')))

(defn- relevant-dir?
  [task dir]
  (->> (str dir "/deps.edn")
       slurp
       edn/read-string
       (filter-dir? task)))

(defn- task-relevant-dirs
  "Process for-all statement, accounting for `:exoscale.project/when` predicate"
  [deps-edn {:keys [for-all] :as task}]
  (let [dirs (or (get-in deps-edn for-all)
                 (throw (ex-info (format "Missing for-all key %s" for-all)
                                 task)))]
    (filter (partial relevant-dir? task)
            dirs)))

(defn task
  [opts args]
   ;; let's assume we have the full env here
  (let [{:as task-deps-edn
         :exoscale.project/keys [tasks lib]
         :keys [id]} (edn/read-string (slurp (dir/canonicalize "deps.edn")))
        tasks (merge default-tasks tasks)
        task-id (keyword (:id opts))
        task-def (get tasks task-id)]

     ;; validate early
    (when-not (s/valid? :exoscale.project/tasks tasks)
      (l/explain :exoscale.project/tasks tasks)
      (flush)
      (System/exit 1))

    (when-not task-def
      (println (format "Task '%s' not found" task-id))
      (System/exit 1))

    (println "starting task:" task-id
             (if lib
               (str "for: " lib)
               "from `root`"))
    (flush)

    (doseq [{:as task :keys [for-all]} task-def
            :let [task (vary-meta task assoc :exoscale.tools.project.api.tasks/task-deps-edn task-deps-edn)]]
      (if (seq for-all)
        (doseq [dir (task-relevant-dirs task-deps-edn task)]
          (run-task! task (merge args {::dir (dir/canonicalize dir)})))
        (run-task! task (merge args {::dir td/*the-dir*}))))))
