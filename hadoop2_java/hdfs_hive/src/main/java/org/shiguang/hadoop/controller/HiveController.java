package org.shiguang.hadoop.controller;

import org.shiguang.hadoop.HiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hive操作的REST API控制器
 */
@RestController
@RequestMapping("/api/hive")
@CrossOrigin
public class HiveController {

    @Autowired
    private HiveClient hiveClient;

    /**
     * 获取Hive连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        try {
            boolean connected = hiveClient.getConnection() != null;
            status.put("connected", connected);
            if (connected) {
                status.put("url", hiveClient.getConnectionUrl());
            }
            return ResponseEntity.ok(status);
        } catch (Exception e) {
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
        try {
            List<String> databases = hiveClient.getDatabases();
            return ResponseEntity.ok(databases);
        } catch (Exception e) {
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
        try {
            List<String> tables;
            if (database != null && !database.isEmpty()) {
                tables = hiveClient.getTables(database);
            } else {
                tables = hiveClient.getTables();
            }
            return ResponseEntity.ok(tables);
        } catch (Exception e) {
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
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "SQL语句不能为空"));
        }

        try {
            List<Map<String, Object>> results = hiveClient.executeQuery(sql);
            return ResponseEntity.ok(results);
        } catch (Exception e) {
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
        if (sql == null || sql.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "SQL语句不能为空"));
        }

        try {
            int rowsAffected = hiveClient.executeUpdate(sql);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("rowsAffected", rowsAffected);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
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
        try {
            List<Map<String, Object>> schema;
            if (database != null && !database.isEmpty()) {
                schema = hiveClient.getTableSchema(database, table);
            } else {
                schema = hiveClient.getTableSchema(table);
            }
            return ResponseEntity.ok(schema);
        } catch (Exception e) {
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
        List<Map<String, Object>> columns = (List<Map<String, Object>>) tableParams.get("columns");
        
        if (tableName == null || tableName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "表名不能为空"));
        }
        
        if (columns == null || columns.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "列定义不能为空"));
        }

        try {
            // 构建CREATE TABLE语句
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
            
            for (int i = 0; i < columns.size(); i++) {
                Map<String, Object> column = columns.get(i);
                String columnName = (String) column.get("name");
                String columnType = (String) column.get("type");
                String columnComment = (String) column.get("comment");
                
                if (columnName == null || columnType == null) {
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
            
            // 执行创建表
            int result = hiveClient.executeUpdate(sqlBuilder.toString());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tableName", tableName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
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
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "表名不能为空"));
        }

        try {
            String sql = "DROP TABLE IF EXISTS " + name;
            int result = hiveClient.executeUpdate(sql);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tableName", name);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 