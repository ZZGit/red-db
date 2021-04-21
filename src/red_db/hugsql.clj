(ns red-db.hugsql)

(defn- operator [c k p o]
  (when (k p) (str " AND " c o k)))

(defn eq [c k p]
  (operator c k p "="))

(defn ne [c k p]
  (operator c k p "<>"))

(defn gt [c k p]
  (operator c k p ">"))

(defn ge [c k p]
  (operator c k p ">="))

(defn lt [c k p]
  (operator c k p "<"))

(defn le [c k p]
  (operator c k p "<="))

(defn like [c k p]
  (let [v (k p)]
    (when v (str " AND " c " LIKE '%" v "%'"))))

(defn like-left [c k p]
  (let [v (k p)]
    (when v (str " AND " c " LIKE '%" v))))

(defn like-right [c k p]
  (let [v (k p)]
    (when v (str " AND " c " LIKE " v "%'"))))
