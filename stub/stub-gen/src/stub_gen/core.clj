(ns stub-gen.core
  (:require [cheshire.core :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [hugsql.core :as hugsql])
  (:import [java.time.format DateTimeFormatter]
           [java.util Date]))

(def date-format (.toFormat (DateTimeFormatter/ISO_INSTANT)))

(hugsql/def-db-fns "stub_gen/queries.sql")

(def conn-str "jdbc:postgresql://192.168.1.206/akvoflow61?user=postgres")

(defn to-iso-8601 [ts]
  (.format date-format (.toInstant (Date. ts))))

(defn folders [parent-id]
  (jdbc/with-db-connection [conn conn-str]
    (let [raw (get-folders conn {:parent-id parent-id})
          parsed (map (fn [item]
                        (let [props (json/parse-string (:props item))
                              id (:id item)
                              created (to-iso-8601 (props "createdDateTime"))
                              updated (to-iso-8601 (props "lastUpdateDateTime"))]
                          {"id" id
                           "parentId" (str (props "parentId"))
                           "name" (format "Folder: %s" (Long/toString (System/nanoTime) 36))
                           "createdAt" created
                           "updatedAt" updated
                           "foldersUrl" (format "https://api.akvo.org/flow/instance/%s/folders?parentId=%s"
                                                "akvoflow-61"
                                                id)
                           "surveysUrl" (format "https://api.akvo.org/flow/instance/%s/surveys?parentId=%s"
                                                "akvoflow-61"
                                                id)})) raw)]
      parsed)))


(defn surveys [folder-id]
  (jdbc/with-db-connection [conn conn-str]
    (let [raw (get-surveys conn {:folder-id folder-id})
          parsed (map (fn [item]
                        (let [props (json/parse-string (:props item))
                              id (:id item)
                              created (to-iso-8601 (props "createdDateTime"))
                              updated (to-iso-8601 (props "lastUpdateDateTime"))]
                          {"id" id
                           "folderId" (str (props "parentId"))
                           "name" (format "Survey: %s" (Long/toString (System/nanoTime) 36))
                           "createdAt" created
                           "updatedAt" updated
                           "surveyUrl" (format "https://api.akvo.org/flow/instance/%s/survey/%s"
                                               "akvoflow-61"
                                               id)})) raw)]
      parsed)))


(defn questions [question-group-id]
  (jdbc/with-db-connection [conn conn-str]
    (let [raw (get-questions conn {:question-group-id question-group-id})
          parsed (map (fn [item]
                        (let [props (json/parse-string (:props item))
                              id (:id item)
                              created (to-iso-8601 (props "createdDateTime"))
                              updated (to-iso-8601 (props "lastUpdateDateTime"))]
                          {"id" id
                           "order" (props "order")
                           "name" (format "Question: %s" (Long/toString (System/nanoTime) 36))
                           "type" (props "type")
                           "createdAt" created
                           "updatedAt" updated})) raw)]
      parsed)))

(defn question-groups [form-id]
  (jdbc/with-db-connection [conn conn-str]
    (let [raw (get-question-groups conn {:form-id form-id})
          parsed (map (fn [item]
                        (let [props (json/parse-string (:props item))
                              id (:id item)
                              created (to-iso-8601 (props "createdDateTime"))
                              updated (to-iso-8601 (props "lastUpdateDateTime"))]
                          {"id" id
                           "order" (props "order")
                           "name" (format "Question Group: %s" (Long/toString (System/nanoTime) 36))
                           "questions" (sort-by #(get % "order")
                                                (questions id))
                           "isRepeatable" (boolean (props "repeatable"))
                           "createdAt" created
                           "updatedAt" updated})) raw)]
      parsed)))

(defn forms [survey-id]
  (jdbc/with-db-connection [conn conn-str]
    (let [raw (get-forms conn {:survey-id survey-id})
          parsed (map (fn [item]
                        (let [props (json/parse-string (:props item))
                              id (:id item)
                              created (to-iso-8601 (props "createdDateTime"))
                              updated (to-iso-8601 (props "lastUpdateDateTime"))]
                          {"id" id
                           "name" (format "Form: %s" (Long/toString (System/nanoTime) 36))
                           "questionGroups" (sort-by #(get % "order")
                                                     (question-groups id))
                           "createdAt" created
                           "updatedAt" updated})) raw)]
      parsed)))


(defn survey [id]
  {"id" id
   "name" "Survey: x"
   "forms" (forms id)})

;; Responses

(defn response-questions [form-definition]
  (let [qs (mapcat (fn [question-group]
                     (map (fn [question]
                            {:id (get question "id")
                             :type (get question "type")
                             :repeatable? (get question-group "isRepeatable")})
                          (get question-group "questions")))
                   (get form-definition "questionGroups"))]
    (reduce (fn [index question]
              (assoc index (:id question) question))
            {}
            qs)))

(defn normalize-iteration [iteration]
  (if (nil? iteration)
    0
    (try (Long/parseLong iteration)
         (catch NumberFormatException e
           0))))

(defn not-implemented [type value]
  (throw (ex-info "Not Implemented" {:type type :value value})))

(defn rand-str []
  (Long/toString (System/nanoTime) 36))

(defn rand-option [prefix]
  {"name" (str prefix " name " (rand-str))
   "code" (str prefix " code " (rand-str))})

(defn anonymize [type value]
  (when value
    (condp = type
      "FREE_TEXT" (str "FREE_TEXT: " (rand-str))
      "OPTION" (rand-option "OPTION")
      "NUMBER" (rand)
      "PHOTO" (format "https://%s/%s.jpg" (rand-str) (rand-str))
      "VIDEO" (format "https://%s/%s.mov" (rand-str) (rand-str))
      "DATE" value
      "CASCADE" (repeatedly (inc (rand-int 6)) #(rand-option "CASCADE")))))

(defn build-response-value [{:keys [type]} value-string]
  (anonymize type
             (condp = type
               "FREE_TEXT" value-string
               "OPTION"  (when value-string
                           (try (json/parse-string value-string)
                                (catch com.fasterxml.jackson.core.JsonParseException e)))
               "NUMBER" (when value-string
                          (try
                            (Double/parseDouble value-string)
                            (catch NumberFormatException e)))
               "GEO" (not-implemented type value-string)
               "PHOTO" value-string
               "VIDEO" value-string
               "SCAN" (not-implemented type value-string)
               "TRACK" (not-implemented type value-string)
               "NAME" (not-implemented type value-string)
               "STRENGTH" (not-implemented type value-string)
               "DATE" (when value-string
                        (to-iso-8601 (try (Long/parseLong value-string)
                                          (catch NumberFormatException e))))
               "CASCADE" (when value-string
                           (try (json/parse-string value-string)
                                (catch com.fasterxml.jackson.core.JsonParseException e)))
               "GEOSHAPE" (not-implemented type value-string)
               "SIGNATURE" (not-implemented type value-string))))

(defn grow-vector [vector size]
  (let [n (count vector)]
    (if (<= n size)
      (into vector (repeat (- size n) nil))
      vector)))

(defn assoc-vector
  "A version of `assoc` for vectors that will grow the vector to make room for the idx"
  [vector idx value]
  (assoc (grow-vector vector (inc idx))
         idx
         value))

(defn responses
  "Creates an index of response data:
    form-instance-id -> question-id -> iteration -> value"
  [form-definition responses-seq]
  (let [questions (response-questions form-definition)]
    (reduce (fn [index {:keys [form-instance-id question-id iteration value]}]
              (if (get-in questions [question-id :repeatable?])
                (update-in index
                           [form-instance-id question-id]
                           (fnil assoc-vector [])
                           (normalize-iteration iteration)
                           (build-response-value (get questions question-id) value))
                (assoc-in index
                          [form-instance-id question-id]
                          (build-response-value (get questions question-id) value))))
            {}
            responses-seq)))

(defn response-form [survey-id form-id]
  (let [survey-definition (survey survey-id)
        forms (get survey-definition "forms")]
    (some #(when (= (get % "id") form-id)
             %)
          forms)))

(defn anonymize-form-instances [form-instances]
  (map #(merge % {"submitter" (str "submitter " (rand-str))
                  "deviceIdentifier" (str "device-identifier-" (rand-str))
                  "identifier" (str "identifier-" (rand-str))
                  "displayName" (str "display-name-" (rand-str))})
       form-instances))

(defn response-data [survey-id form-id]
  (let [;; The full form definition
        form-definition (response-form survey-id form-id)
        ;; A map from form-instance-id to the form-instance
        form-instances (reduce (fn [index form-instance]
                                 (assoc index (get form-instance "id") form-instance))
                               {}
                               (anonymize-form-instances
                                (map #(set/rename-keys % {:id "id"
                                                          :form-id "formId"
                                                          :surveyal-time "surveyalTime"
                                                          :submitter "submitter"
                                                          :submission-date "submissionDate"
                                                          :device-identifier "deviceIdentifier"
                                                          :data-point-id "dataPointId"
                                                          :identifier "identifier"
                                                          :display-name "displayName"})
                                     (get-form-instances conn-str {:form-id form-id}))) )
        ;; An index of response data
        response-index (responses form-definition (get-responses conn-str {:form-id form-id}))]
    {"formInstances" (vals (reduce (fn [resp [form-instance-id form-instance]]
                                           (assoc resp form-instance-id (assoc form-instance
                                                                               "questions"
                                                                               (get response-index form-instance-id))))
                                         {}
                                         form-instances))}))
