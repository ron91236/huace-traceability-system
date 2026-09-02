package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.*;
import com.huace.trace.entity.mongo.CodePackageItemMongo;
import com.huace.trace.mapper.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@lombok.extern.slf4j.Slf4j
public class TracePageService {

    private final CodePackageItemMapper codePackageItemMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final GoodsMapper goodsMapper;
    private final EnterpriseCertMapper certMapper;
    private final CertTypeMapper certTypeMapper;
    private final BatchMapper batchMapper;
    private final EnterpriseBaseMapper baseMapper;
    private final TraceTemplateMapper templateMapper;
    private final ScanRecordMapper scanRecordMapper;
    private final TestReportMapper testReportMapper;
    private final EnterpriseTemplateDataMapper templateDataMapper;
    private final TestReportService testReportService;
    private final ObjectMapper objectMapper;
    private final MongoCodeItemService mongoCodeItemService;
    private final OrderCodeMapper orderCodeMapper;
    private final IpRegionService ipRegionService;
    private final HgzService hgzService;

    /**
     * 清除全部溯源页缓存。
     * 企业信息、模板、商品、批次、证书、订单绑定等数据变更后调用，
     * 确保已绑定的溯源链接扫码时展示最新数据。
     */
    @CacheEvict(value = "tracePage", allEntries = true)
    public void evictAllCache() {
        // 仅用于清空缓存，无实际逻辑
    }

    /**
     * C端溯源查询 - 通过流水号查询完整溯源信息
     */
    public Map<String, Object> queryBySerialNo(String serialNo) {
        return queryBySerialNo(serialNo, null);
    }

    @Cacheable(value = "tracePage", key = "'serial:' + #serialNo + ':v2'")
    public Map<String, Object> queryBySerialNo(String serialNo, HttpServletRequest request) {
        // 1. 优先从 MongoDB 查询码包明细（亿级数据外部存储）
        CodePackageItem item = mongoCodeItemService.findBoundBySerialNo(serialNo)
                .map(this::toCodePackageItem)
                .orElseGet(() -> codePackageItemMapper.selectOne(
                        new LambdaQueryWrapper<CodePackageItem>()
                                .eq(CodePackageItem::getSerialNo, serialNo)
                                .eq(CodePackageItem::getBindStatus, "BOUND")));

        if (item == null) {
            // 尝试查找是否存在但非BOUND状态
            CodePackageItem anyItem = codePackageItemMapper.selectOne(
                    new LambdaQueryWrapper<CodePackageItem>()
                            .eq(CodePackageItem::getSerialNo, serialNo));
            if (anyItem != null) {
                log.warn("溯源码 {} 存在但状态为 {}，非BOUND", serialNo, anyItem.getBindStatus());
                throw new BusinessException("该溯源码尚未激活绑定，流水号：" + serialNo);
            }
            log.warn("溯源码 {} 在 MongoDB 和 MySQL 中均未找到", serialNo);
            throw new BusinessException("未找到该流水号的溯源信息，流水号：" + serialNo);
        }

        // 写入扫码记录(非阻塞)
        try {
            ScanRecord record = new ScanRecord();
            record.setSerialNo(serialNo);
            record.setEnterpriseId(item.getEnterpriseId());
            if (request != null) {
                String ip = getClientIp(request);
                record.setIp(ip);
                record.setUserAgent(request.getHeader("User-Agent"));
                String[] region = ipRegionService.resolve(ip);
                if (region != null) {
                    record.setProvince(region[0]);
                    record.setCity(region[1]);
                }
            }
            scanRecordMapper.insert(record);
        } catch (Exception ignored) {}

        Map<String, Object> result = new HashMap<>();
        result.put("serialNo", item.getSerialNo());
        result.put("antiFakeCode", item.getAntiFakeCode());
        result.put("url", item.getUrl());
        result.put("scanCount", item.getScanCount() != null ? item.getScanCount() : 0);
        // 关联ID（供C端扩展接口使用）
        result.put("_enterpriseId", item.getEnterpriseId());
        result.put("_batchId", item.getBatchId());
        result.put("_goodsId", item.getGoodsId());
        result.put("_certId", item.getCertId());

        // 2. 查询企业信息
        if (item.getEnterpriseId() != null) {
            Enterprise enterprise = enterpriseMapper.selectById(item.getEnterpriseId());
            if (enterprise != null) {
                Map<String, Object> enterpriseInfo = new HashMap<>();
                enterpriseInfo.put("name", enterprise.getName());
                enterpriseInfo.put("introduction", enterprise.getIntroduction());
                enterpriseInfo.put("addressFull", buildFullAddress(enterprise));
                enterpriseInfo.put("contact", enterprise.getContact());
                enterpriseInfo.put("phone", enterprise.getPhone());
                enterpriseInfo.put("creditCode", enterprise.getCreditCode());
                enterpriseInfo.put("licenseImage", enterprise.getLicenseImage());
                enterpriseInfo.put("enterpriseImage", enterprise.getEnterpriseImage());
                enterpriseInfo.put("honors", enterprise.getHonors());
                enterpriseInfo.put("qualifications", enterprise.getQualifications());
                enterpriseInfo.put("mainType", enterprise.getMainType());
                enterpriseInfo.put("promoVideo", enterprise.getPromoVideo());
                enterpriseInfo.put("standardSystem", enterprise.getStandardSystem());
                result.put("enterprise", enterpriseInfo);
            }
        }

        // 3. 查询商品信息
        if (item.getGoodsId() != null) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            if (goods != null) {
                Map<String, Object> goodsInfo = new HashMap<>();
                goodsInfo.put("name", goods.getName());
                goodsInfo.put("introduction", goods.getIntroduction());
                goodsInfo.put("packageSpec", goods.getPackageSpec());
                goodsInfo.put("weightSpec", goods.getWeightSpec());
                goodsInfo.put("sampleImage", goods.getSampleImage());
                goodsInfo.put("storageMethod", goods.getStorageMethod());
                goodsInfo.put("eatingMethod", goods.getEatingMethod());
                goodsInfo.put("promoImage", goods.getPromoImage());
                goodsInfo.put("promoVideo", goods.getPromoVideo());
                result.put("goods", goodsInfo);
            }
        }

