(ns org.akvo.flow-api.middleware.resolve-alias
  (:require [compojure.core :refer [context]]
            [org.akvo.flow-api.anomaly :as anomaly]
            [org.akvo.flow-api.boundary.resolve-alias :as resolve-alias]))

(defn wrap-resolve-alias [handler resolver]
  (context "/orgs/:alias" {{:keys [alias]} :params :as request}
    (if-let [instance-id (resolve-alias/resolve resolver alias)]
      (fn [request]
        (handler (assoc request
                        :instance-id instance-id
                        :alias alias)))
      (anomaly/not-found "Could not resolve alias"
                         {:alias alias}))))
