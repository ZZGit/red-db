(ns red-db.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]
    [mount.core :refer [args defstate]]))

(defn get-datasource-config
  "获取数据源"
  []
  (let [config (load-config)]
    (if-let [ds-pool (:datasource config)]
      ds-pool
      (if-let [ds-url (:database-url config)]
        {:jdbc-url ds-url}
        (throw (Exception. "no database config"))))))

(defn- get-db-config []
  (:red-db (load-config)))

(defn logic-delete?
  "是否逻辑删除"
  []
  (let [result (:logic-delete? (get-db-config))]
    (if (boolean? result) result false)))

(defn get-logic-delete
  "逻辑删除条件"
  []
  (let [{:keys [logic-delete-field
                logic-delete-value]} (get-db-config)]
    {logic-delete-field logic-delete-value}))

(defn get-logic-delete-where
  "获取逻辑删除查询条件"
  [table-key]
  (let [{:keys [logic-delete-field
                logic-not-delete-value]} (get-db-config)]
    [:= (keyword (name table-key) (name logic-delete-field))
     logic-not-delete-value]))

(defn get-logic-delete-insert
  "逻辑删除插入数据中的值"
  []
  (let [{:keys [logic-delete-field
                logic-not-delete-value]} (get-db-config)]
    {logic-delete-field logic-not-delete-value}))
