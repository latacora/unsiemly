(ns unsiemly.internal
  "Internal API and base specs.

  This doesn't live in unsiemly.core for two reasons:

  * It's internal, and core is probably external-facing.
  * Circular dependencies: anything can require this API ns, so it must not
  depend on anything inside unsiemly."
  (:require [clojure.spec.alpha :as s]))

(create-ns 'unsiemly)
(alias 'u 'unsiemly)

(s/def ::u/siem-type #{:stdout :elasticsearch :stackdriver})
(s/def ::u/log-name string?)

(def base-req-keys [::u/siem-type ::u/log-name])

(s/def ::u/base-opts (eval `(s/keys :req ~base-req-keys)))

(defmulti opts-spec ::u/siem-type)
(defmethod opts-spec ::u/stdout [_] ::u/base-opts)

(s/def ::u/opts (s/multi-spec opts-spec ::u/siem-type))

(defmulti entries-callback ::u/siem-type)
(defmethod entries-callback :stdout
  [opts]
  (fn [entries] (doseq [e entries] (println e))))
