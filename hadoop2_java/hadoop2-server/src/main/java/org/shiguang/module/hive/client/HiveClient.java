package org.shiguang.module.hive.client;

import org.shiguang.utils.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.*;
import java.util.*;

/**
 * Hive客户端工具类
 * 提供Hive查询的便捷方法
 */
@Component
public class HiveClient {
    private static final Logger logger = LoggerFactory.getLogger(HiveClient.class);
    
    @Value("${hive.jdbc.url}")
    private String jdbcUrl;
    
    @Value("${hive.jdbc.driver}")
    private String driverClass;
    
    @Value("${hive.jdbc.username:}")
    private String username;
    
    @Value("${hive.jdbc.password:}")
    private String password;
    
    private Connection connection;
    
    /**
     * 初始化Hive连接
     */
    @PostConstruct
    public void init() {
        try {
            Class.forName(driverClass);
            logger.info("加载Hive JDBC驱动: {}", driverClass);
            
            if (username != null && !username.isEmpty()) {
                connection = DriverManager.getConnection(jdbcUrl, username, password);
            } else {
                connection = DriverManager.getConnection(jdbcUrl);
            }
            
            logger.info("成功连接到Hive: {}", jdbcUrl);
        } catch (ClassNotFoundException e) {
            logger.error("未找到Hive JDBC驱动: {}", e.getMessage());
        } catch (SQLException e) {
            logger.error("连接Hive失败: {}", e.getMessage());
        }
    }
    
    /**
     * 关闭Hive连接
     */
    @PreDestroy
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Hive连接已关闭");
            } catch (SQLException e) {
                logger.error("关闭Hive连接时出错: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 获取数据库连接
     */
    public Connection getConnection() {
        return connection;
    }
    
    /**
     * 获取所有数据库列表
     */
    public List<String> getDatabases() throws SQLException {
        List<String> databases = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW DATABASES")) {
            
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
        }
        
        return databases;
    }
    
    /**
     * 切换当前数据库
     */
    public void useDatabase(String database) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("USE " + database);
            logger.info("已切换到数据库: {}", database);
        }
    }
    
    /**
     * 获取当前数据库中的表
     */
    public List<String> getTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW TABLES")) {
            
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        }
        
        return tables;
    }
    
    /**
     * 获取表结构
     */
    public List<Map<String, String>> getTableSchema(String tableName) throws SQLException {
        List<Map<String, String>> columns = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("DESCRIBE " + tableName)) {
            
            while (rs.next()) {
                Map<String, String> column = new HashMap<>();
                column.put("name", rs.getString(1));
                column.put("type", rs.getString(2));
                column.put("comment", rs.getString(3) == null ? "" : rs.getString(3));
                columns.add(column);
            }
        }
        
        return columns;
    }
    
    /**
     * 执行查询语句
     */
    public List<Map<String, Object>> executeQuery(String sql) throws SQLException {
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                
                resultList.add(row);
            }
        }
        
        return resultList;
    }
    
    /**
     * 执行更新语句
     */
    public int executeUpdate(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }
    
    /**
     * 执行DDL语句
     */
    public boolean execute(String sql) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            return stmt.execute(sql);
        }
    }
    
    /**
     * 判断表是否存在
     */
    public boolean tableExists(String tableName) throws SQLException {
        List<String> tables = getTables();
        return tables.contains(tableName.toLowerCase());
    }
} 