package com.huace.trace.controller;

import com.huace.trace.common.BusinessException;
import com.huace.trace.common.GlobalExceptionHandler;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.LongModuleService;
import com.huace.trace.service.TemplateGrantGuard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 企业端长图控制器的契约测试（仓库第一例 MockMvc 测试）。P2 起企业只剩"按骨架填图"这一件事，
 * 所以这里钉的是路由与参数形状：
 * 1. GET 带 templateId，守卫放行的模板行才交给服务，企业 id 只认登录态；
 * 2. PUT 的两个路径变量分别落到 templateId 与 moduleKey，body 的 "pages" 是字符串数组；
 * 3. convert 仍按"这家企业开过任意长图模板"守卫；
 * 4. 守卫必须在服务层之前。
 *
 * 用 standaloneSetup 而不是 @WebMvcTest/@SpringBootTest：不拉起 Spring 上下文、
 * 不需要 profile 和数据库，也零新增依赖——离线 mvn 仓库里只有 pom 现有内容，
 * 没有 spring-security-test，所以 @AuthenticationPrincipal 靠手工注册
 * AuthenticationPrincipalArgumentResolver，认证态直接塞 SecurityContextHolder
 * （standalone MockMvc 在调用线程上同步执行，ThreadLocal 天然可见，用完必须清）。
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseLongModuleControllerTest {

    /** 与生产一致：企业主账号的 userId 就是企业 id（JwtAuthenticationFilter 原样透传 JWT claims） */
    private static final long ENTERPRISE_ID = 23L;
    private static final long TEMPLATE_ID = 20L;

    @Mock private LongModuleService longModuleService;
    /**
     * 默认空实现的 mock：void 方法不打桩时什么都不做，返回类型的方法返回 null。
     * 需要模板行的用例逐个打桩（见 template()），本阶段只测"守卫被接上且能拦住"，
     * 不重复测守卫内部逻辑（那在 TemplateGrantGuardTest）。
     */
    @Mock private TemplateGrantGuard templateGrantGuard;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EnterpriseLongModuleController(longModuleService, templateGrantGuard))
                // 与生产同款异常出口：兜底挂上后 BusinessException 出去的是 Result JSON
                .setControllerAdvice(new GlobalExceptionHandler())
                // standalone 模式不加载安全配置，@AuthenticationPrincipal 的解析器要手工挂
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
        UserPrincipal principal = new UserPrincipal(ENTERPRISE_ID, "ent-user", "ENTERPRISE", ENTERPRISE_ID, "owner");
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ENTERPRISE"))));
    }

    @AfterEach
    void tearDown() {
        // 认证态挂在当前线程的 ThreadLocal 上，不清会漏给同类后续用例
        SecurityContextHolder.clearContext();
    }

    private TraceTemplate template() {
        TraceTemplate t = new TraceTemplate();
        t.setId(TEMPLATE_ID);
        t.setTemplateKey("long-image");
        t.setConfigJson("{\"renderMode\":\"long-image\",\"longModules\":["
                + "{\"key\":\"m1730000000000\",\"name\":\"产品信息\",\"sortOrder\":0}]}");
        return t;
    }

    @Test
    void get带templateId时把守卫放行的一行交给服务且企业取登录态() throws Exception {
        TraceTemplate granted = template();
        when(templateGrantGuard.requireGranted(TEMPLATE_ID, ENTERPRISE_ID, TemplateGrantGuard.LONG_IMAGE))
                .thenReturn(granted);
        when(longModuleService.list(granted, ENTERPRISE_ID))
                .thenReturn(List.of(Map.of("key", "m1730000000000", "name", "产品信息", "pages", List.of())));

        mockMvc.perform(get("/api/enterprise/long-modules").param("templateId", String.valueOf(TEMPLATE_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].key").value("m1730000000000"));

        // 第二参必须是登录态的 23 而不是任何请求参数；将来谁把取值从 getUserId() 换掉，这条就红
        verify(longModuleService).list(granted, ENTERPRISE_ID);
    }

    @Test
    void put的两个路径变量分别落到templateId与moduleKey() throws Exception {
        TraceTemplate granted = template();
        when(templateGrantGuard.requireGranted(TEMPLATE_ID, ENTERPRISE_ID, TemplateGrantGuard.LONG_IMAGE))
                .thenReturn(granted);

        mockMvc.perform(put("/api/enterprise/long-modules/{templateId}/{key}", TEMPLATE_ID, "m1730000000000")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pages\":[\"/uploads/a.png\",\"/uploads/b.png\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 键名必须是 pages：Jackson 若没按 Map<String, List<String>> 泛型还原，元素类型就不对；
        // 路径里的 "m1730000000000" 必须原样落到 moduleKey，服务靠它比对骨架。
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> pagesCaptor = (ArgumentCaptor<List<String>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(longModuleService).savePages(eq(granted), eq("m1730000000000"),
                pagesCaptor.capture(), eq(ENTERPRISE_ID));
        assertEquals(List.of("/uploads/a.png", "/uploads/b.png"), pagesCaptor.getValue());
    }

    @Test
    void convert的url键原样转发给服务() throws Exception {
        when(longModuleService.convertPdf("/uploads/2026/a.pdf"))
                .thenReturn(List.of("/uploads/pdf-images/k/k_p1.jpg"));

        mockMvc.perform(post("/api/enterprise/long-modules/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"/uploads/2026/a.pdf\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0]").value("/uploads/pdf-images/k/k_p1.jpg"));

        verify(longModuleService).convertPdf("/uploads/2026/a.pdf");
        // convert 不落企业内容，只按"有没有任意长图模板"守卫，不该要求 templateId
        verify(longModuleService, never()).savePages(any(), any(), any(), any());
    }

    @Test
    void 未显式分配该模板时守卫拦下且不碰服务() throws Exception {
        // 这条是本阶段真正的安全边界：企业可以随手填一个别人的 templateId
        doThrow(new BusinessException("该模板未对您开放"))
                .when(templateGrantGuard)
                .requireGranted(99L, ENTERPRISE_ID, TemplateGrantGuard.LONG_IMAGE);

        mockMvc.perform(get("/api/enterprise/long-modules").param("templateId", "99"))
                .andExpect(status().isOk())
                // BusinessException 经 GlobalExceptionHandler 出去仍是 HTTP 200，
                // 所以断言必须打在 $.code 上，status() 在这里永远绿
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该模板未对您开放"));

        // 守卫必须在服务层之前：走到 service 就等于放行成功
        verifyNoInteractions(longModuleService);
    }

    @Test
    void convert仍受渲染模式守卫保护() throws Exception {
        doThrow(new BusinessException("该模板未对您开放"))
                .when(templateGrantGuard)
                .assertRenderModeAvailable(ENTERPRISE_ID, TemplateGrantGuard.LONG_IMAGE);

        mockMvc.perform(post("/api/enterprise/long-modules/convert")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"/uploads/2026/a.pdf\"}"))
                .andExpect(jsonPath("$.code").value(500));

        // PDF 转换会落盘并写缓存，未开通的企业不该能白嫖这个重操作
        verifyNoInteractions(longModuleService);
    }
}
