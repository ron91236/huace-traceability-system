package com.huace.trace.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.huace.trace.entity.IotDevice;
import com.huace.trace.entity.mongo.IotDeviceLatest;
import com.huace.trace.entity.mongo.IotGpsTrack;
import com.huace.trace.entity.mongo.IotSensorData;
import com.huace.trace.mapper.IotDeviceMapper;
import com.huace.trace.repository.IotDeviceLatestRepository;
import com.huace.trace.repository.IotGpsTrackRepository;
import com.huace.trace.repository.IotSensorDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * IoT 模拟数据生成器
 * 当 iot.mock-enabled=true 时激活，定时生成传感器数据和GPS轨迹
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "iot.mock-enabled", havingValue = "true", matchIfMissing = true)
public class IotMockDataService {

    private final IotDeviceMapper iotDeviceMapper;
    private final IotSensorDataRepository sensorDataRepository;
    private final IotGpsTrackRepository gpsTrackRepository;
    private final IotDeviceLatestRepository latestRepository;

    private final Random random = new Random();

    // GPS 轨迹状态（模拟行驶路径）
    private double gpsLng = 104.0657;  // 成都起点
    private double gpsLat = 30.6595;
    private final double targetLng = 106.55;  // 重庆终点
    private final double targetLat = 29.56;

    /**
     * 每分钟生成传感器数据
     */
    @Scheduled(fixedRate = 60000)
    public void generateMockSensorData() {
        List<IotDevice> devices = iotDeviceMapper.selectList(
                new LambdaQueryWrapper<IotDevice>()
                        .in(IotDevice::getDeviceType, "soil_sensor", "temp_sensor", "cold_chain")
                        .eq(IotDevice::getStatus, 1));

        for (IotDevice device : devices) {
            Map<String, Double> metrics = new HashMap<>();
            double hourAngle = (LocalDateTime.now().getHour() * 2.0 * Math.PI) / 24.0;

            switch (device.getDeviceType()) {
                case "soil_sensor":
                    metrics.put("temperature", 25.0 + 5.0 * Math.sin(hourAngle) + randomGaussian(0.5));
                    metrics.put("humidity", 60.0 + 15.0 * Math.sin(hourAngle + 1) + randomGaussian(1));
                    metrics.put("soilMoisture", 40.0 + 10.0 * Math.sin(hourAngle + 2) + randomGaussian(0.5));
                    metrics.put("ph", 6.5 + randomGaussian(0.3));
                    break;
                case "temp_sensor":
                    metrics.put("temperature", 25.0 + 5.0 * Math.sin(hourAngle) + randomGaussian(0.5));
                    metrics.put("humidity", 60.0 + 15.0 * Math.sin(hourAngle + 1) + randomGaussian(1));
                    break;
                case "cold_chain":
                    metrics.put("temperature", -18.0 + 3.0 * Math.sin(hourAngle * 3) + randomGaussian(0.3));
                    break;
            }

            // 写入时序集合
            IotSensorData sensorData = new IotSensorData();
            sensorData.setTimestamp(LocalDateTime.now());
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("enterpriseId", device.getEnterpriseId());
            metadata.put("deviceId", device.getId());
            metadata.put("deviceType", device.getDeviceType());
            metadata.put("baseId", device.getBaseId());
            sensorData.setMetadata(metadata);
            sensorData.setMetrics(metrics);
            sensorDataRepository.save(sensorData);

            // 更新 latest
            updateDeviceLatest(device, metrics, null);
        }

        if (!devices.isEmpty()) {
            log.debug("Mock: 生成 {} 个设备的传感器数据", devices.size());
        }
    }

    /**
     * 每 30 秒生成 GPS 轨迹点
     */
    @Scheduled(fixedRate = 30000)
    public void generateMockGpsPoints() {
        List<IotDevice> gpsDevices = iotDeviceMapper.selectList(
                new LambdaQueryWrapper<IotDevice>()
                        .eq(IotDevice::getDeviceType, "gps_tracker")
                        .eq(IotDevice::getStatus, 1));

        for (IotDevice device : gpsDevices) {
            // 向终点移动
            double dLng = targetLng - gpsLng;
            double dLat = targetLat - gpsLat;
            double dist = Math.sqrt(dLng * dLng + dLat * dLat);

            if (dist > 0.01) {
                double step = 0.005;  // 每步移动约 0.5km
                gpsLng += dLng / dist * step + randomGaussian(0.001);
                gpsLat += dLat / dist * step + randomGaussian(0.001);
            } else {
                // 到达终点后重置
                gpsLng = 104.0657;
                gpsLat = 30.6595;
            }

            double speed = 30 + random.nextDouble() * 60;  // 30-90 km/h
            double coldTemp = -18.0 + randomGaussian(1.5);

            IotGpsTrack track = new IotGpsTrack();
            track.setTimestamp(LocalDateTime.now());

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("enterpriseId", device.getEnterpriseId());
            metadata.put("deviceId", device.getId());
            metadata.put("batchId", device.getBatchId());
            metadata.put("vehiclePlate", device.getLocationDesc());
            track.setMetadata(metadata);

            Map<String, Object> location = new HashMap<>();
            location.put("type", "Point");
            location.put("coordinates", List.of(gpsLng, gpsLat));
            track.setLocation(location);

            track.setSpeed(Math.round(speed * 10.0) / 10.0);
            track.setDirection((int) (Math.atan2(dLat, dLng) * 180 / Math.PI));
            track.setTemperature(Math.round(coldTemp * 10.0) / 10.0);

            gpsTrackRepository.save(track);

            // 更新 latest 位置
            Map<String, Double> metrics = new HashMap<>();
            metrics.put("speed", track.getSpeed());
            metrics.put("temperature", track.getTemperature());
            updateDeviceLatest(device, metrics, location);
        }

        if (!gpsDevices.isEmpty()) {
            log.debug("Mock: 生成 {} 个GPS轨迹点 (lng={}, lat={})", gpsDevices.size(),
                    String.format("%.4f", gpsLng), String.format("%.4f", gpsLat));
        }
    }

    private void updateDeviceLatest(IotDevice device, Map<String, Double> metrics, Map<String, Object> location) {
        IotDeviceLatest latest = latestRepository.findByDeviceId(device.getId())
                .orElse(new IotDeviceLatest());
        latest.setDeviceId(device.getId());
        latest.setEnterpriseId(device.getEnterpriseId());
        latest.setDeviceType(device.getDeviceType());
        latest.setMetrics(metrics);
        latest.setLocation(location);
        latest.setUpdatedAt(LocalDateTime.now());
        latestRepository.save(latest);
    }

    private double randomGaussian(double scale) {
        return random.nextGaussian() * scale;
    }
}
