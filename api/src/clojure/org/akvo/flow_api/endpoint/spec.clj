
(ns org.akvo.flow-api.endpoint.spec
  (:require [clojure.spec.alpha :as s]
            [org.akvo.flow-api.anomaly :as anomaly])
  (:import [org.apache.commons.codec.binary Base64]))

(s/def ::positive-integer-string (s/and string? #(re-matches #"[0-9]+" %)))
(s/def ::base64-string (s/and string? #(Base64/isBase64 ^String %)))

(s/def ::page-size (s/nilable ::positive-integer-string))
(s/def ::cursor (s/nilable ::base64-string))
(s/def ::survey-id ::positive-integer-string)
(s/def ::instance-id string?)
(s/def ::form-id ::positive-integer-string)
(s/def ::parent-id ::positive-integer-string)
(s/def ::folder-id ::positive-integer-string)

(s/def ::full-survey-id (s/keys :req-un [::survey-id ::instance-id]))

(defn validate-params [spec params]
  (if (s/valid? spec params)
    params
    (anomaly/bad-request "Invalid params"
                         {:problems (mapv (fn [problem]
                                            {:pred (str (:pred problem))
                                             :val (:val problem)
                                             :in (:in problem)})
                                          (:clojure.spec.alpha/problems
                                           (s/explain-data spec params)))})))
