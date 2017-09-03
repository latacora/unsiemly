(defproject unsiemly "0.1.0-SNAPSHOT"
  :description "Pleasant SIEM abstraction for Clojure"
  :url "https://github.com/latacora/unsiemly"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-alpha18"]

                 ;; Stream abstraction
                 [manifold "0.1.7-alpha5"]

                 ;; Configuration
                 [environ "1.1.0"]

                 ;; -> ElasticSearch
                 [cc.qbits/spandex "0.5.1"]

                 ;; -> AWS ES
                 [vc.inreach.aws/aws-signing-request-interceptor "0.0.16"]
                 [com.amazonaws/aws-java-sdk-core "1.11.138"
                  :exclusions [commons-logging]]

                 ;; -> StackDriver
                 [com.google.cloud/google-cloud-logging "1.2.1"]]
  :main ^:skip-aot unsiemly.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
