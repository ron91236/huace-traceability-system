package com.huace.trace.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.TraceLongModule;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.TraceLongModuleMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 长图内容读写规则的回归测试。P2 把所有权反转给模板，所以钉住的新形状是：
 * 1. 模块清单来自模板骨架，不是来自表里的行：骨架里有但企业没填的模块要出现（空 pages），
 *    骨架里没有但表里还有的行（管理端删过模块留下的孤儿）要忽略
 * 2. 消费端跳过空模块，不给消费者留点不开的空胶囊
 * 3. 写入按 (template_id, enterprise_id, module_key) 定位，跨企业写不进去
 * 4. 保存空页图 = 删行（"企业未填"就是无行），原本无行时不碰库
 * 5. 更新只 SET pages，绝不把 name/sort_order/status 写回去——那是第二份真相
 * 6. PDF 转换的前置校验一条都不放松（这扇门有登录态，不代表路径可信）
 */
@ExtendWith(MockitoExtension.class)
class LongModuleServiceTest {

    @Mock private TraceLongModuleMapper moduleMapper;
    @Mock private PdfConvertService pdfConvertService;
    @Mock private CacheManager cacheManager;

    @TempDir Path tmp;

    private LongModuleService service;

    private static final Long TEMPLATE_ID = 20L;
    private static final Long ENTERPRISE_ID = 23L;

    /** 骨架两个模块：m2 排在前（sortOrder 0），m1 排在后 */
    private static final String CONFIG_TWO =
            "{\"renderMode\":\"long-image\",\"longModules\":["
            + "{\"key\":\"m2\",\"name\":\"产品信息\",\"sortOrder\":0},"
            + "{\"key\":\"m1\",\"name\":\"有机种植\",\"sortOrder\":1}]}";

    /**
     * LambdaQueryWrapper/LambdaUpdateWrapper 渲染 SQL 片段时要查实体的列缓存，而缓存只在
     * MyBatis 启动扫描实体时装好；这里是纯 Mockito 环境，不装就抛
     * "can not find lambda cache for this entity"。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TraceLongModule.class);
    }

    @BeforeEach
    void setUp() {
        // 必须手工构造：@InjectMocks 会把 objectMapper 注入成 mock，mock 的 readValue 返回 null，
        // 会让 parsePages 永远返回空列表、测试假绿。参数顺序与 @RequiredArgsConstructor 字段顺序一致。
        service = new LongModuleService(moduleMapper, new ObjectMapper(), pdfConvertService, cacheManager,
                new TemplateConfigReader(new ObjectMapper()));
    }

    private TraceTemplate template(String configJson) {
        TraceTemplate t = new TraceTemplate();
        t.setId(TEMPLATE_ID);
        t.setTemplateKey("long-image");
        t.setConfigJson(configJson);
        return t;
    }

    private TraceLongModule aRow(long id, String moduleKey, String pages) {
        TraceLongModule m = new TraceLongModule();
        m.setId(id);
        m.setTemplateId(TEMPLATE_ID);
        m.setEnterpriseId(ENTERPRISE_ID);
        m.setModuleKey(moduleKey);
        m.setPages(pages);
        return m;
    }

    /** 抹平空白，避免断言绑死 MyBatis-Plus 渲染出的空格格式 */
    private static String flat(String sql) {
        return sql.replaceAll("\\s+", "");
    }

    // ==================== 企业端列表 ====================

    @Test
    void 列表按骨架顺序输出并把未填的模块留空() {
        // 只存了 m1；m2 是企业还没填的那个，必须照样出现，否则企业看不到"该填但没填"那一栏
        when(moduleMapper.selectList(any())).thenReturn(List.of(
                aRow(9L, "m1", "[\"/uploads/p1.png\",\"/uploads/p2.png\"]")));

        List<Map<String, Object>> list = service.list(template(CONFIG_TWO), ENTERPRISE_ID);

        assertEquals(2, list.size());
        assertEquals("m2", list.get(0).get("key"));
        assertEquals("产品信息", list.get(0).get("name"));
        assertEquals(List.of(), list.get(0).get("pages"));
        assertEquals("m1", list.get(1).get("key"));
        assertEquals(2, ((List<?>) list.get(1).get("pages")).size());
    }

