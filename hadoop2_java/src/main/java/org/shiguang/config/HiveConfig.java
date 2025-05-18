package org.shiguang.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Hive配置类
 */
@Configuration
@Slf4j
public class HiveConfig {

    @Value("${hive.jdbc.url}")
    private String jdbcUrl;

    @Value("${hive.jdbc.username:}")
    private String username;

    @Value("${hive.jdbc.password:}")
    private String password;

    @Value("${hive.jdbc.driver}")
    private String driverClassName;

    /**
     * 初始化Hive JDBC驱动
     */
    @Bean(name = "hiveDriverInit")
    public Boolean initHiveDriver() {
        try {
            Class.forName(driverClassName);
            log.info("Hive JDBC驱动加载成功: {}", driverClassName);
            return true;
        } catch (ClassNotFoundException e) {
            log.error("Hive JDBC驱动加载失败", e);
            return false;
        }
    }

    /**
     * 获取Hive连接
     */
    public Connection getConnection() throws SQLException {
        if (username != null && !username.isEmpty()) {
            return DriverManager.getConnection(jdbcUrl, username, password);
        } else {
            return DriverManager.getConnection(jdbcUrl);
        }
    }

    /**
     * 测试Hive连接
     */
    @Bean(name = "hiveConnectionTest")
    public Boolean testHiveConnection(Boolean hiveDriverInit) {
        if (!hiveDriverInit) {
            log.error("Hive JDBC驱动未加载，无法测试连接");
            return false;
        }

        try (Connection connection = getConnection()) {
            log.info("Hive连接测试成功，URL: {}", jdbcUrl);
            return true;
        } catch (SQLException e) {
            log.error("Hive连接测试失败", e);
            return false;
        }
    }
} 