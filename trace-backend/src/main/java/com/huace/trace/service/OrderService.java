package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import com.huace.trace.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderMapper orderMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final EnterpriseCertMapper certMapper;
    private final OrderCodeMapper orderCodeMapper;
    private final OrderItemMapper orderItemMapper;
    private final AuditLogMapper auditLogMapper;
    private final LabelSpecMapper labelSpecMapper;
    private final BatchMapper batchMapper;
    private final GoodsMapper goodsMapper;
    private final CertProductService certProductService;

    public PageResult<Order> list(int page, int size, String keyword, Long enterpriseId, String status) {
        LambdaQueryWrapper<Order> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) w.eq(Order::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(status)) w.eq(Order::getStatus, status);
        if (StringUtils.hasText(keyword)) {
            w.like(Order::getOrderNo, keyword);
        }
        w.orderByDesc(Order::getId);
        Page<Order> r = orderMapper.selectPage(new Page<>(page, size), w);
        r.getRecords().forEach(o -> {
            Enterprise e = enterpriseMapper.selectById(o.getEnterpriseId());
            if (e != null) o.setEnterpriseName(e.getName());
            if (o.getCertId() != null) {
                EnterpriseCert c = certMapper.selectById(o.getCertId());
                if (c != null) o.setCertName(c.getCertName());
            }
        });
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public Order getById(Long id, Long enterpriseId) {
        Order o = orderMapper.selectById(id);
        if (o != null && enterpriseId != null && o.getEnterpriseId() != null && !o.getEnterpriseId().equals(enterpriseId)) return null;
        if (o != null) {
            Enterprise e = enterpriseMapper.selectById(o.getEnterpriseId());
            if (e != null) o.setEnterpriseName(e.getName());
            if (o.getCertId() != null) {
                EnterpriseCert c = certMapper.selectById(o.getCertId());
                if (c != null) o.setCertName(c.getCertName());
            }
        }
        return o;
    }

    public void create(Order order) {
        order.setOrderNo(generateOrderNo());
        order.setStatus("DRAFT");
        orderMapper.insert(order);
    }

    public void delete(Long id, Long enterpriseId) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限");
        if (!"DRAFT".equals(order.getStatus())) throw new BusinessException("只能删除草稿状态的订单");
        // 删除关联的订单明细
        orderItemMapper.delete(new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        orderMapper.deleteById(id);
    }

    @Transactional
    public void submit(Long id, Long enterpriseId, UserPrincipal principal) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!order.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限");
        if (!"DRAFT".equals(order.getStatus())) throw new BusinessException("只能提交草稿状态的订单");

        order.setSubmitTime(LocalDateTime.now());

        // 尝试自动审批：计算订单重量 → 检查产能
        boolean autoApproved = false;
        if (order.getCertId() != null) {
            BigDecimal weightTons = calculateOrderWeightTons(id);
            autoApproved = certProductService.checkAndDeductProduction(order.getCertId(), weightTons);
        }

        if (autoApproved) {
            order.setStatus("APPROVED");
            order.setReviewTime(LocalDateTime.now());
            order.setReviewNote("产能充足，系统自动审批");
            orderMapper.updateById(order);
            saveAuditLog(id, "SUBMIT", principal, "企业提交审核 - 产能充足，系统自动审批通过");
        } else {
            order.setStatus("PENDING");
            orderMapper.updateById(order);
            saveAuditLog(id, "SUBMIT", principal, "企业提交审核");
        }
    }

    /**
     * 计算订单总重量(吨) = sum(单件重量 × 数量)
     */
    private BigDecimal calculateOrderWeightTons(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, orderId));
        BigDecimal totalGrams = BigDecimal.ZERO;
        for (OrderItem item : items) {
            BigDecimal unitGrams = parseWeightToGrams(item.getGoodsWeight());
            if (unitGrams != null && item.getQuantity() != null) {
                totalGrams = totalGrams.add(unitGrams.multiply(BigDecimal.valueOf(item.getQuantity())));
            }
        }
        // 克转吨: 除以1,000,000
        return totalGrams.divide(BigDecimal.valueOf(1_000_000), 6, java.math.RoundingMode.HALF_UP);
    }

    /**
     * 解析重量字符串为克数
     * "500g"→500, "1kg"→1000, "0.5t"→500000, "500"→500(默认克)
     */
    private BigDecimal parseWeightToGrams(String weight) {
        if (weight == null || weight.isBlank()) return null;
        weight = weight.trim().toLowerCase();
        try {
            if (weight.endsWith("kg")) {
                return new BigDecimal(weight.substring(0, weight.length() - 2).trim()).multiply(BigDecimal.valueOf(1000));
            } else if (weight.endsWith("t")) {
                return new BigDecimal(weight.substring(0, weight.length() - 1).trim()).multiply(BigDecimal.valueOf(1_000_000));
            } else if (weight.endsWith("g")) {
                return new BigDecimal(weight.substring(0, weight.length() - 1).trim());
            } else {
                return new BigDecimal(weight);
            }
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Transactional
    public void approve(Long id, UserPrincipal principal, String note) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"PENDING".equals(order.getStatus())) throw new BusinessException("只能审核待审核状态的订单");

        order.setStatus("APPROVED");
        order.setReviewTime(LocalDateTime.now());
        order.setReviewerId(principal.getUserId());
        order.setReviewNote(note);
        orderMapper.updateById(order);

        saveAuditLog(id, "APPROVE", principal, note);
    }

    @Transactional
    public void reject(Long id, UserPrincipal principal, String note) {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        if (!"PENDING".equals(order.getStatus())) throw new BusinessException("只能审核待审核状态的订单");

        order.setStatus("REJECTED");
        order.setReviewTime(LocalDateTime.now());
        order.setReviewerId(principal.getUserId());
        order.setReviewNote(note);
        orderMapper.updateById(order);

        saveAuditLog(id, "REJECT", principal, note);
    }

    public List<AuditLog> getAuditHistory(Long orderId, Long enterpriseId) {
        if (enterpriseId != null) {
            Order order = orderMapper.selectById(orderId);
            if (order == null || !order.getEnterpriseId().equals(enterpriseId)) return List.of();
        }
        return auditLogMapper.selectList(
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getOrderId, orderId)
                        .orderByAsc(AuditLog::getCreatedAt));
    }

    public List<OrderCode> getOrderCodes(Long orderId) {
        return orderCodeMapper.selectList(
                new LambdaQueryWrapper<OrderCode>()
                        .eq(OrderCode::getOrderId, orderId));
    }

    public List<OrderItem> getOrderItems(Long orderId) {
        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, orderId)
                        .orderByAsc(OrderItem::getId));
        items.forEach(item -> {
            if (item.getBatchId() != null) {
                Batch b = batchMapper.selectById(item.getBatchId());
                if (b != null) item.setBatchName(b.getName());
            }
        });
        return items;
    }

    public void addOrderItem(OrderItem item, Long enterpriseId) {
        if (item.getOrderId() != null) {
            Order order = orderMapper.selectById(item.getOrderId());
            if (order == null || !order.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        }
        fillFromLabelSpec(item);
        calcTotalPrice(item);
        orderItemMapper.insert(item);
    }

    public void updateOrderItem(Long id, OrderItem item, Long enterpriseId) {
        OrderItem existing = orderItemMapper.selectById(id);
        if (existing == null) throw new BusinessException("订单明细不存在");
        if (existing.getOrderId() != null) {
            Order order = orderMapper.selectById(existing.getOrderId());
            if (order == null || !order.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        }
        item.setId(id);
        fillFromLabelSpec(item);
        calcTotalPrice(item);
        orderItemMapper.updateById(item);
    }

    private void fillFromLabelSpec(OrderItem item) {
        if (item.getLabelSpecId() != null) {
            LabelSpec ls = labelSpecMapper.selectById(item.getLabelSpecId());
            if (ls != null) {
                item.setLabelSpecName(ls.getSpecName());
                item.setLabelSpecMaterial(ls.getMaterial());
                item.setLabelSpecType(ls.getUsageMethod());
                item.setPrice(ls.getPrice());
            }
        }
    }

    private void calcTotalPrice(OrderItem item) {
        if (item.getQuantity() != null && item.getPrice() != null) {
            item.setTotalPrice(item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())));
        } else {
            item.setTotalPrice(null);
        }
    }

    public void deleteOrderItem(Long id, Long enterpriseId) {
        OrderItem existing = orderItemMapper.selectById(id);
        if (existing == null) throw new BusinessException("订单明细不存在");
        if (existing.getOrderId() != null) {
            Order order = orderMapper.selectById(existing.getOrderId());
            if (order == null || !order.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        }
        orderItemMapper.deleteById(id);
    }

    private void saveAuditLog(Long orderId, String action, UserPrincipal principal, String note) {
        AuditLog log = new AuditLog();
        log.setOrderId(orderId);
        log.setAction(action);
        log.setNote(note);
        if (principal != null) {
            log.setOperatorId(principal.getUserId());
            log.setOperatorName(principal.getUsername());
        }
        auditLogMapper.insert(log);
    }

    private String generateOrderNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "ORD" + date + rand;
    }
}
