package com.huace.trace.controller;

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
 * 数字标签 - 企业端接口
 */
@RestController
@RequestMapping("/api/enterprise/dl")
@RequiredArgsConstructor
public class DigitalLabelController {

    private final DigitalLabelService dlService;
    private final DlAnalysisService analysisService;
    private final SysUserMapper sysUserMapper;

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
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.listProducts(principal.getUserId(), page, size,
                barcode, foodName, hasLabel, startDate, endDate));
    }

    @PostMapping("/products")
    public Result<DlProduct> createProduct(@RequestBody DlProduct product,
                                           @AuthenticationPrincipal UserPrincipal principal) {
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
        return Result.ok(dlService.listVersions(principal.getUserId(), productId,
                versionNo, status, startDate, endDate));
    }

    @GetMapping("/versions/{id}")
    public Result<DlLabelVersion> getVersion(@PathVariable Long id,
                                             @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.getVersion(principal.getUserId(), id));
    }

    @PostMapping("/products/{productId}/versions")
    public Result<DlLabelVersion> createVersion(
            @PathVariable Long productId,
            @RequestParam(required = false) Long copyFromId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.createVersion(principal.getUserId(), productId,
                copyFromId, principal.getUsername()));
    }

    @PutMapping("/versions/{id}")
    public Result<DlLabelVersion> updateVersion(@PathVariable Long id,
                                                @RequestBody DlLabelVersion data,
                                                @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.updateVersion(principal.getUserId(), id, data, principal.getUsername()));
    }

    @DeleteMapping("/versions/{id}")
    public Result<Void> deleteVersion(@PathVariable Long id,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        dlService.deleteVersion(principal.getUserId(), id, principal.getUsername());
        return Result.ok();
    }

    @PutMapping("/versions/{id}/publish")
    public Result<DlLabelVersion> publishVersion(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.publishVersion(principal.getUserId(), id, principal.getUsername()));
    }

    @PutMapping("/versions/{id}/offline")
    public Result<DlLabelVersion> offlineVersion(@PathVariable Long id,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.offlineVersion(principal.getUserId(), id, principal.getUsername()));
    }

    // ==================== 商品同步 ====================

    @PostMapping("/sync")
    public Result<DlSyncRecord> manualSync(@RequestBody Map<String, String> body,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.manualSync(principal.getUserId(),
                body.get("condition"), body.get("timeRange")));
    }

    @GetMapping("/sync/records")
    public Result<PageResult<DlSyncRecord>> listSyncRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.listSyncRecords(principal.getUserId(), page, size));
    }

    // ==================== 数据分析 ====================

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard(
            @RequestParam(defaultValue = "7") int days,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.dashboard(principal.getUserId(), days));
    }

    @GetMapping("/analysis/scan")
    public Result<Map<String, Object>> scanAnalysis(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.scanAnalysis(principal.getUserId()));
    }

    @GetMapping("/analysis/scan/detail")
    public Result<List<DlScanRecord>> scanDetail(@RequestParam Long versionId,
                                                 @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.scanDetail(principal.getUserId(), versionId));
    }

    @GetMapping("/analysis/geo")
    public Result<List<Map<String, Object>>> geoAnalysis(@AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.geoAnalysis(principal.getUserId()));
    }

    @GetMapping("/analysis/label")
    public Result<Map<String, Object>> labelAnalysis(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.labelAnalysis(principal.getUserId(), days));
    }

    @GetMapping("/analysis/product")
    public Result<Map<String, Object>> productAnalysis(
            @RequestParam(defaultValue = "30") int days,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(analysisService.productAnalysis(principal.getUserId(), days));
    }

    // ==================== 用户管理 ====================

    @GetMapping("/users")
    public Result<PageResult<SysUser>> listUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal principal) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.eq(SysUser::getEnterpriseId, principal.getUserId());
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
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.listOperationLogs(principal.getUserId(), page, size,
                productName, operationType, startDate, endDate));
    }

    @GetMapping("/logs/login")
    public Result<PageResult<DlLoginLog>> listLoginLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String loginType,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(dlService.listLoginLogs(principal.getUserId(), page, size,
                loginType, startDate, endDate));
    }

    /** 记录登录日志（企业用户进入数字标签模块时调用） */
    @PostMapping("/logs/login")
    public Result<Void> recordLogin(@RequestHeader(value = "User-Agent", required = false) String userAgent,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        dlService.recordLogin(principal.getUserId(), principal.getUsername(), userAgent);
        return Result.ok();
    }

    // ==================== 食品分类 ====================

    @GetMapping("/categories")
    public Result<List<DlFoodCategory>> categories() {
        return Result.ok(dlService.categoryTree());
    }
}
