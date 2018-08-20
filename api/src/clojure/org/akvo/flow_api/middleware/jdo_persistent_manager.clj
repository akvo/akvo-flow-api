(ns org.akvo.flow-api.middleware.jdo-persistent-manager
  (:import (com.gallatinsystems.framework.servlet PersistenceFilter)))

(defn wrap-close-persistent-manager [handler]
  (fn [request]
    (try
      (handler request)
      (finally
        (let [jdo-persistence-manager (PersistenceFilter/getManager)]
          (.flush jdo-persistence-manager)
          (.close jdo-persistence-manager))))))