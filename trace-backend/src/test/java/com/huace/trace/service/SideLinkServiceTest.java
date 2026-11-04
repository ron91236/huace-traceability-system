package com.huace.trace.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.EnterpriseSideLink;
import com.huace.trace.mapper.EnterpriseSideLinkMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 企业侧边栏外链的规则。它是企业级资产、与模板无关，所以钉的是：
 * 1. url 与 image 至少一项（否则按钮点了什么也不会发生，"跳转链接优先"的前提就没了）
 * 2. name 必填且不超 50 字（VARCHAR(50)，超长要么报错要么被截断，两种都糟）
 * 3. 企业归属只认登录态：请求体里的 enterpriseId 必须被无条件冲掉
 * 4. 跨企业改/删别人的行被拒；排序列表为空或重复都整体拒绝，不做部分生效
 * 5. 消费者只拿 status=1 的项且按 sort_order 升序
 * 6. 更新与排序都不能顺带把 status 写成 null（那等于凭空停用按钮，而接口照样 200）
 * 7. 更新逐列显式 SET：updateById 的 NOT_NULL 策略会跳过 null，"清空图标/清空跳转地址"就永远不生效
 * 8. 资料（image）是 JSON 数组：写入归一、条目上限、只认 /uploads/、PDF 保存时预热页图并卡总页数
 * 9. 消费者侧把 PDF 展开成页图填进瞬态 pages，转不动的那条跳过而不是整页报错；企业端列表不填 pages
 */
@ExtendWith(MockitoExtension.class)
class SideLinkServiceTest {

    @Mock private EnterpriseSideLinkMapper linkMapper;
    @Mock private PdfConvertService pdfConvertService;

    private SideLinkService service;

