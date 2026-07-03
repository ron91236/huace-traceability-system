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

@Service
@RequiredArgsConstructor
public class EnterpriseBaseService {
    private final EnterpriseBaseMapper baseMapper;
    private final EnterpriseMapper enterpriseMapper;

    public PageResult<EnterpriseBase> list(int page, int size, String keyword, Long enterpriseId) {
        LambdaQueryWrapper<EnterpriseBase> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) w.eq(EnterpriseBase::getEnterpriseId, enterpriseId);
        if (StringUtils.hasText(keyword)) w.like(EnterpriseBase::getName, keyword);
        w.orderByDesc(EnterpriseBase::getId);
        Page<EnterpriseBase> r = baseMapper.selectPage(new Page<>(page, size), w);
        r.getRecords().forEach(b -> {
            Enterprise e = enterpriseMapper.selectById(b.getEnterpriseId());
            if (e != null) b.setEnterpriseName(e.getName());
        });
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(EnterpriseBase b) { baseMapper.insert(b); }

    public void update(Long id, EnterpriseBase b, Long enterpriseId) {
        EnterpriseBase existing = baseMapper.selectById(id);
        if (existing == null) throw new BusinessException("基地不存在");
        if (enterpriseId != null && !existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        b.setId(id);
        if (enterpriseId != null) b.setEnterpriseId(enterpriseId);
        baseMapper.updateById(b);
    }

    public void delete(Long id) {
        baseMapper.deleteById(id);
    }
}
