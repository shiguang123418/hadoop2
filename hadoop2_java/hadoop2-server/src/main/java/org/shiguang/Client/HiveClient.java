package org.shiguang.Client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Hive客户端，提供与Hive交互的功能
 */
@Component
public class HiveClient {
    private static final Logger logger = LoggerFactory.getLogger(HiveClient.class);

    @Value("${hive.url}")
    private String hiveUrl;

    @Value("${hive.username:}")
    private String username;

    @Value("${hive.password:}")
    private String password;

    @Value("${hive.database:default}")
    private String database;

    @Value("${hive.connection.timeout:30}")
    private int connectionTimeout;

    @Value("${hive.connection.required:false}")
    private boolean connectionRequired;

    private Connection connection;
    private boolean connected = false;

    @PostConstruct
    public void init() {
        try {
            // 注册Hive JDBC驱动
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            
            // 建立连接
            logger.info("尝试连接Hive: {}/{}", hiveUrl, database);
            if (username != null && !username.isEmpty()) {
                connection = DriverManager.getConnection(hiveUrl + "/" + database, username, password);
            } else {
                connection = DriverManager.getConnection(hiveUrl + "/" + database);
            }
            
            // 设置连接超时
            if (connection.isValid(connectionTimeout)) {
                connected = true;
                logger.info("Hive连接成功: {}/{}", hiveUrl, database);
            } else {
                throw new SQLException("Hive连接超时: " + hiveUrl + "/" + database);
            }
        } catch (ClassNotFoundException e) {
            logger.error("Hive JDBC驱动未找到", e);
            if (connectionRequired) {
                throw new RuntimeException("Hive JDBC驱动未找到", e);
            }
        } catch (SQLException e) {
            logger.error("Hive连接失败: {}", e.getMessage());
            if (connectionRequired) {
                throw new RuntimeException("无法连接到Hive，应用无法启动", e);
            } else {
                logger.warn("Hive不可用，应用将以有限功能运行");
            }
        }
    }

    /**
     * 检查Hive连接状态
     * @return 是否已连接
     */
    public boolean isConnected() {
        if (!connected || connection == null) {
            return false;
        }
        
        try {
            return connection.isValid(connectionTimeout);
        } catch (SQLException e) {
            logger.error("检查Hive连接失败", e);
            return false;
        }
    }

