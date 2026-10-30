package com.huace.trace.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.*;
import com.huace.trace.entity.mongo.CodePackageItemMongo;
import com.huace.trace.mapper.*;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 防伪码验证核心规则回归测试：
 * 1. 完整防伪码匹配
 * 2. 后4-6位匹配
 * 3. 错误防伪码不递增扫码次数
 * 4. 空输入
 * 5. 流水号不存在
 * 以及溯源页批次切换规则（该功能按企业开通，属个性化定制）：
 * 6. 默认展示码绑定的批次
 * 7. 已开通的企业切换到同企业同商品的其他批次后，批次块与合格证随之变化
 * 8. 其他商品/企业的批次被拒绝
 * 9. 未开通的企业忽略 batchId，批次列表返回空
 * 10. 未绑定或未回填生产日期时的批次列表行为
 * 以及长图模板取数开关：
 * 11. 只有 config_json.renderMode=long-image 的模板才附带 longModules，且取数入参是模板行 + 企业 id
 * 以及防伪验证的写入边界：
 * 12. 验证只推进 scan_count，不把 MongoDB 那份过期拷贝整行写回 MySQL（否则等于解绑）
 */
@ExtendWith(MockitoExtension.class)
class TracePageServiceTest {

    @Mock private CodePackageItemMapper codePackageItemMapper;
    @Mock private EnterpriseMapper enterpriseMapper;
    @Mock private GoodsMapper goodsMapper;
    @Mock private EnterpriseCertMapper certMapper;
    @Mock private CertTypeMapper certTypeMapper;
    @Mock private BatchMapper batchMapper;
    @Mock private EnterpriseBaseMapper baseMapper;
    @Mock private TraceTemplateMapper templateMapper;
    @Mock private ScanRecordMapper scanRecordMapper;
    @Mock private TestReportMapper testReportMapper;
    @Mock private EnterpriseTemplateDataMapper templateDataMapper;
    @Mock private TestReportService testReportService;
    @Mock private ObjectMapper objectMapper;
    @Mock private MongoCodeItemService mongoCodeItemService;
    @Mock private OrderCodeMapper orderCodeMapper;
    @Mock private IpRegionService ipRegionService;
    @Mock private HgzService hgzService;
    @Mock private LongModuleService longModuleService;
    /**
     * 落地页这三个依赖都按 mock 处理：本类测的是 queryLanding 的组装与回落判断，
     * 骨架解析规则在 TemplateConfigReaderTest、内容规则在 LandingContentServiceTest，
     * 在这里再测一遍只会让同一件事有两个归属。
     */
    @Mock private TemplateConfigReader configReader;
    @Mock private LandingContentService landingContentService;
    @Mock private SideLinkService sideLinkService;

    @InjectMocks
    private TracePageService service;

    /** 默认未开通批次切换定制的企业，用例按需 setBatchSwitchEnabled(1) */
    private Enterprise enterprise;