        // 4. 查询证书信息
        if (item.getCertId() != null) {
            EnterpriseCert cert = certMapper.selectById(item.getCertId());
            if (cert != null) {
                Map<String, Object> certInfo = new HashMap<>();
                certInfo.put("certName", cert.getCertName());
                certInfo.put("productName", cert.getProductName());
                certInfo.put("startDate", cert.getStartDate());
                certInfo.put("endDate", cert.getEndDate());
                certInfo.put("certImage", cert.getCertImage());
                if (cert.getCertTypeId() != null) {
                    CertType ct = certTypeMapper.selectById(cert.getCertTypeId());
                    certInfo.put("certTypeName", ct != null ? ct.getName() : "");
                }
                result.put("cert", certInfo);
            }
        }

        // 5. 查询批次信息
        if (item.getBatchId() != null) {
            Batch batch = batchMapper.selectById(item.getBatchId());
            if (batch != null) {
                Map<String, Object> batchInfo = new HashMap<>();
                batchInfo.put("name", batch.getName());
                batchInfo.put("goodsSpec", batch.getGoodsSpec());
                batchInfo.put("testCode", batch.getTestCode());
                batchInfo.put("testReport", batch.getTestReport());
                batchInfo.put("testOrg", batch.getTestOrg());
                batchInfo.put("testTime", batch.getTestTime());
                batchInfo.put("testMethod", batch.getTestMethod());
                batchInfo.put("testBasis", batch.getTestBasis());
                batchInfo.put("testType", batch.getTestType());
                batchInfo.put("testResult", batch.getTestResult());
                result.put("batch", batchInfo);

                // 5.1 查询检测报告（多报告）
                List<Map<String, Object>> testReports = testReportService.buildReportListForBatch(batch.getId(), batch.getTestReportId());
                result.put("testReports", testReports);
                result.put("testReportCount", testReports.size());
                // 兼容旧前端：保留单报告字段（不覆盖 batch.testReport 字符串）
                if (!testReports.isEmpty()) {
                    result.put("testReportInfo", testReports.get(0));
                }

                // 6. 查询基地信息（从批次关联）
                if (batch.getBaseId() != null) {
                    EnterpriseBase base = baseMapper.selectById(batch.getBaseId());
                    if (base != null) {
                        Map<String, Object> baseInfo = new HashMap<>();
                        baseInfo.put("name", base.getName());
                        baseInfo.put("code", base.getCode());
                        baseInfo.put("areaDisplay", base.getArea() + " " + base.getUnit());
                        baseInfo.put("manager", base.getManager());
                        baseInfo.put("phone", base.getPhone());
                        baseInfo.put("planImage", base.getPlanImage());
                        baseInfo.put("realImage", base.getRealImage());
                        baseInfo.put("certification", base.getCertification());
                        baseInfo.put("envReport", base.getEnvReport());
                        baseInfo.put("testItems", base.getTestItems());
                        result.put("base", baseInfo);
                    }
                }
            }
        }

