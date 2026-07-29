-- 企业分配溯源模板：记录企业可使用的模板ID列表（逗号分隔）

ALTER TABLE `enterprise`
ADD COLUMN `assigned_template_ids` VARCHAR(255) DEFAULT NULL COMMENT '分配的溯源模板ID，逗号分隔' AFTER `promo_video`;

-- 标签规格关联证书类型：限制企业只能使用所属证书对应的标签规格

ALTER TABLE `label_spec`
ADD COLUMN `cert_type_id` BIGINT DEFAULT NULL COMMENT '关联证书类型ID，NULL表示通用' AFTER `label_image`;
