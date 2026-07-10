package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("enterprise_cert")
public class EnterpriseCert {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long certTypeId;
    private Long enterpriseId;
    private String certName;
    private String productName;
    private String certNo;
    private Long labelSpecId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer isVoid;
    private String certImage;
    private String certPdf;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 关联字段（非数据库列） */
    @TableField(exist = false)
    private String certTypeName;
    @TableField(exist = false)
    private String enterpriseName;
    @TableField(exist = false)
    private String labelSpecName;
}
