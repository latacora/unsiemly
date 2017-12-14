(ns unsiemly.stdout-test
  (:require [unsiemly.core :as c]
            [clojure.test :as t]
            [unsiemly.stdout :as stdout]
            [clojure.string :as str]
            [manifold.stream :as ms]))

(alias 'u 'unsiemly)

(defmacro with-flushed-out-str
  "Like with-out-str, but also asserts clojure.core/flush was called."
  [& body]
  `(let [flushed?# (atom false)]
     (with-redefs [clojure.core/flush #(reset! flushed?# true)]
       (let [result# (with-out-str ~@body)]
         (t/is @flushed?#)
         result#))))

(t/deftest stdout-str-siem-sink!-test
  (let [s (c/siem-sink! {::u/siem-type :stdout
                         ::u/log-name "test log"})]
    (t/is (= "{:my event}\n{:my second event}\n"
             (with-flushed-out-str
               @(ms/put-all! s [{:my "event"} {:my "second event"}])))))

  (let [s (c/siem-sink! {::u/siem-type :stdout
                         ::u/log-name "test log"})
        deeply-nested [(update-in {} (range 20) :xyzzy)]
        regular (with-flushed-out-str @(ms/put-all! s deeply-nested))
        s (c/siem-sink! {::u/siem-type :stdout
                         ::u/log-name "test log"
                         ::stdout/pretty-printed true})
        pretty-printed (with-flushed-out-str @(ms/put-all! s deeply-nested))
        whitespace-chars #(count (filter (comp str/blank? str) %))]
    (t/is (< (whitespace-chars regular) (whitespace-chars pretty-printed))
          "pretty printed string has more whitespace than regular printed string")))

(t/deftest stdout-json-siem-sink!-test
  (let [s (c/siem-sink! {::u/siem-type :stdout
                         ::u/log-name "test log"
                         ::stdout/format :json})]
    (t/is (= "{\"my\":\"event\"}\n{\"my\":\"second event\"}\n"
             (with-flushed-out-str
               @(ms/put-all! s [{:my "event"} {:my "second event"}])))))

  (let [s (c/siem-sink! {::u/siem-type :stdout
                         ::u/log-name "test log"
                         ::stdout/format :json})
        deeply-nested [(update-in {} (range 20) :xyzzy)]
        regular (with-flushed-out-str @(ms/put-all! s deeply-nested))
        s (c/siem-sink! {::u/siem-type :stdout
                         ::u/log-name "test log"
                         ::stdout/format :json
                         ::stdout/pretty-printed true})
        pretty-printed (with-flushed-out-str @(ms/put-all! s deeply-nested))
        whitespace-chars #(count (filter (comp str/blank? str) %))]
    (t/is (< (whitespace-chars regular) (whitespace-chars pretty-printed))
          "pretty printed string has more whitespace than regular printed string")))
