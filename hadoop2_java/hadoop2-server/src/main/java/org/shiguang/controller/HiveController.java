package org.shiguang.controller;

import org.shiguang.hadoop.HiveClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.ArrayList;
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

    // API响应基类
    public static class ApiResponse {
        private boolean success;
        private String message;

        public ApiResponse() {
        }

        public ApiResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    // 错误响应类
    public static class ErrorResponse extends ApiResponse {
        private String error;

        public ErrorResponse() {
            setSuccess(false);
        }

        public ErrorResponse(String error) {
            setSuccess(false);
            this.error = error;
        }

        public String getError() {
            return error;
        }

        public void setError(String error) {
            this.error = error;
        }
    }

    // 状态响应类
    public static class StatusResponse extends ApiResponse {
        private boolean connected;
        private String url;

        public StatusResponse(boolean connected) {
            super(true, connected ? "已连接" : "未连接");
            this.connected = connected;
        }

        public boolean isConnected() {
            return connected;
        }

        public void setConnected(boolean connected) {
            this.connected = connected;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
    
    // 表操作响应类
    public static class TableOperationResponse extends ApiResponse {
        private String tableName;
        
        public TableOperationResponse() {
            setSuccess(true);
        }
        
        public TableOperationResponse(boolean success, String tableName) {
            setSuccess(success);
            this.tableName = tableName;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
    }
    
    // 更新操作响应类
    public static class UpdateResponse extends ApiResponse {
        private int rowsAffected;
        
        public UpdateResponse(boolean success, int rowsAffected) {
            setSuccess(success);
            this.rowsAffected = rowsAffected;
        }

        public int getRowsAffected() {
            return rowsAffected;
        }

        public void setRowsAffected(int rowsAffected) {
            this.rowsAffected = rowsAffected;
        }
    }

    @Autowired
    private HiveClient hiveClient;

    /**
     * 获取Hive连接状态
     */
    @GetMapping("/status")
    public ResponseEntity<Object> getStatus() {
        logger.info("获取Hive连接状态");
        try {
            boolean connected = hiveClient.isConnected();
            StatusResponse status = new StatusResponse(connected);
            if (connected) {
                status.setUrl(hiveClient.getConnectionUrl());
            }
            logger.info("Hive连接状态: {}", status.isConnected());
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            logger.error("获取Hive连接状态失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
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
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            List<String> databases = hiveClient.getDatabases();
            logger.info("列出数据库成功, 数量: {}", databases.size());
            return ResponseEntity.ok(databases);
        } catch (Exception e) {
            logger.error("列出数据库失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
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
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
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
            ErrorResponse error = new ErrorResponse(e.getMessage());
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
            ErrorResponse error = new ErrorResponse("SQL语句不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("执行查询失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            List<Map<String, Object>> results = hiveClient.executeQuery(sql);
            logger.info("查询执行成功, 结果行数: {}", results.size());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("查询执行失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
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
            ErrorResponse error = new ErrorResponse("SQL语句不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("执行更新失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            int rowsAffected = hiveClient.executeUpdate(sql);
            UpdateResponse result = new UpdateResponse(true, rowsAffected);
            logger.info("更新执行成功, 影响行数: {}", rowsAffected);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("更新执行失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
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
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            List<Map<String, Object>> schema;
            if (database != null && !database.isEmpty()) {
                schema = hiveClient.getTableSchema(database, table);
            } else {
                schema = hiveClient.getTableSchema(table);
            }
            
            // 转换Hive DESCRIBE结果为标准格式
            List<Map<String, Object>> formattedSchema = new ArrayList<>();
            for (Map<String, Object> col : schema) {
                if (col.isEmpty()) {
                    continue;
                }
                
                Map<String, Object> formattedCol = new HashMap<>();
                // 尝试从Hive响应中提取列信息
                if (col.containsKey("col_name") && col.containsKey("data_type")) {
                    formattedCol.put("name", col.get("col_name"));
                    formattedCol.put("type", col.get("data_type"));
                    formattedCol.put("comment", col.get("comment") != null ? col.get("comment") : "");
                } else {
                    // 如果是其他格式，尝试按顺序提取
                    Object[] values = col.values().toArray();
                    if (values.length > 0) {
                        formattedCol.put("name", values[0]);
                        formattedCol.put("type", values.length > 1 ? values[1] : "");
                        formattedCol.put("comment", values.length > 2 ? values[2] : "");
                    }
                }
                
                if (!formattedCol.isEmpty()) {
                    formattedSchema.add(formattedCol);
                }
            }
            
            logger.info("获取表结构成功, 列数: {}", formattedSchema.size());
            return ResponseEntity.ok(formattedSchema);
        } catch (Exception e) {
            logger.error("获取表结构失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
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
            ErrorResponse error = new ErrorResponse("表名不能为空");
            return ResponseEntity.badRequest().body(error);
        }
        
        if (columns == null || columns.isEmpty()) {
            logger.warn("创建表失败: 列定义为空");
            ErrorResponse error = new ErrorResponse("列定义不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("创建表失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
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
                    ErrorResponse error = new ErrorResponse("列名和类型不能为空");
                    return ResponseEntity.badRequest().body(error);
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
            
            TableOperationResponse response = new TableOperationResponse(true, tableName);
            logger.info("创建表成功: {}", tableName);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("创建表失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
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
            ErrorResponse error = new ErrorResponse("表名不能为空");
            return ResponseEntity.badRequest().body(error);
        }

        try {
            // 检查连接状态
            if (!hiveClient.isConnected()) {
                logger.error("删除表失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            String sql = "DROP TABLE IF EXISTS " + name;
            int result = hiveClient.executeUpdate(sql);
            
            TableOperationResponse response = new TableOperationResponse(true, name);
            logger.info("删除表成功: {}", name);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("删除表失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 执行聚合分析
     */
    @PostMapping("/analyze/aggregate")
    public ResponseEntity<?> executeAggregateAnalysis(@RequestBody Map<String, Object> request) {
        logger.info("执行聚合分析: {}", request);
        
        try {
            if (!hiveClient.isConnected()) {
                logger.error("执行聚合分析失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            String tableName = (String) request.get("tableName");
            String aggregateColumn = (String) request.get("aggregateColumn");
            String aggregateFunction = (String) request.get("aggregateFunction");
            String groupByColumn = (String) request.get("groupByColumn");
            String whereClause = (String) request.get("whereClause");
            Integer limit = request.get("limit") != null ? ((Number) request.get("limit")).intValue() : null;
            
            if (tableName == null || aggregateColumn == null || aggregateFunction == null) {
                ErrorResponse error = new ErrorResponse("必须提供表名、聚合列和聚合函数");
                return ResponseEntity.badRequest().body(error);
            }
            
            List<Map<String, Object>> results = hiveClient.executeAggregateQuery(
                tableName, aggregateColumn, aggregateFunction, groupByColumn, whereClause, limit);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("执行聚合分析失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 执行时间序列分析
     */
    @PostMapping("/analyze/timeseries")
    public ResponseEntity<?> executeTimeSeriesAnalysis(@RequestBody Map<String, Object> request) {
        logger.info("执行时间序列分析: {}", request);
        
        try {
            if (!hiveClient.isConnected()) {
                logger.error("执行时间序列分析失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            String tableName = (String) request.get("tableName");
            String timeColumn = (String) request.get("timeColumn");
            String valueColumn = (String) request.get("valueColumn");
            String interval = (String) request.get("interval");
            String aggregateFunction = (String) request.get("aggregateFunction");
            String whereClause = (String) request.get("whereClause");
            Integer limit = request.get("limit") != null ? ((Number) request.get("limit")).intValue() : null;
            
            if (tableName == null || timeColumn == null || valueColumn == null || 
                interval == null || aggregateFunction == null) {
                ErrorResponse error = new ErrorResponse("必须提供表名、时间列、值列、时间间隔和聚合函数");
                return ResponseEntity.badRequest().body(error);
            }
            
            List<Map<String, Object>> results = hiveClient.executeTimeSeriesAnalysis(
                tableName, timeColumn, valueColumn, interval, aggregateFunction, whereClause, limit);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("执行时间序列分析失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 分析列值分布
     */
    @PostMapping("/analyze/distribution")
    public ResponseEntity<?> analyzeColumnDistribution(@RequestBody Map<String, Object> request) {
        logger.info("分析列值分布: {}", request);
        
        try {
            if (!hiveClient.isConnected()) {
                logger.error("分析列值分布失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            String tableName = (String) request.get("tableName");
            String columnName = (String) request.get("columnName");
            Integer limit = request.get("limit") != null ? ((Number) request.get("limit")).intValue() : null;
            
            if (tableName == null || columnName == null) {
                ErrorResponse error = new ErrorResponse("必须提供表名和列名");
                return ResponseEntity.badRequest().body(error);
            }
            
            List<Map<String, Object>> results = hiveClient.analyzeColumnDistribution(
                tableName, columnName, limit);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("分析列值分布失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 计算列统计信息
     */
    @PostMapping("/analyze/statistics")
    public ResponseEntity<?> calculateColumnStatistics(@RequestBody Map<String, Object> request) {
        logger.info("计算列统计信息: {}", request);
        
        try {
            if (!hiveClient.isConnected()) {
                logger.error("计算列统计信息失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            String tableName = (String) request.get("tableName");
            String columnName = (String) request.get("columnName");
            
            if (tableName == null || columnName == null) {
                ErrorResponse error = new ErrorResponse("必须提供表名和列名");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> results = hiveClient.calculateColumnStatistics(
                tableName, columnName);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("计算列统计信息失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 计算相关性
     */
    @PostMapping("/analyze/correlation")
    public ResponseEntity<?> calculateCorrelation(@RequestBody Map<String, Object> request) {
        logger.info("计算列相关性: {}", request);
        
        try {
            if (!hiveClient.isConnected()) {
                logger.error("计算列相关性失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            String tableName = (String) request.get("tableName");
            String column1 = (String) request.get("column1");
            String column2 = (String) request.get("column2");
            
            if (tableName == null || column1 == null || column2 == null) {
                ErrorResponse error = new ErrorResponse("必须提供表名和两个列名");
                return ResponseEntity.badRequest().body(error);
            }
            
            Map<String, Object> results = hiveClient.calculateCorrelation(
                tableName, column1, column2);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("计算列相关性失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 生成直方图数据
     */
    @PostMapping("/analyze/histogram")
    public ResponseEntity<?> generateHistogram(@RequestBody Map<String, Object> request) {
        logger.info("生成直方图数据: {}", request);
        
        try {
            if (!hiveClient.isConnected()) {
                logger.error("生成直方图数据失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            String tableName = (String) request.get("tableName");
            String columnName = (String) request.get("columnName");
            Integer numBuckets = request.get("numBuckets") != null ? 
                ((Number) request.get("numBuckets")).intValue() : 10;
            
            if (tableName == null || columnName == null) {
                ErrorResponse error = new ErrorResponse("必须提供表名和列名");
                return ResponseEntity.badRequest().body(error);
            }
            
            List<Map<String, Object>> results = hiveClient.generateHistogram(
                tableName, columnName, numBuckets);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("生成直方图数据失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
    
    /**
     * 执行透视分析
     */
    @PostMapping("/analyze/pivot")
    public ResponseEntity<?> executePivotAnalysis(@RequestBody Map<String, Object> request) {
        logger.info("执行透视分析: {}", request);
        
        try {
            if (!hiveClient.isConnected()) {
                logger.error("执行透视分析失败: Hive未连接");
                ErrorResponse error = new ErrorResponse("Hive未连接，服务不可用");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
            }
            
            String tableName = (String) request.get("tableName");
            String rowDimension = (String) request.get("rowDimension");
            String colDimension = (String) request.get("colDimension");
            String aggregateColumn = (String) request.get("aggregateColumn");
            String aggregateFunction = (String) request.get("aggregateFunction");
            Integer limit = request.get("limit") != null ? ((Number) request.get("limit")).intValue() : null;
            
            if (tableName == null || rowDimension == null || colDimension == null || 
                aggregateColumn == null || aggregateFunction == null) {
                ErrorResponse error = new ErrorResponse("必须提供表名、行维度、列维度、聚合列和聚合函数");
                return ResponseEntity.badRequest().body(error);
            }
            
            List<Map<String, Object>> results = hiveClient.executePivotAnalysis(
                tableName, rowDimension, colDimension, aggregateColumn, aggregateFunction, limit);
            
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("执行透视分析失败", e);
            ErrorResponse error = new ErrorResponse(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }
} 