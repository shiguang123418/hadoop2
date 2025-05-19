package org.shiguang.hadoop.controller;

import org.shiguang.hadoop.HiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hive操作的REST API控制器
 */
@RestController
@RequestMapping("/hive")
@CrossOrigin
public class HiveController {
    private static final Logger logger = LoggerFactory.getLogger(HiveController.class);

    @Autowired
    private HiveClient hiveClient;

    /**
     * 获取Hive连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        logger.info("获取Hive连接状态");
        Map<String, Object> status = new HashMap<>();
        try {
            boolean connected = hiveClient.isConnected();
            status.put("connected", connected);
            if (connected) {
                status.put("url", hiveClient.getConnectionUrl());
            }
            logger.info("Hive连接状态: {}", status);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("获取Hive连接状态失败", e);
            status.put("connected", false);
            status.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(status);
        }
    }

    /**
     * 列出所有数据库
     */
    @GetMapping("/databases")
    public ResponseEntity<?> listDatabases() {
        logger.info("列出所有数据库");
        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("列出数据库失败: Hive未连接");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Hive未连接，服务不可用"));
            }
            
            List<String> databases = hiveClient.getDatabases();
            logger.info("列出数据库成功, 数量: {}", databases.size());
            return ResponseEntity.ok(databases);
        } catch (Exception e) {
            logger.error("列出数据库失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 列出所有表
     */
    @GetMapping("/tables")
    public ResponseEntity<?> listTables(@RequestParam(required = false) String database) {
        logger.info("列出数据库表, database={}", database);
        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("列出表失败: Hive未连接");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Hive未连接，服务不可用"));
            }
            
            List<String> tables;
            if (database != null && !database.isEmpty()) {
                tables = hiveClient.getTables(database);
            } else {
                tables = hiveClient.getTables();
            }
            logger.info("列出表成功, 数量: {}", tables.size());
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
            logger.error("列出表失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 执行查询操作
     */
    @PostMapping("/query")
    public ResponseEntity<?> executeQuery(@RequestBody Map<String, String> queryParams) {
        String sql = queryParams.get("sql");
        logger.info("执行查询操作, SQL: {}", sql);
        
        if (sql == null || sql.trim().isEmpty()) {
            logger.warn("执行查询失败: SQL语句为空");
            return ResponseEntity.badRequest().body(Map.of("error", "SQL语句不能为空"));
        }

        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("执行查询失败: Hive未连接");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Hive未连接，服务不可用"));
            }
            
            List<Map<String, Object>> results = hiveClient.executeQuery(sql);
            logger.info("查询执行成功, 结果行数: {}", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("查询执行失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 执行更新操作
     */
    @PostMapping("/update")
    public ResponseEntity<?> executeUpdate(@RequestBody Map<String, String> updateParams) {
        String sql = updateParams.get("sql");
        logger.info("执行更新操作, SQL: {}", sql);
        
        if (sql == null || sql.trim().isEmpty()) {
            logger.warn("执行更新失败: SQL语句为空");
            return ResponseEntity.badRequest().body(Map.of("error", "SQL语句不能为空"));
        }

        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("执行更新失败: Hive未连接");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Hive未连接，服务不可用"));
            }
            
            int rowsAffected = hiveClient.executeUpdate(sql);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rowsAffected", rowsAffected);
            logger.info("更新执行成功, 影响行数: {}", rowsAffected);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("更新执行失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取表结构
     */
    @GetMapping("/schema")
    public ResponseEntity<?> getTableSchema(@RequestParam String table, @RequestParam(required = false) String database) {
        logger.info("获取表结构, table: {}, database: {}", table, database);
        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("获取表结构失败: Hive未连接");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Hive未连接，服务不可用"));
            }
            
            List<Map<String, Object>> schema;
            if (database != null && !database.isEmpty()) {
                schema = hiveClient.getTableSchema(database, table);
            } else {
                schema = hiveClient.getTableSchema(table);
            }
            logger.info("获取表结构成功, 列数: {}", schema.size());
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
            logger.error("获取表结构失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 创建表
     */
    @PostMapping("/table")
    public ResponseEntity<?> createTable(@RequestBody Map<String, Object> tableParams) {
        String tableName = (String) tableParams.get("name");
        logger.info("创建表: {}", tableName);
        
        List<Map<String, Object>> columns = (List<Map<String, Object>>) tableParams.get("columns");
        
        if (tableName == null || tableName.trim().isEmpty()) {
            logger.warn("创建表失败: 表名为空");
            return ResponseEntity.badRequest().body(Map.of("error", "表名不能为空"));
        }
        
        if (columns == null || columns.isEmpty()) {
            logger.warn("创建表失败: 列定义为空");
            return ResponseEntity.badRequest().body(Map.of("error", "列定义不能为空"));
        }

        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("创建表失败: Hive未连接");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Hive未连接，服务不可用"));
            }
            
            // 构建CREATE TABLE语句
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
            
            for (int i = 0; i < columns.size(); i++) {
                Map<String, Object> column = columns.get(i);
                String columnName = (String) column.get("name");
                String columnType = (String) column.get("type");
                String columnComment = (String) column.get("comment");
                
                if (columnName == null || columnType == null) {
                    logger.warn("创建表失败: 列名或类型为空");
                    return ResponseEntity.badRequest().body(Map.of("error", "列名和类型不能为空"));
                }
                
                sqlBuilder.append(columnName).append(" ").append(columnType);
                
                if (columnComment != null && !columnComment.isEmpty()) {
                    sqlBuilder.append(" COMMENT '").append(columnComment).append("'");
                }
                
                if (i < columns.size() - 1) {
                    sqlBuilder.append(", ");
                }
            }
            
            sqlBuilder.append(")");
            
            // 表注释
            String tableComment = (String) tableParams.get("comment");
            if (tableComment != null && !tableComment.isEmpty()) {
                sqlBuilder.append(" COMMENT '").append(tableComment).append("'");
            }
            
            // 文件格式
            String fileFormat = (String) tableParams.get("fileFormat");
            if (fileFormat != null && !fileFormat.isEmpty()) {
                sqlBuilder.append(" STORED AS ").append(fileFormat);
            }
            
            String sql = sqlBuilder.toString();
            logger.debug("创建表SQL: {}", sql);
            
            // 执行创建表
            int result = hiveClient.executeUpdate(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tableName", tableName);
            logger.info("创建表成功: {}", tableName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建表失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 删除表
     */
    @DeleteMapping("/table")
    public ResponseEntity<?> dropTable(@RequestParam String name) {
        logger.info("删除表: {}", name);
        
        if (name == null || name.trim().isEmpty()) {
            logger.warn("删除表失败: 表名为空");
            return ResponseEntity.badRequest().body(Map.of("error", "表名不能为空"));
        }

        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("删除表失败: Hive未连接");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", "Hive未连接，服务不可用"));
            }
            
            String sql = "DROP TABLE IF EXISTS " + name;
            int result = hiveClient.executeUpdate(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tableName", name);
            logger.info("删除表成功: {}", name);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除表失败", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 