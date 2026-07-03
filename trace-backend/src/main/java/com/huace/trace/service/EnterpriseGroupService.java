package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.common.BusinessException;
import com.huace.trace.entity.Enterprise;
import com.huace.trace.mapper.EnterpriseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EnterpriseGroupService {

    private final EnterpriseMapper enterpriseMapper;

    public void createMasterEnterprise(Enterprise enterprise) {
        enterprise.setAccountLevel("master");
        enterprise.setParentId(null);
        enterpriseMapper.insert(enterprise);
    }

    public void createChildEnterprise(Long parentEnterpriseId, Enterprise child) {
        Enterprise parent = enterpriseMapper.selectById(parentEnterpriseId);
        if (parent == null || !"master".equals(parent.getAccountLevel())) {
            throw new BusinessException("母账号不存在或类型不正确");
        }
        child.setParentId(parentEnterpriseId);
        child.setAccountLevel("child");
        enterpriseMapper.insert(child);
    }

    public List<Enterprise> listChildEnterprises(Long parentEnterpriseId) {
        return enterpriseMapper.selectList(
                new LambdaQueryWrapper<Enterprise>()
                        .eq(Enterprise::getParentId, parentEnterpriseId));
    }

    public void removeChildEnterprise(Long parentEnterpriseId, Long childId) {
        Enterprise child = enterpriseMapper.selectById(childId);
        if (child == null || !parentEnterpriseId.equals(child.getParentId())) {
            throw new BusinessException("子企业不存在");
        }
        child.setParentId(null);
        child.setAccountLevel("standalone");
        enterpriseMapper.updateById(child);
    }

    public List<Long> getGroupEnterpriseIds(Long enterpriseId) {
        Enterprise enterprise = enterpriseMapper.selectById(enterpriseId);
        if (enterprise == null) return List.of(enterpriseId);

        if ("master".equals(enterprise.getAccountLevel())) {
            List<Long> ids = new ArrayList<>();
            ids.add(enterpriseId);
            List<Enterprise> children = enterpriseMapper.selectList(
                    new LambdaQueryWrapper<Enterprise>()
                            .eq(Enterprise::getParentId, enterpriseId)
                            .select(Enterprise::getId));
            children.forEach(c -> ids.add(c.getId()));
            return ids;
        }
        return List.of(enterpriseId);
    }
}
