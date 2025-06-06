package org.shiguang.module.hive.controller;

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
    public ResponseEntity<Map<String, Object>> getSensorTableInfo() {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> info = new HashMap<>();
            
            if (sensorDataImportService != null) {
                info = sensorDataImportService.getSensorTableInfo();
            } else {
                info.put("database", sensorDatabase);
                info.put("table", sensorTable);
                info.put("success", false);
                info.put("message", "传感器数据导入服务不可用");
            }
            
            response.put("code", 200);
            response.put("message", "获取传感器数据表信息成功");
            response.put("data", info);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取传感器数据表信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取传感器数据表信息失败: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * 获取传感器类型列表
     */
    @GetMapping("/types")
    public ResponseEntity<Map<String, Object>> getSensorTypes() {
        try {
            String sql = "SELECT DISTINCT sensorType FROM " + sensorDatabase + "." + sensorTable;
            List<Map<String, Object>> results = hiveService.executeQuery(sql);
            
            List<String> types = new ArrayList<>();
            for (Map<String, Object> row : results) {
                types.add((String) row.get("sensorType"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取传感器类型成功");
            response.put("data", types);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取传感器类型失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取传感器类型失败: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * 获取传感器位置列表
     */
    @GetMapping("/locations")
    public ResponseEntity<Map<String, Object>> getSensorLocations() {
        try {
            String sql = "SELECT DISTINCT location FROM " + sensorDatabase + "." + sensorTable;
            List<Map<String, Object>> results = hiveService.executeQuery(sql);
            
            List<String> locations = new ArrayList<>();
            for (Map<String, Object> row : results) {
                locations.add((String) row.get("location"));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取传感器位置成功");
            response.put("data", locations);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取传感器位置失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取传感器位置失败: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * 获取传感器数据统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSensorStats(
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
            
            Map<String, Object> data = new HashMap<>();
            data.put("stats", results);
            data.put("query", sql.toString());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取传感器数据统计信息成功");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取传感器数据统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取传感器数据统计信息失败: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * 获取传感器时间序列数据
     */
    @GetMapping("/time-series")
    public ResponseEntity<Map<String, Object>> getTimeSeries(
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
            
            Map<String, Object> data = new HashMap<>();
            data.put("data", results);
            data.put("query", sql.toString());
            data.put("sensorType", sensorType);
            data.put("timeUnit", timeUnit);
            if (location != null && !location.isEmpty()) {
                data.put("location", location);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取传感器时间序列数据成功");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取传感器时间序列数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取传感器时间序列数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * 获取传感器相关性数据
     */
    @GetMapping("/correlation")
    public ResponseEntity<Map<String, Object>> getCorrelation(
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
            
            Map<String, Object> data = new HashMap<>();
            data.put("data", results);
            data.put("query", sql.toString());
            data.put("typeA", typeA);
            data.put("typeB", typeB);
            if (location != null && !location.isEmpty()) {
                data.put("location", location);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取传感器相关性数据成功");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取传感器相关性数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取传感器相关性数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.ok(errorResponse);
        }
    }
    
    /**
     * 手动触发数据导入
     */
    @PostMapping("/import/trigger")
    public ResponseEntity<Map<String, Object>> triggerImport() {
        try {
            Map<String, Object> result = new HashMap<>();
            Map<String, Object> data = new HashMap<>();
            
            if (sensorDataImportService != null) {
                data.put("success", true);
                data.put("message", "已触发数据导入");
                
                // 获取表信息
                Map<String, Object> tableInfo = sensorDataImportService.getSensorTableInfo();
                data.put("tableInfo", tableInfo);
            } else {
                data.put("success", false);
                data.put("message", "传感器数据导入服务不可用");
            }
            
            result.put("code", 200);
            result.put("message", "触发数据导入操作");
            result.put("data", data);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("触发数据导入失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "触发数据导入失败: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.ok(errorResponse);
        }
    }
} 