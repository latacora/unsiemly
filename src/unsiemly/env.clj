(ns unsiemly.env
  "Tools for creating an options map from the environment."
  (:require [environ.core :as environ]
            [camel-snake-kebab.core :as csk]
            [unsiemly.internal :as internal]
            [clojure.string :as str]))

(defn ^:private parse-list
  [s]
  (str/split s #","))

(defn ^:private parse-bool
  [s]
  (case s
    "true" true
    "false" false))

(def ^:private env-value-parser
  {:siem-type keyword
   :elasticsearch-hosts parse-list
   :elasticsearch-aws-request-signing parse-bool})

(def ^:private base-req-key?
  (set (map (comp keyword name) internal/base-req-keys)))

(defn opts-from-env!
  "Attempts to read suitable options for building a SIEM sink from environment
  variables."
  []
  (let [siem-type (environ/env :siem-type)
        parse-env-entry
        (fn [[k v]]
          (when-let [ns (cond
                          (str/starts-with? (name k) siem-type)
                          (str "unsiemly." siem-type)

                          (base-req-key? k)
                          "unsiemly")]
            [(keyword ns (str/replace-first (name k) (str siem-type "-") ""))
             ((env-value-parser k identity) v)]))]
    (->> environ/env (keep parse-env-entry) (into {}))))
