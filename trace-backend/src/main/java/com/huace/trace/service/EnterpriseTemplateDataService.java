package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.entity.EnterpriseTemplateData;
import com.huace.trace.mapper.EnterpriseTemplateDataMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnterpriseTemplateDataService {

    private final EnterpriseTemplateDataMapper dataMapper;

    public List<EnterpriseTemplateData> listByEnterpriseAndTemplate(Long enterpriseId, Long templateId) {
        return dataMapper.selectList(
                new LambdaQueryWrapper<EnterpriseTemplateData>()
                        .eq(EnterpriseTemplateData::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseTemplateData::getTemplateId, templateId));
    }

    public void saveData(Long enterpriseId, Long templateId, List<EnterpriseTemplateData> dataList) {
        // Delete existing data for this enterprise+template
        dataMapper.delete(
                new LambdaQueryWrapper<EnterpriseTemplateData>()
                        .eq(EnterpriseTemplateData::getEnterpriseId, enterpriseId)
                        .eq(EnterpriseTemplateData::getTemplateId, templateId));
        // Insert new data
        for (EnterpriseTemplateData data : dataList) {
            data.setEnterpriseId(enterpriseId);
            data.setTemplateId(templateId);
            data.setId(null);
            dataMapper.insert(data);
        }
    }
}
