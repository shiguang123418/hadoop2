package org.shiguang.module.sensor.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 传感器配置管理
 * 用于集中管理传感器数据处理的各项配置
 */
@Component
public class SensorConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(SensorConfig.class);
    
    @Value("${sensor.enabled:true}")
    private boolean enabled;
    
    @Value("${sensor.data.batch-size:100}")
    private int dataProcessingBatchSize;
    
    @Value("${sensor.simulation.enabled:false}")
    private boolean simulationEnabled;
    
    @Value("${sensor.kafka.topic:agriculture-sensor-data}")
    private String kafkaTopic;
    
    @Value("${sensor.data.interval:5000}")
    private int dataCollectionInterval;

    /**
     * 检查传感器模块是否全局启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置传感器模块是否全局启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("传感器模块已{}", enabled ? "启用" : "禁用");
    }

    public int getDataProcessingBatchSize() {
        return dataProcessingBatchSize;
    }

    public void setDataProcessingBatchSize(int dataProcessingBatchSize) {
        this.dataProcessingBatchSize = dataProcessingBatchSize;
        logger.info("传感器数据处理批处理大小已设置为{}", dataProcessingBatchSize);
    }

    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }

    public void setSimulationEnabled(boolean simulationEnabled) {
        this.simulationEnabled = simulationEnabled;
        logger.info("传感器数据模拟已{}", simulationEnabled ? "启用" : "禁用");
    }

    public String getKafkaTopic() {
        return kafkaTopic;
    }

    public void setKafkaTopic(String kafkaTopic) {
        this.kafkaTopic = kafkaTopic;
        logger.info("传感器Kafka主题已设置为{}", kafkaTopic);
    }

    public int getDataCollectionInterval() {
        return dataCollectionInterval;
    }

    public void setDataCollectionInterval(int dataCollectionInterval) {
        if (dataCollectionInterval < 1000) {
            logger.warn("数据采集间隔设置过小 ({}ms)，已调整为最小值1000ms", dataCollectionInterval);
            this.dataCollectionInterval = 1000;
        } else {
            this.dataCollectionInterval = dataCollectionInterval;
            logger.info("传感器数据采集间隔已设置为{}毫秒", dataCollectionInterval);
        }
    }
} 