        // 5.5 承诺达标合格证（按批次→商品→企业反查最新有效证，无证不注入）
        Map<String, Object> hgzInfo = buildHgzBlock(item.getEnterpriseId(), item.getBatchId(), item.getGoodsId());
        if (hgzInfo != null) result.put("hgz", hgzInfo);

        // 6.5 生产信息（从订单绑定关系获取）
        if (item.getOrderCodeId() != null) {
            OrderCode oc = orderCodeMapper.selectById(item.getOrderCodeId());
            if (oc != null) {
                Map<String, Object> productionInfo = new HashMap<>();
                if (oc.getProductionTime() != null) {
                    productionInfo.put("productionTime",
                            oc.getProductionTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                }
                productionInfo.put("productName", oc.getProductName());
                result.put("production", productionInfo);
            }
        }

        // 7. 查询溯源模板配置
        TraceTemplate template = null;
        if (item.getTraceTemplate() != null) {
            template = templateMapper.selectOne(
                    new LambdaQueryWrapper<TraceTemplate>()
                            .eq(TraceTemplate::getTemplateKey, item.getTraceTemplate())
                            .eq(TraceTemplate::getStatus, 1));
        }

        // 如果没有指定模板，使用默认模板
        if (template == null) {
            template = templateMapper.selectOne(
                    new LambdaQueryWrapper<TraceTemplate>()
                            .eq(TraceTemplate::getTemplateKey, "default")
                            .eq(TraceTemplate::getStatus, 1));
        }

        if (template != null) {
            result.put("traceTemplate", template.getTemplateKey());
            result.put("templateKey", template.getTemplateKey());
            result.put("templateName", template.getTemplateName());
            result.put("backgroundImage", template.getBackgroundImage());
            // 将 configJson 字符串解析为对象返回
            if (template.getConfigJson() != null) {
                try {
                    Map<String, Object> config = objectMapper.readValue(
                            template.getConfigJson(), new TypeReference<Map<String, Object>>() {});
                    result.put("templateConfig", config);

                    // 7.1 查询企业自定义字段数据
                    if (item.getEnterpriseId() != null) {
                        List<EnterpriseTemplateData> customData = templateDataMapper.selectList(
                                new LambdaQueryWrapper<EnterpriseTemplateData>()
                                        .eq(EnterpriseTemplateData::getEnterpriseId, item.getEnterpriseId())
                                        .eq(EnterpriseTemplateData::getTemplateId, template.getId()));
                        if (!customData.isEmpty()) {
                            Map<String, Object> customFields = new HashMap<>();
                            for (EnterpriseTemplateData d : customData) {
                                Map<String, Object> field = new HashMap<>();
                                field.put("label", d.getFieldLabel());
                                field.put("value", d.getFieldValue());
                                field.put("type", d.getFieldType());
                                customFields.put(d.getFieldKey(), field);
                            }
                            result.put("customFields", customFields);
                        }
                    }
                } catch (Exception e) {
                    result.put("configJson", template.getConfigJson());
                }
            }
        }

        return result;
    }

