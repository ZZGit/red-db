(ns red-db.helper
  (:require
   [honeysql.helpers :as helpers]))

(defn like
  ([v]
   {:like (str "%" v "%")})
  ([k v]
   [:like k (str "%" v "%")]))

(defn like-left
  ([v]
   {:like (str "%" v)})
  ([k v]
   [:like k (str "%" v)]))

(defn like-right
  ([v]
   {:like (str v "%")})
  ([k v]
   [:like k (str v "%")]))

(defn gt
  ([v]
   {:> v})
  ([k v]
   [:> k v]))
