(ns org.akvo.flow-api.middleware.anomaly-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.tools.logging :as log]
            [clojure.tools.logging.impl :as log-impl]
            [org.akvo.flow-api.middleware.anomaly :as anomaly])
  (:import [java.io IOException]))

(defn ex? [exception-or-message]
  (try
    (anomaly/translate-exception (if (= (type exception-or-message) String)
                                   (IOException. ^String exception-or-message)
                                   exception-or-message))
    "Should never reach here"
    (catch Exception e
      (if (ex-data e)
        (:org.akvo.flow-api/anomaly (ex-data e))
        e))))

(deftest exception-translation
  (let [exception (ArrayIndexOutOfBoundsException. "Hi I am not special")]
    (is (= exception (ex? exception))))
  (let [exception (ArrayIndexOutOfBoundsException. nil)]
    (is (= exception (ex? exception))))
  (is (= (ex? "... Over Quota ...") :org.akvo.flow-api.anomaly/too-many-requests))
  (is (= (ex? "... required more quota ...") :org.akvo.flow-api.anomaly/too-many-requests))
  (is (= (ex? "... Please try again in 30 seconds ...") :org.akvo.flow-api.anomaly/bad-gateway))
  (is (= (ex? "remote API call: I/O error") :org.akvo.flow-api.anomaly/bad-gateway)))

;; Binds the logger factory rather than redefining `log*`: `with-redefs` alters a var
;; root every thread shares, eftest runs these in parallel, and the sibling test reaches
;; the same branch -- so a redef captures whichever throwable arrived last.
(defn captured-log [f]
  (let [entry (atom nil)
        factory (reify log-impl/LoggerFactory
                  (name [_] "capturing")
                  (get-logger [_ _]
                    (reify log-impl/Logger
                      (enabled? [_ _] true)
                      (write! [_ level throwable _message]
                        (reset! entry {:level level :throwable throwable})))))]
    (binding [log/*logger-factory* factory]
      (try (f) (catch Throwable _ nil)))
    @entry))

(deftest unhandled-exceptions-are-logged-with-their-throwable
  (testing "an exception we do not recognise"
    (let [boom (ArrayIndexOutOfBoundsException. "Hi I am not special")
          entry (captured-log #(anomaly/translate-exception boom))]
      (is (= :error (:level entry)))
      (is (identical? boom (:throwable entry))
          "the throwable must reach the logger, or the entry has no stack trace")))

  (testing "a condition we translate deliberately stays quiet"
    (is (nil? (captured-log #(anomaly/translate-exception (IOException. "... Over Quota ...")))))))
