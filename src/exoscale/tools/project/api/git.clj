(ns exoscale.tools.project.api.git
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.tools.build.api :as b]
            [clojure.tools.deps.alpha.util.dir :as td]
            [exoscale.tools.project.api.version :as v]
            [exoscale.tools.project.io :as pio]))

(defn commit-version
  [{:as opts :exoscale.project/keys [version-file]}]
  (pio/shell [(format "git add %s" version-file)
              "git commit -m \"Version $VERSION\""]
             {:dir td/*the-dir*
              :env {"VERSION" (v/get-version opts)}}))

(defn tag-version
  [opts]
  (pio/shell ["git tag -a \"$VERSION\" --no-sign -m \"Release $VERSION\""]
             {:dir td/*the-dir*
              :env {"VERSION" (v/get-version opts)}}))

(defn push
  [_]
  (pio/shell ["git pull"
              "git push --follow-tags"]
             {:dir td/*the-dir*}))

(defn revision-sha
  [_]
  (-> (pio/shell ["git rev-parse HEAD"]
                 {:dir td/*the-dir*
                  :out :capture
                  :fatal? false})
      :out
      str/trim-newline))

(defn git-count-revs
  [{:as _opts :exoscale.project/keys [version-template-file]
    :or {version-template-file "VERSION_TEMPLATE"}}]
  (str/replace (slurp (td/canonicalize (io/file version-template-file)))
               "GENERATED_VERSION"
               (b/git-count-revs nil)))
