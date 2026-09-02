package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.dto.HgzPublicVO;
import com.huace.trace.dto.HgzSaveRequest;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import com.huace.trace.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 农产品承诺达标合格证服务。
 * 政策依据：《农产品质量安全承诺达标合格证管理办法》农业农村部令2025年第4号。
 * 合格证由生产经营者自行开具，平台仅提供载体；开证时服务端从批次/商品/企业数据自动带出法定要素。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HgzService {

    private final HgzMapper hgzMapper;
    private final BatchMapper batchMapper;
    private final GoodsMapper goodsMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseBaseMapper baseMapper;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final DateTimeFormatter CODE_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");

    /** 默认承诺事项（法定要素，开证时企业需逐项确认） */
    public static final List<String> DEFAULT_PROMISE_ITEMS = List.of(
            "不使用禁用的农药兽药、停用兽药及非法添加物",
            "遵守农药安全间隔期、兽药休药期规定",
            "销售的食用农产品符合农药兽药残留食品安全国家标准");
    /** 默认承诺依据选项 */
    public static final List<String> DEFAULT_BASIS_ITEMS = List.of(
            "自我承诺",
            "委托检测合格",
            "自我检测合格",
            "质量安全内部控制合格");

    // ==================== 默认选项 ====================

    public Map<String, Object> defaults() {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> promiseItems = new ArrayList<>();
        for (String title : DEFAULT_PROMISE_ITEMS) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", title);
            item.put("isSelect", true);
            promiseItems.add(item);
        }
        List<Map<String, Object>> basisItems = new ArrayList<>();
        for (String title : DEFAULT_BASIS_ITEMS) {
            Map<String, Object> item = new HashMap<>();
            item.put("title", title);
            item.put("isSelect", false);
            basisItems.add(item);
        }
        result.put("promiseItems", promiseItems);
        result.put("basisItems", basisItems);
        return result;
    }

    // ==================== 企业端/管理端 ====================

    /** 列表：企业用户看自己的，管理员可全局筛选（enterpriseId 为空=全部） */
    public PageResult<Hgz> list(Long scopeEnterpriseId, int page, int size, String keyword) {
        LambdaQueryWrapper<Hgz> w = new LambdaQueryWrapper<>();
        if (scopeEnterpriseId != null) w.eq(Hgz::getEnterpriseId, scopeEnterpriseId);
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(Hgz::getCode, keyword)
                    .or().like(Hgz::getProductName, keyword)
                    .or().like(Hgz::getPromiseUser, keyword));
        }
        w.orderByDesc(Hgz::getId);
        Page<Hgz> r = hgzMapper.selectPage(new Page<>(page, size), w);
        List<Hgz> records = r.getRecords();
        if (!records.isEmpty()) {
            Set<Long> batchIds = new HashSet<>();
            for (Hgz h : records) {
                if (h.getBatchId() != null) batchIds.add(h.getBatchId());
            }
            Map<Long, String> batchNames = new HashMap<>();
            if (!batchIds.isEmpty()) {
                for (Batch b : batchMapper.selectBatchIds(batchIds)) {
                    batchNames.put(b.getId(), b.getName());
                }
            }
            for (Hgz h : records) {
                if (h.getBatchId() != null) h.setBatchName(batchNames.get(h.getBatchId()));
                parseJson(h);
            }
        }
        return new PageResult<>(records, r.getTotal());
    }

    public Hgz detail(Long id, Long scopeEnterpriseId, boolean isAdmin) {
        Hgz h = isAdmin ? hgzMapper.selectById(id) : requireOwned(id, scopeEnterpriseId);
        if (h == null) throw new BusinessException("合格证不存在");
        parseJson(h);
        return h;
    }

    /** 开证：自动带出批次/商品/企业数据，生成证号、链接 */
    public Hgz create(HgzSaveRequest req, Long enterpriseId) {
        Hgz h = new Hgz();
        h.setEnterpriseId(enterpriseId);
        applyRequest(h, req);
        // 从批次带出：商品、产地(基地)、溯源回链
        if (req.getBatchId() != null) {
            Batch batch = batchMapper.selectById(req.getBatchId());
            if (batch == null) throw new BusinessException("批次不存在");
            if (!enterpriseId.equals(batch.getEnterpriseId())) throw new BusinessException("无权使用该批次");
            h.setBatchId(batch.getId());
            if (h.getGoodsId() == null) h.setGoodsId(batch.getGoodsId());
            if (!StringUtils.hasText(h.getQueryUrl())) {
                h.setQueryUrl(baseUrl + "/trace/batch/" + batch.getId());
            }
            if (!StringUtils.hasText(h.getPlaceOfOrigin()) && batch.getBaseId() != null) {
                EnterpriseBase base = baseMapper.selectById(batch.getBaseId());
                if (base != null) h.setPlaceOfOrigin(base.getName());
            }
        }
        // 从商品带出产品名称
        if (h.getGoodsId() != null && !StringUtils.hasText(h.getProductName())) {
            Goods goods = goodsMapper.selectById(h.getGoodsId());
            if (goods != null) h.setProductName(goods.getName());
        }
        // 从企业带出主体/联系方式/产地
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise != null) {
            if (!StringUtils.hasText(h.getPromiseUser())) h.setPromiseUser(enterprise.getName());
            if (!StringUtils.hasText(h.getContact())) {
                h.setContact(StringUtils.hasText(enterprise.getContact()) ? enterprise.getContact() : enterprise.getPhone());
            }
            if (!StringUtils.hasText(h.getPlaceOfOrigin())) {
                h.setPlaceOfOrigin(joinOrigin(enterprise));
            }
        }
        validate(h);
        h.setCode(generateCode());
        h.setStatus(1);
        h.setQrUrl(baseUrl + "/hgz/" + h.getCode());
        serializeJson(h);
        hgzMapper.insert(h);
        parseJson(h);
        evictTracePage();
        return h;
    }

    /** 编辑：仅企业可改，作废后不可改；更新后清除公开页缓存 */
    @CacheEvict(value = "hgz", key = "'code:' + #result.code")
    public Hgz update(Long id, HgzSaveRequest req, Long enterpriseId) {
        Hgz h = requireOwned(id, enterpriseId);
        if (!Integer.valueOf(1).equals(h.getStatus())) throw new BusinessException("该合格证已作废，不可编辑");
        applyRequest(h, req);
        if (req.getBatchId() != null) {
            Batch batch = batchMapper.selectById(req.getBatchId());
            if (batch == null) throw new BusinessException("批次不存在");
            if (!enterpriseId.equals(batch.getEnterpriseId())) throw new BusinessException("无权使用该批次");
            h.setBatchId(batch.getId());
            if (h.getGoodsId() == null) h.setGoodsId(batch.getGoodsId());
            if (!StringUtils.hasText(h.getQueryUrl())) {
                h.setQueryUrl(baseUrl + "/trace/batch/" + batch.getId());
            }
        }
        validate(h);
        serializeJson(h);
        hgzMapper.updateById(h);
        parseJson(h);
        evictTracePage();
        return h;
    }

    /** 作废：企业作废自己的，管理员可作废任意（监管）；清除公开页缓存 */
    @CacheEvict(value = "hgz", key = "'code:' + #result.code")
    public Hgz voidCert(Long id, Long scopeEnterpriseId, boolean isAdmin) {
        Hgz h = hgzMapper.selectById(id);
        if (h == null) throw new BusinessException("合格证不存在");
        if (!isAdmin && !scopeEnterpriseId.equals(h.getEnterpriseId())) throw new BusinessException("无权限操作");
        if (!Integer.valueOf(1).equals(h.getStatus())) throw new BusinessException("该合格证已作废");
        Hgz u = new Hgz();
        u.setId(h.getId());
        u.setStatus(0);
        hgzMapper.updateById(u);
        h.setStatus(0);
        evictTracePage();
        return h;
    }

    /** 开证/作废会影响溯源页展示的合格证卡片，直接清溯源页缓存（避免与 TracePageService 循环依赖） */
    private void evictTracePage() {
        try {
            Cache cache = cacheManager.getCache("tracePage");
            if (cache != null) cache.clear();
        } catch (Exception e) {
            log.warn("清除溯源页缓存失败", e);
        }
    }

    public String qrcode(Long id, Long scopeEnterpriseId, boolean isAdmin) {
        Hgz h = isAdmin ? hgzMapper.selectById(id) : requireOwned(id, scopeEnterpriseId);
        if (h == null) throw new BusinessException("合格证不存在");
        String url = StringUtils.hasText(h.getQrUrl()) ? h.getQrUrl() : baseUrl + "/hgz/" + h.getCode();
        return QrCodeUtil.generateBase64(url, 400, 400);
    }

    // ==================== 公开查询 ====================

    /** 公开查询：5分钟缓存；作废证也返回（页面展示作废态） */
    @Cacheable(value = "hgz", key = "'code:' + #code")
    public HgzPublicVO publicDetail(String code) {
        Hgz h = hgzMapper.selectOne(new LambdaQueryWrapper<Hgz>().eq(Hgz::getCode, code));
        if (h == null) throw new BusinessException("未找到该合格证，证号：" + code);
        HgzPublicVO vo = new HgzPublicVO();
        vo.setCode(h.getCode());
        vo.setUserType(h.getUserType());
        vo.setProductName(h.getProductName());
        vo.setNumber(h.getNumber());
        vo.setPlaceOfOrigin(h.getPlaceOfOrigin());
        vo.setPromiseUser(h.getPromiseUser());
        vo.setContact(h.getContact());
        vo.setUseTime(h.getUseTime());
        vo.setSignature(h.getSignature());
        vo.setIsShowEnterprise(h.getIsShowEnterprise());
        vo.setStatus(h.getStatus());
        vo.setQrUrl(h.getQrUrl());
        vo.setQueryUrl(h.getQueryUrl());
        vo.setBatchId(h.getBatchId());
        vo.setGoodsId(h.getGoodsId());
        vo.setPromiseList(parseList(h.getPromiseList()));
        vo.setBasisList(parseList(h.getBasisList()));
        if (h.getBatchId() != null) {
            Batch batch = batchMapper.selectById(h.getBatchId());
            if (batch != null) vo.setBatchName(batch.getName());
        }
        if (h.getEnterpriseId() != null) {
            Enterprise e = enterpriseMapper.selectById(h.getEnterpriseId());
            if (e != null) {
                vo.setEnterpriseName(e.getName());
                vo.setEnterpriseIntroduction(e.getIntroduction());
                vo.setEnterpriseImage(e.getEnterpriseImage());
                vo.setEnterpriseSealImage(e.getSealImage());
            }
        }
        return vo;
    }

    /** 溯源页反查：按批次→商品→企业顺序找最新有效合格证 */
    public HgzPublicVO findLatestForTrace(Long enterpriseId, Long batchId, Long goodsId) {
        Hgz h = null;
        if (batchId != null) {
            h = hgzMapper.selectOne(new LambdaQueryWrapper<Hgz>()
                    .eq(Hgz::getBatchId, batchId)
                    .eq(Hgz::getStatus, 1)
                    .orderByDesc(Hgz::getId)
                    .last("LIMIT 1"));
        }
        if (h == null && goodsId != null) {
            h = hgzMapper.selectOne(new LambdaQueryWrapper<Hgz>()
                    .eq(Hgz::getGoodsId, goodsId)
                    .eq(Hgz::getStatus, 1)
                    .orderByDesc(Hgz::getId)
                    .last("LIMIT 1"));
        }
        if (h == null && enterpriseId != null) {
            h = hgzMapper.selectOne(new LambdaQueryWrapper<Hgz>()
                    .eq(Hgz::getEnterpriseId, enterpriseId)
                    .eq(Hgz::getStatus, 1)
                    .orderByDesc(Hgz::getId)
                    .last("LIMIT 1"));
        }
        if (h == null) return null;
        return publicDetail(h.getCode());
    }

    // ==================== 内部工具 ====================

    private Hgz requireOwned(Long id, Long scopeEnterpriseId) {
        Hgz h = hgzMapper.selectById(id);
        if (h == null) throw new BusinessException("合格证不存在");
        if (!scopeEnterpriseId.equals(h.getEnterpriseId())) throw new BusinessException("无权限操作");
        return h;
    }

    private void applyRequest(Hgz h, HgzSaveRequest req) {
        if (req.getBatchId() != null) h.setBatchId(req.getBatchId());
        if (req.getGoodsId() != null) h.setGoodsId(req.getGoodsId());
        if (req.getUserType() != null) h.setUserType(req.getUserType());
        if (StringUtils.hasText(req.getProductName())) h.setProductName(req.getProductName().trim());
        if (StringUtils.hasText(req.getNumber())) h.setNumber(req.getNumber().trim());
        if (StringUtils.hasText(req.getPlaceOfOrigin())) h.setPlaceOfOrigin(req.getPlaceOfOrigin().trim());
        if (StringUtils.hasText(req.getPromiseUser())) h.setPromiseUser(req.getPromiseUser().trim());
        if (StringUtils.hasText(req.getContact())) h.setContact(req.getContact().trim());
        if (StringUtils.hasText(req.getUseTime())) {
            try {
                h.setUseTime(LocalDate.parse(req.getUseTime().trim()));
            } catch (Exception e) {
                throw new BusinessException("开具日期格式不正确");
            }
        }
        if (h.getUseTime() == null) h.setUseTime(LocalDate.now());
        if (req.getSignature() != null) h.setSignature(req.getSignature());
        if (req.getPromiseList() != null) h.setPromiseItems(new ArrayList<>(req.getPromiseList()));
        if (req.getBasisList() != null) h.setBasisItems(new ArrayList<>(req.getBasisList()));
        if (req.getIsShowEnterprise() != null) h.setIsShowEnterprise(req.getIsShowEnterprise());
        if (h.getUserType() == null) h.setUserType(1);
        if (h.getIsShowEnterprise() == null) h.setIsShowEnterprise(1);
    }

    private void validate(Hgz h) {
        if (!StringUtils.hasText(h.getProductName())) throw new BusinessException("产品名称不能为空");
        if (!StringUtils.hasText(h.getPromiseUser())) throw new BusinessException("承诺主体不能为空");
        if (h.getPromiseItems() == null || h.getPromiseItems().isEmpty()) throw new BusinessException("请至少勾选一项承诺事项");
        boolean hasSelected = h.getPromiseItems().stream()
                .anyMatch(m -> Boolean.TRUE.equals(m.get("isSelect")));
        if (!hasSelected) throw new BusinessException("请至少勾选一项承诺事项");
        boolean hasBasis = h.getBasisItems() != null && h.getBasisItems().stream()
                .anyMatch(m -> Boolean.TRUE.equals(m.get("isSelect")));
        if (!hasBasis) throw new BusinessException("请至少选择一项承诺依据");
    }

    private String generateCode() {
        for (int i = 0; i < 5; i++) {
            StringBuilder suffix = new StringBuilder(4);
            for (int j = 0; j < 4; j++) {
                suffix.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
            }
            String code = "HGZ" + LocalDate.now().format(CODE_DATE) + "-" + suffix;
            if (hgzMapper.selectCount(new LambdaQueryWrapper<Hgz>().eq(Hgz::getCode, code)) == 0) {
                return code;
            }
        }
        throw new BusinessException("证号生成失败，请重试");
    }

    private String joinOrigin(Enterprise e) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(e.getProvince())) sb.append(e.getProvince());
        if (StringUtils.hasText(e.getCity())) sb.append(e.getCity());
        if (StringUtils.hasText(e.getDistrict())) sb.append(e.getDistrict());
        return sb.toString();
    }

    private void serializeJson(Hgz h) {
        try {
            h.setPromiseList(objectMapper.writeValueAsString(h.getPromiseItems()));
            h.setBasisList(objectMapper.writeValueAsString(h.getBasisItems()));
        } catch (Exception e) {
            throw new BusinessException("承诺事项数据格式错误");
        }
    }

    private void parseJson(Hgz h) {
        h.setPromiseItems(parseList(h.getPromiseList()));
        h.setBasisItems(parseList(h.getBasisList()));
    }

    private List<Map<String, Object>> parseList(String json) {
        if (!StringUtils.hasText(json)) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            log.warn("合格证JSON解析失败: {}", json, e);
            return new ArrayList<>();
        }
    }
}
