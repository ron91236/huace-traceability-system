package com.huace.trace.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * MongoDB 传感器时序数据文档
 */
@Data
@Document(collection = "iot_sensor_data")
public class IotSensorData {

    @Id
    private String id;

    private LocalDateTime timestamp;

    private Map<String, Object> metadata;  // enterpriseId, deviceId, deviceType, baseId

    private Map<String, Double> metrics;   // temperature, humidity, soilMoisture, ph
}
