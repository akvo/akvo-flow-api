(ns org.akvo.flow-api.endpoint.survey
  (:require [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [ring.util.response :refer [response]])
  (:import [clojure.lang ExceptionInfo]))

(defn add-survey-links [surveys api-root instance-id]
  (for [survey surveys]
    (assoc survey
           :survey (format "%s/orgs/%s/surveys/%s"
                           api-root instance-id (:id survey)))))

(defn add-form-instances-links [survey api-root instance-id]
  (let [forms (for [form (:forms survey)]
                (assoc form
                       :form-instances-url
                       (format "%s/orgs/%s/form-instances/%s/%s"
                               api-root instance-id (:id survey) (:id form))))]
    (assoc survey :forms forms)))

(defn endpoint [{:keys [remote-api api-root]}]
  (context "/orgs" {:keys [email params]}
    (let-routes []
      (GET "/:instance-id/surveys/:survey-id" [instance-id survey-id]
        (-> remote-api
          (survey/by-id instance-id
                        (user/id-by-email remote-api instance-id email)
                        survey-id)
          (add-form-instances-links api-root instance-id)
          (response)))
      (GET "/:instance-id/surveys" [instance-id]
        (-> remote-api
          (survey/list instance-id
                       (user/id-by-email remote-api instance-id email)
                       (:folderId params))
          (add-survey-links api-root instance-id)
          (response))))))
