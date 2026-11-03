package com.huace.trace.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.TestReport;
import com.huace.trace.mapper.BatchTestReportMapper;
import com.huace.trace.mapper.TestReportMapper;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 检测报告的两条规则：
 * 1. 更新逐列显式 SET：updateById 的 NOT_NULL 策略会跳过 null，企业在表单里删掉"检测时间"
 *    保存后库里仍是旧值，扫码页照旧显示——接口却回了 200
 * 2. 报告字段是给溯源页 Map 用的，LocalDateTime 进了 Map 就丢掉 @JsonFormat，
 *    序列化出来带 T（"2026-08-02T00:00:00"），所以在这里就格式化成展示串
 * 另外报告一改消费者页就要立刻看到，所以清 tracePage 缓存；跨企业改删一律拒。
 */
@ExtendWith(MockitoExtension.class)
class TestReportServiceTest {

    private static final Long ENTERPRISE_ID = 50L;

    @Mock private TestReportMapper testReportMapper;
    @Mock private BatchTestReportMapper batchTestReportMapper;
    @Mock private CacheManager cacheManager;
    @Mock private Cache tracePageCache;

    private TestReportService service;

    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TestReport.class);
    }

    @BeforeEach
    void setUp() {
        // 参数顺序必须与 @RequiredArgsConstructor 的字段顺序一致
        service = new TestReportService(testReportMapper, batchTestReportMapper, cacheManager);
    }

    private TestReport ownedReport(long id) {
        TestReport r = new TestReport();
        r.setId(id);
        r.setEnterpriseId(ENTERPRISE_ID);
        r.setReportName("热干面");
        r.setTestTime(LocalDateTime.of(2026, 8, 2, 0, 0, 0));
        r.setTestOrg("武汉市质检院");
        return r;
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<TestReport> captureUpdate() {
        ArgumentCaptor<LambdaUpdateWrapper<TestReport>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(testReportMapper).update(isNull(), captor.capture());
        return captor.getValue();
    }

    private static String flat(String sql) {
        return sql.replaceAll("\\s+", "");
    }

    @Test
    void 清空检测时间要真的写成null() {
        when(testReportMapper.selectById(9L)).thenReturn(ownedReport(9L));
        when(cacheManager.getCache("tracePage")).thenReturn(tracePageCache);

        // 表单里删掉日期后 el-date-picker 给的就是 null，其余字段照常提交
        TestReport form = new TestReport();
        form.setReportName("热干面");
        form.setTestOrg("武汉市质检院");

        service.update(9L, form, ENTERPRISE_ID);

        String set = flat(captureUpdate().getSqlSet());
        assertTrue(set.contains("test_time="), "清空必须进 SET，否则库里永远是旧值：" + set);
        assertTrue(set.contains("report_name=") && set.contains("test_org="), set);
        assertFalse(set.contains("enterprise_id="), "归属不许被更新接口改写：" + set);
        verify(tracePageCache).clear();
    }

    @Test
    void 跨企业改报告被拒且不清缓存() {
        TestReport other = ownedReport(9L);
        other.setEnterpriseId(99L);
        when(testReportMapper.selectById(9L)).thenReturn(other);

        BusinessException e = assertThrows(BusinessException.class,
                () -> service.update(9L, new TestReport(), ENTERPRISE_ID));

        assertEquals("无权限操作", e.getMessage());
        verify(testReportMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }

    @Test
    void 删除报告后也要清缓存() {
        when(testReportMapper.selectById(9L)).thenReturn(ownedReport(9L));
        when(cacheManager.getCache("tracePage")).thenReturn(tracePageCache);

        service.delete(9L, ENTERPRISE_ID);

        verify(testReportMapper).deleteById(9L);
        verify(tracePageCache).clear();
    }

    @Test
    void 报告信息里的检测时间不带T() {
        Map<String, Object> info = service.buildReportInfo(ownedReport(9L));

        assertEquals("2026-08-02 00:00:00", info.get("testTime"));
        // 前端按 substring(0,7) 分月，格式化后仍能切出月份
        assertEquals("2026-08", ((String) info.get("testTime")).substring(0, 7));
    }

    @Test
    void 没填检测时间时报告信息里是空而不是当天零点() {
        TestReport r = ownedReport(9L);
        r.setTestTime(null);

        Map<String, Object> info = service.buildReportInfo(r);

        assertNull(info.get("testTime"));
    }
}
