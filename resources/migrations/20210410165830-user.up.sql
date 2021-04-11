--;; 用户表
create table user (
  id int auto_increment primary key,
  name varchar(32),
  age int,
  email varchar(255),
  delete_flag tinyint(1) default 0
);