    /**
     * C端批次溯源查询 - 通过批次ID查询溯源信息
     */
    public Map<String, Object> queryByBatchId(Long batchId) {
        Batch batch = batchMapper.selectById(batchId);
        if (batch == null) {
            throw new BusinessException("未找到该批次的溯源信息");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("batchId", batchId);
        result.put("_enterpriseId", batch.getEnterpriseId());
        result.put("_baseId", batch.getBaseId());

        // 1. 查询企业信息
        if (batch.getEnterpriseId() != null) {
            Enterprise enterprise = enterpriseMapper.selectById(batch.getEnterpriseId());
            if (enterprise != null) {
                Map<String, Object> enterpriseInfo = new HashMap<>();
                enterpriseInfo.put("name", enterprise.getName());
                enterpriseInfo.put("introduction", enterprise.getIntroduction());
                enterpriseInfo.put("addressFull", buildFullAddress(enterprise));
                enterpriseInfo.put("contact", enterprise.getContact());
                enterpriseInfo.put("phone", enterprise.getPhone());
                enterpriseInfo.put("creditCode", enterprise.getCreditCode());
                enterpriseInfo.put("licenseImage", enterprise.getLicenseImage());
                enterpriseInfo.put("enterpriseImage", enterprise.getEnterpriseImage());
                enterpriseInfo.put("honors", enterprise.getHonors());
                enterpriseInfo.put("qualifications", enterprise.getQualifications());
                enterpriseInfo.put("mainType", enterprise.getMainType());
                result.put("enterprise", enterpriseInfo);
            }
        }

        // 2. 查询商品信息
        if (batch.getGoodsId() != null) {
            Goods goods = goodsMapper.selectById(batch.getGoodsId());
            if (goods != null) {
                Map<String, Object> goodsInfo = new HashMap<>();
                goodsInfo.put("name", goods.getName());
                goodsInfo.put("introduction", goods.getIntroduction());
                goodsInfo.put("packageSpec", goods.getPackageSpec());
                goodsInfo.put("weightSpec", goods.getWeightSpec());
                goodsInfo.put("sampleImage", goods.getSampleImage());
                goodsInfo.put("storageMethod", goods.getStorageMethod());
                goodsInfo.put("eatingMethod", goods.getEatingMethod());
                goodsInfo.put("promoImage", goods.getPromoImage());
                goodsInfo.put("promoVideo", goods.getPromoVideo());
                result.put("goods", goodsInfo);
            }
        }

        // 3. 查询批次信息
        Map<String, Object> batchInfo = new HashMap<>();
        batchInfo.put("name", batch.getName());
        batchInfo.put("goodsSpec", batch.getGoodsSpec());
        batchInfo.put("testCode", batch.getTestCode());
        batchInfo.put("testReport", batch.getTestReport());
        batchInfo.put("testOrg", batch.getTestOrg());
        batchInfo.put("testTime", batch.getTestTime());
        batchInfo.put("testMethod", batch.getTestMethod());
        batchInfo.put("testBasis", batch.getTestBasis());
        batchInfo.put("testType", batch.getTestType());
        batchInfo.put("testResult", batch.getTestResult());
        result.put("batch", batchInfo);

        // 3.1 检测报告（多报告）
        List<Map<String, Object>> testReports = testReportService.buildReportListForBatch(batch.getId(), batch.getTestReportId());
        result.put("testReports", testReports);
        result.put("testReportCount", testReports.size());
        // 兼容旧前端：保留单报告字段
        if (!testReports.isEmpty()) {
            result.put("testReport", testReports.get(0));
        }

        // 4. 查询基地信息
        if (batch.getBaseId() != null) {
            EnterpriseBase base = baseMapper.selectById(batch.getBaseId());
            if (base != null) {
                Map<String, Object> baseInfo = new HashMap<>();
                baseInfo.put("name", base.getName());
                baseInfo.put("code", base.getCode());
                baseInfo.put("areaDisplay", base.getArea() + " " + base.getUnit());
                baseInfo.put("manager", base.getManager());
                baseInfo.put("phone", base.getPhone());
                baseInfo.put("planImage", base.getPlanImage());
                baseInfo.put("realImage", base.getRealImage());
                baseInfo.put("certification", base.getCertification());
                baseInfo.put("envReport", base.getEnvReport());
                baseInfo.put("testItems", base.getTestItems());
                result.put("base", baseInfo);
            }
        }

        // 5. 承诺达标合格证（批次模式：按批次→商品→企业反查）
        Map<String, Object> hgzInfo = buildHgzBlock(batch.getEnterpriseId(), batchId, batch.getGoodsId());
        if (hgzInfo != null) result.put("hgz", hgzInfo);

        return result;
    }

    /** 反查最新有效合格证，构造溯源页展示块；无证返回 null */
    private Map<String, Object> buildHgzBlock(Long enterpriseId, Long batchId, Long goodsId) {
        com.huace.trace.dto.HgzPublicVO hgz = hgzService.findLatestForTrace(enterpriseId, batchId, goodsId);
        if (hgz == null) return null;
        Map<String, Object> info = new HashMap<>();
        info.put("code", hgz.getCode());
        info.put("userType", hgz.getUserType());
        info.put("productName", hgz.getProductName());
        info.put("number", hgz.getNumber());
        info.put("placeOfOrigin", hgz.getPlaceOfOrigin());
        info.put("promiseUser", hgz.getPromiseUser());
        info.put("useTime", hgz.getUseTime());
        info.put("qrUrl", hgz.getQrUrl());
        info.put("queryUrl", hgz.getQueryUrl());
        return info;
    }

    /**
     * 验证防伪码 - 返回详细结果
     * 支持完整防伪码或后4-6位匹配
     */
    public Map<String, Object> verifyAntiFakeCode(String serialNo, String antiFakeCode) {
        Map<String, Object> result = new HashMap<>();
        CodePackageItem item = mongoCodeItemService.findBySerialNo(serialNo)
                .map(this::toCodePackageItem)
                .orElseGet(() -> codePackageItemMapper.selectOne(
                        new LambdaQueryWrapper<CodePackageItem>()
                                .eq(CodePackageItem::getSerialNo, serialNo)));
        if (item == null) {
            result.put("verified", false);
            result.put("message", "未找到该产品的溯源信息");
            return result;
        }
        boolean match = matchAntiFakeCode(antiFakeCode, item.getAntiFakeCode());
        // 只有防伪码正确时才递增扫码次数
        int scanCount = item.getScanCount() != null ? item.getScanCount() : 0;
        if (match) {
            scanCount++;
            item.setScanCount(scanCount);
            codePackageItemMapper.updateById(item);
            // 同步更新 MongoDB 中的扫码次数
            mongoCodeItemService.updateScanCount(serialNo, scanCount);
        }

        result.put("verified", match);
        result.put("scanCount", scanCount);
        if (match) {
            result.put("message", "验证通过，该产品为正品");
        } else {
            result.put("message", "验证失败，防伪码不匹配，请谨防假冒");
        }
        // 查询企业名称
        if (item.getEnterpriseId() != null) {
            Enterprise enterprise = enterpriseMapper.selectById(item.getEnterpriseId());
            result.put("enterpriseName", enterprise != null ? enterprise.getName() : "");
        }
        // 查询产品名称
        if (item.getGoodsId() != null) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            result.put("productName", goods != null ? goods.getName() : "");
        }
        // 查询认证机构
        if (item.getCertId() != null) {
            EnterpriseCert cert = certMapper.selectById(item.getCertId());
            result.put("certName", cert != null ? cert.getCertName() : "");
        }
        result.put("serialNo", item.getSerialNo());
        result.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        return result;
    }

