package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Goods;
import com.huace.trace.entity.TraceLongModule;
import com.huace.trace.mapper.GoodsMapper;
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
    /** 放在最后：@RequiredArgsConstructor 的字段顺序就是构造参数顺序，测试手工构造时追加即可 */
    private final GoodsMapper goodsMapper;

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
            List<String> pages = parsePages(m.getId(), m.getPages());
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

    /** 新建模块：企业归属与排序由服务端决定，前端不传 */
    @Transactional
    public void create(TraceLongModule form, Long enterpriseId) {
        validate(form, null);
        assertGoodsOwned(form.getGoodsId(), enterpriseId);
        Long count = moduleMapper.selectCount(new LambdaQueryWrapper<TraceLongModule>()
                .eq(TraceLongModule::getEnterpriseId, enterpriseId));
        form.setId(null);
        form.setEnterpriseId(enterpriseId);
        form.setSortOrder(count == null ? 0 : count.intValue());
        if (form.getStatus() == null) form.setStatus(1);
        moduleMapper.insert(form);
        evictTracePage();
    }

    /** 更新模块：名称、图片、商品归属、启停；不允许改归属企业 */
    @Transactional
    public void update(Long id, TraceLongModule form, Long enterpriseId) {
        TraceLongModule existing = ownedModule(id, enterpriseId);
        validate(form, existing.getId());
        assertGoodsOwned(form.getGoodsId(), enterpriseId);
        // 用 wrapper 显式列 SET：updateById 的 NOT_NULL 策略会跳过 null，
        // 清空适用商品（回落企业默认）就写不进去，接口却照样回 200。
        moduleMapper.update(null, new LambdaUpdateWrapper<TraceLongModule>()
                .eq(TraceLongModule::getId, existing.getId())
                .set(TraceLongModule::getName, form.getName().trim())
                .set(TraceLongModule::getPages, form.getPages())
                .set(TraceLongModule::getGoodsId, form.getGoodsId())
                .set(TraceLongModule::getStatus, form.getStatus() == null ? existing.getStatus() : form.getStatus()));
        evictTracePage();
    }

    /** 删除模块：只删记录，转图结果按内容哈希共享，不删图片文件 */
    @Transactional
    public void delete(Long id, Long enterpriseId) {
        ownedModule(id, enterpriseId);
        moduleMapper.deleteById(id);
        evictTracePage();
    }

    /** 拖拽排序：传入完整 id 顺序，任一不属于本企业或有重复则整体拒绝 */
    @Transactional
    public void sort(List<Long> ids, Long enterpriseId) {
        if (ids == null || ids.isEmpty()) return;
        // 重复 id 会让同一行拿到两个不同 index，sort_order 直接撞车；
        // 与 ownedModule 一样"全部校验通过才动手写"，不做部分生效
        if (ids.stream().distinct().count() != ids.size()) {
            throw new BusinessException("排序列表不能有重复模块");
        }
        List<TraceLongModule> owned = new ArrayList<>();
        for (Long id : ids) {
            owned.add(ownedModule(id, enterpriseId));
        }
        for (int i = 0; i < owned.size(); i++) {
            TraceLongModule patch = new TraceLongModule();
            patch.setId(owned.get(i).getId());
            patch.setSortOrder(i);
            // 只带 sortOrder：goods_id 不在 SET 子句里，商品级模块不会被拖拽排序降级
            moduleMapper.updateById(patch);
        }
        evictTracePage();
    }

    private TraceLongModule ownedModule(Long id, Long enterpriseId) {
        TraceLongModule existing = moduleMapper.selectById(id);
        if (existing == null) throw new BusinessException("模块不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        return existing;
    }

    /**
     * 校验写入的商品归属：goods_id 为空是企业默认（全部商品共用），合法且是常态；
     * 非空必须能查到、且属于本企业，否则模块会挂到别人商品下永不展示，配置漂移也无从追查。
     * 与 HgzService 校验批次同款写法（先查行再判归属）。
     */
    private void assertGoodsOwned(Long goodsId, Long enterpriseId) {
        if (goodsId == null) return;
        Goods goods = goodsMapper.selectById(goodsId);
        if (goods == null) throw new BusinessException("商品不存在");
        if (!enterpriseId.equals(goods.getEnterpriseId())) throw new BusinessException("无权使用该商品");
    }

    private void validate(TraceLongModule form, Long moduleId) {
        if (form.getName() == null || form.getName().trim().isEmpty()) {
            throw new BusinessException("模块名称不能为空");
        }
        if (form.getName().trim().length() > NAME_MAX) {
            throw new BusinessException("模块名称不能超过 " + NAME_MAX + " 字");
        }
        // parsePages 的签名带模块 id：坏数据的日志要能定位到行。
        // 这里的 moduleId 由调用方给权威值（新建为 null，更新为路径 id 对应的行），
        // 不能取 form.getId()——前端传什么就报什么，日志会指向没被写入的那一行。
        List<String> pages = parsePages(moduleId, form.getPages());
        if (pages.isEmpty()) throw new BusinessException("模块至少一张长图");
        if (pages.size() > MAX_PAGES) throw new BusinessException("单个模块最多拼接 " + MAX_PAGES + " 页");
        form.setName(form.getName().trim());
    }

    /** 解析 pages：坏数据按无内容跳过，日志带模块 id 才能定位是哪行数据坏了 */
    private List<String> parsePages(Long moduleId, String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            List<String> pages = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return pages.stream().filter(p -> p != null && !p.isBlank()).toList();
        } catch (Exception e) {
            log.warn("长图模块 id={} 的 pages 解析失败: {}", moduleId, json, e);
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
