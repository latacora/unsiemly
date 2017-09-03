(ns unsiemly.core
  (:gen-class)
  (:require [clojure.set :as set]
            [clojure.spec.alpha :as s]
            [unsiemly.stackdriver :as stackdriver]
            [unsiemly.elasticsearch :as elasticsearch]
            [environ.core :refer [env]]
            [manifold.stream :as ms]))

(s/def ::siem-type #{:stdout :elasticsearch :aws-elasticsearch :stackdriver})

(defmulti opts-spec ::siem-type)
(s/def ::opts (s/multi-spec opts-spec ::siem-type))

(defmethod opts-spec ::stdout [_] (s/keys :req [::siem-type]))

(defn ->siem!
  "Consume everything in the given source, transform it and send it to a SIEM, as
  described by the given opts."
  [source opts]
  )

(defn siem-sink!
  "Build a stream (sink) that consumes general events, cleans them up, and
  forwards them to a specific SIEM for ingestion.

  If you already have a stream, look at [[->siem!]]."
  [opts]
  (->siem! (ms/stream) opts))
