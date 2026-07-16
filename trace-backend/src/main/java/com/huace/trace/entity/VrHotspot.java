package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("vr_hotspot")
public class VrHotspot {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sceneId;
    private String type;
    private Long targetSceneId;
    private String label;
    private String tooltip;
    private BigDecimal hYaw;
    private BigDecimal vPitch;
    private Integer sortOrder;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
