package org.shiguang.module.hive.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 传感器数据导入服务
 * 将Kafka传感器数据导入到Hive中供分析使用
 */
@Service
public class SensorDataImportService {
    private static final Logger logger = LoggerFactory.getLogger(SensorDataImportService.class);

    @Autowired
    private HiveService hiveService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${hive.sensor.database:agri_data}")
    private String sensorDatabase;

    @Value("${hive.sensor.table:sensor_data}")
    private String sensorTable;

    @Value("${sensor.data.import.enabled:true}")
    private boolean importEnabled;

    @PostConstruct
    public void init() {
        logger.info("初始化传感器数据导入服务...");
        if (importEnabled) {
            logger.info("传感器数据导入服务已启用，目标数据库: {}, 表: {}", sensorDatabase, sensorTable);
            ensureDatabaseAndTableExist();
            // 初始化时导入一些示例数据（可选）
            // importSampleSensorData();
        } else {
            logger.info("传感器数据导入服务已禁用");
        }
    }

    /**
     * 确保数据库和表存在
     */
    private void ensureDatabaseAndTableExist() {
        try {
            // 创建数据库（如果不存在）
            logger.info("正在创建Hive数据库(如果不存在): {}", sensorDatabase);
            String createDbSql = "CREATE DATABASE IF NOT EXISTS " + sensorDatabase;
            boolean dbCreated = hiveService.executeUpdate(createDbSql);
            
            if (dbCreated) {
                logger.info("数据库创建或已存在: {}", sensorDatabase);
                
                // 检查表是否存在
                logger.info("检查表是否存在: {}.{}", sensorDatabase, sensorTable);
                String checkTableSql = "SHOW TABLES IN " + sensorDatabase + " LIKE '" + sensorTable + "'";
                List<Map<String, Object>> tableResult = hiveService.executeQuery(checkTableSql);
                
                if (tableResult.isEmpty()) {
                    logger.info("表不存在，开始创建表: {}.{}", sensorDatabase, sensorTable);
                    
                    // 创建传感器数据表
                    String createTableSQL = "CREATE TABLE " + sensorDatabase + "." + sensorTable + " (" +
                            "id STRING, " +
                            "sensorId STRING, " +
                            "sensorType STRING, " +
                            "value DOUBLE, " +
                            "unit STRING, " +
                            "event_time BIGINT, " +
                            "readableTime STRING, " +
                            "location STRING, " +
                            "batteryLevel INT, " +
                            "month STRING, " +
                            "year INT, " +
                            "day STRING" +
                            ") ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE";
                    
                    logger.info("执行创建表SQL: {}", createTableSQL);
                    boolean tableCreated = hiveService.executeUpdate(createTableSQL);
                    
                    if (tableCreated) {
                        logger.info("表创建成功: {}.{}", sensorDatabase, sensorTable);
                    } else {
                        logger.error("表创建失败: {}.{}", sensorDatabase, sensorTable);
                    }
                } else {
                    logger.info("表已存在: {}.{}", sensorDatabase, sensorTable);
                }
            } else {
                logger.error("数据库创建失败: {}", sensorDatabase);
            }
        } catch (Exception e) {
            logger.error("创建数据库或表时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 导入传感器数据到Hive
     * 
     * @param sensorDataJson 传感器数据JSON字符串
     * @return 是否导入成功
     */
    public boolean importSensorData(String sensorDataJson) {
        if (!importEnabled) {
            logger.debug("传感器数据导入服务已禁用");
            return false;
        }
        
        try {
            logger.debug("开始导入传感器数据到Hive: {}", sensorDataJson);
            
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
            
            // 确保数据库和表存在
            ensureDatabaseAndTableExist();
            
            // 构建插入SQL
            String insertSQL = "INSERT INTO " + sensorDatabase + "." + sensorTable + 
                " VALUES ('" + id + "', '" + 
                sensorId + "', '" + 
                sensorType + "', " + 
                value + ", '" + 
                unit + "', " + 
                timestamp + ", '" + 
                readableTime + "', '" + 
                location + "', " + 
                batteryLevel + ", '" + 
                month + "', " + 
                year + ", '" + 
                day + "')";
            
            logger.info("执行插入SQL: {}", insertSQL);
            boolean success = hiveService.executeUpdate(insertSQL);
            
            if (success) {
                logger.info("传感器数据导入成功: id={}, sensorId={}, type={}", id, sensorId, sensorType);
            } else {
                logger.error("传感器数据导入失败: sensorId={}, type={}", sensorId, sensorType);
            }
            
            return success;
        } catch (Exception e) {
            logger.error("导入传感器数据到Hive时出错: {}", e.getMessage(), e);
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
            logger.info("获取传感器数据表信息");
            
            // 检查表是否存在
            String checkTableSql = "SHOW TABLES IN " + sensorDatabase + " LIKE '" + sensorTable + "'";
            List<Map<String, Object>> tableResult = hiveService.executeQuery(checkTableSql);
            
            if (tableResult.isEmpty()) {
                logger.info("表不存在: {}.{}", sensorDatabase, sensorTable);
                info.put("exists", false);
                info.put("success", false);
                info.put("message", "表不存在");
                return info;
            }
            
            info.put("exists", true);
            
            // 获取数据表行数
            String countSql = "SELECT COUNT(*) AS count FROM " + sensorDatabase + "." + sensorTable;
            List<Map<String, Object>> countResult = hiveService.executeQuery(countSql);
            if (!countResult.isEmpty() && countResult.get(0).containsKey("count")) {
                info.put("rowCount", countResult.get(0).get("count"));
                logger.info("表行数: {}", countResult.get(0).get("count"));
            }
            
            // 获取最新数据时间
            String latestTimeSql = "SELECT MAX(readableTime) AS latestTime FROM " + sensorDatabase + "." + sensorTable;
            List<Map<String, Object>> timeResult = hiveService.executeQuery(latestTimeSql);
            if (!timeResult.isEmpty() && timeResult.get(0).containsKey("latestTime")) {
                info.put("latestTime", timeResult.get(0).get("latestTime"));
                logger.info("最新数据时间: {}", timeResult.get(0).get("latestTime"));
            }
            
            // 获取表结构信息
            String descTableSql = "DESCRIBE " + sensorDatabase + "." + sensorTable;
            List<Map<String, Object>> tableSchema = hiveService.executeQuery(descTableSql);
            info.put("schema", tableSchema);
            
            info.put("success", true);
        } catch (Exception e) {
            info.put("success", false);
            info.put("error", e.getMessage());
            logger.error("获取传感器数据表信息时出错: {}", e.getMessage(), e);
        }
        
        return info;
    }
} 