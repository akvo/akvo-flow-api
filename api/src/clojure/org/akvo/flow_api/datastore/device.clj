(ns org.akvo.flow-api.datastore.device
  (:refer-clojure :exclude [list])
  (:require [org.akvo.flow-api.datastore :as ds])
  (:import [com.gallatinsystems.device.dao DeviceDAO]
           [com.gallatinsystems.device.dao DeviceGroupDAO]
           [com.gallatinsystems.device.domain Device]
           [com.gallatinsystems.device.domain DeviceGroup]))

(defn get-device-grops []
  (let [device-group-dao (DeviceGroupDAO.)
        groups (.list device-group-dao "all")]
    (->> groups
         (reduce (fn [ret ^DeviceGroup group]
                   (assoc ret (str (ds/id group)) (.getCode group)))
                 {}))))

(defn list []
  (let [device-dao (DeviceDAO.)
        devices (.list device-dao "all")
        groups (get-device-grops)]
    (->> devices
         (map (fn [^Device device]
                {:id (str (ds/id device))
                 :emei (.getEsn device)
                 :device-id (.getDeviceIdentifier device)
                 :device-group (str (get groups (.getDeviceGroup device)))
                 :last-contact (ds/to-iso-8601 (.getLastLocationBeaconTime device))
                 :version (.getGallatinSoftwareManifest device)
                 :created-at (ds/created-at device)
                 :modified-at (ds/modified-at device)})))))
