(ns unsiemly.env
  "Tools for creating an options map from the environment."
  (:require [environ.core :as environ]
            [camel-snake-kebab.core :as csk]
            [unsiemly.internal :as internal]
            [clojure.string :as str]))

(def ^:private env-value-parser
  {:siem-type keyword
   :elasticsearch-hosts #(str/split % #",")})

(def ^:private base-req-key?
  (set (map (comp keyword name) internal/base-req-keys)))

(defn opts-from-env!
  "Attempts to read suitable options for building a SIEM sink from environment
  variables."
  []
  (let [siem-type (environ/env :siem-type)
        prefix (if (= siem-type "aws-elasticsearch")
                 "elasticsearch"
                 siem-type)

        parse-env-entry
        (fn [[k v]]
          (when-let [ns (cond
                          (str/starts-with? (name k) prefix)
                          (str "unsiemly." prefix)

                          (base-req-key? k)
                          "unsiemly")]
            [(keyword ns (str/replace-first (name k) (str prefix "-") ""))
             ((env-value-parser k identity) v)]))]
    (->> environ/env (keep parse-env-entry) (into {}))))
