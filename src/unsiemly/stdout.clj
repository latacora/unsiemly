(ns unsiemly.stdout
  (:require [unsiemly.internal :as internal]
            [clojure.spec.alpha :as s]))

(alias 'u 'unsiemly)

(defmethod internal/entries-callback :stdout
  [opts]
  (fn [entries] (doseq [e entries] (println e))))

(defmethod internal/opts-spec :stdout [_] ::u/base-opts)
