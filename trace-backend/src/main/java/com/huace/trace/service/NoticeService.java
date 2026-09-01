package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.Notice;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.NoticeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NoticeService {
    private final NoticeMapper noticeMapper;
    private final EnterpriseMapper enterpriseMapper;

    @Cacheable(value = "adminList", key = "'notice:' + #page + ':' + #size + ':' + (#keyword == null ? '' : #keyword) + ':' + (#enterpriseId == null ? 'all' : #enterpriseId)")
    public PageResult<Notice> list(int page, int size, String keyword, Long enterpriseId) {
        LambdaQueryWrapper<Notice> w = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            w.and(q -> q.eq(Notice::getEnterpriseId, enterpriseId).or().isNull(Notice::getEnterpriseId));
        }
        if (StringUtils.hasText(keyword)) w.like(Notice::getTitle, keyword);
        w.orderByDesc(Notice::getId);
        Page<Notice> r = noticeMapper.selectPage(new Page<>(page, size), w);
        List<Notice> records = r.getRecords();
        if (!records.isEmpty()) {
            // 批量预取 enterprise，消除逐行查询
            java.util.Set<Long> entIds = records.stream().map(Notice::getEnterpriseId)
                    .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet());
            Map<Long, Enterprise> entMap = entIds.isEmpty() ? java.util.Collections.emptyMap()
                    : enterpriseMapper.selectBatchIds(entIds).stream()
                            .collect(java.util.stream.Collectors.toMap(Enterprise::getId, e -> e));
            records.forEach(n -> {
                Enterprise e = entMap.get(n.getEnterpriseId());
                if (e != null) n.setEnterpriseName(e.getName());
            });
        }
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    @CacheEvict(value = "adminList", allEntries = true)
    public void create(Notice n) { noticeMapper.insert(n); }

    @CacheEvict(value = "adminList", allEntries = true)
    public void delete(Long id) { noticeMapper.deleteById(id); }
}
