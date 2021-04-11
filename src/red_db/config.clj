(ns red-db.config
  (:require
   [cprop.core :refer [load-config]]))


(defn get-datasource
  "获取数据源"
  []
  (:datasource (load-config)))

(defn- get-db-config []
  (:red-db (load-config)))

(defn logic-delete?
  "是否逻辑删除"
  []
  (let [result (:logic-delete? (get-db-config))]
    (if (boolean? result) result false)))

(defn get-logic-delete-where
  "获取逻辑删除查询条件"
  []
  (let [{:keys [logic-delete-field
                logic-not-delete-value]} (get-db-config)]
    [:= logic-delete-field logic-not-delete-value]))

(defn get-logic-delete-insert
  "逻辑删除插入数据中的值"
  []
  (let [{:keys [logic-delete-field
                logic-not-delete-value]} (get-db-config)]
    {logic-delete-field logic-not-delete-value}))
