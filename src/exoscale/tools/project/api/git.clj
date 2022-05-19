(ns exoscale.tools.project.api.git
  (:require [clojure.tools.deps.alpha.util.dir :as td]
            [exoscale.tools.project.api.version :as v]
            [exoscale.tools.project.io :as pio]))

(defn commit+tag-version
  [opts]
  (pio/shell ["git config --global --add safe.directory $PWD"
              "git add VERSION"
              "git commit -m \"Version $VERSION\""
              "git tag -a \"$VERSION\" --no-sign -m \"Release $VERSION\""]
             {:dir td/*the-dir*
              :env {"VERSION" (v/get-version opts)}}))

(defn commit-snapshot
  [opts]
  (pio/shell ["git config --global --add safe.directory $PWD"
              "git add VERSION"
              "git commit -m \"Version $VERSION\""]
             {:dir td/*the-dir*
              :env {"VERSION" (v/get-version opts)}}))

(defn push
  [_]
  (pio/shell ["git pull && git push --follow-tags"]
             {:dir td/*the-dir*}))
