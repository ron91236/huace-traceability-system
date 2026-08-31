package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.EnterpriseBase;
import com.huace.trace.mapper.EnterpriseBaseMapper;
import com.huace.trace.mapper.EnterpriseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnterpriseBaseService {
    private final EnterpriseBaseMapper baseMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final TracePageService tracePageService;

    public PageResult<EnterpriseBase> list(int page, int size, String keyword, Long enterpriseId) {
        LambdaQueryWrapper<EnterpriseBase> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) w.eq(EnterpriseBase::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(keyword)) w.like(EnterpriseBase::getName, keyword);
        w.orderByDesc(EnterpriseBase::getId);
        Page<EnterpriseBase> r = baseMapper.selectPage(new Page<>(page, size), w);
        List<EnterpriseBase> records = r.getRecords();
        if (!records.isEmpty()) {
            // 批量预取 enterprise，消除逐行查询
            java.util.Set<Long> entIds = records.stream().map(EnterpriseBase::getEnterpriseId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            Map<Long, Enterprise> entMap = entIds.isEmpty() ? java.util.Collections.emptyMap()
                    : enterpriseMapper.selectBatchIds(entIds).stream()
                            .collect(java.util.stream.Collectors.toMap(Enterprise::getId, e -> e));
            records.forEach(b -> {
                Enterprise e = entMap.get(b.getEnterpriseId());
                if (e != null) b.setEnterpriseName(e.getName());
            });
        }
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(EnterpriseBase b) { baseMapper.insert(b); tracePageService.evictAllCache(); }

    public void update(Long id, EnterpriseBase b, Long enterpriseId) {
        EnterpriseBase existing = baseMapper.selectById(id);
        if (existing == null) throw new BusinessException("基地不存在");
        if (enterpriseId != null && !existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        b.setId(id);
        if (enterpriseId != null) b.setEnterpriseId(enterpriseId);
        baseMapper.updateById(b);
        tracePageService.evictAllCache();
    }

    public void delete(Long id) {
        baseMapper.deleteById(id);
        tracePageService.evictAllCache();
    }
}
