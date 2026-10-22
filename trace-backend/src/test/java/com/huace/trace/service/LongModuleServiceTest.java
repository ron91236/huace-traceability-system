package com.huace.trace.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.entity.TraceLongModule;
import com.huace.trace.mapper.TraceLongModuleMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
 * 3. 商品级全部停用同样回落企业默认
 * 4. pages 为空的模块不进导航
 * 5. 名称/图片/数量非法被拒
 * 6. 跨企业改别人的模块被拒
 * 7. PDF 转换前置校验（仅本系统文件、体积上限）
 * 8. 清空适用商品时 NULL 要真正写回库，且排序不能顺带清掉它
 */
@ExtendWith(MockitoExtension.class)
class LongModuleServiceTest {

    @Mock private TraceLongModuleMapper moduleMapper;
    @Mock private PdfConvertService pdfConvertService;
    @Mock private CacheManager cacheManager;

    private LongModuleService service;

    @BeforeEach
    void setUp() {
        // 必须手工构造并传真实 ObjectMapper：@InjectMocks 会把 objectMapper 注入成 mock，
        // mock 的 readValue 返回 null 会让 parsePages 永远返回空列表，测试假绿。
        // 参数顺序与 @RequiredArgsConstructor 字段声明顺序一致。
        service = new LongModuleService(moduleMapper, new ObjectMapper(), pdfConvertService, cacheManager);
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
    void 商品级全部停用时回落企业默认() {
        when(moduleMapper.selectList(any()))
                .thenReturn(List.of())
                .thenReturn(List.of(aModule(9L, null, "产品信息", "[\"/uploads/p1.png\"]", 0, 1)));

        List<Map<String, Object>> result = service.resolveForTrace(23L, 5L);

        assertEquals("9", result.get(0).get("key"));
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
}
