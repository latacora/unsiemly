(ns unsiemly.bigquery-test
  (:require [unsiemly.bigquery :as ub]
            [clojure.test :as t]
            [unsiemly.internal :as internal])
  (:import (com.google.cloud.bigquery BigQuery InsertAllRequest InsertAllResponse)))

(alias 'u 'unsiemly)

(t/deftest entries-callback-test
  (let [insert-requests (atom [])]
    (with-redefs
      [unsiemly.bigquery/build-client!
       (fn []
         (proxy [BigQuery] []
           (insertAll [request]
             (swap! insert-requests conj request)
             nil ;; Have to return a InsertAllResponse
             )))]

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
                 (map #(.getContent %) (.getRows req))))))))
