package org.shiguang.db;

import org.shiguang.utils.ConfigKeys;
import org.shiguang.utils.ConfigManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * 数据库连接工具类
 */
public class DbConnection {
    private static String dbDriver;
    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    
    // 初始化数据库配置
    private static void initDbConfig() {
        if (dbDriver == null) {
            dbDriver = ConfigManager.getString(ConfigKeys.Database.DRIVER);
            dbUrl = ConfigManager.getString(ConfigKeys.Database.URL);
            dbUser = ConfigManager.getString(ConfigKeys.Database.USER);
            dbPassword = ConfigManager.getString(ConfigKeys.Database.PASSWORD);
        }
    }
    
    /**
     * 获取数据库连接
     */
    public static Connection getConnection() {
        initDbConfig();
        
        try {
            Class.forName(dbDriver);
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (Exception e) {
            System.out.println("获取数据库连接时出错: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 测试数据库连接
     */
    public static boolean testConnection() {
        Connection connection = null;
        try {
            connection = getConnection();
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * 初始化数据库表
     */
    public static void initDatabase() {
        Connection connection = null;
        Statement statement = null;
        
        try {
            connection = getConnection();
            statement = connection.createStatement();
            
            // 创建传感器数据表
            String createTableSQL = 
                "CREATE TABLE IF NOT EXISTS sensor_data (" +
                "  id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  sensor_id VARCHAR(50) NOT NULL," +
                "  timestamp TIMESTAMP NOT NULL," +
                "  temperature DOUBLE NOT NULL," +
                "  humidity DOUBLE NOT NULL," +
                "  soil_moisture DOUBLE NOT NULL," +
                "  light_intensity DOUBLE NOT NULL," +
                "  location VARCHAR(100) NOT NULL," +
                "  battery_level DOUBLE NOT NULL," +
                "  INDEX idx_sensor_time (sensor_id, timestamp)" +
                ")";
            
            statement.execute(createTableSQL);
            System.out.println("数据库表初始化成功");
            
        } catch (Exception e) {
            System.out.println("初始化数据库时出错: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
} 