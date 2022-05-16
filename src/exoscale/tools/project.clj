(ns exoscale.tools.project
  (:refer-clojure :exclude [compile])
  (:require [exoscale.tools.project.api :as api]
            [exoscale.tools.project.api.jar :as jar]
            [exoscale.tools.project.api.java :as java]
            [exoscale.tools.project.api.deploy :as deploy]
            [clojure.tools.deps.alpha.util.dir :as td]
            [clojure.spec.alpha :as s]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [exoscale.lingo :as l]))

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

(s/def :exoscale.project/opts
  (s/keys :opt [:exoscale.project/lib
                :exoscale.project/version
                :exoscale.project/version-file
                :exoscale.project/target-dir
                :exoscale.project/class-dir
                :exoscale.project/javac-opts
                :exoscale.project/src-dirs
                :exoscale.project/java-src-dirs
                :exoscale.project/deps-file]))

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
    (assoc :exoscale.project/version (slurp (td/canonicalize (io/file version-file))))))

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

(defn compile [opts]
  (-> opts
      into-opts
      java/compile))

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

(defn run-shell
  [args {:keys [dir env]}]
  (let [res (apply shell/sh
                   (concat ["sh" "-c"]
                           args
                           (cond-> []
                             dir
                             (conj :dir dir)
                             env
                             (conj :env env))))]
    (when (pos? (:exit res))
      (throw (ex-info "Command failed to run" (assoc res :cmd args))))
    (println (:out res))
    res))

(defn run-task
  [task opts]
  (td/with-dir (td/as-canonical (io/file (:dir opts)))
    ((requiring-resolve (symbol task)) opts)))

(defn- run-cmds!
  [{:as _task :keys [commands tasks]} opts]
  (if commands
    (run! #(run-shell % opts) commands)
    (run! #(run-task % opts) tasks)))

(defn task
  [opts]
  ;; let's assume we have the full env here
  (let [{:exoscale.project/keys [deps-file]} (into-opts opts)
        {:as root-deps-edn
         :exoscale.project/keys [tasks]
         :keys [id]} (edn/read-string (slurp (td/canonicalize (io/file deps-file))))
        task-id (keyword (:id opts))
        task (get tasks task-id)
        for-all (:for-all task)]

    (when-not task
      (println (format "Task '%s' not found" id))
      (System/exit 1))

    (println (format "Running task: '%s'" (name task-id)))

    (if for-all
      (run! #(run-cmds! (assoc task :args opts)
                        {:dir %})
            (get-in root-deps-edn for-all))
      (run-cmds! task nil))))



