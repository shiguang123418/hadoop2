package org.shiguang.kafka.controller;

import org.shiguang.kafka.service.SensorStreamingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 传感器流处理REST控制器
 */
@RestController
@RequestMapping("/api/streaming")
public class SensorStreamingController {

    @Autowired
    private SensorStreamingService streamingService;

    /**
     * 获取流处理服务状态
     * @return 状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", streamingService.isRunning());
        status.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(status);
    }

    /**
     * 启动流处理服务
     * @return 操作结果
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startStreaming() {
        Map<String, Object> result = new HashMap<>();
        boolean success = streamingService.startStreaming();
        result.put("success", success);
        result.put("running", streamingService.isRunning());
        result.put("message", success ? "流处理服务启动成功" : "流处理服务已经在运行或无法启动");
        return ResponseEntity.ok(result);
    }

    /**
     * 停止流处理服务
     * @return 操作结果
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopStreaming() {
        Map<String, Object> result = new HashMap<>();
        boolean success = streamingService.stopStreaming();
        result.put("success", success);
        result.put("running", streamingService.isRunning());
        result.put("message", success ? "流处理服务停止成功" : "流处理服务未在运行");
        return ResponseEntity.ok(result);
    }
} 