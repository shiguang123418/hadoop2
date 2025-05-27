package org.shiguang.controller;

import org.shiguang.module.kafka.client.KafkaClient;
import org.shiguang.module.spark.service.SparkStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/realtime")
public class RealtimeDataController {
    private static final Logger logger = LoggerFactory.getLogger(RealtimeDataController.class);
    
    @Autowired
    private SparkStreamingService sparkStreamingService;
    
    @Autowired
    private KafkaClient kafkaClient;
    
    /**
     * 将CSV数据加载到Kafka主题
     */
    @PostMapping("/load/weather")
    public ResponseEntity<?> loadWeatherData(
            @RequestParam("filePath") String filePath,
            @RequestParam("topicName") String topicName) {
        
        try {
            // 创建Kafka主题
            boolean created = kafkaClient.createTopic(topicName, 1, (short) 1);
            logger.info("Kafka主题 {} 创建状态: {}", topicName, created ? "成功" : "已存在");
            
            // 将CSV文件内容加载到Kafka
            int recordCount = loadCsvToKafka(filePath, topicName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "成功加载 " + recordCount + " 条天气数据到Kafka主题: " + topicName);
            response.put("topic", topicName);
            response.put("recordCount", recordCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("加载天气数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "加载数据失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 将CSV数据加载到Kafka主题
     */
    @PostMapping("/load/products")
    public ResponseEntity<?> loadProductData(
            @RequestParam("filePath") String filePath,
            @RequestParam("topicName") String topicName) {
        
        try {
            // 创建Kafka主题
            boolean created = kafkaClient.createTopic(topicName, 1, (short) 1);
            logger.info("Kafka主题 {} 创建状态: {}", topicName, created ? "成功" : "已存在");
            
            // 将CSV文件内容加载到Kafka
            int recordCount = loadCsvToKafka(filePath, topicName);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "成功加载 " + recordCount + " 条产品数据到Kafka主题: " + topicName);
            response.put("topic", topicName);
            response.put("recordCount", recordCount);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("加载产品数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "加载数据失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 启动天气数据流处理
     */
    @PostMapping("/start/weather")
    public ResponseEntity<?> startWeatherDataStreaming(
            @RequestParam("topicName") String topicName,
            @RequestParam(value = "groupId", defaultValue = "weather-consumer-group") String groupId,
            @RequestParam(value = "batchInterval", defaultValue = "5") int batchIntervalSeconds) {
        
        try {
            sparkStreamingService.startWeatherDataStreaming(topicName, groupId, batchIntervalSeconds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "天气数据流处理已启动");
            response.put("topic", topicName);
            response.put("groupId", groupId);
            response.put("batchInterval", batchIntervalSeconds);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("启动天气数据流处理失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "启动失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 启动产品数据流处理
     */
    @PostMapping("/start/products")
    public ResponseEntity<?> startProductDataStreaming(
            @RequestParam("topicName") String topicName,
            @RequestParam(value = "groupId", defaultValue = "product-consumer-group") String groupId,
            @RequestParam(value = "batchInterval", defaultValue = "5") int batchIntervalSeconds) {
        
        try {
            sparkStreamingService.startProductDataStreaming(topicName, groupId, batchIntervalSeconds);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "产品数据流处理已启动");
            response.put("topic", topicName);
            response.put("groupId", groupId);
            response.put("batchInterval", batchIntervalSeconds);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("启动产品数据流处理失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "启动失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 停止所有流处理作业
     */
    @PostMapping("/stop")
    public ResponseEntity<?> stopStreaming() {
        try {
            sparkStreamingService.stopStreaming();
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "实时流处理已停止");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("停止流处理失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "停止失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * 检查流处理状态
     */
    @GetMapping("/status")
    public ResponseEntity<?> checkStreamingStatus() {
        boolean isRunning = sparkStreamingService.isStreamingRunning();
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("isRunning", isRunning);
        response.put("message", isRunning ? "流处理正在运行" : "流处理未运行");
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 发送消息到WebSocket
     * 用于测试WebSocket连接
     */
    @MessageMapping("/send-test")
    @SendTo("/topic/test")
    public String sendTest(String message) {
        return "服务器收到: " + message;
    }
    
    /**
     * 辅助方法：将CSV文件内容加载到Kafka主题
     */
    private int loadCsvToKafka(String filePath, String topicName) throws Exception {
        // 读取CSV文件并将每行发送到Kafka
        java.nio.file.Path path = java.nio.file.Paths.get(filePath);
        java.util.List<String> lines = java.nio.file.Files.readAllLines(path);
        
        int recordCount = 0;
        // 发送标题行
        String header = lines.get(0);
        kafkaClient.sendMessage(topicName, "header", header);
        
        // 发送数据行
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            kafkaClient.sendMessage(topicName, String.valueOf(i), line);
            recordCount++;
        }
        
        return recordCount;
    }
} 