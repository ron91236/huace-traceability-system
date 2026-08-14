package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import com.huace.trace.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DigitalLabelService {

    private final DlProductMapper productMapper;
    private final DlLabelVersionMapper versionMapper;
    private final DlScanRecordMapper scanRecordMapper;
    private final DlSyncRecordMapper syncRecordMapper;
    private final DlOperationLogMapper operationLogMapper;
    private final DlLoginLogMapper loginLogMapper;
    private final DlFoodCategoryMapper categoryMapper;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    // ==================== 商品管理 ====================

    public PageResult<DlProduct> listProducts(Long enterpriseId, int page, int size,
                                              String barcode, String foodName, String hasLabel,
                                              String startDate, String endDate) {
        LambdaQueryWrapper<DlProduct> w = new LambdaQueryWrapper<>();
        w.eq(DlProduct::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(barcode)) w.like(DlProduct::getBarcode, barcode);
        if (StringUtils.hasText(foodName)) w.like(DlProduct::getFoodName, foodName);
        if ("yes".equals(hasLabel)) w.gt(DlProduct::getLabelVersionCount, 0);
        else if ("no".equals(hasLabel)) w.eq(DlProduct::getLabelVersionCount, 0);
        if (StringUtils.hasText(startDate)) w.ge(DlProduct::getCreatedAt, startDate + " 00:00:00");
        if (StringUtils.hasText(endDate)) w.le(DlProduct::getCreatedAt, endDate + " 23:59:59");
        w.orderByDesc(DlProduct::getId);
        Page<DlProduct> r = productMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public DlProduct createProduct(Long enterpriseId, DlProduct product) {
        if (!StringUtils.hasText(product.getFoodName())) throw new BusinessException("食品名称不能为空");
        if (!StringUtils.hasText(product.getBarcode())) throw new BusinessException("商品条码不能为空");
        Long cnt = productMapper.selectCount(new LambdaQueryWrapper<DlProduct>()
                .eq(DlProduct::getEnterpriseId, enterpriseId)
                .eq(DlProduct::getBarcode, product.getBarcode()));
        if (cnt != null && cnt > 0) throw new BusinessException("该商品条码已存在");
        product.setId(null);
        product.setEnterpriseId(enterpriseId);
        product.setLabelVersionCount(0);
        product.setSyncStatus("synced");
        productMapper.insert(product);
        return product;
    }

    private DlProduct getProductOwned(Long enterpriseId, Long productId) {
        DlProduct p = productMapper.selectById(productId);
        if (p == null || !p.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException("商品不存在");
        }
        return p;
    }

    // ==================== 标签版本管理 ====================

    public List<DlLabelVersion> listVersions(Long enterpriseId, Long productId,
                                             String versionNo, String status,
                                             String startDate, String endDate) {
        DlProduct p = getProductOwned(enterpriseId, productId);
        LambdaQueryWrapper<DlLabelVersion> w = new LambdaQueryWrapper<>();
        w.eq(DlLabelVersion::getProductId, p.getId());
        if (StringUtils.hasText(versionNo)) w.like(DlLabelVersion::getVersionNo, versionNo);
        if (StringUtils.hasText(status)) w.eq(DlLabelVersion::getStatus, status);
        if (StringUtils.hasText(startDate)) w.ge(DlLabelVersion::getCreatedAt, startDate + " 00:00:00");
        if (StringUtils.hasText(endDate)) w.le(DlLabelVersion::getCreatedAt, endDate + " 23:59:59");
        w.orderByDesc(DlLabelVersion::getId);
        List<DlLabelVersion> list = versionMapper.selectList(w);
        list.forEach(this::fillQrCode);
        return list;
    }

    public DlLabelVersion getVersion(Long enterpriseId, Long id) {
        DlLabelVersion v = getOwnedVersion(enterpriseId, id);
        fillQrCode(v);
        return v;
    }

    private DlLabelVersion getOwnedVersion(Long enterpriseId, Long id) {
        DlLabelVersion v = versionMapper.selectById(id);
        if (v == null) throw new BusinessException("标签版本不存在");
        DlProduct p = productMapper.selectById(v.getProductId());
        if (p == null || !p.getEnterpriseId().equals(enterpriseId)) {
            throw new BusinessException("标签版本不存在");
        }
        return v;
    }

    /** 生成版本号：yyyyMMdd-当日序号 */
    private String generateVersionNo(Long enterpriseId) {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<Long> productIds = productMapper.selectList(new LambdaQueryWrapper<DlProduct>()
                        .eq(DlProduct::getEnterpriseId, enterpriseId).select(DlProduct::getId))
                .stream().map(DlProduct::getId).collect(Collectors.toList());
        long count = 0;
        if (!productIds.isEmpty()) {
            count = versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                    .in(DlLabelVersion::getProductId, productIds)
                    .likeRight(DlLabelVersion::getVersionNo, today));
        }
        return today + "-" + String.format("%03d", count + 1);
    }

    @Transactional
    public DlLabelVersion createVersion(Long enterpriseId, Long productId, Long copyFromId, String operator) {
        DlProduct p = getProductOwned(enterpriseId, productId);
        DlLabelVersion v = new DlLabelVersion();
        if (copyFromId != null) {
            DlLabelVersion src = getOwnedVersion(enterpriseId, copyFromId);
            BeanUtils.copyProperties(src, v, "id", "versionNo", "status", "publishedAt",
                    "createdAt", "updatedAt", "qrCode", "scanUrl");
        }
        v.setProductId(p.getId());
        v.setFoodName(p.getFoodName());
        v.setBarcode(p.getBarcode());
        if (!StringUtils.hasText(v.getSpec())) v.setSpec(p.getSpec());
        v.setVersionNo(generateVersionNo(enterpriseId));
        v.setStatus("draft");
        versionMapper.insert(v);
        refreshVersionCount(p.getId());
        recordLog(enterpriseId, p.getFoodName(), v.getVersionNo(), "创建版本", null, v, operator);
        fillQrCode(v);
        return v;
    }

    @Transactional
    public DlLabelVersion updateVersion(Long enterpriseId, Long id, DlLabelVersion data, String operator) {
        DlLabelVersion old = getOwnedVersion(enterpriseId, id);
        if ("published".equals(old.getStatus())) throw new BusinessException("已发布的版本不能编辑，请先下架");
        data.setId(old.getId());
        data.setProductId(old.getProductId());
        data.setVersionNo(old.getVersionNo());
        data.setStatus(old.getStatus());
        data.setPublishedAt(old.getPublishedAt());
        versionMapper.updateById(data);
        DlLabelVersion now = versionMapper.selectById(id);
        DlProduct p = productMapper.selectById(old.getProductId());
        recordLog(enterpriseId, p != null ? p.getFoodName() : null, old.getVersionNo(), "更新版本", old, now, operator);
        fillQrCode(now);
        return now;
    }

    @Transactional
    public void deleteVersion(Long enterpriseId, Long id, String operator) {
        DlLabelVersion v = getOwnedVersion(enterpriseId, id);
        if ("published".equals(v.getStatus())) throw new BusinessException("已发布的版本不能删除，请先下架");
        versionMapper.deleteById(id);
        refreshVersionCount(v.getProductId());
        DlProduct p = productMapper.selectById(v.getProductId());
        recordLog(enterpriseId, p != null ? p.getFoodName() : null, v.getVersionNo(), "删除版本", v, null, operator);
    }

    @Transactional
    public DlLabelVersion publishVersion(Long enterpriseId, Long id, String operator) {
        DlLabelVersion v = getOwnedVersion(enterpriseId, id);
        if ("published".equals(v.getStatus())) throw new BusinessException("版本已发布");
        // 同一商品仅允许一个已发布版本，发布时自动下架其他版本
        versionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DlLabelVersion>()
                .eq(DlLabelVersion::getProductId, v.getProductId())
                .eq(DlLabelVersion::getStatus, "published")
                .set(DlLabelVersion::getStatus, "offline"));
        v.setStatus("published");
        v.setPublishedAt(LocalDateTime.now());
        versionMapper.updateById(v);
        DlProduct p = productMapper.selectById(v.getProductId());
        recordLog(enterpriseId, p != null ? p.getFoodName() : null, v.getVersionNo(), "发布", null, v, operator);
        fillQrCode(v);
        return v;
    }

    @Transactional
    public DlLabelVersion offlineVersion(Long enterpriseId, Long id, String operator) {
        DlLabelVersion v = getOwnedVersion(enterpriseId, id);
        if (!"published".equals(v.getStatus())) throw new BusinessException("版本未发布，无需下架");
        v.setStatus("offline");
        versionMapper.updateById(v);
        DlProduct p = productMapper.selectById(v.getProductId());
        recordLog(enterpriseId, p != null ? p.getFoodName() : null, v.getVersionNo(), "下架", null, v, operator);
        fillQrCode(v);
        return v;
    }

    private void refreshVersionCount(Long productId) {
        long cnt = versionMapper.selectCount(new LambdaQueryWrapper<DlLabelVersion>()
                .eq(DlLabelVersion::getProductId, productId));
        DlProduct p = new DlProduct();
        p.setId(productId);
        p.setLabelVersionCount((int) cnt);
        productMapper.updateById(p);
    }

    private void fillQrCode(DlLabelVersion v) {
        if (v == null || !StringUtils.hasText(v.getBarcode())) return;
        String url = baseUrl + "/dl/scan/" + v.getBarcode();
        v.setScanUrl(url);
        v.setQrCode(QrCodeUtil.generateBase64(url, 300, 300));
    }

    // ==================== 商品同步 ====================

    @Transactional
    public DlSyncRecord manualSync(Long enterpriseId, String condition, String timeRange) {
        LambdaQueryWrapper<DlProduct> w = new LambdaQueryWrapper<>();
        w.eq(DlProduct::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(condition)) {
            w.and(x -> x.like(DlProduct::getFoodName, condition).or().like(DlProduct::getBarcode, condition));
        }
        if (StringUtils.hasText(timeRange) && !"all".equals(timeRange)) {
            int days = parseDays(timeRange);
            w.ge(DlProduct::getUpdatedAt, LocalDateTime.now().minusDays(days));
        }
        long total = productMapper.selectCount(w);
        // 无真实外部平台，Mock 同步：全部视为相同
        DlSyncRecord r = new DlSyncRecord();
        r.setEnterpriseId(enterpriseId);
        r.setSyncType("manual");
        r.setSyncCondition(condition);
        r.setTimeRange(timeRange);
        r.setTotalCount((int) total);
        r.setSameCount((int) total);
        r.setNewCount(0);
        r.setUpdateCount(0);
        r.setStatus("success");
        syncRecordMapper.insert(r);
        return r;
    }

    private int parseDays(String timeRange) {
        switch (timeRange) {
            case "7d": return 7;
            case "14d": return 14;
            case "30d": return 30;
            case "60d": return 60;
            default: return 3650;
        }
    }

    public PageResult<DlSyncRecord> listSyncRecords(Long enterpriseId, int page, int size) {
        LambdaQueryWrapper<DlSyncRecord> w = new LambdaQueryWrapper<>();
        w.eq(DlSyncRecord::getEnterpriseId, enterpriseId).orderByDesc(DlSyncRecord::getId);
        Page<DlSyncRecord> r = syncRecordMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    // ==================== 日志 ====================

    private void recordLog(Long enterpriseId, String productName, String versionCode,
                           String operationType, Object before, Object after, String creator) {
        try {
            DlOperationLog log = new DlOperationLog();
            log.setEnterpriseId(enterpriseId);
            log.setProductName(productName);
            log.setVersionName(versionCode);
            log.setVersionCode(versionCode);
            log.setOperationType(operationType);
            log.setBeforeData(before == null ? null : objectMapper.writeValueAsString(before));
            log.setAfterData(after == null ? null : objectMapper.writeValueAsString(after));
            log.setCreator(creator);
            operationLogMapper.insert(log);
        } catch (Exception ignored) {
            // 日志记录失败不影响主流程
        }
    }

    public PageResult<DlOperationLog> listOperationLogs(Long enterpriseId, int page, int size,
                                                        String productName, String operationType,
                                                        String startDate, String endDate) {
        LambdaQueryWrapper<DlOperationLog> w = new LambdaQueryWrapper<>();
        w.eq(DlOperationLog::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(productName)) w.like(DlOperationLog::getProductName, productName);
        if (StringUtils.hasText(operationType)) w.eq(DlOperationLog::getOperationType, operationType);
        if (StringUtils.hasText(startDate)) w.ge(DlOperationLog::getCreatedAt, startDate + " 00:00:00");
        if (StringUtils.hasText(endDate)) w.le(DlOperationLog::getCreatedAt, endDate + " 23:59:59");
        w.orderByDesc(DlOperationLog::getId);
        Page<DlOperationLog> r = operationLogMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public PageResult<DlLoginLog> listLoginLogs(Long enterpriseId, int page, int size,
                                                String loginType, String startDate, String endDate) {
        LambdaQueryWrapper<DlLoginLog> w = new LambdaQueryWrapper<>();
        w.eq(DlLoginLog::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(loginType)) w.eq(DlLoginLog::getLoginType, loginType);
        if (StringUtils.hasText(startDate)) w.ge(DlLoginLog::getLoginTime, startDate + " 00:00:00");
        if (StringUtils.hasText(endDate)) w.le(DlLoginLog::getLoginTime, endDate + " 23:59:59");
        w.orderByDesc(DlLoginLog::getId);
        Page<DlLoginLog> r = loginLogMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void recordLogin(Long enterpriseId, String username, String userAgent) {
        try {
            DlLoginLog log = new DlLoginLog();
            log.setEnterpriseId(enterpriseId);
            log.setUsername(username);
            String ua = userAgent == null ? "" : userAgent.toLowerCase();
            log.setLoginType(ua.contains("mobile") || ua.contains("android") || ua.contains("iphone") ? "mobile" : "PC");
            log.setCountry("中国");
            log.setLoginTime(LocalDateTime.now());
            loginLogMapper.insert(log);
        } catch (Exception ignored) {
        }
    }

    // ==================== 食品分类 ====================

    public List<DlFoodCategory> categoryTree() {
        List<DlFoodCategory> all = categoryMapper.selectList(
                new LambdaQueryWrapper<DlFoodCategory>().orderByAsc(DlFoodCategory::getSortOrder));
        Map<Long, List<DlFoodCategory>> grouped = all.stream()
                .collect(Collectors.groupingBy(DlFoodCategory::getParentId));
        List<DlFoodCategory> roots = grouped.getOrDefault(0L, List.of());
        roots.forEach(r -> r.setChildren(grouped.getOrDefault(r.getId(), List.of())));
        return roots;
    }

    // ==================== 消费者扫码（公开） ====================

    public Map<String, Object> getScanData(String barcode, String ip, String userAgent) {
        DlLabelVersion v = versionMapper.selectOne(new LambdaQueryWrapper<DlLabelVersion>()
                .eq(DlLabelVersion::getBarcode, barcode)
                .eq(DlLabelVersion::getStatus, "published")
                .orderByDesc(DlLabelVersion::getPublishedAt)
                .last("LIMIT 1"));
        if (v == null) throw new BusinessException("未找到该商品的已发布数字标签");
        // 记录扫码日志
        try {
            DlScanRecord rec = new DlScanRecord();
            rec.setVersionId(v.getId());
            rec.setProductId(v.getProductId());
            rec.setScanTime(LocalDateTime.now());
            rec.setIp(ip);
            rec.setUserAgent(userAgent != null && userAgent.length() > 500 ? userAgent.substring(0, 500) : userAgent);
            scanRecordMapper.insert(rec);
        } catch (Exception ignored) {
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", v);
        result.put("foodImages", splitList(v.getFoodImages()));
        result.put("certificates", splitList(v.getCertificates()));
        result.put("customFields", parseJson(v.getCustomFields()));
        result.put("productionInfo", parseJson(v.getProductionInfo()));
        return result;
    }

    private List<String> splitList(String s) {
        if (!StringUtils.hasText(s)) return List.of();
        return Arrays.asList(s.split(","));
    }

    private Object parseJson(String json) {
        if (!StringUtils.hasText(json)) return List.of();
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return List.of();
        }
    }
}
