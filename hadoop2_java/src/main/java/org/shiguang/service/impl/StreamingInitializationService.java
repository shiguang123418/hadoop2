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
@Profile("!test") // 非测试环境
public class StreamingInitializationService {

    private static final Logger logger = LoggerFactory.getLogger(StreamingInitializationService.class);
    
    @Autowired(required = false)
    private KafkaStreamingService kafkaStreamingService;
    
    @Value("${kafka.streaming.auto-start:false}") // 改为默认禁用
    private boolean autoStartStreaming;
    
    @Value("${kafka.topics:agriculture-sensor-data,agriculture-weather-data}")
    private String[] topicNames;
    
    /**
     * 在应用程序启动完成后初始化流处理
     * 此方法仅在非测试环境中执行
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeStreaming() {
        if (!autoStartStreaming) {
            logger.info("Kafka流处理自动启动已禁用");
            return;
        }
        
        // 如果StreamingService不可用，跳过
        if (kafkaStreamingService == null) {
            logger.info("KafkaStreamingService不可用，跳过流处理初始化");
            return;
        }

        logger.info("应用启动完成，准备自动启动Kafka流处理...");
        
        try {
            kafkaStreamingService.startStreamProcessing(topicNames);
        } catch (Exception e) {
            logger.error("自动启动Kafka流处理失败", e);
        }
    }
}