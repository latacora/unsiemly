(ns unsiemly.elasticsearch
  (:require [qbits.spandex :as spandex]
            [clojure.string :as str]
            [taoensso.timbre :refer [info]]
            [clj-time.coerce :as tc]
            [clj-time.format :as tf])
  (:import (com.amazonaws.auth DefaultAWSCredentialsProviderChain)
           (com.google.common.base Supplier)
           (org.apache.http.impl.nio.client HttpAsyncClientBuilder)
           (java.time LocalDateTime ZoneOffset)
           (vc.inreach.aws.request AWSSigner AWSSigningRequestInterceptor)))

(defn client-from-config
  "Given a config, creates a well-configured client.

  Client automatically comes with an ES sniffer."
  [config]
  (let [cfg (merge
             {:hosts
              (-> config :elasticsearch-hosts (str/split #","))}
             (when (= (config :output-format) "aws-elasticsearch")
               {:http-client
                {::aws-signing-request-interceptor
                 {:service "es"
                  :region (get config :aws-default-region "us-east-1")}}}))
        client (spandex/client cfg)
        sniffer (spandex/sniffer client)]
    client))

(def ^:private ^Supplier clock-supplier
  "A supplier that returns the current UTC date. Only used for AWS request
  signing."
  (reify Supplier
    (get [this]
      (LocalDateTime/now ZoneOffset/UTC))))

(defmethod qbits.spandex.client-options/set-http-client-option!
  ::aws-signing-request-interceptor
  [_ ^HttpAsyncClientBuilder builder {:keys [service region]}]
  (let [interceptor (-> (DefaultAWSCredentialsProviderChain.)
                        (AWSSigner. region service clock-supplier)
                        (AWSSigningRequestInterceptor.))]
    (.addInterceptorLast builder interceptor)))

(defn clean-entry
  "Massages an entry so that it is more palatable for ES.

  Notably, parses the time format to ISO8601 so that we don't have to tell ES
  that's a date index."
  [raw-entry]
  (update raw-entry "created_at"
          (fn [millis-str]
            (let [dt (tc/from-long (Long/parseLong millis-str))]
              (tf/unparse (tf/formatters :date-time) dt)))))

(defn ^:private index-name!
  "Gets an index name for today."
  []
  (.format
   (java.text.SimpleDateFormat. "'unsiemly-'yyyy-MM-dd")
   (java.util.Date.)))

(defn submit-entries
  [client entries]
  (info "submitting " (count entries) " entries to ES")
  (let [index-name (index-name!)]
    (doseq [entry entries]
      (spandex/request
       client
       {:method :post
        :url [index-name :audit_log_entry]
        :body (clean-entry entry)}))))
