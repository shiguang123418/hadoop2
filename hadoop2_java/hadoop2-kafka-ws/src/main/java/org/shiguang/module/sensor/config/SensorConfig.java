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


    public boolean isEnabled() {
        return enabled;
    }


    public String getKafkaTopic() {
        return kafkaTopic;
    }

} 