    /**
     * 扫码即防伪验证 - 不需要防伪码，扫码直接验证并记录次数
     */
    public Map<String, Object> directVerifyBySerialNo(String serialNo) {
        Map<String, Object> result = new HashMap<>();
        CodePackageItem item = mongoCodeItemService.findBySerialNo(serialNo)
                .map(this::toCodePackageItem)
                .orElseGet(() -> codePackageItemMapper.selectOne(
                        new LambdaQueryWrapper<CodePackageItem>()
                                .eq(CodePackageItem::getSerialNo, serialNo)));
        if (item == null) {
            result.put("verified", false);
            result.put("message", "未找到该产品的溯源信息");
            return result;
        }
        // 每次扫码都视为一次有效查询，递增计数
        int scanCount = (item.getScanCount() != null ? item.getScanCount() : 0) + 1;
        item.setScanCount(scanCount);
        codePackageItemMapper.updateById(item);
        mongoCodeItemService.updateScanCount(serialNo, scanCount);

        result.put("verified", true);
        result.put("scanCount", scanCount);
        result.put("message", "该产品已通过防伪验证");
        result.put("serialNo", item.getSerialNo());
        // 查询企业信息
        if (item.getEnterpriseId() != null) {
            Enterprise enterprise = enterpriseMapper.selectById(item.getEnterpriseId());
            result.put("enterpriseName", enterprise != null ? enterprise.getName() : "");
        }
        // 查询产品名称
        if (item.getGoodsId() != null) {
            Goods goods = goodsMapper.selectById(item.getGoodsId());
            result.put("productName", goods != null ? goods.getName() : "");
        }
        // 查询认证机构
        if (item.getCertId() != null) {
            EnterpriseCert cert = certMapper.selectById(item.getCertId());
            result.put("certName", cert != null ? cert.getCertName() : "");
        }
        result.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        return result;
    }

