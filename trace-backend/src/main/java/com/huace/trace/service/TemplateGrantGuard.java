package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 模板显式授权守卫。
 *
 * <p>与 {@code CommonController#getTraceTemplates} 的"分配字段为空=全部可用"故意相反：
 * 长图与落地页是后加的能力，要求 {@code enterprise.assigned_template_ids} 显式包含模板 id 才算开通。
 * 保持全局语义不变是为了不让现有 7 家 assigned_template_ids 为 NULL 的企业突然用不了老模板，
 * 收紧只发生在这两条新链路上。改动本类前请先读 spec §10.1。
 */
@Component
@RequiredArgsConstructor
public class TemplateGrantGuard {

    public static final String LONG_IMAGE = "long-image";
    public static final String LANDING = "landing";

    /** 必须被显式分配才算开放的渲染模式 */
    public static final Set<String> GRANT_REQUIRED = Set.of(LONG_IMAGE, LANDING);

    private static final String DENIED = "该模板未对您开放";

    private final EnterpriseMapper enterpriseMapper;
    private final TraceTemplateMapper traceTemplateMapper;
    /** 放在最后：@RequiredArgsConstructor 的字段顺序就是构造参数顺序 */
    private final TemplateConfigReader configReader;

    /** 未显式开放该渲染模式的模板时抛业务异常 */
    public void assertRenderModeAvailable(Long enterpriseId, String renderMode) {
        if (grantedTemplates(enterpriseId, renderMode).isEmpty()) {
            throw new BusinessException(DENIED);
        }
    }

    /** 该企业被显式开放的、指定渲染模式的启用模板，按 id 升序 */
    public List<TraceTemplate> grantedTemplates(Long enterpriseId, String renderMode) {
        if (enterpriseId == null) return List.of();
        Enterprise e = enterpriseMapper.selectById(enterpriseId);
        if (e == null || e.getAssignedTemplateIds() == null || e.getAssignedTemplateIds().isBlank()) {
            return List.of();
        }
        List<Long> ids = Arrays.stream(e.getAssignedTemplateIds().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).map(Long::parseLong).toList();
        if (ids.isEmpty()) return List.of();

        List<TraceTemplate> templates = traceTemplateMapper.selectList(
                new LambdaQueryWrapper<TraceTemplate>()
                        .in(TraceTemplate::getId, ids)
                        .eq(TraceTemplate::getStatus, 1)
                        .orderByAsc(TraceTemplate::getId));
        fillRenderMode(templates);
        return templates.stream().filter(t -> renderMode.equals(t.getRenderMode())).toList();
    }

    /** 填充派生的 renderMode，供前端按渲染模式决定菜单显隐，不让它自己解析 config_json */
    public List<TraceTemplate> fillRenderMode(List<TraceTemplate> templates) {
        templates.forEach(t -> t.setRenderMode(renderModeOf(t)));
        return templates;
    }

    /**
     * 给"未分配模板=全部可用"的兜底列表摘掉需要显式授权的渲染模式（顺带填充 renderMode）。
     * 否则前端菜单会照这份宽松列表放行长图入口，点进去又被 {@link #assertRenderModeAvailable} 拦下。
     */
    public List<TraceTemplate> stripGrantRequired(List<TraceTemplate> templates) {
        return fillRenderMode(templates).stream()
                // 存量模板 config_json 里没有 renderMode，取出来是 null；
                // Set.of 造出的集合 contains(null) 直接抛 NPE，必须先挡掉再判
                .filter(t -> t.getRenderMode() == null || !GRANT_REQUIRED.contains(t.getRenderMode()))
                .toList();
    }

    /** 从 config_json 取 renderMode；解析细节归 TemplateConfigReader，这里保持方法签名不变 */
    public String renderModeOf(TraceTemplate t) {
        return configReader.renderModeOf(t.getConfigJson());
    }
}
