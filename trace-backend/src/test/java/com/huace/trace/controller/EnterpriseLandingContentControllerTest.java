package com.huace.trace.controller;

import com.huace.trace.common.BusinessException;
import com.huace.trace.common.GlobalExceptionHandler;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.LandingContentService;
import com.huace.trace.service.TemplateGrantGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 企业端落地页内容控制器的契约测试。只钉"仅经控制器可达、服务层测试覆盖不到"的部分
 * （守卫内部逻辑在 TemplateGrantGuardTest，内容与区块的规则在 LandingContentServiceTest）：
 * 1. GET 带 templateId，守卫放行的那一行模板才交给服务，企业 id 只认登录态；
 * 2. PUT 的两个路径变量分别落到 templateId 与 blockKey，body 的 "content" 原样转发；
 * 3. 未显式分配该模板时守卫拦下，服务一次都不被调用。
 *
 * 用 standaloneSetup 的理由同 EnterpriseLongModuleControllerTest：不起 Spring 上下文、
 * 零新增依赖，离线仓库里没有 spring-security-test，所以认证解析器要手工挂。
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseLandingContentControllerTest {

    /** 与生产一致：企业主账号的 userId 就是企业 id（JwtAuthenticationFilter 原样透传 JWT claims） */
    private static final long ENTERPRISE_ID = 8L;
    private static final long TEMPLATE_ID = 30L;

    @Mock private LandingContentService landingContentService;
    /** 需要模板行的用例逐个打桩；本类只测"守卫被接上且能拦住" */
    @Mock private TemplateGrantGuard templateGrantGuard;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EnterpriseLandingContentController(landingContentService, templateGrantGuard))
                // 与生产同款异常出口：兜底挂上后 BusinessException 出去的是 Result JSON
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        UserPrincipal principal = new UserPrincipal(ENTERPRISE_ID, "ent-user", "ENTERPRISE", ENTERPRISE_ID, "owner");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ENTERPRISE"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private TraceTemplate template() {
        TraceTemplate t = new TraceTemplate();
        t.setId(TEMPLATE_ID);
        t.setTemplateKey("tpl_landing");
        t.setConfigJson("{\"renderMode\":\"landing\",\"landingBlocks\":["
                + "{\"key\":\"richtext\",\"name\":\"内容\",\"sortOrder\":0,\"enabled\":true}]}");
        return t;
    }

    @Test
    void get带templateId时把守卫放行的一行交给服务且企业取登录态() throws Exception {
        TraceTemplate granted = template();
        when(templateGrantGuard.requireGranted(TEMPLATE_ID, ENTERPRISE_ID, TemplateGrantGuard.LANDING))
                .thenReturn(granted);
        when(landingContentService.list(granted, ENTERPRISE_ID))
                .thenReturn(List.of(Map.of("key", "richtext", "content", "<p>正文</p>")));

        mockMvc.perform(get("/api/enterprise/landing-content").param("templateId", String.valueOf(TEMPLATE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].key").value("richtext"));

        verify(landingContentService).list(granted, ENTERPRISE_ID);
    }

    @Test
    void put的两个路径变量分别落到templateId与blockKey() throws Exception {
        TraceTemplate granted = template();
        when(templateGrantGuard.requireGranted(TEMPLATE_ID, ENTERPRISE_ID, TemplateGrantGuard.LANDING))
                .thenReturn(granted);

        mockMvc.perform(put("/api/enterprise/landing-content/{templateId}/{blockKey}", TEMPLATE_ID, "richtext")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"<p>正文</p>\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(landingContentService).saveContent(eq(granted), eq("richtext"), eq("<p>正文</p>"), eq(ENTERPRISE_ID));
    }

    @Test
    void 未显式分配该模板时守卫拦下且不碰服务() throws Exception {
        // 这条是本阶段的安全边界：企业可以随手填一个别人的 templateId
        doThrow(new BusinessException("该模板未对您开放"))
                .when(templateGrantGuard)
                .requireGranted(99L, ENTERPRISE_ID, TemplateGrantGuard.LANDING);

        mockMvc.perform(get("/api/enterprise/landing-content").param("templateId", "99"))
                .andExpect(status().isOk())
                // BusinessException 经 GlobalExceptionHandler 出去仍是 HTTP 200，
                // 所以断言必须打在 $.code 上，status() 在这里永远绿
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该模板未对您开放"));

        // 守卫必须在服务层之前：走到 service 就等于放行成功
        verifyNoInteractions(landingContentService);
    }
}
