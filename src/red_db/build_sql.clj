(ns red-db.build-sql
  (:require
   [honeysql.core :as sql]
   [red-db.config :as config]
   [clojure.set :refer [rename-keys]]
   [honeysql.helpers :as helper]))

(defn- add-select-* [sqlmap]
  (if (:select sqlmap)
    sqlmap
    (assoc sqlmap :select [:*])))

(defn- dissoc-empty-where [sqlmap]
  (if (seq (:where sqlmap))
    sqlmap
    (dissoc sqlmap :where)))

(defn format-sql [sqlmap]
  (-> sqlmap (sql/format)))

(defn add-logic-delete-where [sqlmap opt]
  (if (and (not (config/logic-delete-exclude-tables? (:from sqlmap)))
           (config/logic-delete? opt))
    (->> (:from sqlmap)
         (map config/get-logic-delete-where)
         (reduce helper/merge-where sqlmap))
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
    (apply helper/where (into [sqlmap] (mp->vp props)))))

(defn- build-sqlmap [args]
  (let [first-arg (first args)]
    (if (keyword? first-arg)
      (build-simple-sqlmap first-arg (second args))
      first-arg)))

(defn build-sql
  "构造查询sql语句"
  [args]
  (-> args
      (build-sqlmap)
      (add-logic-delete-where (last args))
      (add-select-*)
      (dissoc-empty-where)
      (format-sql)))

(defn build-page-sql
  "构造分页查询sql语句"
  [sqlmap opt]
  (-> sqlmap
      (add-logic-delete-where opt)
      (add-select-*)
      (dissoc-empty-where)
      (format-sql)))

(defn- build-simple-count-sqlmap [from-key props]
  (let [sqlmap {:select [[:%count.* :count]]
             :from [from-key]}]
    (apply helper/where (into [sqlmap] (mp->vp props)))))

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
      (add-logic-delete-where (last args))
      (dissoc-empty-where)
      (format-sql)))

(defn- build-simple-delete-sqlmap [k props]
  (let [sqlmap (helper/delete-from k)]
    (apply helper/where (into [sqlmap] (mp->vp props)))))

(defn- build-delete-sqlmap
  [args]
  (let [first-arg (first args)]
    (if (keyword? first-arg)
      (build-simple-delete-sqlmap first-arg (second args))
      first-arg)))

(defn build-physics-deldete-sql
  "构造物理删除sql语句"
  [args]
  (-> args
      (build-delete-sqlmap)
      (dissoc-empty-where)
      (format-sql)))

(defn- build-simple-update-sqlmap [k props]
  (let [sqlmap {:update k}]
    (apply helper/where (into [sqlmap] (mp->vp props)))))

(defn- build-logic-delete-sqlmap
  [args]
  (let [first-arg (first args)]
    (if (keyword? first-arg)
      (build-simple-update-sqlmap first-arg (second args))
      (rename-keys first-arg {:delete-from :update}))))

(defn set-logic-delete [sqlmap]
  (if (:update sqlmap)
    (assoc sqlmap :set (config/get-logic-delete))
    sqlmap))

(defn build-logic-delete-sql
  "构造逻辑删除sql语句"
  [args]
  (-> args
      (build-logic-delete-sqlmap)
      (set-logic-delete)
      (dissoc-empty-where)
      (format-sql)))

(defn- logic-delete?
  [args]
  (let [first-arg (first args)]
    (if (keyword? first-arg)
      (and (not (config/logic-delete-exclude-tables? [first-arg]))
           (config/logic-delete? (last args)))
      (and (not (config/logic-delete-exclude-tables? (:delete-from first-arg)))
           (config/logic-delete? (last args))))))

(defn build-deldete-sql
  [args]
  (if (logic-delete? args)
    (build-logic-delete-sql args)
    (build-physics-deldete-sql args)))

(defn build-update-sql
  "构造更新sql语句"
  [sqlmap opt]
  (-> sqlmap
      (add-logic-delete-where opt)
      (dissoc-empty-where)
      (format-sql)))

(defn build-insert-row
  "构造insert插入的记录"
  [table row opt]
  (if (and (not (config/logic-delete-exclude-tables? [table]))
           (config/logic-delete? opt))
    (merge row (config/get-logic-delete-insert))
    row))
