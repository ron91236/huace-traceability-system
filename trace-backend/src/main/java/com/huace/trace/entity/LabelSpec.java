package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("label_spec")
public class LabelSpec {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String specName;
    private String material;
    private BigDecimal price;
    private String usageMethod;
    private Integer supportManualAssign;
    private Integer isVoid;
    private String labelImage;
    private Long certTypeId;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
