package org.shiguang.ws.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.shiguang.ws.service.DataGeneratorService;
import org.shiguang.ws.service.KafkaConsumerService;
import org.shiguang.ws.service.SparkStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统状态控制器
 * 提供系统各组件状态检查API
 */
@RestController
@RequestMapping("/system")
public class SystemStatusController {

    private static final Logger logger = LoggerFactory.getLogger(SystemStatusController.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Autowired
    private KafkaConsumerService kafkaConsumerService;
    
    @Autowired
    private DataGeneratorService dataGeneratorService;
    
    @Autowired(required = false)
    private SparkStreamingService sparkStreamingService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * 获取系统状态
     */
    @GetMapping("/status")
    public ResponseEntity<String> getSystemStatus() {
        try {
            ObjectNode statusNode = objectMapper.createObjectNode();
            
            // 服务状态
            statusNode.put("status", "UP");
            statusNode.put("kafkaConsumer", kafkaConsumerService.getConsumerStats());
            
            // WebSocket连接信息
            statusNode.put("websocketEnabled", messagingTemplate != null);
            
            // Spark状态
            statusNode.put("sparkEnabled", sparkStreamingService != null);
            
            // 发送系统状态通知
            String statusJson = objectMapper.writeValueAsString(statusNode);
            messagingTemplate.convertAndSend("/topic/system-notifications", statusJson);
            
            return ResponseEntity.ok(statusJson);
        } catch (Exception e) {
            logger.error("获取系统状态时出错: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * 测试WebSocket连接
     */
    @GetMapping("/test-websocket")
    public ResponseEntity<String> testWebSocket() {
        try {
            ObjectNode messageNode = objectMapper.createObjectNode();
            messageNode.put("type", "TEST");
            messageNode.put("message", "这是一条WebSocket测试消息");
            messageNode.put("timestamp", System.currentTimeMillis());
            
            String messageJson = objectMapper.writeValueAsString(messageNode);
            
            // 发送到所有主题
            messagingTemplate.convertAndSend("/topic/agriculture-sensor-data", messageJson);
            messagingTemplate.convertAndSend("/topic/weather-stats", messageJson);
            messagingTemplate.convertAndSend("/topic/product-stats", messageJson);
            messagingTemplate.convertAndSend("/topic/system-notifications", messageJson);
            
            return ResponseEntity.ok("{\"status\": \"success\", \"message\": \"已发送测试消息到所有WebSocket主题\"}");
        } catch (Exception e) {
            logger.error("测试WebSocket时出错: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
} 