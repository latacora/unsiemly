(ns unsiemly.core
  (:gen-class)
  (:require
   [unsiemly.internal :as internal]
   [manifold.stream :as ms]
   [manifold.deferred :as md]
   [clojure.spec.alpha :as s]))

;; side-effecty requires:
(require '[unsiemly.stackdriver])
(require '[unsiemly.elasticsearch])
(require '[unsiemly.stdout])

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

(defn process!
  "Sends a single batch of msgs to a SIEM defined by opts. Returns a deferred that
  will fire when the sending is complete."
  [opts msgs]
  (let [cb (internal/entries-callback opts)]
    (md/future (cb msgs))))
