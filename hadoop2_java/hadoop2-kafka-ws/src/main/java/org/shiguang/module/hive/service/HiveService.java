package org.shiguang.module.hive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hive服务
 * 提供与Hive数据库交互的基本功能
 */
@Service
public class HiveService {

    private static final Logger logger = LoggerFactory.getLogger(HiveService.class);

    @Value("${hive.url:jdbc:hive2://shiguang:10000}")
    private String hiveUrl;

    @Value("${hive.username:}")
    private String hiveUsername;

    @Value("${hive.password:}")
    private String hivePassword;

    @Value("${hive.driver:org.apache.hive.jdbc.HiveDriver}")
    private String hiveDriver;

    @PostConstruct
    public void init() {
        try {
            // 加载Hive JDBC驱动
            Class.forName(hiveDriver);
            logger.info("Hive JDBC驱动加载成功: {}", hiveDriver);
            
            // 测试连接
            try (Connection conn = getConnection()) {
                logger.info("Hive连接测试成功: {}", hiveUrl);
            }
        } catch (ClassNotFoundException e) {
            logger.error("Hive JDBC驱动加载失败: {}", e.getMessage(), e);
        } catch (SQLException e) {
            logger.error("Hive连接测试失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 获取Hive数据库连接
     * 
     * @return Hive数据库连接
     * @throws SQLException 如果连接失败
     */
    public Connection getConnection() throws SQLException {
        if (hiveUsername != null && !hiveUsername.isEmpty()) {
            return DriverManager.getConnection(hiveUrl, hiveUsername, hivePassword);
        } else {
            return DriverManager.getConnection(hiveUrl);
        }
    }

    /**
     * 执行Hive查询，返回结果集
     * 
     * @param sql SQL查询语句
     * @return 查询结果列表
     */
    public List<Map<String, Object>> executeQuery(String sql) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                
                resultList.add(row);
            }
            
            logger.debug("执行查询SQL成功: {}, 返回 {} 行结果", sql, resultList.size());
        } catch (SQLException e) {
            logger.error("执行查询SQL失败: {}, 错误: {}", sql, e.getMessage(), e);
        }
        
        return resultList;
    }

    /**
     * 执行Hive更新操作（DDL、DML）
     * 
     * @param sql SQL更新语句
     * @return 是否执行成功
     */
    public boolean executeUpdate(String sql) {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute(sql);
            logger.debug("执行更新SQL成功: {}", sql);
            return true;
        } catch (SQLException e) {
            logger.error("执行更新SQL失败: {}, 错误: {}", sql, e.getMessage(), e);
            return false;
        }
    }
} 