    /**
     * 防伪码匹配规则：
     * 1. 输入长度等于防伪码长度：完全匹配
     * 2. 输入长度 4-6 且小于防伪码长度：匹配防伪码后输入长度位
     * 3. 其他情况：不匹配
     */
    private boolean matchAntiFakeCode(String input, String realCode) {
        if (input == null || input.isEmpty() || realCode == null || realCode.isEmpty()) {
            return false;
        }
        int inputLen = input.length();
        int realLen = realCode.length();
        if (inputLen == realLen) {
            return input.equals(realCode);
        }
        if (inputLen >= 4 && inputLen <= 6 && inputLen < realLen) {
            return realCode.endsWith(input);
        }
        return false;
    }

    private String buildFullAddress(Enterprise e) {
        StringBuilder sb = new StringBuilder();
        if (e.getProvince() != null) sb.append(e.getProvince());
        if (e.getCity() != null) sb.append(e.getCity());
        if (e.getDistrict() != null) sb.append(e.getDistrict());
        if (e.getAddress() != null) sb.append(e.getAddress());
        return sb.toString();
    }

    private CodePackageItem toCodePackageItem(CodePackageItemMongo m) {
        CodePackageItem item = new CodePackageItem();
        item.setId(m.getItemId());
        item.setPackageId(m.getPackageId());
        item.setSerialNo(m.getSerialNo());
        item.setAntiFakeCode(m.getAntiFakeCode());
        item.setUrl(m.getUrl());
        item.setBindStatus(m.getBindStatus());
        item.setEnterpriseId(m.getEnterpriseId());
        item.setGoodsId(m.getGoodsId());
        item.setCertId(m.getCertId());
        item.setBatchId(m.getBatchId());
        item.setTraceTemplate(m.getTraceTemplate());
        item.setOrderCodeId(m.getOrderCodeId());
        item.setBindTime(m.getBindTime());
        item.setScanCount(m.getScanCount());
        item.setCreatedAt(m.getCreatedAt());
        return item;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
