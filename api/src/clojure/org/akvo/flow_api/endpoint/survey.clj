(ns org.akvo.flow-api.endpoint.survey
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec]
            [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.endpoint.utils :as utils]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [ring.util.response :refer [response]]))

(defn add-survey-links [surveys api-root instance-id]
  (for [survey surveys]
    (assoc survey
           :survey-url (format "%sorgs/%s/surveys/%s"
                               api-root instance-id (:id survey)))))

(defn add-form-instances-links [survey api-root instance-id]
  (let [forms (for [form (:forms survey)]
                (assoc form
                       :form-instances-url
                       (format "%sorgs/%s/form_instances?%s"
                               api-root
                               instance-id
                               (utils/query-params-str {"survey_id" (:id survey)
                                                        "form_id" (:id form)}))))]
    (assoc survey :forms forms)))

(defn add-data-points-link [survey api-root instance-id]
  (assoc survey
         :data-points-url
         (format "%sorgs/%s/data_points?%s"
                 api-root
                 instance-id
                 (utils/query-params-str {"survey_id" (:id survey)}))))

(defn surveys-response [surveys]
  (response {:surveys surveys}))

(def survey-definition-params-spec (clojure.spec/keys :req-un [::spec/survey-id]))

(def survey-list-params-spec (clojure.spec/keys :req-un [::spec/folder-id]))

(defn endpoint* [{:keys [remote-api api-root]}]
  (routes
   (GET "/surveys/:survey-id" {:keys [email alias instance-id params]}
     (let [{:keys [survey-id]} (spec/validate-params survey-definition-params-spec
                                                     params)]
       (-> remote-api
           (survey/by-id instance-id
                         (user/id-by-email remote-api instance-id email)
                         survey-id)
           (add-form-instances-links api-root alias)
           (add-data-points-link api-root alias)
           (response))))
   (GET "/surveys" {:keys [email alias instance-id params]}
     (let [{:keys [folder-id]} (spec/validate-params survey-list-params-spec
                                                     (rename-keys params
                                                                  {:folder_id :folder-id}))]
       (-> remote-api
           (survey/list instance-id
                        (user/id-by-email remote-api instance-id email)
                        folder-id)
           (add-survey-links api-root alias)
           (surveys-response))))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)))
