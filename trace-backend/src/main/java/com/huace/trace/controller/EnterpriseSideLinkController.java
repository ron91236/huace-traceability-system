package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.entity.EnterpriseSideLink;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.SideLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 企业侧边栏外链的配置接口。
 *
 * <p>外链是企业的固有资产、与模板无关（spec 决定 4），所以这里刻意**不**加落地页授权守卫：
 * 还没开通落地页的企业也该先把公众号/微博链接存好，等开通那天直接可用。
 * 企业归属一律取登录态，请求体里的 enterpriseId 不作数。
 */
@RestController
@RequestMapping("/api/enterprise/side-links")
@RequiredArgsConstructor
public class EnterpriseSideLinkController {

    private final SideLinkService sideLinkService;

    /** 含停用项，按 sort_order 升序；企业端要看得见自己关掉的按钮才能重新打开 */
    @GetMapping
    public Result<List<EnterpriseSideLink>> list(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(sideLinkService.list(principal.getUserId()));
    }

    @PostMapping
    public Result<Void> create(@RequestBody EnterpriseSideLink form,
                               @AuthenticationPrincipal UserPrincipal principal) {
        sideLinkService.create(form, principal.getUserId());
        return Result.ok();
    }

    /** 必须排在 /{id} 之前？不需要——Spring 的精确路径优先匹配会让 /sort 落到这里，测试里钉着这条 */
    @PutMapping("/sort")
    public Result<Void> sort(@RequestBody Map<String, List<Long>> body,
                             @AuthenticationPrincipal UserPrincipal principal) {
        sideLinkService.sort(body.get("ids"), principal.getUserId());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody EnterpriseSideLink form,
                               @AuthenticationPrincipal UserPrincipal principal) {
        sideLinkService.update(id, form, principal.getUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal UserPrincipal principal) {
        sideLinkService.delete(id, principal.getUserId());
        return Result.ok();
    }
}
