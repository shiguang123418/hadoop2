package org.shiguang.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Kafka-WebSocket模块的自动配置类
 * 自动扫描并注册所有组件
 */
@Configuration
@ComponentScan(basePackages = {
    "org.shiguang.config",
    "org.shiguang.spark",
    "org.shiguang.kafka.service"
})
public class KafkaWsAutoConfiguration {
    // 配置在ComponentScan注解中完成
} 