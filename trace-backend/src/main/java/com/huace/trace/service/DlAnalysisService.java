package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.entity.DlLabelVersion;
import com.huace.trace.entity.DlProduct;
import com.huace.trace.entity.DlScanRecord;
import com.huace.trace.mapper.DlLabelVersionMapper;
import com.huace.trace.mapper.DlProductMapper;
import com.huace.trace.mapper.DlScanRecordMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DlAnalysisService {

    private final DlProductMapper productMapper;
    private final DlLabelVersionMapper versionMapper;
    private final DlScanRecordMapper scanRecordMapper;

    /** enterpriseId 为空时返回 null 表示不限企业（避免全表 IN 查询） */
    private List<Long> productIds(Long enterpriseId) {
        if (enterpriseId == null) return null;
        return productMapper.selectList(new LambdaQueryWrapper<DlProduct>()
                        .eq(DlProduct::getEnterpriseId, enterpriseId).select(DlProduct::getId))
                .stream().map(DlProduct::getId).collect(Collectors.toList());
    }

    private long scanCountSince(List<Long> productIds, LocalDateTime start) {
        if (productIds != null && productIds.isEmpty()) return 0;
        LambdaQueryWrapper<DlScanRecord> w = new LambdaQueryWrapper<DlScanRecord>()
                .ge(DlScanRecord::getScanTime, start);
        if (productIds != null) w.in(DlScanRecord::getProductId, productIds);
        Long cnt = scanRecordMapper.selectCount(w);
        return cnt == null ? 0 : cnt;
    }

    private long scanCountBetween(List<Long> productIds, LocalDateTime start, LocalDateTime end) {
        if (productIds != null && productIds.isEmpty()) return 0;
        LambdaQueryWrapper<DlScanRecord> w = new LambdaQueryWrapper<DlScanRecord>()
                .ge(DlScanRecord::getScanTime, start)
                .lt(DlScanRecord::getScanTime, end);
        if (productIds != null) w.in(DlScanRecord::getProductId, productIds);
        Long cnt = scanRecordMapper.selectCount(w);
        return cnt == null ? 0 : cnt;
    }

    /** 补齐缺失日期为0的日趋势 */
    private List<Map<String, Object>> buildTrend(int days, List<Map<String, Object>> rows) {
        Map<String, Object> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            map.put(String.valueOf(row.get("d")), row.get("cnt"));
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = days - 1; i >= 0; i--) {
            String d = today.minusDays(i).format(fmt);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date", d);
            item.put("count", map.getOrDefault(d, 0));
            result.add(item);
        }
        return result;
    }

    private Map<String, Object> scanStats(Long enterpriseId) {
        List<Long> ids = productIds(enterpriseId);
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime yesterdayStart = todayStart.minusDays(1);
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("today", scanCountBetween(ids, todayStart, LocalDateTime.now().plusSeconds(1)));
        stats.put("yesterday", scanCountBetween(ids, yesterdayStart, todayStart));
        stats.put("last7d", scanCountSince(ids, todayStart.minusDays(6)));
        stats.put("last14d", scanCountSince(ids, todayStart.minusDays(13)));
        stats.put("last30d", scanCountSince(ids, todayStart.minusDays(29)));
        stats.put("total", scanCountSince(ids, LocalDateTime.of(2000, 1, 1, 0, 0)));
        return stats;
    }

    // ==================== 工作台首页 ====================

    public Map<String, Object> dashboard(Long enterpriseId, int trendDays) {
        List<Long> ids = productIds(enterpriseId);
        LambdaQueryWrapper<DlProduct> pw = new LambdaQueryWrapper<>();
        if (enterpriseId != null) pw.eq(DlProduct::getEnterpriseId, enterpriseId);
        long productCount = productMapper.selectCount(pw);
        long versionCount = 0, publishedCount = 0;
        Map<String, Long> statusDist = new LinkedHashMap<>();
        statusDist.put("draft", 0L);
        statusDist.put("published", 0L);
        statusDist.put("offline", 0L);
        if (ids == null || !ids.isEmpty()) {
            LambdaQueryWrapper<DlLabelVersion> vw = new LambdaQueryWrapper<>();
            if (ids != null) vw.in(DlLabelVersion::getProductId, ids);
            versionCount = versionMapper.selectCount(vw);
            publishedCount = versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                    .in(ids != null, DlLabelVersion::getProductId, ids)
                    .eq(DlLabelVersion::getStatus, "published"));
            for (String s : statusDist.keySet()) {
                statusDist.put(s, versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                        .in(ids != null, DlLabelVersion::getProductId, ids)
                        .eq(DlLabelVersion::getStatus, s)));
            }
        }
        List<Map<String, Object>> scanTrend = buildTrend(trendDays,
                scanRecordMapper.countByDay(enterpriseId,
                        LocalDate.now().minusDays(trendDays - 1).atStartOfDay()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productCount", productCount);
        result.put("versionCount", versionCount);
        result.put("publishedCount", publishedCount);
        result.put("statusDistribution", statusDist);
        result.put("scanStats", scanStats(enterpriseId));
        result.put("scanTrend", scanTrend);
        result.put("topProducts", scanRecordMapper.topByProduct(enterpriseId, 10));
        return result;
    }

    // ==================== 扫码分析 ====================

    public Map<String, Object> scanAnalysis(Long enterpriseId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stats", scanStats(enterpriseId));
        result.put("list", scanRecordMapper.groupByVersion(enterpriseId));
        return result;
    }

    public List<DlScanRecord> scanDetail(Long enterpriseId, Long versionId) {
        // 校验归属（管理员不限企业）
        DlLabelVersion v = versionMapper.selectById(versionId);
        List<Long> ids = productIds(enterpriseId);
        if (v == null || (ids != null && !ids.contains(v.getProductId()))) {
            return List.of();
        }
        return scanRecordMapper.selectList(new LambdaQueryWrapper<DlScanRecord>()
                .eq(DlScanRecord::getVersionId, versionId)
                .orderByDesc(DlScanRecord::getScanTime)
                .last("LIMIT 200"));
    }

    // ==================== 地域分析 ====================

    public List<Map<String, Object>> geoAnalysis(Long enterpriseId) {
        return scanRecordMapper.countByProvince(enterpriseId);
    }

    // ==================== 标签分析 ====================

    public Map<String, Object> labelAnalysis(Long enterpriseId, int trendDays) {
        List<Long> ids = productIds(enterpriseId);
        long versionCount = 0, publishedCount = 0, newYesterday = 0, new7d = 0, new30d = 0;
        Map<String, Long> statusDist = new LinkedHashMap<>();
        statusDist.put("draft", 0L);
        statusDist.put("published", 0L);
        statusDist.put("offline", 0L);
        if (ids == null || !ids.isEmpty()) {
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            versionCount = versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                    .in(ids != null, DlLabelVersion::getProductId, ids));
            publishedCount = versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                    .in(ids != null, DlLabelVersion::getProductId, ids).eq(DlLabelVersion::getStatus, "published"));
            newYesterday = versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                    .in(ids != null, DlLabelVersion::getProductId, ids)
                    .ge(DlLabelVersion::getCreatedAt, todayStart.minusDays(1))
                    .lt(DlLabelVersion::getCreatedAt, todayStart));
            new7d = versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                    .in(ids != null, DlLabelVersion::getProductId, ids)
                    .ge(DlLabelVersion::getCreatedAt, todayStart.minusDays(6)));
            new30d = versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                    .in(ids != null, DlLabelVersion::getProductId, ids)
                    .ge(DlLabelVersion::getCreatedAt, todayStart.minusDays(29)));
            for (String s : statusDist.keySet()) {
                statusDist.put(s, versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                        .in(ids != null, DlLabelVersion::getProductId, ids).eq(DlLabelVersion::getStatus, s)));
            }
        }
        List<Map<String, Object>> trend = buildTrend(trendDays,
                versionMapper.countByDay(enterpriseId,
                        LocalDate.now().minusDays(trendDays - 1).atStartOfDay()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("versionCount", versionCount);
        result.put("publishedCount", publishedCount);
        result.put("newYesterday", newYesterday);
        result.put("new7d", new7d);
        result.put("new30d", new30d);
        result.put("statusDistribution", statusDist);
        result.put("trend", trend);
        return result;
    }

    // ==================== 商品分析 ====================

    public Map<String, Object> productAnalysis(Long enterpriseId, int trendDays) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long productCount = productMapper.selectCount(new LambdaQueryWrapper<DlProduct>()
                .eq(enterpriseId != null, DlProduct::getEnterpriseId, enterpriseId));
        long newYesterday = productMapper.selectCount(new LambdaQueryWrapper<DlProduct>()
                .eq(enterpriseId != null, DlProduct::getEnterpriseId, enterpriseId)
                .ge(DlProduct::getCreatedAt, todayStart.minusDays(1))
                .lt(DlProduct::getCreatedAt, todayStart));
        long new7d = productMapper.selectCount(new LambdaQueryWrapper<DlProduct>()
                .eq(enterpriseId != null, DlProduct::getEnterpriseId, enterpriseId)
                .ge(DlProduct::getCreatedAt, todayStart.minusDays(6)));
        long new14d = productMapper.selectCount(new LambdaQueryWrapper<DlProduct>()
                .eq(enterpriseId != null, DlProduct::getEnterpriseId, enterpriseId)
                .ge(DlProduct::getCreatedAt, todayStart.minusDays(13)));
        long new30d = productMapper.selectCount(new LambdaQueryWrapper<DlProduct>()
                .eq(enterpriseId != null, DlProduct::getEnterpriseId, enterpriseId)
                .ge(DlProduct::getCreatedAt, todayStart.minusDays(29)));
        List<Map<String, Object>> trend = buildTrend(trendDays,
                productMapper.countByDay(enterpriseId,
                        LocalDate.now().minusDays(trendDays - 1).atStartOfDay()
                                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("productCount", productCount);
        result.put("newYesterday", newYesterday);
        result.put("new7d", new7d);
        result.put("new14d", new14d);
        result.put("new30d", new30d);
        result.put("trend", trend);
        return result;
    }
}
