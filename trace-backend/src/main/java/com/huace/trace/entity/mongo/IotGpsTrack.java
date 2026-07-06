package com.huace.trace.entity.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * MongoDB GPS 轨迹时序数据文档
 */
@Data
@Document(collection = "iot_gps_track")
public class IotGpsTrack {

    @Id
    private String id;

    private LocalDateTime timestamp;

    private Map<String, Object> metadata;  // enterpriseId, deviceId, batchId, vehiclePlate

    private Map<String, Object> location;  // GeoJSON: { type: "Point", coordinates: [lng, lat] }

    private Double speed;

    private Integer direction;

    private Double temperature;  // 冷链车厢温度（可选）
}
