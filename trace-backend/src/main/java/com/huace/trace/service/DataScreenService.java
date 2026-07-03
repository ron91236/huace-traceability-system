package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import lombok.RequiredArgsConstructor;
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

        Map<String, Long> categoryCount = goodsList.stream()
                .collect(Collectors.groupingBy(
                        g -> g.getProductId() != null ? getProductCategoryName(g.getProductId()) : "未分类",
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

    private String getProductCategoryName(Long productId) {
        if (productId == null) return "未分类";
        Product p = productMapper.selectById(productId);
        return p != null && p.getCategory() != null ? p.getCategory() : (p != null ? p.getName() : "未分类");
    }

    private List<Map<String, Object>> getEnterpriseScanRanking() {
        // Get scan count per enterprise
        List<ScanRecord> allRecords = scanRecordMapper.selectList(null);
        Map<Long, Long> countMap = allRecords.stream()
                .filter(r -> r.getEnterpriseId() != null)
                .collect(Collectors.groupingBy(ScanRecord::getEnterpriseId, Collectors.counting()));

        List<Map<String, Object>> ranking = new ArrayList<>();
        countMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    Enterprise e = enterpriseMapper.selectById(entry.getKey());
                    Map<String, Object> item = new HashMap<>();
                    item.put("enterpriseName", e != null ? e.getName() : "未知");
                    item.put("scanCount", entry.getValue());
                    ranking.add(item);
                });
        return ranking;
    }
}
