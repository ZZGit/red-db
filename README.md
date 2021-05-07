# red-db 
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.redcreation/red-db.svg)](https://clojars.org/org.clojars.redcreation/red-db)

## 动机
青岛红创众投科技选择了clojure作为公司的主要技术栈。数据库操作一直使用的是
[hugsql](https://www.hugsql.org/), 但是在日常使用中发现，hugsql使用起来并不是非常的方便，尤其是单表的操作，增加了复杂度。于是发现了[honeysql](https://github.com/seancorfield/honeysql), 它以简单的map结构代表sql语句，以及提供了一些辅助函数，非常适合简单的语句操作。所以就结合hugsql和honeysql开发了red-db。

### 特性
* 简单的单表操作不需要再写语句
* 更友好的分页查询
* 逻辑删除全局配置，不需要再显式的指定

## 快速开始

通过一个简单的Demo,来介绍red-db的功能。

首页我们创建一张用户表，数据库 Schema 脚本如下:
```sql
DROP TABLE IF EXISTS user;

CREATE TABLE user
(
	id BIGINT(20) NOT NULL COMMENT '主键ID',
	user_name VARCHAR(30) NULL DEFAULT NULL COMMENT '姓名',
	user_age INT(11) NULL DEFAULT NULL COMMENT '年龄',
	user_email VARCHAR(50) NULL DEFAULT NULL COMMENT '邮箱',
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

我们需要在上面那两个namespace中,把red-db依赖进来，比如
```clojure
(ns custombackend.user
  (:require
   [red-db.core]
   ...
   ))
```

```clojure
(ns custombackend.core
  (:require
   [red-db.core]
   ...
   ))
```

### 开始使用

#### 插入单条记录
我们先从最简单的插入单条数据开始

```clojure
(require [red-db.core :as red-db])

(red-db/insert!
 :user
 {:id 1 :user_name "刘一" :user_age 11 :user_email "liuyi@qq.com"})
```
在上面的操作中我们插入了一条记录，返回的结果在不同的数据库会有所不同。

##### H2数据库返回结果
```clojure
{:ID 1}
```

##### Mysql数据库返回结果
```clojrue
{:GENERATED_KEY 1}
```

在上面添加的数据中
```clojure
{:id 1 :user_name "刘一" :user_age 11 :user_email "liuyi@qq.com"}
```
我们是以下snake_case作为为key的命名方式，这样和数据库的命名方式是一样的。如果你更偏向于clojure的kebab-case的命名方式，你也可以使用这种命名方式。
```clojure
(require [red-db.core :as red-db])

(red-db/insert!
 :user
 {:id 2 :user-name "陈二" :user-age 12 :user-email "chener@qq.com"})
```

#### 插入多条记录

```clojure
(require [red-db.core :as red-db])

(red-db/insert-multi!
 :user
 [{:id 3 :user_name "张三" :user_age 13 :user_email "zhangsan@qq.com"}
  {:id 4 :user_name "李四" :user_age 14 :user_email "lisi@qq.com"}])
```

### 查询

查询总共分为两步
1. 通过[honeysql](https://github.com/seancorfield/honeysql)构建sqlmap
2. 将sqlmap作为参数传入给red-db提供的查询方法，比如get-one、get-list、get-page等

下面我们看一例子：

#### 查询一条数据
首先我们要构建sqlmap,我们可以直接构建，也可以通过辅助函数来构建

直接构建sqlmap
```clojure

(def sqlmap {:select [:*]
             :from   [:user]
             :where  [:= :user_name "张三"]})
```
上面的sqlmap等价sql语句
```sql
select * from user where user_name = "张三"
```

通过辅助函数来构建sqlmap
辅助函数都放在red-db.helper这个命名空间下
```clojure
(require [red-db.helper :refer :all])

(-> (select :*)
    (from :user)
    (where (eq :user_name "张三")))
```
推荐使用这种方式来创建sqlmap,辅助函数会提供一些便捷的操作，后续我们会讲到。

构建好sqlmap之后，我们就可以用它来查询了
```clojure
(require [red-db.core :as red-db])

(def sqlmap {:select [:*]
             :from   [:user]
             :where  [:= :user_name "张三"]})
			 
(red-db/get-one sqlmap)
```

```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (select :*)
    (from :user)
    (where (eq :user_name "张三"))
	(red-db/get-one))
```

上面两个操作都是等价的，返回结果
```clojure
{:id 3, :user_name "张三", :user_age 13, :user_email "zhangsan@qq.com"}
```

除此之外，red-db还提供了更简单的查询操作

```clojure
(require [red-db.core :as red-db])

(red-db/get-one :user {:user_name "张三"})
```
辅助函数同样可以应用到这种形式的查询
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

;;; 等价于 (red-db/get-one :user {:user_name "张三"}) 
(red-db/get-one :user {:user_name (eq "张三")})

;; 相当于 select * from user where user_email like "%@qq.com% and user_age < 14"
(red-db/get-one :user {:user_email (like "@qq.com")
	                   :user_age (lt 14})
```

不过这种简单的形式只支持where条只有AND的情况，更复杂的where条件还是通过前面两种形式来构建。

#### 查询多条数据
查询多条数据和查询单条数据的步骤是相同的，同样支持上面三种形式。只不过返回的结果变为多条
```clojure
(require [red-db.core :as red-db])

;; 形式一
(def sqlmap {:select [:*]
             :from   [:user]
             :where  [:and [:like :user_email "%@qq.com%"]
			               [:< :user_age 13]})

(red-db/get-list sqlmap)

;; 形式二
(-> (select :*)
    (from :user)
	(where (AND (like :user_email "@qq.com")
	            (lt :user_age 13)))
    (red-db/get-list))


;; 形式三
(red-db/get-list :user {:user_email (like "@qq.com")
	                    :user_age (lt 13)})

```
返回数据
```clojure
[{:id 1, :user_name "刘一", :user_age 11, :user_email "liuyi@qq.com"}
 {:id 2, :user_name "陈二", :user_age 12, :user_email "chener@qq.com"}]
```

#### 查询分页记录
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (select :*) 
	(from :user)
	(where (AND (like :user_email "@qq.com")
	            (lt :user_age 13)))
	(pagination 0 10)
	(red-db/get-page))
