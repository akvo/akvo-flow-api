(ns org.akvo.flow-api.endpoint.data-point
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec]
            [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.data-point :as data-point]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.endpoint.utils :as utils]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [ring.util.response :refer [response]]))

(defn next-page-url [api-root instance-id survey-id page-size cursor]
  (utils/url-builder api-root instance-id "data_points" {"survey_id" survey-id
                                                         "page_size" page-size
                                                         "cursor" cursor}))

(defn add-next-page-url [data-points api-root instance-id survey-id page-size]
  (if (empty? (:data-points data-points))
    (dissoc data-points :cursor)
    (-> data-points
        (assoc :next-page-url
               (next-page-url api-root
                              instance-id
                              survey-id
                              page-size
                              (:cursor data-points)))
        (dissoc :cursor))))

(def params-spec (clojure.spec/keys :req-un [::spec/survey-id]
                                    :opt-un [::spec/page-size ::spec/cursor]))

(defn endpoint* [{:keys [remote-api api-root]}]
  (GET "/data_points" {:keys [email instance-id alias params]}
    (let [{:keys [survey-id
                  page-size
                  cursor]} (spec/validate-params params-spec
                                                 (rename-keys params
                                                              {:survey_id :survey-id
                                                               :page_size :page-size}))
          page-size (when page-size
                      (Long/parseLong page-size))
          user-id (user/id-by-email remote-api instance-id email)
          survey (survey/by-id remote-api instance-id user-id survey-id)]
      (-> remote-api
          (data-point/list instance-id user-id survey {:page-size page-size
                                                       :cursor cursor})
          (add-next-page-url api-root alias survey-id page-size)
          (response)))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)
      (jdo-pm/wrap-close-persistent-manager)))
