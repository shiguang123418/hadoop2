package org.shiguang.ws.service;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Kafka消费者服务
 * 用于从Kafka接收传感器数据并转发到WebSocket
 */
@Service
public class KafkaConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group.id}")
    private String groupId;

    @Value("${kafka.topics.agriculture-sensor-data}")
    private String sensorTopic;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private DataProcessingService dataProcessingService;

    private ExecutorService executorService;
    private AtomicBoolean running = new AtomicBoolean(false);
    private AtomicInteger messageCounter = new AtomicInteger(0);
    private AtomicInteger errorCounter = new AtomicInteger(0);

    /**
     * 初始化并启动Kafka消费者
     */
    @PostConstruct
    public void init() {
        logger.info("初始化Kafka消费者服务，服务器: {}, 分组ID: {}, 主题: {}", 
                bootstrapServers, groupId, sensorTopic);
        executorService = Executors.newSingleThreadExecutor();
        start();
    }

    /**
     * 关闭Kafka消费者
     */
    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdown();
        }
        logger.info("Kafka消费者服务已关闭");
    }

    /**
     * 启动Kafka消费
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            logger.info("启动Kafka消费者...");
            executorService.submit(this::consumeMessages);
        } else {
            logger.info("Kafka消费者已经在运行中");
        }
    }

    /**
     * 消费Kafka消息的主循环
     */
    private void consumeMessages() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");

        logger.info("Kafka消费者配置: {}", props);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singletonList(sensorTopic));
            logger.info("Kafka消费者已启动，正在监听主题: {}", sensorTopic);

            // 每分钟报告一次统计信息
            long lastReportTime = System.currentTimeMillis();

            while (running.get()) {
                try {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    if (!records.isEmpty()) {
                        logger.debug("收到 {} 条消息", records.count());
                        
                        for (ConsumerRecord<String, String> record : records) {
                            String key = record.key();
                            String value = record.value();
                            logger.debug("收到消息: key={}, value={}", key, value);
                            
                            try {
                                // 使用数据处理服务处理原始数据
                                String processedData = dataProcessingService.processRawData(value);
                                
                                // 发送到WebSocket
                                messagingTemplate.convertAndSend("/topic/agriculture-sensor-data", processedData);
                                messageCounter.incrementAndGet();
                                logger.debug("已将处理后的数据发送到WebSocket: {}", processedData);
                            } catch (Exception e) {
                                errorCounter.incrementAndGet();
                                logger.error("处理消息时发生错误: {}", e.getMessage(), e);
                            }
                        }
                    }

                    // 每分钟报告一次统计信息
                    long now = System.currentTimeMillis();
                    if (now - lastReportTime > 60000) {
                        logger.info("Kafka消费者统计: 处理消息 {} 条, 错误 {} 条", 
                                messageCounter.get(), errorCounter.get());
                        lastReportTime = now;
                    }
                } catch (Exception e) {
                    logger.error("消费消息时发生错误: {}", e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            logger.error("Kafka消费者发生错误: {}", e.getMessage(), e);
            if (running.get()) {
                // 如果仍然处于运行状态，则尝试重启
                try {
                    logger.info("5秒后尝试重新启动Kafka消费者...");
                    Thread.sleep(5000);
                    consumeMessages();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    logger.error("重启Kafka消费者时被中断");
                }
            }
        }
    }
    
    /**
     * 获取消费者统计信息
     */
    public String getConsumerStats() {
        return String.format("处理消息: %d, 错误: %d", messageCounter.get(), errorCounter.get());
    }
} 