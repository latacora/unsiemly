(ns unsiemly.sns
  (:require
   [unsiemly.internal :as internal]
   [unsiemly.xforms :as xf]
   [cognitect.aws.client.api :as aws]
   [cheshire.core :as json]
   [unsiemly.internal :as internal]))

(alias 'u 'unsiemly)

(defmethod internal/entries-callback :sns
  [{:keys [::target-arn ::u/log-name]}]
  (let [sns (aws/client {:api :sns})]
    (fn sns-entries-callback [entries]
      (doseq [e entries
              :let [message (json/generate-string {"default" e})]]
        (aws/invoke sns {:op :Publish
                         :request {:Message message
                                   :MessageStructure "json"
                                   :Subject log-name
                                   :TargetArn target-arn}})))))
