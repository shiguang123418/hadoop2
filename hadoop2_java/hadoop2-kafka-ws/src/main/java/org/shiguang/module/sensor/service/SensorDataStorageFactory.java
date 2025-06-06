package org.shiguang.module.sensor.service;

import org.shiguang.module.hive.service.SensorDataImportService;
import org.shiguang.module.mysql.service.MySQLSensorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * 传感器数据存储工厂
 * 根据配置决定数据存储的目标（MySQL、Hive或两者）
 */
@Service
public class SensorDataStorageFactory {
    private static final Logger logger = LoggerFactory.getLogger(SensorDataStorageFactory.class);

    @Autowired(required = false)
    private SensorDataImportService hiveService;

    @Autowired
    private MySQLSensorService mysqlService;

    @Value("${sensor.data.storage.target:mysql}")
    private String storageTarget;

    @Value("${sensor.data.storage.hive-enabled:false}")
    private boolean hiveEnabled;

    @Value("${sensor.data.storage.mysql-enabled:true}")
    private boolean mysqlEnabled;

    @PostConstruct
    public void init() {
        try {
            logger.info("初始化传感器数据存储工厂...");
            logger.info("存储目标: {}, Hive启用: {}, MySQL启用: {}", storageTarget, hiveEnabled, mysqlEnabled);
            
            // 检查存储服务可用性
            if (mysqlEnabled && mysqlService == null) {
                logger.warn("MySQL存储已启用，但MySQL服务不可用");
            }
            
            if (hiveEnabled && hiveService == null) {
                logger.warn("Hive存储已启用，但Hive服务不可用");
            }
        } catch (Exception e) {
            logger.error("初始化传感器数据存储工厂出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 存储传感器数据
     * 
     * @param sensorDataJson 传感器数据JSON
     * @return 是否成功存储到任意一个目标
     */
    public boolean storeSensorData(String sensorDataJson) {
        boolean success = false;
        
        // 根据配置决定存储目标
        if ("both".equalsIgnoreCase(storageTarget)) {
            // 存储到两个目标
            boolean mysqlSuccess = storeToMySQL(sensorDataJson);
            boolean hiveSuccess = storeToHive(sensorDataJson);
            success = mysqlSuccess || hiveSuccess;
        } else if ("hive".equalsIgnoreCase(storageTarget)) {
            // 仅存储到Hive
            success = storeToHive(sensorDataJson);
        } else {
            // 默认存储到MySQL
            success = storeToMySQL(sensorDataJson);
        }
        
        return success;
    }
    
    /**
     * 存储到MySQL
     */
    private boolean storeToMySQL(String sensorDataJson) {
        if (!mysqlEnabled || mysqlService == null) {
            logger.debug("MySQL存储已禁用或服务不可用");
            return false;
        }
        
        try {
            return mysqlService.importSensorData(sensorDataJson);
        } catch (Exception e) {
            logger.error("存储传感器数据到MySQL时出错: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 存储到Hive
     */
    private boolean storeToHive(String sensorDataJson) {
        if (!hiveEnabled || hiveService == null) {
            logger.debug("Hive存储已禁用或服务不可用");
            return false;
        }
        
        try {
            return hiveService.importSensorData(sensorDataJson);
        } catch (Exception e) {
            logger.error("存储传感器数据到Hive时出错: {}", e.getMessage(), e);
            return false;
        }
    }
} 