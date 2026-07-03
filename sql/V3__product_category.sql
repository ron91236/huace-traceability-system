-- 产品表增加分类和编码字段
ALTER TABLE product ADD COLUMN category VARCHAR(50) DEFAULT NULL COMMENT '产品分类' AFTER description;
ALTER TABLE product ADD COLUMN code VARCHAR(50) DEFAULT NULL COMMENT '产品编码' AFTER category;
