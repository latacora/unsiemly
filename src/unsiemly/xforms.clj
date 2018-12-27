(ns unsiemly.xforms
  "Data transformation tools useful for SIEMs. Usually this is about format
  conversion (e.g. seconds-since-epoch to ISO8601) or flattening data (e.g.
  turning a map into a string).

  Typically, you'll compose some of these functions and then use something like
  specter or update/update-in to apply them to maps."
  (:require [java-time :as jt]
            [com.rpl.specter :as sr]
            [clojure.reflect :as refl])
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

(defn stringify-kw
  "If something is a keyword, turn it into an (unqualified) string.

  See [[jsonify-val]]."
  [maybe-kw]
  (if (keyword? maybe-kw) (name maybe-kw) maybe-kw))

(defn jsonify-val
  "Turns complex types into ones StackDriver or BigQuery will understand."
  [x]
  (->> x
       (sr/transform TREE-KEYS stringify-kw)
       (sr/transform
        TREE-LEAVES
        (fn [x]
          (cond
            (or (boolean? x) (number? x) (string? x)) x
            (keyword? x) (name x)
            (inst? x) (->iso8601 x)
            :else (str x))))))

(defmacro defproxytype
  "Creates a type with given name that implements only the given interface. The
  type will have one constructor, taking an object that implements the same
  interface. Instances of this type will proxy the instance you give it,
  exposing only the behavior of that interface.

  This is useful when you have an object implementing multiple interfaces A,
  B... and you need to interact with some code that does bogus type checking in
  the wrong order (e.g. checking if it's a B first when you want it to act like
  an A). So, create a proxy type for A, wrap your x in the proxy type, and now
  you have x-except-only-looks-like-an-A.

  This uses deftype internally so you'll also get a ->TypeName fn for free.

  To see why this is necessary:
  - https://github.com/latacora/unsiemly/issues/11
  - https://github.com/GoogleCloudPlatform/google-cloud-java/issues/2432
  "
  [type-name iface-sym]
  (let [iface (Class/forName (str iface-sym))
        {:keys [members]} (refl/reflect iface)
        wrapped-obj '(.-obj this)]
    `(deftype ~type-name [~(with-meta 'obj {:tag iface})]
       ~iface-sym
       ~@(for [{:keys [name parameter-types return-type flags]} members
               :when (not (:static flags))
               :let [m (with-meta (symbol name) {:tag return-type})
                     args (map (fn [arg-idx arg-type]
                                 (with-meta
                                   (symbol (str "arg" arg-idx))
                                   {:tag arg-type}))
                               (range) parameter-types)]]
           `(~m [~'this ~@args] (. ~wrapped-obj ~m ~@args))))))

(defproxytype JustAMap java.util.Map)

(defn noniterable-maps
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

     coll?
     [sr/ALL p]))
   ->JustAMap x))

(defn seqs->vecs
  "Finds all seqs and lists in the nested data structure and turns them into vectors.

  BigQuery gets horribly confused when you give it a Clojure list, which is a
  List but also lazy. I looked through the code and I'm not sure why. Vecs work
  though."
  [x]
  (sr/transform
   (sr/recursive-path
    [] p
    (sr/cond-path
     (fn [obj] (or (map? obj) (instance? java.util.Map obj)))
     [sr/MAP-VALS p]

     (some-fn seq? list? set?) ;; leave vecs alone
     (sr/continue-then-stay sr/ALL p)))
   vec x))
