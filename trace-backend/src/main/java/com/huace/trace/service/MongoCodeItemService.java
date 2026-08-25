package com.huace.trace.service;

import com.huace.trace.entity.CodePackageItem;
import com.huace.trace.entity.mongo.CodePackageItemMongo;
import com.huace.trace.repository.CodePackageItemMongoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB 码包明细服务
 * 作为 MySQL 的扩展存储，承载亿级码包的明细读写
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MongoCodeItemService {

    private final CodePackageItemMongoRepository mongoRepository;
    private final MongoTemplate mongoTemplate;

    private volatile boolean mongoAvailable = true;

    public boolean isMongoAvailable() {
        if (!mongoAvailable) return false;
        try {
            mongoTemplate.getDb().getName();
            return true;
        } catch (DataAccessException e) {
            log.warn("MongoDB 连接不可用，降级到 MySQL: {}", e.getMessage());
            mongoAvailable = false;
            return false;
        }
    }

    public Optional<CodePackageItemMongo> findBySerialNo(String serialNo) {
        if (!isMongoAvailable()) return Optional.empty();
        try {
            return mongoRepository.findBySerialNo(serialNo);
        } catch (DataAccessException e) {
            log.warn("MongoDB 查询失败，serialNo={}: {}", serialNo, e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<CodePackageItemMongo> findBoundBySerialNo(String serialNo) {
        if (!isMongoAvailable()) return Optional.empty();
        try {
            return mongoRepository.findBySerialNoAndBindStatus(serialNo, "BOUND");
        } catch (DataAccessException e) {
            log.warn("MongoDB 查询失败，serialNo={}: {}", serialNo, e.getMessage());
            return Optional.empty();
        }
    }

    public void saveAll(List<CodePackageItemMongo> items) {
        saveAll(items, 5000);
    }

    public void saveAll(List<CodePackageItemMongo> items, int batchSize) {
        if (items == null || items.isEmpty()) return;
        if (!isMongoAvailable()) {
            log.warn("MongoDB 不可用，跳过 {} 条码明细写入", items.size());
            return;
        }
        try {
            int size = items.size();
            for (int i = 0; i < size; i += batchSize) {
                List<CodePackageItemMongo> batch = items.subList(i, Math.min(i + batchSize, size));
                mongoRepository.saveAll(batch);
            }
            log.info("MongoDB 批量写入完成: count={}", size);
        } catch (DataAccessException e) {
            log.error("MongoDB 批量写入失败: {}", e.getMessage());
        }
    }

    public void save(CodePackageItemMongo item) {
        if (!isMongoAvailable()) return;
        try {
            mongoRepository.save(item);
        } catch (DataAccessException e) {
            log.error("MongoDB 单条写入失败: {}", e.getMessage());
        }
    }

    public void deleteByPackageId(Long packageId) {
        if (!isMongoAvailable()) return;
        try {
            List<CodePackageItemMongo> list = mongoRepository.findByPackageId(packageId);
            if (!list.isEmpty()) {
                mongoRepository.deleteAll(list);
            }
        } catch (DataAccessException e) {
            log.error("MongoDB 删除 packageId={} 失败: {}", packageId, e.getMessage());
        }
    }

    /**
     * 解绑：将指定订单码段关联的 MongoDB 明细恢复为未绑定状态
     */
    public void unbindByOrderCodeId(Long orderCodeId) {
        if (!isMongoAvailable()) return;
        try {
            Query query = new Query(Criteria.where("orderCodeId").is(orderCodeId));
            Update update = new Update()
                    .set("bindStatus", "UNBOUND")
                    .unset("orderCodeId")
                    .unset("bindTime")
                    .unset("enterpriseId")
                    .unset("goodsId")
                    .unset("certId")
                    .unset("batchId")
                    .unset("traceTemplate");
            mongoTemplate.updateMulti(query, update, CodePackageItemMongo.class);
        } catch (DataAccessException e) {
            log.warn("MongoDB 解绑 orderCodeId={} 失败: {}", orderCodeId, e.getMessage());
        }
    }

    /**
     * 更新 MongoDB 中指定流水号的扫码次数
     */
    public void updateScanCount(String serialNo, Integer scanCount) {
        if (!isMongoAvailable()) return;
        try {
            Query query = new Query(Criteria.where("serialNo").is(serialNo));
            Update update = new Update().set("scanCount", scanCount);
            mongoTemplate.updateFirst(query, update, CodePackageItemMongo.class);
        } catch (DataAccessException e) {
            log.warn("MongoDB 更新 scanCount 失败，serialNo={}: {}", serialNo, e.getMessage());
        }
    }

    public static CodePackageItemMongo fromMyBatis(CodePackageItem item) {
        CodePackageItemMongo m = new CodePackageItemMongo();
        m.setItemId(item.getId());
        m.setPackageId(item.getPackageId());
        m.setSerialNo(item.getSerialNo());
        m.setAntiFakeCode(item.getAntiFakeCode());
        m.setUrl(item.getUrl());
        m.setBindStatus(item.getBindStatus());
        m.setEnterpriseId(item.getEnterpriseId());
        m.setGoodsId(item.getGoodsId());
        m.setCertId(item.getCertId());
        m.setBatchId(item.getBatchId());
        m.setTraceTemplate(item.getTraceTemplate());
        m.setOrderCodeId(item.getOrderCodeId());
        m.setBindTime(item.getBindTime());
        m.setScanCount(item.getScanCount());
        m.setCreatedAt(item.getCreatedAt());
        return m;
    }
}
