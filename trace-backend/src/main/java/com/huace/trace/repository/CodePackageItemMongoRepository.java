package com.huace.trace.repository;

import com.huace.trace.entity.mongo.CodePackageItemMongo;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CodePackageItemMongoRepository extends MongoRepository<CodePackageItemMongo, String> {

    Optional<CodePackageItemMongo> findBySerialNo(String serialNo);

    Optional<CodePackageItemMongo> findBySerialNoAndBindStatus(String serialNo, String bindStatus);

    List<CodePackageItemMongo> findByPackageId(Long packageId);

    List<CodePackageItemMongo> findByPackageIdAndBindStatus(Long packageId, String bindStatus);

    long countByPackageIdAndBindStatus(Long packageId, String bindStatus);
}
