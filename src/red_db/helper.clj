(ns red-db.helper
  (:require
   [honeysql.core :as sql]
   [honeysql.helpers :as helpers]))

(def select helpers/select)
(def update-table helpers/update)
(def delete-from helpers/delete-from)
(def delete helpers/delete)
(def sset helpers/sset)
(def from helpers/from)
(def where helpers/where)
(def order-by helpers/order-by)
(def group helpers/group)
(def limit helpers/limit)
(def offset helpers/offset)
(def modifiers helpers/modifiers)
(def join helpers/join)
(def left-join helpers/left-join)
(def right-join helpers/right-join)
(def having helpers/having)

(defn pagination
  [sqlmap page size]
  (sql/build sqlmap :offset (* page size)  :limit size))

(defn eq
  ([v]
   {:= v})
  ([k v]
   [:= k v])
  ([b k v]
   (when b (eq k v))))

(defn ne
  ([v]
   {:<> v})
  ([k v]
   [:<> k v])
  ([b k v]
   (when b (ne k v))))

(defn gt
  "大于 >"
  ([v]
   {:> v})
  ([k v]
   [:> k v])
  ([b k v]
   (when b (gt k v))))

(defn ge
  "大于等于 >="
  ([v]
   {:>= v})
  ([k v]
   [:>= k v])
  ([b k v]
   (when b (ge k v))))

(defn lt
  "小于 <"
  ([v]
   {:< v})
  ([k v]
   [:< k v])
  ([b k v]
   (when b (lt k v))))

(defn le
  "小于等于 <="
  ([v]
   {:<= v})
  ([k v]
   [:<= k v])
  ([b k v]
   (when b (le k v))))

(defn between
  "BETWEEN 值1 AND 值2"
  ([v1 v2]
   {:between [v1 v2]})
  ([k v1 v2]
   [:between k v1 v2])
  ([b k v1 v2]
   (when b (between k v1 v2))))

(defn like
  "LIKE '%值%'"
  ([v]
   {:like (str "%" v "%")})
  ([k v]
   [:like k (str "%" v "%")])
  ([b k v]
   (when b (like k v))))

(defn like-left
  "LIKE '%值'"
  ([v]
   {:like (str "%" v)})
  ([k v]
   [:like k (str "%" v)])
  ([b k v]
   (when b (like-left k v))))

(defn like-right
  "LIKE '值%'"
  ([v]
   {:like (str v "%")})
  ([k v]
   [:like k (str v "%")])
  ([b k v]
   (when b (like-right k v))))

(defn is-null
  "字段 IS NULL"
  ([]
   {:= nil})
  ([k]
   [:= k nil])
  ([b k]
   (when b (is-null k))))

(defn is-not-null
  "字段 IS NULL"
  ([]
   {:not= nil})
  ([k]
   [:not= k nil])
  ([b k]
   (when b (is-not-null k))))

(defn in
  "小于等于 <="
  ([v]
   {:in v})
  ([k v]
   [:in k v])
  ([b k v]
   (when b (in k v))))

(defn- valid-args [args]
  (filter #(not (nil? %)) args))

(defn- get-where-args [args]
  (let [first-arg (first args)]
    (if (boolean? (first args))
      (when first-arg (valid-args (rest args)))
      (valid-args args))))

(defn OR
  "拼接 OR"
  [& args]
  (let [vargs (get-where-args args)]
    (when (seq vargs) (into [:or] vargs))))

(defn AND
  "拼接 AND"
  [& args]
  (let [vargs (get-where-args args)]
    (when (seq vargs) (into [:and] vargs))))

(defn- to-asc-vals [ks]
  (mapv (fn [k] [k :asc]) ks))

(defn order-by-asc
  "升序排序"
  [sqlmap & args]
  (apply helpers/order-by (into [sqlmap] (to-asc-vals args))))

(defn- to-desc-vals [ks]
  (mapv (fn [k] [k :desc]) ks))

(defn order-by-desc
  "降序排序"
  [sqlmap & args]
  (apply helpers/order-by (into [sqlmap] (to-desc-vals args))))
