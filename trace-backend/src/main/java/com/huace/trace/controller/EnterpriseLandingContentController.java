package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.LandingContentService;
import com.huace.trace.service.TemplateGrantGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 落地页区块内容的企业端接口。
 *
 * <p>企业只能往模板定义好的骨架里填内容：区块有哪些、叫什么、什么顺序、启不启用
 * 都归管理端的落地页设计器，所以这里只有"读骨架 + 写内容"两个动作。
 * 每个端点先过 TemplateGrantGuard.requireGranted(..., LANDING)——
 * assigned_template_ids 必须显式含这个 templateId，前端把菜单隐藏掉不是安全边界。
 */
@RestController
@RequestMapping("/api/enterprise/landing-content")
@RequiredArgsConstructor
public class EnterpriseLandingContentController {

    private final LandingContentService landingContentService;
    private final TemplateGrantGuard templateGrantGuard;

    /** 区块骨架 ⊕ 本企业已填内容；未填的区块带空串出现，骨架外的历史行不出现 */
    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long templateId,
                                                  @AuthenticationPrincipal UserPrincipal principal) {
        TraceTemplate template = templateGrantGuard.requireGranted(
                templateId, principal.getUserId(), TemplateGrantGuard.LANDING);
        return Result.ok(landingContentService.list(template, principal.getUserId()));
    }

    /** 写某个区块的内容。路径 (templateId, blockKey) 就是行坐标，请求体只带 content */
    @PutMapping("/{templateId}/{blockKey}")
    public Result<Void> saveContent(@PathVariable Long templateId,
                                    @PathVariable String blockKey,
                                    @RequestBody Map<String, String> body,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        TraceTemplate template = templateGrantGuard.requireGranted(
                templateId, principal.getUserId(), TemplateGrantGuard.LANDING);
        landingContentService.saveContent(template, blockKey, body.get("content"), principal.getUserId());
        return Result.ok();
    }
}
