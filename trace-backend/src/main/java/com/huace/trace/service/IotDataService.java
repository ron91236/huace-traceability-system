package com.huace.trace.service;

import com.huace.trace.entity.IotDevice;
import com.huace.trace.entity.mongo.IotDeviceLatest;
import com.huace.trace.entity.mongo.IotGpsTrack;
import com.huace.trace.entity.mongo.IotSensorData;
import com.huace.trace.mapper.IotDeviceMapper;
import com.huace.trace.repository.IotDeviceLatestRepository;
import com.huace.trace.repository.IotGpsTrackRepository;
import com.huace.trace.repository.IotSensorDataRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IotDataService {

    private final IotSensorDataRepository sensorDataRepository;
    private final IotGpsTrackRepository gpsTrackRepository;
    private final IotDeviceLatestRepository latestRepository;
    private final IotDeviceMapper iotDeviceMapper;

    /**
     * 获取传感器历史数据
     */
    public List<IotSensorData> getSensorHistory(Long deviceId, LocalDateTime from, LocalDateTime to) {
        return sensorDataRepository.findByDeviceIdAndTimeRange(deviceId, from, to);
    }

    /**
     * 获取温度曲线数据 [{time, value}]
     */
    public List<Map<String, Object>> getTemperatureCurve(Long deviceId, LocalDateTime from, LocalDateTime to) {
        List<IotSensorData> dataList = getSensorHistory(deviceId, from, to);
        List<Map<String, Object>> result = new ArrayList<>();
        for (IotSensorData d : dataList) {
            if (d.getMetrics() != null && d.getMetrics().containsKey("temperature")) {
                Map<String, Object> point = new HashMap<>();
                point.put("time", d.getTimestamp());
                point.put("value", d.getMetrics().get("temperature"));
                result.add(point);
            }
        }
        return result;
    }

    /**
     * 获取GPS轨迹 [{lng, lat, time, speed, temperature}]
     */
    public List<Map<String, Object>> getGpsTrack(Long deviceId, LocalDateTime from, LocalDateTime to) {
        List<IotGpsTrack> tracks = gpsTrackRepository.findByDeviceIdAndTimeRange(deviceId, from, to);
        List<Map<String, Object>> result = new ArrayList<>();
        for (IotGpsTrack t : tracks) {
            Map<String, Object> point = new HashMap<>();
            point.put("time", t.getTimestamp());
            if (t.getLocation() != null) {
                @SuppressWarnings("unchecked")
                List<Double> coords = (List<Double>) t.getLocation().get("coordinates");
                if (coords != null && coords.size() >= 2) {
                    point.put("lng", coords.get(0));
                    point.put("lat", coords.get(1));
                }
            }
            point.put("speed", t.getSpeed());
            point.put("temperature", t.getTemperature());
            result.add(point);
        }
        return result;
    }

    /**
     * 获取企业最新读数列表
     */
    public List<Map<String, Object>> getLatestReadings(Long enterpriseId, Long baseId) {
        if (enterpriseId == null) return Collections.emptyList();

        // 查询企业关联设备
        LambdaQueryWrapper<IotDevice> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IotDevice::getEnterpriseId, enterpriseId).eq(IotDevice::getStatus, 1);
        if (baseId != null) {
            wrapper.and(w -> w.eq(IotDevice::getBaseId, baseId).or().isNull(IotDevice::getBaseId));
        }
        List<IotDevice> devices = iotDeviceMapper.selectList(wrapper);

        List<Map<String, Object>> result = new ArrayList<>();
        for (IotDevice device : devices) {
            Optional<IotDeviceLatest> latestOpt = latestRepository.findByDeviceId(device.getId());
            if (latestOpt.isPresent()) {
                IotDeviceLatest latest = latestOpt.get();
                Map<String, Object> reading = new HashMap<>();
                reading.put("deviceId", device.getId());
                reading.put("deviceName", device.getDeviceName());
                reading.put("deviceType", device.getDeviceType());
                reading.put("locationDesc", device.getLocationDesc());
                reading.put("metrics", latest.getMetrics());
                reading.put("location", latest.getLocation());
                reading.put("updatedAt", latest.getUpdatedAt());
                result.add(reading);
            }
        }
        return result;
    }

    /**
     * 为溯源页面获取批次维度的 GPS 轨迹
     */
    public List<Map<String, Object>> getGpsTrackForBatch(Long batchId) {
        if (batchId == null) return Collections.emptyList();
        List<IotDevice> gpsDevices = iotDeviceMapper.selectList(
                new LambdaQueryWrapper<IotDevice>()
                        .eq(IotDevice::getBatchId, batchId)
                        .eq(IotDevice::getDeviceType, "gps_tracker")
                        .eq(IotDevice::getStatus, 1));
        if (gpsDevices.isEmpty()) return Collections.emptyList();

        // 取第一个 GPS 设备的最近 24 小时轨迹
        IotDevice gpsDevice = gpsDevices.get(0);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(24);
        List<Map<String, Object>> track = getGpsTrack(gpsDevice.getId(), from, to);

        // 附加设备信息
        Map<String, Object> info = new HashMap<>();
        info.put("vehiclePlate", gpsDevice.getLocationDesc());
        info.put("deviceName", gpsDevice.getDeviceName());

        if (!track.isEmpty()) {
            Map<String, Object> latest = track.get(track.size() - 1);
            info.put("latestSpeed", latest.get("speed"));
            info.put("latestTemp", latest.get("temperature"));
        }

        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("points", track);
        wrapper.put("info", info);
        result.add(wrapper);
        return result;
    }

    /**
     * 为溯源页面获取批次维度的温度曲线
     */
    public List<Map<String, Object>> getTemperatureCurveForBatch(Long batchId) {
        if (batchId == null) return Collections.emptyList();
        List<IotDevice> coldDevices = iotDeviceMapper.selectList(
                new LambdaQueryWrapper<IotDevice>()
                        .eq(IotDevice::getBatchId, batchId)
                        .in(IotDevice::getDeviceType, "cold_chain", "temp_sensor")
                        .eq(IotDevice::getStatus, 1));
        if (coldDevices.isEmpty()) return Collections.emptyList();

        IotDevice device = coldDevices.get(0);
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusHours(24);
        return getTemperatureCurve(device.getId(), from, to);
    }
}
