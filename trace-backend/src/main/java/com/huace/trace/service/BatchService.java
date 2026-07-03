package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.*;
import com.huace.trace.mapper.*;
import com.huace.trace.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class BatchService {
    private final BatchMapper batchMapper;
    private final GoodsMapper goodsMapper;
    private final EnterpriseBaseMapper baseMapper;
    private final EnterpriseMapper enterpriseMapper;

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
        });
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(Batch b) { batchMapper.insert(b); }
    public void update(Long id, Batch b, Long enterpriseId) {
        Batch existing = batchMapper.selectById(id);
        if (existing == null) throw new BusinessException("批次不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        b.setId(id);
        b.setEnterpriseId(enterpriseId);
        batchMapper.updateById(b);
    }

    public String generateQrcode(Long id, Long enterpriseId) {
        Batch batch = batchMapper.selectById(id);
        if (batch == null) throw new BusinessException("批次不存在");
        if (!batch.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        String traceUrl = baseUrl + "/trace/batch/" + id;
        return QrCodeUtil.generateBase64(traceUrl, 300, 300);
    }
}
