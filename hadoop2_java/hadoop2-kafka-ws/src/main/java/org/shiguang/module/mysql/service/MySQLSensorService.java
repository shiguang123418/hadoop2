package org.shiguang.module.mysql.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * MySQL传感器数据服务
 * 负责将传感器数据存储到MySQL数据库中
 */
@Service
public class MySQLSensorService {
    private static final Logger logger = LoggerFactory.getLogger(MySQLSensorService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${sensor.data.storage.mysql-enabled:true}")
    private boolean mysqlEnabled;

    @Value("${hive.sensor.database:agri_data}")
    private String sensorDatabase;

    @Value("${hive.sensor.table:sensor_data}")
    private String sensorTable;

    @PostConstruct
    public void init() {
        if (!mysqlEnabled) {
            logger.info("MySQL传感器数据服务已禁用");
            return;
        }

        logger.info("初始化MySQL传感器数据服务...");
        try {
            createTableIfNotExists();
            logger.info("MySQL传感器数据服务初始化完成");
        } catch (Exception e) {
            logger.error("MySQL传感器数据服务初始化失败: {}", e.getMessage(), e);
        }
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
            jdbcTemplate.execute(createTableSQL);
            logger.info("MySQL传感器数据表创建成功或已存在: {}", safeTableName);
            
            // 更新内部使用的表名
            sensorTable = safeTableName;
        } catch (Exception e) {
            logger.error("创建MySQL传感器数据表时出错: {}", e.getMessage(), e);
            throw e;
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

            int rowsAffected = jdbcTemplate.update(insertSQL,
                    id, sensorId, sensorType, value, unit, timestamp, readableTime,
                    location, batteryLevel, month, year, day);

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
        }
    }

    /**
     * 获取传感器数据表信息
     */
    public Map<String, Object> getSensorTableInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("database", sensorDatabase);
        info.put("table", sensorTable);

        try {
            logger.info("获取MySQL传感器数据表信息");

            // 检查表是否存在
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables " +
                    "WHERE table_schema = DATABASE() AND table_name = ?";
            
            Integer tableExists = jdbcTemplate.queryForObject(checkTableSql, Integer.class, sensorTable);
            
            if (tableExists == null || tableExists == 0) {
                logger.info("MySQL表不存在: {}", sensorTable);
                info.put("exists", false);
                info.put("success", false);
                info.put("message", "表不存在");
                return info;
            }

            info.put("exists", true);

            // 获取数据表行数
            String countSql = "SELECT COUNT(*) FROM " + sensorTable;
            Integer rowCount = jdbcTemplate.queryForObject(countSql, Integer.class);
            info.put("rowCount", rowCount);
            logger.info("MySQL表行数: {}", rowCount);

            // 获取最新数据时间
            String latestTimeSql = "SELECT MAX(readable_time) FROM " + sensorTable;
            String latestTime = jdbcTemplate.queryForObject(latestTimeSql, String.class);
            info.put("latestTime", latestTime);
            logger.info("MySQL最新数据时间: {}", latestTime);

            // 获取表结构信息
            String descTableSql = "DESCRIBE " + sensorTable;
            List<Map<String, Object>> tableSchema = jdbcTemplate.queryForList(descTableSql);
            info.put("schema", tableSchema);

            info.put("success", true);
        } catch (Exception e) {
            info.put("success", false);
            info.put("error", e.getMessage());
            logger.error("获取MySQL传感器数据表信息时出错: {}", e.getMessage(), e);
        }

        return info;
    }
} 