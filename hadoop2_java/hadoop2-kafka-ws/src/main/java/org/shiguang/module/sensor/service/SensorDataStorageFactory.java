package org.shiguang.module.sensor.service;

import org.shiguang.module.mysql.service.MySQLSensorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 传感器数据存储工厂
 * 负责将传感器数据存储到MySQL数据库中
 */
@Service
public class SensorDataStorageFactory {
    private static final Logger logger = LoggerFactory.getLogger(SensorDataStorageFactory.class);

    @Autowired
    private MySQLSensorService mysqlService;

    @Value("${sensor.data.storage.mysql-enabled:true}")
    private boolean mysqlEnabled;
    
    // 统计信息
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);

    @PostConstruct
    public void init() {
        try {
            logger.info("初始化传感器数据存储工厂...");
            logger.info("MySQL存储已{}启用", mysqlEnabled ? "" : "未");
            
            // 检查存储服务可用性
            if (mysqlEnabled && mysqlService == null) {
                logger.warn("MySQL存储已启用，但MySQL服务不可用");
            }
        } catch (Exception e) {
            logger.error("初始化传感器数据存储工厂出错: {}", e.getMessage(), e);
        }
    }

    /**
     * 存储传感器数据到MySQL
     * 
     * @param sensorDataJson 传感器数据JSON
     * @return 是否成功存储
     */
    public boolean storeSensorData(String sensorDataJson) {
        totalRequests.incrementAndGet();
        boolean success = false;
        
        try {
            if (mysqlEnabled && mysqlService != null) {
                logger.debug("开始将传感器数据存储到MySQL...");
                success = mysqlService.importSensorData(sensorDataJson);
                
                if (success) {
                    logger.debug("传感器数据成功存储到MySQL");
                    successCount.incrementAndGet();
                } else {
                    logger.error("传感器数据存储到MySQL失败");
                    failureCount.incrementAndGet();
                }
            } else {
                logger.warn("MySQL存储已禁用或服务不可用，无法存储传感器数据");
                failureCount.incrementAndGet();
            }
        } catch (Exception e) {
            logger.error("存储传感器数据到MySQL时出错: {}", e.getMessage(), e);
            failureCount.incrementAndGet();
        }
        
        return success;
    }
    
    /**
     * 获取存储统计信息
     */
    public String getStorageStatistics() {
        long total = totalRequests.get();
        long success = successCount.get();
        long failure = failureCount.get();
        double successRate = total > 0 ? (double) success / total * 100 : 0;
        
        return String.format("总请求数: %d, 成功: %d, 失败: %d, 成功率: %.2f%%", 
                total, success, failure, successRate);
    }
} 