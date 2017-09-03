(ns unsiemly.env
  "Tools for creating an options map from the environment.")

(defn opts-from-env!
  "Attempts to read suitable options for building a SIEM sink from environment
  variables."
  []
  (select-keys [:elasticsearch-hosts]))
