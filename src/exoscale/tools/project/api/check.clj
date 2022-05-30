(ns exoscale.tools.project.api.check
  "Inspired from https://github.com/athos/clj-check"
  (:require [bultitude.core     :as bultitude]
            [clojure.java.shell :as sh]
            [clojure.java.io    :as io]
            [clojure.stacktrace :as st]
            [clojure.string     :as str]))

(defn- file-for [ns] (-> ns name (str/replace \- \_) (str/replace \. \/)))

(defn- check-ns
  [ns]
  (println "compiling namespace" ns)
  (try
    (binding [*warn-on-reflection* true]
      (load (file-for ns)))
    (catch ExceptionInInitializerError e
      (binding [*out* *err*]
        (println "failed to load namespace" ns)
        (st/print-stack-trace e 10)
        e))))

(defn- find-namespaces
  [dirs]
  (bultitude/namespaces-on-classpath
   :classpath (map io/file dirs)
   :ignore-unreadable? false))

(defn check
  [opts]
  (println "namespace checks for:" (:exoscale.project/lib opts))
  (let [dirs       (concat (or (:paths opts) ["src"])
                           (get-in opts [:aliases :test :extra-paths]))
        namespaces (find-namespaces dirs)
        failures   (count
                    (for [ns namespaces :when (check-ns ns)]
                      1))]
    (shutdown-agents)
    (when-not (zero? failures)
      (binding [*out* *err*]
        (println failures "namespaces failed to load"))
      (System/exit 1))))