    @Test
    void 骨架之外的历史行被忽略() {
        // 管理端删掉模块后企业内容不会级联删除（spec §12），取数时必须按骨架过滤，
        // 否则一个已经不在导航里的模块会一直挂在企业页面上
        when(moduleMapper.selectList(any())).thenReturn(List.of(
                aRow(9L, "m1", "[\"/uploads/p1.png\"]"),
                aRow(10L, "m9", "[\"/uploads/orphan.png\"]")));

        List<Map<String, Object>> list = service.list(template(CONFIG_TWO), ENTERPRISE_ID);

        assertTrue(list.stream().noneMatch(m -> "m9".equals(m.get("key"))), list.toString());
        assertEquals(2, list.size());
    }

    // ==================== 消费端取数 ====================

    @Test
    void 消费端按骨架顺序输出企业已填的模块() {
        when(moduleMapper.selectList(any())).thenReturn(List.of(
                aRow(9L, "m1", "[\"/uploads/p1.png\"]"),
                aRow(10L, "m2", "[\"/uploads/p2.png\"]")));

        List<Map<String, Object>> result = service.resolveForTrace(template(CONFIG_TWO), ENTERPRISE_ID);

        // 骨架里 m2 的 sortOrder 是 0，所以 m2 在前，与表里的 id 顺序无关
        assertEquals(List.of("m2", "m1"), result.stream().map(m -> m.get("key")).toList());
        assertEquals("产品信息", result.get(0).get("name"));
    }

    @Test
    void 消费端跳过企业未填的模块() {
        when(moduleMapper.selectList(any())).thenReturn(List.of(
                aRow(9L, "m1", "[]"),
                aRow(10L, "m2", "[\"/uploads/p2.png\"]")));

        List<Map<String, Object>> result = service.resolveForTrace(template(CONFIG_TWO), ENTERPRISE_ID);

        assertEquals(1, result.size());
        assertEquals("m2", result.get(0).get("key"));
    }

    @Test
    void 模板骨架为空时消费端返回空() {
        when(moduleMapper.selectList(any())).thenReturn(List.of(
                aRow(9L, "m1", "[\"/uploads/p1.png\"]")));

        assertTrue(service.resolveForTrace(
                template("{\"renderMode\":\"long-image\",\"longModules\":[]}"), ENTERPRISE_ID).isEmpty());
    }

    @Test
    void 企业为空时不查库直接返回空() {
        assertTrue(service.resolveForTrace(template(CONFIG_TWO), null).isEmpty());
        verifyNoInteractions(moduleMapper);
    }

    @Test
    void pages脏数据按无内容跳过而不是让整页取数失败() {
        when(moduleMapper.selectList(any())).thenReturn(List.of(
                aRow(9L, "m1", "不是JSON"),
                aRow(10L, "m2", "[\"/uploads/p2.png\"]")));

        List<Map<String, Object>> result = service.resolveForTrace(template(CONFIG_TWO), ENTERPRISE_ID);

        assertEquals(1, result.size());
        assertEquals("m2", result.get(0).get("key"));
    }

    // ==================== 写入 ====================

    @Test
    void 新行只带模板企业key与页图() {
        when(moduleMapper.selectOne(any())).thenReturn(null);

        service.savePages(template(CONFIG_TWO), "m1", List.of("/uploads/a.png"), ENTERPRISE_ID);

        ArgumentCaptor<TraceLongModule> captor = ArgumentCaptor.forClass(TraceLongModule.class);
        verify(moduleMapper).insert(captor.capture());
        TraceLongModule row = captor.getValue();
        assertEquals(TEMPLATE_ID, row.getTemplateId());
        assertEquals(ENTERPRISE_ID, row.getEnterpriseId());
        assertEquals("m1", row.getModuleKey());
        assertEquals("[\"/uploads/a.png\"]", row.getPages());
        // 这三个是模板的字段。写回去就等于在表里存第二份模块名，管理端改名后两边会分叉。
        assertNull(row.getName(), "名称归模板骨架，行里不该写");
        assertNull(row.getSortOrder());
        assertNull(row.getStatus());
    }

