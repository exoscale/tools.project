(ns exoscale.tools.project.api.version
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]))

(def default-opts
  #:exoscale.project.version{:file "VERSION"
                             :default "0.1.0-SNAPSHOT"
                             :op :inc
                             :rx "^(?i)(?:(\\d+)\\.)(?:(\\d+)\\.)(\\d+)(?:\\-(.+))?"})

(s/def :exoscale.project.version/file string?)
(s/def :exoscale.project.version/default string?)
(s/def :exoscale.project.version/key simple-ident?)
(s/def :exoscale.project.version/op #{:inc :dec})
(s/def :exoscale.project.version/suffix (s/nilable string?))
(s/def :exoscale.project.version/suffix-fn ifn?)

(s/def :exoscale.project.version/opts
  (s/keys :req [:exoscale.project.version/file
                :exoscale.project.version/default
                :exoscale.project.version/rx
                (or :exoscale.project.version/key
                    (or :exoscale.project.version/suffix
                        :exoscale.project.version/suffix-fn))]))

(defn parse-version [rx s]
  (let [[[_ major minor patch suffix]] (re-seq rx s)]
    (cond-> #:exoscale.project.version{:major (parse-long (or major 0))
                                       :minor (parse-long (or minor 0))
                                       :patch (parse-long (or patch 0))}
      suffix
      (assoc :exoscale.project.version/suffix suffix))))

(defn read-version-file
  ([] (read-version-file default-opts))
  ([opts]
   (let [{:exoscale.project.version/keys [default file]} (into default-opts opts)]
     (try
       (slurp file)
       (catch java.io.FileNotFoundException _
         default)))))

(defn read-version [{:as opts :exoscale.project.version/keys [rx]}]
  (parse-version (re-pattern rx)
                 (read-version-file opts)))

(defn version-string
  [{:as version-map :exoscale.project.version/keys [suffix]}]
  (cond-> (str/join "." ((juxt
                          :exoscale.project.version/major
                          :exoscale.project.version/minor
                          :exoscale.project.version/patch)
                         version-map))
    (string? suffix)
    (str "-" suffix)))

(defn write-version-file
  [version-map {:exoscale.project.version/keys [file]}]
  (spit file (version-string version-map)))

(defn inc-version
  [version-map k]
  (update version-map k inc))

(defn dec-version
  [version-map k]
  (update version-map k dec))

(defn update-suffix
  [version-map f]
  (update version-map :exoscale.project.version/suffix f))

(defn update-version* [opts]
  (let [{:exoscale.project.version/keys [op key suffix suffix-fn] :as opts}
        (->> opts
             (into default-opts)
             (s/assert :exoscale.project.version/opts))
        version-map (read-version opts)

        version-map (if (ident? key)
                      (let [target-key (keyword "exoscale.project.version" (name key))]
                        (case op
                          :inc (inc-version version-map target-key)
                          :dec (dec-version version-map target-key)))
                      version-map)
        new-version (cond-> version-map
                      ;; only update suffix is it's persent in opt map
                      (contains? opts :exoscale.project.version/suffix)
                      (update-suffix (fn [_] suffix))

                      ;; could be used as a lib (ex via build file)
                      (ifn? suffix-fn)
                      (update-suffix suffix-fn))]
    (write-version-file new-version opts)
    new-version))

(defn update-version
  [& {:as opts}]
  (update-version* (into {}
                         (map (fn [[k v]]
                                [(keyword "exoscale.project.version" (name k)) v]))
                         opts)))

(defn pr-update-version
  [& {:as opts}]
  (prn (update-version opts)))

;; (bump-version :file "/home/mpenet/code/instancepool/VERSION"
;;               :key :patch
;;               :suffix "SNAPSHOT")
