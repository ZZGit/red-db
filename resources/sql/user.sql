-- :name find-users :? :*
-- :doc 查询用户
/* :require [red-db.hugsql :refer :all] */
select *
from user
where 1=1
--~ (like "name" :name params)


-- :name insert-user! :! :n
-- :doc 插入用户
insert into user
(name, age, email, delete_flag)
values
(:name, :age, :email, :delete_flag)
