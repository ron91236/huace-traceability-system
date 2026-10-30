package com.huace.trace.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.TraceLandingContent;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.TraceLandingContentMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 落地页内容读写规则。与长图内容同构，所以钉的是同一组性质：
 * 1. 区块清单来自模板骨架：骨架里有但没填的在企业端要出现（空内容），骨架外的历史行要忽略
 * 2. 消费端只拿"启用的且有内容的"块；antiFake 例外，它没有内容但默认要出现
 * 3. 保存空内容 = 删行（"企业未填"就是无行）
 * 4. 定位查询同时锁 (template_id, enterprise_id, block_key)，跨企业写不进去
 * 5. antiFake 只接受 1/0 开关值（0=企业不展示），骨架外的 blockKey 拒写
 * 6. promoImages 必须是 URL 数组，裸字符串挡在库外，别让消费者端每次渲染都去猜
 */
@ExtendWith(MockitoExtension.class)
class LandingContentServiceTest {

    @Mock private TraceLandingContentMapper contentMapper;
    @Mock private CacheManager cacheManager;

    private LandingContentService service;

    private static final Long TEMPLATE_ID = 30L;
    private static final Long ENTERPRISE_ID = 8L;

    /** 三个区块都在骨架里且都启用，其中 antiFake 不该有内容 */
    private static final String CONFIG =
            "{\"renderMode\":\"landing\",\"landingBlocks\":["
            + "{\"key\":\"richtext\",\"name\":\"内容\",\"sortOrder\":0,\"enabled\":true},"
            + "{\"key\":\"promoImages\",\"name\":\"宣传图片\",\"sortOrder\":1,\"enabled\":true},"
            + "{\"key\":\"antiFake\",\"name\":\"防伪查询\",\"sortOrder\":2,\"enabled\":true}]}";

