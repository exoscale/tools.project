(ns exoscale.tools.project.api
  (:refer-clojure :exclude [test])
  (:require [clojure.tools.build.api :as b]
            [clojure.edn :as edn]
            [clojure.spec.alpha :as s]
            [exoscale.lingo :as l]
            [babashka.fs :as fs]
            [cljfmt.main :as cljfmt]
            [clj-kondo.core :as kondo]
            [antq.core :as antq]
            [exoscale.tools.project.dir :as dir]
            [exoscale.tools.project.api.git :as git]
            [exoscale.tools.project.io :as pio]))

(defn- find-source-dirs
  [{:keys [aliases paths]
    :exoscale.project/keys [source-path-exclusions]
    :or {source-path-exclusions #"(resources|^target|^classes)"}}]
  (into []
        (comp cat
              (filter (comp fs/exists? dir/canonicalize))
              (remove (partial re-find source-path-exclusions))
              (map dir/canonicalize))
        [paths (get-in aliases [:test :extra-paths])]))

(defn clean
  [opts]
  (let [{:exoscale.tools.project.api.tasks/keys [dir]} opts
        target-dir (str (fs/path dir (:exoscale.project/target-dir opts)))
        extra-clean-targets (for [t (:exoscale.project/extra-clean-targets opts)]
                              (str (fs/path dir t)))
        git-version (str (fs/path dir "resources" "git-version"))]
    (println "running clean for:" (:exoscale.project/lib opts))
    (b/delete {:path target-dir})
    (b/delete {:path git-version})
    (doseq [t extra-clean-targets]
      (b/delete {:path t}))
    opts))

(defn revision-sha
  [opts]
  (let [{:exoscale.tools.project.api.tasks/keys [dir]} opts
        git-version-file (str (fs/path dir "resources" "git-version"))]
    (fs/create-dirs (fs/path dir "resources"))
    (spit git-version-file (git/revision-sha opts))
    (println "storing git sha in" git-version-file)
    opts))

(defn info
  [opts]
  (let [{:exoscale.project/keys [extra-deps-files version modules lib]} opts]
    (printf "%s project: %s version %s [%s]\n"
            (if (some? modules) "multi-module" "standalone")
            lib
            version
            (git/revision-sha opts))
    (doseq [module modules]
      (println " submodule:" module))
    (when (some? extra-deps-files)
      (println " additional managed deps files:" (vec extra-deps-files)))
    (flush)
    opts))

(defn prep
  [{:exoscale.tools.project.api.tasks/keys [dir]
    :exoscale.project/keys [lib]
    :or {dir "."}
    :as opts}]
  (println "running prep task for dependencies in:" lib)
  (pio/shell [["clojure" "-J-Dclojure.main.report=stderr" "-X:deps" "prep" ":log" "debug"]] {:dir dir #_#_:env {"JAVA_OPTS" (System/getenv "JAVA_OPTS")}})
  opts)

(defn prep-self
  [{:exoscale.project/keys [deps-file lib]
    :exoscale.tools.project.api.tasks/keys [dir]
    :or {dir "."}
    :as opts}]
  (let [{:deps/keys [prep-lib]} (-> (fs/path dir deps-file) str slurp edn/read-string)
        {f :fn :keys [alias ensure]} prep-lib]
    (when (some? prep-lib)
      (when-not (s/valid? :deps/prep-lib prep-lib)
        (binding [*out* *err*]
          (l/explain :deps/prep-lib prep-lib {:colors? true})
          (flush)
          (System/exit 1)))
      (println "running prep task for:" lib)
      (pio/shell [["clojure" "-J-Dclojure.main.report=stderr" (str "-X" alias) (str f)]] {:dir dir})
      (when (and (some? ensure) (not (fs/exists? (fs/path dir ensure))))
        (binding [*out* *err*]
          (println "prep failed to produce the required output file or directory:"
                   (str (fs/path dir ensure)))
          (flush)
          (System/exit 1)))))
  opts)

(defn format-check
  [opts]
  (let [srcdirs (find-source-dirs opts)]
    (println "running format checks with cljfmt for:" (:exoscale.project/lib opts))
    (cljfmt/check srcdirs cljfmt/default-options)
    opts))

(defn format-fix
  [opts]
  (let [srcdirs (find-source-dirs opts)]
    (println "running format checks with cljfmt for:" (:exoscale.project/lib opts))
    (cljfmt/fix srcdirs cljfmt/default-options)
    opts))

(defn lint
  [opts]
  (let [srcdirs (find-source-dirs opts)]
    (println "running lint with clj-kondo for:" (:exoscale.project/lib opts))
    (let [{:keys [summary] :as results} (kondo/run! {:lint srcdirs})]
      (kondo/print! results)
      (flush)
      (when (pos? (:error summary))
        (System/exit 1)))
    opts))

(defn outdated
  [opts]
  (println "checking for outdated versions with antq for:"
           (:exoscale.project/lib opts))
  (antq/main* (merge {:check-clojure-tools true
                      :directory ["."]
                      :reporter "table"}
                     (:antq.core/options opts))
              nil)
  opts)

;; We do this here to avoid forcing the registration of
;; the project tool as an extra-dep
(def deps-check-config
  '{:aliases
    {:spootnik-deps-check
     {:extra-deps {org.spootnik/deps-check {:mvn/version "0.5.2"}}}}})

(defn check
  [opts]
  (let [dir (or (:exoscale.tools.project.api.tasks/dir opts) ".")
        source-dirs (find-source-dirs opts)]
    (pio/shell [["clojure" "-J-Dclojure.main.report=stderr" "-Sdeps" (pr-str deps-check-config) "-X:spootnik-deps-check:test"
                 "spootnik.deps-check/check" ":paths" (pr-str source-dirs)]]
               {:dir dir #_#_:env {"JAVA_OPTS" (System/getenv "JAVA_OPTS")}})
    opts))

(defn test
  [opts]
  (let [dir (or (:exoscale.tools.project.api.tasks/dir opts) ".")
        cmdline (reduce-kv
                 (fn [cmdline k v]
                   (-> cmdline
                       (conj (pr-str k))
                       (conj (pr-str v))))
                 ["clojure" "-J-Dclojure.main.report=stderr" "-X:test"]
                 (dissoc opts :exoscale.tools.project.api.tasks/dir))]
    (pio/shell [cmdline] {:dir dir #_#_:env {"JAVA_OPTS" (System/getenv "JAVA_OPTS")}}))
  opts)

(defn version
  [opts]
  (println (:exoscale.project/version opts))
  opts)
