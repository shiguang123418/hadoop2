package org.shiguang.module.kafka.controller;

import org.shiguang.entity.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Kafka相关的REST API
 */
@RestController
@RequestMapping("/kafka")
@CrossOrigin
public class KafkaController {
    private static final Logger logger = LoggerFactory.getLogger(KafkaController.class);
    
    @Value("${kafka.bootstrap.servers}")
    private String kafkaServers;
    
    /**
     * 获取Kafka状态
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getKafkaStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            // 这里应该实现实际的Kafka连接检查
            // 暂时模拟连接成功
            boolean connected = true;
            status.put("connected", connected);
            status.put("url", kafkaServers);
            return ResponseEntity.ok(new ApiResponse<>(200, "Kafka状态获取成功", status));
        } catch (Exception e) {
            logger.error("获取Kafka状态失败", e);
            status.put("connected", false);
            status.put("url", kafkaServers);
            status.put("error", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(200, "Kafka状态获取成功", status));
        }
    }
    
    /**
     * 获取Kafka主题列表
     */
    @GetMapping("/topics")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopics() {
        try {
            // 模拟返回一些示例主题
            List<Map<String, Object>> topics = new ArrayList<>();
            
            Map<String, Object> topic1 = new HashMap<>();
            topic1.put("name", "agriculture-sensor-data");
            topic1.put("partitions", 3);
            topic1.put("replicationFactor", 1);
            topics.add(topic1);
            
            Map<String, Object> topic2 = new HashMap<>();
            topic2.put("name", "agriculture-weather-data");
            topic2.put("partitions", 3);
            topic2.put("replicationFactor", 1);
            topics.add(topic2);
            
            Map<String, Object> topic3 = new HashMap<>();
            topic3.put("name", "agriculture-market-data");
            topic3.put("partitions", 3);
            topic3.put("replicationFactor", 1);
            topics.add(topic3);
            
            return ResponseEntity.ok(new ApiResponse<>(200, "获取主题列表成功", topics));
        } catch (Exception e) {
            logger.error("获取主题列表失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取主题列表失败: " + e.getMessage(), null));
        }
    }
} 