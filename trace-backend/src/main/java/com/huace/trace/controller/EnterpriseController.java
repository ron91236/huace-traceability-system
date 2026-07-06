package com.huace.trace.controller;

import com.huace.trace.common.PageResult;
import com.huace.trace.common.Result;
import com.huace.trace.entity.*;
import com.huace.trace.security.UserPrincipal;
import com.huace.trace.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
public class EnterpriseController {

    private final EnterpriseCertService certService;
    private final EnterpriseBaseService baseService;
    private final GoodsService goodsService;
    private final AddressService addressService;
    private final BatchService batchService;
    private final OrderService orderService;
    private final OrderCodeService orderCodeService;
    private final TraceInventoryService traceInventoryService;
    private final NoticeService noticeService;
    private final DashboardService dashboardService;
    private final TestReportService testReportService;
    private final EnterpriseGroupService enterpriseGroupService;
    private final DataScreenService dataScreenService;
    private final VideoSourceService videoSourceService;
    private final IotDeviceService iotDeviceService;
    private final IotDataService iotDataService;
    private final com.huace.trace.mapper.IotAlertRuleMapper alertRuleMapper;
    private final com.huace.trace.mapper.IotAlertRecordMapper alertRecordMapper;

    /**
     * 解析企业ID列表 — 支持集团母账号聚合查看
     */
    private List<Long> resolveEnterpriseIds(UserPrincipal principal) {
        Long eid = principal.getUserId();
        if ("master".equals(principal.getAccountLevel())) {
            return enterpriseGroupService.getGroupEnterpriseIds(eid);
        }
        return List.of(eid);
    }

    // ==================== 企业认证（只读） ====================
    @GetMapping("/certs")
    public Result<PageResult<EnterpriseCert>> listCerts(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal principal) {
        Long enterpriseId = principal.getUserId();
        return Result.ok(certService.list(page, size, keyword, enterpriseId));
    }

    // ==================== 基地管理 ====================
    @GetMapping("/bases")
    public Result<PageResult<EnterpriseBase>> listBases(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(baseService.list(page, size, keyword, principal.getUserId()));
    }

    @PutMapping("/bases/{id}")
    public Result<Void> updateBase(@PathVariable Long id, @RequestBody EnterpriseBase base,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        baseService.update(id, base, principal.getUserId());
        return Result.ok();
    }

    // ==================== 商品管理 ====================
    @GetMapping("/goods")
    public Result<PageResult<Goods>> listGoods(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(goodsService.list(page, size, keyword, principal.getUserId()));
    }

    @PostMapping("/goods")
    public Result<Void> createGoods(@RequestBody Goods goods,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        goods.setEnterpriseId(principal.getUserId());
        goodsService.create(goods);
        return Result.ok();
    }

    @PutMapping("/goods/{id}")
    public Result<Void> updateGoods(@PathVariable Long id, @RequestBody Goods goods,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        goodsService.update(id, goods, principal.getUserId());
        return Result.ok();
    }

    @DeleteMapping("/goods/{id}")
    public Result<Void> deleteGoods(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        goodsService.delete(id, principal.getUserId());
        return Result.ok();
    }

