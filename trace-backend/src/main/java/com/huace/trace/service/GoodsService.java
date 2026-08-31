package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.Goods;
import com.huace.trace.entity.Product;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.GoodsMapper;
import com.huace.trace.mapper.ProductMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoodsService {
    private final GoodsMapper goodsMapper;
    private final ProductMapper productMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final TraceTemplateMapper traceTemplateMapper;
    private final TracePageService tracePageService;

    public PageResult<Goods> list(int page, int size, String keyword, Long enterpriseId) {
        LambdaQueryWrapper<Goods> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) w.eq(Goods::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(keyword)) w.like(Goods::getName, keyword);
        w.orderByDesc(Goods::getId);
        Page<Goods> r = goodsMapper.selectPage(new Page<>(page, size), w);
        List<Goods> records = r.getRecords();
        if (!records.isEmpty()) {
            // 批量预取 product/enterprise/traceTemplate，消除逐行查询
            java.util.Set<Long> productIds = records.stream().map(Goods::getProductId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            java.util.Set<Long> entIds = records.stream().map(Goods::getEnterpriseId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            java.util.Set<Long> templateIds = records.stream().map(Goods::getTraceTemplateId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            Map<Long, Product> productMap = productIds.isEmpty() ? java.util.Collections.emptyMap()
                    : productMapper.selectBatchIds(productIds).stream()
                            .collect(java.util.stream.Collectors.toMap(Product::getId, p -> p));
            Map<Long, Enterprise> entMap = entIds.isEmpty() ? java.util.Collections.emptyMap()
                    : enterpriseMapper.selectBatchIds(entIds).stream()
                            .collect(java.util.stream.Collectors.toMap(Enterprise::getId, e -> e));
            Map<Long, TraceTemplate> templateMap = templateIds.isEmpty() ? java.util.Collections.emptyMap()
                    : traceTemplateMapper.selectBatchIds(templateIds).stream()
                            .collect(java.util.stream.Collectors.toMap(TraceTemplate::getId, t -> t));
            records.forEach(g -> {
                Product p = productMap.get(g.getProductId());
                if (p != null) g.setProductName(p.getName());
                Enterprise e = entMap.get(g.getEnterpriseId());
                if (e != null) g.setEnterpriseName(e.getName());
                TraceTemplate tt = templateMap.get(g.getTraceTemplateId());
                if (tt != null) g.setTraceTemplateName(tt.getTemplateName());
            });
        }
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(Goods g) { goodsMapper.insert(g); tracePageService.evictAllCache(); }

    public void update(Long id, Goods g, Long enterpriseId) {
        Goods existing = goodsMapper.selectById(id);
        if (existing == null) throw new BusinessException("商品不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        g.setId(id);
        g.setEnterpriseId(enterpriseId);
        goodsMapper.updateById(g);
        tracePageService.evictAllCache();
    }

    public void delete(Long id, Long enterpriseId) {
        Goods existing = goodsMapper.selectById(id);
        if (existing == null) throw new BusinessException("商品不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        goodsMapper.deleteById(id);
        tracePageService.evictAllCache();
    }

    /** 复制商品（名称加“（副本）”后缀） */
    public Goods copy(Long id, Long enterpriseId) {
        Goods existing = goodsMapper.selectById(id);
        if (existing == null) throw new BusinessException("商品不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        Goods ng = new Goods();
        ng.setName(existing.getName() + "（副本）");
        ng.setProductId(existing.getProductId());
        ng.setEnterpriseId(existing.getEnterpriseId());
        ng.setPackageSpec(existing.getPackageSpec());
        ng.setShowOuterPackage(existing.getShowOuterPackage());
        ng.setWeightSpec(existing.getWeightSpec());
        ng.setSampleImage(existing.getSampleImage());
        ng.setIntroduction(existing.getIntroduction());
        ng.setStorageMethod(existing.getStorageMethod());
        ng.setEatingMethod(existing.getEatingMethod());
        ng.setPromoImage(existing.getPromoImage());
        ng.setPromoVideo(existing.getPromoVideo());
        ng.setTraceTemplateId(existing.getTraceTemplateId());
        goodsMapper.insert(ng);
        return ng;
    }
}
