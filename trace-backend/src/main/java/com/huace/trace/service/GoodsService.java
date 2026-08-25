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
        r.getRecords().forEach(g -> {
            if (g.getProductId() != null) {
                Product p = productMapper.selectById(g.getProductId());
                if (p != null) g.setProductName(p.getName());
            }
            if (g.getEnterpriseId() != null) {
                Enterprise e = enterpriseMapper.selectById(g.getEnterpriseId());
                if (e != null) g.setEnterpriseName(e.getName());
            }
            if (g.getTraceTemplateId() != null) {
                TraceTemplate tt = traceTemplateMapper.selectById(g.getTraceTemplateId());
                if (tt != null) g.setTraceTemplateName(tt.getTemplateName());
            }
        });
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
