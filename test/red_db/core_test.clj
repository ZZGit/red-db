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

;;(red-db/bind-connection "sql/user.sql")


(-> (select :*)
    (from :user)
    (where (eq :user_name "张三")))

(defn test-transaction []
  (red-db/with-transaction
    (red-db/insert!
     :user
     {:user_name "tom" :user_age 10 :user_email "18354@qq.com"})
    (throw (Exception. "error"))))

(deftest test-insert
  (testing "插入单条记录"
    (let [row (red-db/insert!
               :user
               {:user_name "tom" :user_age 10 :user_email "18354@qq.com"})]
      (is (not (nil? row)))))
  (testing "插入多条记录"
    (let [rows (red-db/insert-multi!
                :user
                [{:id 3 :user_name "tom" :user_age 10 :user_email "18354@qq.com"}
                 {:id 4 :user_name "jerry" :user_age 15 :user_email "18354@qq.com"}])]
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
