package org.shiguang.controller;

import org.shiguang.service.KafkaStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka流数据处理控制器
 */
@RestController
@RequestMapping("/kafka")
public class KafkaStreamingController {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamingController.class);

    @Autowired
    private KafkaStreamingService kafkaStreamingService;

    /**
     * 启动Kafka流处理
     *
     * @param topics Kafka主题数组
     * @return 操作结果
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startStreaming(@RequestParam("topics") String[] topics) {
        logger.info("接收到启动Kafka流处理请求，主题: {}", String.join(", ", topics));
        
        Map<String, Object> response = new HashMap<>();
        
        if (topics == null || topics.length == 0) {
            response.put("success", false);
            response.put("message", "至少需要提供一个Kafka主题");
            return ResponseEntity.badRequest().body(response);
        }
        
        boolean result = kafkaStreamingService.startStreamProcessing(topics);
        
        response.put("success", result);
        if (result) {
            response.put("message", "Kafka流处理已启动");
            response.put("topics", topics);
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "启动Kafka流处理失败，可能是Spark未启用或已有流处理在运行");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 停止Kafka流处理
     *
     * @return 操作结果
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopStreaming() {
        logger.info("接收到停止Kafka流处理请求");
        
        Map<String, Object> response = new HashMap<>();
        boolean result = kafkaStreamingService.stopStreamProcessing();
        
        response.put("success", result);
        if (result) {
            response.put("message", "Kafka流处理已停止");
            return ResponseEntity.ok(response);
        } else {
            response.put("message", "停止Kafka流处理失败，可能当前没有流处理在运行");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * 获取流处理状态
     *
     * @return 状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStreamingStatus() {
        logger.info("获取Kafka流处理状态");
        
        Map<String, Object> status = new HashMap<>();
        boolean isActive = kafkaStreamingService.isStreamingActive();
        
        status.put("active", isActive);
        status.put("message", isActive ? "Kafka流处理正在运行" : "Kafka流处理未运行");
        
        return ResponseEntity.ok(status);
    }
} 