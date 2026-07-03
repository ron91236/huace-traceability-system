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
        r.getRecords().forEach(oc -> {
            Order order = orderMapper.selectById(oc.getOrderId());
            if (order != null) oc.setOrderNo(order.getOrderNo());
            if (oc.getLabelSpecId() != null) {
                LabelSpec ls = labelSpecMapper.selectById(oc.getLabelSpecId());
                if (ls != null) oc.setLabelSpecName(ls.getSpecName());
            }
            if (oc.getBatchId() != null) {
                Batch b = batchMapper.selectById(oc.getBatchId());
                if (b != null) oc.setBatchName(b.getName());
            }
        });
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
        String qrcode = QrCodeUtil.generateBase64(traceUrl, 300, 300);

        Map<String, Object> result = new HashMap<>();
        result.put("orderCode", oc);
        result.put("qrcode", qrcode);
        result.put("traceUrl", traceUrl);
        result.put("serialNo", oc.getSerialStart());
        return result;
    }
}
