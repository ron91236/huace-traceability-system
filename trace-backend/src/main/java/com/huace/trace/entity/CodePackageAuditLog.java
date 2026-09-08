package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("code_package_audit_log")
public class CodePackageAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long packageId;
    private String action;
    private String operatorName;
    private String reason;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
