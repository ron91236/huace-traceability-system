package com.huace.trace.controller;

import com.huace.trace.common.GlobalExceptionHandler;
import com.huace.trace.entity.EnterpriseSideLink;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.SideLinkService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 企业端外链控制器的契约测试。两件事只有经控制器才会坏：
 * 1. PUT /sort 路由到 sort，而不是被 PUT /{id} 的 @PathVariable 吞掉，
 *    且请求体键名就是 "ids"（P2 把长图的排序端点删了，这条防线现在只剩这里还需要）；
 * 2. 企业归属取登录态主体，请求体传入的 enterpriseId 影响不到服务入参。
 */
@ExtendWith(MockitoExtension.class)
class EnterpriseSideLinkControllerTest {

    private static final long ENTERPRISE_ID = 8L;

    @Mock private SideLinkService sideLinkService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EnterpriseSideLinkController(sideLinkService))
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

    @Test
    void putSort路由到sort而不是被路径id吞掉() throws Exception {
        mockMvc.perform(put("/api/enterprise/side-links/sort")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ids\":[2,1]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 键名必须是 ids：Jackson 若没按 Map<String, List<Long>> 泛型还原，元素会是 Integer，这里即红；
        // 若路由被 /{id} 抢走，"sort" 转 Long 在进方法前就失败，sort 不会被调到。
        // 注意此时 HTTP 仍是 200（GlobalExceptionHandler 把异常包成 code:500 的正常响应），
        // 真正变红的是下面这条 $.code 断言与 sort 的 verify——别当成 status 的重复检查删掉
        verify(sideLinkService).sort(List.of(2L, 1L), ENTERPRISE_ID);
        verify(sideLinkService, never()).update(any(), any(), any());
    }

    @Test
    void 路径id与登录态企业分别落到服务参数() throws Exception {
        mockMvc.perform(put("/api/enterprise/side-links/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"公众号\",\"url\":\"https://a\",\"enterpriseId\":999}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        // 第二参必须是登录态的 8 而不是请求体的 999；服务内部还会把归属再覆盖一次，
        // 这里是第一道：将来谁把取值从 getUserId() 换掉，这条会红
        verify(sideLinkService).update(eq(5L), any(EnterpriseSideLink.class), eq(ENTERPRISE_ID));
    }

    @Test
    void 新建走请求体加登录态企业() throws Exception {
        mockMvc.perform(post("/api/enterprise/side-links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"微博\",\"url\":\"https://b\"}"))
                .andExpect(jsonPath("$.code").value(200));

        verify(sideLinkService).create(any(EnterpriseSideLink.class), eq(ENTERPRISE_ID));
    }

    @Test
    void 删除与列表也用登录态企业() throws Exception {
        mockMvc.perform(delete("/api/enterprise/side-links/7"))
                .andExpect(jsonPath("$.code").value(200));
        verify(sideLinkService).delete(7L, ENTERPRISE_ID);

        mockMvc.perform(get("/api/enterprise/side-links"))
                .andExpect(jsonPath("$.code").value(200));
        verify(sideLinkService).list(ENTERPRISE_ID);
    }
}
