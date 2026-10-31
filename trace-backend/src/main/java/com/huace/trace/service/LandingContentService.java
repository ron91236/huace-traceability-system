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
 * 与长图内容同一套语义（spec §6.2、§12）。同一张表还存落地页的展示开关
 * （{@link #DISPLAY_KEYS}）：那些行存的不是内容而是 1/0，无行按"展示"处理，
 * 所以不会因为上线了这轮配置而改变任何已有页面的样子。
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

    /** 由系统实时供给内容的区块；企业能改的只有"在不在这页上展示" */
    private static final String ANTI_FAKE = "antiFake";
    private static final String SHOW = "1";
    private static final String HIDE = "0";

    /**
     * 落地页的四项展示开关（页面标题 / 防伪查询 / 侧栏「溯源详情」/ 底部大按钮）。
     * 与内容同表同坐标，值只允许 1/0，**无行=展示**，所以老页面的样子不会因为这轮上线而变。
     */
    public static final List<String> DISPLAY_KEYS = List.of("title", "antiFake", "traceDetail", "bottomEntry");
    /** 开关在清单里汇总成一条，省得前端去区块列表里翻四行 */
    public static final String DISPLAY_BLOCK = "display";

    private final TraceLandingContentMapper contentMapper;
    private final ObjectMapper objectMapper;
    private final CacheManager cacheManager;
    /** 放在最后：@RequiredArgsConstructor 的字段顺序就是构造参数顺序，测试手工构造时追加即可 */
    private final TemplateConfigReader configReader;

    /**
     * 企业端列表：模板骨架 ⊕ 该企业已填内容，末尾再挂一条汇总的「页面展示设置」。
     * 骨架里有但没填的区块带空串出现（企业要看清"该填哪几栏"），骨架外的历史行被忽略。
     */
    public List<Map<String, Object>> list(TraceTemplate template, Long enterpriseId) {
        Map<String, String> stored = storedContents(template, enterpriseId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (TemplateConfigReader.LandingBlockDef d : skeleton(template)) {
            // 防伪区块没有可填内容，改由下面那条开关管，留在清单里只会多一格填不了的东西
            if (ANTI_FAKE.equals(d.key())) continue;
            Map<String, Object> item = new HashMap<>();
            item.put("key", d.key());
            item.put("name", d.name());
            item.put("sortOrder", d.sortOrder());
            item.put("enabled", d.enabled());
            item.put("content", stored.getOrDefault(d.key(), ""));
            result.add(item);
        }
        Map<String, Object> display = new HashMap<>();
        display.put("key", DISPLAY_BLOCK);
        display.put("name", "页面展示设置");
        display.put("enabled", true);
        display.put("content", effectiveDisplay(template, stored));
        display.put("allowed", allowedDisplay(template));
        result.add(display);
        return result;
    }

    /** 生效后的展示开关：模板允许的 ∧ 企业没关的 */
    public Map<String, Boolean> effectiveDisplay(TraceTemplate template, Long enterpriseId) {
        return effectiveDisplay(template, enterpriseId == null
                ? Map.of() : storedContents(template, enterpriseId));
    }

    private Map<String, Boolean> effectiveDisplay(TraceTemplate template, Map<String, String> stored) {
        Map<String, Boolean> allowed = allowedDisplay(template);
        Map<String, Boolean> out = new java.util.LinkedHashMap<>();
        for (String key : DISPLAY_KEYS) {
            out.put(key, Boolean.TRUE.equals(allowed.get(key)) && !HIDE.equals(stored.get(key)));
        }
        return out;
    }

    /**
     * 模板侧允不允许。企业端拿它把不允许的开关显示成灰的——否则会出现"我明明打开了，
     * 消费者那边却没有"，而管理端骨架本来就是唯一能决定这块存不存的地方。
     */
    private Map<String, Boolean> allowedDisplay(TraceTemplate template) {
        boolean antiFakeOn = template != null && skeleton(template).stream()
                .anyMatch(d -> ANTI_FAKE.equals(d.key()) && d.enabled());
        Map<String, Object> menu = configReader.sideMenuOf(template == null ? null : template.getConfigJson());
        return Map.of(
                "title", true,
                "antiFake", antiFakeOn,
                "traceDetail", !Boolean.FALSE.equals(menu.get("showTraceDetail")),
                "bottomEntry", !Boolean.FALSE.equals(menu.get("showBottomEntry")));
    }

    /**
     * 消费者落地页取数：按骨架顺序输出**启用的且有内容的**区块。
     *
     * <p>antiFake 是唯一的例外——它没有内容，但默认要出现在载荷里，否则消费者页连防伪入口都没有；
     * 企业取消勾选（存 0）时才整块不出现。它的 content 给 null，是"这块由系统供给"的显式标记，
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
            // 企业取消勾选就是整块藏掉：它没有内容，套不进"没内容就不输出"那条规则，得单独判
            if (isAntiFake && HIDE.equals(content)) continue;
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
        String key = blockKey == null ? "" : blockKey.trim();

        // 展示开关排在骨架校验之前：title / traceDetail / bottomEntry 本来就不是骨架里的区块，
        // 走下面的查询会被当成"区块不存在"拒掉
        if (DISPLAY_KEYS.contains(key)) {
            String flag = content == null ? "" : content.trim();
            // 空串=回到默认（展示），与"清空内容删行"同一套语义，所以交给 upsert 的空值分支处理
            if (!flag.isEmpty() && !SHOW.equals(flag) && !HIDE.equals(flag)) {
                throw new BusinessException("展示开关只能提交 1（展示）或 0（不展示）");
            }
            upsert(template, key, flag, enterpriseId);
            return;
        }

        TemplateConfigReader.LandingBlockDef def = skeleton(template).stream()
                .filter(d -> d.key().equals(key))
                .findFirst()
                // 骨架里没有这个 key 就别写：那会落出一行任何一侧要不到的孤儿数据
                .orElseThrow(() -> new BusinessException("区块不存在"));

        String value = content == null ? "" : content.trim();
        if ("promoImages".equals(def.key()) && !value.isEmpty()) {
            value = normalizeImageArray(value);
        }
        if (value.length() > CONTENT_MAX) throw new BusinessException("内容过长，请精简后重新提交");

        upsert(template, def.key(), value, enterpriseId);
    }

    private void upsert(TraceTemplate template, String blockKey, String value, Long enterpriseId) {
        TraceLandingContent existing = contentMapper.selectOne(new LambdaQueryWrapper<TraceLandingContent>()
                .eq(TraceLandingContent::getTemplateId, template.getId())
                .eq(TraceLandingContent::getEnterpriseId, enterpriseId)
                .eq(TraceLandingContent::getBlockKey, blockKey));

        if (value.isEmpty()) {
            if (existing != null) contentMapper.deleteById(existing.getId());
            evictTracePage();
            return;
        }

        if (existing == null) {
            TraceLandingContent row = new TraceLandingContent();
            row.setTemplateId(template.getId());
            row.setEnterpriseId(enterpriseId);
            row.setBlockKey(blockKey);
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
