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
        r.getRecords().forEach(ti -> {
            if (ti.getEnterpriseId() != null) {
                Enterprise e = enterpriseMapper.selectById(ti.getEnterpriseId());
                if (e != null) ti.setEnterpriseName(e.getName());
            }
            if (ti.getLabelSpecId() != null) {
                LabelSpec ls = labelSpecMapper.selectById(ti.getLabelSpecId());
                if (ls != null) ti.setLabelSpecName(ls.getSpecName());
            }
        });
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
