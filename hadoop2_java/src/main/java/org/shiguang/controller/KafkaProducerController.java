package org.shiguang.controller;

import org.shiguang.model.SensorData;
import org.shiguang.util.KafkaProducerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka生产者控制器，用于生成测试数据并发送到Kafka
 */
@RestController
@RequestMapping("/producer")
public class KafkaProducerController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaProducerController.class);
    
    @Autowired
    private KafkaProducerUtil kafkaProducerUtil;
    
    /**
     * 发送单条随机传感器数据
     */
    @PostMapping("/send")
    public ResponseEntity<Map<String, Object>> sendSingleData() {
        logger.info("发送单条随机传感器数据");
        
        Map<String, Object> response = new HashMap<>();
        SensorData data = null;
        
        // 使用Kafka服务生成数据
        logger.info("使用Kafka服务生成数据");
        data = kafkaProducerUtil.sendRandomSensorData();
        
        if (data != null) {
            response.put("success", true);
            response.put("message", "成功发送传感器数据");
            response.put("data", data);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "发送数据失败");
            return ResponseEntity.internalServerError().body(response);
        }
    }
    
    /**
     * 发送多条随机传感器数据
     * 
     * @param count 要发送的数据条数
     */
    @PostMapping("/send-batch")
    public ResponseEntity<Map<String, Object>> sendBatchData(@RequestParam(defaultValue = "10") int count) {
        logger.info("发送批量随机传感器数据: count={}", count);
        
        // 限制一次最多发送1000条，防止过载
        final int finalCount = count > 1000 ? 1000 : count;
        
        Map<String, Object> response = new HashMap<>();
        
        // 异步发送数据，避免阻塞请求线程
        new Thread(() -> {
            kafkaProducerUtil.sendMultipleRandomSensorData(finalCount);
        }).start();
        
        response.put("success", true);
        response.put("message", "正在异步发送 " + finalCount + " 条随机传感器数据到Kafka");
        response.put("count", finalCount);
        return ResponseEntity.accepted().body(response);
    }
    
    /**
     * 发送模拟异常数据
     */
    @PostMapping("/send-anomaly")
    public ResponseEntity<Map<String, Object>> sendAnomalyData(
            @RequestParam(defaultValue = "温度") String sensorType,
            @RequestParam(defaultValue = "100.0") double anomalyValue) {
        
        logger.info("发送异常传感器数据: type={}, value={}", sensorType, anomalyValue);
        
        // 这里可以实现发送异常数据的逻辑，为了简单起见，这里仅记录
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "功能待实现");
        
        return ResponseEntity.ok(response);
    }
} 