package org.shiguang.module.hive.controller;

import org.shiguang.entity.dto.ApiResponse;
import org.shiguang.module.hive.service.HiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hive相关的REST API
 */
@RestController
@RequestMapping("/hive")
@CrossOrigin
public class HiveController {
    private static final Logger logger = LoggerFactory.getLogger(HiveController.class);
    
    @Value("${hive.url}")
    private String hiveUrl;
    
    @Autowired
    private HiveService hiveService;
    
    /**
     * 获取Hive状态
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHiveStatus() {
        try {
            Map<String, Object> status = hiveService.getConnectionStatus();
            return ResponseEntity.ok(new ApiResponse<>(200, "Hive状态获取成功", status));
        } catch (Exception e) {
            logger.error("获取Hive状态失败", e);
            Map<String, Object> status = new HashMap<>();
            status.put("connected", false);
            status.put("url", hiveUrl);
            status.put("error", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(200, "Hive状态获取成功", status));
        }
    }
    
    /**
     * 获取数据库列表
     */
    @GetMapping("/databases")
    public ResponseEntity<ApiResponse<List<String>>> getDatabases() {
        try {
            List<String> databases = hiveService.getDatabases();
            return ResponseEntity.ok(new ApiResponse<>(200, "获取数据库列表成功", databases));
        } catch (Exception e) {
            logger.error("获取数据库列表失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取数据库列表失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取表列表
     */
    @GetMapping("/tables")
    public ResponseEntity<ApiResponse<List<String>>> getTables(
            @RequestParam(required = false) String database) {
        try {
            List<String> tables = hiveService.getTables(database);
            return ResponseEntity.ok(new ApiResponse<>(200, "获取表列表成功", tables));
        } catch (Exception e) {
            logger.error("获取表列表失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取表列表失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 获取表结构
     */
    @GetMapping("/schema")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTableSchema(
            @RequestParam String table,
            @RequestParam(required = false) String database) {
        try {
            List<Map<String, Object>> schema = hiveService.getTableSchema(table, database);
            return ResponseEntity.ok(new ApiResponse<>(200, "获取表结构成功", schema));
        } catch (Exception e) {
            logger.error("获取表结构失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "获取表结构失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 执行查询
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> executeQuery(
            @RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "SQL查询不能为空", null));
        }
        
        try {
            logger.info("执行Hive查询: {}", sql);
            List<Map<String, Object>> results = hiveService.executeQuery(sql);
            return ResponseEntity.ok(new ApiResponse<>(200, "查询执行成功", results));
        } catch (Exception e) {
            logger.error("执行查询失败", e);
            return ResponseEntity.ok(new ApiResponse<>(500, "执行查询失败: " + e.getMessage(), null));
        }
    }
    
    /**
     * 执行更新操作
     */
    @PostMapping("/update")
    public ResponseEntity<ApiResponse<Map<String, Object>>> executeUpdate(
            @RequestBody Map<String, String> request) {
        String sql = request.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse<>(400, "SQL语句不能为空", null));
        }
        
        try {
            logger.info("执行Hive更新操作: {}", sql);
            Map<String, Object> result = hiveService.executeUpdate(sql);
            return ResponseEntity.ok(new ApiResponse<>(200, "更新操作执行成功", result));
        } catch (Exception e) {
            logger.error("执行更新操作失败", e);
            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("success", false);
            errorResult.put("error", e.getMessage());
            return ResponseEntity.ok(new ApiResponse<>(500, "执行更新操作失败: " + e.getMessage(), errorResult));
        }
    }
} 