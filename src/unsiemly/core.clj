(ns unsiemly.core
  (:gen-class)
  (:require
   [unsiemly.internal :as internal]
   [unsiemly.stackdriver :as stackdriver]
   [unsiemly.elasticsearch :as elasticsearch]
   [manifold.stream :as ms]
   [clojure.spec.alpha :as s]))

(defn ->siem!
  "Consume everything in the given source, transform it and send it to a SIEM, as
  described by the given opts."
  [source opts]
  {:pre [(s/valid? :unsiemly/opts opts)]}
  (->> source
       (ms/batch 1 0)
       (ms/consume (internal/entries-callback opts))))

(defn siem-sink!
  "Build a stream (sink) that consumes general events, cleans them up, and
  forwards them to a specific SIEM for ingestion.

  If you already have a stream, look at [[->siem!]]."
  [opts]
  (let [s (ms/stream)]
    (->siem! s opts)
    s))
