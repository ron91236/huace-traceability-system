package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 长图溯源内容：某个企业在某个模板的某个模块上拼的整页图。
 *
 * <p>模块的名字、顺序、启停都不在这里——那是模板的骨架（config_json.longModules），
 * 由管理端决定。goods_id / name / sort_order / status 四列仍留在表里，
 * 但代码不再读写（spec 决定 8 与决定 10）：新行只填 template_id / enterprise_id /
 * module_key / pages，其余走库默认值。name 因此必须允许 NULL，见 V28。
 */
@Data
@TableName("trace_long_module")
public class TraceLongModule {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 归属模板；与 config_json 那行模板对应 */
    private Long templateId;
    private Long enterpriseId;
    /** 对应 config_json.longModules[].key；结构归管理端，这里只存该企业填的页图 */
    private String moduleKey;
    /** 旧列，代码不再读写 */
    private Long goodsId;
    /** 旧列，代码不再读写：模块名归模板骨架 */
    private String name;
    /** 整页图片 URL 的 JSON 数组字符串，顺序即拼接顺序 */
    private String pages;
    /** 旧列，代码不再读写 */
    private Integer sortOrder;
    /** 旧列，代码不再读写：整个模板有 trace_template.status */
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