    /**
     * LambdaUpdateWrapper 渲染 SET/WHERE 片段时要查实体的列缓存，而缓存只在 MyBatis 启动扫描
     * 实体时装好；这里是纯 Mockito 环境，不装就抛 "can not find lambda cache for this entity"。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), CodePackageItem.class);
    }

    private static String flat(String sql) {
        return sql.replaceAll("\\s+", "");
    }

    @BeforeEach
    void setUp() {
        lenient().when(mongoCodeItemService.findBySerialNo(anyString())).thenReturn(Optional.empty());
        enterprise = new Enterprise();
        enterprise.setId(1L);
        enterprise.setName("河南福鹿家鲜啤酒业有限公司");
        enterprise.setBatchSwitchEnabled(0);
        lenient().when(enterpriseMapper.selectById(1L)).thenReturn(enterprise);
        Goods goods = new Goods();
        goods.setId(2L);
        goods.setName("德式小麦");
        lenient().when(goodsMapper.selectById(2L)).thenReturn(goods);
        EnterpriseCert cert = new EnterpriseCert();
        cert.setId(3L);
        cert.setCertName("全程可追溯产品验证证书");
        lenient().when(certMapper.selectById(3L)).thenReturn(cert);
    }

    private CodePackageItem buildItem(String antiFakeCode) {
        CodePackageItem item = new CodePackageItem();
        item.setId(1L);
        item.setSerialNo("00000626");
        item.setAntiFakeCode(antiFakeCode);
        item.setScanCount(5);
        item.setEnterpriseId(1L);
        item.setGoodsId(2L);
        item.setCertId(3L);
        item.setBindStatus("BOUND");
        return item;
    }

    @Test
    void 完整防伪码匹配_验证通过并递增扫码次数() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "1234567890");

        assertEquals(true, result.get("verified"));
        assertEquals(6, result.get("scanCount"));
        assertEquals("验证通过，该产品为正品", result.get("message"));
        assertEquals("河南福鹿家鲜啤酒业有限公司", result.get("enterpriseName"));
        assertEquals("德式小麦", result.get("productName"));
        assertNull(result.get("certName"), "认证机构字段已移除");
        // MySQL 兜底出来的整行也不能随手写回：这条路径唯一的正当改动是计数
        verify(codePackageItemMapper, never()).updateById(any(CodePackageItem.class));
        String set = flat(captureCountUpdate().getSqlSet());
        assertTrue(set.contains("scan_count="), set);
        assertFalse(set.contains("bind_status="), set);
        verify(mongoCodeItemService).updateScanCount(eq("00000626"), eq(6));
    }

    @Test
    void 后四位防伪码匹配_验证通过() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "7890");

        assertEquals(true, result.get("verified"));
        assertEquals(6, result.get("scanCount"));
    }

    @Test
    void 后六位防伪码匹配_验证通过() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "567890");

        assertEquals(true, result.get("verified"));
    }

    @Test
    void 错误防伪码_验证失败且不递增扫码次数() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "9999");

        assertEquals(false, result.get("verified"));
        assertEquals(5, result.get("scanCount"));
        assertEquals("验证失败，防伪码不匹配，请谨防假冒", result.get("message"));
        verify(codePackageItemMapper, never()).updateById(any(CodePackageItem.class));
        verify(codePackageItemMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(mongoCodeItemService, never()).updateScanCount(anyString(), anyInt());
    }

    @Test
    void 三位输入_不匹配() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "890");

        assertEquals(false, result.get("verified"));
    }

    /** Mongo 里那份过期拷贝：绑码只写 MySQL，所以码在 Mongo 仍是 UNBOUND、关联 id 全是 null */
    private CodePackageItemMongo staleMongoDoc() {
        CodePackageItemMongo m = new CodePackageItemMongo();
        m.setItemId(1L);
        m.setPackageId(32L);
        m.setSerialNo("00000626");
        m.setAntiFakeCode("1234567890");
        m.setBindStatus("UNBOUND");
        return m;
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<CodePackageItem> captureCountUpdate() {
        ArgumentCaptor<LambdaUpdateWrapper<CodePackageItem>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(codePackageItemMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }

    @Test
    void 扫码即验证只写扫码次数不动绑定状态() {
        // 旧写法把 Mongo 文档整个 updateById 回 MySQL，NOT_NULL 策略连 bind_status='UNBOUND'
        // 一起写回去 —— 消费者点一次验证就把企业已激活的码解绑了（2026-10-29 生产实测复现）
        when(mongoCodeItemService.findBySerialNo("00000626")).thenReturn(Optional.of(staleMongoDoc()));

        Map<String, Object> result = service.directVerifyBySerialNo("00000626");

        assertEquals(true, result.get("verified"));
        assertEquals(1, result.get("scanCount"));
        verify(codePackageItemMapper, never()).updateById(any(CodePackageItem.class));
        LambdaUpdateWrapper<CodePackageItem> w = captureCountUpdate();
        String set = flat(w.getSqlSet());
        assertTrue(set.contains("scan_count="), set);
        assertFalse(set.contains("bind_status="), set);
        assertFalse(set.contains("enterprise_id="), set);
        assertFalse(set.contains("trace_template="), set);
        // WHERE 必须锁在流水号上：条件为空就是一条全表 UPDATE，会把九千多个未绑定码一起改掉
        assertTrue(flat(w.getSqlSegment()).contains("serial_no="), w.getSqlSegment());
        verify(mongoCodeItemService).updateScanCount(eq("00000626"), eq(1));
    }

    @Test
    void 防伪码验证命中时也只写扫码次数() {
        when(mongoCodeItemService.findBySerialNo("00000626")).thenReturn(Optional.of(staleMongoDoc()));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "1234567890");

        assertEquals(true, result.get("verified"));
        assertEquals(1, result.get("scanCount"));
        verify(codePackageItemMapper, never()).updateById(any(CodePackageItem.class));
        String set = flat(captureCountUpdate().getSqlSet());
        assertTrue(set.contains("scan_count="), set);
        assertFalse(set.contains("bind_status="), set);
    }

    @Test
    void 空输入_验证失败() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(buildItem("1234567890"));

        Map<String, Object> result = service.verifyAntiFakeCode("00000626", "");

        assertEquals(false, result.get("verified"));
        assertEquals(5, result.get("scanCount"));
    }

