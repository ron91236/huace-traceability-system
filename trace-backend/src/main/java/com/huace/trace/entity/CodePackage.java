package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("code_package")
public class CodePackage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String packageNo;
    private Long labelSpecId;
    private Integer totalCount;
    private String sourceFile;
    // 码包生成字段
    private String productCode;
    private String yearCode;
    private Integer serialDigits;
    private Long serialStart;
    private Long serialEnd;
    private Long startQuantity;
    private String codeType;
    private String urlPrefix;
    private Integer antiFakeDigits;  // 防伪码位数
    private String verifyMode;       // 防伪验证模式: INPUT(输入防伪码) / DIRECT(扫码即查)
    private String remark;
    private String sourceType;
    // 新增字段
    private String packageName;
    private String productDesc;
    private String creator;
    private String packagingType;
    // 通用字段
    private String status;
    private LocalDateTime importTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String labelSpecName;
    @TableField(exist = false)
    private Integer boundCount;
}
package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("code_package")
public class CodePackage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String packageNo;
    private Long labelSpecId;
    private Integer totalCount;
    private String sourceFile;
    // 码包生成字段
    private String productCode;
    private String yearCode;
    private Integer serialDigits;
    private Long serialStart;
    private Long serialEnd;
    private Long startQuantity;
    private String codeType;
    private String urlPrefix;
    private Integer antiFakeDigits;  // 防伪码位数
    private String verifyMode;       // 防伪验证模式: INPUT(输入防伪码) / DIRECT(扫码即查)
    private String remark;
    private String sourceType;
    // 通用字段
    private String status;
    private LocalDateTime importTime;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String labelSpecName;
    @TableField(exist = false)
    private Integer boundCount;
}
