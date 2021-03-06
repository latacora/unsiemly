(ns unsiemly.elasticsearch
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]
            [qbits.spandex :as spandex]
            [unsiemly.internal :as internal])
  (:import (com.amazonaws.auth DefaultAWSCredentialsProviderChain)
           (com.google.common.base Supplier)
           (java.time LocalDateTime ZoneOffset)
           (org.apache.http.impl.nio.client HttpAsyncClientBuilder)
           (vc.inreach.aws.request AWSSigner AWSSigningRequestInterceptor)))

(alias 'u 'unsiemly)

(s/def ::hosts (s/coll-of string?))
(s/def ::aws-region string?)
(s/def ::aws-request-signing boolean?)

(defmethod internal/opts-spec :elasticsearch [_]
  (s/and
   ::u/base-opts
   (s/keys :req [::hosts]
           :opt [::aws-request-signing ::aws-region])))

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

(defn ^:private index-name!
  "Gets an index name for today."
  [log-name]
  (.format
   (java.text.SimpleDateFormat. (str "'" log-name "-'yyyy-MM-dd"))
   (java.util.Date.)))

(defmethod internal/entries-callback :elasticsearch
  [{::u/keys [log-name] ::keys [hosts aws-request-signing aws-region] :as opts}]
  (let [client (spandex/client
                (merge
                 {:hosts hosts}
                 (when aws-request-signing
                   {:http-client
                    {::aws-signing-request-interceptor
                     {:service "es"
                      :region aws-region}}})))
        sniffer (spandex/sniffer client)
        callback (fn [entries]
                   (let [index-name (index-name! log-name)]
                     (doseq [entry entries]
                       (spandex/request
                        client
                        {:method :post
                         :url [index-name :entry]
                         :body entry}))))
        meta {::u/opts opts ::client client ::sniffer sniffer}]
    (with-meta callback meta)))
