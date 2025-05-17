package org.shiguang.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.shiguang.entity.SensorReading;
import org.shiguang.repository.SensorReadingRepository;
import org.shiguang.service.KafkaStreamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * KafkaStreamingService的模拟实现
 * 当spark.use-mock=true时使用此实现
 * 此实现不使用Spark但会实际消费Kafka消息并保存到数据库
 */
@Service
@Primary
@ConditionalOnProperty(name = "spark.use-mock", havingValue = "true")
public class MockKafkaStreamingServiceImpl implements KafkaStreamingService {

    private final AtomicBoolean isStreaming = new AtomicBoolean(false);
    private ExecutorService executorService;
    private ObjectMapper objectMapper;
    
    @Autowired
    private SensorReadingRepository sensorReadingRepository;
    
    @Value("${kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id:agri-data-group}")
    private String groupId;

    @Value("${kafka.consumer.auto-offset-reset:latest}")
    private String autoOffsetReset;
    
    public MockKafkaStreamingServiceImpl() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 支持Java 8日期时间类型
    }

    @Override
    public boolean startStreamProcessing(String[] topics) {
        if (isStreaming.get()) {
            return false;
        }
        
        try {
            // 创建一个单独的线程来处理Kafka消息
            executorService = Executors.newSingleThreadExecutor();
            executorService.submit(() -> processKafkaMessages(topics));
            
            isStreaming.set(true);
            return true;
        } catch (Exception e) {
            System.err.println("启动流处理失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 创建Kafka消费者配置
     */
    private Properties getKafkaConsumerProps() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        return props;
    }
    
    /**
     * 实际处理Kafka消息的方法
     */
    private void processKafkaMessages(String[] topics) {
        try {
            Properties props = getKafkaConsumerProps();
            System.out.println("Kafka消费者配置: " + props);
            
            try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
                System.out.println("Kafka消费者创建成功，开始订阅主题: " + Arrays.toString(topics));
                consumer.subscribe(Arrays.asList(topics));
                
                while (isStreaming.get()) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    
                    if (!records.isEmpty()) {
                        System.out.println("收到 " + records.count() + " 条消息");
                    }
                    
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            System.out.println("处理消息: " + record.value());
                            // 解析JSON消息到Map
                            Map<String, Object> dataMap = objectMapper.readValue(record.value(), Map.class);
                            
                            // 创建传感器读数实体并保存
                            SensorReading reading = new SensorReading();
                            reading.setSensorId((String) dataMap.get("sensorId"));
                            reading.setSensorType((String) dataMap.get("sensorType"));
                            reading.setRegion((String) dataMap.get("region"));
                            reading.setCropType((String) dataMap.get("cropType"));
                            
                            // 处理数值，确保是double类型
                            Object valueObj = dataMap.get("value");
                            double value;
                            if (valueObj instanceof Number) {
                                value = ((Number) valueObj).doubleValue();
                            } else if (valueObj instanceof String) {
                                value = Double.parseDouble((String) valueObj);
                            } else {
                                value = 0.0;
                            }
                            reading.setValue(value);
                            
                            // 处理时间戳
                            String timestampStr = (String) dataMap.get("timestamp");
                            LocalDateTime timestamp = parseTimestamp(timestampStr);
                            reading.setTimestamp(timestamp);
                            
                            reading.setUnit((String) dataMap.get("unit"));
                            reading.setDescription((String) dataMap.get("description"));
                            
                            // 简单异常检测：如果值小于0或大于100，标记为异常
                            reading.setAnomaly(value < 0 || value > 100);
                            
                            // 保存到数据库
                            sensorReadingRepository.save(reading);
                            System.out.println("成功保存传感器数据: " + reading.getSensorId());
                            
                        } catch (Exception e) {
                            // 静默处理异常，不中断消费
                            System.err.println("处理消息异常: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // 发生异常时退出
            isStreaming.set(false);
            System.err.println("Kafka消费异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 解析各种时间戳格式为LocalDateTime
     */
    private LocalDateTime parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // 尝试直接解析ISO格式
            return LocalDateTime.parse(timestampStr);
        } catch (DateTimeParseException e) {
            try {
                // 尝试解析包含T和毫秒的格式
                return LocalDateTime.parse(timestampStr, DateTimeFormatter.ISO_DATE_TIME);
            } catch (DateTimeParseException e2) {
                try {
                    // 尝试解析不包含T的格式
                    return LocalDateTime.parse(timestampStr.replace("T", " ").split("\\.")[0], 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (Exception e3) {
                    // 所有尝试失败，返回当前时间
                    return LocalDateTime.now();
                }
            }
        }
    }

    @Override
    public boolean stopStreamProcessing() {
        if (!isStreaming.get()) {
            return false;
        }
        
        isStreaming.set(false);
        
        if (executorService != null) {
            executorService.shutdownNow();
        }
        
        return true;
    }

    @Override
    public boolean isStreamingActive() {
        return isStreaming.get();
    }

    @Override
    public JavaStreamingContext getStreamingContext() {
        return null;
    }
} 