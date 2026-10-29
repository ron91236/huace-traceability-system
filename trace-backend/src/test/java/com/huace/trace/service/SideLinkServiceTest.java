package com.huace.trace.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
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
 */
@ExtendWith(MockitoExtension.class)
class SideLinkServiceTest {

    @Mock private EnterpriseSideLinkMapper linkMapper;

    private SideLinkService service;

    private static final Long ENTERPRISE_ID = 8L;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), EnterpriseSideLink.class);
    }

    @BeforeEach
    void setUp() {
        service = new SideLinkService(linkMapper);
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
        verify(linkMapper, never()).updateById(any());
    }

    @Test
    void 更新不带status时保留原启停状态() {
        when(linkMapper.selectById(5L)).thenReturn(aLink(5L, "https://a", null, 0));

        EnterpriseSideLink form = new EnterpriseSideLink();
        form.setName("改名");
        form.setUrl("https://b");
        form.setStatus(null);

        service.update(5L, form, ENTERPRISE_ID);

        ArgumentCaptor<EnterpriseSideLink> captor = ArgumentCaptor.forClass(EnterpriseSideLink.class);
        verify(linkMapper).updateById(captor.capture());
        assertEquals(0, captor.getValue().getStatus());
        assertEquals(ENTERPRISE_ID, captor.getValue().getEnterpriseId(), "更新不许把行改到别的企业名下");
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
}
