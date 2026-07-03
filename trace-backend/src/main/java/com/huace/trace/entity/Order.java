package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_order")
public class Order extends BaseEntity {
    private String orderNo;
    private Long enterpriseId;
    private Long certId;
    private String status;
    private Long addressId;
    private String attachment;
    private LocalDateTime submitTime;
    private LocalDateTime reviewTime;
    private Long reviewerId;
    private String reviewNote;

    @TableField(exist = false)
    private String enterpriseName;
    @TableField(exist = false)
    private String certName;
}
