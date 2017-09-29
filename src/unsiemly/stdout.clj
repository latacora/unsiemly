(ns unsiemly.stdout
  (:require [unsiemly.internal :as internal]
            [clojure.spec.alpha :as s]
            [clojure.pprint :as pp]))

(alias 'u 'unsiemly)

(s/def ::pretty-printed boolean?)
(s/def ::opts
  (s/and
   ::u/base-opts
   (s/keys :opt [::pretty-printed])))

(defmethod internal/opts-spec :stdout [_]
  ::opts)

(defmethod internal/entries-callback :stdout
  [{::keys [pretty-printed]}]
  (let [printer (if pretty-printed pp/pprint println)]
    (fn [entries]
      (doseq [e entries] (printer e)))))
