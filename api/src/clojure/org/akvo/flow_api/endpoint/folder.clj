(ns org.akvo.flow-api.endpoint.folder
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec]
            [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.folder :as folder]
            [org.akvo.flow-api.boundary.user :as user]
            [org.akvo.flow-api.endpoint.spec :as spec]
            [org.akvo.flow-api.endpoint.utils :as utils]
            [org.akvo.flow-api.middleware.resolve-alias :refer [wrap-resolve-alias]]
            [ring.util.response :refer [response]]))

(defn add-links [folders api-root instance-id]
  (for [{:keys [id] :as folder} folders]
    (assoc folder
           :surveys-url (utils/url-builder api-root instance-id "surveys" {"folder_id" id})
           :folders-url (utils/url-builder api-root instance-id "folders" {"parent_id" id}))))

(defn folders-response [folders]
  (response {:folders folders}))

(def params-spec (clojure.spec/keys :opt-un [::spec/parent-id]))

(defn endpoint* [{:keys [remote-api akvo-flow-server-config api-root]}]
  (GET "/folders" {:keys [email instance-id alias params]}
    (let [{:keys [parent-id]} (spec/validate-params params-spec
                                                    (rename-keys params
                                                                 {:parent_id :parent-id}))]
      (-> remote-api
          (folder/list instance-id
                       (user/id-by-email remote-api instance-id email)
                       (or parent-id "0"))
          (add-links api-root alias)
          (folders-response)))))

(defn endpoint [{:keys [akvo-flow-server-config] :as deps}]
  (-> (endpoint* deps)
      (wrap-resolve-alias akvo-flow-server-config)))
