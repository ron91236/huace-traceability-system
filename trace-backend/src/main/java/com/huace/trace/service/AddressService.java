package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.Address;
import com.huace.trace.mapper.AddressMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressMapper addressMapper;

    public PageResult<Address> listByEnterprise(int page, int size, Long enterpriseId) {
        LambdaQueryWrapper<Address> w = new LambdaQueryWrapper<>();
        w.eq(Address::getEnterpriseId, enterpriseId).orderByDesc(Address::getId);
        Page<Address> r = addressMapper.selectPage(new Page<>(page, size), w);
        return new PageResult<>(r.getRecords(), r.getTotal());
    }

    public void create(Address a) { addressMapper.insert(a); }

    public void update(Long id, Address a, Long enterpriseId) {
        Address existing = addressMapper.selectById(id);
        if (existing == null) throw new BusinessException("地址不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        a.setId(id);
        a.setEnterpriseId(enterpriseId);
        addressMapper.updateById(a);
    }

    public void delete(Long id, Long enterpriseId) {
        Address existing = addressMapper.selectById(id);
        if (existing == null) throw new BusinessException("地址不存在");
        if (!existing.getEnterpriseId().equals(enterpriseId)) throw new BusinessException("无权限操作");
        addressMapper.deleteById(id);
    }
}
