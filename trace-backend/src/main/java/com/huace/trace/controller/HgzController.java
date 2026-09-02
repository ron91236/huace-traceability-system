package com.huace.trace.controller;

import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.common.Result;
import com.huace.trace.dto.HgzSaveRequest;
import com.huace.trace.entity.Hgz;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.HgzService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 农产品承诺达标合格证 - 企业端接口（管理员可全局查看/作废）
 */
@RestController
@RequestMapping("/api/enterprise/hgz")
@RequiredArgsConstructor
public class HgzController {

    private final HgzService hgzService;

    private boolean isAdmin(UserPrincipal principal) {
        return "admin".equals(principal.getUserType());
    }

    /** 解析数据范围：管理员可按企业筛选（为空=全部企业），企业用户固定为自己 */
    private Long scopeEnterpriseId(UserPrincipal principal, Long requested) {
        return isAdmin(principal) ? requested : principal.getUserId();
    }

    /** 管理员仅可查看与作废，不允许开证/编辑 */
    private void requireEnterprise(UserPrincipal principal) {
        if (isAdmin(principal)) throw new BusinessException("管理员仅可查看合格证数据");
    }

    @GetMapping
    public Result<PageResult<Hgz>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(hgzService.list(scopeEnterpriseId(principal, enterpriseId), page, size, keyword));
    }

    /** 默认承诺事项/依据选项（法定要素模板） */
    @GetMapping("/defaults")
    public Result<Map<String, Object>> defaults() {
        return Result.ok(hgzService.defaults());
    }

    @PostMapping
    public Result<Hgz> create(@RequestBody HgzSaveRequest req,
                              @AuthenticationPrincipal UserPrincipal principal) {
        requireEnterprise(principal);
        return Result.ok(hgzService.create(req, principal.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<Hgz> detail(@PathVariable Long id,
                              @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(hgzService.detail(id, scopeEnterpriseId(principal, null), isAdmin(principal)));
    }

    @PutMapping("/{id}")
    public Result<Hgz> update(@PathVariable Long id, @RequestBody HgzSaveRequest req,
                              @AuthenticationPrincipal UserPrincipal principal) {
        requireEnterprise(principal);
        return Result.ok(hgzService.update(id, req, principal.getUserId()));
    }

    @PostMapping("/{id}/void")
    public Result<Hgz> voidCert(@PathVariable Long id,
                                @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(hgzService.voidCert(id, scopeEnterpriseId(principal, null), isAdmin(principal)));
    }

    @GetMapping("/{id}/qrcode")
    public Result<String> qrcode(@PathVariable Long id,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(hgzService.qrcode(id, scopeEnterpriseId(principal, null), isAdmin(principal)));
    }
}
