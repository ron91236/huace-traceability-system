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
