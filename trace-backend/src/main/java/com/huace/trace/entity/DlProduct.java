package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_product")
public class DlProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    /** 企业名称（非持久化，管理员全局查看时填充） */
    @TableField(exist = false)
    private String enterpriseName;
    private String foodName;
    private String barcode;
    private String spec;
    private Integer labelVersionCount;
    private String syncStatus;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
