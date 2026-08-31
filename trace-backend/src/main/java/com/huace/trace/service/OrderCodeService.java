package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import com.huace.trace.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderCodeService {

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    private final OrderCodeMapper orderCodeMapper;
    private final OrderMapper orderMapper;
    private final LabelSpecMapper labelSpecMapper;
    private final BatchMapper batchMapper;

    public PageResult<OrderCode> list(int page, int size, Long orderId, Long enterpriseId) {
        LambdaQueryWrapper<OrderCode> w = new LambdaQueryWrapper<>();
        if (orderId != null) {
            w.eq(OrderCode::getOrderId, orderId);
        } else if (enterpriseId != null) {
            List<Long> orderIds = orderMapper.selectList(
                    new LambdaQueryWrapper<Order>().eq(Order::getEnterpriseId, enterpriseId)
                            .select(Order::getId)).stream().map(Order::getId).toList();
            if (orderIds.isEmpty()) return new PageResult<>(List.of(), 0);
            w.in(OrderCode::getOrderId, orderIds);
        }
        w.orderByDesc(OrderCode::getId);
        Page<OrderCode> r = orderCodeMapper.selectPage(new Page<>(page, size), w);
        List<OrderCode> records = r.getRecords();
        if (!records.isEmpty()) {
            // 批量预取 order/labelSpec/batch，消除逐行查询
            java.util.Set<Long> orderIds = records.stream().map(OrderCode::getOrderId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            java.util.Set<Long> labelSpecIds = records.stream().map(OrderCode::getLabelSpecId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            java.util.Set<Long> batchIds = records.stream().map(OrderCode::getBatchId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            Map<Long, Order> orderMap = orderIds.isEmpty() ? java.util.Collections.emptyMap()
                    : orderMapper.selectBatchIds(orderIds).stream()
                            .collect(java.util.stream.Collectors.toMap(Order::getId, o -> o));
            Map<Long, LabelSpec> labelSpecMap = labelSpecIds.isEmpty() ? java.util.Collections.emptyMap()
                    : labelSpecMapper.selectBatchIds(labelSpecIds).stream()
                            .collect(java.util.stream.Collectors.toMap(LabelSpec::getId, ls -> ls));
            Map<Long, Batch> batchMap = batchIds.isEmpty() ? java.util.Collections.emptyMap()
                    : batchMapper.selectBatchIds(batchIds).stream()
                            .collect(java.util.stream.Collectors.toMap(Batch::getId, b -> b));
            records.forEach(oc -> {
                Order order = orderMap.get(oc.getOrderId());
                if (order != null) oc.setOrderNo(order.getOrderNo());
                LabelSpec ls = labelSpecMap.get(oc.getLabelSpecId());
                if (ls != null) oc.setLabelSpecName(ls.getSpecName());
                Batch b = batchMap.get(oc.getBatchId());
                if (b != null) oc.setBatchName(b.getName());
            });
        }
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void update(Long id, OrderCode orderCode, Long enterpriseId) {
        OrderCode existing = orderCodeMapper.selectById(id);
        if (existing == null) throw new BusinessException("订单条码不存在");
        if (existing.getOrderId() != null) {
            Order order = orderMapper.selectById(existing.getOrderId());
            if (order == null || !order.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        }
        orderCode.setId(id);
        orderCodeMapper.updateById(orderCode);
    }

    public void delete(Long id, Long enterpriseId) {
        OrderCode existing = orderCodeMapper.selectById(id);
        if (existing == null) throw new BusinessException("订单条码不存在");
        if (existing.getOrderId() != null) {
            Order order = orderMapper.selectById(existing.getOrderId());
            if (order == null || !order.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        }
        orderCodeMapper.deleteById(id);
    }

    public Map<String, Object> preview(Long id, Long enterpriseId) {
        OrderCode oc = orderCodeMapper.selectById(id);
        if (oc == null) throw new BusinessException("订单条码不存在");
        if (enterpriseId != null && oc.getOrderId() != null) {
            Order order = orderMapper.selectById(oc.getOrderId());
            if (order == null || !order.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        }

        // 生成URL型QR码：扫码跳转到溯源页
        String traceUrl = baseUrl + "/trace/" + oc.getSerialStart();
        String qrcode = QrCodeUtil.generateBase64(traceUrl, 400, 400);

        Map<String, Object> result = new HashMap<>();
        result.put("orderCode", oc);
        result.put("qrcode", qrcode);
        result.put("traceUrl", traceUrl);
        result.put("serialNo", oc.getSerialStart());
        return result;
    }
}
