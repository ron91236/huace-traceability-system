package com.huace.trace.service;

import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 数据大屏聚合逻辑回归测试（管理员视图 + 企业视图）。
 */
@ExtendWith(MockitoExtension.class)
class DataScreenServiceTest {

    @Mock private ScanRecordMapper scanRecordMapper;
    @Mock private EnterpriseMapper enterpriseMapper;
    @Mock private GoodsMapper goodsMapper;
    @Mock private CodePackageItemMapper codePackageItemMapper;
    @Mock private ProductMapper productMapper;
    @Mock private BatchMapper batchMapper;

    @InjectMocks
    private DataScreenService service;

    private void stubCommonCounts() {
        // kpi.totalInventory, scanRate.total, scanRate.bound
        when(codePackageItemMapper.selectCount(any())).thenReturn(11L, 10L, 7L);
        // kpi.totalScans, scanRate.scanned
        when(scanRecordMapper.selectCount(any())).thenReturn(100L, 3L);
    }

    private void stubProductCategory() {
        Goods g1 = new Goods();
        g1.setId(1L);
        g1.setProductId(null);
        Goods g2 = new Goods();
        g2.setId(2L);
        g2.setProductId(5L);
        when(goodsMapper.selectList(any())).thenReturn(List.of(g1, g2));

        Product p = new Product();
        p.setId(5L);
        p.setCategory("饮料");
        p.setName("精酿啤酒");
        when(productMapper.selectBatchIds(anyCollection())).thenReturn(List.of(p));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 管理员视图_KPI与扫码率计算正确() {
        stubCommonCounts();
        stubProductCategory();
        when(enterpriseMapper.selectCount(isNull())).thenReturn(10L);
        when(goodsMapper.selectCount(any())).thenReturn(17L);

        Map<String, Object> kpi = service.getAllData(null);
        Map<String, Object> kpiData = (Map<String, Object>) kpi.get("kpi");
        Map<String, Object> rate = (Map<String, Object>) kpi.get("scanRate");

        assertEquals(11L, kpiData.get("totalInventory"));
        assertEquals(100L, kpiData.get("totalScans"));
        assertEquals(10L, kpiData.get("merchantCount"));
        assertEquals(17L, kpiData.get("productCount"));

        assertEquals(10L, rate.get("total"));
        assertEquals(7L, rate.get("bound"));
        assertEquals(3L, rate.get("unbound"));
        assertEquals(3L, rate.get("scanned"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 管理员视图_产品类别分组含未分类() {
        stubCommonCounts();
        stubProductCategory();
        when(enterpriseMapper.selectCount(isNull())).thenReturn(10L);

        Map<String, Object> result = service.getAllData(null);
        List<Map<String, Object>> categories = (List<Map<String, Object>>) result.get("productCategory");

        Map<String, Object> byName = new HashMap<>();
        for (Map<String, Object> c : categories) byName.put((String) c.get("name"), c);
        assertEquals(1L, byName.get("未分类").get("value"));
        assertEquals(1L, byName.get("饮料").get("value"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 管理员视图_企业扫码排行回填企业名称() {
        stubCommonCounts();
        when(enterpriseMapper.selectCount(isNull())).thenReturn(10L);

        Map<String, Object> row = new HashMap<>();
        row.put("enterprise_id", 1L);
        row.put("count", 120L);
        when(scanRecordMapper.countByEnterprise()).thenReturn(List.of(row));

        Enterprise e = new Enterprise();
        e.setId(1L);
        e.setName("河南福鹿家鲜啤酒业有限公司");
        when(enterpriseMapper.selectBatchIds(anyCollection())).thenReturn(List.of(e));

        Map<String, Object> result = service.getAllData(null);
        List<Map<String, Object>> ranking = (List<Map<String, Object>>) result.get("enterpriseRanking");

        assertEquals(1, ranking.size());
        assertEquals("河南福鹿家鲜啤酒业有限公司", ranking.get(0).get("enterpriseName"));
        assertEquals(120L, ranking.get(0).get("scanCount"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void 企业视图_商家数为1且含批次统计() {
        stubCommonCounts();
        when(goodsMapper.selectCount(any())).thenReturn(3L);
        when(batchMapper.selectCount(any())).thenReturn(5L);

        Map<String, Object> result = service.getAllData(1L);
        Map<String, Object> kpiData = (Map<String, Object>) result.get("kpi");

        assertEquals(1L, kpiData.get("merchantCount"));
        assertEquals(5L, kpiData.get("batchCount"));
        assertNull(result.get("enterpriseRanking"), "企业视图不应包含企业排行");
    }

    @Test
    void 企业视图_扫码率未绑定数正确() {
        stubCommonCounts();

        Map<String, Object> result = service.getAllData(1L);
        @SuppressWarnings("unchecked")
        Map<String, Object> rate = (Map<String, Object>) result.get("scanRate");

        assertEquals(10L, rate.get("total"));
        assertEquals(7L, rate.get("bound"));
        assertEquals(3L, rate.get("unbound"));
    }
}
