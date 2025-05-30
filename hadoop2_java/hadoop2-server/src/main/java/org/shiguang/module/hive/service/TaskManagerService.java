package org.shiguang.module.hive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步任务管理服务
 * 用于处理长时间运行的Hive分析任务
 */
@Service
@EnableAsync
public class TaskManagerService {
    private static final Logger logger = LoggerFactory.getLogger(TaskManagerService.class);
    
    @Autowired
    private HiveService hiveService;
    
    // 存储任务状态和结果
    private final ConcurrentHashMap<String, Map<String, Object>> taskResults = new ConcurrentHashMap<>();
    
    /**
     * 异步执行分析任务
     * 
     * @param analysisType 分析类型
     * @param database 数据库名
     * @param table 表名
     * @param params 参数
     * @return 任务ID
     */
    public String submitAnalysisTask(String analysisType, String database, String table, Map<String, Object> params) {
        String taskId = UUID.randomUUID().toString();
        Map<String, Object> taskState = new HashMap<>();
        taskState.put("status", "SUBMITTED");
        taskState.put("progress", 0);
        taskState.put("startTime", System.currentTimeMillis());
        taskState.put("analysisType", analysisType);
        taskState.put("database", database);
        taskState.put("table", table);
        taskState.put("params", params);
        
        taskResults.put(taskId, taskState);
        
        // 异步执行任务
        executeAnalysisTask(taskId, analysisType, database, table, params);
        
        return taskId;
    }
    
    /**
     * 异步执行分析任务
     */
    @Async
    public void executeAnalysisTask(String taskId, String analysisType, String database, String table, Map<String, Object> params) {
        try {
            logger.info("开始执行异步分析任务: {}, 类型: {}, 数据库: {}, 表: {}", taskId, analysisType, database, table);
            
            // 更新状态为运行中
            Map<String, Object> taskState = taskResults.get(taskId);
            taskState.put("status", "RUNNING");
            taskResults.put(taskId, taskState);
            
            // 根据分析类型构建SQL
            String sql;
            List<Map<String, Object>> results;
            
            switch (analysisType) {
                case "distribution":
                    String field = (String) params.get("field");
                    sql = String.format(
                            "SELECT %s, COUNT(*) as count FROM %s.%s GROUP BY %s ORDER BY count DESC LIMIT 20",
                            field, database, table, field);
                    break;
                case "correlation":
                    @SuppressWarnings("unchecked")
                    List<String> fields = (List<String>) params.get("fields");
                    sql = String.format(
                            "SELECT %s FROM %s.%s LIMIT 1000", 
                            String.join(", ", fields), database, table);
                    break;
                case "time_series":
                    String timeField = (String) params.get("timeField");
                    String valueField = (String) params.get("valueField");
                    sql = String.format(
                            "SELECT %s, %s FROM %s.%s ORDER BY %s LIMIT 1000",
                            timeField, valueField, database, table, timeField);
                    break;
                case "clustering":
                case "regression":
                    @SuppressWarnings("unchecked")
                    List<String> clusterFields = (List<String>) params.get("fields");
                    sql = String.format(
                            "SELECT %s FROM %s.%s LIMIT 1000",
                            String.join(", ", clusterFields), database, table);
                    break;
                default:
                    throw new IllegalArgumentException("不支持的分析类型: " + analysisType);
            }
            
            // 执行查询
            logger.info("执行分析查询: {}", sql);
            results = hiveService.executeQuery(sql);
            
            // 更新任务状态和结果
            taskState.put("status", "COMPLETED");
            taskState.put("endTime", System.currentTimeMillis());
            taskState.put("result", results);
            taskState.put("progress", 100);
            taskResults.put(taskId, taskState);
            
            logger.info("异步分析任务完成: {}, 结果行数: {}", taskId, results.size());
        } catch (Exception e) {
            logger.error("执行异步分析任务失败: " + taskId, e);
            
            // 更新失败状态
            Map<String, Object> taskState = taskResults.get(taskId);
            taskState.put("status", "FAILED");
            taskState.put("endTime", System.currentTimeMillis());
            taskState.put("error", e.getMessage());
            taskResults.put(taskId, taskState);
        }
    }
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态和结果
     */
    public Map<String, Object> getTaskStatus(String taskId) {
        return taskResults.getOrDefault(taskId, new HashMap<>());
    }
    
    /**
     * 获取所有任务状态
     * 
     * @return 所有任务状态
     */
    public Map<String, Map<String, Object>> getAllTasks() {
        return new HashMap<>(taskResults);
    }
    
    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelTask(String taskId) {
        Map<String, Object> taskState = taskResults.get(taskId);
        if (taskState != null && ("SUBMITTED".equals(taskState.get("status")) || "RUNNING".equals(taskState.get("status")))) {
            taskState.put("status", "CANCELLED");
            taskState.put("endTime", System.currentTimeMillis());
            taskResults.put(taskId, taskState);
            return true;
        }
        return false;
    }
} 