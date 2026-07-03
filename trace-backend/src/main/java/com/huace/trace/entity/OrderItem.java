package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_item")
public class OrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long batchId;
    private Long goodsId;
    private String goodsName;
    private String goodsSpec;
    private String goodsWeight;
    private Long labelSpecId;
    private String labelSpecName;
    private String labelSpecMaterial;
    private String labelSpecType;
    private BigDecimal price;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String remark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String batchName;
}
