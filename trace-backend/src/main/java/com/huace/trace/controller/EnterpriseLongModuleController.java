package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.LongModuleService;
import com.huace.trace.service.TemplateGrantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 长图溯源内容的企业端接口。
 *
 * <p>P2 起企业只能往模板定义好的骨架里填整页图：建模块、改名、模块排序、删模块、启停、
 * 适用商品这六个动作都归管理端的模板编辑器，所以这里没有 POST / DELETE / PUT /sort。
 * 每个端点先过 TemplateGrantGuard——assigned_template_ids 必须显式含这个 templateId，
 * 前端把菜单隐藏掉不是安全边界。
 */
@RestController
@RequestMapping("/api/enterprise/long-modules")
@RequiredArgsConstructor
public class EnterpriseLongModuleController {

    private final LongModuleService longModuleService;
    private final TemplateGrantGuard templateGrantGuard;

    /** 模块骨架 ⊕ 本企业已填页图；未填的模块带空数组出现，骨架外的历史行不出现 */
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long templateId,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        TraceTemplate template = templateGrantGuard.requireGranted(
                templateId, principal.getUserId(), TemplateGrantGuard.LONG_IMAGE);
        return Result.ok(longModuleService.list(template, principal.getUserId()));
    }

    /** 写某个模块的整页图。路径 (templateId, key) 就是行坐标，请求体只带 pages */
    @PutMapping("/{templateId}/{key}")
    public Result<Void> savePages(@PathVariable Long templateId,
                                  @PathVariable String key,
                                  @RequestBody Map<String, List<String>> body,
                                  @AuthenticationPrincipal UserPrincipal principal) {
        TraceTemplate template = templateGrantGuard.requireGranted(
                templateId, principal.getUserId(), TemplateGrantGuard.LONG_IMAGE);
        longModuleService.savePages(template, key, body.get("pages"), principal.getUserId());
        return Result.ok();
    }

    /**
     * 上传后的 PDF 转整页图，返回页图 URL 数组供前端预览确认后再保存。
     * 这个端点与具体模板无关（只读不写），所以仍按渲染模式守卫。
     */
    @PostMapping("/convert")
    public Result<List<String>> convert(@RequestBody Map<String, String> body,
                                        @AuthenticationPrincipal UserPrincipal principal) {
        templateGrantGuard.assertRenderModeAvailable(principal.getUserId(), TemplateGrantGuard.LONG_IMAGE);
        return Result.ok(longModuleService.convertPdf(body.get("url")));
    }
}
