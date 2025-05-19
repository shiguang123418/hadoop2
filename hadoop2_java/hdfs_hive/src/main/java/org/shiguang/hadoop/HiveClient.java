package org.shiguang.hadoop;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hive客户端，提供与Hive交互的功能
 */
@Component
public class HiveClient {

    @Value("${hive.url:jdbc:hive2://localhost:10000}")
    private String hiveUrl;

    @Value("${hive.username:}")
    private String username;

    @Value("${hive.password:}")
    private String password;

    @Value("${hive.database:default}")
    private String database;

    @Value("${hive.connection.timeout:30}")
    private int connectionTimeout;

    private Connection connection;

    @PostConstruct
    public void init() throws SQLException {
        try {
            // 注册Hive JDBC驱动
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            
            // 建立连接
            if (username != null && !username.isEmpty()) {
                connection = DriverManager.getConnection(hiveUrl + "/" + database, username, password);
            } else {
                connection = DriverManager.getConnection(hiveUrl + "/" + database);
            }
            
            // 设置连接超时
            if (connection.isValid(connectionTimeout)) {
                System.out.println("Hive连接成功: " + hiveUrl + "/" + database);
            } else {
                throw new SQLException("Hive连接超时: " + hiveUrl + "/" + database);
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Hive JDBC驱动未找到", e);
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
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(connectionTimeout)) {
                init(); // 重新初始化连接
            }
        } catch (SQLException e) {
            throw new SQLException("Hive连接已关闭或无效，无法执行操作", e);
        }
    }

    @PreDestroy
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Hive连接已关闭");
            } catch (SQLException e) {
                // 记录关闭连接失败的错误
                System.err.println("关闭Hive连接失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
} 