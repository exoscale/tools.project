(ns exoscale.tools.project
  (:refer-clojure :exclude [compile])
  (:require [exoscale.tools.project.api :as api]
            [aero.core :as aero]))

(def default-opts
  #:exo.project{:exo.deps-version/key :patch
                :target-dir "target"
                :class-dir "target/classes"
                :javac-opts ["-source" "11" "-target" "11"]
                :src-dirs ["src" "resources"]
                :java-src-dirs ["java"]
                :deps-file "deps.edn"})

(defn read-project
  []
  (try (aero/read-config "project.edn")
       (catch java.io.FileNotFoundException _fnf)))

(defmethod aero/reader 'slurp
  [_ _ file]
  (slurp file))

(defn- qualify-keys
  [opts]
  (update-keys opts
               (fn [k]
                 (if (simple-ident? k)
                   (keyword "exo.project" (name k))
                   k))))

(defn into-opts [opts]
  (merge default-opts
         (read-project)
         (qualify-keys opts)))

(defn clean [opts]
  (-> opts into-opts api/clean))

(defn compile [opts]
  (-> opts
      into-opts
      api/compile))

(defn jar [opts]
  (-> opts
      into-opts
      api/clean
      api/jar))

(defn uberjar
  [opts]
  (-> opts
      into-opts
      api/uberjar))
