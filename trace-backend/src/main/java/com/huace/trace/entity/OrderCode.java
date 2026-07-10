package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("order_code")
public class OrderCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long codePackageId;     // 码包ID
    private Long labelSpecId;
    private Long batchId;
    private String productName;
    private String traceTemplate;
    private String productTemplate;
    private Integer quantity;
    private BigDecimal price;
    private Integer isUnsubscribed;
    private String serialStart;
    private String serialEnd;
    private Integer wasteCount;     // 作废数量
    private Integer bindCount;      // 实际绑定数量(总数-作废数)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String orderNo;
    @TableField(exist = false)
    private String labelSpecName;
    @TableField(exist = false)
    private String batchName;
    @TableField(exist = false)
    private String productDescription;
    @TableField(exist = false)
    private String goodsName;
    @TableField(exist = false)
    private String enterpriseName;
    @TableField(exist = false)
    private String certNo;
    @TableField(exist = false)
    private String goodsPackageSpec;
    @TableField(exist = false)
    private String goodsWeightSpec;
}
