(ns org.akvo.flow-api.endpoint.flumenfly
  (:require [clojure.set :refer [rename-keys]]
            [clojure.spec]
            [clojure.walk]
            [compojure.core :refer :all]
            [org.akvo.flow-api.boundary.survey :as survey]
            [org.akvo.flow-api.middleware.resolve-alias]
            [org.akvo.flow-api.boundary.resolve-alias :as resolve-alias]
            [org.akvo.flow-api.middleware.jdo-persistent-manager :as jdo-pm]
            [ring.util.response :refer [response]]
            [org.akvo.flow-api.endpoint.spec :as spec]))

(def survey-list-spec (clojure.spec/coll-of ::spec/full-survey-id))

(def renames {"instance_id" :instance-id
              "survey_id" :survey-id})
(def renames-revert (clojure.set/map-invert renames))

(defn rename [rename-map]
  (fn [m]
    (rename-keys m rename-map)))

(defn surveys [akvo-flow-server-config remote-api email body]
  (let [surveys (spec/validate-params survey-list-spec (map (rename renames) body))]
    (->> surveys
         (map (fn [{alias :instance-id :as m}]
                (assoc m
                  :alias alias
                  :instance-id (resolve-alias/resolve akvo-flow-server-config alias))))
         (filter :instance-id)
         (survey/filter-surveys remote-api email)
         (map (rename {:alias :instance-id}))
         (map (rename renames-revert))
         response)))

(defn endpoint* [{:keys [remote-api akvo-flow-server-config]}]
  (routes
    (POST "/check_permissions" {:keys [email body]}
      (surveys akvo-flow-server-config remote-api email body))))

(defn endpoint [deps]
  (-> (endpoint* deps)
    (jdo-pm/wrap-close-persistent-manager)))

(comment
  (->
    (clj-http.client/post "http://localhost:3000/check_permissions"
      {:as :json
       :headers {"x-akvo-email" "akvo.flow.user.test@gmail.com"}
       :form-params [{:instance_id "akvoflowsandbox"
                      :survey_id "152342023"
                      :additional-properties "props"}
                     {:instance_id "akvoflowsandbox"
                      :survey_id "148412329"}]
       :content-type :json})
    (try (catch Exception e (ex-data e)))
    :body
    ;(cheshire.core/parse-string true)
    )
  )