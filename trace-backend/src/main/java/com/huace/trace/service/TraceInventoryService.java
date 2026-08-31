package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.LabelSpec;
import com.huace.trace.entity.TraceInventory;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.LabelSpecMapper;
import com.huace.trace.mapper.TraceInventoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TraceInventoryService {
    private final TraceInventoryMapper traceInventoryMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final LabelSpecMapper labelSpecMapper;

    public PageResult<TraceInventory> list(int page, int size, Long enterpriseId) {
        LambdaQueryWrapper<TraceInventory> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) w.eq(TraceInventory::getEnterpriseId, enterpriseId);
        w.orderByDesc(TraceInventory::getId);
        Page<TraceInventory> r = traceInventoryMapper.selectPage(new Page<>(page, size), w);
        List<TraceInventory> records = r.getRecords();
        if (!records.isEmpty()) {
            // 批量预取 enterprise/labelSpec，消除逐行查询
            java.util.Set<Long> entIds = records.stream().map(TraceInventory::getEnterpriseId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            java.util.Set<Long> labelSpecIds = records.stream().map(TraceInventory::getLabelSpecId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            Map<Long, Enterprise> entMap = entIds.isEmpty() ? java.util.Collections.emptyMap()
                    : enterpriseMapper.selectBatchIds(entIds).stream()
                            .collect(java.util.stream.Collectors.toMap(Enterprise::getId, e -> e));
            Map<Long, LabelSpec> labelSpecMap = labelSpecIds.isEmpty() ? java.util.Collections.emptyMap()
                    : labelSpecMapper.selectBatchIds(labelSpecIds).stream()
                            .collect(java.util.stream.Collectors.toMap(LabelSpec::getId, ls -> ls));
            records.forEach(ti -> {
                Enterprise e = entMap.get(ti.getEnterpriseId());
                if (e != null) ti.setEnterpriseName(e.getName());
                LabelSpec ls = labelSpecMap.get(ti.getLabelSpecId());
                if (ls != null) ti.setLabelSpecName(ls.getSpecName());
            });
        }
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(TraceInventory ti) { traceInventoryMapper.insert(ti); }
    public void delete(Long id, Long enterpriseId) {
        TraceInventory existing = traceInventoryMapper.selectById(id);
        if (existing == null) throw new BusinessException("记录不存在");
        if (existing.getEnterpriseId() != null && !existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        traceInventoryMapper.deleteById(id);
    }
}
