# red-db

青岛红创众投数据库操作

## 安装

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.redcreation/red-db.svg)](https://clojars.org/org.clojars.redcreation/red-db)

## 使用

### 插入一条记录
```clojure
(require [red-db.core :as red-db])

(red-db/insert! :user {:name "tom" :age 18 :email "569721086@qq.com"})

;;=> INSERT INTO user (name, age, email) VALUES ('tom', 10, '569721086@qq.com') 
```

#### 返回结果
返回添加的主键值

### 插入多条记录
```clojure
(require [red-db.core :as red-db])

(red-db/insert-multi! :user 
	[{:name "tom" :age 18 :email "18354@qq.com"}
     {:name "jerry" :age 20 :email "18354@qq.com"}])
	 
;;=> INSERT INTO user (name, age, email) VALUES ('tom', 18, '18354@qq.com'), ('jerry', 20, '18354@qq.com')
```

#### 返回结果
返回插入数据主键值数组

### 更新记录
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (update-table :user) 
	(sset {:name "tom666"}) 
	(where (eq :id 193)) 
	(red-db/update!))
	
;;=> UPDATE user SET name = 'tom666' WHERE id = 193
```

#### 返回结果
返回更新的记录个数

### 删除记录
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (delete-from :user) 
	(where (eq :id 227)) 
	(red-db/delete!))
	
;;=> DELETE FROM user WHERE id = 227
```
#### 返回结果
返回删除的记录个数

### 查询单条记录
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

;; 简单的形式，只满足where条件只有AND的情况
(red-db/get-one :user {:id 226 :age (lt 20)})
;;=> SELECT * FROM user WHERE (id = 226 AND age < 20)

;; 完整的形式
(-> (select :*) 
	(from :user) 
	(where (AND (eq :id 226) 
		        (OR (eq :age 20) 
					(like :name "tom"))))
	(red-db/get-one))

;;=> sql语句: SELECT * FROM user WHERE id = 226 AND (age = 20 OR name like '%tom%')
```

### 查询多条记录
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

;; 简单的形式，只满足where条件只有AND的情况
(red-db/get-list :user {:id 226 :age (lt 20)})
;;=> SELECT * FROM user WHERE (id = 226 AND age < 20)

;; 完整的形式
(-> (select :*) 
	(from :user) 
	(where (AND (eq :id 226) 
		        (OR (eq :age 20) 
					(like :name "tom"))))
	(red-db/get-list))

;;=> sql语句: SELECT * FROM user WHERE id = 226 AND (age = 20 OR name like '%tom%')
```

### 查询分页记录
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (select :*) 
	(from :user)
	(where (like :name "tom"))
	(pagination 0 10)
	(red-db/get-page))
```

#### 返回数据格式
```clojure
{:rows
 [{:id 193,
   :name "tom666",
   :age 10,
   :email "18354@qq.com",
   :delete-flag 0}
  {:id 194,
   :name "tom",
   :age 10,
   :email "18354@qq.com",
   :delete-flag 0}],
 :page 0,
 :size 10,
 :total-count 5,
 :total-page 1}
```


### 事务操作
```clojure
(require [red-db.core :as red-db])

(red-db/with-transaction
    (red-db/insert! :user {:name "tom" :age 10 :email "18354@qq.com"})
    (red-db/insert! :user {:name "tom" :age 10 :email "18354@qq.com"}))
```

### 条件构造

#### OR
#### AND
#### eq
#### ne
#### gt
#### ge
#### lt
#### le
#### between
#### like
#### like-left
#### like-right
#### is-null
#### is-not-null
#### in
#### order-by-asc
#### order-by-desc

## 配置

### 数据源配置
```clojure
{:datasource
 {:driver-class-name "com.p6spy.engine.spy.P6SpyDriver"
  :jdbc-url     "jdbc:p6spy:h2:./demo"}}
```
### 逻辑删除配置
```clojure
{:red-db
 {:logic-delete? true
  :logic-delete-field :delete_flag
  :logic-delete-value 1
  :logic-not-delete-value 0}}
```
