package com.huace.trace.service;

import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 模板显式授权守卫回归测试。钉住三件事：
 * 1. 长图/落地页要求 assigned_template_ids 显式包含，NULL 或空串一律未开通
 *    ——这与 CommonController 的"空=全部可用"故意相反，不能被"顺手统一"掉
 * 2. 只认启用状态且 renderMode 精确匹配的模板
 * 3. config_json 解析失败按不匹配处理，不抛异常打断请求
 * 4. "未分配=全部可用"的兜底列表要剔除长图/落地页，而已显式分配的企业不受影响
 */
@ExtendWith(MockitoExtension.class)
class TemplateGrantGuardTest {

    @Mock private EnterpriseMapper enterpriseMapper;
    @Mock private TraceTemplateMapper traceTemplateMapper;

    private TemplateGrantGuard guard;

    @BeforeEach
    void setUp() {
        // 必须在 @BeforeEach 里构造：字段初始化器跑在 @Mock 注入之前，
        // 写成 `= new TemplateGrantGuard(enterpriseMapper, ...)` 会拿到 null mapper，
        // 表现是抛 NullPointerException 而不是 BusinessException。
        // ObjectMapper 用真实实例——mock 的 readValue 恒返回 null，会让"不匹配"用例假通过。
        guard = new TemplateGrantGuard(enterpriseMapper, traceTemplateMapper, new ObjectMapper());
    }

    private Enterprise enterprise(String assigned) {
        Enterprise e = new Enterprise();
        e.setId(23L);
        e.setAssignedTemplateIds(assigned);
        return e;
    }

    private TraceTemplate template(Long id, String configJson) {
        TraceTemplate t = new TraceTemplate();
        t.setId(id);
        t.setConfigJson(configJson);
        t.setStatus(1);
        return t;
    }

    @Test
    void 显式开放长图模板时通过() {
        when(enterpriseMapper.selectById(23L)).thenReturn(enterprise("14,20"));
        when(traceTemplateMapper.selectList(any())).thenReturn(
                List.of(template(20L, "{\"renderMode\":\"long-image\"}")));

        assertDoesNotThrow(() -> guard.assertRenderModeAvailable(23L, TemplateGrantGuard.LONG_IMAGE));
    }

    @Test
    void 分配字段为NULL时判定未开通() {
        when(enterpriseMapper.selectById(23L)).thenReturn(enterprise(null));

        BusinessException e = assertThrows(BusinessException.class,
                () -> guard.assertRenderModeAvailable(23L, TemplateGrantGuard.LONG_IMAGE));
        assertTrue(e.getMessage().contains("未对您开放"), e.getMessage());
        // 未开通时不应再去查模板表
        verify(traceTemplateMapper, never()).selectList(any());
    }

    @Test
    void 分配字段为空串时判定未开通() {
        when(enterpriseMapper.selectById(23L)).thenReturn(enterprise("   "));

        assertThrows(BusinessException.class,
                () -> guard.assertRenderModeAvailable(23L, TemplateGrantGuard.LONG_IMAGE));
        verify(traceTemplateMapper, never()).selectList(any());
    }

    @Test
    void 企业不存在时判定未开通() {
        when(enterpriseMapper.selectById(99L)).thenReturn(null);

        assertThrows(BusinessException.class,
                () -> guard.assertRenderModeAvailable(99L, TemplateGrantGuard.LONG_IMAGE));
    }

    @Test
    void enterpriseId为空时判定未开通() {
        assertThrows(BusinessException.class,
                () -> guard.assertRenderModeAvailable(null, TemplateGrantGuard.LONG_IMAGE));
        verifyNoInteractions(enterpriseMapper);
    }

    @Test
    void 渲染模式不匹配时判定未开通() {
        when(enterpriseMapper.selectById(23L)).thenReturn(enterprise("14,20"));
        when(traceTemplateMapper.selectList(any())).thenReturn(
                List.of(template(20L, "{\"renderMode\":\"visual\"}")));

        assertThrows(BusinessException.class,
                () -> guard.assertRenderModeAvailable(23L, TemplateGrantGuard.LONG_IMAGE));
    }

