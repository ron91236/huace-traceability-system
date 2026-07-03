package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.BatchTestReport;
import com.huace.trace.entity.TestReport;
import com.huace.trace.mapper.BatchTestReportMapper;
import com.huace.trace.mapper.TestReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TestReportService {

    private final TestReportMapper testReportMapper;
    private final BatchTestReportMapper batchTestReportMapper;

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
        report.setId(id);
        report.setEnterpriseId(enterpriseId);
        testReportMapper.updateById(report);
    }

    public void delete(Long id, Long enterpriseId) {
        TestReport existing = testReportMapper.selectById(id);
        if (existing == null) throw new BusinessException("检测报告不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        testReportMapper.deleteById(id);
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

    // ==================== 构建报告信息Map ====================

    public Map<String, Object> buildReportInfo(TestReport report) {
        Map<String, Object> info = new HashMap<>();
        info.put("id", report.getId());
        info.put("reportName", report.getReportName());
        info.put("testCode", report.getTestCode());
        info.put("testOrg", report.getTestOrg());
        info.put("testTime", report.getTestTime());
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
}
