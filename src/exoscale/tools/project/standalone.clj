(ns exoscale.tools.project.standalone
  (:refer-clojure :exclude [test])
  (:require [babashka.fs :as fs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.tools.deps.alpha.specs]
            [clojure.tools.deps.alpha.util.dir :as td]
            [exoscale.deps-modules :as deps-modules]
            [exoscale.deps-version :as version]
            [exoscale.lingo :as l]
            [exoscale.tools.project.api :as api]
            [exoscale.tools.project.api.deploy :as deploy]
            [exoscale.tools.project.api.git :as git]
            [exoscale.tools.project.api.jar :as jar]
            [exoscale.tools.project.api.tasks :as tasks]
            [exoscale.tools.project.api.version :as v]
            [exoscale.tools.project.template :as template]))

(def default-opts
  #:exoscale.project{:file "deps.edn"
                     :keypath []
                     :exoscale.deps-version/key :patch
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
(s/def :exoscale.project/uber-opts map?)

(s/def :exoscale.project/opts
  (s/keys :opt [:exoscale.project/lib
                :exoscale.project/version
                :exoscale.project/version-file
                :exoscale.project/target-dir
                :exoscale.project/class-dir
                :exoscale.project/javac-opts
                :exoscale.project/src-dirs
                :exoscale.project/java-src-dirs
                :exoscale.project/deps-file
                :exoscale.project/uber-opts]))

(defn read-project
  [{:as _opts :exoscale.project/keys [file keypath]}]
  (try (some-> (td/canonicalize (io/file file))
               slurp
               edn/read-string
               (get-in keypath))
       (catch java.io.FileNotFoundException _fnf)))

(defn assoc-version
  [{:as opts :exoscale.project/keys [version-file]}]
  (let [v (version/read-version-file* version-file)]
    (cond-> opts
      (some? v)
      (assoc :exoscale.project/version (version/read-version-file* version-file)))))

(defn- assoc-deps-file
  "unless explicit options are given, prepare deps-module configuration"
  [{:exoscale.project/keys [modules extra-deps-files]
    :exoscale.deps/keys [deps-files]
    :as opts}]
  (cond-> opts
    (and (nil? deps-files) (nil? (:deps-files opts)))
    (assoc :deps-files
           (if (some? modules)
             (into []
                   cat
                   [extra-deps-files
                    (map (comp fs/normalize #(str % "/deps.edn")) modules)])
             ["deps.edn"]))))

(defn into-opts [opts]
  (let [opts (-> default-opts
                 (merge (read-project (into default-opts opts)) opts)
                 assoc-version
                 assoc-deps-file)]
    (when-not (s/valid? :exoscale.project/opts opts)
      (let [msg (format "Invalid exoscale.project configuration in %s"
                        (some-> (-> opts
                                    :exoscale.project/file
                                    io/file
                                    .getCanonicalPath)))]
        (println msg)
        (l/explain :exoscale.project/opts opts {:colors? true})
        (flush)
        (System/exit 1)))
    opts))

;; actions meant to be called from the command line

(def ^{:arglists '([opts])} prep
  (comp api/prep into-opts))

(def ^{:arglists '([opts])} prep-self
  (comp api/prep-self into-opts))

(def ^{:arglists '([opts])} add-module
  (comp template/add-module into-opts))

(def ^{:arglists '([opts])} clean
  (comp api/clean into-opts))

(def ^{:arglists '([opts])} jar
  (comp jar/jar api/prep-self api/prep api/clean into-opts))

(def ^{:arglists '([opts])} init
  (comp template/init into-opts))

(def ^{:arglists '([opts])} info
  (comp api/info into-opts))

(def ^{:arglists '([opts])} install
  (comp deploy/local into-opts))

(def ^{:arglists '([opts])} deploy
  (comp deploy/remote into-opts))

(defn task
  [opts]
  (-> opts
      into-opts
      (tasks/task opts)))

(defn release
  [opts]
  (-> opts
      into-opts
      (assoc :id :release/single)
      (tasks/task opts)))

(defn release+tag
  [opts]
  (-> opts
      into-opts
      (assoc :id :release+tag/single)
      (tasks/task opts)))

(def ^{:arglists '([opts])} version-bump-and-snapshot
  (comp v/bump-and-snapshot into-opts))

(def ^{:arglists '([opts])} version-remove-snapshot
  (comp v/remove-snapshot into-opts))

(def ^{:arglists '([opts])} git-commit-version
  (comp git/commit-version into-opts))

(def ^{:arglists '([opts])} git-tag-version
  (comp git/tag-version into-opts))

(def ^{:arglists '([opts])} git-push
  (comp git/push into-opts))

(def ^{:arglists '([opts])} format-check
  (comp api/format-check into-opts))

(def ^{:arglists '([opts])} format-fix
  (comp api/format-fix into-opts))

(def ^{:arglists '([opts])} lint
  (comp api/lint into-opts))

(defn merge-deps
  [opts]
  (doto (into-opts opts)
    (deps-modules/merge-deps)))

(defn merge-aliases
  [opts]
  (doto (into-opts opts)
    (deps-modules/merge-aliases)))

(def ^{:arglists '([opts])} outdated
  (comp api/outdated into-opts))

(def ^{:arglists '([opts])} revision-sha
  (comp api/revision-sha into-opts))

(def ^{:arglists '([opts])} check
  (comp api/check api/revision-version api/revision-sha api/prep-self api/prep into-opts))

(def ^{:arglists '([opts])} uberjar
  (comp jar/uberjar api/revision-version api/revision-sha api/prep-self api/prep api/clean into-opts))

(def ^{:arglists '([opts])} test
  api/test)

(def ^{:arglists '([opts])} version
  (comp api/version into-opts))
