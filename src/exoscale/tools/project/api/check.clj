(ns exoscale.tools.project.api.check
  "Inspired from https://github.com/athos/clj-check"
  (:require [bultitude.core  :as bultitude]
            [clojure.java.io :as io]
            [clojure.string  :as str]))

(defn- file-for [ns] (-> ns name (str/replace \- \_) (str/replace \. \/)))

(defn- check-ns
  [ns]
  (println "compiling namespace" ns)
  (try
    (binding [*warn-on-reflection* true]
      (load (file-for ns))
      true)
    (catch Exception e
      (binding [*out* *err*]
        (println "failed to load namespace" ns ":" (ex-message (ex-cause e)))
        false))))

(defn- find-namespaces
  [dirs]
  (bultitude/namespaces-on-classpath
   :classpath (map io/file dirs)
   :ignore-unreadable? false))

(defn- find-dirs
  [opts]
  (filter
   (complement (partial re-find #"resources"))
   (concat (or (:paths opts) ["src"])
           (get-in opts [:aliases :test :extra-paths]))))

(defn check
  [opts]
  (println "namespace checks for:" (:exoscale.project/lib opts))
  (let [dirs       (find-dirs opts)
        namespaces (find-namespaces dirs)]
    (println "namespace checks for:" (:exoscale.project/lib opts) "in" (doall dirs))
    (when (some false? (mapv check-ns namespaces))
      (System/exit 1))
    (shutdown-agents))
  opts)
