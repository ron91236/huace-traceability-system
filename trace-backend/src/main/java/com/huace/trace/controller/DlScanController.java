package com.huace.trace.controller;

import com.huace.trace.common.BusinessException;
import com.huace.trace.common.Result;
import com.huace.trace.service.DigitalLabelService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 数字标签 - 消费者扫码公开接口
 */
@RestController
@RequestMapping("/api/dl/scan")
@RequiredArgsConstructor
public class DlScanController {

    private final DigitalLabelService dlService;

    @GetMapping("/{barcode}")
    public Result<Map<String, Object>> scan(@PathVariable String barcode,
                                            @RequestHeader(value = "User-Agent", required = false) String userAgent,
                                            HttpServletRequest request) {
        if (!StringUtils.hasText(barcode) || barcode.length() > 50) {
            throw new BusinessException("条码无效");
        }
        return Result.ok(dlService.getScanData(barcode, getClientIp(request), userAgent));
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
            int idx = ip.indexOf(',');
            return idx > 0 ? ip.substring(0, idx).trim() : ip.trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(ip)) return ip;
        return request.getRemoteAddr();
    }
}
