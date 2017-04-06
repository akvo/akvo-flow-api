(ns org.akvo.flow-api.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [com.stuartsierra.component :as component]
            [duct.util.runtime :refer [add-shutdown-hook]]
            [duct.util.system :refer [load-system]]
            [environ.core :refer [env]]))

(defn -main [& args]
  (let [bindings {'http-port (Integer/parseInt (:port env "3000"))
                  'github-auth-token (:github-auth-token env)}
        system   (->> (load-system [(io/resource "org/akvo/flow_api/system.edn")] bindings)
                      (component/start))]
    (add-shutdown-hook ::stop-system #(component/stop system))
    (println "Started HTTP server on port" (-> system :http :port))))
