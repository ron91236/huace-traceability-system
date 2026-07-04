package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.LabelSpec;
import com.huace.trace.mapper.LabelSpecMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelSpecService {
    private final LabelSpecMapper labelSpecMapper;

    public PageResult<LabelSpec> list(int page, int size, String keyword) {
        LambdaQueryWrapper<LabelSpec> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) w.like(LabelSpec::getSpecName, keyword);
        w.orderByDesc(LabelSpec::getId);
        Page<LabelSpec> r = labelSpecMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public List<LabelSpec> all() {
        return labelSpecMapper.selectList(new LambdaQueryWrapper<LabelSpec>()
                .eq(LabelSpec::getIsVoid, 0).orderByDesc(LabelSpec::getId));
    }

    public LabelSpec getById(Long id) { return labelSpecMapper.selectById(id); }
    public void create(LabelSpec s) { labelSpecMapper.insert(s); }
    public void update(Long id, LabelSpec s) { s.setId(id); labelSpecMapper.updateById(s); }
    public void delete(Long id) { labelSpecMapper.deleteById(id); }
}
