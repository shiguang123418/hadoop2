package org.shiguang.module.hive.controller;

import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.hive.service.HiveService;
import org.shiguang.module.hive.service.TaskManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hive数据分析相关API
 */
@RestController
@RequestMapping("/hive/analytics")
@CrossOrigin
public class HiveAnalyticsController {
    private static final Logger logger = LoggerFactory.getLogger(HiveAnalyticsController.class);
    
    @Autowired
    private HiveService hiveService;
    
    @Autowired
    private TaskManagerService taskManagerService;
    
    /**
     * 异步执行分析任务
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> submitAnalysisTask(
            @RequestBody Map<String, Object> request) {
        try {
            String analysisType = (String) request.get("analysisType");
            String database = (String) request.get("database");
            String table = (String) request.get("table");
            
            if (analysisType == null || database == null || table == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, "参数不完整", null));
            }
            
            logger.info("提交分析任务: {}, 数据库: {}, 表: {}", analysisType, database, table);
            
            // 获取当前用户
            String username = getCurrentUsername();
            
            // 提交任务
            String taskId = taskManagerService.submitAnalysisTask(analysisType, database, table, request, username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("status", "SUBMITTED");
            response.put("message", "任务已提交，请使用任务ID查询结果");
            
            return ResponseEntity.ok(new ApiResponse<>(200, "任务提交成功", response));
        } catch (Exception e) {
            logger.error("提交分析任务失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "提交分析任务失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取任务状态
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTaskStatus(
            @PathVariable String taskId) {
        try {
            Map<String, Object> taskStatus = taskManagerService.getTaskStatus(taskId);
            
            if (taskStatus.isEmpty()) {
                return ResponseEntity.ok(new ApiResponse<>(404, "任务不存在", null));
            }
            
            return ResponseEntity.ok(new ApiResponse<>(200, "获取任务状态成功", taskStatus));
        } catch (Exception e) {
            logger.error("获取任务状态失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取任务状态失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取所有任务
     */
    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<Map<String, Map<String, Object>>>> getAllTasks() {
        try {
            Map<String, Map<String, Object>> tasks = taskManagerService.getAllTasks();
            return ResponseEntity.ok(new ApiResponse<>(200, "获取所有任务成功", tasks));
        } catch (Exception e) {
            logger.error("获取所有任务失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取所有任务失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取当前用户的任务列表
     */
    @GetMapping("/user-tasks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserTasks() {
        try {
            String username = getCurrentUsername();
            List<Map<String, Object>> tasks = taskManagerService.getUserTasks(username);
            return ResponseEntity.ok(new ApiResponse<>(200, "获取用户任务成功", tasks));
        } catch (Exception e) {
            logger.error("获取用户任务失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取用户任务失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取最近的任务
     */
    @GetMapping("/recent-tasks")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentTasks(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<Map<String, Object>> tasks = taskManagerService.getRecentTasks(limit);
            return ResponseEntity.ok(new ApiResponse<>(200, "获取最近任务成功", tasks));
        } catch (Exception e) {
            logger.error("获取最近任务失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取最近任务失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 删除任务
     */
    @DeleteMapping("/task/{taskId}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(@PathVariable String taskId) {
        try {
            taskManagerService.deleteTask(taskId);
            return ResponseEntity.ok(new ApiResponse<>(200, "删除任务成功", null));
        } catch (Exception e) {
            logger.error("删除任务失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "删除任务失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 取消任务
     */
    @PostMapping("/task/{taskId}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelTask(
            @PathVariable String taskId) {
        try {
            boolean cancelled = taskManagerService.cancelTask(taskId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("cancelled", cancelled);
            
            return ResponseEntity.ok(new ApiResponse<>(200, cancelled ? "任务取消成功" : "任务无法取消", response));
        } catch (Exception e) {
            logger.error("取消任务失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "取消任务失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 执行分布分析
     */
    @PostMapping("/distribution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> distributionAnalysis(
            @RequestBody Map<String, Object> request) {
        try {
            String database = (String) request.get("database");
            String table = (String) request.get("table");
            String field = (String) request.get("field");
            
            if (database == null || table == null || field == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, "参数不完整", null));
            }
            
            // 构建分析SQL
            String sql = String.format(
                    "SELECT %s, COUNT(*) as count FROM %s.%s GROUP BY %s ORDER BY count DESC LIMIT 20", 
                    field, database, table, field);
            
            logger.info("执行分布分析: {}", sql);
            List<Map<String, Object>> results = hiveService.executeQuery(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", results);
            response.put("type", "distribution");
            response.put("field", field);
            
            return ResponseEntity.ok(new ApiResponse<>(200, "分布分析执行成功", response));
        } catch (Exception e) {
            logger.error("执行分布分析失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "执行分布分析失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 执行相关性分析
     */
    @PostMapping("/correlation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> correlationAnalysis(
            @RequestBody Map<String, Object> request) {
        try {
            String database = (String) request.get("database");
            String table = (String) request.get("table");
            @SuppressWarnings("unchecked")
            List<String> fields = (List<String>) request.get("fields");
            
            if (database == null || table == null || fields == null || fields.size() < 2) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, "参数不完整", null));
            }
            
            // 构建分析SQL
            String fieldStr = String.join(", ", fields);
            String sql = String.format("SELECT %s FROM %s.%s LIMIT 1000", fieldStr, database, table);
            
            logger.info("执行相关性分析: {}", sql);
            List<Map<String, Object>> results = hiveService.executeQuery(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", results);
            response.put("type", "correlation");
            response.put("fields", fields);
            
            return ResponseEntity.ok(new ApiResponse<>(200, "相关性分析执行成功", response));
        } catch (Exception e) {
            logger.error("执行相关性分析失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "执行相关性分析失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 执行时间序列分析
     */
    @PostMapping("/time-series")
    public ResponseEntity<ApiResponse<Map<String, Object>>> timeSeriesAnalysis(
            @RequestBody Map<String, Object> request) {
        try {
            String database = (String) request.get("database");
            String table = (String) request.get("table");
            String timeField = (String) request.get("timeField");
            String valueField = (String) request.get("valueField");
            
            if (database == null || table == null || timeField == null || valueField == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, "参数不完整", null));
            }
            
            // 构建分析SQL
            String sql = String.format(
                    "SELECT %s, %s FROM %s.%s ORDER BY %s LIMIT 1000", 
                    timeField, valueField, database, table, timeField);
            
            logger.info("执行时间序列分析: {}", sql);
            List<Map<String, Object>> rawResults = hiveService.executeQuery(sql);
            
            // 处理返回结果，确保字段名称是明确的
            List<Map<String, Object>> processedResults = new ArrayList<>();
            for (Map<String, Object> row : rawResults) {
                Map<String, Object> newRow = new HashMap<>();
                // 确保使用标准字段名
                Object timeValue = row.get(timeField);
                Object dataValue = row.get(valueField);
                
                // 明确设置字段名为month和value，避免前端解析问题
                newRow.put("month", timeValue);
                newRow.put("value", dataValue);
                
                processedResults.add(newRow);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", processedResults);
            response.put("type", "time_series");
            response.put("timeField", "month"); // 固定使用month作为时间字段名
            response.put("valueField", "value"); // 固定使用value作为值字段名
            
            return ResponseEntity.ok(new ApiResponse<>(200, "时间序列分析执行成功", response));
        } catch (Exception e) {
            logger.error("执行时间序列分析失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "执行时间序列分析失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 执行聚类分析
     */
    @PostMapping("/clustering")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clusteringAnalysis(
            @RequestBody Map<String, Object> request) {
        try {
            String database = (String) request.get("database");
            String table = (String) request.get("table");
            @SuppressWarnings("unchecked")
            List<String> fields = (List<String>) request.get("fields");
            
            if (database == null || table == null || fields == null || fields.size() < 2) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, "参数不完整", null));
            }
            
            // 构建分析SQL
            String fieldStr = String.join(", ", fields);
            String sql = String.format("SELECT %s FROM %s.%s LIMIT 1000", fieldStr, database, table);
            
            logger.info("执行聚类分析: {}", sql);
            List<Map<String, Object>> results = hiveService.executeQuery(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", results);
            response.put("type", "clustering");
            response.put("fields", fields);
            
            return ResponseEntity.ok(new ApiResponse<>(200, "聚类分析执行成功", response));
        } catch (Exception e) {
            logger.error("执行聚类分析失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "执行聚类分析失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 执行回归分析
     */
    @PostMapping("/regression")
    public ResponseEntity<ApiResponse<Map<String, Object>>> regressionAnalysis(
            @RequestBody Map<String, Object> request) {
        try {
            String database = (String) request.get("database");
            String table = (String) request.get("table");
            @SuppressWarnings("unchecked")
            List<String> fields = (List<String>) request.get("fields");
            
            if (database == null || table == null || fields == null || fields.size() < 2) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, "参数不完整", null));
            }
            
            // 构建分析SQL
            String fieldStr = String.join(", ", fields);
            String sql = String.format("SELECT %s FROM %s.%s LIMIT 1000", fieldStr, database, table);
            
            logger.info("执行回归分析: {}", sql);
            List<Map<String, Object>> results = hiveService.executeQuery(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("data", results);
            response.put("type", "regression");
            response.put("fields", fields);
            
            return ResponseEntity.ok(new ApiResponse<>(200, "回归分析执行成功", response));
        } catch (Exception e) {
            logger.error("执行回归分析失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "执行回归分析失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 保存分析结果
     */
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Map<String, Object>>> saveAnalysisResult(
            @RequestBody Map<String, Object> request) {
        try {
            String name = (String) request.get("name");
            String description = (String) request.get("description");
            String analysisType = (String) request.get("analysisType");
            
            if (name == null || description == null || analysisType == null) {
                return ResponseEntity.badRequest()
                        .body(new ApiResponse<>(400, "参数不完整", null));
            }
            
            // TODO: 实际项目中需要保存分析结果到数据库
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", System.currentTimeMillis());
            response.put("name", name);
            response.put("saveTime", System.currentTimeMillis());
            
            return ResponseEntity.ok(new ApiResponse<>(200, "分析结果保存成功", response));
        } catch (Exception e) {
            logger.error("保存分析结果失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "保存分析结果失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取当前登录用户名
     */
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "anonymous";
    }
} 