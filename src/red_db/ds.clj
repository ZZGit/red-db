(ns red-db.ds
  (:require
   [mount.core :refer [defstate]]
   [red-db.config :refer [get-datasource]]
   [conman.core :refer [connect! disconnect!]]))

(defstate ^:dynamic *ds*
  :start (connect! (get-datasource))
  :stop (disconnect! *ds*))
