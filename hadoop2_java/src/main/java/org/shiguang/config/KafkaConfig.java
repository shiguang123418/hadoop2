package org.shiguang.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka配置类
 * 用于配置Kafka消费者参数
 */
@Configuration
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    // 添加对点分格式配置的支持
    @Value("${kafka.bootstrap.servers:${kafka.bootstrap-servers:localhost:9092}}")
    private String bootstrapServersWithDots;

    @Value("${kafka.consumer.group-id:agri-data-group}")
    private String groupId;

    @Value("${kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;

    @Value("${kafka.enable.auto.commit:true}")
    private String enableAutoCommit;

    @Value("${kafka.auto.commit.interval.ms:1000}")
    private String autoCommitIntervalMs;

    /**
     * Kafka消费者配置
     * @return Kafka消费者配置参数Map
     */
    @Bean
    public Map<String, Object> kafkaConsumerConfig() {
        // 优先使用点分格式的配置，这是Kafka Java客户端期望的格式
        String effectiveBootstrapServers = bootstrapServersWithDots;
        logger.info("初始化Kafka消费者配置: bootstrapServers={}, groupId={}", effectiveBootstrapServers, groupId);
        
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, effectiveBootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, autoCommitIntervalMs);
        
        // 确保props中的每个键值对都是字符串，避免可能的类型不匹配
        for (Map.Entry<String, Object> entry : props.entrySet()) {
            if (entry.getValue() != null && !(entry.getValue() instanceof String)) {
                props.put(entry.getKey(), entry.getValue().toString());
            }
        }
        
        return props;
    }
} 