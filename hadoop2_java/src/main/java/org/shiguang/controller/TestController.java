package org.shiguang.controller;

import org.shiguang.entity.SensorReading;
import org.shiguang.repository.SensorReadingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器
 * 用于测试数据库连接和基本功能
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    /**
     * 测试API是否正常
     */
    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
    
    /**
     * 测试数据库连接
     */
    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> testDb() {
        Map<String, Object> response = new HashMap<>();
        try {
            long count = sensorReadingRepository.count();
            response.put("status", "ok");
            response.put("records", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 测试保存数据到数据库
     */
    @PostMapping("/save-test-data")
    public ResponseEntity<Map<String, Object>> saveTestData() {
        Map<String, Object> response = new HashMap<>();
        try {
            SensorReading reading = new SensorReading();
            reading.setSensorId("TEST-SENSOR");
            reading.setSensorType("温度");
            reading.setRegion("测试区域");
            reading.setCropType("测试作物");
            reading.setValue(25.5);
            reading.setTimestamp(LocalDateTime.now());
            reading.setUnit("°C");
            reading.setDescription("测试数据");
            reading.setAnomaly(false);
            
            SensorReading saved = sensorReadingRepository.save(reading);
            
            response.put("status", "ok");
            response.put("saved", saved != null);
            response.put("id", saved != null ? saved.getId() : null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
} 