package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 长图溯源模块：企业自助配置的一屏整页图内容，pages 为图片 URL 的 JSON 数组字符串 */
@Data
@TableName("trace_long_module")
public class TraceLongModule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    /** NULL 表示企业默认（全部商品共用），非空表示仅该商品可见；写入见 LongModuleService#update */
    private Long goodsId;
    private String name;
    private String pages;
    private Integer sortOrder;
    /** 1 启用 0 停用 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
