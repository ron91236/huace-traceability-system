package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.huace.trace.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("iot_alert_rule")
public class IotAlertRule extends BaseEntity {
    private Long enterpriseId;
    private Long deviceId;
    private String metricName;
    private String operator;
    private BigDecimal threshold;
    private String alertLevel;
    private String alertMessage;
    private Integer status;
}
