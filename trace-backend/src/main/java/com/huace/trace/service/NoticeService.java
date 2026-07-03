package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.Notice;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeMapper noticeMapper;
    private final EnterpriseMapper enterpriseMapper;

    public PageResult<Notice> list(int page, int size, String keyword, Long enterpriseId) {
        LambdaQueryWrapper<Notice> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            w.and(q -> q.eq(Notice::getEnterpriseId, enterpriseId).or().isNull(Notice::getEnterpriseId));
        }
        if (StringUtils.hasText(keyword)) w.like(Notice::getTitle, keyword);
        w.orderByDesc(Notice::getId);
        Page<Notice> r = noticeMapper.selectPage(new Page<>(page, size), w);
        r.getRecords().forEach(n -> {
            if (n.getEnterpriseId() != null) {
                Enterprise e = enterpriseMapper.selectById(n.getEnterpriseId());
                if (e != null) n.setEnterpriseName(e.getName());
            }
        });
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(Notice n) { noticeMapper.insert(n); }
    public void delete(Long id) { noticeMapper.deleteById(id); }
}
