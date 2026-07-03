package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("test_report")
public class TestReport extends BaseEntity {
    private Long enterpriseId;
    private String reportName;
    private String testCode;
    private String reportImage;
    private String reportPdf;
    private String testOrg;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime testTime;
    private String testMethod;
    private String testBasis;
    private String testType;
    private String testResult;
}
