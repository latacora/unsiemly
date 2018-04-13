(ns unsiemly.stackdriver-test
  (:require [unsiemly.stackdriver :as sd]
            [clojure.test :as t]
            [unsiemly.internal :as internal]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as sgen]
            [clojure.spec.test.alpha :as stest]
            [java-time :as jt]
            [unsiemly.xforms :as xf])
  (:import [com.google.cloud.logging
            LogEntry Payload$JsonPayload
            Logging Logging$WriteOption
            LoggingOptions LoggingOptions$Builder
            Option]
           com.google.cloud.MonitoredResource))

(alias 'u 'unsiemly)

;; build-client! is currently untested: see #9 for details.

(defn ^:private access
  "Get a named field on the superclass of obj."
  [obj n]
  (let [m (.. obj getClass getSuperclass (getDeclaredField n))]
    (.setAccessible m true)
    (.get m obj)))

(defn opt-kv
  [opt]
  [(.name (access opt "optionType"))
   (access opt "value")])

(defn is-global-resource-opt?
  [opt]
  (let [[k v] (opt-kv opt)]
    (t/is (= "RESOURCE" k))
    (t/is (= @#'sd/global-resource v))))

(defn is-log-name-opt?
  [opt log-name]
  (let [[k v] (opt-kv opt)]
    (t/is (= "LOG_NAME" k))
    (t/is (= log-name v))))

(t/deftest entries-callback-test
  (let [writes (atom [])
        first-evs [{"event" {:nested {:deeply 1.0}}} {"event" ::kw-val}]
        first-jsond-evs [{"event" {"nested" {"deeply" 1.0}}} {"event" "kw-val"}]

        second-evs [{:event 3.0} {"event" 4.0}]
        second-jsond-evs [{"event" 3.0} {"event" 4.0}]]
    (with-redefs [unsiemly.stackdriver/build-client!
                  (fn []
                    (proxy [Logging] []
                      (write [msgs opts]
                        (swap! writes conj [msgs opts]))))]
      ;; entries-callback attempts to build a client as soon as it is called, so
      ;; we need to do this in a let _inside_ the with-redefs.
      (let [cb (internal/entries-callback
                {::u/siem-type :stackdriver
                 ::u/log-name "mylogname"
                 ::sd/project-id "myproject"})]
        (cb first-evs)
        (cb second-evs))

      (let [[[first-msgs first-opts] [second-msgs second-opts]] @writes
            [first-log-name-opt first-resource-opt] first-opts
            [second-log-name-opt second-resource-opt] second-opts]
        (is-log-name-opt? first-log-name-opt "mylogname")
        (is-log-name-opt? second-log-name-opt "mylogname")

        (is-global-resource-opt? first-resource-opt)
        (is-global-resource-opt? second-resource-opt)

        (t/is (= first-jsond-evs
                 (map #(.getDataAsMap (.getPayload %)) first-msgs)))
        (t/is (= second-jsond-evs
                 (map #(.getDataAsMap (.getPayload %)) second-msgs)))))))

;; The following tests are going to be confusing unless you go read:
;; - https://github.com/latacora/unsiemly/issues/11
;; - https://github.com/GoogleCloudPlatform/google-cloud-java/issues/2432

(defn roundtrip
  "Do a GCP StackDriver serialization roundtrip to see how data comes out."
  [x]
  (.getDataAsMap (Payload$JsonPayload/of x)))

(create-ns 'unsiemly.stackdriver-test.roundtrippable)
(alias 'rt 'unsiemly.stackdriver-test.roundtrippable)

(s/def ::rt/map (s/map-of string? ::rt/value :gen-max 5))
(s/def ::rt/vec (s/coll-of ::rt/value :gen-max 5))
(s/def ::rt/atom boolean?)
(s/def ::rt/value (s/or :map ::rt/map :vec ::rt/vec :atom ::rt/atom))

(s/fdef fixed-roundtrip :args (s/cat :x ::rt/map) :re ::rt/map)
;; toplevel is a map not a value: has to be a JSON object aka map.

(defn fixed-noniterable-map-roundtrip
  "Like [[roundtrip]], but first wraps this thing in noniterable-map, so more
  things should roundtrip correctly. This basically tests that noniterable-map
  is smart enough to make GCP do the right thing when serializing data."
  [x]
  (roundtrip (xf/noniterable-maps x)))

(t/deftest noniterable-maps-test
  (t/testing "regression test (if this fails GCP fixed a bug; go delete the workaround)"
    (t/is (= {"a" [["b" [["c" 1.0] ["d" 2.0]]]]}
             (roundtrip {"a" {"b" {"c" 1 "d" 2}}}))))

  (t/testing "nested map"
    (let [m {"a" {"b" {"c" 1.0}}}]
      (t/is (= m (fixed-noniterable-map-roundtrip m)))))

  (t/testing "nested map with vecs"
    (let [m {"a" {"b" [{"c" 1.0} {"d" 2.0}]}}]
      (t/is (= m (fixed-noniterable-map-roundtrip m)))))

  (t/testing "nested map with lists"
    (let [m {"a" {"b" (list {"c" 1.0} {"d" 2.0})}}]
      (t/is (= m (fixed-noniterable-map-roundtrip m))))))

(t/deftest ^:generative generative-test
  (t/is (every? (comp nil? :failure) (stest/check `fixed-noniterable-maps-roundtrip))))
