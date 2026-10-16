-- ============================================================
-- V23 清理码包删除后遗留的孤儿引用数据
-- 背景：CodePackageService.delete() 此前只删 code_package / code_package_item，
--      未清理引用码包的派生数据，导致发放管理、作废码段、溯源码库存页面出现脏数据。
--      代码侧已补齐级联删除，本脚本清理历史存量。
-- 保留：订单号 20261016-1 的发放记录（业务确认需保留）
-- 幂等：以"码包是否仍存在"为判定条件，重复执行不产生副作用
-- ============================================================

-- 发放记录：码包已删除或从未关联码包的记录
DELETE oc FROM order_code oc
LEFT JOIN code_package cp ON cp.id = oc.code_package_id
LEFT JOIN t_order o ON o.id = oc.order_id
WHERE cp.id IS NULL
  AND (o.order_no IS NULL OR o.order_no <> '20261016-1');

-- 作废码段：所属码包已不存在
DELETE v FROM voided_code_range v
LEFT JOIN code_package cp ON cp.id = v.code_package_id
WHERE cp.id IS NULL;

-- 溯源码库存日志：所属码包已不存在
DELETE l FROM label_inventory_log l
LEFT JOIN code_package cp ON cp.id = l.code_package_id
WHERE cp.id IS NULL;

-- 作废码：所属码包已不存在
DELETE w FROM waste_code w
LEFT JOIN code_package cp ON cp.id = w.code_package_id
WHERE cp.id IS NULL;
