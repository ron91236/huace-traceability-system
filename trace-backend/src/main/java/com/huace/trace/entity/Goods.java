package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("goods")
public class Goods extends BaseEntity {
    private Long productId;
    private Long enterpriseId;
    private String name;
    private String packageSpec;
    private Integer showOuterPackage;
    private String weightSpec;
    private String sampleImage;
    private String introduction;
    private String storageMethod;
    private String eatingMethod;
    private String promoImage;
    private String promoVideo;
    private Long traceTemplateId;

    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String enterpriseName;
    @TableField(exist = false)
    private String traceTemplateName;
}
    @TableField(exist = false)
    private String traceTemplateName;
}
