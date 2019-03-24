(ns unsiemly.sns-test
  (:require
   [clojure.test :as t]
   [unsiemly.sns :as sns]
   [cognitect.aws.client.api :as aws]
   [unsiemly.internal :as internal]))

(alias 'u 'unsiemly)

(t/deftest sns-tests
  (let [arn "arn:aws:sns:us-east-2:1234567890:My-Topic"
        log "what is a loggalo"

        events (atom [])
        add! (fn [& args] (swap! events conj args) (first args))]
    (with-redefs
      [aws/client (partial add! ::client)
       aws/invoke (partial add! ::invoke)]
      (let [cb (internal/entries-callback
                {::u/siem-type :sns
                 ::u/log-name log
                 ::sns/target-arn arn})]
        (t/is (= [[::client {:api :sns}]]
                 @events))
        (reset! events [])

        (cb [{:a 1} {:a 2}])
        (t/is (= [[::invoke
                   ::client
                   {:op :Publish
                    :request {:Message "{\"default\":\"{\\\"a\\\":1}\"}"
                              :MessageStructure "json"
                              :Subject log
                              :TargetArn arn}}]
                  [::invoke
                   ::client
                   {:op :Publish
                    :request {:Message "{\"default\":\"{\\\"a\\\":2}\"}"
                              :MessageStructure "json"
                              :Subject log
                              :TargetArn arn}}]]
                 @events))))))
