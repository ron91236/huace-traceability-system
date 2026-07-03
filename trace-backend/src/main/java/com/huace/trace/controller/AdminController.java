package com.huace.trace.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.common.PageResult;
import com.huace.trace.common.Result;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.*;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.huace.trace.common.BusinessException;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CertTypeService certTypeService;
    private final EnterpriseService enterpriseService;
    private final EnterpriseCertService enterpriseCertService;
    private final ProductService productService;
    private final LabelSpecService labelSpecService;
    private final EnterpriseBaseService baseService;
    private final GoodsService goodsService;
    private final OrderService orderService;
    private final CodePackageService codePackageService;
    private final NoticeService noticeService;
    private final DashboardService dashboardService;
    private final CodeGenerationService codeGenerationService;
    private final CodeGenerationAsyncService codeGenerationAsyncService;
    private final VoidedCodeRangeService voidedCodeRangeService;
    private final CertProductService certProductService;
    private final TraceTemplateService traceTemplateService;
    private final OrderCodeService orderCodeService;
    private final OrderCodeMapper orderCodeMapper;
    private final CodePackageMapper codePackageMapper;
    private final CodePackageItemMapper codePackageItemMapper;
    private final EnterpriseGroupService enterpriseGroupService;
    private final DataScreenService dataScreenService;
    private final TraceTemplateMapper traceTemplateMapper;
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    // ==================== 证书类型 ====================
    @GetMapping("/cert-types")
    public Result<PageResult<CertType>> listCertTypes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(certTypeService.list(page, size, keyword));
    }

    @PostMapping("/cert-types")
    public Result<Void> createCertType(@RequestBody CertType certType) {
        certTypeService.create(certType);
        return Result.ok();
    }

    @PutMapping("/cert-types/{id}")
    public Result<Void> updateCertType(@PathVariable Long id, @RequestBody CertType certType) {
        certTypeService.update(id, certType);
        return Result.ok();
    }

    @DeleteMapping("/cert-types/{id}")
    public Result<Void> deleteCertType(@PathVariable Long id) {
        certTypeService.delete(id);
        return Result.ok();
    }

    // ==================== 企业管理 ====================
    @GetMapping("/enterprises")
    public Result<PageResult<Enterprise>> listEnterprises(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(enterpriseService.list(page, size, keyword));
    }

    @GetMapping("/enterprises/all")
    public Result<List<Enterprise>> allEnterprises() {
        return Result.ok(enterpriseService.all());
    }

    @PostMapping("/enterprises")
    public Result<Void> createEnterprise(@RequestBody Enterprise enterprise) {
        enterpriseService.create(enterprise);
        return Result.ok();
    }

    @PutMapping("/enterprises/{id}")
    public Result<Void> updateEnterprise(@PathVariable Long id, @RequestBody Enterprise enterprise) {
        enterpriseService.update(id, enterprise);
        return Result.ok();
    }

    @DeleteMapping("/enterprises/{id}")
    public Result<Void> deleteEnterprise(@PathVariable Long id) {
        enterpriseService.delete(id);
        return Result.ok();
    }

    // ==================== 企业集团管理 ====================
    @PostMapping("/enterprises/master")
    public Result<Void> createMasterEnterprise(@RequestBody Enterprise enterprise) {
        enterpriseGroupService.createMasterEnterprise(enterprise);
        return Result.ok();
    }

    @PostMapping("/enterprises/{parentId}/children")
    public Result<Void> createChildEnterprise(@PathVariable Long parentId, @RequestBody Enterprise child) {
        enterpriseGroupService.createChildEnterprise(parentId, child);
        return Result.ok();
    }

    @GetMapping("/enterprises/{parentId}/children")
    public Result<List<Enterprise>> listChildEnterprises(@PathVariable Long parentId) {
        return Result.ok(enterpriseGroupService.listChildEnterprises(parentId));
    }

    @DeleteMapping("/enterprises/{parentId}/children/{id}")
    public Result<Void> removeChildEnterprise(@PathVariable Long parentId, @PathVariable Long id) {
        enterpriseGroupService.removeChildEnterprise(parentId, id);
        return Result.ok();
    }

    // ==================== 企业认证 ====================
    @GetMapping("/enterprise-certs")
    public Result<PageResult<EnterpriseCert>> listEnterpriseCerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long enterpriseId) {
        return Result.ok(enterpriseCertService.list(page, size, keyword, enterpriseId));
    }

    @PostMapping("/enterprise-certs")
    public Result<Void> createEnterpriseCert(@RequestBody EnterpriseCert cert) {
        enterpriseCertService.create(cert);
        return Result.ok();
    }

    @PutMapping("/enterprise-certs/{id}")
    public Result<Void> updateEnterpriseCert(@PathVariable Long id, @RequestBody EnterpriseCert cert) {
        enterpriseCertService.update(id, cert);
        return Result.ok();
    }

    @DeleteMapping("/enterprise-certs/{id}")
    public Result<Void> deleteEnterpriseCert(@PathVariable Long id) {
        enterpriseCertService.delete(id);
        return Result.ok();
    }

    @GetMapping("/enterprise-certs/{id}/qrcode")
    public Result<Map<String, String>> getCertQrcode(@PathVariable Long id) {
        String qrUrl = enterpriseCertService.generateQrcode(id);
        String certUrl = enterpriseCertService.getCertQrcodeUrl(id);
        return Result.ok(Map.of("qrUrl", qrUrl, "certUrl", certUrl));
    }

    // ==================== 产品管理 ====================
    @GetMapping("/products")
    public Result<PageResult<Product>> listProducts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(productService.list(page, size, keyword));
    }

    @PostMapping("/products")
    public Result<Void> createProduct(@RequestBody Product product) {
        productService.create(product);
        return Result.ok();
    }

    @PutMapping("/products/{id}")
    public Result<Void> updateProduct(@PathVariable Long id, @RequestBody Product product) {
        productService.update(id, product);
        return Result.ok();
    }

    @DeleteMapping("/products/{id}")
    public Result<Void> deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return Result.ok();
    }

    // ==================== 标签规格 ====================
    @GetMapping("/label-specs")
    public Result<PageResult<LabelSpec>> listLabelSpecs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(labelSpecService.list(page, size, keyword));
    }

    @PostMapping("/label-specs")
    public Result<Void> createLabelSpec(@RequestBody LabelSpec labelSpec) {
        labelSpecService.create(labelSpec);
        return Result.ok();
    }

    @PutMapping("/label-specs/{id}")
    public Result<Void> updateLabelSpec(@PathVariable Long id, @RequestBody LabelSpec labelSpec) {
        labelSpecService.update(id, labelSpec);
        return Result.ok();
    }

    @DeleteMapping("/label-specs/{id}")
    public Result<Void> deleteLabelSpec(@PathVariable Long id) {
        labelSpecService.delete(id);
        return Result.ok();
    }

    // ==================== 企业基地 ====================
    @GetMapping("/bases")
    public Result<PageResult<EnterpriseBase>> listBases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long enterpriseId) {
        return Result.ok(baseService.list(page, size, keyword, enterpriseId));
    }

    @PostMapping("/bases")
    public Result<Void> createBase(@RequestBody EnterpriseBase base) {
        baseService.create(base);
        return Result.ok();
    }

    @PutMapping("/bases/{id}")
    public Result<Void> updateBase(@PathVariable Long id, @RequestBody EnterpriseBase base) {
        baseService.update(id, base, null);
        return Result.ok();
    }

    @DeleteMapping("/bases/{id}")
    public Result<Void> deleteBase(@PathVariable Long id) {
        baseService.delete(id);
        return Result.ok();
    }

    // ==================== 商品管理（只读） ====================
    @GetMapping("/goods")
    public Result<PageResult<Goods>> listGoods(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long enterpriseId) {
        return Result.ok(goodsService.list(page, size, keyword, enterpriseId));
    }

    // ==================== 订单管理 ====================
    @GetMapping("/orders")
    public Result<PageResult<Order>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.ok(orderService.list(page, size, keyword, null, status));
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long id) {
        Order order = orderService.getById(id, null);
        if (order != null) {
            Enterprise e = enterpriseService.getById(order.getEnterpriseId());
            if (e != null) order.setEnterpriseName(e.getName());
            if (order.getCertId() != null) {
                EnterpriseCert c = enterpriseCertService.getById(order.getCertId());
                if (c != null) order.setCertName(c.getCertName());
            }
        }
        List<OrderCode> orderCodes = orderService.getOrderCodes(id);
        List<OrderItem> orderItems = orderService.getOrderItems(id);
        List<AuditLog> auditHistory = orderService.getAuditHistory(id, null);
        return Result.ok(Map.of(
                "order", order != null ? order : new Order(),
                "orderCodes", orderCodes,
                "orderItems", orderItems,
                "auditHistory", auditHistory
        ));
    }

    @PostMapping("/orders/{id}/approve")
    public Result<Void> approveOrder(@PathVariable Long id,
                                      @AuthenticationPrincipal UserPrincipal principal,
                                      @RequestBody(required = false) Map<String, String> body) {
        String note = body != null ? body.get("note") : null;
        orderService.approve(id, principal, note);
        return Result.ok();
    }

    @PostMapping("/orders/{id}/reject")
    public Result<Void> rejectOrder(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal principal,
                                     @RequestBody Map<String, String> body) {
        orderService.reject(id, principal, body.get("note"));
        return Result.ok();
    }

    // ==================== 码包管理 ====================
    @GetMapping("/code-packages")
    public Result<PageResult<CodePackage>> listCodePackages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(codePackageService.list(page, size, keyword));
    }

    @PostMapping("/code-packages/import")
    public Result<Void> importCodePackage(@RequestParam("file") MultipartFile file) throws Exception {
        codePackageService.importFile(file);
        return Result.ok();
    }

    @GetMapping("/code-packages/{id}")
    public Result<Map<String, Object>> getCodePackageDetail(@PathVariable Long id) {
        return Result.ok(codePackageService.getDetail(id));
    }

    @DeleteMapping("/code-packages/{id}")
    public Result<Void> deleteCodePackage(@PathVariable Long id) {
        codePackageService.delete(id);
        return Result.ok();
    }

    // ==================== 码中台 - 码包生成 ====================
    @PostMapping("/code-packages/generate")
    public Result<Map<String, Object>> generateCodePackage(@RequestBody Map<String, Object> params) {
        long serialStart = longVal(params.get("serialStart"));
        long serialEnd = longVal(params.get("serialEnd"));
        int totalCount = (int) (serialEnd - serialStart + 1);
        // 超过 10 万使用异步生成，避免阻塞请求
        if (totalCount > 100_000) {
            long taskId = System.currentTimeMillis();
            codeGenerationAsyncService.generateAsync(taskId, params);
            return Result.ok(Map.of("async", true, "taskId", taskId, "message", "码包正在后台生成，请通过任务ID查询进度"));
        }
        return Result.ok(Map.of("async", false, "data", codeGenerationService.generate(params)));
    }

    @GetMapping("/code-packages/{id}/export")
    public void exportCodePackage(@PathVariable Long id, HttpServletResponse response) throws Exception {
        codeGenerationService.exportCsv(id, response);
    }

    /**
     * 获取指定规则(通过serialDigits区分)的最后一条流水号，用于自动填充起始流水号
     * @param serialDigits 8=规则A, 10=规则B
     */
    @GetMapping("/code-packages/last-serial")
    public Result<Map<String, Object>> getLastSerialByRule(@RequestParam(defaultValue = "8") int serialDigits) {
        CodePackage last = codePackageMapper.selectOne(
            new LambdaQueryWrapper<CodePackage>()
                .eq(CodePackage::getSerialDigits, serialDigits)
                .eq(CodePackage::getSourceType, "GENERATE")
                .isNotNull(CodePackage::getSerialEnd)
                .orderByDesc(CodePackage::getId)
                .last("LIMIT 1"));
        Map<String, Object> result = new HashMap<>();
        if (last != null && last.getSerialEnd() != null) {
            result.put("lastSerialEnd", last.getSerialEnd());
            result.put("nextSerialStart", last.getSerialEnd() + 1);
        } else {
            result.put("lastSerialEnd", 0);
            result.put("nextSerialStart", 1);
        }
        return Result.ok(result);
    }

    // ==================== 公告管理 ====================
    @GetMapping("/notices")
    public Result<PageResult<Notice>> listNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(noticeService.list(page, size, keyword, null));
    }

    @PostMapping("/notices")
    public Result<Void> createNotice(@RequestBody Notice notice) {
        noticeService.create(notice);
        return Result.ok();
    }

    @DeleteMapping("/notices/{id}")
    public Result<Void> deleteNotice(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.ok();
    }

    // ==================== 控制台统计 ====================
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> adminDashboard() {
        return Result.ok(dashboardService.adminStats());
    }

    // ==================== 订单条码绑定 ====================

    @GetMapping("/orders/{id}/codes")
    public Result<List<OrderCode>> getOrderCodes(@PathVariable Long id) {
        return Result.ok(orderService.getOrderCodes(id));
    }

    @PostMapping("/orders/{id}/codes")
    public Result<Void> bindOrderCode(@PathVariable Long id, @RequestBody OrderCode orderCode) {
        orderCode.setOrderId(id);
        if (orderCode.getWasteCount() == null) orderCode.setWasteCount(0);

        long start, end;
        if (orderCode.getSerialStart() != null && orderCode.getSerialEnd() != null) {
            try {
                start = Long.parseLong(orderCode.getSerialStart());
                end = Long.parseLong(orderCode.getSerialEnd());
            } catch (NumberFormatException e) {
                throw new BusinessException("码段格式不正确");
            }
            if (end < start) throw new BusinessException("结束码不能小于开始码");

            // 校验码段位数
            if (orderCode.getCodePackageId() != null) {
                CodePackage pkg = codePackageMapper.selectById(orderCode.getCodePackageId());
                if (pkg != null && pkg.getSerialDigits() != null && pkg.getSerialDigits() > 0) {
                    int expected = pkg.getSerialDigits();
                    if (orderCode.getSerialStart().length() != expected) {
                        throw new BusinessException("开始身份码必须为 " + expected + " 位，当前 " + orderCode.getSerialStart().length() + " 位，不足请前补0");
                    }
                    if (orderCode.getSerialEnd().length() != expected) {
                        throw new BusinessException("结束身份码必须为 " + expected + " 位，当前 " + orderCode.getSerialEnd().length() + " 位，不足请前补0");
                    }
                }
            }

            orderCode.setQuantity((int)(end - start + 1));

            // 检查码段是否重复绑定
            long overlap = orderCodeMapper.selectCount(
                new LambdaQueryWrapper<OrderCode>()
                    .ne(orderCode.getId() != null, OrderCode::getId, orderCode.getId())
                    .and(w -> w
                        .apply("CAST(serial_start AS UNSIGNED) <= {0}", end)
                        .apply("CAST(serial_end AS UNSIGNED) >= {0}", start)));
            if (overlap > 0) {
                throw new BusinessException("码段 [" + orderCode.getSerialStart() + " - " + orderCode.getSerialEnd() + "] 与已有绑定重复");
            }
        } else {
            start = end = 0;
        }
        if (orderCode.getBindCount() == null) {
            orderCode.setBindCount(orderCode.getQuantity());
        }
        orderCodeMapper.insert(orderCode);

        // 更新码包明细为已绑定状态
        if (start > 0 && end > 0 && orderCode.getCodePackageId() != null) {
            Order order = orderMapper.selectById(id);
            Long enterpriseId = order != null ? order.getEnterpriseId() : null;
            Long certId = order != null ? order.getCertId() : null;
            Long goodsId = null;
            Long batchId = null;
            // 从订单明细获取商品和批次信息
            if (orderCode.getLabelSpecId() != null) {
                OrderItem oi = orderItemMapper.selectOne(
                    new LambdaQueryWrapper<OrderItem>()
                        .eq(OrderItem::getOrderId, id)
                        .eq(OrderItem::getLabelSpecId, orderCode.getLabelSpecId())
                        .last("LIMIT 1"));
                if (oi != null) {
                    goodsId = oi.getGoodsId();
                    batchId = oi.getBatchId();
                }
            }
            // 批量更新码包明细
            String startStr = String.format("%08d", start);
            String endStr = String.format("%08d", end);
            CodePackageItem updateItem = new CodePackageItem();
            updateItem.setBindStatus("BOUND");
            updateItem.setBindTime(LocalDateTime.now());
            updateItem.setOrderCodeId(orderCode.getId());
            updateItem.setEnterpriseId(enterpriseId);
            updateItem.setGoodsId(goodsId);
            updateItem.setCertId(certId);
            updateItem.setBatchId(batchId);
            updateItem.setTraceTemplate(orderCode.getTraceTemplate());
            codePackageItemMapper.update(updateItem,
                new LambdaQueryWrapper<CodePackageItem>()
                    .eq(CodePackageItem::getPackageId, orderCode.getCodePackageId())
                    .apply("serial_no >= {0}", startStr)
                    .apply("serial_no <= {0}", endStr));
        }
        return Result.ok();
    }

    /**
     * 获取上次绑定的最后码段，用于自动填充
     */
    @GetMapping("/orders/{id}/last-serial")
    public Result<Map<String, Object>> getLastSerial(@PathVariable Long id) {
        OrderCode last = orderCodeMapper.selectOne(
            new LambdaQueryWrapper<OrderCode>()
                .eq(OrderCode::getOrderId, id)
                .isNotNull(OrderCode::getSerialEnd)
                .orderByDesc(OrderCode::getId)
                .last("LIMIT 1"));
        Map<String, Object> result = new HashMap<>();
        if (last != null && last.getSerialEnd() != null) {
            try {
                long lastEnd = Long.parseLong(last.getSerialEnd());
                result.put("nextStart", String.valueOf(lastEnd + 1));
                result.put("lastEnd", last.getSerialEnd());
            } catch (NumberFormatException e) {
                result.put("nextStart", "");
            }
        } else {
            result.put("nextStart", "");
        }
        return Result.ok(result);
    }

    @DeleteMapping("/order-codes/{id}")
    public Result<Void> deleteOrderCode(@PathVariable Long id) {
        orderCodeMapper.deleteById(id);
        return Result.ok();
    }

    // ==================== 预览码（管理端） ====================
    @GetMapping("/order-codes/{id}/preview-qrcode")
    public Result<Map<String, Object>> previewOrderCodeQrcode(@PathVariable Long id) {
        return Result.ok(orderCodeService.preview(id, null));
    }

    @GetMapping("/code-packages/all")
    public Result<List<Map<String, Object>>> allCodePackages() {
        List<CodePackage> packages = codePackageMapper.selectList(
                new LambdaQueryWrapper<CodePackage>().orderByDesc(CodePackage::getId));
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (CodePackage cp : packages) {
            long boundCount = codePackageItemMapper.selectCount(
                    new LambdaQueryWrapper<CodePackageItem>()
                            .eq(CodePackageItem::getPackageId, cp.getId())
                            .eq(CodePackageItem::getBindStatus, "BOUND"));
            long wasteCount = codePackageItemMapper.selectCount(
                    new LambdaQueryWrapper<CodePackageItem>()
                            .eq(CodePackageItem::getPackageId, cp.getId())
                            .eq(CodePackageItem::getBindStatus, "WASTE"));
            long available = (cp.getTotalCount() != null ? cp.getTotalCount() : 0) - boundCount - wasteCount;

            Map<String, Object> item = new HashMap<>();
            item.put("id", cp.getId());
            item.put("packageNo", cp.getPackageNo());
            item.put("totalCount", cp.getTotalCount());
            item.put("boundCount", boundCount);
            item.put("wasteCount", wasteCount);
            item.put("available", available);
            item.put("serialStart", cp.getSerialStart());
            item.put("serialEnd", cp.getSerialEnd());
            item.put("labelSpecId", cp.getLabelSpecId());
            if (cp.getLabelSpecId() != null) {
                LabelSpec ls = labelSpecService.getById(cp.getLabelSpecId());
                item.put("labelSpecName", ls != null ? ls.getSpecName() : "");
            }
            result.add(item);
        }
        return Result.ok(result);
    }

    // ==================== 溯源码作废管理 ====================

    @GetMapping("/voided-count")
    public Result<Map<String, Object>> getVoidedCount(
            @RequestParam String serialStart,
            @RequestParam String serialEnd) {
        try {
            long start = Long.parseLong(serialStart);
            long end = Long.parseLong(serialEnd);
            int totalCount = (int)(end - start + 1);
            int voidedCount = voidedCodeRangeService.countOverlapping(start, end);
            int bindCount = totalCount - voidedCount;
            return Result.ok(Map.of(
                    "totalCount", totalCount,
                    "voidedCount", voidedCount,
                    "bindCount", bindCount
            ));
        } catch (NumberFormatException e) {
            return Result.ok(Map.of("totalCount", 0, "voidedCount", 0, "bindCount", 0));
        }
    }

    @GetMapping("/voided-code-ranges")
    public Result<PageResult<VoidedCodeRange>> listVoidedCodeRanges(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(voidedCodeRangeService.list(page, size));
    }

    @PostMapping("/voided-code-ranges/batch")
    public Result<Void> batchImportVoidedCodeRanges(@RequestBody List<VoidedCodeRange> ranges) {
        voidedCodeRangeService.batchImport(ranges);
        return Result.ok();
    }

    @DeleteMapping("/voided-code-ranges/{id}")
    public Result<Void> deleteVoidedCodeRange(@PathVariable Long id) {
        voidedCodeRangeService.delete(id);
        return Result.ok();
    }

    // ==================== 证书产品管理 ====================

    @GetMapping("/enterprise-certs/{certId}/products")
    public Result<List<CertProduct>> listCertProducts(@PathVariable Long certId) {
        return Result.ok(certProductService.listByCertId(certId));
    }

    @PostMapping("/enterprise-certs/{certId}/products")
    public Result<Void> addCertProduct(@PathVariable Long certId, @RequestBody CertProduct certProduct) {
        certProduct.setCertId(certId);
        certProductService.addProduct(certProduct);
        return Result.ok();
    }

    @DeleteMapping("/cert-products/{id}")
    public Result<Void> removeCertProduct(@PathVariable Long id) {
        certProductService.removeProduct(id);
        return Result.ok();
    }

    // ==================== 溯源模板管理 ====================

    @GetMapping("/trace-templates")
    public Result<PageResult<TraceTemplate>> listTraceTemplates(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(traceTemplateService.list(page, size, keyword));
    }

    @PostMapping("/trace-templates")
    public Result<Void> createTraceTemplate(@RequestBody TraceTemplate template) {
        traceTemplateService.create(template);
        return Result.ok();
    }

    @PutMapping("/trace-templates/{id}")
    public Result<Void> updateTraceTemplate(@PathVariable Long id, @RequestBody TraceTemplate template) {
        traceTemplateService.update(id, template);
        return Result.ok();
    }

    @DeleteMapping("/trace-templates/{id}")
    public Result<Void> deleteTraceTemplate(@PathVariable Long id) {
        traceTemplateService.delete(id);
        return Result.ok();
    }

    // ==================== 数据大屏 ====================
    @GetMapping("/data-screen/all")
    public Result<Map<String, Object>> getAdminDataScreen() {
        return Result.ok(dataScreenService.getAllData(null));
    }

    private long longVal(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return 0; }
    }
}
