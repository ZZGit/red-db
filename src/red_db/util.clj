(ns red-db.util
  (:require
   [honeysql.core :as sql]
   [honeysql.helpers :refer [merge-where where]]
   [red-db.ds :refer [*ds*]]
   [red-db.config :as config]))

(defn get-datasource
  "获取数据源"
  [opt]
  (or (:datasource opt) *ds*))

(defn- add-logic-delete-where [sqlmap]
  (if (config/logic-delete?)
    (merge-where sqlmap (config/get-logic-delete-where))
    sqlmap))

(defn- mp->vp
  [props]
  (map
   (fn [[k v]]
     (if (map? v)
       [(first (keys v)) k (first (vals v))]
       [:= k v]))
   props))

(defn- build-simple-sqlmap [from-key props]
  (let [sqlmap {:select [:*]
             :from [from-key]}]
    (apply where (into [sqlmap] (mp->vp props)))))

(defn- build-sqlmap [args]
  (let [first-arg (first args)]
    (if (keyword? first-arg)
      (build-simple-sqlmap first-arg (second args))
      first-arg)))

(defn build-sql
  "构造sql语句"
  [args]
  (-> args
      (build-sqlmap)
      (add-logic-delete-where)
      (sql/format)))

(defn- build-simple-count-sqlmap [from-key props]
  (let [sqlmap {:select [[:%count.* :count]]
             :from [from-key]}]
    (apply where (into [sqlmap] (mp->vp props)))))

(defn- build-count-sqlmap [args]
  (let [first-arg (first args)]
    (if (keyword? first-arg)
      (build-simple-count-sqlmap first-arg (second args))
      (assoc first-arg :select [[:%count.* :count]]))))

(defn build-count-sql
  "构造count sql语句"
  [args]
  (-> args
      (build-count-sqlmap)
      (add-logic-delete-where)
      (sql/format)))

(defn build-insert-row
  "构造insert插入的记录"
  [row]
  (if (config/logic-delete?)
    (merge row (config/get-logic-delete-insert))
    row))
