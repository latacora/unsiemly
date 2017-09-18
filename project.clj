(defproject com.latacora/unsiemly "0.2.0-SNAPSHOT"
  :description "Pleasant SIEM abstraction for Clojure"
  :url "https://github.com/latacora/unsiemly"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.9.0-beta1"]
                 [manifold "0.1.7-alpha5"]
                 [environ "1.1.0"]
                 [clojure.java-time "0.3.1"]
                 [camel-snake-kebab "0.4.0"]

                 ;; xforms
                 [com.rpl/specter "1.0.3"]

                 ;; -> ElasticSearch
                 [cc.qbits/spandex "0.5.2"]

                 ;; -> AWS ES
                 [vc.inreach.aws/aws-signing-request-interceptor "0.0.16"]
                 [com.amazonaws/aws-java-sdk-core "1.11.197"
                  :exclusions [commons-logging]]

                 ;; -> StackDriver
                 [com.google.cloud/google-cloud-logging "1.5.1"]]

  :main ^:skip-aot unsiemly.core
  :target-path "target/%s"
  :monkeypatch-clojure-test false ;; https://github.com/clojure-emacs/cider/issues/1841

  ;; generative tests are useful but really slow, so don't run them all the time:
  :test-selectors {:default (complement (some-fn :generative))
                   :generative :generative}

  :profiles {:uberjar {:aot :all}
             :dev {:dependencies [[org.clojure/test.check "0.10.0-alpha2"]
                                  ;; ;; just for joda-time conversions:
                                  [clj-time "0.14.0"]]}}

  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]])
