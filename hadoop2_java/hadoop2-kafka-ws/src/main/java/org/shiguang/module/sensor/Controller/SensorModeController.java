package org.shiguang.module.sensor.Controller;

import org.shiguang.module.sensor.config.SensorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 传感器数据模式控制器
 * 提供简单的接口用于控制传感器数据处理的各种功能
 */
@RestController
@RequestMapping("/sensor/mode")
public class SensorModeController {

    private static final Logger logger = LoggerFactory.getLogger(SensorModeController.class);
    
    @Autowired
    private SensorConfig sensorConfig;
    
    /**
     * 获取当前模式状态
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getCurrentMode() {
        Map<String, Object> result = new HashMap<>();
        
        // 主要状态
        result.put("enabled", sensorConfig.isEnabled());
        result.put("simulation", sensorConfig.isSimulationEnabled());
        
        // 其他参数
        result.put("dataCollectionInterval", sensorConfig.getDataCollectionInterval());
        result.put("kafkaTopic", sensorConfig.getKafkaTopic());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 启用/禁用传感器模块
     */
    @PostMapping("/enabled")
    public ResponseEntity<Map<String, Object>> setEnabled(
            @RequestParam(required = true) boolean enabled) {
        sensorConfig.setEnabled(enabled);
        logger.info("传感器模块已{}", enabled ? "启用" : "禁用");
        return createResponse("传感器模块已" + (enabled ? "启用" : "禁用"), "enabled", enabled);
    }
    
    /**
     * 设置模拟模式开关
     */
    @PostMapping("/simulation")
    public ResponseEntity<Map<String, Object>> setSimulationMode(
            @RequestParam(required = true) boolean enabled) {
        sensorConfig.setSimulationEnabled(enabled);
        logger.info("传感器数据模拟已{}", enabled ? "启用" : "禁用");
        return createResponse("传感器数据模拟已" + (enabled ? "启用" : "禁用"), "simulation", enabled);
    }
    
    /**
     * 设置采集间隔
     */
    @PostMapping("/interval")
    public ResponseEntity<Map<String, Object>> setCollectionInterval(
            @RequestParam(defaultValue = "5000") int interval) {
        sensorConfig.setDataCollectionInterval(interval);
        logger.info("传感器数据采集间隔已设置为{}毫秒", sensorConfig.getDataCollectionInterval());
        return createResponse("传感器数据采集间隔已设置", "interval", sensorConfig.getDataCollectionInterval());
    }
    
    /**
     * 设置Kafka主题
     */
    @PostMapping("/topic")
    public ResponseEntity<Map<String, Object>> setKafkaTopic(
            @RequestParam(required = true) String topic) {
        sensorConfig.setKafkaTopic(topic);
        logger.info("传感器Kafka主题已设置为{}", topic);
        return createResponse("传感器Kafka主题已设置", "topic", topic);
    }
    
    /**
     * 创建响应
     */
    private ResponseEntity<Map<String, Object>> createResponse(String message, String key, Object value) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", message);
        result.put(key, value);
        return ResponseEntity.ok(result);
    }
} 