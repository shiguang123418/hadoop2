package org.shiguang.db;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 基于Spring数据源的数据库连接工具类
 * 该类会替代手动管理的DbConnection
 */
@Component
@Primary
public class SpringDbConnection {
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 获取数据库连接
     */
    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (Exception e) {
            throw new RuntimeException("无法获取数据库连接", e);
        }
    }
} 