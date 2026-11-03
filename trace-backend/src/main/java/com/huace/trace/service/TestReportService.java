package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.BatchTestReport;
import com.huace.trace.entity.TestReport;
import com.huace.trace.mapper.BatchTestReportMapper;
import com.huace.trace.mapper.TestReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestReportService {

    /** 报告字段是给溯源页的 Map 用的，Map 序列化不认实体上的 @JsonFormat，只能在这里定格式 */
    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TestReportMapper testReportMapper;
    private final BatchTestReportMapper batchTestReportMapper;
    private final CacheManager cacheManager;

    public PageResult<TestReport> list(int page, int size, String keyword, Long enterpriseId) {
        LambdaQueryWrapper<TestReport> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) w.eq(TestReport::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(keyword)) w.like(TestReport::getReportName, keyword);
        w.orderByDesc(TestReport::getId);
        Page<TestReport> r = testReportMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public List<TestReport> listByEnterprise(Long enterpriseId) {
        return testReportMapper.selectList(
                new LambdaQueryWrapper<TestReport>()
                        .eq(TestReport::getEnterpriseId, enterpriseId)
                        .orderByDesc(TestReport::getId));
    }

    public void create(TestReport report) {
        testReportMapper.insert(report);
    }

    public void update(Long id, TestReport report, Long enterpriseId) {
        TestReport existing = testReportMapper.selectById(id);
        if (existing == null) throw new BusinessException("检测报告不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        // 逐列显式 SET：updateById 走 NOT_NULL 策略，企业在表单里删掉的检测时间/检测机构会以
        // null 提交并被整个跳过，库里留旧值、消费者页照旧显示，而接口回的是 200。
        // 归属 enterprise_id 不进 SET：请求体里带的那个不该把报告搬到别的企业名下。
        testReportMapper.update(null, new LambdaUpdateWrapper<TestReport>()
                .eq(TestReport::getId, id)
                .set(TestReport::getReportName, report.getReportName())
                .set(TestReport::getTestCode, report.getTestCode())
                .set(TestReport::getReportImage, report.getReportImage())
                .set(TestReport::getReportPdf, report.getReportPdf())
                .set(TestReport::getTestOrg, report.getTestOrg())
                .set(TestReport::getTestTime, report.getTestTime())
                .set(TestReport::getTestMethod, report.getTestMethod())
                .set(TestReport::getTestBasis, report.getTestBasis())
                .set(TestReport::getTestType, report.getTestType())
                .set(TestReport::getTestResult, report.getTestResult()));
        evictTracePage();
    }

    public void delete(Long id, Long enterpriseId) {
        TestReport existing = testReportMapper.selectById(id);
        if (existing == null) throw new BusinessException("检测报告不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        testReportMapper.deleteById(id);
        evictTracePage();
    }

    // ==================== 批次-报告关联 ====================

    public List<TestReport> listByBatchId(Long batchId) {
        List<BatchTestReport> bindings = batchTestReportMapper.selectList(
                new LambdaQueryWrapper<BatchTestReport>()
                        .eq(BatchTestReport::getBatchId, batchId)
                        .orderByAsc(BatchTestReport::getSortOrder));
        if (bindings.isEmpty()) return List.of();
        List<Long> reportIds = bindings.stream().map(BatchTestReport::getTestReportId).toList();
        return testReportMapper.selectList(
                new LambdaQueryWrapper<TestReport>().in(TestReport::getId, reportIds));
    }

    public void bindToBatch(Long batchId, Long reportId, Long enterpriseId) {
        if (enterpriseId != null) {
            TestReport report = testReportMapper.selectById(reportId);
            if (report == null || !report.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        }
        // 避免重复绑定
        Long count = batchTestReportMapper.selectCount(
                new LambdaQueryWrapper<BatchTestReport>()
                        .eq(BatchTestReport::getBatchId, batchId)
                        .eq(BatchTestReport::getTestReportId, reportId));
        if (count > 0) return;
        // 获取当前最大 sort_order
        Long maxSort = batchTestReportMapper.selectCount(
                new LambdaQueryWrapper<BatchTestReport>()
                        .eq(BatchTestReport::getBatchId, batchId));
        BatchTestReport btr = new BatchTestReport();
        btr.setBatchId(batchId);
        btr.setTestReportId(reportId);
        btr.setSortOrder(maxSort.intValue());
        batchTestReportMapper.insert(btr);
    }

    public void unbindFromBatch(Long batchId, Long reportId, Long enterpriseId) {
        if (enterpriseId != null) {
            TestReport report = testReportMapper.selectById(reportId);
            if (report == null || !report.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        }
        batchTestReportMapper.delete(
                new LambdaQueryWrapper<BatchTestReport>()
                        .eq(BatchTestReport::getBatchId, batchId)
                        .eq(BatchTestReport::getTestReportId, reportId));
    }

    /** 解绑批次下的全部报告 */
    public void unbindAllFromBatch(Long batchId) {
        batchTestReportMapper.delete(
                new LambdaQueryWrapper<BatchTestReport>()
                        .eq(BatchTestReport::getBatchId, batchId));
    }

    // ==================== 构建报告信息Map ====================

    public Map<String, Object> buildReportInfo(TestReport report) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", report.getId());
        info.put("reportName", report.getReportName());
        info.put("testCode", report.getTestCode());
        info.put("testOrg", report.getTestOrg());
        info.put("testTime", report.getTestTime() == null ? null : DISPLAY_TIME.format(report.getTestTime()));
        info.put("testMethod", report.getTestMethod());
        info.put("testBasis", report.getTestBasis());
        info.put("testType", report.getTestType());
        info.put("testResult", report.getTestResult());
        info.put("reportPdf", report.getReportPdf());
        // 将逗号分隔的reportImage拆为数组
        if (StringUtils.hasText(report.getReportImage())) {
            info.put("reportImages", Arrays.asList(report.getReportImage().split(",")));
        } else {
            info.put("reportImages", List.of());
        }
        return info;
    }

    public List<Map<String, Object>> buildReportListForBatch(Long batchId, Long fallbackReportId) {
        List<TestReport> reports = listByBatchId(batchId);
        // 兼容旧数据：中间表无数据时回退到 batch.testReportId
        if (reports.isEmpty() && fallbackReportId != null) {
            TestReport report = testReportMapper.selectById(fallbackReportId);
            if (report != null) return List.of(buildReportInfo(report));
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (TestReport r : reports) {
            result.add(buildReportInfo(r));
        }
        return result;
    }

    /** 报告一改消费者扫码页就得立刻看到，直接清缓存（注入 TracePageService 会成循环依赖） */
    private void evictTracePage() {
        try {
            Cache cache = cacheManager.getCache("tracePage");
            if (cache != null) cache.clear();
        } catch (Exception e) {
            log.warn("清除溯源页缓存失败", e);
        }
    }
}
