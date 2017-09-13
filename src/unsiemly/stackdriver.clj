(ns unsiemly.stackdriver
  (:require [clojure.spec.alpha :as s]
            [unsiemly.internal :as internal]
            [clojure.reflect :as refl]
            [com.rpl.specter :as sr])
  (:import (com.google.cloud.logging
             LogEntry Payload$JsonPayload
             Logging Logging$WriteOption
             LoggingOptions LoggingOptions$Builder)
           (com.google.cloud MonitoredResource)))

(alias 'u 'unsiemly)

(s/def ::project-id string?)
(defmethod internal/opts-spec :stackdriver [_] ::u/base-opts)

(defn ^:private build-client!
  "Creates a StackDriver client.

  Note that building a client requires a project ID to be defined somehow. This
  may be set via opts, though the SDK will attempt to access it in the
  environment. If it's not set in the environment and not set in the opts, this
  may raise an exception."
  [{::keys [project-id]}]
  (let [logging-opts (if (some? project-id)
                       (-> (LoggingOptions/newBuilder)
                           (.setProjectId project-id)
                           (.build))
                       (LoggingOptions/getDefaultInstance))]
    (.getService ^LoggingOptions logging-opts)))

(def ^:private global-resource
  (-> (MonitoredResource/newBuilder "global") (.build)))

(defn ^LogEntry ^:private ->log-entry
  "Given a JSON-able data structure, turns it into a StackDriver LogEntry object."
  [entry]
  (LogEntry/of (Payload$JsonPayload/of entry)))

(defmethod internal/entries-callback :stackdriver
  [opts]
  (let [client (build-client! opts)
        default-write-opts (into-array [(Logging$WriteOption/logName (::u/log-name opts))
                                        (Logging$WriteOption/resource global-resource)])]
    (fn [entries]
      (.write ^Logging client (map ->log-entry entries) default-write-opts))))