    @Test
    void 流水号不存在_返回未找到() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(null);

        Map<String, Object> result = service.verifyAntiFakeCode("99999999", "123456");

        assertEquals(false, result.get("verified"));
        assertEquals("未找到该产品的溯源信息", result.get("message"));
        verify(codePackageItemMapper, never()).updateById(any(CodePackageItem.class));
    }

    // ==================== 溯源页批次切换 ====================

    private Batch buildBatch(Long id, String name, LocalDate productionDate, Long goodsId, Long enterpriseId) {
        Batch b = new Batch();
        b.setId(id);
        b.setName(name);
        b.setProductionDate(productionDate);
        b.setGoodsId(goodsId);
        b.setEnterpriseId(enterpriseId);
        b.setTestResult("合格");
        return b;
    }

    private CodePackageItem itemBoundToBatch(Long batchId) {
        CodePackageItem item = buildItem("1234567890");
        item.setBatchId(batchId);
        return item;
    }

    @SuppressWarnings("unchecked")
    @Test
    void 默认展示码绑定的批次与生产日期() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(itemBoundToBatch(100L));
        when(batchMapper.selectById(100L))
                .thenReturn(buildBatch(100L, "2026春批次", LocalDate.of(2026, 3, 1), 2L, 1L));

        Map<String, Object> result = service.queryBySerialNo("00000626", null, null);

        Map<String, Object> batch = (Map<String, Object>) result.get("batch");
        assertEquals("2026春批次", batch.get("name"));
        assertEquals("2026-03-01", batch.get("productionDate"));
        assertEquals(100L, result.get("_batchId"));
        verify(scanRecordMapper).insert(any(ScanRecord.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 切换批次后报告与合格证按所选批次返回() {
        enterprise.setBatchSwitchEnabled(1);
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(itemBoundToBatch(100L));
        when(batchMapper.selectById(100L))
                .thenReturn(buildBatch(100L, "2026春批次", LocalDate.of(2026, 3, 1), 2L, 1L));
        when(batchMapper.selectById(200L))
                .thenReturn(buildBatch(200L, "2026秋批次", LocalDate.of(2026, 9, 1), 2L, 1L));
        when(testReportService.buildReportListForBatch(200L, null))
                .thenReturn(List.of(Map.of("reportName", "秋季检测报告")));

        Map<String, Object> result = service.queryBySerialNo("00000626", 200L, null);

        Map<String, Object> batch = (Map<String, Object>) result.get("batch");
        assertEquals("2026秋批次", batch.get("name"));
        assertEquals("2026-09-01", batch.get("productionDate"));
        assertEquals(1, result.get("testReportCount"));
        assertEquals(200L, result.get("_batchId"));
        verify(hgzService).findLatestForTrace(1L, 200L, 2L);
        // 切换批次属于同一次查看，不重复写扫码记录
        verify(scanRecordMapper, never()).insert(any(ScanRecord.class));
    }

    @Test
    void 切换到其他商品的批次被拒绝() {
        enterprise.setBatchSwitchEnabled(1);
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(itemBoundToBatch(100L));
        when(batchMapper.selectById(100L))
                .thenReturn(buildBatch(100L, "2026春批次", LocalDate.of(2026, 3, 1), 2L, 1L));
        when(batchMapper.selectById(300L))
                .thenReturn(buildBatch(300L, "其他产品批次", null, 9L, 1L));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.queryBySerialNo("00000626", 300L, null));

        assertTrue(e.getMessage().contains("不属于该产品"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void 未开通批次切换定制的企业忽略batchId() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(itemBoundToBatch(100L));
        when(batchMapper.selectById(100L))
                .thenReturn(buildBatch(100L, "2026春批次", LocalDate.of(2026, 3, 1), 2L, 1L));

        Map<String, Object> result = service.queryBySerialNo("00000626", 200L, null);

        Map<String, Object> batch = (Map<String, Object>) result.get("batch");
        assertEquals("2026春批次", batch.get("name"));
        assertEquals(100L, result.get("_batchId"));
        verify(batchMapper, never()).selectById(200L);
        verify(hgzService).findLatestForTrace(1L, 100L, 2L);
    }

    @Test
    void 未开通批次切换定制时批次列表为空() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(itemBoundToBatch(100L));

        assertTrue(service.listBatchesBySerialNo("00000626").isEmpty());
        verify(batchMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void 批次列表返回同商品批次并标记当前批次() {
        enterprise.setBatchSwitchEnabled(1);
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class)))
                .thenReturn(itemBoundToBatch(100L));
        // 排序由 SQL 完成（production_date DESC, id DESC），此处只校验映射
        when(batchMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(
                        buildBatch(100L, "2026春批次", LocalDate.of(2026, 3, 1), 2L, 1L),
                        buildBatch(101L, "旧批次", null, 2L, 1L)));

        List<Map<String, Object>> list = service.listBatchesBySerialNo("00000626");

        assertEquals(2, list.size());
        assertEquals(100L, list.get(0).get("id"));
        assertEquals(true, list.get(0).get("current"));
        assertEquals("2026-03-01", list.get(0).get("productionDate"));
        assertEquals(false, list.get(1).get("current"));
        assertNull(list.get(1).get("productionDate"));
    }

    @Test
    void 码未绑定商品时批次列表为空() {
        CodePackageItem item = buildItem("1234567890");
        item.setGoodsId(null);
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(item);

        assertTrue(service.listBatchesBySerialNo("00000626").isEmpty());
        verify(batchMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    // ==================== 长图模板 ====================

    @Test
    void 长图模板输出longModules() throws Exception {
        CodePackageItem item = buildItem("1234567890");
        item.setTraceTemplate("long-image");
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(item);

        TraceTemplate tpl = new TraceTemplate();
        tpl.setId(99L);
        tpl.setTemplateKey("long-image");
        tpl.setTemplateName("长图溯源");
        tpl.setConfigJson("{\"renderMode\":\"long-image\"}");
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tpl);
        // objectMapper 是 @Mock，不 stub 则 readValue 返回 null → templateConfig 缺失 → 分支不进
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(new HashMap<>(Map.of("renderMode", "long-image")));
        List<Map<String, Object>> modules = List.of(
                Map.of("key", "m1730000000000", "name", "有机养土", "pages", List.of("/uploads/a.png")));
        when(longModuleService.resolveForTrace(tpl, 1L)).thenReturn(modules);

        Map<String, Object> result = service.queryBySerialNo("00000626", null, null);

        assertEquals("long-image", result.get("templateKey"));
        // 断相等而不是断非空：要证明下发的确实是 resolveForTrace 的返回，
        // 存一个空列表也"非空"的写法会照样通过，前端拿到的却是空白长图
        assertEquals(modules, result.get("longModules"));
        // 断的是"把模板行本身交下去"，不是"id 对得上"：骨架现在读自 config_json，
        // 换成传 id 让服务自己去查的写法会让这条断言形同虚设
        verify(longModuleService).resolveForTrace(tpl, 1L);
    }

    @Test
    void 非长图模板不查模块() throws Exception {
        CodePackageItem item = buildItem("1234567890");
        item.setTraceTemplate("default");
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(item);

        TraceTemplate tpl = new TraceTemplate();
        tpl.setId(1L);
        tpl.setTemplateKey("default");
        tpl.setConfigJson("{\"theme\":{\"key\":\"standard-green\"}}");
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tpl);
        when(objectMapper.readValue(anyString(), any(TypeReference.class)))
                .thenReturn(new HashMap<>(Map.of("theme", Map.of("key", "standard-green"))));

        Map<String, Object> result = service.queryBySerialNo("00000626", null, null);

        assertNull(result.get("longModules"));
        verify(longModuleService, never()).resolveForTrace(any(), any());
    }

    // ==================== 落地页 ====================

    private TraceTemplate landingTpl() {
        TraceTemplate tpl = new TraceTemplate();
        tpl.setId(30L);
        tpl.setTemplateKey("tpl_landing");
        tpl.setTemplateName("落地页模板");
        tpl.setConfigJson("{\"renderMode\":\"landing\"}");
        return tpl;
    }

    @Test
    void 落地页载荷组装区块侧栏配置与外链() {
        CodePackageItem item = buildItem("1234567890");
        item.setTraceTemplate("tpl_landing");
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(item);

        TraceTemplate tpl = landingTpl();
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tpl);
        when(configReader.renderModeOf(tpl.getConfigJson())).thenReturn("landing");
        when(landingContentService.resolveForLanding(tpl, 1L)).thenReturn(
                new java.util.ArrayList<>(List.of(new HashMap<>(Map.of("key", "richtext", "content", "<p>正文</p>")))));
        when(configReader.sideMenuOf(tpl.getConfigJson())).thenReturn(new HashMap<>(Map.of("enabled", true)));
        when(configReader.gatesOf(tpl.getConfigJson())).thenReturn(Map.of());
        when(sideLinkService.resolveForConsumer(1L)).thenReturn(List.of());

        Map<String, Object> result = service.queryLanding("00000004", null);

        assertEquals(true, result.get("isLanding"));
        assertEquals("landing", result.get("renderMode"));
        assertEquals("tpl_landing", result.get("templateKey"));
        assertEquals(1, ((List<?>) result.get("blocks")).size());
        // 填过 richtext 就不算空页，否则前端会把有内容的页 302 掉
        assertEquals(false, result.get("contentEmpty"));
        assertEquals("/trace/00000004", result.get("traceUrl"));
    }

    @Test
    void 只有防伪区块时判定为内容全空() {
        CodePackageItem item = buildItem("1234567890");
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(item);

        TraceTemplate tpl = landingTpl();
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tpl);
        when(configReader.renderModeOf(tpl.getConfigJson())).thenReturn("landing");
        // antiFake 由系统实时供给、从不写内容行：把它算进"填过东西"就会错误地 302（spec §12）
        when(landingContentService.resolveForLanding(tpl, 1L)).thenReturn(
                List.of(new HashMap<>(Map.of("key", "antiFake", "enabled", true))));
        when(configReader.sideMenuOf(tpl.getConfigJson())).thenReturn(Map.of("enabled", false));
        when(configReader.gatesOf(tpl.getConfigJson())).thenReturn(Map.of());
        when(sideLinkService.resolveForConsumer(1L)).thenReturn(List.of());

        Map<String, Object> result = service.queryLanding("00000004", null);

        assertEquals(true, result.get("contentEmpty"));
    }

    @Test
    void 非落地页模板不查内容与外链() {
        CodePackageItem item = buildItem("1234567890");
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(item);

        TraceTemplate tpl = new TraceTemplate();
        tpl.setId(14L);
        tpl.setTemplateKey("tpl_1786948839317");
        tpl.setConfigJson("{\"renderMode\":\"visual\",\"pages\":[]}");
        when(templateMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(tpl);
        when(configReader.renderModeOf(tpl.getConfigJson())).thenReturn("visual");

        Map<String, Object> result = service.queryLanding("00000001", null);

        assertEquals(false, result.get("isLanding"));
        assertEquals("/trace/00000001", result.get("traceUrl"));
        assertNull(result.get("blocks"), "非落地页不该带回区块与外链：前端只看 isLanding 就会 302");
        verifyNoInteractions(landingContentService);
        verifyNoInteractions(sideLinkService);
    }

    @Test
    void 落地页接口对未绑定的码抛业务异常() {
        when(codePackageItemMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException e = assertThrows(BusinessException.class, () -> service.queryLanding("00099999", null));
        assertTrue(e.getMessage().contains("尚未激活绑定"), e.getMessage());
    }
}
