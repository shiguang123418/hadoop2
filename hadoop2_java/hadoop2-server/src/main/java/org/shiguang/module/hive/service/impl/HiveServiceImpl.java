package org.shiguang.module.hive.service.impl;

import org.shiguang.module.hive.service.HiveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.*;
import java.util.*;

/**
 * Hive服务实现类
 */
@Service
public class HiveServiceImpl implements HiveService {
    private static final Logger logger = LoggerFactory.getLogger(HiveServiceImpl.class);
    
    @Value("${hive.url}")
    private String hiveUrl;
    
    @Value("${hive.driver:org.apache.hive.jdbc.HiveDriver}")
    private String hiveDriver;
    
    @Value("${hive.username:}")
    private String hiveUsername;
    
    @Value("${hive.password:}")
    private String hivePassword;
    
    /**
     * 初始化
     */
    @PostConstruct
    public void init() {
        try {
            // 加载Hive JDBC驱动
            Class.forName(hiveDriver);
            logger.info("Hive服务初始化成功，连接URL: {}", hiveUrl);
        } catch (ClassNotFoundException e) {
            logger.error("Hive JDBC驱动加载失败", e);
        }
    }
    
    /**
     * 关闭资源
     */
    @PreDestroy
    public void destroy() {
        logger.info("Hive服务已关闭");
    }
    
    /**
     * 获取数据库连接
     */
    private Connection getConnection() throws SQLException {
        if (hiveUsername != null && !hiveUsername.isEmpty()) {
            return DriverManager.getConnection(hiveUrl, hiveUsername, hivePassword);
        } else {
            return DriverManager.getConnection(hiveUrl);
        }
    }
    
    /**
     * 关闭数据库资源
     */
    private void closeResources(Connection conn, Statement stmt, ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                logger.error("关闭ResultSet失败", e);
            }
        }
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                logger.error("关闭Statement失败", e);
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                logger.error("关闭Connection失败", e);
            }
        }
    }
    
    /**
     * 测试连接
     */
    private boolean testConnection() {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT 1");
            return rs.next();
        } catch (SQLException e) {
            logger.error("Hive连接测试失败", e);
            return false;
        } finally {
            closeResources(conn, stmt, rs);
        }
    }
    
    /**
     * 获取Hive连接状态
     */
    @Override
    public Map<String, Object> getConnectionStatus() {
        Map<String, Object> status = new HashMap<>();
        boolean connected = testConnection();
        status.put("connected", connected);
        status.put("url", hiveUrl);
        return status;
    }
    
    /**
     * 获取所有数据库列表
     */
    @Override
    public List<String> getDatabases() {
        List<String> databases = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SHOW DATABASES");
            
            while (rs.next()) {
                databases.add(rs.getString(1));
            }
        } catch (SQLException e) {
            logger.error("获取数据库列表失败", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return databases;
    }
    
    /**
     * 获取表列表
     */
    @Override
    public List<String> getTables(String database) {
        List<String> tables = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            
            // 如果指定了数据库，先切换到该数据库
            if (database != null && !database.isEmpty()) {
                stmt.execute("USE " + database);
            }
            
            rs = stmt.executeQuery("SHOW TABLES");
            
            while (rs.next()) {
                tables.add(rs.getString(1));
            }
        } catch (SQLException e) {
            logger.error("获取表列表失败", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return tables;
    }
    
    /**
     * 获取表结构
     */
    @Override
    public List<Map<String, Object>> getTableSchema(String tableName, String database) {
        List<Map<String, Object>> columns = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            
            // 如果指定了数据库，先切换到该数据库
            if (database != null && !database.isEmpty()) {
                stmt.execute("USE " + database);
            }
            
            rs = stmt.executeQuery("DESCRIBE " + tableName);
            
            while (rs.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("name", rs.getString(1));
                column.put("type", rs.getString(2));
                column.put("comment", rs.getString(3));
                columns.add(column);
            }
        } catch (SQLException e) {
            logger.error("获取表结构失败", e);
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return columns;
    }
    
    /**
     * 执行查询
     */
    @Override
    public List<Map<String, Object>> executeQuery(String sql) {
        List<Map<String, Object>> results = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                results.add(row);
            }
        } catch (SQLException e) {
            logger.error("执行查询失败: {}", sql, e);
            throw new RuntimeException("执行查询失败: " + e.getMessage(), e);
        } finally {
            closeResources(conn, stmt, rs);
        }
        
        return results;
    }
    
    /**
     * 执行更新操作（DDL语句）
     */
    @Override
    public Map<String, Object> executeUpdate(String sql) {
        Map<String, Object> result = new HashMap<>();
        Connection conn = null;
        Statement stmt = null;
        
        try {
            conn = getConnection();
            stmt = conn.createStatement();
            boolean isResultSet = stmt.execute(sql);
            
            if (isResultSet) {
                result.put("success", true);
                result.put("isQuery", true);
                result.put("message", "查询执行成功");
            } else {
                int updateCount = stmt.getUpdateCount();
                result.put("success", true);
                result.put("isQuery", false);
                result.put("rowsAffected", updateCount);
                result.put("message", "更新操作执行成功");
            }
        } catch (SQLException e) {
            logger.error("执行更新操作失败: {}", sql, e);
            result.put("success", false);
            result.put("error", e.getMessage());
        } finally {
            closeResources(conn, stmt, null);
        }
        
        return result;
    }
} 