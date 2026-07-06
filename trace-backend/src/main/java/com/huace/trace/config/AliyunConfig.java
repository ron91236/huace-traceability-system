package com.huace.trace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云 SDK 配置（IoT 平台 + 视频监控）
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun")
public class AliyunConfig {

    private IotConfig iot = new IotConfig();
    private VideoConfig video = new VideoConfig();

    @Data
    public static class IotConfig {
        private String accessKeyId;
        private String accessKeySecret;
        private String endpoint = "iot.cn-shanghai.aliyuncs.com";
        private String instanceId;
        private AmqpConfig amqp = new AmqpConfig();

        @Data
        public static class AmqpConfig {
            private String endpoint;
            private String groupId = "DEFAULT_GROUP";
            private String queueName = "iot-trace-queue";
        }
    }

    @Data
    public static class VideoConfig {
        private String regionId = "cn-shanghai";
        private String endpoint = "vs.cn-shanghai.aliyuncs.com";
    }
}
