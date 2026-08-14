package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.List;

@Data
@TableName("dl_food_category")
public class DlFoodCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String name;
    private String fullPath;
    private Integer sortOrder;

    /** 非数据库字段：子分类 */
    @TableField(exist = false)
    private List<DlFoodCategory> children;
}
