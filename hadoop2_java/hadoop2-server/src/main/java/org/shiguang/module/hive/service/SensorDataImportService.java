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
            hiveService.executeUpdate("CREATE DATABASE IF NOT EXISTS " + sensorDatabase);
            
            // 创建传感器数据表（如果不存在）
            String createTableSQL = "CREATE TABLE IF NOT EXISTS " + sensorDatabase + "." + sensorTable + " (" +
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
                    ") STORED AS PARQUET";
            
            hiveService.executeUpdate(createTableSQL);
            logger.info("已确保传感器数据表存在: {}.{}", sensorDatabase, sensorTable);
        } catch (Exception e) {
            logger.error("创建数据库或表时出错", e);
        }
    }

    /**
     * 导入示例传感器数据
     */
    private void importSampleSensorData() {
        try {
            // 检查表是否有数据
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
            
            // 生成6个月的数据
            for (int month = 0; month < 6; month++) {
                // 每月生成数据
                for (int day = 1; day <= 30; day++) {
                    calendar.set(Calendar.DAY_OF_MONTH, day);
                    
                    // 调整当前日期
                    if (day > calendar.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                        continue; // 跳过超出当月最大天数的日期
                    }
                    
                    // 每个传感器每天生成3条记录
                    for (Map.Entry<String, String> sensor : sensorTypeMap.entrySet()) {
                        String sensorId = sensor.getKey();
                        String sensorType = sensor.getValue();
                        Map<String, Object> typeInfo = sensorTypes.get(sensorType);
                        String location = locations.get(random.nextInt(locations.size()));
                        
                        for (int i = 0; i < 3; i++) {
                            // 设置时间
                            calendar.set(Calendar.HOUR_OF_DAY, 8 + i * 6); // 8点、14点、20点
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
                            
                            // 构建插入SQL
                            if (batchSize == 0) {
                                insertSQL = new StringBuilder();
                                insertSQL.append("INSERT INTO ").append(sensorDatabase).append(".").append(sensorTable)
                                        .append(" VALUES ");
                            } else {
                                insertSQL.append(", ");
                            }
                            
                            insertSQL.append("('").append(id).append("', '")
                                    .append(sensorId).append("', '")
                                    .append(sensorType).append("', ")
                                    .append(value).append(", '")
                                    .append(unit).append("', ")
                                    .append(timestamp.getTime()).append(", '")
                                    .append(readableTime).append("', '")
                                    .append(location).append("', ")
                                    .append(batteryLevel).append(", '")
                                    .append(monthStr).append("', ")
                                    .append(year).append(", '")
                                    .append(dayStr).append("')");
                            
                            batchSize++;
                            
                            // 每100条执行一次插入
                            if (batchSize >= 100) {
                                hiveService.executeUpdate(insertSQL.toString());
                                totalInserted += batchSize;
                                batchSize = 0;
                                logger.info("已导入 {} 条示例传感器数据", totalInserted);
                            }
                        }
                    }
                }
                
                // 下个月
                calendar.add(Calendar.MONTH, 1);
            }
            
            // 插入剩余的数据
            if (batchSize > 0) {
                hiveService.executeUpdate(insertSQL.toString());
                totalInserted += batchSize;
            }
            
            logger.info("示例传感器数据导入完成，共导入 {} 条数据", totalInserted);
        } catch (Exception e) {
            logger.error("导入示例传感器数据时出错", e);
        }
    }

    /**
     * 定时导入传感器数据到Hive（模拟）
     * 每天执行一次
     */
    @Scheduled(cron = "0 0 0 * * ?") // 每天零点执行
    public void scheduledDataImport() {
        if (!importEnabled) {
            return;
        }
        
        try {
            logger.info("执行计划任务：导入传感器数据");
            importTodaySensorData();
        } catch (Exception e) {
            logger.error("计划任务执行失败", e);
        }
    }
    
    /**
     * 导入今日传感器数据（模拟）
     */
    private void importTodaySensorData() {
        try {
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
            
            StringBuilder insertSQL = new StringBuilder();
            insertSQL.append("INSERT INTO ").append(sensorDatabase).append(".").append(sensorTable)
                    .append(" VALUES ");
            
            int recordCount = 0;
            boolean firstRecord = true;
            
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
                
                // 添加到SQL
                if (!firstRecord) {
                    insertSQL.append(", ");
                }
                
                insertSQL.append("('").append(id).append("', '")
                        .append(sensorId).append("', '")
                        .append(sensorType).append("', ")
                        .append(value).append(", '")
                        .append(unit).append("', ")
                        .append(timestamp.getTime()).append(", '")
                        .append(readableTime).append("', '")
                        .append(location).append("', ")
                        .append(batteryLevel).append(", '")
                        .append(monthStr).append("', ")
                        .append(year).append(", '")
                        .append(dayStr).append("')");
                
                firstRecord = false;
                recordCount++;
            }
            
            // 执行插入
            if (recordCount > 0) {
                hiveService.executeUpdate(insertSQL.toString());
                logger.info("成功导入今日传感器数据，共 {} 条记录", recordCount);
            }
        } catch (Exception e) {
            logger.error("导入今日传感器数据时出错", e);
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
            importTodaySensorData();
            result.put("success", true);
            result.put("message", "已触发传感器数据导入");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
            logger.error("手动触发导入传感器数据时出错", e);
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
            // 获取数据表行数
            String countSql = "SELECT COUNT(*) AS count FROM " + sensorDatabase + "." + sensorTable;
            hiveService.executeQuery(countSql).stream().findFirst().ifPresent(row -> {
                info.put("rowCount", row.get("count"));
            });
            
            // 获取最新数据时间
            String latestTimeSql = "SELECT MAX(readableTime) AS latestTime FROM " + sensorDatabase + "." + sensorTable;
            hiveService.executeQuery(latestTimeSql).stream().findFirst().ifPresent(row -> {
                info.put("latestTime", row.get("latestTime"));
            });
            
            info.put("success", true);
        } catch (Exception e) {
            info.put("success", false);
            info.put("error", e.getMessage());
            logger.error("获取传感器数据表信息时出错", e);
        }
        
        return info;
    }
} 