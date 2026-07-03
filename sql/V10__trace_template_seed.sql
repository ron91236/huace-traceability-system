-- V10: 更新溯源模板 - 添加三种主题风格

-- 更新现有 default 模板为 standard-green 主题
UPDATE `trace_template` SET `config_json` = JSON_SET(
  `config_json`,
  '$.theme.key', 'standard-green',
  '$.theme.primaryColor', '#059669'
) WHERE `template_key` = 'default';

-- 更新现有 vegetable 模板为 standard-green 主题
UPDATE `trace_template` SET `config_json` = JSON_SET(
  `config_json`,
  '$.theme.key', 'standard-green',
  '$.theme.primaryColor', '#059669'
) WHERE `template_key` = 'vegetable';

-- 更新现有 fruit 模板为 standard-green 主题
UPDATE `trace_template` SET `config_json` = JSON_SET(
  `config_json`,
  '$.theme.key', 'standard-green',
  '$.theme.primaryColor', '#059669'
) WHERE `template_key` = 'fruit';

-- 更新现有 meat 模板为 standard-green 主题
UPDATE `trace_template` SET `config_json` = JSON_SET(
  `config_json`,
  '$.theme.key', 'standard-green',
  '$.theme.primaryColor', '#059669'
) WHERE `template_key` = 'meat';

-- 更新现有 grain 模板为 standard-green 主题
UPDATE `trace_template` SET `config_json` = JSON_SET(
  `config_json`,
  '$.theme.key', 'standard-green',
  '$.theme.primaryColor', '#059669'
) WHERE `template_key` = 'grain';

-- 更新现有 aquatic 模板为 standard-green 主题
UPDATE `trace_template` SET `config_json` = JSON_SET(
  `config_json`,
  '$.theme.key', 'standard-green',
  '$.theme.primaryColor', '#059669'
) WHERE `template_key` = 'aquatic';

-- 新增科技蓝主题模板
INSERT INTO `trace_template` (`template_key`, `template_name`, `template_type`, `config_json`, `status`) VALUES
('tech-blue', '科技蓝模板', '通用', '{"sections":[{"key":"enterprise","title":"企业信息","icon":"OfficeBuilding","fields":[{"field":"enterprise.name","label":"企业名称"},{"field":"enterprise.introduction","label":"企业简介","type":"text"},{"field":"enterprise.addressFull","label":"企业地址"}]},{"key":"product","title":"产品信息","icon":"Goods","fields":[{"field":"goods.name","label":"商品名称"},{"field":"goods.introduction","label":"产品介绍","type":"text"},{"field":"goods.storageMethod","label":"储存方式"}]},{"key":"batch","title":"批次信息","icon":"Tickets","fields":[{"field":"batch.name","label":"批次名称"},{"field":"batch.testOrg","label":"检测机构"},{"field":"batch.testResult","label":"检测结果","type":"badge"},{"field":"batch.testTime","label":"检测时间"}]},{"key":"base","title":"基地信息","icon":"Location","fields":[{"field":"base.name","label":"基地名称"},{"field":"base.areaDisplay","label":"基地面积"},{"field":"base.manager","label":"负责人"}]}],"theme":{"key":"tech-blue","primaryColor":"#1e40af","gradient":"linear-gradient(135deg, #1e40af 0%, #3b82f6 100%)"}}', 1);

-- 新增品质金主题模板
INSERT INTO `trace_template` (`template_key`, `template_name`, `template_type`, `config_json`, `status`) VALUES
('premium-gold', '品质金模板', '通用', '{"sections":[{"key":"enterprise","title":"企业信息","icon":"OfficeBuilding","fields":[{"field":"enterprise.name","label":"企业名称"},{"field":"enterprise.introduction","label":"企业简介","type":"text"},{"field":"enterprise.addressFull","label":"企业地址"}]},{"key":"product","title":"产品信息","icon":"Goods","fields":[{"field":"goods.name","label":"商品名称"},{"field":"goods.introduction","label":"产品介绍","type":"text"},{"field":"goods.storageMethod","label":"储存方式"}]},{"key":"batch","title":"批次信息","icon":"Tickets","fields":[{"field":"batch.name","label":"批次名称"},{"field":"batch.testOrg","label":"检测机构"},{"field":"batch.testResult","label":"检测结果","type":"badge"},{"field":"batch.testTime","label":"检测时间"}]},{"key":"base","title":"基地信息","icon":"Location","fields":[{"field":"base.name","label":"基地名称"},{"field":"base.areaDisplay","label":"基地面积"},{"field":"base.manager","label":"负责人"},{"field":"base.certification","label":"基地认证"}]}],"theme":{"key":"premium-gold","primaryColor":"#92400e","gradient":"linear-gradient(135deg, #92400e 0%, #b45309 50%, #d97706 100%)"}}', 1);
