(ns exoscale.tools.project.api.git
  (:require [clojure.tools.deps.alpha.util.dir :as td]
            [exoscale.tools.project.api.version :as v]
            [exoscale.tools.project.io :as pio]
            [clojure.string :as str]))

(defn commit-version
  [opts]
  (pio/shell ["git add VERSION"
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
  (-> (pio/shell ["git rev-parse HEAD"] {:dir td/*the-dir* :out :capture})
      :out
      str/trim-newline))
