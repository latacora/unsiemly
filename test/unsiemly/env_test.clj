(ns unsiemly.env-test
  (:require [unsiemly.env :as env]
            [unsiemly.elasticsearch :as es]
            [clojure.test :as t]
            [clojure.spec.alpha :as s]
            [unsiemly.stdout :as stdout]))

;; Side-effecting require: this loads ES and StackDriver namespaces, which in
;; turn defines some specs that this needs.
(require 'unsiemly.core)

(alias 'u 'unsiemly)

(t/deftest opts-from-env-elasticsearch-test
  (with-redefs [environ.core/env {:siem-type "elasticsearch"
                                  :log-name "my-log"
                                  :elasticsearch-hosts "127.0.0.1:1234,127.0.0.1:5678"}]
    (let [expected {::u/siem-type :elasticsearch
                    ::u/log-name "my-log"
                    ::es/hosts ["127.0.0.1:1234" "127.0.0.1:5678"]}
          opts (env/opts-from-env!)]
      (t/is (nil? (s/explain-data ::u/opts expected)))
      (t/is (nil? (s/explain-data ::u/opts opts)))
      (t/is (= expected opts)))))

(t/deftest opts-from-env-aws-elasticsearch-test
  (with-redefs [environ.core/env {:siem-type "elasticsearch"
                                  :log-name "my-log"
                                  :elasticsearch-hosts "127.0.0.1:1234,127.0.0.1:5678"
                                  :elasticsearch-aws-request-signing "true"
                                  :elasticsearch-aws-region "us-west-1"}]
    (let [expected {::u/siem-type :elasticsearch
                    ::u/log-name "my-log"
                    ::es/hosts ["127.0.0.1:1234" "127.0.0.1:5678"]
                    ::es/aws-request-signing true
                    ::es/aws-region "us-west-1"}
          opts (env/opts-from-env!)]
      (t/is (nil? (s/explain-data ::u/opts expected)))
      (t/is (nil? (s/explain-data ::u/opts opts)))
      (t/is (= expected opts)))))

(t/deftest opts-from-env-stdout-test
  (with-redefs [environ.core/env {:siem-type "stdout"
                                  :log-name "my-log"
                                  :stdout-pretty-printed "true"}]
    (let [expected {::u/siem-type :stdout
                    ::u/log-name "my-log"
                    ::stdout/pretty-printed true}
          opts (env/opts-from-env!)]
      (t/is (nil? (s/explain-data ::u/opts expected)))
      (t/is (nil? (s/explain-data ::u/opts opts)))
      (t/is (= expected opts)))))
