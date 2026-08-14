package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("dl_operation_log")
public class DlOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private String productName;
    private String versionName;
    private String versionCode;
    private String operationType;
    /** JSON字符串：修改前数据 */
    private String beforeData;
    /** JSON字符串：修改后数据 */
    private String afterData;
    private String creator;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
