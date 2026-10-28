package com.huace.trace.controller;

import com.huace.trace.common.BusinessException;
import com.huace.trace.common.GlobalExceptionHandler;
import com.huace.trace.entity.TraceLongModule;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 企业端长图控制器的契约测试（仓库第一例 MockMvc 测试）。
 * 只钉住"仅经控制器可达、服务层测试覆盖不到"的三件事，Task 8/9 前端直接对着这些契约写：
 * 1. PUT /sort 路由到 sort，而不是被 PUT /{id} 的 @PathVariable 吞掉，且请求体键名就是 "ids"；
 * 2. POST /convert 把键名 "url" 的值原样转发给 convertPdf；
 * 3. 企业归属取登录态主体，请求体里的 enterpriseId 影响不到服务入参。
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

    @Mock private LongModuleService longModuleService;
    /**
     * 默认空实现的 mock：void 方法不打桩时什么都不做，所以既有的断言路由与转发的用例
     * 不需要改动就能穿过守卫。本阶段只测"守卫被接上且能拦住"，不重复测守卫内部逻辑。
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

    @Test
    void putSort路由到sort而不是被路径id吞掉() throws Exception {
        mockMvc.perform(put("/api/enterprise/long-modules/sort")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[2,1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 键名必须是 ids：Jackson 若没按 Map<String, List<Long>> 泛型还原，元素会是 Integer，这里即红；
        // 若路由被 /{id} 抢走，"sort" 转 Long 在进方法前就失败，sort 不会被调到。
        // 注意此时 HTTP 仍是 200（GlobalExceptionHandler 把异常包成 code:500 的正常响应），
        // 真正变红的是下面这条 $.code 断言与 sort 的 verify——别把它当成 status 的重复检查删掉
        verify(longModuleService).sort(List.of(2L, 1L), ENTERPRISE_ID);
        verify(longModuleService, never()).update(any(), any(), any());
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
    }

    @Test
    void create的企业归属取登录态而不是请求体() throws Exception {
        mockMvc.perform(post("/api/enterprise/long-modules")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"模块\",\"pages\":\"[\\\"/uploads/a.png\\\"]\",\"enterpriseId\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<TraceLongModule> formCaptor = ArgumentCaptor.forClass(TraceLongModule.class);
        // 第二参必须是登录态的 23 而不是请求体的 999；将来谁把取值从 getUserId() 换成别的来源，这条用例同样会红。
        // 刻意不锁 form.getEnterpriseId() 是否还是 999：本仓库两种写法都在用（EnterpriseController 直接在控制器里
        // 覆盖掉），对齐那种更严格的写法不该让这条用例变红。真正的防线是 LongModuleServiceTest 里
        // 「请求体企业 id 必须被登录态无条件冲掉」那条。
        verify(longModuleService).create(formCaptor.capture(), eq(ENTERPRISE_ID));
        assertEquals("模块", formCaptor.getValue().getName());
    }

    @Test
    void 未开通长图的企业调用任何长图端点都被守卫拦下() throws Exception {
        doThrow(new BusinessException("该模板未对您开放"))
                .when(templateGrantGuard)
                .assertRenderModeAvailable(ENTERPRISE_ID, TemplateGrantGuard.LONG_IMAGE);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/enterprise/long-modules"))
                .andExpect(status().isOk())
                // BusinessException 经 GlobalExceptionHandler 出去仍是 HTTP 200，
                // 所以断言必须打在 $.code 上，status() 在这里永远绿
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该模板未对您开放"));

        // 守卫必须在服务层之前：走到 service 就等于放行成功
        verifyNoInteractions(longModuleService);
    }
}
