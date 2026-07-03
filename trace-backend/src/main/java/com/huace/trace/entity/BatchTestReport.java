package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("batch_test_report")
public class BatchTestReport {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long batchId;
    private Long testReportId;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
