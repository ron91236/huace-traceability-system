package com.huace.trace.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * AMQP 消息消费配置
 * 仅在 iot.enabled=true 时激活 RabbitMQ 监听
 * 阿里云 IoT 平台通过 AMQP 协议推送设备上报消息
 */
@Slf4j
@Configuration
@EnableRabbit
@ConditionalOnProperty(name = "iot.enabled", havingValue = "true")
public class AmqpConfig {
    // RabbitMQ 连接由 spring.rabbitmq.* 自动配置
    // 阿里云 IoT AMQP 端点通过 aliyun.iot.amqp.endpoint 配置
    // @RabbitListener 在 IotMessageConsumer 中声明
}
