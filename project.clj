(defproject com.latacora/unsiemly "0.8.0-SNAPSHOT"
  :description "Pleasant SIEM abstraction for Clojure"
  :url "https://github.com/latacora/unsiemly"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0"]
                 [manifold "0.1.7-alpha5"]
                 [environ "1.1.0"]
                 [clojure.java-time "0.3.1"]
                 [camel-snake-kebab "0.4.0"]
                 [com.taoensso/timbre "4.10.0"]

                 ;; xforms
                 [com.rpl/specter "1.1.0"]

                 ;; -> ElasticSearch
                 [cc.qbits/spandex "0.5.5"]

                 ;; -> AWS ES
                 [vc.inreach.aws/aws-signing-request-interceptor "0.0.20"]
                 [com.amazonaws/aws-java-sdk-core "1.11.281"
                  :exclusions [commons-logging]]

                 ;; -> StackDriver
                 [com.google.cloud/google-cloud-logging "1.16.0"]

                 ;; -> BigQuery
                 [com.google.cloud/google-cloud-bigquery "0.34.0-beta"]]

  :main ^:skip-aot unsiemly.core
  :target-path "target/%s"
  :monkeypatch-clojure-test false ;; https://github.com/clojure-emacs/cider/issues/1841

  ;; generative tests are useful but really slow, so don't run them all the time:
  :test-selectors {:default (complement (some-fn :generative))
                   :generative :generative}

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "0.10.0-alpha2"]
                                  ;; ;; just for joda-time conversions:
                                  [clj-time "0.14.2"]]}}

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]])
