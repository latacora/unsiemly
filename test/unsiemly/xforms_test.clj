(ns unsiemly.xforms-test
  (:require [unsiemly.xforms :as xf]
            [clojure.test :as t]
            [clj-time.format :as tf]))

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
