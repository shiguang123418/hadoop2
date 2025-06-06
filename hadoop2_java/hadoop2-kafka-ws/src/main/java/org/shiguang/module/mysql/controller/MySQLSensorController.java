package org.shiguang.module.mysql.controller;

import org.shiguang.module.mysql.service.MySQLSensorService;
import org.shiguang.module.sensor.service.DataProcessingService;
import org.shiguang.module.sensor.service.KafkaConsumerService;
import org.shiguang.module.sensor.service.SensorDataStorageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL传感器数据控制器
 * 用于测试和查询MySQL中的传感器数据
 */
@RestController
@RequestMapping("/mysql/sensor")
public class MySQLSensorController {
    
    private static final Logger logger = LoggerFactory.getLogger(MySQLSensorController.class);
    
    @Autowired
    private MySQLSensorService mysqlSensorService;
    
    @Autowired
    private KafkaConsumerService kafkaConsumerService;
    
    @Autowired
    private SensorDataStorageFactory storageFactory;
    
    @Autowired
    private DataProcessingService dataProcessingService;
    
    /**
     * 健康检查API
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "MySQL传感器服务运行正常");
        return ResponseEntity.ok(status);
    }
    
    /**
     * 获取MySQL存储状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            status.put("kafkaStatus", kafkaConsumerService.getConsumerStats());
            status.put("storageStats", storageFactory.getStorageStatistics());
            
            Map<String, Object> tableInfo = mysqlSensorService.getSensorTableInfo();
            status.put("tableInfo", tableInfo);
            
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("获取MySQL存储状态时出错: {}", e.getMessage(), e);
            status.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(status);
        }
    }
    
    /**
     * 重建数据库表
     */
    @PostMapping("/rebuild-table")
    public ResponseEntity<Map<String, Object>> rebuildTable() {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("开始重建传感器数据表...");
            boolean success = mysqlSensorService.rebuildTable();
            
            result.put("success", success);
            if (success) {
                result.put("message", "传感器数据表重建成功");
                return ResponseEntity.ok(result);
            } else {
                result.put("message", "传感器数据表重建失败");
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("重建数据表时出错: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
    
    /**
     * 获取最近的传感器数据
     */
    @GetMapping("/recent")
    public ResponseEntity<List<Map<String, Object>>> getRecentData(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> data = mysqlSensorService.getRecentSensorData(limit);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("获取最近传感器数据时出错: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 根据传感器类型获取最近的传感器数据
     */
    @GetMapping("/recent/{sensorType}")
    public ResponseEntity<List<Map<String, Object>>> getRecentDataByType(
            @PathVariable String sensorType,
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> data = mysqlSensorService.getRecentSensorDataByType(sensorType, limit);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("获取最近传感器数据时出错: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 获取时间序列统计数据
     */
    @GetMapping("/timeseries")
    public ResponseEntity<List<Map<String, Object>>> getTimeSeriesStats(
            @RequestParam String sensorType,
            @RequestParam(defaultValue = "day") String timeUnit,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        try {
            List<Map<String, Object>> data = mysqlSensorService.getTimeSeriesStats(
                    sensorType, timeUnit, startTime, endTime);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            logger.error("获取时间序列统计数据时出错: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 手动保存传感器数据（用于测试）
     */
    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> saveSensorData(@RequestBody String sensorDataJson) {
        Map<String, Object> result = new HashMap<>();
        try {
            logger.info("收到手动保存传感器数据请求: {}", sensorDataJson);
            
            // 处理数据
            String processedData = dataProcessingService.processRawData(sensorDataJson);
            result.put("processedData", processedData);
            
            // 存储数据
            boolean stored = storageFactory.storeSensorData(sensorDataJson);
            result.put("stored", stored);
            
            if (stored) {
                result.put("message", "数据成功保存到MySQL");
                return ResponseEntity.ok(result);
            } else {
                result.put("message", "数据保存失败");
                return ResponseEntity.badRequest().body(result);
            }
        } catch (Exception e) {
            logger.error("手动保存传感器数据时出错: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
} 