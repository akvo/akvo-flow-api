(ns org.akvo.flow-api.endpoint.data-point
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec]
            [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.data-point :as data-point]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [ring.util.response :refer [response]]))

(defn cursor-url-fn [api-root instance-id survey-id page-size]
  (fn [cursor]
    (format "%sorgs/%s/data-points/%s?%scursor=%s"
            api-root
            instance-id
            survey-id
            (if page-size
              (format "pageSize=%s&" page-size)
              "")
            cursor)))

(defn add-cursor [data-points api-root instance-id survey-id page-size]
  (if (empty? (:data-points data-points))
    (dissoc data-points :cursor)
    (update data-points :cursor (cursor-url-fn api-root
                                               instance-id
                                               survey-id
                                               page-size))))

(def params-spec (clojure.spec/keys :req-un [::spec/survey-id]
                                    :opt-un [::spec/page-size ::spec/cursor]))

(defn endpoint* [{:keys [remote-api api-root]}]
  (GET "/data-points/:survey-id" {:keys [email instance-id alias params]}
    (let [{:keys [survey-id
                  page-size
                  cursor]} (spec/validate-params params-spec
                                                 (rename-keys params
                                                              {:pageSize :page-size}))
          page-size (when page-size
                      (Long/parseLong page-size))
          user-id (user/id-by-email remote-api instance-id email)
          survey (survey/by-id remote-api instance-id user-id survey-id)]
      (-> remote-api
          (data-point/list instance-id user-id survey {:page-size page-size
                                                       :cursor cursor})
          (add-cursor api-root instance-id survey-id page-size)
          (response)))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)))
