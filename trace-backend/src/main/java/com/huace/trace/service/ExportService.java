package com.huace.trace.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Order;
import com.huace.trace.entity.OrderCode;
import com.huace.trace.mapper.OrderCodeMapper;
import com.huace.trace.mapper.OrderMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportService {

    private final OrderMapper orderMapper;
    private final OrderCodeMapper orderCodeMapper;

    public void exportOrder(Long orderId, Long enterpriseId, HttpServletResponse response) throws IOException {
        Order order = orderMapper.selectById(orderId);
        if (order == null) throw new BusinessException("订单不存在");
        if (enterpriseId != null && !order.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");

        List<OrderCode> orderCodes = orderCodeMapper.selectList(
                new LambdaQueryWrapper<OrderCode>().eq(OrderCode::getOrderId, orderId));

        List<OrderExportRow> rows = new ArrayList<>();
        if (orderCodes.isEmpty()) {
            OrderExportRow row = new OrderExportRow();
            row.setOrderNo(order.getOrderNo());
            row.setStatus(statusName(order.getStatus()));
            rows.add(row);
        } else {
            for (OrderCode oc : orderCodes) {
                OrderExportRow row = new OrderExportRow();
                row.setOrderNo(order.getOrderNo());
                row.setStatus(statusName(order.getStatus()));
                row.setProductName(oc.getProductName());
                row.setQuantity(oc.getQuantity());
                row.setPrice(oc.getPrice() != null ? oc.getPrice().toString() : "");
                row.setSerialStart(oc.getSerialStart());
                row.setSerialEnd(oc.getSerialEnd());
                row.setTraceTemplate(oc.getTraceTemplate());
                rows.add(row);
            }
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition",
                "attachment;filename=" + URLEncoder.encode("订单_" + order.getOrderNo() + ".xlsx", "UTF-8"));

        EasyExcel.write(response.getOutputStream(), OrderExportRow.class)
                .sheet("订单详情")
                .doWrite(rows);
    }

    private String statusName(String status) {
        return switch (status) {
            case "DRAFT" -> "草稿";
            case "PENDING" -> "待审核";
            case "APPROVED" -> "已通过";
            case "REJECTED" -> "已驳回";
            default -> status;
        };
    }

    @Data
    public static class OrderExportRow {
        @ExcelProperty("订单编号")
        private String orderNo;

        @ExcelProperty("状态")
        private String status;

        @ExcelProperty("产品名称")
        private String productName;

        @ExcelProperty("数量")
        private Integer quantity;

        @ExcelProperty("单价")
        private String price;

        @ExcelProperty("起始流水号")
        private String serialStart;

        @ExcelProperty("结束流水号")
        private String serialEnd;

        @ExcelProperty("溯源模板")
        private String traceTemplate;
    }
}
