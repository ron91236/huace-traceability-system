package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("voided_code_range")
public class VoidedCodeRange {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long codePackageId;
    private Integer serialDigits;
    private String serialStart;
    private String serialEnd;
    private Integer count;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String packageNo;
}
