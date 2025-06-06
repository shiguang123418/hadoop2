package org.shiguang.module.mysql.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * MySQL传感器数据服务
 * 负责将传感器数据存储到MySQL数据库中
 */
@Service
public class MySQLSensorService {
    private static final Logger logger = LoggerFactory.getLogger(MySQLSensorService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sensor.data.storage.mysql-enabled:true}")
    private boolean mysqlEnabled;

    @Value("${mysql.sensor.database:agri_data}")
    private String sensorDatabase;

    @Value("${mysql.sensor.table:sensor_data}")
    private String sensorTable;
    
    @Value("${spring.datasource.url}")
    private String jdbcUrl;
    
    @Value("${spring.datasource.username}")
    private String jdbcUsername;
    
    @Value("${spring.datasource.password}")
    private String jdbcPassword;
    
    @Value("${spring.datasource.driver-class-name}")
    private String driverClassName;
    
    private Connection connection;

    @PostConstruct
    public void init() {
        if (!mysqlEnabled) {
            logger.info("MySQL传感器数据服务已禁用");
            return;
        }

        logger.info("初始化MySQL传感器数据服务...");
        try {
            // 加载JDBC驱动
            Class.forName(driverClassName);
            logger.info("MySQL JDBC驱动加载成功: {}", driverClassName);
            
            // 建立数据库连接
            establishConnection();
            
            // 创建表
            createTableIfNotExists();
            logger.info("MySQL传感器数据服务初始化完成, 数据库: {}, 表: {}", sensorDatabase, sensorTable);
        } catch (Exception e) {
            logger.error("MySQL传感器数据服务初始化失败: {}", e.getMessage(), e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        closeConnection();
        logger.info("MySQL传感器数据服务已关闭");
    }
    
    /**
     * 建立数据库连接
     */
    private void establishConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
            logger.info("MySQL数据库连接已建立");
        }
    }
    
