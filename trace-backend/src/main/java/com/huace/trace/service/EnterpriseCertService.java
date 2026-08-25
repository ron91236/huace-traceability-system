package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.CertType;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.entity.EnterpriseCert;
import com.huace.trace.mapper.CertTypeMapper;
import com.huace.trace.mapper.EnterpriseCertMapper;
import com.huace.trace.mapper.EnterpriseMapper;
import com.huace.trace.mapper.LabelSpecMapper;
import com.huace.trace.entity.LabelSpec;
import com.huace.trace.util.QrCodeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EnterpriseCertService {

    private final EnterpriseCertMapper certMapper;
    private final EnterpriseMapper enterpriseMapper;
    private final CertTypeMapper certTypeMapper;
    private final LabelSpecMapper labelSpecMapper;
    private final TracePageService tracePageService;

    public PageResult<EnterpriseCert> list(int page, int size, String keyword, Long enterpriseId) {
        LambdaQueryWrapper<EnterpriseCert> wrapper = new LambdaQueryWrapper<>();
        if (enterpriseId != null) {
            wrapper.eq(EnterpriseCert::getEnterpriseId, enterpriseId);
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.like(EnterpriseCert::getCertName, keyword)
                    .or().like(EnterpriseCert::getProductName, keyword);
        }
        wrapper.orderByDesc(EnterpriseCert::getId);
        Page<EnterpriseCert> result = certMapper.selectPage(new Page<>(page, size), wrapper);

        // 填充关联名称
        result.getRecords().forEach(cert -> {
            if (cert.getCertTypeId() != null) {
                CertType ct = certTypeMapper.selectById(cert.getCertTypeId());
                if (ct != null) cert.setCertTypeName(ct.getName());
            }
            if (cert.getEnterpriseId() != null) {
                Enterprise e = enterpriseMapper.selectById(cert.getEnterpriseId());
                if (e != null) cert.setEnterpriseName(e.getName());
            }
            if (cert.getLabelSpecId() != null) {
                LabelSpec ls = labelSpecMapper.selectById(cert.getLabelSpecId());
                if (ls != null) cert.setLabelSpecName(ls.getSpecName());
            }
        });
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    public void create(EnterpriseCert cert) {
        certMapper.insert(cert);
        tracePageService.evictAllCache();
    }

    public void update(Long id, EnterpriseCert cert) {
        cert.setId(id);
        certMapper.updateById(cert);
        tracePageService.evictAllCache();
    }

    public EnterpriseCert getById(Long id) {
        return certMapper.selectById(id);
    }

    public void delete(Long id) {
        certMapper.deleteById(id);
        tracePageService.evictAllCache();
    }

    public String generateQrcode(Long id) {
        EnterpriseCert cert = certMapper.selectById(id);
        if (cert == null) throw new BusinessException("认证不存在");

        String content = "https://trace.cti-pit.com/cert/" + id;
        return QrCodeUtil.generateBase64(content, 300, 300);
    }

    public String getCertQrcodeUrl(Long id) {
        return "https://trace.cti-pit.com/cert/" + id;
    }

    public Map<String, Object> getCertPublicInfo(Long id) {
        EnterpriseCert cert = certMapper.selectById(id);
        if (cert == null) throw new BusinessException("认证不存在");

        Enterprise enterprise = enterpriseMapper.selectById(cert.getEnterpriseId());
        CertType certType = certTypeMapper.selectById(cert.getCertTypeId());

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("certName", cert.getCertName());
        result.put("productName", cert.getProductName());
        result.put("enterpriseName", enterprise != null ? enterprise.getName() : "");
        result.put("certTypeName", certType != null ? certType.getName() : "");
        result.put("startDate", cert.getStartDate());
        result.put("endDate", cert.getEndDate());
        result.put("isVoid", cert.getIsVoid());
        result.put("certImage", cert.getCertImage());
        result.put("certPdf", cert.getCertPdf());
        return result;
    }
}
