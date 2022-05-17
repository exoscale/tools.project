(ns exoscale.tools.project.api.deploy
  (:require [clojure.tools.cli.api :as td]
            [clojure.tools.deps.alpha.util.dir :as tdd]
            [deps-deploy.deps-deploy :as dd]
            [clojure.java.io :as io]
            [exoscale.tools.project.api.jar :as j]))

(defn local
  [opts]
  (let [opts (j/jar opts)]
    (td/mvn-install {:jar (-> opts
                              :exoscale.project/jar-file
                              io/file
                              tdd/canonicalize)})
    opts))

;; temporary until we have something more official, hence why its keys are ns'ed
(defn remote
  [opts]
  (let [{:as opts
         :exoscale.project/keys [lib target-dir jar-file]
         :slipset.deps-deploy/keys [exec-args]} (j/jar opts)]
    (dd/deploy (into {:artifact (tdd/canonicalize (io/file jar-file))
                      :pom-file (tdd/canonicalize
                                 (io/file (format "%s/classes/META-INF/maven/%s/pom.xml"
                                                  target-dir
                                                  lib)))}
                     exec-args))
    opts))
