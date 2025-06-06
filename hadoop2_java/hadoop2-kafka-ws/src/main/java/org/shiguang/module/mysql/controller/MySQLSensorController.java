package org.shiguang.module.mysql.controller;

import org.shiguang.module.mysql.service.MySQLSensorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MySQL传感器数据控制器
 * 提供对MySQL存储的传感器数据的查询接口
 */
@RestController
@RequestMapping("/mysql/sensor")
@CrossOrigin
public class MySQLSensorController {
    private static final Logger logger = LoggerFactory.getLogger(MySQLSensorController.class);
    
    @Autowired
    private MySQLSensorService mysqlSensorService;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    /**
     * 获取传感器数据表信息
     */
    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> getSensorTableInfo() {
        try {
            Map<String, Object> response = new HashMap<>();
            Map<String, Object> info = mysqlSensorService.getSensorTableInfo();
            
            response.put("code", 200);
            response.put("message", "获取MySQL传感器数据表信息成功");
            response.put("data", info);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取MySQL传感器数据表信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取MySQL传感器数据表信息失败: " + e.getMessage());
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
            String tableName = mysqlSensorService.getSensorTableInfo().get("table").toString();
            String sql = "SELECT DISTINCT sensor_type FROM " + tableName;
            List<String> types = jdbcTemplate.queryForList(sql, String.class);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取MySQL传感器类型成功");
            response.put("data", types);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取MySQL传感器类型失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取MySQL传感器类型失败: " + e.getMessage());
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
            String tableName = mysqlSensorService.getSensorTableInfo().get("table").toString();
            String sql = "SELECT DISTINCT location FROM " + tableName;
            List<String> locations = jdbcTemplate.queryForList(sql, String.class);
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取MySQL传感器位置成功");
            response.put("data", locations);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取MySQL传感器位置失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取MySQL传感器位置失败: " + e.getMessage());
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
            String tableName = mysqlSensorService.getSensorTableInfo().get("table").toString();
            
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT sensor_type, COUNT(*) as count, AVG(value) as avgValue, MAX(value) as maxValue, MIN(value) as minValue, STDDEV(value) as stdDev FROM ")
               .append(tableName);
            
            List<String> whereConditions = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            
            if (sensorType != null && !sensorType.isEmpty()) {
                whereConditions.add("sensor_type = ?");
                params.add(sensorType);
            }
            
            if (location != null && !location.isEmpty()) {
                whereConditions.add("location = ?");
                params.add(location);
            }
            
            if (timeRange != null && !timeRange.isEmpty()) {
                // 简单处理，假设timeRange格式为"yyyy-MM-dd,yyyy-MM-dd"
                String[] dates = timeRange.split(",");
                if (dates.length == 2) {
                    whereConditions.add("readable_time >= ?");
                    params.add(dates[0] + " 00:00:00");
                    whereConditions.add("readable_time <= ?");
                    params.add(dates[1] + " 23:59:59");
                }
            }
            
            if (!whereConditions.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
            }
            
            sql.append(" GROUP BY sensor_type");
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
            Map<String, Object> data = new HashMap<>();
            data.put("stats", results);
            data.put("query", sql.toString());
            
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("message", "获取MySQL传感器数据统计信息成功");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取MySQL传感器数据统计信息失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取MySQL传感器数据统计信息失败: " + e.getMessage());
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
            String tableName = mysqlSensorService.getSensorTableInfo().get("table").toString();
            
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            
            // 根据时间单位选择查询
            if ("month".equals(timeUnit)) {
                sql.append("SELECT month, AVG(value) as value FROM ")
                   .append(tableName);
            } else if ("day".equals(timeUnit)) {
                sql.append("SELECT day, AVG(value) as value FROM ")
                   .append(tableName);
            } else {
                sql.append("SELECT year, month, AVG(value) as value FROM ")
                   .append(tableName);
            }
            
            List<String> whereConditions = new ArrayList<>();
            
            if (sensorType != null && !sensorType.isEmpty()) {
                whereConditions.add("sensor_type = ?");
                params.add(sensorType);
            }
            
            if (location != null && !location.isEmpty()) {
                whereConditions.add("location = ?");
                params.add(location);
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
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
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
            response.put("message", "获取MySQL传感器时间序列数据成功");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取MySQL传感器时间序列数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取MySQL传感器时间序列数据失败: " + e.getMessage());
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
            String tableName = mysqlSensorService.getSensorTableInfo().get("table").toString();
            
            StringBuilder sql = new StringBuilder();
            List<Object> params = new ArrayList<>();
            
            sql.append("SELECT a.value as valueA, b.value as valueB FROM ")
               .append(tableName).append(" a JOIN ")
               .append(tableName).append(" b ON a.event_time = b.event_time ");
            
            List<String> whereConditions = new ArrayList<>();
            whereConditions.add("a.sensor_type = ?");
            params.add(typeA);
            whereConditions.add("b.sensor_type = ?");
            params.add(typeB);
            
            if (location != null && !location.isEmpty()) {
                whereConditions.add("a.location = ?");
                params.add(location);
                whereConditions.add("b.location = ?");
                params.add(location);
            }
            
            sql.append(" WHERE ").append(String.join(" AND ", whereConditions));
            sql.append(" LIMIT 1000");
            
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql.toString(), params.toArray());
            
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
            response.put("message", "获取MySQL传感器相关性数据成功");
            response.put("data", data);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("获取MySQL传感器相关性数据失败", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("code", 500);
            errorResponse.put("message", "获取MySQL传感器相关性数据失败: " + e.getMessage());
            errorResponse.put("data", null);
            return ResponseEntity.ok(errorResponse);
        }
    }
} 