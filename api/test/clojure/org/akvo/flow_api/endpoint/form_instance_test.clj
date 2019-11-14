(ns org.akvo.flow-api.endpoint.form-instance-test
  (:require [clojure.test :refer [deftest testing are]]
            [org.akvo.flow-api.endpoint.form-instance :refer :all])
  (:import java.time.Instant))


(deftest filtering-by-submission-date
  (testing "Parsing submissionDate expression"
    (let [ts (Instant/parse "2019-11-11T14:02:48Z")]
      (are [x y] (= y (parse-filter x))
        ">1573480968" {:operator ">" :timestamp ts}
        ">=1573480968" {:operator ">=" :timestamp ts}
        "<=1573480968" {:operator "<=" :timestamp ts}
        "<1573480968" {:operator "<" :timestamp ts}
        ">=2019-11-11T14:02:48Z" {:operator ">=" :timestamp ts}
        ">2019-11-11T14:02:48Z" {:operator ">" :timestamp ts}
        "<=2019-11-11T14:02:48Z" {:operator "<=" :timestamp ts}
        "<2019-11-11T14:02:48Z" {:operator "<" :timestamp ts}))))