    /**
     * 关闭数据库连接
     */
    private void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("MySQL数据库连接已关闭");
            } catch (SQLException e) {
                logger.error("关闭MySQL数据库连接时出错: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 获取数据库连接
     */
    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            establishConnection();
        }
        return connection;
    }

    /**
     * 创建传感器数据表（如果不存在）
     */
    private void createTableIfNotExists() {
        try {
            // 确保表名不包含路径分隔符或特殊字符
            String safeTableName = sensorTable.replace(".", "_").replace("-", "_");
            
            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + safeTableName + " (" +
                    "id VARCHAR(36) PRIMARY KEY, " +
                    "sensor_id VARCHAR(36) NOT NULL, " +
                    "sensor_type VARCHAR(50) NOT NULL, " +
                    "value DOUBLE NOT NULL, " +
                    "unit VARCHAR(20), " +
                    "event_time BIGINT NOT NULL, " +
                    "readable_time DATETIME, " +
                    "location VARCHAR(100), " +
                    "battery_level INT, " +
                    "month VARCHAR(2), " +
                    "year INT, " +
                    "day VARCHAR(2), " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "INDEX idx_sensor_type (sensor_type), " +
                    "INDEX idx_event_time (event_time), " +
                    "INDEX idx_location (location), " +
                    "INDEX idx_readable_time (readable_time)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci";

            logger.info("创建MySQL表SQL: {}", createTableSQL);
            
            try (Statement stmt = getConnection().createStatement()) {
                stmt.execute(createTableSQL);
            }
            
            logger.info("MySQL传感器数据表创建成功或已存在: {}", safeTableName);
            
            // 更新内部使用的表名
            sensorTable = safeTableName;
        } catch (Exception e) {
            logger.error("创建MySQL传感器数据表时出错: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 导入传感器数据到MySQL
     *
     * @param sensorDataJson 传感器数据JSON字符串
     * @return 是否导入成功
     */
    public boolean importSensorData(String sensorDataJson) {
        if (!mysqlEnabled) {
            logger.debug("MySQL传感器数据服务已禁用");
            return false;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            logger.debug("开始导入传感器数据到MySQL: {}", sensorDataJson);

            // 解析传感器数据JSON
            JsonNode jsonNode = objectMapper.readTree(sensorDataJson);
            String sensorId = jsonNode.path("sensorId").asText();
            String sensorType = jsonNode.path("sensorType").asText();
            double value = jsonNode.path("value").asDouble();
            String unit = jsonNode.path("unit").asText();
            long timestamp = jsonNode.path("timestamp").asLong();
            String location = jsonNode.path("location").asText();
            int batteryLevel = jsonNode.path("batteryLevel").asInt();

            // 生成日期相关字段
            Date date = new Date(timestamp);
            SimpleDateFormat readableFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String readableTime = readableFormat.format(date);

            SimpleDateFormat monthFormat = new SimpleDateFormat("MM");
            String month = monthFormat.format(date);

            SimpleDateFormat dayFormat = new SimpleDateFormat("dd");
            String day = dayFormat.format(date);

            SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy");
            int year = Integer.parseInt(yearFormat.format(date));

            // 生成UUID作为ID
            String id = UUID.randomUUID().toString();

            // 构建插入SQL
            String insertSQL = "INSERT INTO " + sensorTable + " (" +
                    "id, sensor_id, sensor_type, value, unit, event_time, readable_time, " +
                    "location, battery_level, month, year, day) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            conn = getConnection();
            pstmt = conn.prepareStatement(insertSQL);
            pstmt.setString(1, id);
            pstmt.setString(2, sensorId);
            pstmt.setString(3, sensorType);
            pstmt.setDouble(4, value);
            pstmt.setString(5, unit);
            pstmt.setLong(6, timestamp);
            pstmt.setString(7, readableTime);
            pstmt.setString(8, location);
            pstmt.setInt(9, batteryLevel);
            pstmt.setString(10, month);
            pstmt.setInt(11, year);
            pstmt.setString(12, day);
            
            int rowsAffected = pstmt.executeUpdate();
            boolean success = rowsAffected > 0;
            
            if (success) {
                logger.info("传感器数据导入MySQL成功: id={}, sensorId={}, type={}", id, sensorId, sensorType);
            } else {
                logger.error("传感器数据导入MySQL失败: sensorId={}, type={}", sensorId, sensorType);
            }

            return success;
        } catch (Exception e) {
            logger.error("导入传感器数据到MySQL时出错: {}", e.getMessage(), e);
            return false;
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    logger.error("关闭PreparedStatement时出错", e);
                }
            }
        }
    }

    /**
     * 获取传感器数据表信息
     */
    public Map<String, Object> getSensorTableInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("database", extractDatabaseName());
        info.put("table", sensorTable);

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            logger.info("获取MySQL传感器数据表信息");

            conn = getConnection();
            stmt = conn.createStatement();
            
            // 检查表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = DATABASE() AND table_name = '" + sensorTable + "'";
            
            rs = stmt.executeQuery(checkTableSql);
            boolean tableExists = false;
            if (rs.next()) {
                tableExists = rs.getInt(1) > 0;
            }
            
            if (!tableExists) {
                logger.info("MySQL表不存在: {}", sensorTable);
                info.put("exists", false);
                info.put("success", false);
                info.put("message", "表不存在");
                return info;
            }

            info.put("exists", true);
            rs.close();

            // 获取数据表行数
            String countSql = "SELECT COUNT(*) FROM " + sensorTable;
            rs = stmt.executeQuery(countSql);
            if (rs.next()) {
                info.put("rowCount", rs.getInt(1));
                logger.info("MySQL表行数: {}", rs.getInt(1));
            }
            rs.close();

            // 获取最新数据时间
            String latestTimeSql = "SELECT MAX(readable_time) FROM " + sensorTable;
            rs = stmt.executeQuery(latestTimeSql);
            if (rs.next()) {
                info.put("latestTime", rs.getString(1));
                logger.info("MySQL最新数据时间: {}", rs.getString(1));
            }
            rs.close();

            // 获取表结构信息
            String descTableSql = "DESCRIBE " + sensorTable;
            rs = stmt.executeQuery(descTableSql);
            
            List<Map<String, Object>> tableSchema = new ArrayList<>();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> column = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    column.put(metaData.getColumnName(i), rs.getObject(i));
                }
                tableSchema.add(column);
            }
            
            info.put("schema", tableSchema);
            info.put("success", true);
        } catch (Exception e) {
            info.put("success", false);
            info.put("error", e.getMessage());
            logger.error("获取MySQL传感器数据表信息时出错: {}", e.getMessage(), e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("关闭ResultSet时出错", e);
                }
            }
            if (stmt != null) {
                try {
                    stmt.close();
                } catch (SQLException e) {
                    logger.error("关闭Statement时出错", e);
                }
            }
        }

        return info;
    }
    
    /**
     * 从JDBC URL中提取数据库名称
     */
    private String extractDatabaseName() {
        try {
            // 尝试从JDBC URL中提取数据库名称
            if (jdbcUrl != null && jdbcUrl.contains("?")) {
                String[] parts = jdbcUrl.split("\\?")[0].split("/");
                if (parts.length > 0) {
                    return parts[parts.length - 1];
                }
            }
        } catch (Exception e) {
            logger.warn("无法从JDBC URL提取数据库名称: {}", e.getMessage());
        }
        return sensorDatabase;
    }
    
    /**
     * 执行查询并返回结果列表
     */
    public List<Map<String, Object>> executeQuery(String sql, Object... params) {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            
            // 设置参数
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            
            rs = pstmt.executeQuery();
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
        } catch (SQLException e) {
            logger.error("执行查询失败: {}", sql, e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("关闭ResultSet时出错", e);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    logger.error("关闭PreparedStatement时出错", e);
                }
            }
        }
        
        return resultList;
    }
    
    /**
     * 执行单值查询
     */
    public <T> T queryForObject(String sql, Class<T> type, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            
            // 设置参数
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                return (T) rs.getObject(1);
            }
        } catch (SQLException e) {
            logger.error("执行单值查询失败: {}", sql, e);
        } finally {
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    logger.error("关闭ResultSet时出错", e);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    logger.error("关闭PreparedStatement时出错", e);
                }
            }
        }
        
        return null;
    }
    
    /**
     * 执行更新操作
     */
    public int executeUpdate(String sql, Object... params) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        
        try {
            conn = getConnection();
            pstmt = conn.prepareStatement(sql);
            
            // 设置参数
            for (int i = 0; i < params.length; i++) {
                pstmt.setObject(i + 1, params[i]);
            }
            
            return pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("执行更新失败: {}", sql, e);
            return 0;
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    logger.error("关闭PreparedStatement时出错", e);
                }
            }
        }
    }
} 