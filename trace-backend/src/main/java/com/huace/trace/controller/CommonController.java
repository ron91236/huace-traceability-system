package com.huace.trace.controller;

import com.huace.trace.common.Result;
import com.huace.trace.entity.CertType;
import com.huace.trace.entity.LabelSpec;
import com.huace.trace.entity.Product;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.CertTypeMapper;
import com.huace.trace.mapper.LabelSpecMapper;
import com.huace.trace.mapper.ProductMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/common")
@RequiredArgsConstructor
public class CommonController {

    private final ProductMapper productMapper;
    private final LabelSpecMapper labelSpecMapper;
    private final CertTypeMapper certTypeMapper;
    private final TraceTemplateMapper templateMapper;

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
    public Result<List<TraceTemplate>> getTraceTemplates() {
        return Result.ok(templateMapper.selectList(
                new LambdaQueryWrapper<TraceTemplate>()
                        .eq(TraceTemplate::getStatus, 1)
                        .orderByAsc(TraceTemplate::getId)));
    }
}
