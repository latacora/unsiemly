(ns unsiemly.stdout
  (:require [unsiemly.internal :as internal]
            [clojure.spec.alpha :as s]
            [clojure.pprint :as pp]
            [cheshire.core :as json]))

(alias 'u 'unsiemly)

(s/def ::format #{:str :json})
(s/def ::pretty-printed boolean?)
(s/def ::opts
  (s/and
   ::u/base-opts
   (s/keys :opt [::pretty-printed ::format])))

(defmethod internal/opts-spec :stdout [_]
  ::opts)

(defmethod internal/entries-callback :stdout
  [{::keys [pretty-printed format] :or {format :str}}]
  (let [printer
        (case format
          :str (if pretty-printed pp/pprint println)
          :json (fn [obj]
                  (json/generate-stream obj *out* {:pretty pretty-printed})
                  (println)))]
    (fn [entries]
      (doseq [e entries] (printer e))
      (flush))))
