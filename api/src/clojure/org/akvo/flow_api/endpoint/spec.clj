(ns org.akvo.flow-api.endpoint.spec
  (:require [clojure.spec :as spec]
            [org.akvo.flow-api.anomaly :as anomaly])
  (:import [org.apache.commons.codec.binary Base64]))

(spec/def ::positive-integer-string (spec/and string? #(re-matches #"[0-9]+" %)))
(spec/def ::base64-string (spec/and string? #(Base64/isBase64 %)))

(spec/def ::page-size (spec/nilable ::positive-integer-string))
(spec/def ::cursor (spec/nilable ::base64-string))
(spec/def ::survey-id ::positive-integer-string)
(spec/def ::instance-id string?)
(spec/def ::form-id ::positive-integer-string)
(spec/def ::parent-id ::positive-integer-string)
(spec/def ::folder-id ::positive-integer-string)

(spec/def ::full-survey-id (spec/keys :req-un [::survey-id ::instance-id]))

(defn validate-params [spec params]
  (if (spec/valid? spec params)
    params
    (anomaly/bad-request "Invalid params"
                         {:problems (mapv (fn [problem]
                                            {:pred (str (:pred problem))
                                             :val (:val problem)
                                             :in (:in problem)})
                                          (:clojure.spec/problems
                                           (spec/explain-data spec params)))})))