    // ==================== 收货地址 ====================
    @GetMapping("/addresses")
    public Result<PageResult<Address>> listAddresses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(addressService.listByEnterprise(page, size, principal.getUserId()));
    }

    @PostMapping("/addresses")
    public Result<Void> createAddress(@RequestBody Address address,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        address.setEnterpriseId(principal.getUserId());
        addressService.create(address);
        return Result.ok();
    }

    @PutMapping("/addresses/{id}")
    public Result<Void> updateAddress(@PathVariable Long id, @RequestBody Address address,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        addressService.update(id, address, principal.getUserId());
        return Result.ok();
    }

    @DeleteMapping("/addresses/{id}")
    public Result<Void> deleteAddress(@PathVariable Long id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        addressService.delete(id, principal.getUserId());
        return Result.ok();
    }

    // ==================== 批次管理 ====================
    @GetMapping("/batches")
    public Result<PageResult<Batch>> listBatches(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(batchService.list(page, size, keyword, principal.getUserId()));
    }

    @PostMapping("/batches")
    public Result<Void> createBatch(@RequestBody Batch batch,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        batch.setEnterpriseId(principal.getUserId());
        batchService.create(batch);
        return Result.ok();
    }

    @PutMapping("/batches/{id}")
    public Result<Void> updateBatch(@PathVariable Long id, @RequestBody Batch batch,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        batchService.update(id, batch, principal.getUserId());
        return Result.ok();
    }

    @GetMapping("/batches/{id}/qrcode")
    public Result<String> getBatchQrcode(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(batchService.generateQrcode(id, principal.getUserId()));
    }

    // ==================== 订单管理 ====================
    @GetMapping("/orders")
    public Result<PageResult<Order>> listOrders(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(orderService.list(page, size, keyword, principal.getUserId(), status));
    }

    @PostMapping("/orders")
    public Result<Void> createOrder(@RequestBody Order order,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        order.setEnterpriseId(principal.getUserId());
        orderService.create(order);
        return Result.ok();
    }

    @GetMapping("/orders/{id}")
    public Result<Map<String, Object>> getOrderDetail(@PathVariable Long id,
                                                       @AuthenticationPrincipal UserPrincipal principal) {
        Order order = orderService.getById(id, principal.getUserId());
        List<OrderCode> orderCodes = orderService.getOrderCodes(id);
        List<OrderItem> orderItems = orderService.getOrderItems(id);
        return Result.ok(Map.of("order", order, "orderCodes", orderCodes, "orderItems", orderItems));
    }

    @DeleteMapping("/orders/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        orderService.delete(id, principal.getUserId());
        return Result.ok();
    }

    @PostMapping("/orders/{id}/submit")
    public Result<Void> submitOrder(@PathVariable Long id,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        orderService.submit(id, principal.getUserId(), principal);
        return Result.ok();
    }

    @GetMapping("/orders/{id}/audit-history")
    public Result<List<AuditLog>> getAuditHistory(@PathVariable Long id,
                                                   @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(orderService.getAuditHistory(id, principal.getUserId()));
    }

    // ==================== 订单明细 ====================
    @PostMapping("/order-items")
    public Result<Void> addOrderItem(@RequestBody OrderItem item,
                                      @AuthenticationPrincipal UserPrincipal principal) {
        orderService.addOrderItem(item, principal.getUserId());
        return Result.ok();
    }

    @PutMapping("/order-items/{id}")
    public Result<Void> updateOrderItem(@PathVariable Long id, @RequestBody OrderItem item,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        orderService.updateOrderItem(id, item, principal.getUserId());
        return Result.ok();
    }

    @DeleteMapping("/order-items/{id}")
    public Result<Void> deleteOrderItem(@PathVariable Long id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        orderService.deleteOrderItem(id, principal.getUserId());
        return Result.ok();
    }

    // ==================== 订单条码 ====================
    @GetMapping("/order-codes")
    public Result<PageResult<OrderCode>> listOrderCodes(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long orderId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(orderCodeService.list(page, size, orderId, principal.getUserId()));
    }

    @PutMapping("/order-codes/{id}")
    public Result<Void> updateOrderCode(@PathVariable Long id, @RequestBody OrderCode orderCode,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        orderCodeService.update(id, orderCode, principal.getUserId());
        return Result.ok();
    }

    @GetMapping("/order-codes/{id}/preview")
    public Result<Map<String, Object>> previewOrderCode(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(orderCodeService.preview(id, principal.getUserId()));
    }

    // ==================== 条码使用 ====================
    @GetMapping("/code-usages")
    public Result<PageResult<TraceInventory>> listCodeUsages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(traceInventoryService.list(page, size, principal.getUserId()));
    }

    @PostMapping("/code-usages")
    public Result<Void> createCodeUsage(@RequestBody TraceInventory inventory,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        inventory.setEnterpriseId(principal.getUserId());
        traceInventoryService.create(inventory);
        return Result.ok();
    }

    @DeleteMapping("/code-usages/{id}")
    public Result<Void> deleteCodeUsage(@PathVariable Long id,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        traceInventoryService.delete(id, principal.getUserId());
        return Result.ok();
    }

    // ==================== 公告（只读） ====================
    @GetMapping("/notices")
    public Result<PageResult<Notice>> listNotices(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(noticeService.list(page, size, keyword, principal.getUserId()));
    }

    // ==================== 控制台 ====================
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> enterpriseDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> enterpriseIds = resolveEnterpriseIds(principal);
        return Result.ok(dashboardService.enterpriseStats(enterpriseIds));
    }

    // ==================== 检测报告管理 ====================
    @GetMapping("/test-reports")
    public Result<PageResult<TestReport>> listTestReports(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(testReportService.list(page, size, keyword, principal.getUserId()));
    }

    @GetMapping("/test-reports/all")
    public Result<List<TestReport>> allTestReports(
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(testReportService.listByEnterprise(principal.getUserId()));
    }

    @PostMapping("/test-reports")
    public Result<Void> createTestReport(@RequestBody TestReport report,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        report.setEnterpriseId(principal.getUserId());
        testReportService.create(report);
        return Result.ok();
    }

    @PutMapping("/test-reports/{id}")
    public Result<Void> updateTestReport(@PathVariable Long id, @RequestBody TestReport report,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        testReportService.update(id, report, principal.getUserId());
        return Result.ok();
    }

    @DeleteMapping("/test-reports/{id}")
    public Result<Void> deleteTestReport(@PathVariable Long id,
                                          @AuthenticationPrincipal UserPrincipal principal) {
        testReportService.delete(id, principal.getUserId());
        return Result.ok();
    }

    // ==================== 企业集团 ====================
    @GetMapping("/group/children")
    public Result<List<Enterprise>> listGroupChildren(
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!"master".equals(principal.getAccountLevel())) return Result.ok(List.of());
        return Result.ok(enterpriseGroupService.listChildEnterprises(principal.getUserId()));
    }

    // ==================== 数据大屏 ====================
    @GetMapping("/data-screen/all")
    public Result<Map<String, Object>> getEntDataScreen(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Long> enterpriseIds = resolveEnterpriseIds(principal);
        // DataScreenService takes single enterpriseId, for group we pass first or use full
        Long eid = enterpriseIds.size() == 1 ? enterpriseIds.get(0) : null;
        return Result.ok(dataScreenService.getAllData(eid));
    }

    // ==================== 视频源管理 ====================
    @GetMapping("/video-sources")
    public Result<List<VideoSource>> listVideoSources(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long baseId,
            @RequestParam(required = false) Long batchId) {
        return Result.ok(videoSourceService.listByEnterprise(principal.getUserId(), baseId, batchId));
    }

    @PostMapping("/video-sources")
    public Result<Void> createVideoSource(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody VideoSource source) {
        source.setEnterpriseId(principal.getUserId());
        videoSourceService.createVideoSource(source);
        return Result.ok(null);
    }

    @PutMapping("/video-sources/{id}")
    public Result<VideoSource> updateVideoSource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody VideoSource source) {
        source.setEnterpriseId(principal.getUserId());
        return Result.ok(videoSourceService.updateVideoSource(id, source));
    }

    @DeleteMapping("/video-sources/{id}")
    public Result<Void> deleteVideoSource(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        videoSourceService.deleteVideoSource(id);
        return Result.ok(null);
    }

    // ==================== IoT 设备管理 ====================
    @GetMapping("/iot-devices")
    public Result<PageResult<IotDevice>> listIotDevices(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String deviceType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.ok(iotDeviceService.listByEnterprise(principal.getUserId(), deviceType, page, size));
    }

    @PostMapping("/iot-devices")
    public Result<Void> registerIotDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody IotDevice device) {
        device.setEnterpriseId(principal.getUserId());
        iotDeviceService.registerDevice(device);
        return Result.ok(null);
    }

    @PutMapping("/iot-devices/{id}")
    public Result<IotDevice> updateIotDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody IotDevice device) {
        device.setEnterpriseId(principal.getUserId());
        return Result.ok(iotDeviceService.updateDevice(id, device));
    }

    @DeleteMapping("/iot-devices/{id}")
    public Result<Void> deleteIotDevice(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        iotDeviceService.deleteDevice(id);
        return Result.ok(null);
    }

    @GetMapping("/iot-devices/{id}/latest")
    public Result<Object> getDeviceLatest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return Result.ok(iotDeviceService.getDeviceLatest(id));
    }

    @GetMapping("/iot-devices/{id}/history")
    public Result<List<com.huace.trace.entity.mongo.IotSensorData>> getDeviceHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(defaultValue = "24") int hours) {
        java.time.LocalDateTime to = java.time.LocalDateTime.now();
        return Result.ok(iotDataService.getSensorHistory(id, to.minusHours(hours), to));
    }

    // ==================== IoT 告警 ====================
    @GetMapping("/iot-alerts")
    public Result<List<IotAlertRecord>> listIotAlerts(
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(alertRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IotAlertRecord>()
                        .eq(IotAlertRecord::getEnterpriseId, principal.getUserId())
                        .orderByDesc(IotAlertRecord::getCreatedAt)
                        .last("LIMIT 100")));
    }

    @PutMapping("/iot-alerts/{id}/handle")
    public Result<Void> handleIotAlert(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        IotAlertRecord record = new IotAlertRecord();
        record.setId(id);
        record.setHandleStatus(1);
        record.setHandleNote(body.get("handleNote"));
        alertRecordMapper.updateById(record);
        return Result.ok(null);
    }

    @GetMapping("/iot-alert-rules")
    public Result<List<IotAlertRule>> listAlertRules(
            @AuthenticationPrincipal UserPrincipal principal) {
        return Result.ok(alertRuleMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IotAlertRule>()
                        .eq(IotAlertRule::getEnterpriseId, principal.getUserId())));
    }

    @PostMapping("/iot-alert-rules")
    public Result<Void> createAlertRule(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody IotAlertRule rule) {
        rule.setEnterpriseId(principal.getUserId());
        alertRuleMapper.insert(rule);
        return Result.ok(null);
    }
}
