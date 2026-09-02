package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.dto.HgzPublicVO;
import com.huace.trace.service.HgzService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 合格证公开查询接口（扫码访问，5分钟Redis缓存）
 */
@RestController
@RequestMapping("/api/hgz")
@RequiredArgsConstructor
public class HgzPublicController {

    private final HgzService hgzService;

    @GetMapping("/{code}")
    public Result<HgzPublicVO> detail(@PathVariable String code) {
        return Result.ok(hgzService.publicDetail(code));
    }
}
