package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final EnterpriseMapper enterpriseMapper;
    private final GoodsMapper goodsMapper;
    private final OrderMapper orderMapper;
    private final CodePackageMapper codePackageMapper;
    private final CodePackageItemMapper codePackageItemMapper;
    private final EnterpriseCertMapper certMapper;
    private final BatchMapper batchMapper;

    /**
     * 华测管理端统计
     */
    public Map<String, Object> adminStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("enterpriseCount", enterpriseMapper.selectCount(null));
        stats.put("goodsCount", goodsMapper.selectCount(null));
        stats.put("orderCount", orderMapper.selectCount(null));
        stats.put("pendingOrderCount", orderMapper.selectCount(
                new LambdaQueryWrapper<Order>().eq(Order::getStatus, "PENDING")));
        stats.put("codePackageCount", codePackageMapper.selectCount(null));
        stats.put("boundCodeCount", codePackageItemMapper.selectCount(
                new LambdaQueryWrapper<CodePackageItem>().eq(CodePackageItem::getBindStatus, "BOUND")));
        stats.put("unboundCodeCount", codePackageItemMapper.selectCount(
                new LambdaQueryWrapper<CodePackageItem>().eq(CodePackageItem::getBindStatus, "UNBOUND")));
        stats.put("certCount", certMapper.selectCount(null));
        return stats;
    }

    /**
     * 企业端统计 — 单企业便捷调用
     */
    public Map<String, Object> enterpriseStats(Long enterpriseId) {
        return enterpriseStats(List.of(enterpriseId));
    }

    /**
     * 企业端统计 — 支持集团模式(多企业ID)
     */
    public Map<String, Object> enterpriseStats(List<Long> enterpriseIds) {
        Map<String, Object> stats = new HashMap<>();
        if (enterpriseIds == null || enterpriseIds.isEmpty()) return stats;

        boolean single = enterpriseIds.size() == 1;
        Long eid = single ? enterpriseIds.get(0) : null;

        LambdaQueryWrapper<Goods> goodsW = new LambdaQueryWrapper<>();
        if (single) goodsW.eq(Goods::getEnterpriseId, eid);
        else goodsW.in(Goods::getEnterpriseId, enterpriseIds);
        stats.put("goodsCount", goodsMapper.selectCount(goodsW));

        LambdaQueryWrapper<Order> orderW = new LambdaQueryWrapper<>();
        if (single) orderW.eq(Order::getEnterpriseId, eid);
        else orderW.in(Order::getEnterpriseId, enterpriseIds);
        stats.put("orderCount", orderMapper.selectCount(orderW));

        LambdaQueryWrapper<Order> pendingW = new LambdaQueryWrapper<>();
        if (single) pendingW.eq(Order::getEnterpriseId, eid);
        else pendingW.in(Order::getEnterpriseId, enterpriseIds);
        pendingW.eq(Order::getStatus, "PENDING");
        stats.put("pendingOrderCount", orderMapper.selectCount(pendingW));

        LambdaQueryWrapper<Order> approvedW = new LambdaQueryWrapper<>();
        if (single) approvedW.eq(Order::getEnterpriseId, eid);
        else approvedW.in(Order::getEnterpriseId, enterpriseIds);
        approvedW.eq(Order::getStatus, "APPROVED");
        stats.put("approvedOrderCount", orderMapper.selectCount(approvedW));

        LambdaQueryWrapper<EnterpriseCert> certW = new LambdaQueryWrapper<>();
        if (single) certW.eq(EnterpriseCert::getEnterpriseId, eid);
        else certW.in(EnterpriseCert::getEnterpriseId, enterpriseIds);
        stats.put("certCount", certMapper.selectCount(certW));

        LambdaQueryWrapper<Batch> batchW = new LambdaQueryWrapper<>();
        if (single) batchW.eq(Batch::getEnterpriseId, eid);
        else batchW.in(Batch::getEnterpriseId, enterpriseIds);
        stats.put("batchCount", batchMapper.selectCount(batchW));

        return stats;
    }
}
