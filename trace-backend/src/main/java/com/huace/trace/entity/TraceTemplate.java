package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("trace_template")
public class TraceTemplate {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String templateKey;
    private String templateName;
    private String templateType;
    private String previewImage;
    private String backgroundImage;
    private String configJson;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
