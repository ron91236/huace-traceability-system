package com.huace.trace.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.huace.trace.entity.IotAlertRecord;
import com.huace.trace.entity.IotAlertRule;
import com.huace.trace.entity.IotDevice;
import com.huace.trace.entity.mongo.IotDeviceLatest;
import com.huace.trace.entity.mongo.IotSensorData;
import com.huace.trace.mapper.IotAlertRecordMapper;
import com.huace.trace.mapper.IotAlertRuleMapper;
import com.huace.trace.mapper.IotDeviceMapper;
import com.huace.trace.repository.IotDeviceLatestRepository;
import com.huace.trace.repository.IotSensorDataRepository;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 阿里云 IoT AMQP 消息消费者
 * 仅在 iot.enabled=true 时激活
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "iot.enabled", havingValue = "true")
public class IotMessageConsumer {

    private final IotDeviceMapper iotDeviceMapper;
    private final IotSensorDataRepository sensorDataRepository;
    private final IotDeviceLatestRepository latestRepository;
    private final IotAlertRuleMapper alertRuleMapper;
    private final IotAlertRecordMapper alertRecordMapper;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = "${aliyun.iot.amqp.queue-name}")
    public void handleMessage(byte[] messageBody) {
        try {
            JsonNode payload = objectMapper.readTree(messageBody);

            // 从消息中提取设备标识和数据
            String deviceName = payload.path("deviceName").asText("");
            String productKey = payload.path("productKey").asText("");
            JsonNode items = payload.path("items");

            // 查找对应设备
            IotDevice device = iotDeviceMapper.selectOne(
                    new LambdaQueryWrapper<IotDevice>()
                            .eq(IotDevice::getDeviceKey, deviceName)
                            .eq(IotDevice::getProductKey, productKey));

            if (device == null) {
                log.warn("收到未知设备消息: deviceName={}, productKey={}", deviceName, productKey);
                return;
            }

            // 解析传感器指标
            Map<String, Double> metrics = new HashMap<>();
            if (items.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> fields = items.fields();
                while (fields.hasNext()) {
                    Map.Entry<String, JsonNode> field = fields.next();
                    JsonNode value = field.getValue().path("value");
                    if (value.isNumber()) {
                        metrics.put(field.getKey(), value.asDouble());
                    }
                }
            }

            if (metrics.isEmpty()) return;

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
            IotDeviceLatest latest = latestRepository.findByDeviceId(device.getId())
                    .orElse(new IotDeviceLatest());
            latest.setDeviceId(device.getId());
            latest.setEnterpriseId(device.getEnterpriseId());
            latest.setDeviceType(device.getDeviceType());
            latest.setMetrics(metrics);
            latest.setUpdatedAt(LocalDateTime.now());
            latestRepository.save(latest);

            // 检查告警规则
            checkAlertRules(device, metrics);

            log.info("处理IoT消息: device={}, metrics={}", deviceName, metrics);

        } catch (Exception e) {
            log.error("处理IoT消息失败: {}", e.getMessage(), e);
        }
    }

    private void checkAlertRules(IotDevice device, Map<String, Double> metrics) {
        List<IotAlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<IotAlertRule>()
                        .eq(IotAlertRule::getEnterpriseId, device.getEnterpriseId())
                        .eq(IotAlertRule::getStatus, 1)
                        .and(w -> w.eq(IotAlertRule::getDeviceId, device.getId())
                                .or().isNull(IotAlertRule::getDeviceId)));

        for (IotAlertRule rule : rules) {
            Double value = metrics.get(rule.getMetricName());
            if (value == null) continue;

            boolean triggered = false;
            switch (rule.getOperator()) {
                case ">":  triggered = value > rule.getThreshold().doubleValue(); break;
                case "<":  triggered = value < rule.getThreshold().doubleValue(); break;
                case ">=": triggered = value >= rule.getThreshold().doubleValue(); break;
                case "<=": triggered = value <= rule.getThreshold().doubleValue(); break;
                case "==": triggered = Math.abs(value - rule.getThreshold().doubleValue()) < 0.001; break;
            }

            if (triggered) {
                IotAlertRecord record = new IotAlertRecord();
                record.setEnterpriseId(device.getEnterpriseId());
                record.setDeviceId(device.getId());
                record.setRuleId(rule.getId());
                record.setMetricName(rule.getMetricName());
                record.setMetricValue(BigDecimal.valueOf(value));
                record.setThreshold(rule.getThreshold());
                record.setAlertLevel(rule.getAlertLevel());
                record.setAlertMessage(String.format(rule.getAlertMessage() != null ? rule.getAlertMessage() :
                        "指标 %s 当前值 %.2f %s 阈值 %.2f", rule.getMetricName(), value, rule.getOperator(), rule.getThreshold().doubleValue()));
                record.setHandleStatus(0);
                alertRecordMapper.insert(record);
                log.warn("IoT告警触发: device={}, metric={}, value={}, threshold={}",
                        device.getDeviceName(), rule.getMetricName(), value, rule.getThreshold());
            }
        }
    }
}