```

返回数据
```clojure
{:rows   ;;查询记录数据
 [{:id 1, :user_name "刘一", :user_age 11, :user_email "liuyi@qq.com"}
  {:id 2, :user_name "陈二", :user_age 12, :user_email "chener@qq.com"}],
 :page 0,        ;; 当前分页
 :size 10,       ;; 每页的数量
 :total-count 2, ;; 总条数
 :total-page 1   ;; 总页数
 }
```

### 动态的构建查询条件

通过辅助函数我们可以很友好的动态的构建where条件
```clojure

``````clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(def age 12)

(defn valid-age? [age]
   (not (nil? age))

(-> (select :*)
    (from :user)
    (where (eq (valid-age? age)  :user_age age))
	(red-db/get-list))
```
上面操作最终会执行
```sql
select * from user where user_age=12
```
如果我们把age变成nil
```clojure
(def age nil)
```
最终执行的语句就变成了
```sql
select * from user
```


### 更新记录
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (update-table :user) 
	(sset {:name "tom666"}) 
	(where (eq :id 1)) 
	(red-db/update!))
```

### 删除记录
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (delete-from :user) 
	(where (eq :id 227)) 
	(red-db/delete!))
	
;;=> DELETE FROM user WHERE id = 227
```


### 事务操作
```clojure
(require [red-db.core :as red-db])

(red-db/with-transaction
    (red-db/insert! :user {:name "tom" :age 10 :email "18354@qq.com"})
    (red-db/insert! :user {:name "tom" :age 10 :email "18354@qq.com"}))
```

### 辅助函数

#### OR
```clojure
(-> (select :*) 
	(from :user)
	(where (OR (like :user_email "qq")
	            (eq :user_age 13)))
```
等价于
```sql
select * from user where user_email like '%qq%' or user_age=13
```
#### AND
```clojure
(-> (select :*) 
	(from :user)
	(where (AND (like :user_email "qq")
	            (eq :user_age 13)))
```
等价于
```sql
select * from user where user_email like '%qq%' and user_age=13
```
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
