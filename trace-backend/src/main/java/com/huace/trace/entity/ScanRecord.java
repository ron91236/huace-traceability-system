package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("scan_record")
public class ScanRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String serialNo;
    private Long enterpriseId;
    private String province;
    private String city;
    private String ip;
    private String userAgent;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
