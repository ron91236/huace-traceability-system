package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.entity.CertType;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.LabelSpec;
import com.huace.trace.entity.Product;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.CertTypeMapper;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.LabelSpecMapper;
import com.huace.trace.mapper.ProductMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final ProductMapper productMapper;
    private final LabelSpecMapper labelSpecMapper;
    private final CertTypeMapper certTypeMapper;
    private final TraceTemplateMapper templateMapper;
    private final EnterpriseMapper enterpriseMapper;

    @GetMapping("/products")
    public Result<List<Product>> getProducts() {
        return Result.ok(productMapper.selectList(
                new LambdaQueryWrapper<Product>().orderByDesc(Product::getId)));
    }

    @GetMapping("/label-specs")
    public Result<List<LabelSpec>> getLabelSpecs() {
        return Result.ok(labelSpecMapper.selectList(
                new LambdaQueryWrapper<LabelSpec>()
                        .eq(LabelSpec::getIsVoid, 0)
                        .orderByDesc(LabelSpec::getId)));
    }

    @GetMapping("/cert-types")
    public Result<List<CertType>> getCertTypes() {
        return Result.ok(certTypeMapper.selectList(
                new LambdaQueryWrapper<CertType>().orderByDesc(CertType::getId)));
    }

    @GetMapping("/trace-templates")
    public Result<List<TraceTemplate>> getTraceTemplates(@RequestParam(required = false) Long enterpriseId) {
        // 指定企业时，仅返回分配给该企业的模板；企业未配置分配列表（空）则视为全部可用
        if (enterpriseId != null) {
            Enterprise e = enterpriseMapper.selectById(enterpriseId);
            if (e != null && StringUtils.hasText(e.getAssignedTemplateIds())) {
                List<Long> ids = Arrays.stream(e.getAssignedTemplateIds().split(","))
                        .filter(s -> !s.isBlank())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    return Result.ok(templateMapper.selectList(
                            new LambdaQueryWrapper<TraceTemplate>()
                                    .eq(TraceTemplate::getStatus, 1)
                                    .in(TraceTemplate::getId, ids)
                                    .orderByAsc(TraceTemplate::getId)));
                }
            }
        }
        return Result.ok(templateMapper.selectList(
                new LambdaQueryWrapper<TraceTemplate>()
                        .eq(TraceTemplate::getStatus, 1)
                        .orderByAsc(TraceTemplate::getId)));
    }
}
