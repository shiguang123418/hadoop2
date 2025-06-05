package org.shiguang.module.hive.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.shiguang.entity.HiveAnalysisTask;
import org.shiguang.module.hive.repository.HiveAnalysisTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.ArrayList;

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
    
    @Autowired
    private HiveAnalysisTaskRepository taskRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    // 存储任务状态和结果的内存缓存
    private final ConcurrentHashMap<String, Map<String, Object>> taskResults = new ConcurrentHashMap<>();
    
    /**
     * 异步执行分析任务
     * 
     * @param analysisType 分析类型
     * @param database 数据库名
     * @param table 表名
     * @param params 参数
     * @param username 创建者用户名
     * @return 任务ID
     */
    public String submitAnalysisTask(String analysisType, String database, String table, Map<String, Object> params, String username) {
        String taskId = UUID.randomUUID().toString();
        
        // 创建内存中的任务状态
        Map<String, Object> taskState = new HashMap<>();
        taskState.put("status", "SUBMITTED");
        taskState.put("progress", 0);
        taskState.put("startTime", System.currentTimeMillis());
        taskState.put("analysisType", analysisType);
        taskState.put("database", database);
        taskState.put("table", table);
        taskState.put("params", params);
        
        // 存入内存缓存
        taskResults.put(taskId, taskState);
        
        // 创建数据库实体并保存
        try {
            HiveAnalysisTask task = new HiveAnalysisTask();
            task.setTaskId(taskId);
            task.setAnalysisType(analysisType);
            task.setDatabase(database);
            task.setTable(table);
            task.setParameters(objectMapper.writeValueAsString(params));
            task.setStatus("SUBMITTED");
            task.setProgress(0);
            task.setStartTime(LocalDateTime.now());
            task.setCreatedBy(username);
            
            taskRepository.save(task);
        
        // 异步执行任务
        executeAnalysisTask(taskId, analysisType, database, table, params);
        
        return taskId;
        } catch (JsonProcessingException e) {
            logger.error("保存任务参数失败", e);
            throw new RuntimeException("保存任务参数失败", e);
        }
    }

    /**
     * 异步执行分析任务
     */
    @Async
    public void executeAnalysisTask(String taskId, String analysisType, String database, String table, Map<String, Object> params) {
        try {
            logger.info("开始执行异步分析任务: {}, 类型: {}, 数据库: {}, 表: {}", taskId, analysisType, database, table);
            
            // 更新内存中的状态
            Map<String, Object> taskState = taskResults.get(taskId);
            taskState.put("status", "RUNNING");
            taskResults.put(taskId, taskState);
            
            // 更新数据库中的状态
            Optional<HiveAnalysisTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isPresent()) {
                HiveAnalysisTask task = taskOpt.get();
                task.setStatus("RUNNING");
                taskRepository.save(task);
            }
            
            // 根据分析类型构建SQL
            String sql;
            List<Map<String, Object>> results;
            
            switch (analysisType) {
                case "distribution":
                    String field = (String) params.get("field");
                    // 如果前端没有传递field参数，尝试从fields数组中获取第一个字段
                    if (field == null) {
                        @SuppressWarnings("unchecked")
                        List<String> fields = (List<String>) params.get("fields");
                        if (fields != null && !fields.isEmpty()) {
                            field = fields.get(0);
                            // 将field添加到参数中，方便前端使用
                            params.put("field", field);
                            logger.info("从fields参数中获取分布分析字段: {}", field);
                        } else {
                            throw new IllegalArgumentException("缺少必要的字段参数field或fields");
                        }
                    }
                    
                    sql = String.format(
                            "SELECT %s as category, COUNT(*) as count FROM %s.%s GROUP BY %s ORDER BY count DESC LIMIT 20",
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
                    
                    // 检查是否使用了valueFields参数
                    @SuppressWarnings("unchecked")
                    List<String> valueFields = (List<String>) params.get("valueFields");
                    
                    if (timeField == null) {
                        throw new IllegalArgumentException("缺少必要的时间字段参数timeField");
                    }
                    
                    // 优先使用valueFields，如果不存在则使用valueField
                    if (valueFields != null && !valueFields.isEmpty()) {
                        logger.info("使用valueFields参数: {}", valueFields);
                        // 使用第一个值字段作为SQL查询字段，后续会处理其他字段
                        valueField = valueFields.get(0);
                    } else if (valueField == null) {
                        throw new IllegalArgumentException("缺少必要的值字段参数valueField或valueFields");
                    }
                    
                    sql = String.format(
                            "SELECT %s, %s FROM %s.%s ORDER BY %s LIMIT 1000",
                            timeField, valueField, database, table, timeField);
                    
                    // 如果有多个值字段，则构建多个查询
                    if (valueFields != null && valueFields.size() > 1) {
                        List<String> queries = new ArrayList<>();
                        
                        for (String vField : valueFields) {
                            queries.add(String.format(
                                "SELECT %s, %s FROM %s.%s ORDER BY %s LIMIT 1000",
                                timeField, vField, database, table, timeField
                            ));
                        }
                        
                        // 将额外查询存储在参数中，供后续处理
                        params.put("multiValueQueries", queries);
                    }
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
            
            // 记录分析结果样本，方便调试
            if (results != null && !results.isEmpty()) {
                logger.info("分析结果第一条数据: {}", results.get(0));
                logger.info("分析结果共 {} 条数据", results.size());
            } else {
                logger.warn("分析查询未返回任何结果");
            }
            
            // 对时间序列数据进行特殊处理
            if ("time_series".equals(analysisType)) {
                String timeField = (String) params.get("timeField");
                String valueField = (String) params.get("valueField");
                
                // 检查是否使用了valueFields参数
                @SuppressWarnings("unchecked")
                List<String> valueFields = (List<String>) params.get("valueFields");
                
                // 处理主查询结果
                List<Map<String, Object>> processedResults = new ArrayList<>();
                
                // 如果是多值字段情况
                if (valueFields != null && valueFields.size() > 1) {
                    // 获取所有月份值，用于构建完整的数据点
                    Map<Object, Map<String, Object>> monthDataMap = new HashMap<>();
                    
                    // 处理主查询结果（第一个字段）
                    for (Map<String, Object> row : results) {
                        Object timeValue = row.get(timeField);
                        Object dataValue = row.get(valueFields.get(0)); // 使用第一个值字段
                        
                        Map<String, Object> newRow = new HashMap<>();
                        newRow.put("month", timeValue);
                        newRow.put(valueFields.get(0), dataValue); // 以字段名为键
                        
                        // 存入月份映射
                        monthDataMap.put(timeValue, newRow);
                    }
                    
                    // 处理额外的值字段
                    for (int i = 1; i < valueFields.size(); i++) {
                        String vField = valueFields.get(i);
                        
                        // 执行查询
                        String extraSql = String.format(
                                "SELECT %s, %s FROM %s.%s ORDER BY %s LIMIT 1000",
                                timeField, vField, database, table, timeField);
                        
                        List<Map<String, Object>> extraResults = hiveService.executeQuery(extraSql);
                        
                        // 添加到已有的月份数据中
                        for (Map<String, Object> row : extraResults) {
                            Object timeValue = row.get(timeField);
                            Object dataValue = row.get(vField);
                            
                            Map<String, Object> existingRow = monthDataMap.get(timeValue);
                            if (existingRow != null) {
                                existingRow.put(vField, dataValue);
                            } else {
                                // 如果月份不存在于主查询，创建新行
                                Map<String, Object> newRow = new HashMap<>();
                                newRow.put("month", timeValue);
                                newRow.put(vField, dataValue);
                                monthDataMap.put(timeValue, newRow);
                            }
                        }
                    }
                    
                    // 转换为列表
                    processedResults = new ArrayList<>(monthDataMap.values());
                    
                    // 记录生成的多字段结果
                    logger.info("生成多字段时间序列结果，共 {} 条数据，字段: {}", 
                            processedResults.size(), valueFields);
                } else {
                    // 单值字段处理，保持原有逻辑
                    for (Map<String, Object> row : results) {
                        Map<String, Object> newRow = new HashMap<>();
                        // 确保使用标准字段名
                        Object timeValue = row.get(timeField);
                        Object dataValue = row.get(valueField != null ? valueField : valueFields.get(0));
                        
                        // 明确设置字段名为month和value，避免前端解析问题
                        newRow.put("month", timeValue);
                        newRow.put("value", dataValue);
                        
                        processedResults.add(newRow);
                    }
                }
                
                // 替换原结果
                results = processedResults;
            }
            
            // 更新内存中的任务状态和结果
            taskState.put("status", "COMPLETED");
            taskState.put("endTime", System.currentTimeMillis());
            taskState.put("result", results);
            taskState.put("progress", 100);
            taskResults.put(taskId, taskState);
            
            // 更新数据库中的任务状态和结果
            taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isPresent()) {
                HiveAnalysisTask task = taskOpt.get();
                task.setStatus("COMPLETED");
                task.setEndTime(LocalDateTime.now());
                task.setProgress(100);
                task.setResultData(objectMapper.writeValueAsString(results));
                taskRepository.save(task);
            }
            
            logger.info("异步分析任务完成: {}, 结果行数: {}", taskId, results.size());
        } catch (Exception e) {
            logger.error("执行异步分析任务失败: " + taskId, e);
            
            // 更新内存中的失败状态
            Map<String, Object> taskState = taskResults.get(taskId);
            taskState.put("status", "FAILED");
            taskState.put("endTime", System.currentTimeMillis());
            taskState.put("error", e.getMessage());
            taskResults.put(taskId, taskState);
            
            // 更新数据库中的失败状态
            try {
                Optional<HiveAnalysisTask> taskOpt = taskRepository.findById(taskId);
                if (taskOpt.isPresent()) {
                    HiveAnalysisTask task = taskOpt.get();
                    task.setStatus("FAILED");
                    task.setEndTime(LocalDateTime.now());
                    task.setErrorMessage(e.getMessage());
                    taskRepository.save(task);
                }
            } catch (Exception ex) {
                logger.error("更新任务失败状态到数据库失败", ex);
            }
        }
    }
    
    /**
     * 获取任务状态
     * 
     * @param taskId 任务ID
     * @return 任务状态和结果
     */
    public Map<String, Object> getTaskStatus(String taskId) {
        // 先尝试从内存缓存获取
        Map<String, Object> result = taskResults.get(taskId);
        
        // 如果内存中没有，从数据库获取
        if (result == null) {
            Optional<HiveAnalysisTask> taskOpt = taskRepository.findById(taskId);
            if (taskOpt.isPresent()) {
                HiveAnalysisTask task = taskOpt.get();
                result = convertTaskToMap(task);
            } else {
                result = new HashMap<>();
            }
        }
        
        return result;
    }
    
    /**
     * 获取所有任务状态
     * 
     * @return 所有任务状态
     */
    public Map<String, Map<String, Object>> getAllTasks() {
        Map<String, Map<String, Object>> allTasks = new HashMap<>(taskResults);
        
        // 添加数据库中存在但内存中不存在的任务
        List<HiveAnalysisTask> dbTasks = taskRepository.findAll(Sort.by(Sort.Direction.DESC, "startTime"));
        for (HiveAnalysisTask task : dbTasks) {
            if (!allTasks.containsKey(task.getTaskId())) {
                allTasks.put(task.getTaskId(), convertTaskToMap(task));
            }
        }
        
        return allTasks;
    }
    
    /**
     * 获取用户的任务列表
     * 
     * @param username 用户名
     * @return 用户的任务列表
     */
    public List<Map<String, Object>> getUserTasks(String username) {
        List<HiveAnalysisTask> userTasks = taskRepository.findByCreatedBy(username);
        return userTasks.stream()
                .map(this::convertTaskToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * 获取最近的任务
     * 
     * @param limit 限制数量
     * @return 最近的任务列表
     */
    public List<Map<String, Object>> getRecentTasks(int limit) {
        List<HiveAnalysisTask> recentTasks = taskRepository.findRecentTasks(PageRequest.of(0, limit));
        return recentTasks.stream()
                .map(this::convertTaskToMap)
                .collect(Collectors.toList());
    }
    
    /**
     * 取消任务
     * 
     * @param taskId 任务ID
     * @return 是否成功取消
     */
    public boolean cancelTask(String taskId) {
        // 更新内存中的状态
        boolean cancelled = false;
        Map<String, Object> taskState = taskResults.get(taskId);
        if (taskState != null && ("SUBMITTED".equals(taskState.get("status")) || "RUNNING".equals(taskState.get("status")))) {
            taskState.put("status", "CANCELLED");
            taskState.put("endTime", System.currentTimeMillis());
            taskResults.put(taskId, taskState);
            cancelled = true;
        }
        
        // 更新数据库中的状态
        Optional<HiveAnalysisTask> taskOpt = taskRepository.findById(taskId);
        if (taskOpt.isPresent()) {
            HiveAnalysisTask task = taskOpt.get();
            if ("SUBMITTED".equals(task.getStatus()) || "RUNNING".equals(task.getStatus())) {
                task.setStatus("CANCELLED");
                task.setEndTime(LocalDateTime.now());
                taskRepository.save(task);
                cancelled = true;
        }
        }
        
        return cancelled;
    }
    
    /**
     * 删除任务
     * 
     * @param taskId 任务ID
     */
    public void deleteTask(String taskId) {
        // 从内存缓存中移除
        taskResults.remove(taskId);
        
        // 从数据库中删除
        taskRepository.deleteById(taskId);
    }
    
    /**
     * 将HiveAnalysisTask实体转换为Map
     */
    private Map<String, Object> convertTaskToMap(HiveAnalysisTask task) {
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("status", task.getStatus());
        result.put("progress", task.getProgress());
        result.put("analysisType", task.getAnalysisType());
        result.put("database", task.getDatabase());
        result.put("table", task.getTable());
        
        // 转换开始时间和结束时间
        if (task.getStartTime() != null) {
            result.put("startTime", task.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        
        if (task.getEndTime() != null) {
            result.put("endTime", task.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        }
        
        // 解析参数JSON
        try {
            if (task.getParameters() != null) {
                result.put("params", objectMapper.readValue(task.getParameters(), Map.class));
            }
        } catch (Exception e) {
            logger.error("解析任务参数失败", e);
            result.put("params", new HashMap<>());
        }
        
        // 解析结果JSON
        try {
            if (task.getResultData() != null) {
                result.put("result", objectMapper.readValue(task.getResultData(), List.class));
            }
        } catch (Exception e) {
            logger.error("解析任务结果失败", e);
            result.put("result", new ArrayList<>());
        }
        
        // 错误信息
        if (task.getErrorMessage() != null) {
            result.put("error", task.getErrorMessage());
        }
        
        result.put("createdBy", task.getCreatedBy());
        
        return result;
    }
} 