package com.huace.trace.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Goods;
import com.huace.trace.entity.TraceLongModule;
import com.huace.trace.mapper.GoodsMapper;
import com.huace.trace.mapper.TraceLongModuleMapper;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 长图模块取数与写入规则回归测试：
 * 1. 商品级模块存在时只用商品级，不与企业默认混排
 * 2. 无商品级时回落企业默认
 * 3. 回落的两条查询：商品级查询先跑且 goods_id 绑值，企业默认查询用 goods_id IS NULL，
 *    两条都限定本企业、都过滤启用、都按 sort_order 升序
 * 4. pages 为空的模块不进导航
 * 5. 名称/图片/数量非法被拒
 * 6. 跨企业改别人的模块被拒
 * 7. PDF 转换前置校验（仅本系统文件、体积上限）
 * 8. 清空适用商品时 NULL 要真正写回库，且排序不能顺带清掉它
 * 9. 适用商品必须属于本企业；更新不带 status 时保留原有启停状态
 */
@ExtendWith(MockitoExtension.class)
class LongModuleServiceTest {

    @Mock private TraceLongModuleMapper moduleMapper;
    @Mock private PdfConvertService pdfConvertService;
    @Mock private CacheManager cacheManager;
    @Mock private GoodsMapper goodsMapper;

    private LongModuleService service;

