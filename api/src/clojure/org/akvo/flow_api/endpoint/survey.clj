(ns org.akvo.flow-api.endpoint.survey
  (:require [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [ring.util.response :refer [response]]))

(defn add-survey-links [surveys api-root instance-id]
  (for [survey surveys]
    (assoc survey
           :survey (format "%sorgs/%s/surveys/%s"
                           api-root instance-id (:id survey)))))

(defn add-form-instances-links [survey api-root instance-id]
  (let [forms (for [form (:forms survey)]
                (assoc form
                       :form-instances-url
                       (format "%sorgs/%s/form-instances/%s/%s"
                               api-root instance-id (:id survey) (:id form))))]
    (assoc survey :forms forms)))

(defn add-data-points-link [survey api-root instance-id]
  (assoc survey
         :data-points-url
         (format "%s/orgs/%s/data-points/%s"
                 api-root instance-id (:id survey))))

(defn endpoint* [{:keys [remote-api api-root]}]
  (routes
   (GET "/surveys/:survey-id" {:keys [email alias instance-id params]}
     (-> remote-api
         (survey/by-id instance-id
                       (user/id-by-email remote-api instance-id email)
                       (:survey-id params))
         (add-form-instances-links api-root alias)
         (add-data-points-link api-root alias)
         (response)))
   (GET "/surveys" {:keys [email alias instance-id params]}
     (-> remote-api
         (survey/list instance-id
                      (user/id-by-email remote-api instance-id email)
                      (:folderId params))
         (add-survey-links api-root alias)
         (response)))))


(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)))
