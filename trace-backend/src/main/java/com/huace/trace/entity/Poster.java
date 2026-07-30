package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("poster")
public class Poster {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String slug;
    private String fileName;
    private String filePath;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 非数据库字段：二维码Base64 */
    @TableField(exist = false)
    private String qrCode;
    /** 非数据库字段：访问URL */
    @TableField(exist = false)
    private String posterUrl;
}
