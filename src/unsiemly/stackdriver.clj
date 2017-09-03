(ns unsiemly.stackdriver
  (:import [com.google.cloud.logging LogEntry Logging Logging$WriteOption
            LoggingOptions
            Payload$JsonPayload]
           com.google.cloud.MonitoredResource))

(defn client-from-config
  [config]
  (-> (LoggingOptions/getDefaultInstance) (.getService)))

(def ^:private global-resource
  (-> (MonitoredResource/newBuilder "global") (.build)))

(defn ^LogEntry ->log-entry
  "Given a JSON-able data structure, turns it into a StackDriver LogEntry object."
  [entry]
  (LogEntry/of (Payload$JsonPayload/of entry)))

(defn submit-entries
  [^Logging client entries]
  (.write client
          (map ->log-entry entries)
          (Logging$WriteOption/logName "unsiemly")
          (Logging$WriteOption/resource global-resource)))
