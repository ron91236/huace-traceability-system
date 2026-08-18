package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("batch")
public class Batch extends BaseEntity {
    private String name;
    private Long goodsId;
    private String goodsSpec;
    private Long baseId;
    private Long enterpriseId;
    private Long testReportId;
    private String testCode;
    private String testReport;
    private String testOrg;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testTime;
    private String testMethod;
    private String testBasis;
    private String testType;
    private String testResult;

    @TableField(exist = false)
    private String goodsName;
    @TableField(exist = false)
    private String baseName;
    @TableField(exist = false)
    private String enterpriseName;
    @TableField(exist = false)
    private String testReportName;
    @TableField(exist = false)
    private List<Long> testReportIds;
}