    @Test
    void 已有行更新只SET页图() {
        when(moduleMapper.selectOne(any())).thenReturn(aRow(9L, "m1", "[\"/uploads/old.png\"]"));

        service.savePages(template(CONFIG_TWO), "m1", List.of("/uploads/new.png"), ENTERPRISE_ID);

        ArgumentCaptor<LambdaUpdateWrapper<TraceLongModule>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(moduleMapper).update(isNull(), captor.capture());
        verify(moduleMapper, never()).insert(any());
        String set = flat(captor.getValue().getSqlSet());
        assertTrue(set.contains("pages="), set);
        // 一次拖拽页图顺序不该顺带把模块改名或停用
        assertFalse(set.contains("name="), set);
        assertFalse(set.contains("sort_order="), set);
        assertFalse(set.contains("status="), set);
    }

    @Test
    void 写入定位同时锁模板企业与moduleKey() {
        // 只按 key 查的话，A 企业能覆盖 B 企业同模板同 key 的行；模板维度也必须带上，
        // 否则同一企业被开放两个长图模板时两边会互相踩
        when(moduleMapper.selectOne(any())).thenReturn(null);

        service.savePages(template(CONFIG_TWO), "m1", List.of("/uploads/a.png"), ENTERPRISE_ID);

        ArgumentCaptor<LambdaQueryWrapper<TraceLongModule>> captor =
                ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(moduleMapper).selectOne(captor.capture());
        String sql = flat(captor.getValue().getSqlSegment());
        Map<String, Object> params = captor.getValue().getParamNameValuePairs();
        assertTrue(sql.contains("template_id=") && sql.contains("enterprise_id=")
                && sql.contains("module_key="), sql);
        assertTrue(params.containsValue(TEMPLATE_ID), params.toString());
        assertTrue(params.containsValue(ENTERPRISE_ID), params.toString());
        assertTrue(params.containsValue("m1"), params.toString());
    }

    @Test
    void 保存空页图删除已有行() {
        // spec §12：「某模块企业未填」= 表里无行。写空数组进去会让下一次读取又看到一个空模块，
        // 企业端也永远没法真的把内容撤掉
        when(moduleMapper.selectOne(any())).thenReturn(aRow(9L, "m1", "[\"/uploads/a.png\"]"));

        service.savePages(template(CONFIG_TWO), "m1", List.of(), ENTERPRISE_ID);

        verify(moduleMapper).deleteById(9L);
        verify(moduleMapper, never()).insert(any());
        verify(moduleMapper, never()).update(any(), any());
    }

    @Test
    void 保存空页图且原本无行时不写库() {
        when(moduleMapper.selectOne(any())).thenReturn(null);

        service.savePages(template(CONFIG_TWO), "m1", null, ENTERPRISE_ID);

        verify(moduleMapper, never()).insert(any());
        verify(moduleMapper, never()).update(any(), any());
        verify(moduleMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void 骨架里没有的key被拒() {
        // 前端传一个管理端没定义过的 key，就会写出一行永远取不出来的孤儿数据
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.savePages(template(CONFIG_TWO), "mX", List.of("/uploads/a.png"), ENTERPRISE_ID));
        assertEquals("模块不存在", e.getMessage());
        verifyNoInteractions(moduleMapper);
    }

    @Test
    void 超过30页被拒绝() {
        // 不打 selectOne 的桩、末尾 verifyNoInteractions：页数上限必须在摸库之前就挡住。
        // 谁把顺序写成"先定位行再判长度"，这条会因多余的 selectOne 调用直接红。
        List<String> pages = new ArrayList<>();
        for (int i = 0; i < 31; i++) pages.add("/uploads/p" + i + ".png");

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.savePages(template(CONFIG_TWO), "m1", pages, ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("30"), e.getMessage());
        verifyNoInteractions(moduleMapper);
    }

    @Test
    void 空串页图条目被过滤后只剩有效页() {
        when(moduleMapper.selectOne(any())).thenReturn(null);

        service.savePages(template(CONFIG_TWO), "m1",
                Arrays.asList("/uploads/a.png", "", null, "  "), ENTERPRISE_ID);

        ArgumentCaptor<TraceLongModule> captor = ArgumentCaptor.forClass(TraceLongModule.class);
        verify(moduleMapper).insert(captor.capture());
        assertEquals("[\"/uploads/a.png\"]", captor.getValue().getPages());
    }

    @Test
    void 模块变更后溯源页缓存被清空() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("tracePage")).thenReturn(cache);
        when(moduleMapper.selectOne(any())).thenReturn(null);

