(ns red-db.ds
  (:require
   [mount.core :refer [defstate]]
   [next.jdbc.result-set :as rs]
   [next.jdbc :refer [with-options]]
   [camel-snake-kebab.core :as csk]
   [red-db.config :refer [get-datasource]]
   [conman.core :refer [connect! disconnect!]]))

(defstate ^:dynamic *ds*
  :start (let [ds (connect! (get-datasource))]
           (with-options ds
             {:return-keys true
              :builder-fn rs/as-unqualified-kebab-maps})
           *ds*)
  :stop (disconnect! *ds*))
