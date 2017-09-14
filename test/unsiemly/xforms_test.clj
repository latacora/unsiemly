(ns unsiemly.xforms-test
  (:require [unsiemly.xforms :as xf]
            [clojure.test :as t]
            [clj-time.format :as tf]
            [com.rpl.specter :as sr]))

(t/deftest timestamp-test
  (t/is (= (tf/parse "2009-02-13T23:31:30.000Z")
           (xf/epoch-millis-> "1234567890000"))
        "parse epoch millis from an integer literal")

  (t/is (= (tf/parse "2009-02-13T23:31:30.000Z")
           (xf/epoch-millis-> "1234567890000.0"))
        "parse epoch millis from a decimal literal")

  (t/is (= "2009-02-13T23:31:30.000Z"
           (xf/->iso8601-str (tf/parse "2009-02-13T23:31:30.000Z")))
        "unparse datetime to iso8601"))

(t/deftest selector-test
  (t/are [x ks] (= ks (set (sr/select xf/TREE-LEAVES x)))
    {:a {:b {:c 1}}}
    #{1}

    {:a {:b {:c 1 :d 2}}}
    #{1 2}

    {:a {:b {:c [{:d 1} {:e 2}]}}}
    #{1 2})

  (t/are [x ks] (= ks (set (sr/select xf/TREE-KEYS x)))
    {:a {:b {:c 1}}}
    #{:a :b :c}

    {:a {:b {:c [{:d 1} {:e 2}]}}}
    #{:a :b :c :d :e}))
