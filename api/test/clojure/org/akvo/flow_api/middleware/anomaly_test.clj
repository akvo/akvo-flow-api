(ns org.akvo.flow-api.middleware.anomaly-test
  (:require [clojure.test :refer [deftest is]]
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
  (is (= (ex? "... Please try again in 30 seconds ...") :org.akvo.flow-api.anomaly/bad-gateway)))
