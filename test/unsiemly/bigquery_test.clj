(ns unsiemly.bigquery-test
  (:require [unsiemly.bigquery :as ub]
            [clojure.test :as t]
            [unsiemly.internal :as internal]
            [clojure.string :as str])
  (:import (com.google.cloud.bigquery
            BigQuery
            BigQueryError
            InsertAllRequest
            InsertAllResponse
            InsertAllRequest$RowToInsert)))

(alias 'u 'unsiemly)

(def error-xf
  (comp
   (map-indexed
    (fn [i r]
      (when (= (.getContent r) {"nasty" "error"})
        [i [(BigQueryError. "test reason" "test location" "test message")]])))
   (filter some?)))

(def ^:private ->InsertAllResponse
  ;; don't tell me what to do computer I literally own you
  (let [c (-> InsertAllResponse .getDeclaredConstructors first)]
    (.setAccessible c true)
    #(.newInstance c (object-array [%]))))

(defn fake-build-client!
  [insert-requests]
  (fn []
    (proxy [BigQuery] []
      (insertAll [^InsertAllRequest request]
        (def my-req request)
        (swap! insert-requests conj request)
        (->> (.getRows request)
             (into {} error-xf)
             ->InsertAllResponse)))))

(t/deftest entries-callback-test
  (let [insert-requests (atom [])]
    (with-redefs
      [unsiemly.bigquery/build-client! (fake-build-client! insert-requests)]

      (let [cb (internal/entries-callback
                {::u/siem-type :bigquery
                 ::u/log-name "mylogname"})]
        (cb [{:a "1"} {:b "2"}]))

      (t/is (count @insert-requests) 1)
      (let [req (last @insert-requests)
            id (.getTable req)]
        ;; Yes, you're effectively calling (.getTable (.getTable ...)) because what even are words
        (t/is (= "unsiemly" (.getTable id)))
        (t/is (= "mylogname" (.getDataset id)))
        (t/is (nil? (.getProject id)))

        (t/is (=
               [{"a" "1"} {"b" "2"}]
               (map #(.getContent %) (.getRows req)))))

      (let [cb (internal/entries-callback
                {::u/siem-type :bigquery
                 ::ub/project-id "myproject"
                 ::ub/dataset-id "mydataset"
                 ::ub/table-id "mytable"})]
        (cb [{:a "3"} {:b "4"}]))

      (t/is (count @insert-requests) 2)
      (let [req (last @insert-requests)
            id (.getTable req)]
        (t/is (= "mytable" (.getTable id)))
        (t/is (= "mydataset" (.getDataset id)))
        (t/is (= "myproject" (.getProject id)))

        (t/is (= [{"a" "3"} {"b" "4"}]
                 (map #(.getContent %) (.getRows req)))))

      (let [cb (internal/entries-callback
                {::u/siem-type :bigquery
                 ::ub/project-id "myproject"
                 ::ub/dataset-id "mydataset"
                 ::ub/table-id "mytable"})
            out (with-out-str (cb [{:a "5"} {"nasty" "error"}]))]
        (t/is (str/includes? out "inserting bigquery record at index 1"))
        (t/is (str/includes? out "BigQueryError{reason=test reason, location=test location, message=test message}"))))))

(def roundtrip
  (comp #(.getContent ^InsertAllRequest$RowToInsert %) #'ub/->row))

(t/deftest bigquery-roundtrip-tests
  (t/testing "nested maps"
    (let [m {"a" {"a" {"c" 1}}}]
      (t/is (= (roundtrip m))))))
