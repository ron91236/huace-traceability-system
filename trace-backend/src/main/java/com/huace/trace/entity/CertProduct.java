package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("cert_product")
public class CertProduct {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long certId;
    private Long productId;
    private String productName;
    private BigDecimal totalProduction;     // 总产量(吨)
    private BigDecimal remainingProduction; // 剩余产量(吨)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String productDescription;
}
