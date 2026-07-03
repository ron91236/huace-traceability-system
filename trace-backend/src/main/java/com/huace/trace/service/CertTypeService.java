package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.CertType;
import com.huace.trace.mapper.CertTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertTypeService {

    private final CertTypeMapper certTypeMapper;

    public PageResult<CertType> list(int page, int size, String keyword) {
        LambdaQueryWrapper<CertType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CertType::getName, keyword);
        }
        wrapper.orderByDesc(CertType::getId);
        Page<CertType> result = certTypeMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    public void create(CertType certType) {
        certTypeMapper.insert(certType);
    }

    public void update(Long id, CertType certType) {
        certType.setId(id);
        certTypeMapper.updateById(certType);
    }

    public void delete(Long id) {
        certTypeMapper.deleteById(id);
    }

    public void batchDelete(List<Long> ids) {
        certTypeMapper.deleteBatchIds(ids);
    }
}
package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.BusinessException;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.CertType;
import com.huace.trace.mapper.CertTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CertTypeService {

    private final CertTypeMapper certTypeMapper;

    public PageResult<CertType> list(int page, int size, String keyword) {
        LambdaQueryWrapper<CertType> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(CertType::getName, keyword);
        }
        wrapper.orderByDesc(CertType::getId);
        Page<CertType> result = certTypeMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    public List<CertType> all() {
        return certTypeMapper.selectList(new LambdaQueryWrapper<CertType>().orderByDesc(CertType::getId));
    }

    public void create(CertType certType) {
        certTypeMapper.insert(certType);
    }

    public void update(Long id, CertType certType) {
        certType.setId(id);
        certTypeMapper.updateById(certType);
    }

    public void delete(Long id) {
        certTypeMapper.deleteById(id);
    }

    public void batchDelete(List<Long> ids) {
        certTypeMapper.deleteBatchIds(ids);
    }
}
