package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huace.trace.common.PageResult;
import com.huace.trace.entity.IotDevice;
import com.huace.trace.entity.mongo.IotDeviceLatest;
import com.huace.trace.mapper.IotDeviceMapper;
import com.huace.trace.repository.IotDeviceLatestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class IotDeviceService {

    private final IotDeviceMapper iotDeviceMapper;
    private final IotDeviceLatestRepository latestRepository;

    public PageResult<IotDevice> listByEnterprise(Long enterpriseId, String deviceType, int page, int size) {
        LambdaQueryWrapper<IotDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotDevice::getEnterpriseId, enterpriseId);
        if (deviceType != null && !deviceType.isEmpty()) {
            wrapper.eq(IotDevice::getDeviceType, deviceType);
        }
        wrapper.orderByDesc(IotDevice::getCreatedAt);
        Page<IotDevice> p = iotDeviceMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getRecords(), p.getTotal());
    }

    public IotDevice registerDevice(IotDevice device) {
        iotDeviceMapper.insert(device);
        return device;
    }

    public IotDevice updateDevice(Long id, IotDevice device) {
        device.setId(id);
        iotDeviceMapper.updateById(device);
        return iotDeviceMapper.selectById(id);
    }

    public void deleteDevice(Long id) {
        iotDeviceMapper.deleteById(id);
    }

    public IotDevice getById(Long id) {
        return iotDeviceMapper.selectById(id);
    }

    public IotDeviceLatest getDeviceLatest(Long deviceId) {
        return latestRepository.findByDeviceId(deviceId).orElse(null);
    }

    public List<IotDevice> listByBase(Long baseId) {
        return iotDeviceMapper.selectList(
                new LambdaQueryWrapper<IotDevice>().eq(IotDevice::getBaseId, baseId));
    }

    public List<IotDevice> listByBatch(Long batchId) {
        return iotDeviceMapper.selectList(
                new LambdaQueryWrapper<IotDevice>().eq(IotDevice::getBatchId, batchId));
    }

    /**
     * 管理端查询全部设备
     */
    public List<IotDevice> listAll() {
        return iotDeviceMapper.selectList(
                new LambdaQueryWrapper<IotDevice>().orderByDesc(IotDevice::getCreatedAt));
    }
}
