package com.huace.trace.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.common.Result;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.*;
import jakarta.servlet.http.HttpServletResponse;
import com.alibaba.excel.EasyExcel;
import com.huace.trace.dto.OrderExportDTO;
import com.huace.trace.dto.OrderBarcodeExportDTO;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.util.CellRangeAddress;
import java.net.URLEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
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
    private final VideoSourceService videoSourceService;
    private final IotDeviceService iotDeviceService;
    private final com.huace.trace.mapper.IotAlertRecordMapper alertRecordMapper;
    private final BatchMapper batchMapper;
    private final EnterpriseCertMapper enterpriseCertMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final GoodsMapper goodsMapper;
    private final AddressMapper addressMapper;
    private final VrPanoramaService vrPanoramaService;
    private final PosterService posterService;

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

    @GetMapping("/orders/export")
    public void exportOrders(@RequestParam(required = false) String keyword,
                             @RequestParam(required = false) String status,
                             HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=orders.xlsx");
        PageResult<Order> pageResult = orderService.list(1, 10000, keyword, null, status);
        java.util.List<OrderExportDTO> exportList = new java.util.ArrayList<>();
        for (Order o : pageResult.getList()) {
            Enterprise ent = enterpriseService.getById(o.getEnterpriseId());
            String entName = ent != null ? ent.getName() : "";
            String certNoStr = "";
            if (o.getCertId() != null) {
                EnterpriseCert c = enterpriseCertMapper.selectById(o.getCertId());
                if (c != null) certNoStr = c.getCertNo() != null ? c.getCertNo() : "";
            }
            List<OrderItem> items = orderItemMapper.selectList(
                    new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, o.getId()));
            List<OrderCode> codes = orderCodeMapper.selectList(
                    new LambdaQueryWrapper<OrderCode>().eq(OrderCode::getOrderId, o.getId()));
            final String entNameFinal = entName;
            final String certNoFinal = certNoStr;
            if (codes.isEmpty()) {
                for (OrderItem oi : items) {
                    OrderExportDTO dto = new OrderExportDTO();
                    dto.setOrderNo(o.getOrderNo());
                    dto.setEnterpriseName(entNameFinal);
                    dto.setCertNo(certNoFinal);
                    dto.setGoodsName(oi.getGoodsName());
                    dto.setLabelSpecName(oi.getLabelSpecName());
                    dto.setUnitPrice(oi.getPrice() != null ? oi.getPrice().toPlainString() : "");
                    dto.setQuantity(oi.getQuantity());
                    dto.setTotalPrice(oi.getTotalPrice() != null ? oi.getTotalPrice().toPlainString() : "");
                    if (oi.getBatchId() != null) {
                        Batch b = batchMapper.selectById(oi.getBatchId());
                        if (b != null) dto.setProductBatch(b.getName());
                    }
                    if (oi.getGoodsId() != null) {
                        Goods g = goodsMapper.selectById(oi.getGoodsId());
                        if (g != null) { dto.setProductDescription(g.getIntroduction()); dto.setPackageSpec(g.getPackageSpec()); dto.setWeightSpec(g.getWeightSpec()); }
                    }
                    exportList.add(dto);
                }
            } else {
                for (OrderCode oc : codes) {
                    OrderExportDTO dto = new OrderExportDTO();
                    dto.setOrderNo(o.getOrderNo());
                    dto.setEnterpriseName(entNameFinal);
                    dto.setCertNo(certNoFinal);
                    dto.setProductName(oc.getProductName());
                    dto.setSerialStart(oc.getSerialStart());
                    dto.setSerialEnd(oc.getSerialEnd());
                    dto.setQuantity(oc.getQuantity());
                    dto.setUnitPrice(oc.getPrice() != null ? oc.getPrice().toPlainString() : "");
                    if (oc.getProductionTime() != null) {
                        dto.setProductionTime(oc.getProductionTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                    }
                    if (oc.getBatchId() != null) {
                        Batch b = batchMapper.selectById(oc.getBatchId());
                        if (b != null) dto.setProductBatch(b.getName());
                    }
                    if (oc.getLabelSpecId() != null) {
                        LabelSpec ls = labelSpecService.getById(oc.getLabelSpecId());
                        if (ls != null) dto.setLabelSpecName(ls.getSpecName());
                    }
                    if (oc.getLabelSpecId() != null) {
                        items.stream().filter(i -> oc.getLabelSpecId().equals(i.getLabelSpecId())).findFirst().ifPresent(oi -> {
                            dto.setGoodsName(oi.getGoodsName());
                            dto.setTotalPrice(oi.getTotalPrice() != null ? oi.getTotalPrice().toPlainString() : "");
                            if (oi.getGoodsId() != null) {
                                Goods g = goodsMapper.selectById(oi.getGoodsId());
                                if (g != null) { dto.setProductDescription(g.getIntroduction()); dto.setPackageSpec(g.getPackageSpec()); dto.setWeightSpec(g.getWeightSpec()); }
                            }
                        });
                    }
                    exportList.add(dto);
                }
            }
        }
        EasyExcel.write(response.getOutputStream(), OrderExportDTO.class).sheet("订单数据").doWrite(exportList);
    }

    @GetMapping("/orders/{id}/codes/export")
    public void exportOrderBarcodes(@PathVariable Long id, HttpServletResponse response) throws Exception {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=order_barcodes.xlsx");
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        Enterprise ent = enterpriseService.getById(order.getEnterpriseId());
        String entName = ent != null ? ent.getName() : "";
        List<OrderCode> codes = orderService.getOrderCodes(id);
        List<OrderItem> items = orderService.getOrderItems(id);
        java.util.List<OrderBarcodeExportDTO> exportList = new java.util.ArrayList<>();
        for (OrderCode oc : codes) {
            OrderBarcodeExportDTO dto = new OrderBarcodeExportDTO();
            dto.setOrderNo(order.getOrderNo());
            dto.setEnterpriseName(entName);
            dto.setProductName(oc.getProductName());
            dto.setProductDescription(oc.getProductDescription());
            dto.setGoodsName(oc.getGoodsName());
            dto.setLabelSpecName(oc.getLabelSpecName());
            dto.setSerialStart(oc.getSerialStart());
            dto.setSerialEnd(oc.getSerialEnd());
            dto.setQuantity(oc.getQuantity());
            dto.setWasteCount(oc.getWasteCount());
            dto.setBindCount(oc.getBindCount());
            if (oc.getProductionTime() != null) {
                dto.setProductionTime(oc.getProductionTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            if (oc.getBatchId() != null) {
                Batch b = batchMapper.selectById(oc.getBatchId());
                if (b != null) dto.setProductionBatch(b.getName());
            }
            exportList.add(dto);
        }
        EasyExcel.write(response.getOutputStream(), OrderBarcodeExportDTO.class).sheet("订单条码").doWrite(exportList);
    }

    @GetMapping("/orders/{id}/export-single")
    public void exportSingleOrder(@PathVariable Long id, HttpServletResponse response) throws Exception {
        Order order = orderMapper.selectById(id);
        if (order == null) throw new BusinessException("订单不存在");
        Enterprise ent = enterpriseService.getById(order.getEnterpriseId());
        String entName = ent != null ? ent.getName() : "";
        String certNoStr = "";
        if (order.getCertId() != null) {
            EnterpriseCert c = enterpriseCertMapper.selectById(order.getCertId());
            if (c != null) certNoStr = c.getCertNo() != null ? c.getCertNo() : "";
        }
        Address addr = null;
        if (order.getAddressId() != null) addr = addressMapper.selectById(order.getAddressId());
        String addressStr = addr != null ? addr.getAddress() : "";
        String contactStr = addr != null ? addr.getContact() : "";
        String phoneStr = addr != null ? addr.getPhone() : "";

        List<OrderItem> items = orderItemMapper.selectList(
                new LambdaQueryWrapper<OrderItem>().eq(OrderItem::getOrderId, id));
        List<OrderCode> codes = orderCodeMapper.selectList(
                new LambdaQueryWrapper<OrderCode>().eq(OrderCode::getOrderId, id));

        int totalLabels = codes.stream().mapToInt(c -> c.getQuantity() != null ? c.getQuantity() : 0).sum();
        if (totalLabels == 0) totalLabels = items.stream().mapToInt(i -> i.getQuantity() != null ? i.getQuantity() : 0).sum();
        BigDecimal totalPrice = items.stream()
                .map(i -> i.getTotalPrice() != null ? i.getTotalPrice() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("订单");

        // 边框设置辅助
        java.util.function.Consumer<XSSFCellStyle> addBorders = s -> {
            s.setBorderTop(BorderStyle.THIN); s.setBorderBottom(BorderStyle.THIN);
            s.setBorderLeft(BorderStyle.THIN); s.setBorderRight(BorderStyle.THIN);
        };

        // 标题样式
        XSSFCellStyle titleStyle = wb.createCellStyle(); addBorders.accept(titleStyle);
        XSSFFont titleFont = wb.createFont(); titleFont.setBold(true); titleFont.setFontHeightInPoints((short) 16);
        titleStyle.setFont(titleFont);
        titleStyle.setAlignment(HorizontalAlignment.CENTER);
        titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 标签样式
        XSSFCellStyle labelStyle = wb.createCellStyle(); addBorders.accept(labelStyle);
        XSSFFont labelFont = wb.createFont(); labelFont.setBold(true); labelFont.setFontHeightInPoints((short) 11);
        labelStyle.setFont(labelFont);
        labelStyle.setAlignment(HorizontalAlignment.CENTER);
        labelStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 值样式
        XSSFCellStyle valStyle = wb.createCellStyle(); addBorders.accept(valStyle);
        XSSFFont valFont = wb.createFont(); valFont.setFontHeightInPoints((short) 11);
        valStyle.setFont(valFont);
        valStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 表头样式
        XSSFCellStyle thStyle = wb.createCellStyle(); addBorders.accept(thStyle);
        XSSFFont thFont = wb.createFont(); thFont.setBold(true); thFont.setFontHeightInPoints((short) 10);
        thStyle.setFont(thFont);
        thStyle.setAlignment(HorizontalAlignment.CENTER);
        thStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        thStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.GREY_25_PERCENT.getIndex());
        thStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

        // 数据样式
        XSSFCellStyle dataStyle = wb.createCellStyle(); addBorders.accept(dataStyle);
        XSSFFont dataFont = wb.createFont(); dataFont.setFontHeightInPoints((short) 10);
        dataStyle.setFont(dataFont);
        dataStyle.setAlignment(HorizontalAlignment.CENTER);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // 合计样式
        XSSFCellStyle sumStyle = wb.createCellStyle(); addBorders.accept(sumStyle);
        XSSFFont sumFont = wb.createFont(); sumFont.setBold(true); sumFont.setFontHeightInPoints((short) 10);
        sumStyle.setFont(sumFont);
        sumStyle.setAlignment(HorizontalAlignment.CENTER);
        sumStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        final int COL_COUNT = 14; // 列数 0~13

        // Row 0: 标题行
        sheet.createRow(0).createCell(0).setCellValue("北京华测食农认证服务有限公司 — 产品防伪追溯标志订单");
        sheet.getRow(0).getCell(0).setCellStyle(titleStyle);
        sheet.getRow(0).setHeightInPoints(30);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, COL_COUNT - 1));
        // 填充合并区域边框
        for (int i = 1; i < COL_COUNT; i++) { XSSFCell cc = sheet.getRow(0).createCell(i); cc.setCellStyle(titleStyle); }

        // Row 1: 订单编号 + 企业名称
        XSSFRow r1 = sheet.createRow(1);
        r1.createCell(0).setCellValue("订单编号"); r1.getCell(0).setCellStyle(labelStyle);
        r1.createCell(1).setCellValue(order.getOrderNo()); r1.getCell(1).setCellStyle(valStyle);
        r1.createCell(2).setCellStyle(valStyle);
        r1.createCell(3).setCellValue("企业名称"); r1.getCell(3).setCellStyle(labelStyle);
        for (int i = 4; i < COL_COUNT; i++) { r1.createCell(i).setCellStyle(valStyle); }
        r1.getCell(4).setCellValue(entName);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 4, COL_COUNT - 1));

        // Row 2: 收货地址 + 联系人
        XSSFRow r2 = sheet.createRow(2);
        r2.createCell(0).setCellValue("收货地址"); r2.getCell(0).setCellStyle(labelStyle);
        r2.createCell(1).setCellValue(addressStr); r2.getCell(1).setCellStyle(valStyle);
        r2.createCell(2).setCellStyle(valStyle);
        r2.createCell(3).setCellValue("联系人"); r2.getCell(3).setCellStyle(labelStyle);
        for (int i = 4; i < COL_COUNT; i++) { r2.createCell(i).setCellStyle(valStyle); }
        r2.getCell(4).setCellValue(contactStr);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 4, COL_COUNT - 1));

        // Row 3: 联系电话 + 标签总数量 + 订购总价
        XSSFRow r3 = sheet.createRow(3);
        r3.createCell(0).setCellValue("联系电话"); r3.getCell(0).setCellStyle(labelStyle);
        r3.createCell(1).setCellValue(phoneStr); r3.getCell(1).setCellStyle(valStyle);
        r3.createCell(2).setCellStyle(valStyle);
        r3.createCell(3).setCellValue("标签总数量"); r3.getCell(3).setCellStyle(labelStyle);
        r3.createCell(4).setCellValue(totalLabels); r3.getCell(4).setCellStyle(valStyle);
        r3.createCell(5).setCellStyle(valStyle);
        r3.createCell(6).setCellValue("订购总价（元）"); r3.getCell(6).setCellStyle(labelStyle);
        for (int i = 7; i < COL_COUNT; i++) { r3.createCell(i).setCellStyle(valStyle); }
        r3.getCell(7).setCellValue(totalPrice.doubleValue());
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 1, 2));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 4, 5));
        sheet.addMergedRegion(new CellRangeAddress(3, 3, 7, COL_COUNT - 1));

        // Row 4: 空行（分隔）
        sheet.createRow(4).setHeightInPoints(6);

        // Row 5: 表头
        String[] headers = {"子订单编号", "证书编号", "产品名称", "产品描述", "商品名称", "包装规格", "重量规格", "标签规格", "单价(元)", "订购数量(枚)", "总价(元)", "起始身份码", "结束身份码", "备注"};
        XSSFRow rh = sheet.createRow(5);
        rh.setHeightInPoints(22);
        for (int i = 0; i < headers.length; i++) {
            XSSFCell cell = rh.createCell(i); cell.setCellValue(headers[i]); cell.setCellStyle(thStyle);
        }

        // Row 6+: 明细行
        int rowNum = 6;
        BigDecimal totalQuantitySum = BigDecimal.ZERO;
        int totalQtySum = 0;
        BigDecimal totalSumSum = BigDecimal.ZERO;
        int itemIdx = 1;
        for (OrderItem oi : items) {
            List<OrderCode> matchedCodes = new java.util.ArrayList<>();
            if (oi.getLabelSpecId() != null) {
                for (OrderCode oc : codes) {
                    if (oi.getLabelSpecId().equals(oc.getLabelSpecId())) matchedCodes.add(oc);
                }
            }
            Goods g = null;
            if (oi.getGoodsId() != null) g = goodsMapper.selectById(oi.getGoodsId());

            if (matchedCodes.isEmpty()) {
                XSSFRow row = sheet.createRow(rowNum++);
                row.setHeightInPoints(20);
                String[] vals = {
                    order.getOrderNo() + "-" + itemIdx, certNoStr,
                    g != null && g.getProductName() != null ? g.getProductName() : "",
                    g != null && g.getIntroduction() != null ? g.getIntroduction() : "",
                    oi.getGoodsName() != null ? oi.getGoodsName() : "",
                    g != null && g.getPackageSpec() != null ? g.getPackageSpec() : "",
                    g != null && g.getWeightSpec() != null ? g.getWeightSpec() : "",
                    oi.getLabelSpecName() != null ? oi.getLabelSpecName() : "",
                    oi.getPrice() != null ? String.valueOf(oi.getPrice().doubleValue()) : "0",
                    String.valueOf(oi.getQuantity() != null ? oi.getQuantity() : 0),
                    oi.getTotalPrice() != null ? String.valueOf(oi.getTotalPrice().doubleValue()) : "0",
                    "", "", ""
                };
                for (int i = 0; i < vals.length; i++) {
                    XSSFCell cell = row.createCell(i); cell.setCellValue(vals[i]); cell.setCellStyle(dataStyle);
                }
                totalQtySum += oi.getQuantity() != null ? oi.getQuantity() : 0;
                totalSumSum = totalSumSum.add(oi.getTotalPrice() != null ? oi.getTotalPrice() : BigDecimal.ZERO);
            } else {
                int startRow = rowNum;
                for (OrderCode oc : matchedCodes) {
                    XSSFRow row = sheet.createRow(rowNum++);
                    row.setHeightInPoints(20);
                    String[] vals = {
                        order.getOrderNo() + "-" + itemIdx, certNoStr,
                        oc.getProductName() != null ? oc.getProductName() : "",
                        g != null && g.getIntroduction() != null ? g.getIntroduction() : "",
                        oi.getGoodsName() != null ? oi.getGoodsName() : "",
                        g != null && g.getPackageSpec() != null ? g.getPackageSpec() : "",
                        g != null && g.getWeightSpec() != null ? g.getWeightSpec() : "",
                        oc.getLabelSpecName() != null ? oc.getLabelSpecName() : "",
                        oc.getPrice() != null ? String.valueOf(oc.getPrice().doubleValue()) : "0",
                        String.valueOf(oc.getQuantity() != null ? oc.getQuantity() : 0),
                        oi.getTotalPrice() != null ? String.valueOf(oi.getTotalPrice().doubleValue()) : "0",
                        oc.getSerialStart() != null ? oc.getSerialStart() : "",
                        oc.getSerialEnd() != null ? oc.getSerialEnd() : "",
                        ""
                    };
                    for (int i = 0; i < vals.length; i++) {
                        XSSFCell cell = row.createCell(i); cell.setCellValue(vals[i]); cell.setCellStyle(dataStyle);
                    }
                    totalQtySum += oc.getQuantity() != null ? oc.getQuantity() : 0;
                    totalSumSum = totalSumSum.add(oi.getTotalPrice() != null ? oi.getTotalPrice() : BigDecimal.ZERO);
                }
                // 多行时合并前7列（子订单编号~标签规格）
                if (matchedCodes.size() > 1) {
                    for (int col = 0; col <= 7; col++) {
                        sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, col, col));
                    }
                    // 合并总价列
                    sheet.addMergedRegion(new CellRangeAddress(startRow, rowNum - 1, 10, 10));
                }
            }
            itemIdx++;
        }

        // 合计行
        XSSFRow totalRow = sheet.createRow(rowNum);
        totalRow.setHeightInPoints(22);
        for (int i = 0; i < COL_COUNT; i++) {
            XSSFCell cell = totalRow.createCell(i); cell.setCellStyle(sumStyle);
        }
        totalRow.getCell(0).setCellValue("合计");
        sheet.addMergedRegion(new CellRangeAddress(rowNum, rowNum, 0, 8));
        totalRow.getCell(9).setCellValue(totalQtySum);
        totalRow.getCell(10).setCellValue(totalSumSum.doubleValue());

        // 列宽设置
        int[] colWidths = {5000, 4000, 3500, 4000, 3500, 3000, 3000, 3500, 2500, 3000, 3000, 3500, 3500, 2500};
        for (int i = 0; i < COL_COUNT; i++) sheet.setColumnWidth(i, colWidths[i]);

        // 文件名
        String fileName = order.getOrderNo() + ".xlsx";
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        wb.write(response.getOutputStream());
        wb.close();
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
            item.put("serialDigits", cp.getSerialDigits());
            item.put("labelSpecId", cp.getLabelSpecId());
            // 生成生码规则名称
            String ruleName = (cp.getSerialDigits() != null && cp.getSerialDigits() > 0)
                    ? cp.getSerialDigits() + "位身份码条码库"
                    : "通用条码库";
            item.put("ruleName", ruleName);
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

    // ==================== 视频监控管理 ====================
    @GetMapping("/video-sources")
    public Result<List<VideoSource>> listAllVideoSources() {
        return Result.ok(videoSourceService.listAll());
    }

    // ==================== IoT 设备管理 ====================
    @GetMapping("/iot-devices")
    public Result<List<IotDevice>> listAllIotDevices() {
        return Result.ok(iotDeviceService.listAll());
    }

    // ==================== IoT 告警 ====================
    @GetMapping("/iot-alerts")
    public Result<List<IotAlertRecord>> listAllIotAlerts() {
        return Result.ok(alertRecordMapper.selectList(
                new LambdaQueryWrapper<IotAlertRecord>()
                        .orderByDesc(IotAlertRecord::getCreatedAt)
                        .last("LIMIT 200")));
    }

    // ==================== 溯源码发放管理 ====================
    @GetMapping("/code-distribution")
    public Result<PageResult<OrderCode>> listCodeDistribution(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long enterpriseId,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) String certNo) {
        LambdaQueryWrapper<OrderCode> w = new LambdaQueryWrapper<>();
        w.orderByDesc(OrderCode::getId);
        Page<OrderCode> r = orderCodeMapper.selectPage(new Page<>(page, size), w);
        // 填充关联信息
        r.getRecords().forEach(oc -> {
            if (oc.getOrderId() != null) {
                Order o = orderMapper.selectById(oc.getOrderId());
                if (o != null) {
                    oc.setOrderNo(o.getOrderNo());
                    if (o.getEnterpriseId() != null) {
                        Enterprise e = enterpriseMapper.selectById(o.getEnterpriseId());
                        if (e != null) oc.setEnterpriseName(e.getName());
                    }
                    if (o.getCertId() != null) {
                        EnterpriseCert c = enterpriseCertMapper.selectById(o.getCertId());
                        if (c != null) oc.setCertNo(c.getCertNo());
                    }
                }
            }
            if (oc.getLabelSpecId() != null) {
                LabelSpec ls = labelSpecService.getById(oc.getLabelSpecId());
                if (ls != null) oc.setLabelSpecName(ls.getSpecName());
            }
            // 从订单明细获取商品信息
            if (oc.getOrderId() != null && oc.getLabelSpecId() != null) {
                OrderItem oi = orderItemMapper.selectOne(
                        new LambdaQueryWrapper<OrderItem>()
                                .eq(OrderItem::getOrderId, oc.getOrderId())
                                .eq(OrderItem::getLabelSpecId, oc.getLabelSpecId())
                                .last("LIMIT 1"));
                if (oi != null) {
                    oc.setGoodsName(oi.getGoodsName());
                    if (oi.getGoodsId() != null) {
                        Goods g = goodsMapper.selectById(oi.getGoodsId());
                        if (g != null) {
                            oc.setProductDescription(g.getIntroduction());
                            oc.setGoodsPackageSpec(g.getPackageSpec());
                            oc.setGoodsWeightSpec(g.getWeightSpec());
                        }
                    }
                }
            }
        });
        // 前端筛选
        java.util.List<OrderCode> filtered = r.getRecords();
        if (enterpriseId != null) {
            filtered = filtered.stream().filter(oc -> {
                if (oc.getOrderId() == null) return false;
                Order o = orderMapper.selectById(oc.getOrderId());
                return o != null && enterpriseId.equals(o.getEnterpriseId());
            }).collect(java.util.stream.Collectors.toList());
        }
        if (orderNo != null && !orderNo.isEmpty()) {
            String kw = orderNo;
            filtered = filtered.stream().filter(oc -> oc.getOrderNo() != null && oc.getOrderNo().contains(kw)).collect(java.util.stream.Collectors.toList());
        }
        if (certNo != null && !certNo.isEmpty()) {
            String kw = certNo;
            filtered = filtered.stream().filter(oc -> oc.getCertNo() != null && oc.getCertNo().contains(kw)).collect(java.util.stream.Collectors.toList());
        }
        return Result.ok(new PageResult<>(filtered, r.getTotal()));
    }

    // ==================== VR全景管理 ====================
    @GetMapping("/vr-scenes")
    public Result<List<VrScene>> listVrScenes(@RequestParam(required = false) Long enterpriseId) {
        return Result.ok(vrPanoramaService.listScenes(enterpriseId));
    }

    @PostMapping("/vr-scenes")
    public Result<VrScene> createVrScene(@RequestBody VrScene scene) {
        return Result.ok(vrPanoramaService.createScene(scene));
    }

    @PutMapping("/vr-scenes/{id}")
    public Result<VrScene> updateVrScene(@PathVariable Long id, @RequestBody VrScene scene) {
        return Result.ok(vrPanoramaService.updateScene(id, scene));
    }

    @DeleteMapping("/vr-scenes/{id}")
    public Result<Void> deleteVrScene(@PathVariable Long id) {
        vrPanoramaService.deleteScene(id);
        return Result.ok();
    }

    @PostMapping("/vr-hotspots")
    public Result<VrHotspot> createVrHotspot(@RequestBody VrHotspot hotspot) {
        return Result.ok(vrPanoramaService.createHotspot(hotspot));
    }

    @PutMapping("/vr-hotspots/{id}")
    public Result<VrHotspot> updateVrHotspot(@PathVariable Long id, @RequestBody VrHotspot hotspot) {
        return Result.ok(vrPanoramaService.updateHotspot(id, hotspot));
    }

    @DeleteMapping("/vr-hotspots/{id}")
    public Result<Void> deleteVrHotspot(@PathVariable Long id) {
        vrPanoramaService.deleteHotspot(id);
        return Result.ok();
    }

    // ==================== 海报管理 ====================
    @GetMapping("/posters")
    public Result<PageResult<Poster>> listPosters(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        return Result.ok(posterService.list(page, size, keyword));
    }

    @PostMapping("/posters")
    public Result<Poster> createPoster(@RequestParam("file") MultipartFile file,
                                       @RequestParam(required = false) String title) throws Exception {
        return Result.ok(posterService.create(file, title));
    }

    @PutMapping("/posters/{id}")
    public Result<Void> updatePoster(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String title = body.get("title") != null ? body.get("title").toString() : null;
        Integer status = body.get("status") != null ? Integer.parseInt(body.get("status").toString()) : null;
        posterService.update(id, title, status);
        return Result.ok();
    }

    @DeleteMapping("/posters/{id}")
    public Result<Void> deletePoster(@PathVariable Long id) {
        posterService.delete(id);
        return Result.ok();
    }

    @GetMapping("/posters/{id}/qrcode")
    public Result<Map<String, Object>> getPosterQrcode(@PathVariable Long id) {
        Poster poster = posterService.getById(id);
        if (poster == null) throw new BusinessException("海报不存在");
        String qrCode = posterService.getQrCode(id);
        return Result.ok(Map.of("qrCode", qrCode, "posterUrl", poster.getPosterUrl()));
    }
}
