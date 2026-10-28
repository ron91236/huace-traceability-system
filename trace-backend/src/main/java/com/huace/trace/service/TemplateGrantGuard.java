package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
@Slf4j
public class TemplateGrantGuard {

    public static final String LONG_IMAGE = "long-image";
    public static final String LANDING = "landing";

    private static final String DENIED = "该模板未对您开放";

    private final EnterpriseMapper enterpriseMapper;
    private final TraceTemplateMapper traceTemplateMapper;
    private final ObjectMapper objectMapper;

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
        for (TraceTemplate t : templates) {
            t.setRenderMode(renderModeOf(t));
        }
        return templates.stream().filter(t -> renderMode.equals(t.getRenderMode())).toList();
    }

    /** 从 config_json 取 renderMode；缺失或解析失败返回 null，由调用方按"不匹配"处理 */
    public String renderModeOf(TraceTemplate t) {
        if (t.getConfigJson() == null || t.getConfigJson().isBlank()) return null;
        try {
            Map<String, Object> cfg = objectMapper.readValue(
                    t.getConfigJson(), new TypeReference<Map<String, Object>>() {});
            Object v = cfg.get("renderMode");
            return v == null ? null : String.valueOf(v);
        } catch (Exception ex) {
            log.warn("模板 config_json 解析失败，按渲染模式不匹配处理: templateId={}, err={}",
                    t.getId(), ex.getMessage());
            return null;
        }
    }
}
