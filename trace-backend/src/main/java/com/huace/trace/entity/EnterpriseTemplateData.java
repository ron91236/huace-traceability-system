package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("enterprise_template_data")
public class EnterpriseTemplateData extends BaseEntity {
    private Long enterpriseId;
    private Long templateId;
    private String fieldKey;
    private String fieldLabel;
    private String fieldValue;
    private String fieldType;
}
