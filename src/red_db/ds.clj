(ns red-db.ds
  (:require
   [mount.core :refer [defstate]]
   [next.jdbc.result-set :as rs]
   [next.jdbc :refer [with-options]]
   [red-db.config :refer [get-datasource-config]]
   [conman.core :refer [connect! disconnect!]]))

;; 默认数据源
(defstate ^:dynamic *ds*
  :start (connect! (get-datasource-config))
  :stop (disconnect! *ds*))


;; 事务数据源
(def ^:dynamic *t-ds* nil)

(defn get-datasource
  "获取数据源
  数据源来源于三个方面：1：事务的数据源。2：传参的数据源。3：默认数据源
  优先级 事务数据源 > 传参的数据源 > 默认数据源
  "
  ([opt]
   (or *t-ds* (or (:*ds* opt) *ds*)))
  ([]
   (get-datasource nil)))
