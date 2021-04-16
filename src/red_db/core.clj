(ns red-db.core
  (:refer-clojure :exclude [update])
  (:require
   [next.jdbc :as jdbc]
   [camel-snake-kebab.core :as csk]
   [next.jdbc.result-set :as rs]
   [red-db.build-sql :as build]
   [honeysql.helpers :as help]))

(defn- jdbc-execute! [sql ds]
  (jdbc/execute! ds sql))

(defn- jdbc-execute-one! [sql ds]
  (jdbc/execute-one! ds sql))

(defn execute!
  "执行honeysql sqlmap"
  [sqlmap & opt]
  (let [ds (build/get-datasource opt)]
    (-> sqlmap
        (build/add-logic-delete-where)
        (build/format-sql)
        (jdbc-execute! ds))))

(defn insert!
  "插入记录"
  [table row & opt]
  (let [ds (build/get-datasource opt)]
    (-> (help/insert-into table)
        (help/values [(build/build-insert-row row)])
        (build/format-sql)
        (jdbc-execute-one! ds))))

(defn insert-multi!
  "插入多条记录"
  [table rows & opt]
  (let [ds (build/get-datasource opt)]
    (-> (help/insert-into table)
        (help/values (map build/build-insert-row rows))
        (build/format-sql)
        (jdbc-execute! ds))))

(defn update!
  "更新记录"
  [sqlmap & opt]
  (let [ds (build/get-datasource opt)]
    (-> sqlmap
        (build/build-update-sql)
        (jdbc-execute! ds))))

(defn delete!
  "删除记录"
  [& args]
  (let [ds (build/get-datasource (last args))
        sql (build/build-deldete-sql args)]
    (-> (jdbc/execute-one! ds sql)
        :next.jdbc/update-count)))

(defn get-one
  "查询单条记录"
  [& args]
  (let [ds (build/get-datasource (last args))
        sql (build/build-sql args)]
    (jdbc-execute-one! sql ds)))

(defn get-list
  "查询多条记录"
  [& args]
  (let [ds (build/get-datasource (last args))
        sql (build/build-sql args)]
    (jdbc-execute! sql ds)))

(defn get-count
  "查询总数"
  [& args]
  (let [ds (build/get-datasource (last args))
        sql (build/build-count-sql args)]
    (:count (jdbc-execute-one! sql ds))))

(defn get-page
  "查询分页记录"
  [sqlmap & opt]
  (let [ds (build/get-datasource opt)
        sql (build/build-page-sql sqlmap)]
    {:rows (jdbc-execute! sql ds)
     :count (get-count (dissoc sqlmap :limit :offset))}))
