(ns stub-server.handler
  "Small ring server to serve stub data. Run with 'lein run -m stub-server.handler"
  (:require [compojure.core :refer [GET] :as compojure]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params])
  (:gen-class))

(defn slurpf [fmt & args]
  (slurp (apply format fmt args)))

(defn wrap-error-is-404 [handler]
  (fn [req]
    (try
      (handler req)
      (catch Exception e
        (println (.getMessage e))
        {:status 404
         :body ""}))))

(def routes
  (compojure/routes
   (GET "/folders" {:keys [query-params]}
     (let [parent-id (get query-params "parentId")
           file (slurpf "resources/folders-%s.get.json" parent-id)]
       {:status 200
        :headers {"Content-Type" "application/json"}
        :body file}))
   (GET "/surveys" {:keys [query-params]}
     (let [parent-id (get query-params "folderId")
           file (slurpf "resources/surveys-%s.get.json" parent-id)]
       {:status 200
        :headers {"Content-Type" "application/json"}
        :body file}))
   (GET "/survey/:id" [id]
     (let [file (slurpf "resources/survey-%s.get.json" id)]
       {:status 200
        :headers {"Content-Type" "application/json"}
        :body file}))))

(defn -main [& args]
  (-> routes
      params/wrap-params
      wrap-error-is-404
      (jetty/run-jetty {:port 3333})))
