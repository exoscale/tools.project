(ns exoscale.tools.project.api
  (:require [clojure.tools.build.api :as b]
            [babashka.fs :as fs]
            [exoscale.tools.project.api.git :as git]))

(defn create-basis
  ([deps-file]
   (b/create-basis {:project deps-file}))
  ([]
   (create-basis "deps.edn")))

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
