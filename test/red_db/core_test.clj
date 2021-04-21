(ns red-db.core-test
  (:require
   [red-db.build-sql :as build]
   [next.jdbc :as jdbc]
   [mount.core :as mount]
   [migratus.core :as migratus]
   [clojure.test :refer :all]
   [red-db.core :as red-db]
   [red-db.helper :refer :all]))

(def config {:store                :database
             :migration-dir        "migrations/"
             :init-script          "init.sql"
             :db {:connection-uri
                  "jdbc:p6spy:mysql://localhost:3306/red_db?user=root&password=root&useSSL=false&autoReconnect=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Hongkong"
                  ;;"jdbc:h2:./demo"
                  }})

(defn create-migratus
  [file-name]
  (migratus/create config file-name))

(defn migrate []
  (migratus/migrate config))

(defn reset []
  (migratus/reset config))

(defn start []
  (mount/start))

(defn stop []
  (mount/stop))

(red-db/bind-connection "sql/user.sql")

(-> (select :name :age)
    (from :user)
    (pagination 0 10)
    (where (AND (eq false :age 10)
                (like false :name "tom")
                (OR (eq :age 10)
                    (like :name "tom"))))
    (red-db/get-page))

(-> 
    (from :user)
    (limit 2)
    (red-db/get-count))

(defn test-transaction []
  (red-db/with-transaction
    (red-db/insert! :user {:name "tom" :age 10 :email "18354@qq.com"})
    (throw (Exception. "error"))))

(defn test-transaction2 []
  (red-db/with-transaction
    (insert-user! {:name "tom" :age 10 :email "18354@qq.com" :delete_flag 0})
    (throw (Exception. "error"))))

(deftest test-insert
  (testing "插入单条记录"
    (let [row (red-db/insert! :user {:name "tom" :age 10 :email "18354@qq.com"})]
      (is (not (nil? row)))))
  (testing "插入多条记录"
    (let [rows (red-db/insert-multi! :user [{:name "tom" :age 10 :email "18354@qq.com"}
                                     {:name "jerry" :age 11 :email "18354@qq.com"}])]
      (is (= 2 (count rows))))))

#_(deftest test-get-one
  (testing "查询单条记录(简写形式)"
    (let [row (red-db/get-one :user {:age 11 :name (like "jerry")})]
      (is (= (:name row) "jerry"))))
  (testing "查询单条记录(sqlmap形式)"
    (let [sqlmap {:select [:id :name]
                  :from [:user]
                  :where [:and [:= :age 11] (like :name "jer")]}
          row (red-db/get-one sqlmap)]
      (is (= (:name row) "jerry"))))
  (testing "查询单条记录(Vanilla形式)"
    (let [row (-> (select :*)
                  (from :user)
                  (where [:= :age 11] [:= :name "jerry"])
                  (red-db/get-one))]
      (is (= (:name row) "jerry")))))

#_(deftest test-get-one
  (testing "查询多条记录"
    (let [rows (red-db/get-list :user {:age 11 :name "jerry"})]
      (is (pos? (count rows)))))
  (testing "查询多条记录"
    (let [rows (-> (select :*)
                  (from :user)
                  (where [:= :age 11] [:= :name "jerry"])
                  (red-db/get-list))]
      (is (pos? (count rows)))))
  (testing "查询多条记录"
    (let [sqlmap {:select [:id :name]
                  :from [:user]
                  :where [:and [:= :age 11] [:= :name "jerry"]]}
          rows (red-db/get-list sqlmap)]
      (is (pos? (count rows))))))
