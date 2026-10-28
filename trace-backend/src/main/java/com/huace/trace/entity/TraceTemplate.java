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
    /** 派生字段，从 configJson.renderMode 读出后填充；不落库。前端据此决定菜单显隐 */
    @TableField(exist = false)
    private String renderMode;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