        service.savePages(template(CONFIG_TWO), "m1", List.of("/uploads/a.png"), ENTERPRISE_ID);

        verify(cache).clear();
    }

    // ==================== PDF 转换（与 P1 同规则，一条都不松） ====================

    @Test
    void 拒绝转换非本系统文件() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdf("https://evil.example.com/x.pdf"));
        assertTrue(e.getMessage().contains("本系统"), e.getMessage());
        verifyNoInteractions(pdfConvertService);
    }

    @Test
    void 拒绝转换不存在的文件() {
        service.setUploadDir(tmp.toString());
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdf("/uploads/2026/no-such.pdf"));
        assertEquals("文件不存在或不合法，请重新上传", e.getMessage());
        verifyNoInteractions(pdfConvertService);
    }

    @Test
    void 拒绝路径穿越() {
        service.setUploadDir(tmp.toString());
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdf("/uploads/../../etc/passwd"));
        assertEquals("文件不存在或不合法，请重新上传", e.getMessage());
        verifyNoInteractions(pdfConvertService);
    }

    @Test
    void 目录不能被转换() throws Exception {
        // Files.exists 对目录同样为真，而 Files.size(目录) 是个小数字，
        // 于是上传目录里的子目录能一路通过前置校验混到转换环节，最后换成 HTTP 200 + 空数组。
        service.setUploadDir(tmp.toString());
        java.nio.file.Files.createDirectories(tmp.resolve("2026"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdf("/uploads/2026"));
        assertEquals("文件不存在或不合法，请重新上传", e.getMessage());
        verifyNoInteractions(pdfConvertService);
    }

    @Test
    void 非法字符路径按不存在拒绝而不是500() {
        // InvalidPathException 不是 IOException，漏接就会被全局兜底换成
        // "系统内部错误: <用户输入>"回显给对方，而免登录那扇门对同样的输入返回空。
        service.setUploadDir(tmp.toString());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdf("/uploads/a\0b.pdf"));
        assertEquals("文件不存在或不合法，请重新上传", e.getMessage());
        verifyNoInteractions(pdfConvertService);
    }

    @Test
    void 无法解析的PDF要报错() throws Exception {
        service.setUploadDir(tmp.toString());
        java.nio.file.Files.createDirectories(tmp.resolve("2026"));
        java.nio.file.Files.write(tmp.resolve("2026/broken.pdf"), new byte[]{1, 2, 3});
        when(pdfConvertService.convertPdfToImages(anyString(), eq(30))).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdf("/uploads/2026/broken.pdf"));
        assertTrue(e.getMessage().contains("无法解析"), e.getMessage());
    }

    @Test
    void 拒绝超过20MB的PDF() throws Exception {
        service.setUploadDir(tmp.toString());
        java.nio.file.Files.createDirectories(tmp.resolve("2026"));
        java.nio.file.Files.write(tmp.resolve("2026/big.pdf"), new byte[21 * 1024 * 1024]);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.convertPdf("/uploads/2026/big.pdf"));
        assertTrue(e.getMessage().contains("20MB"), e.getMessage());
        verifyNoInteractions(pdfConvertService);
    }

    @Test
    void 合规PDF按上限转换() throws Exception {
        service.setUploadDir(tmp.toString());
        java.nio.file.Files.createDirectories(tmp.resolve("2026"));
        java.nio.file.Files.write(tmp.resolve("2026/ok.pdf"), new byte[]{1, 2, 3});
        when(pdfConvertService.convertPdfToImages("/uploads/2026/ok.pdf", 30))
                .thenReturn(List.of("/uploads/pdf-images/k/k_p1.jpg"));

        assertEquals(List.of("/uploads/pdf-images/k/k_p1.jpg"),
                service.convertPdf("/uploads/2026/ok.pdf"));
    }
}
