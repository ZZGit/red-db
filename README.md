# red-db [![Clojars Project](https://img.shields.io/clojars/v/org.clojars.redcreation/red-db.svg)](https://clojars.org/org.clojars.redcreation/red-db)

## 背景
青岛红创众投科技选择了clojure作为公司的主要技术栈。数据库操作一直使用的是
[hugsql](https://www.hugsql.org/), 但是在日常使用中发现，hugsql使用起来并不是非常的方便，尤其是单表的操作，增加了复杂度。于是发现了[honeysql](https://github.com/seancorfield/honeysql), 它以简单的map结构代表sql语句，以及提供了一些辅助函数，非常适合简单的语句操作。所以就结合hugsql和honeysql开发了red-db。

### 特性
* 简单单表操作不需要再写语句
* 更友好的分页查询
* 逻辑删除全局配置，不需要再显示的指定

## 快速开始

通过一个简单的Demo,来介绍red-db的功能。

首页我们创建一张用户表，数据库 Schema 脚本如下:
```sql
DROP TABLE IF EXISTS user;

CREATE TABLE user
(
	id BIGINT(20) NOT NULL COMMENT '主键ID',
	name VARCHAR(30) NULL DEFAULT NULL COMMENT '姓名',
	age INT(11) NULL DEFAULT NULL COMMENT '年龄',
	email VARCHAR(50) NULL DEFAULT NULL COMMENT '邮箱',
	PRIMARY KEY (id)
);
```

### 添加依赖
在project.clj中添加red-db依赖
```clojure
[org.clojars.redcreation/red-db "0.1.0-SNAPSHOT"]
```
当前red-db还在试用阶段，可能会存在bug，为了快速修改bug，而不必修改版本号
，所以目前使用的是SNAPSHOT版本。

### 配置

#### 配置数据源
在config.edn中配置数据源，详情参数请查看[hikari-cp](https://github.com/tomekw/hikari-cp)
```clojure
{:datasource
 {:driver-class-name "com.p6spy.engine.spy.P6SpyDriver"
  :jdbc-url     "jdbc:p6spy:h2:./demo"}}
```
为了兼容原有的项目，下面的配置也是可以的
```clojure
{:database-url "jdbc:p6spy:mysql://localhost:3306/red_db?user=root&password=root"}
```

red-db自动读取config.edn配置文件，取配置文件中:datasource或:database-url的值作为数据源的配置

#### 在初始的namspace中依赖red-db.core
截取了project.clj中的代码片段
```clojrue
(defproject project-name "0.1.0-SNAPSHOT"
  :dependencies []
  ...
  :main ^:skip-aot custombackend.core
  :profiles
  {:dev {:repl-options {:init-ns custombackend.user}
         ...}})
```
上面配置中`custombackend.user`是作为cider启动后默认进入的namespace. 而`custombackend.core` 是main函数的namespace。我们通过java -jar 运行jar包就会进入这个namespace.

最后我们在上面那两个namespace中,把red-db依赖进来，比如
```clojure
(ns custombackend.user
  (:require
   [red-db.ds]
   ...
   ))
```

```clojure
(ns custombackend.core
  (:require
   [red-db.ds]
   ...
   ))
```

### 开始使用

首先我们先插入多条数据
```clojure
(require [red-db.core :as red-db])

(red-db/insert-multi!
	:user 
	[
		{:id 1 :name "tom1" :age 11 :email "569721086@qq.com"}
		{:id 2 :name "tom2" :age 12 :email "569721086@qq.com"}
		{:id 3 :name "tom3" :age 13 :email "569721086@qq.com"}
		{:id 4 :name "tom4" :age 14 :email "569721086@qq.com"}
		{:id 5 :name "tom5" :age 15 :email "569721086@qq.com"}
	])
```

然后我们把它全部查出来
```clojure
(require [red-db.core :as red-db])

(def users (red-db/get-list :user))

(prn users)
```

打印结果
```
[
	{:id 1 :name "tom1" :age 11 :email "569721086@qq.com"}
	{:id 2 :name "tom2" :age 12 :email "569721086@qq.com"}
	{:id 3 :name "tom3" :age 13 :email "569721086@qq.com"}
	{:id 4 :name "tom4" :age 14 :email "569721086@qq.com"}
	{:id 5 :name "tom5" :age 15 :email "569721086@qq.com"}
]
```

按照id查询一条记录
```clojure
(require [red-db.core :as red-db])

(def user (red-db/get-one :user {:id 1))

(prn user)
```

打印结果
```
{:id 1 :name "tom1" :age 11 :email "569721086@qq.com"}
```


## 使用

### 插入一条记录
```clojure
(require [red-db.core :as red-db])

(red-db/insert! 
	:user 
	{:id 1 :name "tom" :age 18 :email "569721086@qq.com"})
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
