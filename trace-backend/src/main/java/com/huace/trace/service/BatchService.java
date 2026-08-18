package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import com.huace.trace.mapper.TestReportMapper;
import com.huace.trace.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BatchService {
    private final BatchMapper batchMapper;
    private final GoodsMapper goodsMapper;
    private final EnterpriseBaseMapper baseMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final TestReportService testReportService;
    private final TestReportMapper testReportMapper;

    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    public PageResult<Batch> list(int page, int size, String keyword, Long enterpriseId) {
        LambdaQueryWrapper<Batch> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) w.eq(Batch::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(keyword)) w.like(Batch::getName, keyword);
        w.orderByDesc(Batch::getId);
        Page<Batch> r = batchMapper.selectPage(new Page<>(page, size), w);
        r.getRecords().forEach(b -> {
            if (b.getGoodsId() != null) {
                Goods g = goodsMapper.selectById(b.getGoodsId());
                if (g != null) b.setGoodsName(g.getName());
            }
            if (b.getBaseId() != null) {
                EnterpriseBase base = baseMapper.selectById(b.getBaseId());
                if (base != null) b.setBaseName(base.getName());
            }
            // 回填多报告：中间表为主，兼容旧数据 testReportId
            List<TestReport> reports = testReportService.listByBatchId(b.getId());
            List<Long> reportIds = new ArrayList<>();
            List<String> reportNames = new ArrayList<>();
            for (TestReport tr : reports) {
                reportIds.add(tr.getId());
                if (StringUtils.hasText(tr.getReportName())) reportNames.add(tr.getReportName());
            }
            if (reportIds.isEmpty() && b.getTestReportId() != null) {
                TestReport tr = testReportMapper.selectById(b.getTestReportId());
                if (tr != null) {
                    reportIds.add(tr.getId());
                    if (StringUtils.hasText(tr.getReportName())) reportNames.add(tr.getReportName());
                }
            }
            b.setTestReportIds(reportIds);
            b.setTestReportName(String.join("、", reportNames));
        });
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(Batch b) {
        batchMapper.insert(b);
        syncTestReportBindings(b.getId(), b.getTestReportIds(), b.getTestReportId(), b.getEnterpriseId());
    }

    public void update(Long id, Batch b, Long enterpriseId) {
        Batch existing = batchMapper.selectById(id);
        if (existing == null) throw new BusinessException("批次不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        b.setId(id);
        b.setEnterpriseId(enterpriseId);
        batchMapper.updateById(b);
        // 前端传了 testReportIds 才重绑，否则保持原绑定不变
        if (b.getTestReportIds() != null) {
            syncTestReportBindings(id, b.getTestReportIds(), null, enterpriseId);
        }
    }

    /**
     * 同步批次与检测报告的多对多绑定关系。
     * 优先使用 reportIds；未传时回退到旧的单报告字段 testReportId。
     */
    private void syncTestReportBindings(Long batchId, List<Long> reportIds, Long fallbackReportId, Long enterpriseId) {
        List<Long> ids = reportIds != null ? reportIds
                : (fallbackReportId != null ? List.of(fallbackReportId) : null);
        if (ids == null) return;
        testReportService.unbindAllFromBatch(batchId);
        if (ids.isEmpty()) {
            // 清空绑定，同时清空兼容字段
            batchMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Batch>()
                    .eq(Batch::getId, batchId)
                    .set(Batch::getTestReportId, null)
                    .set(Batch::getTestReport, null));
            return;
        }
        for (Long rid : ids) {
            testReportService.bindToBatch(batchId, rid, enterpriseId);
        }
        // 兼容字段：batch.testReportId/testReport 存第一个报告的ID与PDF
        TestReport report = testReportMapper.selectById(ids.get(0));
        batchMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<Batch>()
                .eq(Batch::getId, batchId)
                .set(Batch::getTestReportId, ids.get(0))
                .set(Batch::getTestReport, report != null && StringUtils.hasText(report.getReportPdf()) ? report.getReportPdf() : null));
    }

    /** 复制批次（含检测报告绑定关系） */
    public Batch copy(Long id, Long enterpriseId) {
        Batch existing = batchMapper.selectById(id);
        if (existing == null) throw new BusinessException("批次不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        Batch nb = new Batch();
        nb.setName(existing.getName() + "（副本）");
        nb.setGoodsId(existing.getGoodsId());
        nb.setGoodsSpec(existing.getGoodsSpec());
        nb.setBaseId(existing.getBaseId());
        nb.setEnterpriseId(existing.getEnterpriseId());
        nb.setTestCode(existing.getTestCode());
        nb.setTestReport(existing.getTestReport());
        nb.setTestOrg(existing.getTestOrg());
        nb.setTestTime(existing.getTestTime());
        nb.setTestMethod(existing.getTestMethod());
        nb.setTestBasis(existing.getTestBasis());
        nb.setTestType(existing.getTestType());
        nb.setTestResult(existing.getTestResult());
        batchMapper.insert(nb);
        // 复制报告绑定关系（中间表为主，兼容旧数据单字段）
        List<TestReport> reports = testReportService.listByBatchId(id);
        if (reports.isEmpty() && existing.getTestReportId() != null) {
            testReportService.bindToBatch(nb.getId(), existing.getTestReportId(), null);
        } else {
            for (TestReport tr : reports) {
                testReportService.bindToBatch(nb.getId(), tr.getId(), null);
            }
        }
        if (existing.getTestReportId() != null) {
            Batch u = new Batch();
            u.setId(nb.getId());
            u.setTestReportId(existing.getTestReportId());
            batchMapper.updateById(u);
        }
        return nb;
    }

    public String generateQrcode(Long id, Long enterpriseId) {
        Batch batch = batchMapper.selectById(id);
        if (batch == null) throw new BusinessException("批次不存在");
        if (!batch.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        String traceUrl = baseUrl + "/trace/batch/" + id;
        return QrCodeUtil.generateBase64(traceUrl, 300, 300);
    }
}
