(ns unsiemly.elasticsearch-test
  (:require [unsiemly.elasticsearch :as es]
            [unsiemly.internal :as internal]
            [clojure.test :as t]))

(alias 'u 'unsiemly)

(defn access
  [obj n]
  (let [m (.. obj getClass (getDeclaredField n))]
    (.setAccessible m true)
    (.get m obj)))

(t/deftest index-name-tests
  (t/is (re-matches
         #"my-index-\d{4}-\d{2}-\d{2}"
         (#'es/index-name! "my-index"))))

(t/deftest elasticsearch-entries-callback-tests
  (let [events (atom [])
        add! (fn [& args]
               (swap! events conj args)
               (first args))]
    (with-redefs
      [qbits.spandex/client (partial add! ::client)
       qbits.spandex/sniffer (partial add! ::sniffer)
       qbits.spandex/request (partial add! ::request)
       unsiemly.elasticsearch/index-name!
       (fn [log-name]
         (t/is (= "hi" log-name))
         "hi-2000-01-01")]
      (let [cb (internal/entries-callback
                {::u/siem-type :elasticsearch
                 ::u/log-name "hi"
                 ::es/hosts ["127.0.0.1"]})
            {::es/keys [sniffer client]} (meta cb)]
        (t/is (= [[::client {:hosts ["127.0.0.1"]}]
                  [::sniffer ::client]]
                 @events))
        (reset! events [])
        (cb [{:a 1} {:a 2}])
        (t/is (= [[::request
                   ::client
                   {:method :post
                    :url ["hi-2000-01-01" :entry]
                    :body {:a 1}}]
                  [::request
                   ::client
                   {:method :post
                    :url ["hi-2000-01-01" :entry]
                    :body {:a 2}}]]
                 @events))))))

(t/deftest aws-elasticsearch-entries-callback-tests
  (let [events (atom [])
        add! (fn [& args]
               (swap! events conj args)
               (first args))]
    (with-redefs
      [qbits.spandex/client (partial add! ::client)
       qbits.spandex/sniffer (partial add! ::sniffer)
       qbits.spandex/request (partial add! ::request)
       unsiemly.elasticsearch/index-name!
       (fn [log-name]
         (t/is (= "hi" log-name))
         "hi-2000-01-01")]
      (let [cb (internal/entries-callback
                {::u/siem-type :aws-elasticsearch
                 ::u/log-name "hi"
                 ::es/hosts ["127.0.0.1"]
                 ::es/aws-region "us-west-1"})
            {::es/keys [sniffer client]} (meta cb)]
        (t/is (= [[::client {:hosts ["127.0.0.1"]
                             :http-client
                             {::es/aws-signing-request-interceptor
                              {:service "es"
                               :region "us-west-1"}}}]
                  [::sniffer ::client]]
                 @events))
        (reset! events [])
        (cb [{:a 1} {:a 2}])
        (t/is (= [[::request
                   ::client
                   {:method :post
                    :url ["hi-2000-01-01" :entry]
                    :body {:a 1}}]
                  [::request
                   ::client
                   {:method :post
                    :url ["hi-2000-01-01" :entry]
                    :body {:a 2}}]]
                 @events))))))
