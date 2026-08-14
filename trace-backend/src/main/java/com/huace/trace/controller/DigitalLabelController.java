package com.huace.trace.controller;

import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.common.Result;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.SysUserMapper;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.DigitalLabelService;
import com.huace.trace.service.DlAnalysisService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数字标签 - 企业端接口（管理员可全局只读查看）
 */
@RestController
@RequestMapping("/api/enterprise/dl")
@RequiredArgsConstructor
public class DigitalLabelController {

    private final DigitalLabelService dlService;
    private final DlAnalysisService analysisService;
    private final SysUserMapper sysUserMapper;

    private boolean isAdmin(UserPrincipal principal) {
        return "admin".equals(principal.getUserType());
    }

    /** 解析数据范围：管理员可按企业筛选（为空=全部企业），企业用户固定为自己 */
    private Long scopeEnterpriseId(UserPrincipal principal, Long requested) {
        return isAdmin(principal) ? requested : principal.getUserId();
    }

    /** 管理员仅可查看，不允许变更操作 */
    private void requireEnterprise(UserPrincipal principal) {
        if (isAdmin(principal)) throw new BusinessException("管理员仅可查看数字标签数据");
    }

    // ==================== 商品 ====================

    @GetMapping("/products")
    public Result<PageResult<DlProduct>> listProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String barcode,
            @RequestParam(required = false) String foodName,
            @RequestParam(required = false) String hasLabel,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.listProducts(scopeEnterpriseId(principal, enterpriseId), page, size,
                barcode, foodName, hasLabel, startDate, endDate));
    }

    @PostMapping("/products")
    public Result<DlProduct> createProduct(@RequestBody DlProduct product,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        requireEnterprise(principal);
        return Result.ok(dlService.createProduct(principal.getUserId(), product));
    }

    // ==================== 标签版本 ====================

    @GetMapping("/products/{productId}/versions")
    public Result<List<DlLabelVersion>> listVersions(
            @PathVariable Long productId,
            @RequestParam(required = false) String versionNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.listVersions(scopeEnterpriseId(principal, null), productId,
                versionNo, status, startDate, endDate));
    }

    @GetMapping("/versions/{id}")
    public Result<DlLabelVersion> getVersion(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.getVersion(scopeEnterpriseId(principal, null), id));
    }

    @PostMapping("/products/{productId}/versions")
    public Result<DlLabelVersion> createVersion(
            @PathVariable Long productId,
            @RequestParam(required = false) Long copyFromId,
            @AuthenticationPrincipal UserPrincipal principal) {
        requireEnterprise(principal);
        return Result.ok(dlService.createVersion(principal.getUserId(), productId,
                copyFromId, principal.getUsername()));
    }

    @PutMapping("/versions/{id}")
    public Result<DlLabelVersion> updateVersion(@PathVariable Long id,
                                                @RequestBody DlLabelVersion data,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        requireEnterprise(principal);
        return Result.ok(dlService.updateVersion(principal.getUserId(), id, data, principal.getUsername()));
    }

    @DeleteMapping("/versions/{id}")
    public Result<Void> deleteVersion(@PathVariable Long id,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        requireEnterprise(principal);
        dlService.deleteVersion(principal.getUserId(), id, principal.getUsername());
        return Result.ok();
    }

    @PutMapping("/versions/{id}/publish")
    public Result<DlLabelVersion> publishVersion(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        requireEnterprise(principal);
        return Result.ok(dlService.publishVersion(principal.getUserId(), id, principal.getUsername()));
    }

    @PutMapping("/versions/{id}/offline")
    public Result<DlLabelVersion> offlineVersion(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        requireEnterprise(principal);
        return Result.ok(dlService.offlineVersion(principal.getUserId(), id, principal.getUsername()));
    }

    // ==================== 商品同步 ====================

    @PostMapping("/sync")
    public Result<DlSyncRecord> manualSync(@RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        requireEnterprise(principal);
        return Result.ok(dlService.manualSync(principal.getUserId(),
                body.get("condition"), body.get("timeRange")));
    }

    @GetMapping("/sync/records")
    public Result<PageResult<DlSyncRecord>> listSyncRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.listSyncRecords(scopeEnterpriseId(principal, enterpriseId), page, size));
    }

    // ==================== 数据分析 ====================

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(
            @RequestParam(defaultValue = "7") int days,
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.dashboard(scopeEnterpriseId(principal, enterpriseId), days));
    }

    @GetMapping("/analysis/scan")
    public Result<Map<String, Object>> scanAnalysis(
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.scanAnalysis(scopeEnterpriseId(principal, enterpriseId)));
    }

    @GetMapping("/analysis/scan/detail")
    public Result<List<DlScanRecord>> scanDetail(@RequestParam Long versionId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.scanDetail(scopeEnterpriseId(principal, null), versionId));
    }

    @GetMapping("/analysis/geo")
    public Result<List<Map<String, Object>>> geoAnalysis(
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.geoAnalysis(scopeEnterpriseId(principal, enterpriseId)));
    }

    @GetMapping("/analysis/label")
    public Result<Map<String, Object>> labelAnalysis(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.labelAnalysis(scopeEnterpriseId(principal, enterpriseId), days));
    }

    @GetMapping("/analysis/product")
    public Result<Map<String, Object>> productAnalysis(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.productAnalysis(scopeEnterpriseId(principal, enterpriseId), days));
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<PageResult<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        Long scope = scopeEnterpriseId(principal, enterpriseId);
        if (scope != null) w.eq(SysUser::getEnterpriseId, scope);
        if (StringUtils.hasText(keyword)) {
            w.and(x -> x.like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickname, keyword)
                    .or().like(SysUser::getPhone, keyword));
        }
        w.orderByDesc(SysUser::getId);
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<SysUser> r =
                sysUserMapper.selectPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size), w);
        r.getRecords().forEach(u -> u.setPasswordHash(null));
        return Result.ok(new PageResult<>(r.getRecords(), r.getTotal()));
    }

    // ==================== 日志 ====================

    @GetMapping("/logs/operation")
    public Result<PageResult<DlOperationLog>> listOperationLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.listOperationLogs(scopeEnterpriseId(principal, enterpriseId), page, size,
                productName, operationType, startDate, endDate));
    }

    @GetMapping("/logs/login")
    public Result<PageResult<DlLoginLog>> listLoginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long enterpriseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.listLoginLogs(scopeEnterpriseId(principal, enterpriseId), page, size,
                loginType, startDate, endDate));
    }

    /** 记录登录日志（企业用户进入数字标签模块时调用） */
    @PostMapping("/logs/login")
    public Result<Void> recordLogin(@RequestHeader(value = "User-Agent", required = false) String userAgent,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        if (!isAdmin(principal)) {
            dlService.recordLogin(principal.getUserId(), principal.getUsername(), userAgent);
        }
        return Result.ok();
    }

    // ==================== 食品分类 / 企业列表 ====================

    @GetMapping("/categories")
    public Result<List<DlFoodCategory>> categories() {
        return Result.ok(dlService.categoryTree());
    }

    /** 已创建数字标签的企业列表（管理员企业筛选用） */
    @GetMapping("/enterprises")
    public Result<List<Map<String, Object>>> enterprises(@AuthenticationPrincipal UserPrincipal principal) {
        if (!isAdmin(principal)) return Result.ok(List.of());
        return Result.ok(dlService.dlEnterprises());
    }
}
