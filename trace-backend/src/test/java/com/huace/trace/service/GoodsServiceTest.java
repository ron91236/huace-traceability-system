package com.huace.trace.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.CodePackageItem;
import com.huace.trace.entity.Goods;
import com.huace.trace.entity.OrderCode;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.CodePackageItemMapper;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.GoodsMapper;
import com.huace.trace.mapper.OrderCodeMapper;
import com.huace.trace.mapper.ProductMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 商品换溯源模板时的向下同步规则：
 * 1. 换了模板 → 该商品已绑定的码、这些码所属的条码段、MongoDB 那份拷贝一起改，并失效溯源缓存
 * 2. 没换 / PUT 里根本没带这个字段 → 一处都不碰
 * 3. 目标模板行查不到 → 不写（写进去就是一个取不出来的 key）
 */
@ExtendWith(MockitoExtension.class)
class GoodsServiceTest {

    private static final Long GOODS_ID = 54L;
    private static final Long ENTERPRISE_ID = 40L;
    private static final String NEW_KEY = "tpl_1793266863867";

    @Mock private GoodsMapper goodsMapper;
    @Mock private ProductMapper productMapper;
    @Mock private EnterpriseMapper enterpriseMapper;
    @Mock private TraceTemplateMapper traceTemplateMapper;
    @Mock private TracePageService tracePageService;
    @Mock private CodePackageItemMapper codePackageItemMapper;
    @Mock private OrderCodeMapper orderCodeMapper;
    @Mock private MongoCodeItemService mongoCodeItemService;

    private GoodsService service;

    /**
     * Wrapper 渲染 SET/WHERE 片段要查实体的列缓存，纯 Mockito 环境下没装就抛
     * "can not find lambda cache for this entity"。
     */
    @BeforeAll
    static void initTableInfo() {
        var assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, CodePackageItem.class);
        TableInfoHelper.initTableInfo(assistant, OrderCode.class);
    }

    @BeforeEach
    void setUp() {
        // 参数顺序必须与 @RequiredArgsConstructor 的字段顺序一致
        service = new GoodsService(goodsMapper, productMapper, enterpriseMapper, traceTemplateMapper,
                tracePageService, codePackageItemMapper, orderCodeMapper, mongoCodeItemService);
    }

    private static String flat(String sql) {
        return sql.replaceAll("\\s+", "");
    }

    private Goods existingGoods(Long templateId) {
        Goods g = new Goods();
        g.setId(GOODS_ID);
        g.setEnterpriseId(ENTERPRISE_ID);
        g.setName("牛奶");
        g.setTraceTemplateId(templateId);
        when(goodsMapper.selectById(GOODS_ID)).thenReturn(g);
        return g;
    }

    /** PUT 请求体：本仓库的商品表单整对象提交，所以只有 traceTemplateId 有意义 */
    private Goods payload(Long templateId) {
        Goods g = new Goods();
        g.setName("牛奶");
        g.setTraceTemplateId(templateId);
        return g;
    }

    private void stubTemplateExists() {
        TraceTemplate t = new TraceTemplate();
        t.setId(22L);
        t.setTemplateKey(NEW_KEY);
        when(traceTemplateMapper.selectById(22L)).thenReturn(t);
    }

    private void stubSegmentsOwnedByGoods(Long... orderCodeIds) {
        List<CodePackageItem> rows = java.util.Arrays.stream(orderCodeIds).map(id -> {
            CodePackageItem i = new CodePackageItem();
            i.setOrderCodeId(id);
            return i;
        }).toList();
        when(codePackageItemMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rows);
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<CodePackageItem> captureItemUpdate() {
        ArgumentCaptor<LambdaUpdateWrapper<CodePackageItem>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(codePackageItemMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }

    @Test
    void 换模板后已绑定的码跟着改模板() {
        existingGoods(20L);
        stubTemplateExists();
        stubSegmentsOwnedByGoods(45L);

        service.update(GOODS_ID, payload(22L), ENTERPRISE_ID);

        LambdaUpdateWrapper<CodePackageItem> w = captureItemUpdate();
        String set = flat(w.getSqlSet());
        assertTrue(set.contains("trace_template="), set);
        assertFalse(set.contains("bind_status="), set);
        assertFalse(set.contains("goods_id="), set);
        String where = flat(w.getSqlSegment());
        assertTrue(where.contains("goods_id="), where);
        // 只推已绑定的码：未绑定/作废的码不该被一个商品编辑改动
        assertTrue(where.contains("bind_status="), where);
        assertTrue(w.getParamNameValuePairs().containsValue(GOODS_ID),
                "WHERE 里的商品 id 就是被编辑的那个：" + w.getParamNameValuePairs());
        verify(mongoCodeItemService).updateTraceTemplateByGoodsId(GOODS_ID, NEW_KEY);
        verify(tracePageService).evictAllCache();
    }

    @Test
    void 这些码所属的条码段也一起改否则下次编辑会推回旧模板() {
        existingGoods(20L);
        stubTemplateExists();
        stubSegmentsOwnedByGoods(45L, 46L);

        service.update(GOODS_ID, payload(22L), ENTERPRISE_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<OrderCode>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(orderCodeMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<OrderCode> w = captor.getValue();
        assertTrue(flat(w.getSqlSet()).contains("trace_template="), w.getSqlSet());
        // flat 把空格去掉了，所以要比 "IDIN("
        assertTrue(flat(w.getSqlSegment()).toUpperCase().contains("IDIN("), w.getSqlSegment());
        assertTrue(w.getParamNameValuePairs().containsValue(45L), w.getParamNameValuePairs().toString());
        assertTrue(w.getParamNameValuePairs().containsValue(46L), w.getParamNameValuePairs().toString());
    }

    @Test
    void 码没有归属条码段时只改码明细() {
        existingGoods(20L);
        stubTemplateExists();
        stubSegmentsOwnedByGoods();

        service.update(GOODS_ID, payload(22L), ENTERPRISE_ID);

        verify(codePackageItemMapper).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(orderCodeMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void 模板没变时一处都不碰() {
        existingGoods(20L);

        service.update(GOODS_ID, payload(20L), ENTERPRISE_ID);

        verify(codePackageItemMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(orderCodeMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(mongoCodeItemService, never()).updateTraceTemplateByGoodsId(anyLong(), anyString());
        // 商品本身还是可能改了别的字段，缓存照旧失效
        verify(tracePageService).evictAllCache();
    }

    @Test
    void PUT没带模板字段时不把码上的模板抹成空() {
        // updateById 的 NOT_NULL 策略下商品列不会被动，码也必须跟着不动
        existingGoods(20L);

        service.update(GOODS_ID, payload(null), ENTERPRISE_ID);

        verify(traceTemplateMapper, never()).selectById(any());
        verify(codePackageItemMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(mongoCodeItemService, never()).updateTraceTemplateByGoodsId(anyLong(), anyString());
    }

    @Test
    void 目标模板查不到时不写任何码() {
        existingGoods(20L);
        when(traceTemplateMapper.selectById(22L)).thenReturn(null);

        service.update(GOODS_ID, payload(22L), ENTERPRISE_ID);

        verify(codePackageItemMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(orderCodeMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(mongoCodeItemService, never()).updateTraceTemplateByGoodsId(anyLong(), anyString());
    }

    @Test
    void 别人的商品既不改也不推模板() {
        existingGoods(20L);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.update(GOODS_ID, payload(22L), 999L));

        assertEquals("无权限操作", e.getMessage());
        verify(goodsMapper, never()).updateById(any());
        verify(codePackageItemMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
    }
}
