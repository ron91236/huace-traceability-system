package com.huace.trace.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * config_json 的唯一读取口：渲染模式与长图模块骨架都从这里出。
 *
 * <p>骨架只有管理端能写，企业端与消费端都按 key 关联自己的内容，所以"这个模板有哪些模块、
 * 叫什么、什么顺序"这个问题的答案必须只有一处实现。P4 的 landingBlocks 也从这里长出去。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemplateConfigReader {

    /** 模块名称长度上限，与 trace_long_module.name 的 VARCHAR(50) 一致 */
    public static final int NAME_MAX = 50;
    /** 单个模板最多定义的模块数（spec 决定 13） */
    public static final int MODULE_MAX = 10;

    private final ObjectMapper objectMapper;

    /** 模板定义的一个长图模块；key 由管理端生成且一经生成不可变，改名只动 name */
    public record LongModuleDef(String key, String name, int sortOrder) {}

    /** 顶层 config_json；空、非法或不是对象返回 null，调用方按"没有配置"处理 */
    private Map<String, Object> config(String configJson) {
        if (configJson == null || configJson.isBlank()) return null;
        try {
            return objectMapper.readValue(configJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("模板 config_json 解析失败，按无配置处理: {}", e.getMessage());
            return null;
        }
    }

    /** 渲染模式；缺失或解析失败返回 null，调用方按"不匹配"处理 */
    public String renderModeOf(String configJson) {
        Map<String, Object> cfg = config(configJson);
        if (cfg == null) return null;
        Object v = cfg.get("renderMode");
        return v == null ? null : String.valueOf(v);
    }

    /** 长图模块骨架，按 sortOrder 升序；非长图模板或未配置时返回空列表 */
    public List<LongModuleDef> longModulesOf(String configJson) {
        if (!isLongImage(configJson)) return List.of();
        List<LongModuleDef> defs = new ArrayList<>();
        for (Map<String, Object> row : rawModules(configJson)) {
            String key = trimmed(row.get("key"));
            // 无 key 的行与企业页图关联不上，取数时丢弃；写入侧由 validate 当场拒绝
            if (key == null) continue;
            String name = trimmed(row.get("name"));
            defs.add(new LongModuleDef(key, name == null ? key : name, intOf(row.get("sortOrder"), defs.size())));
        }
        defs.sort(Comparator.comparingInt(LongModuleDef::sortOrder));
        return defs;
    }

    /**
     * 管理端保存模板时校验骨架。非长图模板直接放过：可视化模板没有 longModules，
     * 而列表页的状态开关只会 PUT {status}（configJson 为 null），也都不该被这套规则挡住。
     */
    public void validate(String configJson) {
        if (!isLongImage(configJson)) return;
        List<Map<String, Object>> rows = rawModules(configJson);
        if (rows.size() > MODULE_MAX) {
            throw new BusinessException("长图模块最多 " + MODULE_MAX + " 个");
        }
        Set<String> keys = new HashSet<>();
        int i = 0;
        for (Map<String, Object> row : rows) {
            i++;
            String key = trimmed(row.get("key"));
            if (key == null) throw new BusinessException("第 " + i + " 个模块缺少标识（key），请刷新后重试");
            if (!keys.add(key)) throw new BusinessException("模块标识重复：" + key);
            String name = trimmed(row.get("name"));
            if (name == null) throw new BusinessException("模块名称不能为空");
            if (name.length() > NAME_MAX) throw new BusinessException("模块名称不能超过 " + NAME_MAX + " 字");
        }
    }

    private boolean isLongImage(String configJson) {
        return TemplateGrantGuard.LONG_IMAGE.equals(renderModeOf(configJson));
    }

    /** longModules 数组的原始行；顺序就是管理端保存时的数组顺序 */
    private List<Map<String, Object>> rawModules(String configJson) {
        Map<String, Object> cfg = config(configJson);
        if (cfg == null) return List.of();
        Object v = cfg.get("longModules");
        if (!(v instanceof List<?> list)) return List.of();
        List<Map<String, Object>> out = new ArrayList<>();
        for (Object o : list) {
            if (!(o instanceof Map<?, ?> m)) continue;
            Map<String, Object> row = new LinkedHashMap<>();
            m.forEach((k, val) -> row.put(String.valueOf(k), val));
            out.add(row);
        }
        return out;
    }

    private static String trimmed(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /** Jackson 会把 JSON 数字还原成 Integer，也可能是字符串或 Long；都当成 int */
    private static int intOf(Object v, int fallback) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (NumberFormatException e) {
                return fallback;
            }
        }
        return fallback;
    }
}
