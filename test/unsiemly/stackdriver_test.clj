(ns unsiemly.stackdriver-test
  (:require [unsiemly.stackdriver :as sd]
            [clojure.test :as t]
            [unsiemly.internal :as internal])
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
  (let [writes (atom [])]
    (with-redefs [unsiemly.stackdriver/build-client!
                  (fn [opts]
                    (proxy [Logging] []
                      (write [msgs opts]
                        (swap! writes conj [msgs opts]))))]
      ;; entries-callback attempts to build a client as soon as it is called, so
      ;; we need to do this in a let _inside_ the with-redefs.
      (let [cb (internal/entries-callback
                {::u/siem-type :stackdriver
                 ::u/log-name "mylogname"
                 ::sd/project-id "myproject"})]
        (cb [{"event" {"nested" {"deeply" 1}}} {"event" 2}])
        (cb [{"event" 3} {"event" 4}]))

      (let [[[first-msgs first-opts] [second-msgs second-opts]] @writes
            [first-log-name-opt first-resource-opt] first-opts
            [second-log-name-opt second-resource-opt] second-opts]
        (is-log-name-opt? first-log-name-opt "mylogname")
        (is-log-name-opt? second-log-name-opt "mylogname")

        (is-global-resource-opt? first-resource-opt)
        (is-global-resource-opt? second-resource-opt)

        (t/is (= [] (map #(.getDataAsMap (.getPayload %)) first-msgs)))))))
