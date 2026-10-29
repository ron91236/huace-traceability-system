package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 企业侧边栏外链按钮。挂企业而非模板（spec 决定 4）：公众号/微博是企业固有属性，
 * 同企业多个商品共用，换模板不该丢配置。
 *
 * <p>url 与 image 至少填一项（对应"跳转链接优先"）：有 url 就跳，只有 image 就弹二维码。
 */
@Data
@TableName("enterprise_side_link")
public class EnterpriseSideLink {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    /** 按钮文案，即侧边栏显示文字；VARCHAR(50) */
    private String name;
    /** 图标标识（图片 URL 或图标名），可空则前端用默认样式 */
    private String icon;
    private String url;
    private String image;
    private Integer sortOrder;
    /** 1 启用 0 停用 */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
