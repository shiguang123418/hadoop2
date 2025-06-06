package org.shiguang.module.hive.controller;

import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.hive.service.HiveService;
import org.shiguang.module.hive.service.SensorDataImportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 传感器数据Hive分析控制器
 */
@RestController
@RequestMapping("/hive/sensor")
@CrossOrigin
public class SensorHiveController {
    private static final Logger logger = LoggerFactory.getLogger(SensorHiveController.class);
    
    @Autowired
    private HiveService hiveService;
    
    @Autowired(required = false)
    private SensorDataImportService sensorDataImportService;
    
    @Value("${hive.sensor.database:agri_data}")
    private String sensorDatabase;
    
    @Value("${hive.sensor.table:sensor_data}")
    private String sensorTable;
    
    /**
     * 获取传感器数据表信息
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSensorTableInfo() {
        try {
            Map<String, Object> info = new HashMap<>();
            
            if (sensorDataImportService != null) {
                info = sensorDataImportService.getSensorTableInfo();
            } else {
                info.put("database", sensorDatabase);
                info.put("table", sensorTable);
                info.put("success", false);
                info.put("message", "传感器数据导入服务不可用");
            }
            
            return ResponseEntity.ok(new ApiResponse<>(200, "获取传感器数据表信息成功", info));
        } catch (Exception e) {
            logger.error("获取传感器数据表信息失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取传感器数据表信息失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取传感器类型列表
     */
    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<String>>> getSensorTypes() {
        try {
            String sql = "SELECT DISTINCT sensorType FROM " + sensorDatabase + "." + sensorTable;
            List<Map<String, Object>> results = hiveService.executeQuery(sql);
            
            List<String> types = new ArrayList<>();
            for (Map<String, Object> row : results) {
                types.add((String) row.get("sensorType"));
            }
            
            return ResponseEntity.ok(new ApiResponse<>(200, "获取传感器类型成功", types));
        } catch (Exception e) {
            logger.error("获取传感器类型失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取传感器类型失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取传感器位置列表
     */
    @GetMapping("/locations")
    public ResponseEntity<ApiResponse<List<String>>> getSensorLocations() {
        try {
            String sql = "SELECT DISTINCT location FROM " + sensorDatabase + "." + sensorTable;
            List<Map<String, Object>> results = hiveService.executeQuery(sql);
            
            List<String> locations = new ArrayList<>();
            for (Map<String, Object> row : results) {
                locations.add((String) row.get("location"));
            }
            
            return ResponseEntity.ok(new ApiResponse<>(200, "获取传感器位置成功", locations));
        } catch (Exception e) {
            logger.error("获取传感器位置失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取传感器位置失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取传感器数据统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSensorStats(
            @RequestParam(required = false) String sensorType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String timeRange) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT sensorType, COUNT(*) as count, AVG(value) as avgValue, MAX(value) as maxValue, MIN(value) as minValue, STDDEV(value) as stdDev FROM ")
               .append(sensorDatabase).append(".").append(sensorTable);
            
            List<String> whereConditions = new ArrayList<>();
            
            if (sensorType != null && !sensorType.isEmpty()) {
                whereConditions.add("sensorType = '" + sensorType + "'");
            }
            
            if (location != null && !location.isEmpty()) {
                whereConditions.add("location = '" + location + "'");
            }
            
            if (timeRange != null && !timeRange.isEmpty()) {
                // 简单处理，假设timeRange格式为"yyyy-MM-dd,yyyy-MM-dd"
                String[] dates = timeRange.split(",");
                if (dates.length == 2) {
                    whereConditions.add("readableTime >= '" + dates[0] + " 00:00:00'");
                    whereConditions.add("readableTime <= '" + dates[1] + " 23:59:59'");
                }
            }
            
            if (!whereConditions.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
            }
            
            sql.append(" GROUP BY sensorType");
            
            List<Map<String, Object>> results = hiveService.executeQuery(sql.toString());
            
            Map<String, Object> response = new HashMap<>();
            response.put("stats", results);
            response.put("query", sql.toString());
            
            return ResponseEntity.ok(new ApiResponse<>(200, "获取传感器数据统计信息成功", response));
        } catch (Exception e) {
            logger.error("获取传感器数据统计信息失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取传感器数据统计信息失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取传感器时间序列数据
     */
    @GetMapping("/time-series")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTimeSeries(
            @RequestParam String sensorType,
            @RequestParam(required = false) String location,
            @RequestParam(required = false, defaultValue = "month") String timeUnit) {
        try {
            StringBuilder sql = new StringBuilder();
            
            // 根据时间单位选择查询
            if ("month".equals(timeUnit)) {
                sql.append("SELECT month, AVG(value) as value FROM ")
                   .append(sensorDatabase).append(".").append(sensorTable);
            } else if ("day".equals(timeUnit)) {
                sql.append("SELECT day, AVG(value) as value FROM ")
                   .append(sensorDatabase).append(".").append(sensorTable);
            } else {
                sql.append("SELECT year, month, AVG(value) as value FROM ")
                   .append(sensorDatabase).append(".").append(sensorTable);
            }
            
            List<String> whereConditions = new ArrayList<>();
            
            if (sensorType != null && !sensorType.isEmpty()) {
                whereConditions.add("sensorType = '" + sensorType + "'");
            }
            
            if (location != null && !location.isEmpty()) {
                whereConditions.add("location = '" + location + "'");
            }
            
            if (!whereConditions.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
            }
            
            if ("month".equals(timeUnit)) {
                sql.append(" GROUP BY month ORDER BY month");
            } else if ("day".equals(timeUnit)) {
                sql.append(" GROUP BY day ORDER BY day");
            } else {
                sql.append(" GROUP BY year, month ORDER BY year, month");
            }
            
            List<Map<String, Object>> results = hiveService.executeQuery(sql.toString());
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", results);
            response.put("query", sql.toString());
            response.put("sensorType", sensorType);
            response.put("timeUnit", timeUnit);
            if (location != null && !location.isEmpty()) {
                response.put("location", location);
            }
            
            return ResponseEntity.ok(new ApiResponse<>(200, "获取传感器时间序列数据成功", response));
        } catch (Exception e) {
            logger.error("获取传感器时间序列数据失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取传感器时间序列数据失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取传感器相关性数据
     */
    @GetMapping("/correlation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCorrelation(
            @RequestParam String typeA,
            @RequestParam String typeB,
            @RequestParam(required = false) String location) {
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT a.value as valueA, b.value as valueB FROM ")
               .append(sensorDatabase).append(".").append(sensorTable).append(" a JOIN ")
               .append(sensorDatabase).append(".").append(sensorTable).append(" b ON a.timestamp = b.timestamp ");
            
            List<String> whereConditions = new ArrayList<>();
            whereConditions.add("a.sensorType = '" + typeA + "'");
            whereConditions.add("b.sensorType = '" + typeB + "'");
            
            if (location != null && !location.isEmpty()) {
                whereConditions.add("a.location = '" + location + "'");
                whereConditions.add("b.location = '" + location + "'");
            }
            
            sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
            sql.append(" LIMIT 1000");
            
            List<Map<String, Object>> results = hiveService.executeQuery(sql.toString());
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", results);
            response.put("query", sql.toString());
            response.put("typeA", typeA);
            response.put("typeB", typeB);
            if (location != null && !location.isEmpty()) {
                response.put("location", location);
            }
            
            return ResponseEntity.ok(new ApiResponse<>(200, "获取传感器相关性数据成功", response));
        } catch (Exception e) {
            logger.error("获取传感器相关性数据失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取传感器相关性数据失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 手动触发传感器数据导入
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDataImport() {
        try {
            Map<String, Object> result;
            
            if (sensorDataImportService != null) {
                result = sensorDataImportService.triggerImport();
            } else {
                result = new HashMap<>();
                result.put("success", false);
                result.put("message", "传感器数据导入服务不可用");
            }
            
            return ResponseEntity.ok(new ApiResponse<>(200, "触发传感器数据导入", result));
        } catch (Exception e) {
            logger.error("触发传感器数据导入失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "触发传感器数据导入失败: " + e.getMessage(), null));
        }
    }
} 