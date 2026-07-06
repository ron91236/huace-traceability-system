package com.huace.trace.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB 设备最新状态缓存文档（普通集合，非时序）
 */
@Data
@Document(collection = "iot_device_latest")
public class IotDeviceLatest {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long deviceId;

    private Long enterpriseId;

    private String deviceType;

    private Map<String, Double> metrics;  // 最新读数：temperature, humidity, etc.

    private Map<String, Object> location; // GeoJSON（GPS设备用）

    private LocalDateTime updatedAt;
}
