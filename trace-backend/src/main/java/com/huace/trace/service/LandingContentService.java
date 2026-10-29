package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.TraceLandingContent;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.TraceLandingContentMapper;
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

/**
 * 落地页区块内容：区块骨架归模板（管理端写），内容归企业（本轮由企业写的数据）。
 *
 * <p>行坐标是 (template_id, enterprise_id, block_key)，且「企业未填」用**无行**表示，
 * 与长图内容同一套语义（spec §6.2、§12）。antiFake 区块不写行——防伪数据由系统按码实时取，
 * 存一份快照等于给消费者看一个可能已经过期的结论。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandingContentService {

    /**
     * 单区块内容长度上限。content 列是 MEDIUMTEXT（16MB），这里挡的不是容量而是异常输入：
     * 一次误粘贴的几 MB 正文会让落地页首屏把这么多 HTML 全带上，而没有任何消费者需要它。
     */
    public static final int CONTENT_MAX = 200_000;

    /** 由系统实时供给、不允许企业写内容的区块 */
    private static final String ANTI_FAKE = "antiFake";

    private final TraceLandingContentMapper contentMapper;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    /** 放在最后：@RequiredArgsConstructor 的字段顺序就是构造参数顺序，测试手工构造时追加即可 */
    private final TemplateConfigReader configReader;

    /**
     * 企业端列表：模板骨架 ⊕ 该企业已填内容。
     * 骨架里有但没填的区块带空串出现（企业要看清"该填哪几栏"），骨架外的历史行被忽略。
     */
    public List<Map<String, Object>> list(TraceTemplate template, Long enterpriseId) {
        Map<String, String> stored = storedContents(template, enterpriseId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TemplateConfigReader.LandingBlockDef d : skeleton(template)) {
            Map<String, Object> item = new HashMap<>();
            item.put("key", d.key());
            item.put("name", d.name());
            item.put("sortOrder", d.sortOrder());
            item.put("enabled", d.enabled());
            item.put("content", ANTI_FAKE.equals(d.key()) ? "" : stored.getOrDefault(d.key(), ""));
            result.add(item);
        }
        return result;
    }

    /**
     * 消费者落地页取数：按骨架顺序输出**启用的且有内容的**区块。
     *
     * <p>antiFake 是唯一的例外——它永远没有内容，但必须出现在载荷里，
     * 否则消费者页连防伪入口都没有。它的 content 给 null，是"这块由系统供给"的显式标记，
     * 前端据此渲染实时结论而不是空白块。
     */
    public List<Map<String, Object>> resolveForLanding(TraceTemplate template, Long enterpriseId) {
        if (enterpriseId == null) return List.of();
        Map<String, String> stored = storedContents(template, enterpriseId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TemplateConfigReader.LandingBlockDef d : skeleton(template)) {
            if (!d.enabled()) continue;   // 管理端关掉一块就是不想展示，不是"空着展示"
            String content = stored.get(d.key());
            boolean isAntiFake = ANTI_FAKE.equals(d.key());
            if (!isAntiFake && (content == null || content.isBlank())) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("key", d.key());
            item.put("name", d.name());
            item.put("enabled", true);
            item.put("content", isAntiFake ? null : content);
            result.add(item);
        }
        return result;
    }

    /**
     * 保存某区块的内容。content 为空即"企业未填"，按 spec §12 用无行表示，所以删行而不是写空串。
     *
     * <p>跨企业写入没有独立的归属判断方法：定位行的那条查询本身就同时绑了 template_id 与
     * enterprise_id，别人的行 select 不到，只会走"新增"分支插一条属于自己的行。
     */
    @Transactional
    public void saveContent(TraceTemplate template, String blockKey, String content, Long enterpriseId) {
        TemplateConfigReader.LandingBlockDef def = skeleton(template).stream()
                .filter(d -> d.key().equals(blockKey))
                .findFirst()
                // 骨架里没有这个 key 就别写：那会落出一行任何一侧都取不出来的孤儿数据
                .orElseThrow(() -> new BusinessException("区块不存在"));

        // 先于任何查库动作拒绝：antiFake 压根没有"内容"这个概念，写进去就是一行永远不会被读的脏数据
        if (ANTI_FAKE.equals(def.key())) {
            throw new BusinessException("防伪查询区块由系统按码实时生成，不支持填写内容");
        }

        String value = content == null ? "" : content.trim();
        if ("promoImages".equals(def.key()) && !value.isEmpty()) {
            value = normalizeImageArray(value);
        }
        if (value.length() > CONTENT_MAX) throw new BusinessException("内容过长，请精简后重新提交");

        TraceLandingContent existing = contentMapper.selectOne(new LambdaQueryWrapper<TraceLandingContent>()
                .eq(TraceLandingContent::getTemplateId, template.getId())
                .eq(TraceLandingContent::getEnterpriseId, enterpriseId)
                .eq(TraceLandingContent::getBlockKey, def.key()));

        if (value.isEmpty()) {
            if (existing != null) contentMapper.deleteById(existing.getId());
            evictTracePage();
            return;
        }

        if (existing == null) {
            TraceLandingContent row = new TraceLandingContent();
            row.setTemplateId(template.getId());
            row.setEnterpriseId(enterpriseId);
            row.setBlockKey(def.key());
            row.setContent(value);
            contentMapper.insert(row);
        } else {
            // 只带 content 一个业务字段，其余列不进 SET：name/顺序归模板骨架
            existing.setContent(value);
            contentMapper.updateById(existing);
        }
        evictTracePage();
    }

    /**
     * promoImages 存的是 URL 的 JSON 数组（spec §6.2）。
     * 不是数组就拒：让一个裸字符串混进库里，消费者端每次渲染都要猜它是数组还是单值，
     * 猜错就是把整块宣传图渲染成空白。
     *
     * <p>清完是一张不剩，与"richtext 清空"是同一件事，所以归一成空串交给上层走删行分支——
     * 否则库里会留下一行 "[]"，同一个"未填"就有了两种表示。
     */
    private String normalizeImageArray(String value) {
        List<String> urls;
        try {
            urls = objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            throw new BusinessException("宣传图片内容必须是图片地址数组，请重新提交");
        }
        List<String> clean = urls.stream().filter(u -> u != null && !u.isBlank()).toList();
        if (clean.isEmpty()) return "";
        try {
            return objectMapper.writeValueAsString(clean);
        } catch (Exception e) {
            log.warn("宣传图片序列化失败: {}", e.getMessage());
            throw new BusinessException("宣传图片内容格式错误，请重新提交");
        }
    }

    /** 模板定义的区块骨架；顺序与启停都由 TemplateConfigReader 从 config_json 读出 */
    private List<TemplateConfigReader.LandingBlockDef> skeleton(TraceTemplate template) {
        if (template == null) return List.of();
        return configReader.landingBlocksOf(template.getConfigJson());
    }

    /** 该企业该模板已填的内容，按 block_key 归档 */
    private Map<String, String> storedContents(TraceTemplate template, Long enterpriseId) {
        if (template == null || template.getId() == null || enterpriseId == null) return Map.of();
        Map<String, String> out = new HashMap<>();
        for (TraceLandingContent c : contentMapper.selectList(new LambdaQueryWrapper<TraceLandingContent>()
                .eq(TraceLandingContent::getTemplateId, template.getId())
                .eq(TraceLandingContent::getEnterpriseId, enterpriseId))) {
            if (c.getBlockKey() == null || c.getBlockKey().isBlank()) continue;
            out.put(c.getBlockKey(), c.getContent() == null ? "" : c.getContent());
        }
        return out;
    }

    /** 落地页内容变更会影响消费者页，直接清缓存（与长图同一处置） */
    private void evictTracePage() {
        try {
            Cache cache = cacheManager.getCache("tracePage");
            if (cache != null) cache.clear();
        } catch (Exception e) {
            log.warn("清除溯源页缓存失败", e);
        }
    }
}
