package com.huace.trace.repository;

import com.huace.trace.entity.mongo.IotGpsTrack;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IotGpsTrackRepository extends MongoRepository<IotGpsTrack, String> {

    @Query("{'metadata.deviceId': ?0, 'timestamp': {$gte: ?1, $lte: ?2}}")
    List<IotGpsTrack> findByDeviceIdAndTimeRange(
            Long deviceId, LocalDateTime from, LocalDateTime to);
}