    private static final Long ENTERPRISE_ID = 8L;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), EnterpriseSideLink.class);
    }

    @BeforeEach
    void setUp() {
        // ObjectMapper 用真实实例：mock 会让归一化用例假通过
        service = new SideLinkService(linkMapper, new ObjectMapper(), pdfConvertService);
    }

    private EnterpriseSideLink aLink(long id, String url, String image, int status) {
        EnterpriseSideLink l = new EnterpriseSideLink();
        l.setId(id);
        l.setEnterpriseId(ENTERPRISE_ID);
        l.setName("公众号");
        l.setUrl(url);
        l.setImage(image);
        l.setSortOrder(0);
        l.setStatus(status);
        return l;
    }

    @Test
    void 新建时企业归属取登录态而不是请求体() {
        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("官方微博");
        form.setUrl("https://weibo.com/x");
        // 塞一个别人的企业 id 也必须被冲掉：否则"归属由登录态决定"只是"请求体没填才补"
        form.setEnterpriseId(999L);
        when(linkMapper.selectCount(any())).thenReturn(2L);

        service.create(form, ENTERPRISE_ID);

        assertEquals(ENTERPRISE_ID, form.getEnterpriseId());
        assertEquals(2, form.getSortOrder());
        assertEquals(1, form.getStatus());
        verify(linkMapper).insert(form);
    }

    @Test
    void url与image都为空时被拒() {
        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("什么都没有");

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(form, ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("至少填一项"), e.getMessage());
        verify(linkMapper, never()).insert(any());
    }

    @Test
    void 只有二维码图片是合法的() {
        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("公众号");
        form.setImage("/uploads/qr.png");
        when(linkMapper.selectCount(any())).thenReturn(0L);

        assertDoesNotThrow(() -> service.create(form, ENTERPRISE_ID));
        verify(linkMapper).insert(form);
        // 裸路径是线上现存数据的形态，写入时必须归一成数组，否则库里就有两种"一份资料"的表示
        assertEquals("[\"/uploads/qr.png\"]", form.getImage());
    }

    @Test
    void 非http协议的跳转链接被拒() {
        // 这个值最终是消费者点击时 window.location.href 的目标，
        // 存 javascript: 或 data: 等于把 XSS 写进库里，必须在写入这条边界上挡掉
        BusinessException js = assertThrows(BusinessException.class, () -> {
            EnterpriseSideLink f = new EnterpriseSideLink();
            f.setName("恶意");
            f.setUrl("javascript:alert(1)");
            service.create(f, ENTERPRISE_ID);
        });
        assertTrue(js.getMessage().contains("http"), js.getMessage());

        BusinessException scheme = assertThrows(BusinessException.class, () -> {
            EnterpriseSideLink f = new EnterpriseSideLink();
            f.setName("恶意");
            f.setUrl("data:text/html,<script>1</script>");
            service.create(f, ENTERPRISE_ID);
        });
        assertTrue(scheme.getMessage().contains("http"), scheme.getMessage());
        verify(linkMapper, never()).insert(any());
    }

    @Test
    void 名称为空或超长被拒() {
        BusinessException blank = assertThrows(BusinessException.class, () -> {
            EnterpriseSideLink f = new EnterpriseSideLink();
            f.setUrl("https://a");
            f.setName("   ");
            service.create(f, ENTERPRISE_ID);
        });
        assertTrue(blank.getMessage().contains("名称"), blank.getMessage());

        BusinessException longName = assertThrows(BusinessException.class, () -> {
            EnterpriseSideLink f = new EnterpriseSideLink();
            f.setUrl("https://a");
            f.setName("名".repeat(51));
            service.create(f, ENTERPRISE_ID);
        });
        assertTrue(longName.getMessage().contains("50"), longName.getMessage());
    }

    @Test
    void 更新只能动自己企业的行() {
        EnterpriseSideLink other = aLink(5L, "https://a", null, 1);
        other.setEnterpriseId(77L);
        when(linkMapper.selectById(5L)).thenReturn(other);

        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("改名");
        form.setUrl("https://b");

        BusinessException e = assertThrows(BusinessException.class, () -> service.update(5L, form, ENTERPRISE_ID));
        assertEquals("无权限操作", e.getMessage());
        verify(linkMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<EnterpriseSideLink> captureUpdate() {
        ArgumentCaptor<LambdaUpdateWrapper<EnterpriseSideLink>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(linkMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }

    private static String flat(String sql) {
        return sql.replaceAll("\\s+", "");
    }

    @Test
    void 更新不带status时保留原启停状态() {
        when(linkMapper.selectById(5L)).thenReturn(aLink(5L, "https://a", null, 0));

        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("改名");
        form.setUrl("https://b");
        form.setStatus(null);

        service.update(5L, form, ENTERPRISE_ID);

        LambdaUpdateWrapper<EnterpriseSideLink> w = captureUpdate();
        assertTrue(flat(w.getSqlSet()).contains("status="), w.getSqlSet());
        assertTrue(w.getParamNameValuePairs().containsValue(0),
                "status 不许被写成 null（那等于凭空停用按钮）：" + w.getParamNameValuePairs());
        assertFalse(flat(w.getSqlSet()).contains("sort_order="), "排序不由更新接口管：" + w.getSqlSet());
        assertTrue(flat(w.getSqlSegment()).toUpperCase().contains("ID="), w.getSqlSegment());
    }

    @Test
    void 清空图标与跳转地址要真的进SET() {
        // updateById 的 NOT_NULL 策略会跳过 null 字段，"清空"这类操作在它下面永远不生效，
        // 而接口照样回 200 —— 更新必须逐列显式 SET
        EnterpriseSideLink existing = aLink(5L, "https://a", "/uploads/old.png", 1);
        existing.setIcon("/uploads/old-icon.png");
        when(linkMapper.selectById(5L)).thenReturn(existing);

        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("只看二维码");
        form.setImage("/uploads/qr.png");

        service.update(5L, form, ENTERPRISE_ID);

        LambdaUpdateWrapper<EnterpriseSideLink> w = captureUpdate();
        String set = flat(w.getSqlSet());
        assertTrue(set.contains("url=") && set.contains("icon="), set);
        assertEquals(2, w.getParamNameValuePairs().values().stream().filter(java.util.Objects::isNull).count(),
                "url 与 icon 各一个 null 才会真的被清掉：" + w.getParamNameValuePairs());
    }

    @Test
    void 图标路径超过列宽被拒() {
        // icon 列是 VARCHAR(64)，超长在严格模式下是 "Data too long" 的 500，不如在写入前说清楚
        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("公众号");
        form.setUrl("https://a");
        form.setIcon("/uploads/" + "x".repeat(64));

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(form, ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("图标"), e.getMessage());
    }

    @Test
    void 图标为空串时落成NULL() {
        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("公众号");
        form.setUrl("https://a");
        form.setIcon("   ");

        service.create(form, ENTERPRISE_ID);

        ArgumentCaptor<EnterpriseSideLink> captor = ArgumentCaptor.forClass(EnterpriseSideLink.class);
        verify(linkMapper).insert(captor.capture());
        assertNull(captor.getValue().getIcon(), "空串与 null 两种「没有图标」会让前端判两次");
    }

    @Test
    void 排序列表为空时拒绝而不是静默成功() {
        BusinessException empty = assertThrows(BusinessException.class, () -> service.sort(List.of(), ENTERPRISE_ID));
        assertEquals("排序列表不能为空", empty.getMessage());
        BusinessException missing = assertThrows(BusinessException.class, () -> service.sort(null, ENTERPRISE_ID));
        assertEquals("排序列表不能为空", missing.getMessage());
        verifyNoInteractions(linkMapper);
    }

    @Test
    void 排序列表有重复时整体拒绝() {
        // 去重校验在任何查库之前就拦住，所以这里不需要 stub selectById
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.sort(List.of(1L, 1L), ENTERPRISE_ID));
        assertEquals("排序列表不能有重复链接", e.getMessage());
        verify(linkMapper, never()).updateById(any());
    }

    @Test
    void 排序遇到别企业的行整体拒绝() {
        when(linkMapper.selectById(1L)).thenReturn(aLink(1L, "https://a", null, 1));
        EnterpriseSideLink foreign = aLink(2L, "https://b", null, 1);
        foreign.setEnterpriseId(77L);
        when(linkMapper.selectById(2L)).thenReturn(foreign);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.sort(List.of(1L, 2L), ENTERPRISE_ID));
        assertEquals("无权限操作", e.getMessage());
        verify(linkMapper, never()).updateById(any());
    }

    @Test
    void 排序按传入顺序落库() {
        when(linkMapper.selectById(1L)).thenReturn(aLink(1L, "https://a", null, 1));
        when(linkMapper.selectById(2L)).thenReturn(aLink(2L, "https://b", null, 1));

        service.sort(List.of(2L, 1L), ENTERPRISE_ID);

        ArgumentCaptor<EnterpriseSideLink> captor = ArgumentCaptor.forClass(EnterpriseSideLink.class);
        verify(linkMapper, times(2)).updateById(captor.capture());
        assertEquals(2L, captor.getAllValues().get(0).getId());
        assertEquals(0, captor.getAllValues().get(0).getSortOrder());
        assertEquals(1L, captor.getAllValues().get(1).getId());
        assertEquals(1, captor.getAllValues().get(1).getSortOrder());
        // 排序补丁只带 sortOrder：url/status 不在 SET 里，拖一次顺序不会顺带改跳转目标或停用按钮
        assertNull(captor.getAllValues().get(0).getUrl());
        assertNull(captor.getAllValues().get(0).getStatus());
    }

    @Test
    void 消费者取数只查本企业启用项() {
        when(linkMapper.selectList(any())).thenReturn(List.of(aLink(1L, "https://a", null, 1)));

        List<EnterpriseSideLink> links = service.resolveForConsumer(ENTERPRISE_ID);

        assertEquals(1, links.size());
        ArgumentCaptor<LambdaQueryWrapper<EnterpriseSideLink>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(linkMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment().replaceAll("\\s+", "");
        Map<String, Object> params = captor.getValue().getParamNameValuePairs();
        assertTrue(sql.contains("enterprise_id=") && sql.contains("status="), sql);
        assertTrue(sql.contains("ORDERBYsort_orderASC"), sql);
        assertTrue(params.containsValue(ENTERPRISE_ID), params.toString());
        assertTrue(params.containsValue(1), "只取启用项: " + params);
        assertFalse(params.containsValue(null), "不该把 null 绑成参数: " + params);
    }

    @Test
    void 企业为空时消费者取数为空且不查库() {
        assertTrue(service.resolveForConsumer(null).isEmpty());
        verifyNoInteractions(linkMapper);
    }

    @Test
    void 列表含停用项且按顺序返回() {
        when(linkMapper.selectList(any())).thenReturn(List.of(aLink(1L, "https://a", null, 0)));

        assertEquals(1, service.list(ENTERPRISE_ID).size());

        ArgumentCaptor<LambdaQueryWrapper<EnterpriseSideLink>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(linkMapper).selectList(captor.capture());
        String sql = captor.getValue().getSqlSegment().replaceAll("\\s+", "");
        // 企业端要看得见自己关掉的按钮才能重新打开，所以这条查询不能带 status 过滤
        assertFalse(sql.contains("status="), sql);
    }

    @Test
    void 删除只作用于本企业的行() {
        when(linkMapper.selectById(9L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.delete(9L, ENTERPRISE_ID));
        verify(linkMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void 删除自己的行() {
        when(linkMapper.selectById(9L)).thenReturn(aLink(9L, "https://a", null, 1));

        service.delete(9L, ENTERPRISE_ID);

        verify(linkMapper).deleteById(9L);
    }

    // ---------- 跳转资料：数组语义 ----------

    private EnterpriseSideLink mediaForm(String image) {
        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("检测报告");
        form.setImage(image);
        return form;
    }

    private String insertedImage() {
        ArgumentCaptor<EnterpriseSideLink> captor = ArgumentCaptor.forClass(EnterpriseSideLink.class);
        verify(linkMapper).insert(captor.capture());
        return captor.getValue().getImage();
    }

    @Test
    void 资料数组按上传顺序落库() {
        when(linkMapper.selectCount(any())).thenReturn(0L);

        service.create(mediaForm("[\"/uploads/a.png\",\"/uploads/b.jpg\"]"), ENTERPRISE_ID);

        assertEquals("[\"/uploads/a.png\",\"/uploads/b.jpg\"]", insertedImage());
    }

    @Test
    void 资料里的空项被清掉且空数组落成NULL() {
        when(linkMapper.selectCount(any())).thenReturn(0L);

        service.create(mediaForm("[\"/uploads/a.png\",\"\",null]"), ENTERPRISE_ID);
        assertEquals("[\"/uploads/a.png\"]", insertedImage());

        // url 有值时资料清空是合法的，且不能留下 "[]" 这种"未填"的第二种表示
        EnterpriseSideLink onlyUrl = new EnterpriseSideLink();
        onlyUrl.setName("官微");
        onlyUrl.setUrl("https://weibo.com/x");
        onlyUrl.setImage("[]");
        service.create(onlyUrl, ENTERPRISE_ID);
        assertNull(onlyUrl.getImage());
    }

    @Test
    void url为空时资料必须非空() {
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(mediaForm("[]"), ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("至少填一项"), e.getMessage());
        verify(linkMapper, never()).insert(any());
    }

    @Test
    void 资料不是数组时被拒() {
        BusinessException obj = assertThrows(BusinessException.class,
                () -> service.create(mediaForm("{\"a\":1}"), ENTERPRISE_ID));
        assertTrue(obj.getMessage().contains("资料"), obj.getMessage());

        BusinessException nested = assertThrows(BusinessException.class,
                () -> service.create(mediaForm("[[\"/uploads/a.png\"]]"), ENTERPRISE_ID));
        assertTrue(nested.getMessage().contains("资料"), nested.getMessage());
        verify(linkMapper, never()).insert(any());
    }

    @Test
    void 非uploads前缀的资料被拒() {
        // 这个值最终是消费者免登录页面上 <img> 的 src，放行外链等于把落地页变成别人的追踪像素
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(mediaForm("[\"https://evil.example/x.png\"]"), ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("/uploads/"), e.getMessage());
        verify(linkMapper, never()).insert(any());
    }

    @Test
    void 资料条目超过上限被拒() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 11; i++) {
            if (i > 0) sb.append(',');
            sb.append("\"/uploads/").append(i).append(".png\"");
        }
        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(mediaForm(sb.append(']').toString()), ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("10"), e.getMessage());
        verify(linkMapper, never()).insert(any());
    }

    // ---------- PDF：保存时预热与页数上限 ----------

    @Test
    void 保存时预热PDF页图() {
        when(linkMapper.selectCount(any())).thenReturn(0L);
        when(pdfConvertService.convertPdfToImages("/uploads/report.PDF", 30))
                .thenReturn(List.of("/uploads/pdf-images/k/k_p1.jpg"));

        service.create(mediaForm("[\"/uploads/report.PDF\"]"), ENTERPRISE_ID);

        // 扩展名判定大小写不敏感：.PDF 也是 PDF，漏判就是消费者点开一片空白
        verify(pdfConvertService).convertPdfToImages("/uploads/report.PDF", 30);
        assertEquals("[\"/uploads/report.PDF\"]", insertedImage(), "库里只存原始条目，页图靠缓存目录还原");
    }

    @Test
    void PDF转不动时整个保存被拒且不写库() {
        when(pdfConvertService.convertPdfToImages("/uploads/bad.pdf", 30))
                .thenThrow(new BusinessException("PDF 共 40 页，超过 30 页上限，请拆分后上传"));

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(mediaForm("[\"/uploads/bad.pdf\"]"), ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("bad.pdf"), "文案要带上是哪个文件：" + e.getMessage());
        verify(linkMapper, never()).insert(any());
    }

    @Test
    void PDF转出来是空的也算失败() {
        // 文件不存在、不是 PDF、超过体积上限时 convert 返回空列表而不抛异常，
        // 放过它就会存下一个消费者点开必然空白的条目
        when(pdfConvertService.convertPdfToImages("/uploads/gone.pdf", 30)).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.create(mediaForm("[\"/uploads/gone.pdf\"]"), ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("gone.pdf"), e.getMessage());
        verify(linkMapper, never()).insert(any());
    }

    @Test
    void 展开后总页数超过上限被拒() {
        List<String> thirty = new ArrayList<>();
        for (int i = 1; i <= 30; i++) thirty.add("/uploads/pdf-images/k/k_p" + i + ".jpg");
        when(pdfConvertService.convertPdfToImages("/uploads/a.pdf", 30)).thenReturn(thirty);
        when(pdfConvertService.convertPdfToImages("/uploads/b.pdf", 30)).thenReturn(thirty);
        when(pdfConvertService.convertPdfToImages("/uploads/c.pdf", 30)).thenReturn(thirty);

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(
                mediaForm("[\"/uploads/a.pdf\",\"/uploads/b.pdf\",\"/uploads/c.pdf\"]"), ENTERPRISE_ID));
        assertTrue(e.getMessage().contains("60"), e.getMessage());
        verify(linkMapper, never()).insert(any());
    }

    @Test
    void 更新同样预热PDF并逐列SET() {
        when(linkMapper.selectById(5L)).thenReturn(aLink(5L, null, "[\"/uploads/old.png\"]", 1));
        when(pdfConvertService.convertPdfToImages("/uploads/new.pdf", 30))
                .thenReturn(List.of("/uploads/pdf-images/n/n_p1.jpg"));

        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("检测报告");
        form.setImage("[\"/uploads/new.pdf\"]");

        service.update(5L, form, ENTERPRISE_ID);

        LambdaUpdateWrapper<EnterpriseSideLink> w = captureUpdate();
        assertTrue(flat(w.getSqlSet()).contains("image="), w.getSqlSet());
        assertTrue(w.getParamNameValuePairs().containsValue("[\"/uploads/new.pdf\"]"),
                "资料必须按归一化后的数组写进去：" + w.getParamNameValuePairs());
    }

    // ---------- 消费者侧展开 ----------

    @Test
    void 消费者取数把PDF展开成页图() {
        when(linkMapper.selectList(any())).thenReturn(List.of(
                aLink(1L, null, "[\"/uploads/qr.png\",\"/uploads/r.pdf\"]", 1)));
        when(pdfConvertService.convertPdfToImages("/uploads/r.pdf", 30))
                .thenReturn(List.of("/uploads/pdf-images/k/k_p1.jpg", "/uploads/pdf-images/k/k_p2.jpg"));

        List<EnterpriseSideLink> links = service.resolveForConsumer(ENTERPRISE_ID);

        assertEquals(List.of("/uploads/qr.png", "/uploads/pdf-images/k/k_p1.jpg", "/uploads/pdf-images/k/k_p2.jpg"),
                links.get(0).getPages());
    }

    @Test
    void 消费者取数遇到转不动的PDF只跳过该条() {
        when(linkMapper.selectList(any())).thenReturn(List.of(
                aLink(1L, null, "[\"/uploads/bad.pdf\",\"/uploads/ok.pdf\"]", 1)));
        when(pdfConvertService.convertPdfToImages("/uploads/bad.pdf", 30))
                .thenThrow(new BusinessException("PDF 共 40 页，超过 30 页上限，请拆分后上传"));
        when(pdfConvertService.convertPdfToImages("/uploads/ok.pdf", 30))
                .thenReturn(List.of("/uploads/pdf-images/o/o_p1.jpg"));

        List<EnterpriseSideLink> links = service.resolveForConsumer(ENTERPRISE_ID);

        assertEquals(List.of("/uploads/pdf-images/o/o_p1.jpg"), links.get(0).getPages(),
                "一条转不动不该让整个侧栏按钮点不开");
    }

    @Test
    void 消费者取数兼容库里的裸路径老数据() {
        when(linkMapper.selectList(any())).thenReturn(List.of(aLink(1L, null, "/uploads/legacy-qr.png", 1)));

        List<EnterpriseSideLink> links = service.resolveForConsumer(ENTERPRISE_ID);

        assertEquals(List.of("/uploads/legacy-qr.png"), links.get(0).getPages());
        verifyNoInteractions(pdfConvertService);
    }

    @Test
    void 没有资料的链接pages是空数组而不是null() {
        when(linkMapper.selectList(any())).thenReturn(List.of(aLink(1L, "https://a", null, 1)));

        assertEquals(List.of(), service.resolveForConsumer(ENTERPRISE_ID).get(0).getPages());
    }

    @Test
    void 企业端列表不填pages() {
        // 企业要看的是自己传的原始条目；填 pages 就得每次列表都扫一遍缓存目录，不值当
        when(linkMapper.selectList(any())).thenReturn(List.of(aLink(1L, null, "[\"/uploads/r.pdf\"]", 1)));

        List<EnterpriseSideLink> links = service.list(ENTERPRISE_ID);

        assertNull(links.get(0).getPages());
        verifyNoInteractions(pdfConvertService);
    }
}
