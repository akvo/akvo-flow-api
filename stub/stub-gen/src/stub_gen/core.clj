(ns stub-gen.core
  (:require [clojure.java.jdbc :as jdbc]
            [hugsql.core :as hugsql]
            [cheshire.core :as json])
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


(spit "/project/stub/folders-0.get.json" (json/generate-string {"folders" (folders "0")}
                                                               {:pretty true}))

(spit "/project/stub/folders-2010933.get.json" (json/generate-string {"folders" (folders "2010933")}
                                                               {:pretty true}))

(spit "/project/stub/folders-20650973.get.json" (json/generate-string {"folders" (folders "20650973")}
                                                               {:pretty true}))

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

(spit "/project/stub/surveys-20650973.get.json" (json/generate-string {"surveys" (surveys "20650973")}
                                                               {:pretty true}))


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

(spit "/project/stub/survey-7020925.get.json" (json/generate-string (survey "7020925")
                                                               {:pretty true}))
