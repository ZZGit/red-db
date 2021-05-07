(ns red-db.core
  (:refer-clojure :exclude [update])
  (:require
   [red-db.ds :as ds]
   [next.jdbc :as jdbc]
   [red-db.config :as config]
   [conman.core :as conman]
   [next.jdbc.result-set :as rs]
   [red-db.build-sql :as build]
   [honeysql.helpers :as help]))

(defmacro create-exec-opt
  []
  (let [builder (config/get-result-set-builder)]
    `(def ^:private exec-opt
       {:return-keys true
        :builder-fn (or ~builder rs/as-unqualified-maps)})))

(create-exec-opt)

(defn- jdbc-execute! [sql ds]
  (jdbc/execute! ds sql exec-opt))

(defn- jdbc-execute-one! [sql ds]
  (jdbc/execute-one! ds sql exec-opt))

(defn execute!
  "执行honeysql sqlmap"
  [sqlmap & opt]
  (let [ds (ds/get-datasource opt)]
    (-> sqlmap
        (build/add-logic-delete-where)
        (build/format-sql)
        (jdbc-execute! ds))))

(defn insert!
  "插入记录"
  [table row & opt]
  (let [ds (ds/get-datasource (first opt))]
    (-> (help/insert-into table)
        (help/values [(build/build-insert-row row)])
        (build/format-sql)
        (jdbc-execute-one! ds))))

(defn insert-multi!
  "插入多条记录"
  [table rows & opt]
  (let [ds (ds/get-datasource (first opt))]
    (-> (help/insert-into table)
        (help/values (map build/build-insert-row rows))
        (build/format-sql)
        (jdbc-execute! ds))))

(defn update!
  "更新记录"
  [sqlmap & opt]
  (let [ds (ds/get-datasource opt)]
    (-> sqlmap
        (build/build-update-sql)
        (jdbc-execute! ds))))

(defn delete!
  "删除记录"
  [& args]
  (let [ds (ds/get-datasource (last args))
        sql (build/build-deldete-sql args)]
    (-> (jdbc/execute-one! ds sql)
        :next.jdbc/update-count)))

(defn get-one
  "查询单条记录"
  [& args]
  (let [ds (ds/get-datasource (last args))
        sql (build/build-sql args)]
    (jdbc-execute-one! sql ds)))

(defn get-list
  "查询多条记录"
  [& args]
  (let [ds (ds/get-datasource (last args))
        sql (build/build-sql args)]
    (jdbc-execute! sql ds)))

(defn get-count
  "查询总数"
  [& args]
  (let [ds (ds/get-datasource (last args))
        sql (build/build-count-sql args)]
    (:count (jdbc-execute-one! sql ds))))

(defn- get-total-page
  [count size]
  (if (or (zero? count)
          (zero? size))
    0
    (int (Math/ceil (/ count size)))))

(defn get-page
  "查询分页记录"
  [sqlmap & opt]
  (let [ds (ds/get-datasource opt)
        sql (build/build-page-sql sqlmap)
        size (:limit sqlmap)
        count (get-count (dissoc sqlmap :limit :offset :order-by :group-by))
        rows (if (zero? count) [] (jdbc-execute! sql ds))]
    {:rows rows
     :page (int (/ (:offset sqlmap) size))
     :size size
     :total-count count 
     :total-page (get-total-page count size)}))

(defmacro with-transaction
  "数据库事务"
  [& args]
  (let [first-arg (first args)]
    (if (vector? first-arg)
      (let [[dbsym & opts] first-arg
            body (rest args)]
        `(next.jdbc/with-transaction [tx# ~dbsym ~@opts]
           (binding [red-db.ds/*t-ds* tx#]
             ~@body)))
      `(next.jdbc/with-transaction [tx# (red-db.ds/get-datasource nil)]
         (binding [red-db.ds/*t-ds* tx#]
           ~@args)))))

(defmacro bind-connection [& args]
  (let [first-arg (first args)
        rest-args (rest args)]
    (if (string? first-arg)
      `(conman.core/bind-connection (ds/get-datasource) ~@args)
      `(conman.core/bind-connection ~first-arg ~@rest-args))))
