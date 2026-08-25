package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.CodePackageItem;
import com.huace.trace.entity.OrderCode;
import com.huace.trace.entity.TraceTemplate;
import com.huace.trace.mapper.CodePackageItemMapper;
import com.huace.trace.mapper.OrderCodeMapper;
import com.huace.trace.mapper.TraceTemplateMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TraceTemplateService {

    private final TraceTemplateMapper templateMapper;
    private final OrderCodeMapper orderCodeMapper;
    private final CodePackageItemMapper codePackageItemMapper;
    private final ObjectMapper objectMapper;
    private final TracePageService tracePageService;

    public PageResult<TraceTemplate> list(int page, int size, String keyword) {
        LambdaQueryWrapper<TraceTemplate> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(TraceTemplate::getTemplateName, keyword);
        }
        w.orderByDesc(TraceTemplate::getId);
        Page<TraceTemplate> r = templateMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(TraceTemplate t) {
        t.setTemplateKey("tpl_" + System.currentTimeMillis());
        if (t.getStatus() == null) t.setStatus(1);
        templateMapper.insert(t);
        tracePageService.evictAllCache();
    }

    public void update(Long id, TraceTemplate t) {
        TraceTemplate existing = templateMapper.selectById(id);
        if (existing == null) throw new BusinessException("模板不存在");
        t.setId(id);
        t.setTemplateKey(existing.getTemplateKey()); // 不允许修改 key
        templateMapper.updateById(t);
        tracePageService.evictAllCache();
    }

    public void delete(Long id) {
        TraceTemplate existing = templateMapper.selectById(id);
        if (existing == null) throw new BusinessException("模板不存在");

        Long orderCodeCount = orderCodeMapper.selectCount(
                new LambdaQueryWrapper<OrderCode>()
                        .eq(OrderCode::getTraceTemplate, existing.getTemplateKey()));
        if (orderCodeCount > 0) {
            throw new BusinessException("该模板已被订单条码使用，无法删除");
        }
        Long itemCount = codePackageItemMapper.selectCount(
                new LambdaQueryWrapper<CodePackageItem>()
                        .eq(CodePackageItem::getTraceTemplate, existing.getTemplateKey()));
        if (itemCount > 0) {
            throw new BusinessException("该模板已被码包使用，无法删除");
        }

        templateMapper.deleteById(id);
        tracePageService.evictAllCache();
    }

    public TraceTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    public Map<String, Object> getByKey(String templateKey) {
        TraceTemplate template = templateMapper.selectOne(
                new LambdaQueryWrapper<TraceTemplate>()
                        .eq(TraceTemplate::getTemplateKey, templateKey)
                        .eq(TraceTemplate::getStatus, 1));
        if (template == null) return Collections.emptyMap();

        try {
            Map<String, Object> result = objectMapper.readValue(
                    template.getConfigJson(), new TypeReference<Map<String, Object>>() {});
            result.put("templateKey", template.getTemplateKey());
            result.put("templateName", template.getTemplateName());
            return result;
        } catch (Exception e) {
            return Map.of("templateKey", template.getTemplateKey(),
                    "templateName", template.getTemplateName() != null ? template.getTemplateName() : "");
        }
    }
}
