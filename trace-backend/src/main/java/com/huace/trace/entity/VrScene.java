package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("vr_scene")
public class VrScene {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long baseId;
    private String name;
    private String panoramaUrl;
    private Integer hfov;
    private Integer vfov;
    private Integer sortOrder;
    private Integer isDefault;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private List<VrHotspot> hotspots;
    @TableField(exist = false)
    private String enterpriseName;
    @TableField(exist = false)
    private String baseName;
}
