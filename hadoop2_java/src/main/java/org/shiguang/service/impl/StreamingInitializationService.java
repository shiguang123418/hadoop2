package org.shiguang.service.impl;

import org.shiguang.service.KafkaStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * 流处理初始化服务
 * 用于应用启动时自动初始化Kafka流处理
 */
@Service
@Profile("!test") // 非测试环境下启用
public class StreamingInitializationService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingInitializationService.class);
    
    @Autowired
    private KafkaStreamingService kafkaStreamingService;
    
    @Value("${spark.enabled:false}")
    private boolean sparkEnabled;
    
    @Value("${kafka.streaming.auto-start:false}")
    private boolean autoStartStreaming;
    
    @Value("${kafka.topics.sensor-data:agriculture-sensor-data}")
    private String sensorDataTopic;
    
    @Value("${kafka.topics.weather-data:agriculture-weather-data}")
    private String weatherDataTopic;
    
    /**
     * 应用启动后自动初始化流处理
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeStreaming() {
        if (sparkEnabled && autoStartStreaming) {
            logger.info("应用启动完成，准备自动启动Kafka流处理...");
            
            String[] topics = {sensorDataTopic, weatherDataTopic};
            boolean success = kafkaStreamingService.startStreamProcessing(topics);
            
            if (success) {
                logger.info("成功自动启动Kafka流处理，订阅主题: {}", String.join(", ", topics));
            } else {
                logger.error("自动启动Kafka流处理失败");
            }
        } else {
            logger.info("Kafka流处理自动启动未启用，可通过API手动启动");
        }
    }
}