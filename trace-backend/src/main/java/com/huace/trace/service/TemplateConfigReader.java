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

    /** 落地页的固定区块枚举（spec §5：key 不新生成，骨架只决定哪些块存在） */
    public static final List<String> LANDING_BLOCK_KEYS = List.of("richtext", "video", "promoImages", "antiFake");
    /** 侧边栏显示形态：float=带把手可收起，inline=常驻直显（参考页的 is_show_menu 0/1） */
    public static final List<String> SIDE_MENU_DISPLAYS = List.of("float", "inline");

    /** 模板定义的一个落地页区块 */
    public record LandingBlockDef(String key, String name, int sortOrder, boolean enabled) {}

    /** 落地页区块骨架，按 sortOrder 升序；非 landing 模板或未配置时返回空列表 */
    public List<LandingBlockDef> landingBlocksOf(String configJson) {
        if (!isLanding(configJson)) return List.of();
        List<LandingBlockDef> defs = new ArrayList<>();
        for (Map<String, Object> row : rawList(configJson, "landingBlocks")) {
            String key = trimmed(row.get("key"));
            // 不在枚举里的 key 一侧都渲染不出来，取数时丢弃；写入侧由 validate 当场拒绝
            if (key == null || !LANDING_BLOCK_KEYS.contains(key)) continue;
            String name = trimmed(row.get("name"));
            defs.add(new LandingBlockDef(key, name == null ? key : name,
                    intOf(row.get("sortOrder"), defs.size()), boolOf(row.get("enabled"), true)));
        }
        defs.sort(Comparator.comparingInt(LandingBlockDef::sortOrder));
        return defs;
    }

    /**
     * 侧边栏配置。**永远返回非 null 并补齐默认值**，让上层不必到处判空——
     * "没配 sideMenu"和"配了但只写了一项"是同一个意思：其余项按默认走。
     */
    public Map<String, Object> sideMenuOf(String configJson) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("enabled", false);
        out.put("display", "float");
        out.put("showTraceDetail", true);
        out.put("showBottomEntry", true);
        Map<String, Object> cfg = config(configJson);
        if (cfg == null || !(cfg.get("sideMenu") instanceof Map<?, ?> m)) return out;
        if (m.containsKey("enabled")) out.put("enabled", boolOf(m.get("enabled"), false));
        String display = trimmed(m.get("display"));
        if (display != null && SIDE_MENU_DISPLAYS.contains(display)) out.put("display", display);
        if (m.containsKey("showTraceDetail")) out.put("showTraceDetail", boolOf(m.get("showTraceDetail"), true));
        if (m.containsKey("showBottomEntry")) out.put("showBottomEntry", boolOf(m.get("showBottomEntry"), true));
        return out;
    }

    /**
     * 闸门配置（目前只有 requireAntiFakeBeforeJump）。没配就返回空 Map，
     * 取值处 `gates.get("...")` 拿到 null 按 false 处理——闸门默认必须是关的，
     * 默认开着会让所有已存在的落地页在下次发布时突然拦跳转。
     */
    public Map<String, Object> gatesOf(String configJson) {
        Map<String, Object> cfg = config(configJson);
        if (cfg == null || !(cfg.get("gates") instanceof Map<?, ?> m)) return Map.of();
        Map<String, Object> out = new LinkedHashMap<>();
        m.forEach((k, v) -> out.put(String.valueOf(k), v));
        return out;
    }

    /**
     * 管理端保存模板时校验骨架。非长图/落地页模板直接放过：可视化模板没有这两块结构，
     * 而列表页的状态开关只会 PUT {status}（configJson 为 null），都不该被这套规则挡住。
     */
    public void validate(String configJson) {
        String mode = renderModeOf(configJson);
        if (TemplateGrantGuard.LONG_IMAGE.equals(mode)) validateLongModules(configJson);
        else if (TemplateGrantGuard.LANDING.equals(mode)) validateLandingBlocks(configJson);
    }

    private void validateLongModules(String configJson) {
        List<Map<String, Object>> rows = rawList(configJson, "longModules");
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

    private void validateLandingBlocks(String configJson) {
        List<Map<String, Object>> rows = rawList(configJson, "landingBlocks");
        if (rows.size() > MODULE_MAX) {
            throw new BusinessException("落地页区块最多 " + MODULE_MAX + " 个");
        }
        Set<String> keys = new HashSet<>();
        for (Map<String, Object> row : rows) {
            String key = trimmed(row.get("key"));
            if (key == null) throw new BusinessException("区块缺少标识（key）");
            if (!LANDING_BLOCK_KEYS.contains(key)) {
                throw new BusinessException("不认识的区块：" + key + "，只能是 " + LANDING_BLOCK_KEYS);
            }
            if (!keys.add(key)) throw new BusinessException("区块标识重复：" + key);
            String name = trimmed(row.get("name"));
            if (name != null && name.length() > NAME_MAX) {
                throw new BusinessException("区块名称不能超过 " + NAME_MAX + " 字");
            }
        }
        // display 只能 float/inline。这里必须读**原始** sideMenu 而不是 sideMenuOf 的结果：
        // 读取侧会把非法值收敛成默认值，用收敛后的结果校验等于永远查不出问题
        Map<String, Object> cfg = config(configJson);
        Object rawMenu = cfg == null ? null : cfg.get("sideMenu");
        if (rawMenu instanceof Map<?, ?> m) {
            Object d = m.get("display");
            if (d != null && !SIDE_MENU_DISPLAYS.contains(String.valueOf(d).trim())) {
                throw new BusinessException("侧边栏显示方式只能是 " + SIDE_MENU_DISPLAYS);
            }
        }
    }

    private boolean isLongImage(String configJson) {
        return TemplateGrantGuard.LONG_IMAGE.equals(renderModeOf(configJson));
    }

    private boolean isLanding(String configJson) {
        return TemplateGrantGuard.LANDING.equals(renderModeOf(configJson));
    }

    /** longModules 数组的原始行；顺序就是管理端保存时的数组顺序 */
    private List<Map<String, Object>> rawModules(String configJson) {
        return rawList(configJson, "longModules");
    }

    /** 取 config_json 里某个数组键的原始行；缺失、非法或不是数组返回空列表 */
    private List<Map<String, Object>> rawList(String configJson, String field) {
        Map<String, Object> cfg = config(configJson);
        if (cfg == null) return List.of();
        Object v = cfg.get(field);
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

    /** JSON 布尔正常是 Boolean，但历史数据里可能是 "true" 字符串或 0/1；认不出来就用 fallback 而不是抛 */
    private static boolean boolOf(Object v, boolean fallback) {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        if (v instanceof String s) {
            String t = s.trim();
            if ("true".equalsIgnoreCase(t) || "1".equals(t)) return true;
            if ("false".equalsIgnoreCase(t) || "0".equals(t)) return false;
        }
        return fallback;
    }
}
