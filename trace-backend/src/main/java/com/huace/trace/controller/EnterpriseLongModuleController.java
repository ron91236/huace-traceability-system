package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.entity.TraceLongModule;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.LongModuleService;
import com.huace.trace.service.TemplateGrantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 长图溯源模块的企业端配置接口；企业归属只认登录态，忽略请求体传入的 enterpriseId。
 * 每个端点先过 TemplateGrantGuard：未被供应商显式开放长图模板的企业一律拒绝——
 * 前端把菜单隐藏掉不是安全边界。
 */
@RestController
@RequestMapping("/api/enterprise/long-modules")
@RequiredArgsConstructor
public class EnterpriseLongModuleController {

    private final LongModuleService longModuleService;
    private final TemplateGrantGuard templateGrantGuard;

    @GetMapping
    public Result<List<TraceLongModule>> list(@AuthenticationPrincipal UserPrincipal principal) {
        templateGrantGuard.assertRenderModeAvailable(principal.getUserId(), TemplateGrantGuard.LONG_IMAGE);
        return Result.ok(longModuleService.list(principal.getUserId()));
    }

    @PostMapping
    public Result<Void> create(@RequestBody TraceLongModule form,
                               @AuthenticationPrincipal UserPrincipal principal) {
        templateGrantGuard.assertRenderModeAvailable(principal.getUserId(), TemplateGrantGuard.LONG_IMAGE);
        longModuleService.create(form, principal.getUserId());
        return Result.ok();
    }

    @PutMapping("/sort")
    public Result<Void> sort(@RequestBody Map<String, List<Long>> body,
                             @AuthenticationPrincipal UserPrincipal principal) {
        templateGrantGuard.assertRenderModeAvailable(principal.getUserId(), TemplateGrantGuard.LONG_IMAGE);
        longModuleService.sort(body.get("ids"), principal.getUserId());
        return Result.ok();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id,
                               @RequestBody TraceLongModule form,
                               @AuthenticationPrincipal UserPrincipal principal) {
        templateGrantGuard.assertRenderModeAvailable(principal.getUserId(), TemplateGrantGuard.LONG_IMAGE);
        longModuleService.update(id, form, principal.getUserId());
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @AuthenticationPrincipal UserPrincipal principal) {
        templateGrantGuard.assertRenderModeAvailable(principal.getUserId(), TemplateGrantGuard.LONG_IMAGE);
        longModuleService.delete(id, principal.getUserId());
        return Result.ok();
    }

    /**
     * 上传后的 PDF 转整页图，返回页图 URL 数组供前端预览确认后再保存。
     * 登录态参数曾被当作"未使用"去掉，现在守卫要用它——PDF 转换会落盘并写缓存，
     * 未开通的企业不该能白嫖这个重操作。
     */
    @PostMapping("/convert")
    public Result<List<String>> convert(@RequestBody Map<String, String> body,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        templateGrantGuard.assertRenderModeAvailable(principal.getUserId(), TemplateGrantGuard.LONG_IMAGE);
        return Result.ok(longModuleService.convertPdf(body.get("url")));
    }
}
