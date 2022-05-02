(ns exoscale.tools.project.path
  "Path and file manipulation utilities"
  (:import java.nio.file.Path))

(defn sibling
  "Resolve a file path relative to the file it was referenced in."
  [src dst]
  (-> (Path/of src (into-array String []))
      (.resolveSibling dst)
      (str)))