    @Test
    void 无config_json的模板按不匹配处理() {
        when(enterpriseMapper.selectById(23L)).thenReturn(enterprise("14,20"));
        when(traceTemplateMapper.selectList(any())).thenReturn(List.of(template(20L, null)));

        assertThrows(BusinessException.class,
                () -> guard.assertRenderModeAvailable(23L, TemplateGrantGuard.LONG_IMAGE));
    }

    @Test
    void config_json非法时按不匹配处理而不是抛解析异常() {
        when(enterpriseMapper.selectById(23L)).thenReturn(enterprise("20"));
        when(traceTemplateMapper.selectList(any())).thenReturn(
                List.of(template(20L, "{不是合法JSON")));

        BusinessException e = assertThrows(BusinessException.class,
                () -> guard.assertRenderModeAvailable(23L, TemplateGrantGuard.LONG_IMAGE));
        assertTrue(e.getMessage().contains("未对您开放"), e.getMessage());
    }

    @Test
    void grantedTemplates返回渲染模式并暴露给调用方() {
        when(enterpriseMapper.selectById(23L)).thenReturn(enterprise("14,20"));
        when(traceTemplateMapper.selectList(any())).thenReturn(
                List.of(template(20L, "{\"renderMode\":\"long-image\"}")));

        List<TraceTemplate> granted = guard.grantedTemplates(23L, TemplateGrantGuard.LONG_IMAGE);

        assertEquals(1, granted.size());
        // renderMode 必须已被填充，前端菜单显隐直接读它，不再解析 config_json
        assertEquals("long-image", granted.get(0).getRenderMode());
    }

    @Test
    void 只含其他模式的模板时长图列表为空() {
        when(enterpriseMapper.selectById(23L)).thenReturn(enterprise("14"));
        when(traceTemplateMapper.selectList(any())).thenReturn(
                List.of(template(14L, "{\"renderMode\":\"visual\"}")));

        assertTrue(guard.grantedTemplates(23L, TemplateGrantGuard.LONG_IMAGE).isEmpty());
    }

    @Test
    void renderModeOf读出渲染模式() {
        assertEquals("long-image",
                guard.renderModeOf(template(20L, "{\"renderMode\":\"long-image\"}")));
        assertEquals("landing",
                guard.renderModeOf(template(21L, "{\"renderMode\":\"landing\",\"sideMenu\":{\"enabled\":true}}")));
    }

    @Test
    void renderModeOf对无该字段的模板返回null() {
        assertNull(guard.renderModeOf(template(1L, "{\"pages\":[]}")));
        assertNull(guard.renderModeOf(template(1L, null)));
        assertNull(guard.renderModeOf(template(1L, "  ")));
    }

    @Test
    void 未分配企业的兜底列表剔除长图与落地页但保留老模板() {
        // 存量模板 config_json 里没有 renderMode，必须原样留下——
        // 这条一旦红，7 家 assigned_template_ids 为 NULL 的企业会集体丢模板
        List<TraceTemplate> out = guard.stripGrantRequired(List.of(
                template(1L, "{\"pages\":[]}"),
                template(14L, "{\"renderMode\":\"visual\"}"),
                template(20L, "{\"renderMode\":\"long-image\"}"),
                template(21L, "{\"renderMode\":\"landing\"}")));

        assertEquals(List.of(1L, 14L), out.stream().map(TraceTemplate::getId).toList());
        // 顺带填充 renderMode，前端菜单直接读，不用再解析 config_json
        assertNull(out.get(0).getRenderMode());
        assertEquals("visual", out.get(1).getRenderMode());
    }

    @Test
    void 已显式分配的企业仍能看到长图模板() {
        // 与上一条对照：收紧只发生在"未分配"的兜底路径上，
        // 否则被授权的企业菜单会跟着消失，长图功能等于下线
        List<TraceTemplate> out = guard.fillRenderMode(List.of(
                template(14L, "{\"renderMode\":\"visual\"}"),
                template(20L, "{\"renderMode\":\"long-image\"}")));

        assertEquals(2, out.size());
        assertEquals("long-image", out.get(1).getRenderMode());
    }
}
