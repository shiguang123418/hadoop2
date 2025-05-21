package org.shiguang.kafka.controller;

import org.shiguang.kafka.service.SensorStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * <p>
 * 提供用于控制和监控传感器数据流处理的RESTful API。
 * 包括启动、停止流处理服务以及查询服务状态的接口。
 * </p>
 */
@RestController
@RequestMapping("/api/streaming")
public class SensorStreamingController {

    private static final Logger logger = LoggerFactory.getLogger(SensorStreamingController.class);
    
    @Autowired
    private SensorStreamingService streamingService;

    /**
     * 获取流处理服务状态
     * 
     * @return 包含服务运行状态和时间戳的响应
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        logger.debug("接收到获取流处理状态请求");
        
        Map<String, Object> status = new HashMap<>();
        boolean isRunning = streamingService.isRunning();
        
        status.put("running", isRunning);
        status.put("timestamp", System.currentTimeMillis());
        status.put("status", isRunning ? "active" : "stopped");
        
        logger.debug("返回流处理状态: {}", isRunning ? "running" : "stopped");
        return ResponseEntity.ok(status);
    }

    /**
     * 启动流处理服务
     * 
     * @return 包含操作结果和服务状态的响应
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> startStreaming() {
        logger.info("接收到启动流处理服务请求");
        
        try {
            Map<String, Object> result = new HashMap<>();
            boolean success = streamingService.startStreaming();
            boolean isRunning = streamingService.isRunning();
            
            result.put("success", success);
            result.put("running", isRunning);
            result.put("message", success ? "流处理服务启动成功" : "流处理服务已经在运行或无法启动");
            result.put("timestamp", System.currentTimeMillis());
            
            logger.info("流处理服务启动请求处理完成，结果: {}", success ? "成功" : "失败");
            return ResponseEntity.ok(result);
        } catch (Throwable t) {
            logger.error("启动流处理服务时发生严重错误", t);
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("running", false);
            result.put("error", t.getMessage());
            result.put("message", "启动流处理服务失败: " + t.getClass().getName());
            return ResponseEntity.ok(result);
        }
    }

    /**
     * 停止流处理服务
     * 
     * @return 包含操作结果和服务状态的响应
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stopStreaming() {
        logger.info("接收到停止流处理服务请求");
        
        Map<String, Object> result = new HashMap<>();
        boolean success = streamingService.stopStreaming();
        boolean isRunning = streamingService.isRunning();
        
        result.put("success", success);
        result.put("running", isRunning);
        result.put("message", success ? "流处理服务停止成功" : "流处理服务未在运行");
        result.put("timestamp", System.currentTimeMillis());
        
        logger.info("流处理服务停止请求处理完成，结果: {}", success ? "成功" : "失败");
        return ResponseEntity.ok(result);
    }
} 