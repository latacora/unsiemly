(ns unsiemly.xforms
  "Data transformation tools useful for SIEMs. Usually this is about format
  conversion (e.g. seconds-since-epoch to ISO8601) or flattening data (e.g.
  turning a map into a string).

  Typically, you'll compose some of these functions and then use something like
  specter or update/update-in to apply them to maps."
  (:require [java-time :as jt]
            [com.rpl.specter :as sr])
  (:import [java.time Instant]))

(defn ^Instant epoch-millis->
  "Parse a number of milliseconds (as a string or numeric type) to an instant."
  [millis]
  (jt/instant
   (if (string? millis)
     ;; parsing as double because it allows for both "integer" literals as well
     ;; as literals with a decimal point in them, and they still have plenty of
     ;; room to represent real epoch millis for a long time to come.
     (.longValue (Double/parseDouble millis))
     millis)))

(defn ^String ->iso8601
  "Given a timestamp, parse it to an ISO8601 timestamp.

  This is for many reasons, but one important one is that ElasticSearch will
  recognize these as date fields with no configuration."
  [^Instant instant]
  (jt/format :iso-instant instant))

(def TREE-LEAVES
  "A specter selector for all of the leaves in a nested tree."
  (sr/recursive-path
   [] p
   (sr/cond-path
    map? [sr/MAP-VALS p]
    coll? [sr/ALL p]
    sr/STAY sr/STAY)))

(def NESTED
  "A specter selector for junctions in nested data structures.

  This does post-order traversal. That's useful so that if you're going to
  modify the data structures it finds, that's as efficient as possible."
  (sr/recursive-path
   [] p
   (sr/cond-path
    map? (sr/continue-then-stay sr/MAP-VALS p)
    coll? (sr/continue-then-stay sr/ALL p))))

(def TREE-KEYS
  "A specter selector for all of the map keys in a nested tree."
  (sr/comp-paths NESTED map? sr/MAP-KEYS))

;; Implement Inst for Joda Time, if it's available.
(try
  (Class/forName "org.joda.time.Instant")
  (eval
   '(extend-protocol Inst
      org.joda.time.Instant
      (inst-ms* [inst] (inst-ms* (jt/instant inst)))

      org.joda.time.DateTime
      (inst-ms* [inst] (inst-ms* (jt/instant inst)))))
  (catch ClassNotFoundException cnfe))

(defn insts->iso8601
  "Given a nested data structure, find all the leaves that are also time instants,
  and convert them to iso8601. This will not attempt to be clever and parse e.g.
  strings to see if they're probably a timestamp (say, ISO8601 or RFC822) --
  it's the caller's job to find and parse those first."
  [m]
  (sr/transform [TREE-LEAVES inst?] (comp ->iso8601 jt/instant) m))
