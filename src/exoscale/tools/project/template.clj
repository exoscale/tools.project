(ns exoscale.tools.project.template
  (:require [org.corfield.new :as dnew]
            [rewrite-clj.zip :as z]))

(def data-fn
  (constantly nil))

(defn module-data-fn
  [data]
  (update data :target-dir (partial str "modules/")))

(defn template-fn
  [edn _]
  ;; must return the whole EDN hash map
  edn)

(defn init
  [opts]
  (dnew/create {:name (:name opts) :template "exoscale/project"})
  opts)

(defn- add-module-to-deps
  [deps-file shortname]
  (spit
   deps-file
   (let [zloc (z/of-string (slurp deps-file))]
     (z/root-string
      (if-let [modules (z/get zloc :exoscale.project/modules)]
        (z/assoc zloc
                 :exoscale.project/modules
                 (-> modules
                     z/sexpr
                     vec
                     (conj (str "modules/" shortname))))
        (z/assoc zloc :exoscale.project/modules [(str "modules/" shortname)]))))))

(defn add-module
  [opts]
  (let [shortname (name (symbol (:name opts)))]
    (dnew/create {:name (:name opts) :template "exoscale/module"})
    (add-module-to-deps "deps.edn" shortname))
  opts)
