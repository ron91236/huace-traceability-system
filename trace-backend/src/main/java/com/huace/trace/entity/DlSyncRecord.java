package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_sync_record")
public class DlSyncRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String syncType;
    private String syncCondition;
    private String timeRange;
    private Integer totalCount;
    private Integer sameCount;
    private Integer newCount;
    private Integer updateCount;
    private String status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
