(ns unsiemly.bigquery
  (:require [unsiemly.internal :as internal]
            [taoensso.timbre :refer [error info]]
            [clojure.spec.alpha :as s]
            [unsiemly.xforms :as xf])
  (:import (com.google.cloud.bigquery
            BigQueryOptions
            InsertAllRequest
            InsertAllRequest$Builder
            InsertAllRequest$RowToInsert
            TableId)))

(alias 'u 'unsiemly)

(s/def ::dataset-id string?)
(s/def ::table-id string?)
(s/def ::project-id string?)

(defn ^:private build-client!
  "Creates a default BigQuery client."
  []
  (.getService (BigQueryOptions/getDefaultInstance)))

(def ^:private ->row
  (comp #(InsertAllRequest$RowToInsert/of %)
        xf/jsonify-val
        xf/seqs->vecs))

(defmethod internal/entries-callback :bigquery
  [{:keys [::table-id ::dataset-id ::project-id ::u/log-name]}]
  (let [dataset-id (or dataset-id log-name)
        table-id (or table-id "unsiemly")
        table (if (some? project-id)
                (TableId/of project-id dataset-id table-id)
                (TableId/of dataset-id table-id))
        client (build-client!)]
    (fn bigquery-entries-callback [entries]
      (let [req (InsertAllRequest/of ^TableId table ^Iterable (map ->row entries))
            res (.insertAll client req)]
        (doseq [[idx errs] (.getInsertErrors res)
                err errs
                :let [msg (format "inserting bigquery record at index %s" idx)
                      row (-> req .getRows (nth idx))]]
          (error msg err "offending row:" row))))))
