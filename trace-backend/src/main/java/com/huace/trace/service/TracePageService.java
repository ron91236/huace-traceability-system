package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
    private final LongModuleService longModuleService;
    /**
     * 放在最后：@RequiredArgsConstructor 的字段顺序就是构造参数顺序。
     * configReader 供落地页骨架读取，另两个是落地页内容与企业外链。
     */
    private final TemplateConfigReader configReader;
    private final LandingContentService landingContentService;
    private final SideLinkService sideLinkService;

    /** 判定"企业在落地页到底填过东西没有"时算数的区块。antiFake 不在列：它从不写内容行 */
    private static final List<String> LANDING_CONTENT_KEYS = List.of("richtext", "video", "promoImages");

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
     * C端溯源查询 - 码绑定的默认批次（视频/IoT 等扩展接口使用）
     */
    public Map<String, Object> queryBySerialNo(String serialNo) {
        return queryBySerialNo(serialNo, null, null);
    }

    /**
     * C端溯源查询 - 通过流水号查询完整溯源信息
     * batchId 非空且企业已开通批次切换定制时，切换到同企业同商品的指定批次
     * （检测报告、生产日期、基地、合格证随之变化）；未开通的企业忽略 batchId，始终展示码绑定的批次。
     * 缓存 key 含 batchId，不同批次的展示结果互不污染。
     */
    // v7：长图取数从"企业 + 商品"换成"模板 + 企业"，缓存的形状与含义都变了，
    // 版本号必须跟着走，否则灰度期间旧缓存会继续按商品维度供数据。
    @Cacheable(value = "tracePage", key = "'serial:' + #serialNo + ':b' + (#batchId == null ? 0 : #batchId) + ':v7'")
    public Map<String, Object> queryBySerialNo(String serialNo, Long batchId, HttpServletRequest request) {
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

        // 写入扫码记录(非阻塞)；切换批次属于同一次查看，不重复计数
        if (batchId == null) {
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
        }

        Map<String, Object> result = new HashMap<>();
        result.put("serialNo", item.getSerialNo());
        result.put("antiFakeCode", item.getAntiFakeCode());
        result.put("url", item.getUrl());
        result.put("scanCount", item.getScanCount() != null ? item.getScanCount() : 0);
        // 关联ID（供C端扩展接口使用）
        result.put("_enterpriseId", item.getEnterpriseId());
        result.put("_goodsId", item.getGoodsId());
        result.put("_certId", item.getCertId());

        // 2. 查询企业信息
        Enterprise enterprise = item.getEnterpriseId() == null ? null : enterpriseMapper.selectById(item.getEnterpriseId());
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
            enterpriseInfo.put("antiFakeInputHint", enterprise.getAntiFakeInputHint());
            result.put("enterprise", enterpriseInfo);
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

        // 5. 批次信息（已开通批次切换定制的企业可切换到同企业同商品的其他批次）
        Batch batch = item.getBatchId() == null ? null : batchMapper.selectById(item.getBatchId());
        if (batchId != null && !batchId.equals(item.getBatchId()) && isBatchSwitchEnabled(enterprise)) {
            Batch chosen = batchMapper.selectById(batchId);
            if (!isBatchOfGoods(chosen, item)) {
                throw new BusinessException("所选批次不属于该产品，请重新选择");
            }
            batch = chosen;
        }
        Long targetBatchId = batch == null ? item.getBatchId() : batch.getId();
        result.put("_batchId", targetBatchId);
        if (batch != null) {
            applyBatchBlock(result, batch, "testReportInfo");
        }

        // 5.5 承诺达标合格证（按批次→商品→企业反查最新有效证，无证不注入）
        Map<String, Object> hgzInfo = buildHgzBlock(item.getEnterpriseId(), targetBatchId, item.getGoodsId());
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

                    // 7.0 长图模板：模块骨架来自这个模板的 config_json，页图来自企业，
                    // 模板行直接传下去省一次回查（P2 起不再有商品级回落）
                    if ("long-image".equals(config.get("renderMode")) && item.getEnterpriseId() != null) {
                        result.put("longModules", longModuleService.resolveForTrace(template, item.getEnterpriseId()));
                    }

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
                    // 这个 catch 把 templateConfig、longModules、customFields 一起吞掉：页面照常 200，
                    // 长图企业却静默回落成默认版式，降级结果还会被缓存 5 分钟。不留日志等于线上查不到。
                    log.warn("模板配置处理失败, templateKey={}", template.getTemplateKey(), e);
                    result.put("configJson", template.getConfigJson());
                }
            }
        }

        return result;
    }

    /**
     * 消费者静态落地页取数（免登录）。一次给足 LandingView 需要的东西：
     * 模板骨架（区块 / sideMenu / gates）+ 企业内容 + 企业外链 + 详情页跳转目标。
     *
     * <p>模板不是 landing 时不抛错，只回 isLanding=false 加 traceUrl，由前端 302 到 /trace/{sn}：
     * "落地页模板还没配好"与"这个码本来就不是落地页"对消费者是同一件事——都该直接看详情。
     *
     * <p>刻意不挂 {@code @Cacheable}：企业内容与外链一改就要立刻生效，
     * 而落地页访问量远小于详情页，缓存省下的那点查询不值"改了看不到"的排障成本。
     */
    public Map<String, Object> queryLanding(String serialNo, HttpServletRequest request) {
        CodePackageItem item = mongoCodeItemService.findBoundBySerialNo(serialNo)
                .map(this::toCodePackageItem)
                .orElseGet(() -> codePackageItemMapper.selectOne(
                        new LambdaQueryWrapper<CodePackageItem>()
                                .eq(CodePackageItem::getSerialNo, serialNo)
                                .eq(CodePackageItem::getBindStatus, "BOUND")));
        if (item == null) throw new BusinessException("该溯源码尚未激活绑定，流水号：" + serialNo);

        Map<String, Object> result = new HashMap<>();
        result.put("serialNo", serialNo);
        result.put("enterpriseId", item.getEnterpriseId());
        result.put("traceUrl", "/trace/" + serialNo);

        TraceTemplate template = null;
        if (item.getTraceTemplate() != null) {
            template = templateMapper.selectOne(new LambdaQueryWrapper<TraceTemplate>()
                    .eq(TraceTemplate::getTemplateKey, item.getTraceTemplate())
                    .eq(TraceTemplate::getStatus, 1));
        }
        if (template == null) {
            template = templateMapper.selectOne(new LambdaQueryWrapper<TraceTemplate>()
                    .eq(TraceTemplate::getTemplateKey, "default")
                    .eq(TraceTemplate::getStatus, 1));
        }

        String configJson = template == null ? null : template.getConfigJson();
        String mode = configReader.renderModeOf(configJson);
        boolean isLanding = TemplateGrantGuard.LANDING.equals(mode);
        result.put("isLanding", isLanding);
        result.put("renderMode", mode);
        if (template != null) {
            result.put("templateKey", template.getTemplateKey());
            result.put("templateName", template.getTemplateName());
            result.put("backgroundImage", template.getBackgroundImage());
        }
        if (!isLanding) return result;

        result.put("blocks", landingContentService.resolveForLanding(template, item.getEnterpriseId()));
        result.put("sideMenu", configReader.sideMenuOf(configJson));
        result.put("gates", configReader.gatesOf(configJson));
        // 生效后的展示开关（标题/防伪/侧栏溯源详情/底部入口）：模板允许的与企业没关的取交集，
        // 前端只看这一份，不必再去比对 sideMenu 与 blocks 推"这块到底该不该出现"
        result.put("display", landingContentService.effectiveDisplay(template, item.getEnterpriseId()));
        result.put("sideLinks", sideLinkService.resolveForConsumer(item.getEnterpriseId()));
        // 只有企业真填过这三项之一才算"有内容可看"。antiFake 从不写内容行，
        // 把它算进来会让"填了富文本却仍被判为空"的页被错误地 302 掉（spec §12）
        boolean filled = ((List<?>) result.get("blocks")).stream()
                .filter(b -> b instanceof Map)
                .map(b -> (Map<?, ?>) b)
                .anyMatch(b -> LANDING_CONTENT_KEYS.contains(String.valueOf(b.get("key")))
                        && b.get("content") != null && !String.valueOf(b.get("content")).isBlank());
        result.put("contentEmpty", !filled);
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

        // 3. 批次信息 + 检测报告 + 基地
        applyBatchBlock(result, batch, "testReport");

        // 5. 承诺达标合格证（批次模式：按批次→商品→企业反查）
        Map<String, Object> hgzInfo = buildHgzBlock(batch.getEnterpriseId(), batchId, batch.getGoodsId());
        if (hgzInfo != null) result.put("hgz", hgzInfo);

        return result;
    }

    /**
     * 批次维度展示块：批次字段、检测报告列表、所属基地。
     * 流水号模式（含批次切换）与批次模式共用，legacyReportKey 兼容两种旧前端字段名。
     */
    private void applyBatchBlock(Map<String, Object> result, Batch batch, String legacyReportKey) {
        Map<String, Object> batchInfo = new HashMap<>();
        batchInfo.put("id", batch.getId());
        batchInfo.put("name", batch.getName());
        batchInfo.put("goodsSpec", batch.getGoodsSpec());
        batchInfo.put("productionDate", batch.getProductionDate() != null ? batch.getProductionDate().toString() : null);
        batchInfo.put("testCode", batch.getTestCode());
        batchInfo.put("testReport", batch.getTestReport());
        batchInfo.put("testOrg", batch.getTestOrg());
        batchInfo.put("testTime", batch.getTestTime());
        batchInfo.put("testMethod", batch.getTestMethod());
        batchInfo.put("testBasis", batch.getTestBasis());
        batchInfo.put("testType", batch.getTestType());
        batchInfo.put("testResult", batch.getTestResult());
        result.put("batch", batchInfo);

        List<Map<String, Object>> testReports = testReportService.buildReportListForBatch(batch.getId(), batch.getTestReportId());
        result.put("testReports", testReports);
        result.put("testReportCount", testReports.size());
        if (!testReports.isEmpty()) {
            result.put(legacyReportKey, testReports.get(0));
        }

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

    /**
     * C端批次切换列表 - 该溯源码可展示的同企业同商品批次，按生产日期倒序（无生产日期排最后）
     * 未开通批次切换定制的企业返回空列表，前端因此不渲染切换器。
     */
    public List<Map<String, Object>> listBatchesBySerialNo(String serialNo) {
        CodePackageItem item = mongoCodeItemService.findBoundBySerialNo(serialNo)
                .map(this::toCodePackageItem)
                .orElseGet(() -> codePackageItemMapper.selectOne(
                        new LambdaQueryWrapper<CodePackageItem>()
                                .eq(CodePackageItem::getSerialNo, serialNo)
                                .eq(CodePackageItem::getBindStatus, "BOUND")));
        if (item == null || item.getGoodsId() == null || item.getEnterpriseId() == null) {
            return List.of();
        }
        Enterprise enterprise = enterpriseMapper.selectById(item.getEnterpriseId());
        if (!isBatchSwitchEnabled(enterprise)) {
            return List.of();
        }
        List<Batch> batches = batchMapper.selectList(new LambdaQueryWrapper<Batch>()
                .eq(Batch::getEnterpriseId, item.getEnterpriseId())
                .eq(Batch::getGoodsId, item.getGoodsId())
                .orderByDesc(Batch::getProductionDate)
                .orderByDesc(Batch::getId));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Batch b : batches) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", b.getId());
            m.put("name", b.getName());
            m.put("goodsSpec", b.getGoodsSpec());
            m.put("productionDate", b.getProductionDate() != null ? b.getProductionDate().toString() : null);
            m.put("testResult", b.getTestResult());
            m.put("current", b.getId().equals(item.getBatchId()));
            list.add(m);
        }
        return list;
    }

    /** 批次切换属于企业个性化定制能力，需管理端为企业开通 */
    private boolean isBatchSwitchEnabled(Enterprise enterprise) {
        return enterprise != null && Integer.valueOf(1).equals(enterprise.getBatchSwitchEnabled());
    }

    /** 校验切换目标批次确实属于该码绑定的企业与商品 */
    private boolean isBatchOfGoods(Batch batch, CodePackageItem item) {
        if (batch == null || item.getGoodsId() == null || item.getEnterpriseId() == null) return false;
        return item.getGoodsId().equals(batch.getGoodsId())
                && item.getEnterpriseId().equals(batch.getEnterpriseId());
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
            bumpScanCount(serialNo, scanCount);
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
        bumpScanCount(serialNo, scanCount);
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
        result.put("queryTime", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")));
        return result;
    }

    /**
     * 验证只推进扫码计数，必须用定向更新而不是整行 updateById：这两条验证路径先读 MongoDB 的码拷贝，
     * 而绑定关系只写 MySQL、从不写 MongoDB，那份拷贝至今还是 bind_status=UNBOUND 且关联 id 全空；
     * NOT_NULL 策略会把这身过期状态写回 MySQL，等于消费者验一次就把企业已激活的码解绑。
     */
    private void bumpScanCount(String serialNo, int scanCount) {
        codePackageItemMapper.update(null, new LambdaUpdateWrapper<CodePackageItem>()
                .eq(CodePackageItem::getSerialNo, serialNo)
                .set(CodePackageItem::getScanCount, scanCount));
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
