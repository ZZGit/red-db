(ns red-db.config
  (:require
   [cprop.source :as source]
   [cprop.core :refer [load-config]]
   [mount.core :refer [args defstate]]
   [clojure.set :refer [intersection]]
   [camel-snake-kebab.core :refer [->kebab-case-keyword]]))

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

(defn logic-delete-exclude-tables?
  "是不是逻辑删除排查的表"
  [tables]
  (let [exclude-tables (or (:logic-delete-exclude-tables (get-db-config)) [])]
    (if (pos? (count exclude-tables))
      (pos?
       (count
        (intersection
         (set (map ->kebab-case-keyword tables))
         (set (map ->kebab-case-keyword exclude-tables)))))
      false)))

(defn logic-delete?
  "是否逻辑删除"
  ([]
   (let [ld? (:logic-delete? (get-db-config))]
     (if (boolean? ld?) ld? false)))
  ([opt]
   (let [ld? (:*logic-delete? opt)]
     (if (boolean? ld?) ld? (logic-delete?)))))

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

(defn get-result-set-builder
  "获取配置的result-set-builder"
  []
  (:result-set-builder (get-db-config)))
