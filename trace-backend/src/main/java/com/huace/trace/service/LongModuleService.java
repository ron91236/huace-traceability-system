package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.TraceLongModule;
import com.huace.trace.mapper.TraceLongModuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 长图溯源模块：企业自助配置的整屏长图内容，商品级优先于企业默认 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LongModuleService {

    /** 单模块最多拼接的整页图数量 */
    public static final int MAX_PAGES = 30;
    /** 模块名称长度上限，与表字段 VARCHAR(50) 一致 */
    public static final int NAME_MAX = 50;

    private final TraceLongModuleMapper moduleMapper;
    private final ObjectMapper objectMapper;
    private final PdfConvertService pdfConvertService;
    private final CacheManager cacheManager;

    /**
     * 消费端取数：该商品有任一启用的商品级模块就只用商品级，
     * 否则整体回落企业默认（goods_id IS NULL）；不混排。
     */
    public List<Map<String, Object>> resolveForTrace(Long enterpriseId, Long goodsId) {
        if (enterpriseId == null) return List.of();
        List<TraceLongModule> modules = goodsId == null ? List.of()
                : moduleMapper.selectList(new LambdaQueryWrapper<TraceLongModule>()
                        .eq(TraceLongModule::getEnterpriseId, enterpriseId)
                        .eq(TraceLongModule::getGoodsId, goodsId)
                        .eq(TraceLongModule::getStatus, 1)
                        .orderByAsc(TraceLongModule::getSortOrder));
        if (modules.isEmpty()) {
            modules = moduleMapper.selectList(new LambdaQueryWrapper<TraceLongModule>()
                    .eq(TraceLongModule::getEnterpriseId, enterpriseId)
                    .isNull(TraceLongModule::getGoodsId)
                    .eq(TraceLongModule::getStatus, 1)
                    .orderByAsc(TraceLongModule::getSortOrder));
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (TraceLongModule m : modules) {
            List<String> pages = parsePages(m.getPages());
            if (pages.isEmpty()) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("key", String.valueOf(m.getId()));
            item.put("name", m.getName());
            item.put("pages", pages);
            result.add(item);
        }
        return result;
    }

    /** 企业端列表，含停用项，按 sort_order 升序 */
    public List<TraceLongModule> list(Long enterpriseId) {
        return moduleMapper.selectList(new LambdaQueryWrapper<TraceLongModule>()
                .eq(TraceLongModule::getEnterpriseId, enterpriseId)
                .orderByAsc(TraceLongModule::getSortOrder)
                .orderByAsc(TraceLongModule::getId));
    }

    private List<String> parsePages(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> pages = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return pages.stream().filter(p -> p != null && !p.isBlank()).toList();
        } catch (Exception e) {
            log.warn("长图模块 pages 解析失败: {}", json, e);
            return List.of();
        }
    }

    /** 长图模块变更会影响溯源页，直接清缓存（避免与 TracePageService 循环依赖） */
    private void evictTracePage() {
        try {
            Cache cache = cacheManager.getCache("tracePage");
            if (cache != null) cache.clear();
        } catch (Exception e) {
            log.warn("清除溯源页缓存失败", e);
        }
    }
}
