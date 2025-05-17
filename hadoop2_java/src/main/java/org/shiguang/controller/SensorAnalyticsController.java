package org.shiguang.controller;

import org.shiguang.entity.SensorReading;
import org.shiguang.repository.SensorReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传感器数据分析控制器
 */
@RestController
@RequestMapping("/analytics")
public class SensorAnalyticsController {

    private static final Logger logger = LoggerFactory.getLogger(SensorAnalyticsController.class);

    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    /**
     * 获取最新的传感器数据
     */
    @GetMapping("/latest")
    public ResponseEntity<List<SensorReading>> getLatestReadings() {
        logger.info("获取最新传感器数据");
        return ResponseEntity.ok(sensorReadingRepository.findLatestReadings());
    }

    /**
     * 按区域和传感器类型获取平均值
     */
    @GetMapping("/region-averages")
    public ResponseEntity<List<Map<String, Object>>> getRegionAverages() {
        logger.info("获取区域平均值数据");
        return ResponseEntity.ok(sensorReadingRepository.findAverageByRegionAndType());
    }

    /**
     * 按作物类型和传感器类型获取平均值
     */
    @GetMapping("/crop-averages")
    public ResponseEntity<List<Map<String, Object>>> getCropAverages() {
        logger.info("获取作物平均值数据");
        return ResponseEntity.ok(sensorReadingRepository.findAverageByCropAndType());
    }

    /**
     * 获取异常数据
     */
    @GetMapping("/anomalies")
    public ResponseEntity<List<SensorReading>> getAnomalies() {
        logger.info("获取异常传感器数据");
        return ResponseEntity.ok(sensorReadingRepository.findByAnomaly(true));
    }

    /**
     * 按时间范围查询数据
     */
    @GetMapping("/by-time-range")
    public ResponseEntity<List<SensorReading>> getByTimeRange(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        logger.info("查询时间范围[{} - {}]的传感器数据", start, end);
        return ResponseEntity.ok(sensorReadingRepository.findByTimestampBetween(start, end));
    }

    /**
     * 按传感器类型查询
     */
    @GetMapping("/by-sensor-type/{type}")
    public ResponseEntity<List<SensorReading>> getBySensorType(@PathVariable("type") String sensorType) {
        logger.info("查询传感器类型[{}]的数据", sensorType);
        return ResponseEntity.ok(sensorReadingRepository.findBySensorType(sensorType));
    }

    /**
     * 按区域查询
     */
    @GetMapping("/by-region/{region}")
    public ResponseEntity<List<SensorReading>> getByRegion(@PathVariable("region") String region) {
        logger.info("查询区域[{}]的传感器数据", region);
        return ResponseEntity.ok(sensorReadingRepository.findByRegion(region));
    }

    /**
     * 按作物类型查询
     */
    @GetMapping("/by-crop/{cropType}")
    public ResponseEntity<List<SensorReading>> getByCropType(@PathVariable("cropType") String cropType) {
        logger.info("查询作物[{}]的传感器数据", cropType);
        return ResponseEntity.ok(sensorReadingRepository.findByCropType(cropType));
    }
    
    /**
     * 获取数据统计信息
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        logger.info("获取传感器数据摘要");
        
        Map<String, Object> summary = new HashMap<>();
        long total = sensorReadingRepository.count();
        long anomalies = sensorReadingRepository.findByAnomaly(true).size();
        
        summary.put("totalReadings", total);
        summary.put("anomalies", anomalies);
        summary.put("anomalyPercentage", total > 0 ? (double)anomalies / total * 100 : 0);
        summary.put("regions", sensorReadingRepository.findAverageByRegionAndType().size());
        summary.put("cropTypes", sensorReadingRepository.findAverageByCropAndType().size());
        
        return ResponseEntity.ok(summary);
    }
}