package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.entity.SysFile;
import com.huace.trace.mapper.SysFileMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import com.huace.trace.service.EnterpriseCertService;
import com.huace.trace.service.TracePageService;
import com.huace.trace.service.TraceTemplateService;
import com.huace.trace.util.FileUploadUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TracePageController {

    private final EnterpriseCertService enterpriseCertService;
    private final TracePageService tracePageService;
    private final TraceTemplateMapper templateMapper;
    private final TraceTemplateService traceTemplateService;
    private final FileUploadUtil fileUploadUtil;
    private final SysFileMapper sysFileMapper;

    /**
     * C端批次溯源查询 - 通过批次ID查询溯源信息
     */
    @GetMapping("/api/trace/batch/{batchId}")
    public Result<Map<String, Object>> queryBatchTrace(@PathVariable Long batchId) {
        return Result.ok(tracePageService.queryByBatchId(batchId));
    }

    /**
     * C端溯源查询 - 通过流水号查询溯源信息
     */
    @GetMapping("/api/trace/{serialNo}")
    public Result<Map<String, Object>> queryTrace(@PathVariable String serialNo, HttpServletRequest request) {
        return Result.ok(tracePageService.queryBySerialNo(serialNo, request));
    }

    /**
     * 防伪码验证 - 返回详细结果
     */
    @PostMapping("/api/trace/verify")
    public Result<Map<String, Object>> verifyAntiFakeCode(@RequestBody Map<String, String> body) {
        return Result.ok(tracePageService.verifyAntiFakeCode(
                body.get("serialNo"), body.get("antiFakeCode")));
    }

    /**
     * 扫码即防伪验证 - 不需要防伪码，扫码直接验证
     */
    @GetMapping("/api/trace/direct-verify/{serialNo}")
    public Result<Map<String, Object>> directVerify(@PathVariable String serialNo) {
        return Result.ok(tracePageService.directVerifyBySerialNo(serialNo));
    }

    /**
     * 证书公开查询 - 扫码查看证书信息
     */
    @GetMapping("/api/trace/cert/{id}")
    public Result<Map<String, Object>> getCertPublicInfo(@PathVariable Long id) {
        return Result.ok(enterpriseCertService.getCertPublicInfo(id));
    }

    /**
     * 按模板标识获取模板配置
     */
    @GetMapping("/api/trace/template/{templateKey}")
    public Result<Map<String, Object>> getTemplateByKey(@PathVariable String templateKey) {
        return Result.ok(traceTemplateService.getByKey(templateKey));
    }

    /**
     * 通用文件上传
     */
    @PostMapping("/api/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                               @AuthenticationPrincipal com.huace.trace.security.UserPrincipal principal) throws Exception {
        Long uploaderId = principal != null ? principal.getUserId() : null;
        SysFile sysFile = fileUploadUtil.upload(file, uploaderId);
        sysFileMapper.insert(sysFile);

        return Result.ok(Map.of(
                "id", sysFile.getId(),
                "url", fileUploadUtil.getAccessUrl(sysFile.getFilePath()),
                "name", sysFile.getOriginalName(),
                "size", sysFile.getFileSize()
        ));
    }
}
package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.entity.SysFile;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.SysFileMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import com.huace.trace.service.EnterpriseCertService;
import com.huace.trace.service.TracePageService;
import com.huace.trace.service.TraceTemplateService;
import com.huace.trace.util.FileUploadUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TracePageController {

    private final EnterpriseCertService enterpriseCertService;
    private final TracePageService tracePageService;
    private final TraceTemplateMapper templateMapper;
    private final TraceTemplateService traceTemplateService;
    private final FileUploadUtil fileUploadUtil;
    private final SysFileMapper sysFileMapper;

    /**
     * C端批次溯源查询 - 通过批次ID查询溯源信息
     */
    @GetMapping("/api/trace/batch/{batchId}")
    public Result<Map<String, Object>> queryBatchTrace(@PathVariable Long batchId) {
        return Result.ok(tracePageService.queryByBatchId(batchId));
    }

    /**
     * C端溯源查询 - 通过流水号查询溯源信息
     */
    @GetMapping("/api/trace/{serialNo}")
    public Result<Map<String, Object>> queryTrace(@PathVariable String serialNo, HttpServletRequest request) {
        return Result.ok(tracePageService.queryBySerialNo(serialNo, request));
    }

    /**
     * 防伪码验证 - 返回详细结果
     */
    @PostMapping("/api/trace/verify")
    public Result<Map<String, Object>> verifyAntiFakeCode(@RequestBody Map<String, String> body) {
        return Result.ok(tracePageService.verifyAntiFakeCode(
                body.get("serialNo"), body.get("antiFakeCode")));
    }

    /**
     * 扫码即防伪验证 - 不需要防伪码，扫码直接验证
     */
    @GetMapping("/api/trace/direct-verify/{serialNo}")
    public Result<Map<String, Object>> directVerify(@PathVariable String serialNo) {
        return Result.ok(tracePageService.directVerifyBySerialNo(serialNo));
    }

    /**
     * 证书公开查询 - 扫码查看证书信息
     */
    @GetMapping("/api/trace/cert/{id}")
    public Result<Map<String, Object>> getCertPublicInfo(@PathVariable Long id) {
        return Result.ok(enterpriseCertService.getCertPublicInfo(id));
    }

    /**
     * 获取所有溯源模板
     */
    @GetMapping("/api/trace/templates")
    public Result<List<TraceTemplate>> listTemplates() {
        return Result.ok(templateMapper.selectList(
                new LambdaQueryWrapper<TraceTemplate>()
                        .eq(TraceTemplate::getStatus, 1)
                        .orderByAsc(TraceTemplate::getId)));
    }

    /**
     * 按模板标识获取模板配置
     */
    @GetMapping("/api/trace/template/{templateKey}")
    public Result<Map<String, Object>> getTemplateByKey(@PathVariable String templateKey) {
        return Result.ok(traceTemplateService.getByKey(templateKey));
    }

    /**
     * 通用文件上传
     */
    @PostMapping("/api/upload")
    public Result<Map<String, Object>> upload(@RequestParam("file") MultipartFile file,
                                               @AuthenticationPrincipal com.huace.trace.security.UserPrincipal principal) throws Exception {
        Long uploaderId = principal != null ? principal.getUserId() : null;
        SysFile sysFile = fileUploadUtil.upload(file, uploaderId);
        sysFileMapper.insert(sysFile);

        return Result.ok(Map.of(
                "id", sysFile.getId(),
                "url", fileUploadUtil.getAccessUrl(sysFile.getFilePath()),
                "name", sysFile.getOriginalName(),
                "size", sysFile.getFileSize()
        ));
    }
}
