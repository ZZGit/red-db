(ns red-db.cond)

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
