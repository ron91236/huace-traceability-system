package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("waste_code")
public class WasteCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long codePackageId;
    private String serialStart;
    private String serialEnd;
    private Integer count;
    private String reason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String packageNo;
}
