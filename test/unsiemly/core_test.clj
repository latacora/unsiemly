(ns unsiemly.core-test
  (:require [clojure.test :as t]
            [unsiemly.core :as c]
            [manifold.deferred :as md]))

(alias 'u 'unsiemly)

(t/deftest validate-spec-test
  (t/is (thrown? AssertionError (c/siem-sink! {})))
  (t/is (thrown? AssertionError (c/siem-sink! {::u/siem-type :invalid}))))

(t/deftest process!-test
  (let [msgs (gensym)
        opts (gensym)]
    (with-redefs [unsiemly.internal/entries-callback
                  (fn [got-opts]
                    (t/is (= opts got-opts))
                    (fn [got-msgs]
                      (t/is (= msgs got-msgs))
                      ::result))]
      (let [r (c/process! opts msgs)]
        (t/is (md/deferred? r))
        (t/is (= ::result @r))))))
