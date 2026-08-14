package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_scan_record")
public class DlScanRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long versionId;
    private Long productId;
    private LocalDateTime scanTime;
    private String locationProvince;
    private String locationCity;
    private String ip;
    private String userAgent;
}
