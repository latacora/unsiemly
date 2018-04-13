(ns unsiemly.xforms-test
  (:require [unsiemly.xforms :as xf]
            [clojure.test :as t]
            [clj-time.format :as tf]
            [java-time :as jt]
            [com.rpl.specter :as sr]
            [clj-time.coerce :as cljtc]))

(t/deftest timestamp-test
  (t/is (= (jt/instant "2009-02-13T23:31:30.000Z")
           (xf/epoch-millis-> "1234567890000"))
        "parse epoch millis from an integer literal")

  (t/is (= (jt/instant "2009-02-13T23:31:30.000Z")
           (xf/epoch-millis-> "1234567890000.0"))
        "parse epoch millis from a decimal literal")

  (t/is (= "2009-02-13T23:31:30.100Z"
           (xf/->iso8601 (jt/instant "2009-02-13T23:31:30.100Z")))
        "unparse datetime to iso8601"))

(t/deftest selector-test
  (t/are [x ks] (= ks (set (sr/select xf/TREE-LEAVES x)))
    {:a {:b {:c 1}}}
    #{1}

    {:a {:b {:c 1 :d 2}}}
    #{1 2}

    {:a {:b {:c [{:d 1} {:e 2}]}}}
    #{1 2}

    {:a {:b {:c '({:d 1} {:e 2})}}}
    #{1 2})

  (t/are [x ks] (= ks (set (sr/select xf/TREE-KEYS x)))
    {:a {:b {:c 1}}}
    #{:a :b :c}

    {:a {:b {:c [{:d 1} {:e 2}]}}}
    #{:a :b :c :d :e}

    {:a {:b {:c '({:d 1} {:e 2})}}}
    #{:a :b :c :d :e}))

(def ref-time-iso8601 "2009-02-13T23:31:30.100Z")
(def ref-time (jt/instant ref-time-iso8601))

(jt/format :iso-instant ref-time)

(t/deftest instants-to-iso8601-test
  (doseq [[inst inst-descr] [[ref-time "java.time Instant"]
                             [(cljtc/from-string ref-time-iso8601) "joda time instant"]
                             [(jt/java-date ref-time) "Java date"]]
          template [(fn [x] {:time x})
                    (fn [x] {:deeply {:nested {:time x}}})
                    (fn [x] {:a [{:b x} {:c x} {:d :something-else} {:nested {:e x}}]})]]
    (t/is (= (template ref-time-iso8601) (xf/insts->iso8601 (template inst)))
          (str inst-descr))))
