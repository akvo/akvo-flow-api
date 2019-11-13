(ns org.akvo.flow-api.endpoint.root
  (:require [compojure.core :refer [GET]]
            [ring.util.response :refer [response]]))

(defn endpoint [_]
  (GET "/" _
    (response {})))
