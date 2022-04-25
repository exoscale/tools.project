(ns exoscale.tools.project
  (:refer-clojure :exclude [compile])
  (:require [exoscale.tools.project.api :as api]
            [aero.core :as aero]
            [clojure.spec.alpha :as s]
            [exoscale.lingo :as l]))

(def default-opts
  #:exoscale.project{:exoscale.deps-version/key :patch
                     :slipset.deps-deploy/exec-args
                     {:repository {"releases" {:url "s3p://exo-artifacts/releases"}}
                      :installer :remote
                      :sign-releases? false}
                     :target-dir "target"
                     :class-dir "target/classes"
                     :javac-opts ["-source" "11" "-target" "11"]
                     :src-dirs ["src" "resources"]
                     :java-src-dirs ["java"]
                     :deps-file "deps.edn"})

(s/def :exoscale.project/lib qualified-ident?)
(s/def :exoscale.project/version string?)
(s/def :exoscale.project/target-dir string?)
(s/def :exoscale.project/class-dir string?)
(s/def :exoscale.project/javac-opts (s/coll-of string?))
(s/def :exoscale.project/src-dirs (s/coll-of string?))
(s/def :exoscale.project/java-src-dirs (s/coll-of string?))
(s/def :exoscale.project/deps-file string?)

(s/def :exoscale.project/opts
  (s/keys :req [(or :exoscale.project/lib
                    :exoscale.project/modules)]
          :opt [:exoscale.project/version
                :exoscale.project/target-dir
                :exoscale.project/class-dir
                :exoscale.project/javac-opts
                :exoscale.project/src-dirs
                :exoscale.project/java-src-dirs
                :exoscale.project/deps-file]))

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
                   (keyword "exoscale.project" (name k))
                   k))))

(defn into-opts [opts]
  (let [opts (merge default-opts
                    (read-project)
                    (qualify-keys opts))]
    (when-not (s/valid? :exoscale.project/opts opts)
      (prn "Invalid exoscale.project configuration")
      (l/explain :exoscale.project/opts opts)
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

