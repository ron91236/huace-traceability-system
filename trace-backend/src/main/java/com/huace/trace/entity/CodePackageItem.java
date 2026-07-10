package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("code_package_item")
public class CodePackageItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long packageId;
    private String serialNo;
    private String antiFakeCode;
    private String url;
    private Long orderCodeId;
    private Long enterpriseId;
    private Long goodsId;
    private Long certId;
    private Long batchId;
    private String traceTemplate;
    private String bindStatus;
    private LocalDateTime bindTime;
    private Integer scanCount;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String enterpriseName;
    @TableField(exist = false)
    private String goodsName;
    @TableField(exist = false)
    private String certName;
    @TableField(exist = false)
    private String batchName;
    @TableField(exist = false)
    private String orderNo;
}