    /**
     * LambdaQueryWrapper 渲染 SQL 片段时要查实体的列缓存，而缓存只在 MyBatis 启动扫描实体时装好；
     * 这里是纯 Mockito 环境，不装就抛 "can not find lambda cache for this entity"。
     */
    @BeforeAll
    static void initLongModuleTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TraceLongModule.class);
    }

    @BeforeEach
    void setUp() {
        // 必须手工构造并传真实 ObjectMapper：@InjectMocks 会把 objectMapper 注入成 mock，
        // mock 的 readValue 返回 null 会让 parsePages 永远返回空列表，测试假绿。
        // 参数顺序与 @RequiredArgsConstructor 字段声明顺序一致。
        service = new LongModuleService(moduleMapper, new ObjectMapper(), pdfConvertService, cacheManager, goodsMapper);
    }

    private TraceLongModule aModule(long id, Long goodsId, String name, String pages, int sort, int status) {
        TraceLongModule m = new TraceLongModule();
        m.setId(id);
        m.setEnterpriseId(23L);
        m.setGoodsId(goodsId);
        m.setName(name);
        m.setPages(pages);
        m.setSortOrder(sort);
        m.setStatus(status);
        return m;
    }

    @Test
    void 商品级模块存在时只用商品级() {
        when(moduleMapper.selectList(any())).thenReturn(List.of(
                aModule(11L, 5L, "牧场故事", "[\"/uploads/a.png\"]", 0, 1)));

        List<Map<String, Object>> result = service.resolveForTrace(23L, 5L);

        assertEquals(1, result.size());
        assertEquals("11", result.get(0).get("key"));
        assertEquals("牧场故事", result.get(0).get("name"));
        assertEquals(List.of("/uploads/a.png"), result.get(0).get("pages"));
    }

    @Test
    void 无商品级时回落企业默认() {
        // 第一次查商品级为空，第二次查企业默认
        when(moduleMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(aModule(9L, null, "产品信息", "[\"/uploads/p1.png\",\"/uploads/p2.png\"]", 0, 1)));

        List<Map<String, Object>> result = service.resolveForTrace(23L, 5L);

        assertEquals(1, result.size());
        assertEquals("9", result.get(0).get("key"));
        assertEquals(2, ((List<?>) result.get(0).get("pages")).size());
    }

    @Test
    void 商品级查询先跑且回落查询按goodsId空值过滤() {
        // "商品级全部停用"和"无商品级"的返回值完全一样，只看返回值测不出任何东西；
        // status 过滤和 goods_id 判空都写在 wrapper 里，必须把两条查询生成的 SQL 都抓出来断言。
        when(moduleMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(aModule(9L, null, "产品信息", "[\"/uploads/p1.png\"]", 0, 1)));

        List<Map<String, Object>> result = service.resolveForTrace(23L, 5L);
        assertEquals("9", result.get(0).get("key"));

        ArgumentCaptor<LambdaQueryWrapper<TraceLongModule>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        // 捕获顺序就是调用顺序：第 0 条必须是商品级查询，第 1 条才是企业默认回落
        verify(moduleMapper, times(2)).selectList(captor.capture());
        LambdaQueryWrapper<TraceLongModule> goodsQuery = captor.getAllValues().get(0);
        LambdaQueryWrapper<TraceLongModule> defaultQuery = captor.getAllValues().get(1);
        // 先渲染 SQL：#{...} 对应的值是 getSqlSegment() 过程中才登记进 paramNameValuePairs 的
        String goodsSql = goodsQuery.getSqlSegment();
        String defaultSql = defaultQuery.getSqlSegment();

        // MP 3.5.6 实测渲染为
        // "(enterprise_id = #{ew.paramNameValuePairs.MPGENVAL1} AND goods_id = #{...MPGENVAL2}
        //   AND status = #{...MPGENVAL3}) ORDER BY sort_order ASC"
        // 空格数量与 MPGENVAL 序号是实现细节，抹平空白后只锚定列名、谓词形状和绑定值。
        assertScopedEnabledAndOrdered(goodsSql);
        assertScopedEnabledAndOrdered(defaultSql);
        // 商品级查询：goods_id 与绑定值比较
        assertTrue(flat(goodsSql).contains("goods_id="), goodsSql);
        assertFalse(flat(goodsSql).contains("goods_idISNULL"), goodsSql);
        // 企业默认查询：goods_id IS NULL，不能写成 eq(getGoodsId, null)
        assertTrue(flat(defaultSql).contains("goods_idISNULL"), defaultSql);
        assertFalse(flat(defaultSql).contains("goods_id="), defaultSql);

        Map<String, Object> goodsParams = goodsQuery.getParamNameValuePairs();
        Map<String, Object> defaultParams = defaultQuery.getParamNameValuePairs();
        assertTrue(goodsParams.containsValue(5L), "商品级查询应把 goodsId 绑成参数: " + goodsParams);
        assertTrue(goodsParams.containsValue(1), "商品级查询应只取启用模块: " + goodsParams);
        // 回落查询既不绑定具体商品，也不把 null 当参数绑上去（那正是 eq(getGoodsId, null) 的写法）
        assertFalse(defaultParams.containsValue(5L), "企业默认查询不该绑定具体商品: " + defaultParams);
        assertFalse(defaultParams.containsValue(null), "企业默认查询不该绑定 null: " + defaultParams);
        assertTrue(defaultParams.containsValue(1), "企业默认查询应只取启用模块: " + defaultParams);
    }

    @Test
    void pages为空的模块不进导航() {
        when(moduleMapper.selectList(any())).thenReturn(List.of(
                aModule(11L, null, "有内容", "[\"/uploads/a.png\"]", 0, 1),
                aModule(12L, null, "空模块", "[]", 1, 1)));

        List<Map<String, Object>> result = service.resolveForTrace(23L, null);

        assertEquals(1, result.size());
        assertEquals("有内容", result.get(0).get("name"));
    }

    @Test
    void 新建模块落库前补齐归属与排序() {
        TraceLongModule form = new TraceLongModule();
        form.setName("有机养土");
        form.setPages("[\"/uploads/a.png\"]");
        when(moduleMapper.selectCount(any())).thenReturn(3L);

        service.create(form, 23L);

        assertEquals(23L, form.getEnterpriseId());
        assertEquals(3, form.getSortOrder());
        assertEquals(1, form.getStatus());
        verify(moduleMapper).insert(form);
    }

    @Test
    void 空模块被拒绝() {
        TraceLongModule form = new TraceLongModule();
        form.setName("空壳");
        form.setPages("[]");

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(form, 23L));
        assertTrue(e.getMessage().contains("至少一张"), e.getMessage());
        verify(moduleMapper, never()).insert(any());
    }

    @Test
    void 名称为空被拒绝() {
        TraceLongModule form = new TraceLongModule();
        form.setName("  ");
        form.setPages("[\"/uploads/a.png\"]");

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(form, 23L));
        assertTrue(e.getMessage().contains("名称"), e.getMessage());
    }

    @Test
    void 超过30页被拒绝() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 31; i++) sb.append(i == 0 ? "\"/uploads/p.png\"" : ",\"/uploads/p.png\"");
        sb.append("]");
        TraceLongModule form = new TraceLongModule();
        form.setName("超长");
        form.setPages(sb.toString());

        BusinessException e = assertThrows(BusinessException.class, () -> service.create(form, 23L));
        assertTrue(e.getMessage().contains("30"), e.getMessage());
    }

    @Test
    void 跨企业修改被拒绝() {
        when(moduleMapper.selectById(11L)).thenReturn(aModule(11L, null, "别人的", "[\"/uploads/a.png\"]", 0, 1));
        TraceLongModule form = new TraceLongModule();
        form.setName("改名");
        form.setPages("[\"/uploads/b.png\"]");

        BusinessException e = assertThrows(BusinessException.class, () -> service.update(11L, form, 99L));
        assertEquals("无权限操作", e.getMessage());
        verify(moduleMapper, never()).update(any(), any());
    }

    @Test
    void 绑定别企业商品被拒绝() {
        // goods_id 不校验归属，租户就能把模块挂到别企业（或不存在）的商品下：
        // 模块对谁都不可见，配置漂移也无从追查。与 HgzService 校验批次同款的两步判断。
        Goods foreign = new Goods();
        foreign.setEnterpriseId(99L);
        when(goodsMapper.selectById(77L)).thenReturn(foreign);
        when(moduleMapper.selectById(11L)).thenReturn(aModule(11L, null, "A", "[\"/uploads/a.png\"]", 0, 1));
        TraceLongModule form = new TraceLongModule();
        form.setName("有机养土");
        form.setPages("[\"/uploads/a.png\"]");
        form.setGoodsId(77L);

        BusinessException create = assertThrows(BusinessException.class, () -> service.create(form, 23L));
        assertEquals("无权使用该商品", create.getMessage());
        BusinessException update = assertThrows(BusinessException.class, () -> service.update(11L, form, 23L));
        assertEquals("无权使用该商品", update.getMessage());
        verify(moduleMapper, never()).insert(any());
        verify(moduleMapper, never()).update(any(), any());
    }

    @Test
    void 排序只作用于本企业模块() {
        when(moduleMapper.selectById(11L)).thenReturn(aModule(11L, null, "A", "[\"/uploads/a.png\"]", 0, 1));
        when(moduleMapper.selectById(12L)).thenReturn(aModule(12L, 5L, "B", "[\"/uploads/b.png\"]", 1, 1));

        service.sort(List.of(12L, 11L), 23L);

        ArgumentCaptor<TraceLongModule> captor = ArgumentCaptor.forClass(TraceLongModule.class);
        verify(moduleMapper, times(2)).updateById(captor.capture());
        List<TraceLongModule> saved = captor.getAllValues();
        assertEquals(12L, saved.get(0).getId());
        assertEquals(0, saved.get(0).getSortOrder());
        assertEquals(11L, saved.get(1).getId());
        assertEquals(1, saved.get(1).getSortOrder());
        // 排序补丁只带 sortOrder：goods_id 不在 SET 子句里，商品级模块不会因为拖拽排序被降级
        assertNull(saved.get(0).getGoodsId());
        assertNull(saved.get(1).getGoodsId());
    }

    @Test
    void 更新时清空适用商品要把NULL写回库() {
        when(moduleMapper.selectById(11L)).thenReturn(aModule(11L, 5L, "B", "[\"/uploads/b.png\"]", 0, 1));
        TraceLongModule form = new TraceLongModule();
        form.setName("改名");
        form.setPages("[\"/uploads/b.png\"]");
        form.setGoodsId(null);

        ArgumentCaptor<LambdaUpdateWrapper<TraceLongModule>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        service.update(11L, form, 23L);

        verify(moduleMapper).update(isNull(), captor.capture());
        verify(moduleMapper, never()).updateById(any());
        // updateById 的 NOT_NULL 策略会跳过 null 字段，改名成功但归属清不掉、接口照样回 200
        String sqlSet = captor.getValue().getSqlSet();
        assertTrue(sqlSet.contains("goods_id="), sqlSet);
    }

    @Test
    void 更新不带status时保留原有启停状态() {
        // 只改名称不动状态：form.getStatus() 为 null，直接绑进 SET 就是把这一行停用，
        // 而且 update 接口照样回 200 —— 三元判断被删掉时这条用例必须红。
        when(moduleMapper.selectById(11L)).thenReturn(aModule(11L, null, "A", "[\"/uploads/a.png\"]", 0, 1));
        Goods owned = new Goods();
        owned.setId(5L);
        owned.setEnterpriseId(23L);
        when(goodsMapper.selectById(5L)).thenReturn(owned);
        TraceLongModule form = new TraceLongModule();
        form.setName("改名");
        form.setPages("[\"/uploads/b.png\"]");
        form.setGoodsId(5L);
        form.setStatus(null);

        ArgumentCaptor<LambdaUpdateWrapper<TraceLongModule>> captor =
                ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        service.update(11L, form, 23L);

        verify(moduleMapper).update(isNull(), captor.capture());
        LambdaUpdateWrapper<TraceLongModule> wrapper = captor.getValue();
        // 先渲染 SET 再看绑定值
        String sqlSet = wrapper.getSqlSet();
        assertTrue(flat(sqlSet).contains("status="), sqlSet);
        Map<String, Object> params = wrapper.getParamNameValuePairs();
        assertTrue(params.containsValue(1), "SET 里 status 应绑定原有值 1，实际绑定: " + params);
        assertFalse(params.containsValue(null), "SET 里不该有 null 绑定，那会把模块直接停用: " + params);
    }

    @Test
    void 排序遇到别企业模块整体拒绝() {
        when(moduleMapper.selectById(11L)).thenReturn(aModule(11L, null, "A", "[\"/uploads/a.png\"]", 0, 1));
        TraceLongModule foreign = aModule(12L, null, "B", "[\"/uploads/b.png\"]", 1, 1);
        foreign.setEnterpriseId(77L);
        when(moduleMapper.selectById(12L)).thenReturn(foreign);

        BusinessException e = assertThrows(BusinessException.class, () -> service.sort(List.of(11L, 12L), 23L));
        assertEquals("无权限操作", e.getMessage());
        verify(moduleMapper, never()).updateById(any());
    }

    @Test
    void 模块变更后溯源页缓存被清空() {
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("tracePage")).thenReturn(cache);
        when(moduleMapper.selectById(11L)).thenReturn(aModule(11L, null, "A", "[\"/uploads/a.png\"]", 0, 1));

        service.delete(11L, 23L);

        verify(cache).clear();
    }

    /** 两条查询都必须限定本企业、只取启用模块、按 sort_order 升序 */
    private static void assertScopedEnabledAndOrdered(String sql) {
        String flat = flat(sql);
        assertTrue(flat.contains("enterprise_id="), sql);
        assertTrue(flat.contains("status="), sql);
        assertTrue(flat.contains("ORDERBYsort_orderASC"), sql);
    }

    /** 抹平空白，避免断言绑死 MyBatis-Plus 渲染出的空格格式 */
    private static String flat(String sql) {
        return sql.replaceAll("\\s+", "");
    }
}
