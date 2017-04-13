(ns org.akvo.flow-api.main
  (:gen-class)
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.stuartsierra.component :as component]
            [duct.util.runtime :refer [add-shutdown-hook]]
            [duct.util.system :refer [load-system]]
            [environ.core :refer [env]]))

(defn ensure-trailing-slash [s]
  (if (str/ends-with? s "/")
    s
    (str s "/")))

(defn secret-value
  "Reads the value of a secret from a file. It expects an
  environment variable SECRETS_MOUNT_PATH pointing to a folder
  containing secret files"
  [secret-key]
  (let [mount-path (ensure-trailing-slash (:secrets-mount-path env))]
    (-> (format "%s%s" secret-key)
        slurp
        str/trim)))

(defn -main [& args]
  (let [bindings {'http-port (Integer/parseInt (:http-port env "3000"))
                  'github-auth-token (secret-value "github-auth-token")
                  'api-root (ensure-trailing-slash (:api-root env))
                  'sentry-dsn (secret-value "sentry-dsn")
                  'tmp-dir (ensure-trailing-slash (System/getProperty "java.io.tmpdir"))}
        system   (->> (load-system [(io/resource "org/akvo/flow_api/system.edn")] bindings)
                      (component/start))]
    (add-shutdown-hook ::stop-system #(component/stop system))
    (println "Started HTTP server on port" (-> system :http :port))))
