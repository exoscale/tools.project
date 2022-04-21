(ns exoscale.tools.project
  (:refer-clojure :exclude [compile])
  (:require [exoscale.tools.project.api :as api]
            [aero.core :as aero]
            [clojure.spec.alpha :as s]
            [exoscale.lingo :as l]))

(def default-opts
  #:exo.project{:exo.deps-version/key :patch
                :slipset.deps-deploy/args
                {:repository {"releases" {:url "s3p://exo-artifacts/releases"}}
                 :installer :remote
                 :sign-releases? false}
                :target-dir "target"
                :class-dir "target/classes"
                :javac-opts ["-source" "11" "-target" "11"]
                :src-dirs ["src" "resources"]
                :java-src-dirs ["java"]
                :deps-file "deps.edn"})

(s/def :exo.project/lib qualified-ident?)
(s/def :exo.project/version string?)
(s/def :exo.project/target-dir string?)
(s/def :exo.project/class-dir string?)
(s/def :exo.project/javac-opts (s/coll-of string?))
(s/def :exo.project/src-dirs (s/coll-of string?))
(s/def :exo.project/java-src-dirs (s/coll-of string?))
(s/def :exo.project/deps-file string?)

(s/def :exo.project/opts
  (s/keys :req [:exo.project/lib]
          :opt [:exo.project/version
                :exo.project/target-dir
                :exo.project/class-dir
                :exo.project/javac-opts
                :exo.project/src-dirs
                :exo.project/java-src-dirs
                :exo.project/deps-file]))

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
  (let [opts (merge default-opts
                    (read-project)
                    (qualify-keys opts))]
    (when-not (s/valid? :exo.project/opts opts)
      (prn "Invalid exo.project configuration")
      (l/explain :exo.project/opts opts)
      (System/exit 1))
    opts))

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

(defn install
  [opts]
  (-> opts
      into-opts
      api/install))

(defn deploy
  [opts]
  (-> opts
      into-opts
      api/deploy))