    /**
     * LambdaQueryWrapper 渲染 SQL 片段时要查实体的列缓存，纯 Mockito 环境不装就抛
     * "can not find lambda cache for this entity"。
     */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TraceLandingContent.class);
    }

    @BeforeEach
    void setUp() {
        // 必须在 @BeforeEach 里构造：字段初始化器跑在 @Mock 注入之前会拿到 null mapper。
        // ObjectMapper / TemplateConfigReader 都要真实实例，mock 会让解析类用例假通过。
        service = new LandingContentService(contentMapper, new ObjectMapper(), cacheManager,
                new TemplateConfigReader(new ObjectMapper()));
    }

    private TraceTemplate template(String configJson) {
        TraceTemplate t = new TraceTemplate();
        t.setId(TEMPLATE_ID);
        t.setTemplateKey("tpl_landing");
        t.setConfigJson(configJson);
        return t;
    }

    private TraceLandingContent aRow(long id, String blockKey, String content) {
        TraceLandingContent c = new TraceLandingContent();
        c.setId(id);
        c.setTemplateId(TEMPLATE_ID);
        c.setEnterpriseId(ENTERPRISE_ID);
        c.setBlockKey(blockKey);
        c.setContent(content);
        return c;
    }

    // ==================== 企业端列表 ====================

    @Test
    void 列表按骨架顺序输出并把未填的区块留空() {
        when(contentMapper.selectList(any())).thenReturn(List.of(aRow(3L, "richtext", "<p>正文</p>")));

        List<Map<String, Object>> list = service.list(template(CONFIG), ENTERPRISE_ID);

        assertEquals(List.of("richtext", "promoImages", "antiFake"),
                list.stream().map(m -> m.get("key")).toList());
        assertEquals("<p>正文</p>", list.get(0).get("content"));
        assertEquals("", list.get(1).get("content"));
        // antiFake 没有"内容"，企业能改的只有展示与否；没勾过就是默认的 1（展示）
        assertEquals("1", list.get(2).get("content"));
    }

    @Test
    void 企业勾掉防伪后清单里带的是0() {
        when(contentMapper.selectList(any())).thenReturn(List.of(aRow(4L, "antiFake", "0")));

        List<Map<String, Object>> list = service.list(template(CONFIG), ENTERPRISE_ID);

        assertEquals("0", list.get(2).get("content"));
    }

    @Test
    void 骨架之外的历史行被忽略() {
        when(contentMapper.selectList(any())).thenReturn(List.of(
                aRow(3L, "richtext", "<p>正文</p>"),
                aRow(9L, "video", "https://legacy.example.com/v.mp4")));

        List<Map<String, Object>> list = service.list(template(CONFIG), ENTERPRISE_ID);

        assertTrue(list.stream().noneMatch(m -> "video".equals(m.get("key"))), list.toString());
        assertEquals(3, list.size());
    }

    // ==================== 消费端取数 ====================

    @Test
    void 消费端只输出已填内容的区块并始终带上防伪块() {
        when(contentMapper.selectList(any())).thenReturn(List.of(aRow(3L, "richtext", "<p>正文</p>")));

        List<Map<String, Object>> blocks = service.resolveForLanding(template(CONFIG), ENTERPRISE_ID);

        // promoImages 没有行 = 企业没填；antiFake 永远没有内容，但必须出现，否则消费者页没有防伪入口
        assertEquals(List.of("richtext", "antiFake"), blocks.stream().map(m -> m.get("key")).toList());
        assertEquals(Boolean.TRUE, blocks.get(1).get("enabled"));
        assertNull(blocks.get(1).get("content"), "防伪区块的内容由系统供给，载荷里必须是 null 而不是空串");
    }

    @Test
    void 企业勾掉防伪后消费端不再带这块() {
        when(contentMapper.selectList(any())).thenReturn(List.of(
                aRow(3L, "richtext", "<p>正文</p>"),
                aRow(4L, "antiFake", "0")));

        List<Map<String, Object>> blocks = service.resolveForLanding(template(CONFIG), ENTERPRISE_ID);

        // "0" 是企业的显式选择，不能沿用"antiFake 一定出现"的旧规则，否则取消勾选没有效果
        assertEquals(List.of("richtext"), blocks.stream().map(m -> m.get("key")).toList());
    }

    @Test
    void 防伪开关写成1时照常出现() {
        when(contentMapper.selectList(any())).thenReturn(List.of(aRow(4L, "antiFake", "1")));

        List<Map<String, Object>> blocks = service.resolveForLanding(template(CONFIG), ENTERPRISE_ID);

        assertEquals(List.of("antiFake"), blocks.stream().map(m -> m.get("key")).toList());
        assertNull(blocks.get(0).get("content"), "开关值只决定出不出现，载荷里的内容仍然由系统供给");
    }

    @Test
    void 清空宣传图片等于删行而不是留一行空数组() {
        // "未填"只该有一种表示。留一行 "[]" 会让同一个状态在取数、企业端列表和
        // 消费者页三处各自判一次，漏一处就是一个渲染不出东西的空块
        when(contentMapper.selectOne(any())).thenReturn(aRow(4L, "promoImages", "[\"/uploads/a.png\"]"));

        service.saveContent(template(CONFIG), "promoImages", "[]", ENTERPRISE_ID);

        verify(contentMapper).deleteById(4L);
        verify(contentMapper, never()).insert(any());
        verify(contentMapper, never()).updateById(any());
    }

    @Test
    void 未启用的区块不进消费端载荷() {
        when(contentMapper.selectList(any())).thenReturn(List.of(aRow(3L, "richtext", "<p>正文</p>")));

        List<Map<String, Object>> blocks = service.resolveForLanding(
                template("{\"renderMode\":\"landing\",\"landingBlocks\":["
                        + "{\"key\":\"richtext\",\"name\":\"内容\",\"sortOrder\":0,\"enabled\":false},"
                        + "{\"key\":\"antiFake\",\"name\":\"防伪查询\",\"sortOrder\":1,\"enabled\":true}]}"),
                ENTERPRISE_ID);

        // 管理端关掉一块就是不想展示，不是"空着展示"
        assertEquals(List.of("antiFake"), blocks.stream().map(m -> m.get("key")).toList());
    }

    @Test
    void 企业为空时消费端不查库() {
        assertTrue(service.resolveForLanding(template(CONFIG), null).isEmpty());
        verifyNoInteractions(contentMapper);
    }

    // ==================== 写入 ====================

    @Test
    void 富文本新建时带模板企业blockKey与内容() {
        when(contentMapper.selectOne(any())).thenReturn(null);

        service.saveContent(template(CONFIG), "richtext", "<p>新内容</p>", ENTERPRISE_ID);

        ArgumentCaptor<TraceLandingContent> captor = ArgumentCaptor.forClass(TraceLandingContent.class);
        verify(contentMapper).insert(captor.capture());
        assertEquals(TEMPLATE_ID, captor.getValue().getTemplateId());
        assertEquals(ENTERPRISE_ID, captor.getValue().getEnterpriseId());
        assertEquals("richtext", captor.getValue().getBlockKey());
        assertEquals("<p>新内容</p>", captor.getValue().getContent());
    }

    @Test
    void 写入定位同时锁模板企业与blockKey() {
        when(contentMapper.selectOne(any())).thenReturn(null);

        service.saveContent(template(CONFIG), "richtext", "<p>x</p>", ENTERPRISE_ID);

        ArgumentCaptor<LambdaQueryWrapper<TraceLandingContent>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(contentMapper).selectOne(captor.capture());
        String sql = captor.getValue().getSqlSegment().replaceAll("\\s+", "");
        Map<String, Object> params = captor.getValue().getParamNameValuePairs();
        assertTrue(sql.contains("template_id=") && sql.contains("enterprise_id=") && sql.contains("block_key="), sql);
        assertTrue(params.containsValue(TEMPLATE_ID), params.toString());
        assertTrue(params.containsValue(ENTERPRISE_ID), params.toString());
        assertTrue(params.containsValue("richtext"), params.toString());
    }

    @Test
    void 已有行走updateById() {
        when(contentMapper.selectOne(any())).thenReturn(aRow(3L, "richtext", "<p>旧</p>"));

        service.saveContent(template(CONFIG), "richtext", "<p>新</p>", ENTERPRISE_ID);

        ArgumentCaptor<TraceLandingContent> captor = ArgumentCaptor.forClass(TraceLandingContent.class);
        verify(contentMapper).updateById(captor.capture());
        verify(contentMapper, never()).insert(any());
        assertEquals(3L, captor.getValue().getId());
        assertEquals("<p>新</p>", captor.getValue().getContent());
    }

    @Test
    void 保存空内容删除已有行() {
        when(contentMapper.selectOne(any())).thenReturn(aRow(3L, "richtext", "<p>旧</p>"));

        service.saveContent(template(CONFIG), "richtext", "   ", ENTERPRISE_ID);

        verify(contentMapper).deleteById(3L);
        verify(contentMapper, never()).updateById(any());
    }

    @Test
    void 保存空内容且原本无行时不写库() {
        when(contentMapper.selectOne(any())).thenReturn(null);

        service.saveContent(template(CONFIG), "richtext", null, ENTERPRISE_ID);

        verify(contentMapper, never()).insert(any());
        verify(contentMapper, never()).updateById(any());
        verify(contentMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void 防伪区块只接受1和0两种开关值() {
        // 防伪结论由系统按码实时算，企业能改的只有"出不出现"；写别的值就是库里一个没人认的开关
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveContent(template(CONFIG), "antiFake", "随便", ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("防伪"), e.getMessage());
        verifyNoInteractions(contentMapper);
    }

    @Test
    void 取消勾选防伪落库是一行0() {
        when(contentMapper.selectOne(any())).thenReturn(null);

        service.saveContent(template(CONFIG), "antiFake", "0", ENTERPRISE_ID);

        ArgumentCaptor<TraceLandingContent> captor = ArgumentCaptor.forClass(TraceLandingContent.class);
        verify(contentMapper).insert(captor.capture());
        assertEquals("antiFake", captor.getValue().getBlockKey());
        assertEquals("0", captor.getValue().getContent());
    }

    @Test
    void 重新勾选防伪走删行回到默认展示() {
        when(contentMapper.selectOne(any())).thenReturn(aRow(4L, "antiFake", "0"));

        // 前端勾回来提交的是 "1"；而清空（空串）按本仓库统一语义是删行，两者效果一样是展示
        service.saveContent(template(CONFIG), "antiFake", "", ENTERPRISE_ID);

        verify(contentMapper).deleteById(4L);
        verify(contentMapper, never()).insert(any());
    }

    @Test
    void 骨架里没有的blockKey被拒() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveContent(template(CONFIG), "video", "https://a.mp4", ENTERPRISE_ID));
        assertEquals("区块不存在", e.getMessage());
        verifyNoInteractions(contentMapper);
    }

    @Test
    void 宣传图片是URL数组时正常写入() {
        when(contentMapper.selectOne(any())).thenReturn(null);

        service.saveContent(template(CONFIG), "promoImages",
                "[\"/uploads/a.png\",\"/uploads/b.png\"]", ENTERPRISE_ID);

        verify(contentMapper).insert(any());
    }

    @Test
    void 宣传图片不是数组时拒绝() {
        // 裸字符串混进库后，消费者端每次渲染都要猜它是数组还是单值，猜错就是整块空白
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveContent(template(CONFIG), "promoImages", "https://a.mp4", ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("图片"), e.getMessage());
        verifyNoInteractions(contentMapper);
    }

    @Test
    void 宣传图片数组里的空项被清掉() {
        when(contentMapper.selectOne(any())).thenReturn(null);

        service.saveContent(template(CONFIG), "promoImages",
                "[\"/uploads/a.png\",\"\",null,\"  \"]", ENTERPRISE_ID);

        ArgumentCaptor<TraceLandingContent> captor = ArgumentCaptor.forClass(TraceLandingContent.class);
        verify(contentMapper).insert(captor.capture());
        assertEquals("[\"/uploads/a.png\"]", captor.getValue().getContent());
    }

    @Test
    void 内容变更后清掉溯源页缓存() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("tracePage")).thenReturn(cache);
        when(contentMapper.selectOne(any())).thenReturn(null);

        service.saveContent(template(CONFIG), "richtext", "<p>x</p>", ENTERPRISE_ID);

        verify(cache).clear();
    }

    @Test
    void 超长内容被拒() {
        // 不打 selectOne 的桩：长度校验排在摸库之前，多一次查询这条就会红
        String big = "<p>" + "字".repeat(LandingContentService.CONTENT_MAX) + "</p>";

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.saveContent(template(CONFIG), "richtext", big, ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("内容过长"), e.getMessage());
        verifyNoInteractions(contentMapper);
    }
}
