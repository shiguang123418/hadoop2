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
     * 健康检查API
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"message\": \"服务运行正常\"}");
    }
} 