package org.shiguang.module.sensor.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.shiguang.module.sensor.utils.DataGeneratorUtil;
import org.shiguang.module.sensor.service.DataProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * 传感器数据控制器
 * 提供数据监控和手动触发API
 */
@RestController
@RequestMapping("/sensor")
public class SensorDataController {

    private static final Logger logger = LoggerFactory.getLogger(SensorDataController.class);
    
    @Autowired
    private DataGeneratorUtil dataGeneratorUtil;
    
    @Autowired
    private DataProcessingService dataProcessingService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * 获取聚合统计数据
     */
    @GetMapping("/stats")
    public ResponseEntity<String> getStats() {
        try {
            String stats = dataProcessingService.getAggregatedStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("获取统计数据时出错: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * 直接发送WebSocket消息测试
     */
    @GetMapping("/direct-test")
    public ResponseEntity<String> sendDirectTestMessage() {
        try {
            // 创建一个简单的测试消息
            ObjectNode dataNode = objectMapper.createObjectNode();
            dataNode.put("sensorId", "direct-test-001");
            dataNode.put("sensorType", "temperature");
            dataNode.put("value", 26.5);
            dataNode.put("unit", "°C");
            dataNode.put("timestamp", System.currentTimeMillis());
            dataNode.put("location", "测试区域");
            dataNode.put("batteryLevel", 100);
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
            dataNode.put("readableTime", dateFormat.format(new Date()));
            
            // 直接发送到WebSocket
            String jsonData = objectMapper.writeValueAsString(dataNode);
            messagingTemplate.convertAndSend("/topic/agriculture-sensor-data", jsonData);
            logger.info("直接测试数据已发送到WebSocket: {}", jsonData);
            
            return ResponseEntity.ok("{\"status\": \"success\", \"message\": \"WebSocket测试消息已直接发送\"}");
        } catch (Exception e) {
            logger.error("直接发送WebSocket测试消息时出错: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    
    /**
     * 健康检查API
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"message\": \"服务运行正常\"}");
    }
} 