    /**
     * 执行查询语句
     * @param sql SQL查询语句
     * @return 查询结果列表
     * @throws SQLException 如果发生SQL错误
     */
    public List<Map<String, Object>> executeQuery(String sql) throws SQLException {
        validateConnection();
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            
            ResultSetMetaData metaData = resultSet.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (resultSet.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = resultSet.getObject(i);
                    row.put(columnName, value);
                }
                resultList.add(row);
            }
        }
        
        return resultList;
    }

    /**
     * 执行更新语句（DDL或DML）
     * @param sql SQL语句
     * @return 受影响的行数或0
     * @throws SQLException 如果发生SQL错误
     */
    public int executeUpdate(String sql) throws SQLException {
        validateConnection();
        try (Statement statement = connection.createStatement()) {
            boolean isResultSet = statement.execute(sql);
            if (!isResultSet) {
                return statement.getUpdateCount();
            }
            return 0;
        }
    }

    /**
     * 执行带参数的预编译SQL语句
     * @param sql 预编译SQL语句
     * @param params 参数列表
     * @return 查询结果列表
     * @throws SQLException 如果发生SQL错误
     */
    public List<Map<String, Object>> executeQueryWithParams(String sql, Object... params) throws SQLException {
        validateConnection();
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                preparedStatement.setObject(i + 1, params[i]);
            }
            
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                ResultSetMetaData metaData = resultSet.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                while (resultSet.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        String columnName = metaData.getColumnName(i);
                        Object value = resultSet.getObject(i);
                        row.put(columnName, value);
                    }
                    resultList.add(row);
                }
            }
        }
        
        return resultList;
    }

    /**
     * 检查表是否存在
     * @param tableName 表名
     * @return 表是否存在
     * @throws SQLException 如果发生SQL错误
     */
    public boolean tableExists(String tableName) throws SQLException {
        validateConnection();
        String sql = "SHOW TABLES LIKE '" + tableName + "'";
        List<Map<String, Object>> result = executeQuery(sql);
        return !result.isEmpty();
    }

    /**
     * 获取表结构
     * @param tableName 表名
     * @return 表结构信息
     * @throws SQLException 如果发生SQL错误
     */
    public List<Map<String, Object>> getTableSchema(String tableName) throws SQLException {
        validateConnection();
        String sql = "DESCRIBE " + tableName;
        return executeQuery(sql);
    }
    
    /**
     * 获取指定数据库中表的结构
     * @param database 数据库名
     * @param tableName 表名
     * @return 表结构信息
     * @throws SQLException 如果发生SQL错误
     */
    public List<Map<String, Object>> getTableSchema(String database, String tableName) throws SQLException {
        validateConnection();
        String currentDb = this.database;
        try {
            useDatabase(database);
            String sql = "DESCRIBE " + tableName;
            return executeQuery(sql);
        } finally {
            // 恢复原始数据库
            useDatabase(currentDb);
        }
    }
    
    /**
     * 获取数据库列表
     * @return 数据库列表
     * @throws SQLException 如果发生SQL错误
     */
    public List<String> getDatabases() throws SQLException {
        validateConnection();
        List<String> databases = new ArrayList<>();
        List<Map<String, Object>> results = executeQuery("SHOW DATABASES");
        
        for (Map<String, Object> row : results) {
            String dbName = row.values().iterator().next().toString();
            databases.add(dbName);
        }
        
        return databases;
    }
    
    /**
     * 获取表列表
     * @return 表列表
     * @throws SQLException 如果发生SQL错误
     */
    public List<String> getTables() throws SQLException {
        validateConnection();
        List<String> tables = new ArrayList<>();
        List<Map<String, Object>> results = executeQuery("SHOW TABLES");
        
        for (Map<String, Object> row : results) {
            String tableName = row.values().iterator().next().toString();
            tables.add(tableName);
        }
        
        return tables;
    }
    
    /**
     * 获取指定数据库中的表列表
     * @param database 数据库名
     * @return 表列表
     * @throws SQLException 如果发生SQL错误
     */
    public List<String> getTables(String database) throws SQLException {
        validateConnection();
        String currentDb = this.database;
        try {
            useDatabase(database);
            return getTables();
        } finally {
            // 恢复原始数据库
            useDatabase(currentDb);
        }
    }
    
    /**
     * 切换数据库
     * @param dbName 数据库名称
     * @throws SQLException 如果发生SQL错误
     */
    public void useDatabase(String dbName) throws SQLException {
        validateConnection();
        executeUpdate("USE " + dbName);
        this.database = dbName;
    }
    
    /**
     * 创建数据库
     * @param dbName 数据库名称
     * @throws SQLException 如果发生SQL错误
     */
    public void createDatabase(String dbName) throws SQLException {
        validateConnection();
        executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
    }
    
    /**
     * 删除数据库
     * @param dbName 数据库名称
     * @param cascade 是否级联删除（删除该数据库中的所有表）
     * @throws SQLException 如果发生SQL错误
     */
    public void dropDatabase(String dbName, boolean cascade) throws SQLException {
        validateConnection();
        if (cascade) {
            executeUpdate("DROP DATABASE IF EXISTS " + dbName + " CASCADE");
        } else {
            executeUpdate("DROP DATABASE IF EXISTS " + dbName);
        }
    }

    /**
     * 获取连接
     * @return Hive JDBC连接
     */
    public Connection getConnection() {
        return connection;
    }
    
    /**
     * 获取连接URL
     * @return Hive JDBC连接URL
     */
    public String getConnectionUrl() {
        return hiveUrl + "/" + database;
    }
    
    /**
     * 验证连接是否有效
     * @throws SQLException 如果连接无效
     */
    private void validateConnection() throws SQLException {
        if (!connected || connection == null) {
            throw new SQLException("Hive未连接，无法执行操作");
        }
        
        try {
            if (connection.isClosed() || !connection.isValid(connectionTimeout)) {
                try {
                    init(); // 尝试重新初始化连接
                    if (!isConnected()) {
                        throw new SQLException("无法重新建立Hive连接");
                    }
                } catch (Exception e) {
                    throw new SQLException("Hive连接已关闭或无效，重新连接失败", e);
                }
            }
        } catch (SQLException e) {
            logger.error("Hive连接验证失败", e);
            throw new SQLException("Hive连接已关闭或无效，无法执行操作", e);
        }
    }

    @PreDestroy
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Hive连接已关闭");
            } catch (SQLException e) {
                // 记录关闭连接失败的错误
                logger.error("关闭Hive连接失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 执行聚合分析查询
     * @param tableName 表名
     * @param aggregateColumn 需要聚合的列
     * @param aggregateFunction 聚合函数（COUNT, SUM, AVG, MAX, MIN）
     * @param groupByColumn 分组列（可选）
     * @param whereClause WHERE子句（可选）
     * @param limit 结果限制数（可选）
     * @return 聚合分析结果
     * @throws SQLException 如果发生SQL错误
     */
    public List<Map<String, Object>> executeAggregateQuery(
            String tableName, 
            String aggregateColumn, 
            String aggregateFunction, 
            String groupByColumn, 
            String whereClause, 
            Integer limit) throws SQLException {
        
        validateConnection();
        
        // 构建SQL
        StringBuilder sqlBuilder = new StringBuilder("SELECT ");
        
        // 如果有分组列，添加到SELECT子句
        if (groupByColumn != null && !groupByColumn.trim().isEmpty()) {
            sqlBuilder.append(groupByColumn).append(", ");
        }
        
        // 添加聚合函数
        sqlBuilder.append(aggregateFunction).append("(")
                .append(aggregateColumn)
                .append(") AS aggregated_value");
        
        // 添加FROM子句
        sqlBuilder.append(" FROM ").append(tableName);
        
        // 添加WHERE子句（如果有）
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sqlBuilder.append(" WHERE ").append(whereClause);
        }
        
        // 添加GROUP BY子句（如果有分组列）
        if (groupByColumn != null && !groupByColumn.trim().isEmpty()) {
            sqlBuilder.append(" GROUP BY ").append(groupByColumn);
        }
        
        // 添加ORDER BY子句，按聚合值降序排序
        sqlBuilder.append(" ORDER BY aggregated_value DESC");
        
        // 添加LIMIT子句（如果有）
        if (limit != null && limit > 0) {
            sqlBuilder.append(" LIMIT ").append(limit);
        }
        
        String sql = sqlBuilder.toString();
        logger.info("执行聚合分析查询: {}", sql);
        
        return executeQuery(sql);
    }
    
    /**
     * 执行时间序列分析
     * @param tableName 表名
     * @param timeColumn 时间列
     * @param valueColumn 值列
     * @param interval 时间间隔（例如：'1 day', '1 month', '1 year'）
     * @param aggregateFunction 聚合函数
     * @param whereClause WHERE子句（可选）
     * @param limit 结果限制数（可选）
     * @return 时间序列分析结果
     * @throws SQLException 如果发生SQL错误
     */
    public List<Map<String, Object>> executeTimeSeriesAnalysis(
            String tableName,
            String timeColumn,
            String valueColumn,
            String interval,
            String aggregateFunction,
            String whereClause,
            Integer limit) throws SQLException {
        
        validateConnection();
        
        // 构建SQL - 注意：这里使用了Hive的时间函数，可能需要根据Hive版本进行调整
        StringBuilder sqlBuilder = new StringBuilder("SELECT ");
        
        // 添加时间分组
        sqlBuilder.append("TRUNC(").append(timeColumn).append(", '").append(interval).append("') AS time_group, ");
        
        // 添加聚合函数
        sqlBuilder.append(aggregateFunction).append("(")
                .append(valueColumn)
                .append(") AS aggregated_value");
        
        // 添加FROM子句
        sqlBuilder.append(" FROM ").append(tableName);
        
        // 添加WHERE子句（如果有）
        if (whereClause != null && !whereClause.trim().isEmpty()) {
            sqlBuilder.append(" WHERE ").append(whereClause);
        }
        
        // 添加GROUP BY和ORDER BY子句
        sqlBuilder.append(" GROUP BY TRUNC(").append(timeColumn).append(", '").append(interval).append("')");
        sqlBuilder.append(" ORDER BY time_group");
        
        // 添加LIMIT子句（如果有）
        if (limit != null && limit > 0) {
            sqlBuilder.append(" LIMIT ").append(limit);
        }
        
        String sql = sqlBuilder.toString();
        logger.info("执行时间序列分析: {}", sql);
        
        return executeQuery(sql);
    }
    
    /**
     * 统计列值分布
     * @param tableName 表名
     * @param columnName 列名
     * @param limit 结果限制数
     * @return 列值分布统计
     * @throws SQLException 如果发生SQL错误
     */
    public List<Map<String, Object>> analyzeColumnDistribution(
            String tableName,
            String columnName,
            Integer limit) throws SQLException {
        
        validateConnection();
        
        String sql = "SELECT " + columnName + ", COUNT(*) AS count " +
                    "FROM " + tableName + " " +
                    "GROUP BY " + columnName + " " +
                    "ORDER BY count DESC";
        
        if (limit != null && limit > 0) {
            sql += " LIMIT " + limit;
        }
        
        logger.info("分析列值分布: {}", sql);
        return executeQuery(sql);
    }
    
    /**
     * 计算列的基本统计信息
     * @param tableName 表名
     * @param columnName 列名
     * @return 基本统计信息
     * @throws SQLException 如果发生SQL错误
     */
    public Map<String, Object> calculateColumnStatistics(
            String tableName,
            String columnName) throws SQLException {
        
        validateConnection();
        
        String sql = "SELECT " +
                    "COUNT(" + columnName + ") AS count, " +
                    "AVG(" + columnName + ") AS avg, " +
                    "STDDEV(" + columnName + ") AS stddev, " +
                    "MIN(" + columnName + ") AS min, " +
                    "MAX(" + columnName + ") AS max " +
                    "FROM " + tableName;
        
        logger.info("计算列统计信息: {}", sql);
        List<Map<String, Object>> results = executeQuery(sql);
        
        if (results.isEmpty()) {
            return new HashMap<>();
        }
        
        return results.get(0);
    }
    
    /**
     * 执行相关性分析（两列之间）
     * @param tableName 表名
     * @param column1 列1
     * @param column2 列2
     * @return 相关性分析结果（简化版）
     * @throws SQLException 如果发生SQL错误
     */
    public Map<String, Object> calculateCorrelation(
            String tableName,
            String column1,
            String column2) throws SQLException {
        
        validateConnection();
        
        // 在Hive中计算相关系数（使用协方差/标准差）
        String sql = "SELECT " +
                    "AVG(" + column1 + " * " + column2 + ") - (AVG(" + column1 + ") * AVG(" + column2 + ")) AS covariance, " +
                    "STDDEV(" + column1 + ") AS stddev1, " +
                    "STDDEV(" + column2 + ") AS stddev2 " +
                    "FROM " + tableName;
        
        logger.info("计算相关性: {}", sql);
        List<Map<String, Object>> results = executeQuery(sql);
        
        if (results.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> data = results.get(0);
        
        Double covariance = (Double) data.get("covariance");
        Double stddev1 = (Double) data.get("stddev1");
        Double stddev2 = (Double) data.get("stddev2");
        
        if (covariance != null && stddev1 != null && stddev2 != null && stddev1 > 0 && stddev2 > 0) {
            Double correlation = covariance / (stddev1 * stddev2);
            result.put("correlation", correlation);
        } else {
            result.put("correlation", null);
        }
        
        result.put("column1", column1);
        result.put("column2", column2);
        
        return result;
    }
    
    /**
     * 生成列的直方图数据
     * @param tableName 表名
     * @param columnName 列名
     * @param numBuckets 分桶数
     * @return 直方图数据
     * @throws SQLException 如果发生SQL错误
     */
    public List<Map<String, Object>> generateHistogram(
            String tableName,
            String columnName,
            int numBuckets) throws SQLException {
        
        validateConnection();
        
        // 首先获取列的最小值和最大值
        String minMaxSql = "SELECT MIN(" + columnName + ") AS min_val, MAX(" + columnName + ") AS max_val FROM " + tableName;
        List<Map<String, Object>> minMaxResult = executeQuery(minMaxSql);
        
        if (minMaxResult.isEmpty()) {
            return new ArrayList<>();
        }
        
        Map<String, Object> minMax = minMaxResult.get(0);
        double minVal = ((Number) minMax.get("min_val")).doubleValue();
        double maxVal = ((Number) minMax.get("max_val")).doubleValue();
        
        // 计算桶宽度
        double bucketWidth = (maxVal - minVal) / numBuckets;
        
        List<Map<String, Object>> histogramData = new ArrayList<>();
        
        for (int i = 0; i < numBuckets; i++) {
            double lowerBound = minVal + (i * bucketWidth);
            double upperBound = minVal + ((i + 1) * bucketWidth);
            
            String bucketSql = "SELECT COUNT(*) AS bucket_count FROM " + tableName + " WHERE " +
                              columnName + " >= " + lowerBound + " AND " +
                              columnName + " < " + (i == numBuckets - 1 ? maxVal + 1 : upperBound);
            
            List<Map<String, Object>> bucketResult = executeQuery(bucketSql);
            
            if (!bucketResult.isEmpty()) {
                Map<String, Object> bucket = new LinkedHashMap<>();
                bucket.put("bucket", i + 1);
                bucket.put("lower_bound", lowerBound);
                bucket.put("upper_bound", i == numBuckets - 1 ? maxVal : upperBound);
                bucket.put("count", bucketResult.get(0).get("bucket_count"));
                histogramData.add(bucket);
            }
        }
        
        return histogramData;
    }
    
    /**
     * 执行数据透视分析
     * @param tableName 表名 
     * @param rowDimension 行维度（分组列）
     * @param colDimension 列维度（分组列）
     * @param aggregateColumn 聚合列
     * @param aggregateFunction 聚合函数
     * @param limit 结果限制数
     * @return 透视表数据
     * @throws SQLException 如果发生SQL错误
     */
    public List<Map<String, Object>> executePivotAnalysis(
            String tableName,
            String rowDimension,
            String colDimension,
            String aggregateColumn,
            String aggregateFunction,
            Integer limit) throws SQLException {
        
        validateConnection();
        
        // 在Hive中，实现透视表需要使用条件聚合和CASE WHEN语句
        // 首先，获取所有唯一的列维度值
        String colValuesSql = "SELECT DISTINCT " + colDimension + " FROM " + tableName + " ORDER BY " + colDimension;
        List<Map<String, Object>> colValues = executeQuery(colValuesSql);
        
        // 构建透视SQL
        StringBuilder pivotSql = new StringBuilder("SELECT " + rowDimension);
        
        // 为每个列维度值添加条件聚合
        for (Map<String, Object> colValue : colValues) {
            String colVal = colValue.values().iterator().next().toString();
            pivotSql.append(", ")
                   .append(aggregateFunction)
                   .append("(CASE WHEN ")
                   .append(colDimension)
                   .append(" = '")
                   .append(colVal.replace("'", "''")) // 转义单引号
                   .append("' THEN ")
                   .append(aggregateColumn)
                   .append(" END) AS `")
                   .append(colVal.replace("`", "``")) // 转义反引号
                   .append("`");
        }
        
        // 完成SQL
        pivotSql.append(" FROM ")
               .append(tableName)
               .append(" GROUP BY ")
               .append(rowDimension)
               .append(" ORDER BY ")
               .append(rowDimension);
        
        if (limit != null && limit > 0) {
            pivotSql.append(" LIMIT ").append(limit);
        }
        
        String sql = pivotSql.toString();
        logger.info("执行透视分析: {}", sql);
        
        return executeQuery(sql);
    }
} 