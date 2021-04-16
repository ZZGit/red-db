--;; 订单表
create table t_order (
  id int auto_increment primary key,
  order_no varchar(32),
  price float,
  delete_flag tinyint(1) default 0
);
