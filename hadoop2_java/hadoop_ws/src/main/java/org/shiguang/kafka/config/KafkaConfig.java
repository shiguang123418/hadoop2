package org.shiguang.kafka.config;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka相关配置类
 */
@Configuration
@PropertySource(value = "classpath:kafka-config.properties", ignoreResourceNotFound = true)
public class KafkaConfig {

    @Value("${kafka.bootstrap.servers:192.168.1.192:9092}")
    private String bootstrapServers;

    @Value("${kafka.default.topic.partitions:3}")
    private int defaultPartitions;

    @Value("${kafka.default.topic.replication:1}")
    private short defaultReplication;

    /**
     * 创建Kafka管理客户端
     * @return AdminClient实例
     */
    @Bean
    public AdminClient kafkaAdminClient() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        return AdminClient.create(configs);
    }

    /**
     * 创建默认主题Bean - 可以根据需要创建多个主题
     * @return 默认主题配置
     */
    @Bean
    public NewTopic defaultTopic() {
        return new NewTopic("default-topic", defaultPartitions, defaultReplication);
    }

    /**
     * 创建数据采集主题Bean
     * @return 数据采集主题配置
     */
    @Bean
    public NewTopic dataCollectionTopic() {
        return new NewTopic("data-collection", defaultPartitions, defaultReplication);
    }

    /**
     * 创建实时处理主题Bean
     * @return 实时处理主题配置
     */
    @Bean
    public NewTopic realTimeProcessingTopic() {
        return new NewTopic("real-time-processing", defaultPartitions, defaultReplication);
    }
} 