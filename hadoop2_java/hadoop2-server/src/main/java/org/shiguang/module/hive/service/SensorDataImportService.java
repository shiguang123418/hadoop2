package org.shiguang.module.hive.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
            // 初始化时导入一些示例数据
            importSampleSensorData();
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
            hiveService.executeUpdate(createDbSql);
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
                        "timestamp BIGINT, " +
                        "readableTime STRING, " +
                        "location STRING, " +
                        "batteryLevel INT, " +
                        "month STRING, " +
                        "year INT, " +
                        "day STRING" +
                        ") ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE";
                
                logger.info("执行创建表SQL: {}", createTableSQL);
                hiveService.executeUpdate(createTableSQL);
                logger.info("表创建成功: {}.{}", sensorDatabase, sensorTable);
            } else {
                logger.info("表已存在: {}.{}", sensorDatabase, sensorTable);
            }
        } catch (Exception e) {
            logger.error("创建数据库或表时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 导入示例传感器数据
     */
    private void importSampleSensorData() {
        try {
            // 检查表是否有数据
            logger.info("检查表中是否已有数据...");
            String countSql = "SELECT COUNT(*) AS count FROM " + sensorDatabase + "." + sensorTable;
            List<Map<String, Object>> countResult = hiveService.executeQuery(countSql);
            long count = 0;
            
            if (!countResult.isEmpty()) {
                Object countObj = countResult.get(0).get("count");
                if (countObj != null) {
                    count = Long.parseLong(countObj.toString());
                }
            }
            
            // 如果表中已有数据，则不导入示例数据
            if (count > 0) {
                logger.info("传感器数据表已有 {} 条数据，跳过示例数据导入", count);
                return;
            }
            
            logger.info("开始导入示例传感器数据...");
            
            // 传感器类型及其数据范围
            Map<String, Map<String, Object>> sensorTypes = new HashMap<>();
            
            // 温度传感器 (摄氏度)
            Map<String, Object> temperature = new HashMap<>();
            temperature.put("min", 15.0);
            temperature.put("max", 35.0);
            temperature.put("unit", "°C");
            sensorTypes.put("temperature", temperature);
            
            // 湿度传感器 (%)
            Map<String, Object> humidity = new HashMap<>();
            humidity.put("min", 30.0);
            humidity.put("max", 90.0);
            humidity.put("unit", "%");
            sensorTypes.put("humidity", humidity);
            
            // 土壤湿度传感器 (%)
            Map<String, Object> soilMoisture = new HashMap<>();
            soilMoisture.put("min", 20.0);
            soilMoisture.put("max", 80.0);
            soilMoisture.put("unit", "%");
            sensorTypes.put("soilMoisture", soilMoisture);
            
            // 光照传感器 (lux)
            Map<String, Object> light = new HashMap<>();
            light.put("min", 0.0);
            light.put("max", 80000.0);
            light.put("unit", "lux");
            sensorTypes.put("light", light);
            
            // 二氧化碳传感器 (ppm)
            Map<String, Object> co2 = new HashMap<>();
            co2.put("min", 400.0);
            co2.put("max", 2000.0);
            co2.put("unit", "ppm");
            sensorTypes.put("co2", co2);
            
            // 传感器ID和类型映射
            Map<String, String> sensorTypeMap = new HashMap<>();
            sensorTypeMap.put("sensor-001", "temperature");
            sensorTypeMap.put("sensor-002", "humidity");
            sensorTypeMap.put("sensor-003", "soilMoisture");
            sensorTypeMap.put("sensor-004", "light");
            sensorTypeMap.put("sensor-005", "co2");
            
            // 位置列表
            List<String> locations = Arrays.asList(
                "北京农场", "上海农场", "广州农场", "深圳农场", "成都农场"
            );
            
            Random random = new Random();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            
            // 生成6个月的数据，每个传感器每天3条记录
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MONTH, -6); // 从6个月前开始
            
            int totalInserted = 0;
            int batchSize = 0;
            StringBuilder insertSQL = new StringBuilder();
            
            // 一次只生成少量数据用于测试（每种传感器1条数据）
            for (Map.Entry<String, String> sensor : sensorTypeMap.entrySet()) {
                String sensorId = sensor.getKey();
                String sensorType = sensor.getValue();
                Map<String, Object> typeInfo = sensorTypes.get(sensorType);
                String location = locations.get(random.nextInt(locations.size()));
                
                // 设置时间
                calendar.set(Calendar.HOUR_OF_DAY, 8);
                calendar.set(Calendar.MINUTE, random.nextInt(60));
                calendar.set(Calendar.SECOND, random.nextInt(60));
                
                Date timestamp = calendar.getTime();
                String readableTime = sdf.format(timestamp);
                String monthStr = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
                String dayStr = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
                int year = calendar.get(Calendar.YEAR);
                
                // 生成传感器值
                double min = (double) typeInfo.get("min");
                double max = (double) typeInfo.get("max");
                double value = min + (max - min) * random.nextDouble();
                value = Math.round(value * 100) / 100.0; // 保留两位小数
                
                String unit = (String) typeInfo.get("unit");
                int batteryLevel = 50 + random.nextInt(50);
                
                // 生成UUID作为ID
                String id = UUID.randomUUID().toString();
                
                // 使用单行插入方式，这样更容易跟踪问题
                String singleInsertSQL = "INSERT INTO " + sensorDatabase + "." + sensorTable + 
                    " VALUES ('" + id + "', '" + 
                    sensorId + "', '" + 
                    sensorType + "', " + 
                    value + ", '" + 
                    unit + "', " + 
                    timestamp.getTime() + ", '" + 
                    readableTime + "', '" + 
                    location + "', " + 
                    batteryLevel + ", '" + 
                    monthStr + "', " + 
                    year + ", '" + 
                    dayStr + "')";
                
                logger.info("执行插入SQL: {}", singleInsertSQL);
                hiveService.executeUpdate(singleInsertSQL);
                
                totalInserted++;
                logger.info("成功插入第 {} 条数据", totalInserted);
            }
            
            logger.info("示例传感器数据导入完成，共导入 {} 条数据", totalInserted);
            
            // 确认数据已插入
            checkDataImported();
        } catch (Exception e) {
            logger.error("导入示例传感器数据时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查数据是否已成功导入
     */
    private void checkDataImported() {
        try {
            logger.info("检查数据是否已成功导入...");
            String countSql = "SELECT COUNT(*) AS count FROM " + sensorDatabase + "." + sensorTable;
            List<Map<String, Object>> countResult = hiveService.executeQuery(countSql);
            
            if (!countResult.isEmpty()) {
                Object countObj = countResult.get(0).get("count");
                if (countObj != null) {
                    long count = Long.parseLong(countObj.toString());
                    logger.info("当前表中有 {} 条数据", count);
                    
                    if (count > 0) {
                        // 查询一些示例数据
                        String sampleSql = "SELECT * FROM " + sensorDatabase + "." + sensorTable + " LIMIT 3";
                        List<Map<String, Object>> sampleResult = hiveService.executeQuery(sampleSql);
                        
                        logger.info("示例数据（{}条）:", sampleResult.size());
                        for (Map<String, Object> row : sampleResult) {
                            logger.info("数据行: {}", row);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("检查数据导入时出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 定时导入传感器数据到Hive（模拟）
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000) // 每小时执行一次
    public void scheduledDataImport() {
        if (!importEnabled) {
            return;
        }
        
        try {
            logger.info("执行计划任务：导入传感器数据");
            importTodaySensorData();
        } catch (Exception e) {
            logger.error("计划任务执行失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 导入今日传感器数据（模拟）
     */
    private void importTodaySensorData() {
        try {
            logger.info("开始导入今日传感器数据...");
            
            Calendar today = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Random random = new Random();
            
            // 传感器类型及其数据范围
            Map<String, Map<String, Object>> sensorTypes = new HashMap<>();
            
            // 温度传感器 (摄氏度)
            Map<String, Object> temperature = new HashMap<>();
            temperature.put("min", 15.0);
            temperature.put("max", 35.0);
            temperature.put("unit", "°C");
            sensorTypes.put("temperature", temperature);
            
            // 湿度传感器 (%)
            Map<String, Object> humidity = new HashMap<>();
            humidity.put("min", 30.0);
            humidity.put("max", 90.0);
            humidity.put("unit", "%");
            sensorTypes.put("humidity", humidity);
            
            // 土壤湿度传感器 (%)
            Map<String, Object> soilMoisture = new HashMap<>();
            soilMoisture.put("min", 20.0);
            soilMoisture.put("max", 80.0);
            soilMoisture.put("unit", "%");
            sensorTypes.put("soilMoisture", soilMoisture);
            
            // 光照传感器 (lux)
            Map<String, Object> light = new HashMap<>();
            light.put("min", 0.0);
            light.put("max", 80000.0);
            light.put("unit", "lux");
            sensorTypes.put("light", light);
            
            // 二氧化碳传感器 (ppm)
            Map<String, Object> co2 = new HashMap<>();
            co2.put("min", 400.0);
            co2.put("max", 2000.0);
            co2.put("unit", "ppm");
            sensorTypes.put("co2", co2);
            
            // 传感器ID和类型映射
            Map<String, String> sensorTypeMap = new HashMap<>();
            sensorTypeMap.put("sensor-001", "temperature");
            sensorTypeMap.put("sensor-002", "humidity");
            sensorTypeMap.put("sensor-003", "soilMoisture");
            sensorTypeMap.put("sensor-004", "light");
            sensorTypeMap.put("sensor-005", "co2");
            
            // 位置列表
            List<String> locations = Arrays.asList(
                "北京农场", "上海农场", "广州农场", "深圳农场", "成都农场"
            );
            
            int recordCount = 0;
            
            // 为每个传感器生成当天的数据
            for (Map.Entry<String, String> sensor : sensorTypeMap.entrySet()) {
                String sensorId = sensor.getKey();
                String sensorType = sensor.getValue();
                Map<String, Object> typeInfo = sensorTypes.get(sensorType);
                String location = locations.get(random.nextInt(locations.size()));
                
                // 生成当前小时的数据
                today.set(Calendar.MINUTE, random.nextInt(60));
                today.set(Calendar.SECOND, random.nextInt(60));
                
                Date timestamp = today.getTime();
                String readableTime = sdf.format(timestamp);
                String monthStr = String.format("%02d", today.get(Calendar.MONTH) + 1);
                String dayStr = String.format("%02d", today.get(Calendar.DAY_OF_MONTH));
                int year = today.get(Calendar.YEAR);
                
                // 生成传感器值
                double min = (double) typeInfo.get("min");
                double max = (double) typeInfo.get("max");
                double value = min + (max - min) * random.nextDouble();
                value = Math.round(value * 100) / 100.0; // 保留两位小数
                
                String unit = (String) typeInfo.get("unit");
                int batteryLevel = 50 + random.nextInt(50);
                
                // 生成UUID作为ID
                String id = UUID.randomUUID().toString();
                
                // 执行单行插入
                String insertSQL = "INSERT INTO " + sensorDatabase + "." + sensorTable + 
                    " VALUES ('" + id + "', '" + 
                    sensorId + "', '" + 
                    sensorType + "', " + 
                    value + ", '" + 
                    unit + "', " + 
                    timestamp.getTime() + ", '" + 
                    readableTime + "', '" + 
                    location + "', " + 
                    batteryLevel + ", '" + 
                    monthStr + "', " + 
                    year + ", '" + 
                    dayStr + "')";
                
                logger.info("执行插入SQL: {}", insertSQL);
                hiveService.executeUpdate(insertSQL);
                
                recordCount++;
            }
            
            logger.info("成功导入今日传感器数据，共 {} 条记录", recordCount);
            
            // 验证数据已插入
            checkDataImported();
        } catch (Exception e) {
            logger.error("导入今日传感器数据时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 手动触发导入传感器数据
     */
    public Map<String, Object> triggerImport() {
        Map<String, Object> result = new HashMap<>();
        
        if (!importEnabled) {
            result.put("success", false);
            result.put("message", "传感器数据导入服务已禁用");
            return result;
        }
        
        try {
            logger.info("手动触发传感器数据导入");
            importTodaySensorData();
            result.put("success", true);
            result.put("message", "已触发传感器数据导入");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
            logger.error("手动触发导入传感器数据时出错: {}", e.getMessage(), e);
        }
        
        return result;
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
            hiveService.executeQuery(countSql).stream().findFirst().ifPresent(row -> {
                info.put("rowCount", row.get("count"));
                logger.info("表行数: {}", row.get("count"));
            });
            
            // 获取最新数据时间
            String latestTimeSql = "SELECT MAX(readableTime) AS latestTime FROM " + sensorDatabase + "." + sensorTable;
            hiveService.executeQuery(latestTimeSql).stream().findFirst().ifPresent(row -> {
                info.put("latestTime", row.get("latestTime"));
                logger.info("最新数据时间: {}", row.get("latestTime"));
            });
            
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