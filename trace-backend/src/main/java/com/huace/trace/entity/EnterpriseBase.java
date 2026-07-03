package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("enterprise_base")
public class EnterpriseBase extends BaseEntity {
    private Long enterpriseId;
    private String name;
    private String code;
    private BigDecimal area;
    private String unit;
    private String manager;
    private String phone;
    private String planImage;
    private String realImage;
    private String certification;
    private String province;
    private String city;
    private String district;
    private String testItems;
    private String envReport;
    private String monitorInfo;

    @TableField(exist = false)
    private String enterpriseName;
}
