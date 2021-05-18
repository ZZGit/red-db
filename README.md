# red-db 
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.redcreation/red-db.svg)](https://clojars.org/org.clojars.redcreation/red-db)

## 动机
青岛红创众投科技选择了clojure作为公司的主要技术栈。数据库操作使用的是
[hugsql](https://www.hugsql.org/), hugsql比较灵活，它会有把sql语句转换成clojure函数。但是使用起来还是比较繁琐，即使简单的单表操作也得写一条sql语句。之前有同事尝试一些改进工作，把hugsql常用的单表操作封装了一下，[点击查看](http://blog.3vyd.com/blog/posts-output/2020-10-10-hugsql-%E9%80%9A%E7%94%A8%E5%A4%84%E7%90%86%E6%96%B9%E6%B3%95/)。但是还是有些不完善的地方，比如分页查询、逻辑删除。当我发现了[honeysql](https://github.com/seancorfield/honeysql), 它以简单的map结构代表sql语句，以及提供了一些辅助函数，非常适合简单的语句操作。所以就结合hugsql和honeysql开发了red-db。

## 安装
在project.clj中添加red-db依赖
```clojure
[org.clojars.redcreation/red-db "0.1.0-SNAPSHOT"]
```

## 配置

### 配置数据源
在config.edn文件中添加配置，详情参数请查看[hikari-cp](https://github.com/tomekw/hikari-cp)
```clojure
{:datasource
 {:driver-class-name "com.p6spy.engine.spy.P6SpyDriver"
  :jdbc-url     "jdbc:p6spy:h2:./demo"}}
```
为了兼容原有的项目，下面的配置也是可以的
```clojure
{:database-url "jdbc:p6spy:mysql://localhost:3306/red_db?user=root&password=root"}
```

### 在入口namspace中引入red-db.core
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

我们需要在上面那两个namespace中,把red-db.core引入一下，比如
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
这一步的原因是由于red-db使用[mount](https://github.com/tolitius/mount)来包装了数据源, 为了使mount生效，这里需要引用namespace。[具体查看mount文档](https://github.com/tolitius/mount#packaging)

## honeysql语法

在进行操作之前我先简单讲解一下honeysql的功能，具体请查看[hoenysql](https://github.com/seancorfield/honeysql)。hoenysql最核心的功能就是把sql语句映射成一个clojure的map结构，作者称之为sqlmap。比如下面例子:
```clojure
(def sqlmap1 {:select [:*]
             :from   [:user]
             :where  [:= :name "张三"]})
			 
(honeysql.core/format sqlmap1)
=> ["SELECT * FROM user WHERE name = ?" "张三"]
		
(def sqlmap2 {:select [:name :age]
             :from   [:user]
             :where  [:> :age 12]})

(honeysql.core/format sqlmap2)
=> ["SELECT name age FROM user WHERE age > ?" 12]
```
可以看到hoenysql一个map最终转化成一条sql语句。

除了手动构造sqlmap之外，honeysql还提供了一些辅助函数来帮助我们更友好的构造sqlmap
```clojure
(-> (select :*)
	(from :user)
	(where [:= :name "张三"])
```
上面的输出的结果如下：
```clojure
{:select [:*]
 :from   [:user]
 :where  [:= :name "张三"]}
```

## 核心功能
honeysql只负责把map转为sql,它不负责语句的执行。所以red-db主要的功能就是执行由honeysql构造的sql语句。red-db提供了下面介绍的函数来执行操作，除此之外red-db还增加一些辅助函数，尤其是简化了where条件构造。详细的会在下面介绍，这里举几个例子
```clojure
;; 所有的辅助函数都放在red-db.helper这个命名空间下
(require [red-db.helper :refer :all])
(require [red-db.core :as red-db])

(-> (select :*)
    (from :user)
	(where (OR (eq :name "张三")
               (like :email "test@qq")))
    (red-db/get-list))
=> select * from user where name = "张三" or email like "%test@qq%"
```

下面介绍一下几个主要的函数

### insert!
插入单条数据
```clojure
(require [red-db.core :as red-db])

(red-db/insert!
 :user
 {:id 2 :name "刘一" :age 11 :email "liuyi@qq.com"})
```
insert!函数不需要构造sqlmap,第一个参数是以keyword代表的表名。第二个参数是需要插入的数据。

insert!函数返回的结果在不同的数据库也会有所不同。比如
* Mysql返回结果
```clojure
{:GENERATED_KEY 1}
```
GENERATED_KEY表示受影响的条数，不是主键值

* Postgresql返回结果
```clojure
{:id 2 :name "刘一" :age 11 :email "liuyi@qq.com"}
```

* H2返回结果
```clojure
{:ID 2}
```

借助insert!函数，这里说明一下关于数据库、参数以及返回结果的命名问题，下面介绍的核心函数也同样遵循。
在数据库中我们一般使用`snake_case`, 以下划线作为连词符号的命名方式。比如
```sql
CREATE TABLE t_user
(
	user_id BIGINT(20) NOT NULL COMMENT '主键ID',
	user_name VARCHAR(30) NULL DEFAULT NULL COMMENT '用户名称',
	PRIMARY KEY (user_id)
);
```
在insert!参数中我们依然可以使用这种命名方式
```clojure
(red-db/insert!
 :t_user
 {:user_id 2 :user_name "刘一"})
```
由于在clojure建议我们的命名方式使用`kebab-case`的形式，以中划线作为连词符号。为了书写统一，也可以使用这种命名方式,它会自动转成数据库以下划线命名的方式。
```clojure
(red-db/insert!
 :t-user
 {:user-id 2 :user-name "刘一"})
```

函数返回结果默认是使用数据库命名方式，比如我们使用下面的要介绍的查询函数
```clojure
(require [red-db.core :as red-db])

(red-db/get-list :t-user)
=> [{:user_id 2, :user_name "刘一"}]
```
返回的结果以下划线作为命名方式，如果要修改返回结构的命名方式，比如我们要改成以`kebab-case`形式。则要在配置文件中统一修改，详细的配置信息后面会说明。
```clojure
{:red-db
 {:result-set-builder next.jdbc.result-set/as-unqualified-kebab-maps}
 }
```
然后在执行函数
```clojure
(require [red-db.core :as red-db])

(red-db/get-list :t-user)
=> [{:user-id 2, :user-name "刘一"}]
```

### update!
更新数据
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (update-table :user)
    (sset {:name "李四"})
	(where (eq :id 1))
	(red-db/update!))
=> update user set name = "李四" where id = 1
```

### delete!
删除数据
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (delete-from :user)
    (where (eq :name "张三"))
	(red-db/delete!))

=> delete from user where name = "张三"
```
在where条件只有and的情况下，delete!函数支持一种简写的形式，
```clojure
(red-db/delete! :user {:age 10 :name "张三"})
=> delete from user where  age = 10 and name = "张三";

;; 辅助函数也可以作用于这种形式
(red-db/delete! :user {:age (lt 10) :name (like "张三")})
=> delete from user where age < 10 and  name like  "%张三%" ;
```

### get-one
查询单条数据
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (select :*)
    (from :user)
	(where (eq :name "张三"))
	(red-db/get-one))
```
返回结果
```clojure
{:id 1 :name "张三" :age 12}
```
在where条件只有and的情况下，get-one函数同样也支持简写的形式
```clojure
(red-db/get-one :user {:age 10 :name "张三"})
=> select * from user where  age = 10 and name = "张三";

(red-db/get-one :user {:age (lt 10) :name (like "张三")})
=> select * from user where age < 10 and  name like  "%张三%" ;
```

### get-list
查询多条数据,类似于get-one函数，只是返回结果变成数组
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (select :*)
    (from :user)
	(where (eq :name "张三"))
	(red-db/get-list))
	
(red-db/get-list :user {:name "张三"})
```
返回结果
```clojure
[{:id 1 :name "张三" :age 12}]
```

### get-count
查询数量，类似于get-list, 只是返回结果变成查询的数量
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (from :user)
	(where (eq :name "张三"))
	(red-db/get-count))
	
(red-db/get-count :user {:name "张三"})
```
返回结果
```clojure
1
```

### get-page
分页查询

```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(-> (select :*) 
	(from :user)
	(where (AND (like :email "@qq.com")
	            (lt :age 13)))
	(pagination 0 10)
	(red-db/get-page))
```

返回数据
```clojure
{:rows   ;;查询记录数据
 [{:id 1, :name "刘一", :age 11, :email "liuyi@qq.com"}
  {:id 2, :name "陈二", :age 12, :email "chener@qq.com"}],
 :page 0,        ;; 当前分页
 :size 10,       ;; 每页的数量
 :total-count 2, ;; 总条数
 :total-page 1   ;; 总页数
 }
```

### 动态的构建查询
有时候我们会根据条件动态的改变where的查询条件，使用辅助函数可以友好的实现。比如在一个用户管理系统中，我们可以输入用户名进行查找，不输入用户名就查找所有。
```clojure
(require [red-db.core :as red-db])
(require [red-db.helper :refer :all])

(defn get-users [name]
	(-> (select :*)
		(from :user)
		(where (eq (seq name) :name name))
		(red-db/get-list)))
		
(get-users "张三")
=> select * from user where name = "张三"

(get-users nil)
=> select * from user;
```
上面的辅助函数eq, 如果传入三个参数，那第一个参数就作为判断条件。

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
	(where (OR (eq :name "张三")
	           (eq :age 13)))

=> select * from user where name = "张三" or age = 13
```

#### AND
```clojure
(-> (select :*) 
	(from :user)
	(where (AND (eq :name "张三")
	           (eq :age 13)))

=> select * from user where name = "张三" and age = 13
```

#### eq
等于 = 
```clojure
;; 三种使用场景

;; 1. 传入一个参数
(red-db/get-list :user {:name (eq "张三")})

;; 1. 传入两个参数
(-> (select :*)
    (from :user)
	(where (eq :name "张三")))
	
;; 2. 传入三个参数
(-> (select :*)
    (from :user)
	(where (eq :name "张三")))
```
#### ne
不等于 <>
使用方式同辅助函数eq

#### gt
大于 >
使用方式同辅助函数eq

#### ge
大于等于 >=
使用方式同辅助函数eq

#### lt
小于 <
使用方式同辅助函数eq

#### le
小于等于 <=
使用方式同辅助函数eq

#### between
BETWEEN 值1 AND 值2
使用方式同辅助函数eq

#### like
LIKE '%值%'
使用方式同辅助函数eq

#### like-left
LIKE '%值'
使用方式同辅助函数eq

#### like-right
LIKE '值%'
使用方式同辅助函数eq

#### is-null
IS NULL
使用方式同辅助函数eq

#### is-not-null
 IS NOT NULL
使用方式同辅助函数eq

#### in

#### order-by-asc

#### order-by-desc

## 扩展

### 逻辑删除配置
```clojure
{:red-db
 {:logic-delete? true
  :logic-delete-field :delete_flag
  :logic-delete-value 1
  :logic-not-delete-value 0}}
```

### 返结果命名配置
```clojure
{:red-db
 {:result-set-builder next.jdbc.result-set/as-unqualified-kebab-maps}
 }
```

## hugsql增强
正如开头所说的，red-db是honeysql和hugsql的结合。在某些比较复杂的查询,比如多表统计，使用hugsql还是比较方便的。虽然hoenysql也支持多表查询, 但是结构一复杂，hoenysql这种语法反而变的繁琐，比如拿[hoenysql](https://github.com/seancorfield/honeysql)中举的例子
```clojure
;; big-complicated-map
{:select [:f.* :b.baz :c.quux [:b.bla "bla-bla"]
             (sql/call :now) (sql/raw "@x := 10")]
    :modifiers [:distinct]
    :from [[:foo :f] [:baz :b]]
    :join [:draq [:= :f.b :draq.x]]
    :left-join [[:clod :c] [:= :f.a :c.d]]
    :right-join [:bock [:= :bock.z :c.e]]
    :where [:or
             [:and [:= :f.a "bort"] [:not= :b.baz (sql/param :param1)]]
             [:< 1 2 3]
             [:in :f.e [1 (sql/param :param2) 3]]
             [:between :f.e 10 20]]
    :group-by [:f.a :c.e]
    :having [:< 0 :f.e]
    :order-by [[:b.baz :desc] :c.quux [:f.a :nulls-first]]
    :limit 50
    :offset 10}
```
为了搞懂这个查询，我们首先就是要把这个map转化为sql,然后来进分析。
而hugsql直接使用sql，看起来很直接。
```sql
-- :name find-member-coupons :? :*
-- :doc 查询会员优惠券列表
select *
from t_coupon_member cm, t_coupon c , t_app a
where c.id = cm.coupon_id 
and c.coupon_state = 1 
and c.app_id = a.app_id
```

但是在hugsql判断条件比较麻烦
```sql
-- :name find-users :? :*
-- :doc 查询用户
select *
from user
where 1=1
--~(when (seq (:name params)) (str "and name LIKE '%"(:name params)"%'"))
```

为此red-db为hugsql也实现了常用的辅助函数，放在red-db.hugsql这个命名空间下，使用方法如下
```sql
-- :name find-users :? :*
-- :doc 查询用户
/* :require [red-db.hugsql :refer :all] */
select *
from user
where 1=1
--~ (like "name" :name params)
```

下面是red-db目前提供给hugsql的辅助函数

| 函数       | 作用        |
|------------|-------------|
| eq         | 等于=       |
| ne         | 不等于 <>   |
| gt         | 大于 >      |
| ge         | 大于等于 >= |
| lt         | 小于 <      |
| le         | 小于等于 <= |
| like       | LIKE '%值%' |
| like-left  | LIKE '%值'  |
| liek-right | LIKE '值%'  |
