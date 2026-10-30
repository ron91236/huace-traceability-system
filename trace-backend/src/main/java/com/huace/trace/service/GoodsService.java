package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.CodePackageItem;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.Goods;
import com.huace.trace.entity.OrderCode;
import com.huace.trace.entity.Product;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.CodePackageItemMapper;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.GoodsMapper;
import com.huace.trace.mapper.OrderCodeMapper;
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
    /**
     * 放在最后：@RequiredArgsConstructor 的字段顺序就是构造参数顺序。
     * 这三个只为"商品换模板要同步已绑定的码"服务。
     */
    private final CodePackageItemMapper codePackageItemMapper;
    private final OrderCodeMapper orderCodeMapper;
    private final MongoCodeItemService mongoCodeItemService;

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
        syncBoundCodesTemplate(id, g.getTraceTemplateId(), existing.getTraceTemplateId());
        tracePageService.evictAllCache();
    }

    /**
     * 商品换溯源模板要把它已绑定的码一起带走：扫码页读的是 code_package_item 上那份模板快照，
     * 不同步的话企业换了模板，已经印出去的码仍按旧模板渲染。
     *
     * <p>{@code newTemplateId} 为 null 表示这次 PUT 没带这个字段——updateById 的 NOT_NULL 策略下
     * 商品列本身也不会变，所以码上的值同样要原样留着，不能当成"清空模板"抹掉。
     */
    private void syncBoundCodesTemplate(Long goodsId, Long newTemplateId, Long oldTemplateId) {
        if (newTemplateId == null || newTemplateId.equals(oldTemplateId)) return;
        TraceTemplate template = traceTemplateMapper.selectById(newTemplateId);
        if (template == null) return;
        String key = template.getTemplateKey();

        codePackageItemMapper.update(null, new LambdaUpdateWrapper<CodePackageItem>()
                .eq(CodePackageItem::getGoodsId, goodsId)
                .eq(CodePackageItem::getBindStatus, "BOUND")
                .set(CodePackageItem::getTraceTemplate, key));

        // 条码段上的模板也一起改：它是"编辑订单条码"再下发时的默认值，留着旧值等于埋一颗
        // 会把码改回旧模板的雷。段与商品没有直接外键，只能从这些码自己的 order_code_id 反推。
        List<Long> segmentIds = codePackageItemMapper.selectList(new LambdaQueryWrapper<CodePackageItem>()
                        .select(CodePackageItem::getOrderCodeId)
                        .eq(CodePackageItem::getGoodsId, goodsId)
                        .eq(CodePackageItem::getBindStatus, "BOUND")
                        .isNotNull(CodePackageItem::getOrderCodeId)
                        .groupBy(CodePackageItem::getOrderCodeId))
                .stream().map(CodePackageItem::getOrderCodeId).toList();
        if (!segmentIds.isEmpty()) {
            orderCodeMapper.update(null, new LambdaUpdateWrapper<OrderCode>()
                    .in(OrderCode::getId, segmentIds)
                    .set(OrderCode::getTraceTemplate, key));
        }

        mongoCodeItemService.updateTraceTemplateByGoodsId(goodsId, key);
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
