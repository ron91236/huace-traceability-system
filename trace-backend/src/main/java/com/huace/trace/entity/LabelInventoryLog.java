package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("label_inventory_log")
public class LabelInventoryLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long codePackageId;
    private Long labelSpecId;
    private String changeType;
    private Integer quantity;
    private String serialStart;
    private String serialEnd;
    private Long orderId;
    private Long orderCodeId;
    private Long voidedRangeId;
    private String remark;
    private String operatorName;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String packageNo;
    @TableField(exist = false)
    private String labelSpecName;
}
