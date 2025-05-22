package org.shiguang.controller;

import org.shiguang.model.SensorData;
import org.shiguang.service.SensorDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/sensors")
public class SensorDataController {

    @Autowired
    private SensorDataService sensorDataService;

    /**
     * 获取指定传感器在时间范围内的数据
     */
    @GetMapping("/{sensorId}/data")
    public ResponseEntity<List<SensorData>> getSensorData(
            @PathVariable String sensorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        long startTimestamp = startTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endTimestamp = endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        List<SensorData> data = sensorDataService.findByTimeRange(sensorId, startTimestamp, endTimestamp);
        return ResponseEntity.ok(data);
    }

    /**
     * 手动添加传感器数据（用于测试）
     */
    @PostMapping("/data")
    public ResponseEntity<SensorData> addSensorData(@RequestBody SensorData sensorData) {
        SensorData savedData = sensorDataService.save(sensorData);
        return ResponseEntity.ok(savedData);
    }
} 