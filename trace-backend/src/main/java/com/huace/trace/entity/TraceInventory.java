package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("trace_inventory")
public class TraceInventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String codePool;
    private Long labelSpecId;
    private String startSerial;
    private String endSerial;
    private LocalDateTime produceTime;
    private Long enterpriseId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String enterpriseName;
    @TableField(exist = false)
    private String labelSpecName;
}
