(ns exoscale.tools.project.api
  (:refer-clojure :exclude [compile])
  (:require [clojure.tools.build.api :as b]
            [clojure.tools.cli.api :as td]
            [deps-deploy.deps-deploy :as dd]))

(defn create-basis
  ([deps-file]
   (b/create-basis {:project deps-file}))
  ([] (create-basis "deps.edn")))

(defn jar-file*
  ([lib version]
   (jar-file* "target" lib version))
  ([path lib version]
   (format "%s/%s-%s.jar" path lib version)))

(defn uberjar-file*
  ([lib version]
   (uberjar-file* "target" lib version))
  ([path lib version]
   (format "%s/%s-%s-standalone.jar" path lib version)))

(defn get-version
  [{:as _opts :exo.project/keys [version-file version]}]
  (or version
      (some-> version-file slurp)))

(defn clean [opts]
  (let [{:as opts :exo.project/keys [target-dir]} opts]
    (b/delete {:path target-dir})
    opts))

(defn compile [opts]
  (let [{:as opts
         :exo.project/keys [basis class-dir java-src-dirs]} opts]
    (b/javac {:basis basis
              :class-dir class-dir
              :src-dirs java-src-dirs})
    opts))

(defn jar [opts]
  (let [{:exo.project/keys [_env lib _version _version-file class-dir src-dirs basis jar-file deps-file]
         :as opts} opts]
    (clean opts)
    (let [version (get-version opts)
          basis (or basis (create-basis deps-file))
          jar-file (or jar-file (jar-file* (name lib) version))]
      (println "Writing pom.xml")
      (b/write-pom {:basis basis
                    :class-dir class-dir
                    :lib lib
                    :src-dirs src-dirs
                    :version version})
      (println "Copying src-dirs: " src-dirs)
      (b/copy-dir {:src-dirs src-dirs
                   :target-dir class-dir})
      (println "Creating jar:" jar-file)
      (b/jar {:class-dir class-dir
              :jar-file jar-file})
      (into opts
            #:exo.project{:basis basis
                          :version version
                          :jar-file jar-file}))))

(defn uberjar
  [opts]
  (let [{:exo.project/keys [_env lib _version _version-file main src-dirs class-dir basis uberjar-file deps-file]
         :as opts} opts
        _ (clean opts)
        version (get-version opts)
        basis (or basis (create-basis deps-file))
        uber-file (or uberjar-file (uberjar-file* (name lib) version))]
    (println "Copying src-dirs")
    (b/copy-dir {:src-dirs src-dirs
                 :target-dir class-dir})
    (println "Compiling" src-dirs)
    (b/compile-clj {:basis basis
                    :class-dir class-dir
                    :src-dirs src-dirs})
    (println "Creating uberjar: " uber-file)
    (b/uber {:basis basis
             :class-dir class-dir
             :main main
             :uber-file uber-file})
    (into opts
          #:exo.project{:basis basis
                        :version version
                        :uberjar-file uber-file})))

(defn install
  [opts]
  (let [opts (jar opts)]
    (td/mvn-install {:jar (:exo.project/jar-file opts)})
    opts))

(defn deploy
  [opts]
  (let [{:as opts :exo.project/keys [lib target-dir jar-file deploy-repository]} (jar opts)]
    (dd/deploy {:artifact jar-file
                :pom-file (format "%s/classes/META-INF/maven/%s/pom.xml"
                                  target-dir
                                  lib)
                :repository {"releases" deploy-repository}
                :installer :remote
                :sign-releases? false})
    opts))
