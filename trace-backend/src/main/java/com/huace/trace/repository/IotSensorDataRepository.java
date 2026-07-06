package com.huace.trace.repository;

import com.huace.trace.entity.mongo.IotSensorData;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IotSensorDataRepository extends MongoRepository<IotSensorData, String> {

    List<IotSensorData> findByMetadataDeviceIdAndTimestampBetweenOrderByTimestampAsc(
            Long deviceId, LocalDateTime from, LocalDateTime to);
}
