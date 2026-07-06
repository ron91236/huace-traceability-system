package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_device")
public class IotDevice extends BaseEntity {
    private Long enterpriseId;
    private Long baseId;
    private Long batchId;
    private String deviceName;
    private String deviceType;
    private String productKey;
    private String deviceKey;
    private String iotInstanceId;
    private String locationDesc;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Integer status;
    private LocalDateTime lastOnlineAt;
}
