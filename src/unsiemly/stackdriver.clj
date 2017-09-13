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

;; Trust me, I'm a doctor :-)
;; See:
;; - https://github.com/latacora/unsiemly/issues/11
;; - https://github.com/GoogleCloudPlatform/google-cloud-java/issues/2432

(defmacro defproxytype
  [type-name iface-sym]
  (let [iface (Class/forName (str iface-sym))
        {:keys [members]} (refl/reflect iface)
        wrapped-obj '(.-obj this)]
    `(deftype ~type-name [~(with-meta 'obj {:tag iface})]
       ~iface-sym
       ~@(for [{:keys [name parameter-types return-type]} members
               :let [m (with-meta (symbol name) {:tag return-type})
                     args (map (fn [arg-idx arg-type]
                                 (with-meta
                                   (symbol (str "arg" arg-idx))
                                   {:tag arg-type}))
                               (range) parameter-types)]]
           `(~m [~'this ~@args] (. ~wrapped-obj ~m ~@args))))))

(defproxytype JustAMap java.util.Map)

(defn ^:private noniterable-maps
  "Finds all maps that are also iterable in the (nested) data structure x, and
  replace them with zero-copy map replacements that are not also iterable.

  For a rationale, see #11."
  [x]
  (sr/transform
   (sr/recursive-path
    [] p
    (sr/cond-path
     (fn [obj] (and (instance? Iterable obj) (instance? java.util.Map obj)))
     (sr/continue-then-stay sr/MAP-VALS p)

     vector?
     [sr/ALL p]

     sr/STOP sr/STOP))
   ->JustAMap x))

(defn ^:private build-client!
  "Creates a StackDriver client."
  []
  (.getService (LoggingOptions/getDefaultInstance)))

(def ^:private global-resource
  (-> (MonitoredResource/newBuilder "global") (.build)))

(defn ^LogEntry ^:private ->log-entry
  "Given a JSON-able data structure, turns it into a StackDriver LogEntry object."
  [entry]
  (LogEntry/of (Payload$JsonPayload/of (noniterable-maps entry))))

(defmethod internal/entries-callback :stackdriver
  [opts]
  (let [client (build-client!)
        default-write-opts (into-array [(Logging$WriteOption/logName (::u/log-name opts))
                                        (Logging$WriteOption/resource global-resource)])]
    (fn [entries]
      (.write ^Logging client (map ->log-entry entries) default-write-opts))))
