(ns org.akvo.flow-api.fixtures
  (:require [com.stuartsierra.component :as component]
            clj-http.client
            [duct.util.system :as duct]))

(def ^:dynamic *system*)

(defmacro try-for [msg how-long & body]
  `(let [start-time# (System/currentTimeMillis)]
     (loop []
       (let [[status# return#] (try
                                 (let [result# (do ~@body)]
                                   [(if result# ::ok ::fail) result#])
                                 (catch Throwable e# [::error e#]))
             more-time# (> (* ~how-long 1000)
                          (- (System/currentTimeMillis) start-time#))]
         (cond
           (= status# ::ok) return#
           more-time# (do (Thread/sleep 1000) (recur))
           (= status# ::fail) (throw (ex-info (str "Failed: " ~msg) {:last-result return#}))
           (= status# ::error) (throw (RuntimeException. (str "Failed: " ~msg) return#)))))))

(defn wait-for-server [url]
  (try-for (str "Not 200 for " url) 60
    (= 200 (:status (clj-http.client/get url)))))

(defn check-servers-up []
  (wait-for-server "http://localhost:8888/_ah/admin")
  (wait-for-server "http://mainnetwork:3000/"))

(defn system [system]
  (fn [f]
    (binding [*system* (component/start (duct/build-system system))]
      (check-servers-up)
      (f)
      (component/stop system))))
