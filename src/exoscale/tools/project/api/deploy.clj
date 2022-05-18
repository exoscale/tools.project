(ns exoscale.tools.project.api.deploy
  (:require [clojure.tools.cli.api :as td]
            [exoscale.tools.project.dir :as dir]
            [deps-deploy.deps-deploy :as dd]
            [exoscale.tools.project.api.jar :as j]))

(defn local
  [opts]
  (let [opts (j/jar opts)]
    (td/mvn-install {:jar (:exoscale.project/jar-file opts)})
    opts))

;; temporary until we have something more official, hence why its keys are ns'ed
(defn remote
  [opts]
  (let [{:as opts
         :exoscale.project/keys [lib target-dir jar-file]
         :slipset.deps-deploy/keys [exec-args]} (j/jar opts)]
    (dd/deploy (into {:artifact (dir/canonicalize jar-file)
                      :pom-file (dir/canonicalize (format "%s/classes/META-INF/maven/%s/pom.xml"
                                                          target-dir
                                                          lib))}
                     exec-args))
    opts))
