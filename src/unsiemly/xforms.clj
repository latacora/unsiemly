(ns unsiemly.xforms
  "Data transformation tools useful for SIEMs. Usually this is about format
  conversion (e.g. seconds-since-epoch to ISO8601) or flattening data (e.g.
  turning a map into a string).

  Typically, you'll compose some of these functions and then use something like
  specter or update/update-in to apply them to maps."
  (:require [clj-time.coerce :as tc]
            [clj-time.format :as tf]
            [com.rpl.specter :as sr]))

(defn epoch-millis->
  "Parse a number of milliseconds (as a string or numeric type) to a Joda
  timesatmp."
  [millis]
  (tc/from-long
   (if (string? millis)
     ;; parsing as double because it allows for both "integer" literals as well
     ;; as literals with a decimal point in them, and they still have plenty of
     ;; room to represent real epoch millis for a long time to come.
     (.longValue (Double/parseDouble millis))
     millis)))

(defn ->iso8601-str
  "Given a timestamp, parse it to an ISO8601 timestamp.

  This is for many reasons, but one important one is that ElasticSearch will
  recognize these as date fields with no configuration."
  [timestamp]
  (tf/unparse (tf/formatters :date-time) timestamp))

(def TREE-LEAVES
  "A specter selector for all of the leaves in a nested tree."
  (sr/recursive-path
   [] p
   (sr/cond-path
    vector? [sr/ALL p]
    map? [sr/MAP-VALS p]
    sr/STAY sr/STAY)))

(def NESTED
  "A specter selector for junctions in nested data structures."
  (sr/recursive-path
   [] p
   (sr/cond-path
    vector? (sr/stay-then-continue sr/ALL p)
    map? (sr/stay-then-continue sr/MAP-VALS p))))

(def TREE-KEYS
  "A specter selector for all of the map keys in a nested tree."
  (sr/comp-paths NESTED map? sr/MAP-KEYS))
