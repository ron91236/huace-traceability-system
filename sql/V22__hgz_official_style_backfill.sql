-- ============================================================
-- V22 合格证官方样式改造：历史数据回填
-- 政策依据：《农产品质量安全承诺达标合格证管理办法》农业农村部令2025年第4号
-- 1) 承诺事项改为按主体类型固定法定文案（样式一=生产者 / 样式二=收购单位(个人)）
-- 2) 承诺依据历史标题归一化为法定3项：质量安全控制符合要求 / 自行检测合格 / 委托检测合格
-- 幂等：重复执行不产生副作用
-- ============================================================

-- 承诺事项：按主体类型写入法定固定文案
UPDATE hgz
SET promise_list = '[{"title":"未使用禁用农药、兽药及其他化合物；使用的常规农药、兽药残留不超标。","isSelect":true}]'
WHERE user_type IS NULL OR user_type <> 2;

UPDATE hgz
SET promise_list = '[{"title":"已按规定收取并保存该批次农产品承诺达标合格证或者其他质量安全合格证明；未违规使用保鲜剂、防腐剂、添加剂等。","isSelect":true}]'
WHERE user_type = 2;

-- 承诺依据：历史标题映射为法定标题（带引号精确匹配，避免误伤"委托检测合格"）
UPDATE hgz SET basis_list = REPLACE(basis_list, '"自我承诺"', '"质量安全控制符合要求"')
WHERE basis_list LIKE '%"自我承诺"%';

UPDATE hgz SET basis_list = REPLACE(basis_list, '"质量安全内部控制合格"', '"质量安全控制符合要求"')
WHERE basis_list LIKE '%"质量安全内部控制合格"%';

UPDATE hgz SET basis_list = REPLACE(basis_list, '"自我检测合格"', '"自行检测合格"')
WHERE basis_list LIKE '%"自我检测合格"%';
