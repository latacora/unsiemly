(ns unsiemly.stackdriver
  (:require [clojure.spec.alpha :as s]
            [unsiemly.internal :as internal]
            [unsiemly.xforms :as xf])
  (:import (com.google.cloud.logging
             LogEntry Payload$JsonPayload
             Logging Logging$WriteOption
             LoggingOptions LoggingOptions$Builder)
           (com.google.cloud MonitoredResource)))

(alias 'u 'unsiemly)

(s/def ::project-id string?)
(defmethod internal/opts-spec :stackdriver [_] ::u/base-opts)

(def ^:private prepare-entry
  "Given a nested value consisting of common Clojure types (maps, vecs, insts,
  keywords...) turn it into something that StackDriver's LogEntry knows how to
  serialize."
  (comp xf/noniterable-maps xf/jsonify-val))

(defn ^:private build-client!
  "Creates a StackDriver client."
  []
  (.getService (LoggingOptions/getDefaultInstance)))

(def ^:private global-resource
  (-> (MonitoredResource/newBuilder "global") (.build)))

(defn ^LogEntry ^:private ->log-entry
  "Given a JSON-able data structure, turns it into a StackDriver LogEntry object."
  [entry]
  (LogEntry/of (Payload$JsonPayload/of (prepare-entry entry))))

(defmethod internal/entries-callback :stackdriver
  [opts]
  (let [client (build-client!)
        default-write-opts (into-array [(Logging$WriteOption/logName (::u/log-name opts))
                                        (Logging$WriteOption/resource global-resource)])]
    (fn [entries]
      (.write ^Logging client (map ->log-entry entries) default-write-opts))))
