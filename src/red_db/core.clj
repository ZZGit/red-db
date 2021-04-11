(ns red-db.core
  (:refer-clojure :exclude [update])
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [red-db.util :as util]
   [honeysql.core :as sql]
   [honeysql.helpers :as help]))

(Class/forName "com.p6spy.engine.spy.P6SpyDriver")

(defn- execute! [sql ds]
  (jdbc/execute!
   ds sql {:return-keys true
           :builder-fn rs/as-unqualified-lower-maps}))

(defn- execute-one! [sql ds]
  (jdbc/execute-one!
   ds sql {:return-keys true
           :builder-fn rs/as-unqualified-lower-maps}))

(defn insert
  "插入记录"
  [table row & opt]
  (let [ds (util/get-datasource opt)]
    (-> (help/insert-into table)
        (help/values [(util/build-insert-row row)])
        (sql/format)
        (execute-one! ds))))

(defn insert-batch
  "插入多条记录"
  [table rows & opt]
  (let [ds (util/get-datasource opt)]
    (-> (help/insert-into table)
        (help/values (map util/build-insert-row rows))
        (sql/format)
        (execute! ds))))

(defn update
  "更新记录"
  [])

(defn delete
  "删除记录"
  [])

(defn get-one
  "查询单条记录"
  [& args]
  (let [ds (util/get-datasource (last args))
        sql (util/build-sql args)]
    (execute-one! sql ds)))

(defn get-list
  "查询多条记录"
  [& args]
  (let [ds (util/get-datasource (last args))
        sql (util/build-sql args)]
    (execute! sql ds)))

(defn get-count
  "查询总数"
  [& args]
  (let [ds (util/get-datasource (last args))
        sql (util/build-count-sql args)]
    (:count (execute-one! sql ds))))

(defn get-page
  "查询分页记录"
  [& args]
  (let [ds (util/get-datasource (last args))
        sql (util/build-sql args)]
    (execute! sql ds)))
