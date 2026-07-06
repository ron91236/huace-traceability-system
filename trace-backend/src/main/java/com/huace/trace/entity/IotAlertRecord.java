package com.huace.trace.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("iot_alert_record")
public class IotAlertRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long enterpriseId;
    private Long deviceId;
    private Long ruleId;
    private String metricName;
    private BigDecimal metricValue;
    private BigDecimal threshold;
    private String alertLevel;
    private String alertMessage;
    private Integer handleStatus;
    private String handleNote;
    private LocalDateTime createdAt;
}
