package com.huace.trace.repository;

import com.huace.trace.entity.mongo.IotDeviceLatest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IotDeviceLatestRepository extends MongoRepository<IotDeviceLatest, String> {

    Optional<IotDeviceLatest> findByDeviceId(Long deviceId);

    List<IotDeviceLatest> findByEnterpriseId(Long enterpriseId);
}
