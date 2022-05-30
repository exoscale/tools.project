(ns exoscale.tools.project
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.tools.deps.alpha.util.dir :as td]
            [cljfmt.main :as cljfmt]
            [clj-kondo.core :as kondo]
            [antq.core :as antq]
            [exoscale.deps-version :as version]
            [exoscale.deps-modules :as deps-modules]
            [exoscale.lingo :as l]
            [exoscale.tools.project.dir :as dir]
            [exoscale.tools.project.api :as api]
            [exoscale.tools.project.api.check :as check]
            [exoscale.tools.project.api.deploy :as deploy]
            [exoscale.tools.project.api.git :as git]
            [exoscale.tools.project.api.jar :as jar]
            [exoscale.tools.project.api.java :as java]
            [exoscale.tools.project.api.tasks :as tasks]
            [exoscale.tools.project.api.version :as v]))

(def default-opts
  #:exoscale.project{:file "deps.edn"
                     :keypath []
                     :exoscale.deps-version/key :patch
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
  (cond-> opts
    (some? version-file)
    (assoc :exoscale.project/version (version/read-version-file* version-file))))

(defn into-opts [opts]
  (let [opts (-> default-opts
                 (merge (read-project (into default-opts opts)) opts)
                 assoc-version)]
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

(defn clean [opts]
  (-> opts into-opts api/clean))

(defn jar [opts]
  (-> opts
      into-opts
      api/clean
      jar/jar))

(defn uberjar
  [opts]
  (-> opts
      into-opts
      jar/uberjar))

(defn install
  [opts]
  (-> opts
      into-opts
      deploy/local))

(defn deploy
  [opts]
  (-> opts
      into-opts
      deploy/remote))

(defn task
  [opts]
  (-> opts
      into-opts
      tasks/task))

(defn release
  [opts]
  (task (assoc (into-opts opts)
               :id :test)))

(defn version-bump-and-snapshot
  [opts]
  (-> opts
      into-opts
      v/bump-and-snapshot))

(defn version-remove-snapshot
  [opts]
  (-> opts
      into-opts
      v/remove-snapshot))

(defn git-commit-version
  [opts]
  (let [opts (into-opts opts)]
    (git/commit-version opts)
    opts))

(defn git-tag-version
  [opts]
  (let [opts (into-opts opts)]
    (git/tag-version opts)
    opts))

(defn git-push
  [opts]
  (let [opts (into-opts opts)]
    (git/push opts)
    opts))

(defn find-source-dirs
  [{:keys                                  [aliases paths]
    :exoscale.project/keys                 [source-path-exclusions]
    :exoscale.tools.project.api.tasks/keys [dir]
    :or                                    {source-path-exclusions #"resources"
                                            dir                    "."}}]
  (into []
        (comp cat
              (remove (partial re-find source-path-exclusions))
              (map dir/canonicalize))
        [paths (get-in aliases [:test :extra-paths])]))

(defn format-check
  [opts]
  (let [opts    (into-opts opts)
        srcdirs (find-source-dirs opts)]
    (println "running format checks with cljfmt for:" (:exoscale.project/lib opts))
    (cljfmt/check srcdirs cljfmt/default-options)
    opts))

(defn format-fix
  [opts]
  (let [opts    (into-opts opts)
        srcdirs (find-source-dirs opts)]
    (println "running format fixes with cljfmt for:" (:exoscale.project/lib opts))
    (cljfmt/check srcdirs cljfmt/default-options)
    opts))

(defn lint
  [opts]
  (let [opts    (into-opts opts)
        srcdirs (find-source-dirs opts)]
    (println "running lint with clj-kondo for:" (:exoscale.project/lib opts))
    (kondo/run! {:lint srcdirs})
    opts))

(defn merge-deps
  [opts]
  (let [opts    (into-opts opts)]
    (deps-modules/merge-deps opts)
    opts))

(defn merge-aliases
  [opts]
  (let [opts    (into-opts opts)]
    (deps-modules/merge-aliases opts)
    opts))

(defn outdated
  [opts]
  (let [opts (into-opts opts)]
    (println "checking for outdated versions with antq for:"
             (:exoscale.project/lib opts))
    (antq/main* (merge {:check-clojure-tools true
                        :directory           ["."]
                        :reporter            "table"}
                       (:antq.core/options opts))
                nil)
    opts))

(defn check
  [opts]
  (let [opts (into-opts opts)]
    (check/check opts)
    opts))
