package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DataScreenService {

    private final ScanRecordMapper scanRecordMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final GoodsMapper goodsMapper;
    private final CodePackageItemMapper codePackageItemMapper;
    private final ProductMapper productMapper;
    private final BatchMapper batchMapper;

    @Cacheable(value = "dataScreen", key = "#enterpriseId == null ? 'all:admin' : 'all:' + #enterpriseId")
    public Map<String, Object> getAllData(Long enterpriseId) {
        Map<String, Object> result = new HashMap<>();
        result.put("kpi", getKpiData(enterpriseId));
        result.put("provinceScan", scanRecordMapper.countByProvince(enterpriseId));
        result.put("cityRanking", scanRecordMapper.countByCity(enterpriseId));
        result.put("scanTrend", scanRecordMapper.scanTrend(enterpriseId));
        result.put("scanRate", getScanRate(enterpriseId));
        result.put("labelDistribution", scanRecordMapper.monthlyScans(enterpriseId));
        result.put("productCategory", getProductCategoryRatio(enterpriseId));
        if (enterpriseId == null) {
            result.put("enterpriseRanking", getEnterpriseScanRanking());
        }
        return result;
    }

    private Map<String, Object> getKpiData(Long enterpriseId) {
        Map<String, Object> kpi = new HashMap<>();
        LambdaQueryWrapper<CodePackageItem> boundQuery = new LambdaQueryWrapper<CodePackageItem>()
                .eq(CodePackageItem::getBindStatus, "BOUND");
        if (enterpriseId != null) boundQuery.eq(CodePackageItem::getEnterpriseId, enterpriseId);
        kpi.put("totalInventory", codePackageItemMapper.selectCount(boundQuery));

        LambdaQueryWrapper<ScanRecord> scanQuery = new LambdaQueryWrapper<>();
        if (enterpriseId != null) scanQuery.eq(ScanRecord::getEnterpriseId, enterpriseId);
        kpi.put("totalScans", scanRecordMapper.selectCount(scanQuery));

        if (enterpriseId != null) {
            kpi.put("merchantCount", 1L);
            kpi.put("batchCount", batchMapper.selectCount(
                    new LambdaQueryWrapper<Batch>().eq(Batch::getEnterpriseId, enterpriseId)));
        } else {
            kpi.put("merchantCount", enterpriseMapper.selectCount(null));
        }

        LambdaQueryWrapper<Goods> goodsQuery = new LambdaQueryWrapper<>();
        if (enterpriseId != null) goodsQuery.eq(Goods::getEnterpriseId, enterpriseId);
        kpi.put("productCount", goodsMapper.selectCount(goodsQuery));
        return kpi;
    }

    private Map<String, Object> getScanRate(Long enterpriseId) {
        LambdaQueryWrapper<CodePackageItem> totalQuery = new LambdaQueryWrapper<>();
        if (enterpriseId != null) totalQuery.eq(CodePackageItem::getEnterpriseId, enterpriseId);
        long total = codePackageItemMapper.selectCount(totalQuery);

        LambdaQueryWrapper<CodePackageItem> boundQuery = new LambdaQueryWrapper<CodePackageItem>()
                .eq(CodePackageItem::getBindStatus, "BOUND");
        if (enterpriseId != null) boundQuery.eq(CodePackageItem::getEnterpriseId, enterpriseId);
        long bound = codePackageItemMapper.selectCount(boundQuery);

        Map<String, Object> rate = new HashMap<>();
        rate.put("total", total);
        rate.put("scanned", scanRecordMapper.selectCount(
                enterpriseId != null ? new LambdaQueryWrapper<ScanRecord>()
                        .eq(ScanRecord::getEnterpriseId, enterpriseId) : new LambdaQueryWrapper<>()));
        rate.put("bound", bound);
        rate.put("unbound", total - bound);
        return rate;
    }

    private List<Map<String, Object>> getProductCategoryRatio(Long enterpriseId) {
        LambdaQueryWrapper<Goods> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) w.eq(Goods::getEnterpriseId, enterpriseId);
        List<Goods> goodsList = goodsMapper.selectList(w);

        Set<Long> productIds = goodsList.stream()
                .map(Goods::getProductId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Product> productMap = productIds.isEmpty() ? Collections.emptyMap() :
                productMapper.selectBatchIds(productIds).stream()
                        .collect(Collectors.toMap(Product::getId, p -> p));

        Map<String, Long> categoryCount = goodsList.stream()
                .collect(Collectors.groupingBy(
                        g -> productCategoryName(productMap.get(g.getProductId())),
                        Collectors.counting()));

        List<Map<String, Object>> result = new ArrayList<>();
        categoryCount.forEach((name, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", count);
            result.add(item);
        });
        return result;
    }

    private String productCategoryName(Product p) {
        if (p == null) return "未分类";
        return p.getCategory() != null ? p.getCategory() : (p.getName() != null ? p.getName() : "未分类");
    }

    private List<Map<String, Object>> getEnterpriseScanRanking() {
        List<Map<String, Object>> stats = scanRecordMapper.countByEnterprise();
        List<Long> enterpriseIds = stats.stream()
                .map(m -> ((Number) m.get("enterprise_id")).longValue())
                .collect(Collectors.toList());
        Map<Long, Enterprise> entMap = enterpriseIds.isEmpty() ? Collections.emptyMap() :
                enterpriseMapper.selectBatchIds(enterpriseIds).stream()
                        .collect(Collectors.toMap(Enterprise::getId, e -> e));

        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Map<String, Object> row : stats) {
            Enterprise e = entMap.get(((Number) row.get("enterprise_id")).longValue());
            Map<String, Object> item = new HashMap<>();
            item.put("enterpriseName", e != null ? e.getName() : "未知");
            item.put("scanCount", ((Number) row.get("count")).longValue());
            ranking.add(item);
        }
        return ranking;
    }
}
