package org.shiguang.spark.anomaly;

import org.shiguang.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 基于阈值的异常检测器实现
 * <p>
 * 通过比较传感器读数与预设阈值来检测是否存在异常。
 * 阈值配置从application.yml中加载。
 * </p>
 */
@Component
public class ThresholdAnomalyDetector implements AnomalyDetector, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ThresholdAnomalyDetector.class);
    private final Map<String, Double> anomalyThresholds = new HashMap<>();
    
    @Autowired
    private transient AppConfig config;
    
    /**
     * 初始化异常检测阈值
     */
    @PostConstruct
    public void init() {
        logger.info("正在初始化异常检测阈值...");
        loadAnomalyThresholds();
        logger.info("异常检测阈值初始化完成: {}", anomalyThresholds);
    }
    
    /**
     * 从配置中加载异常阈值
     */
    private void loadAnomalyThresholds() {
        anomalyThresholds.put("temperature", config.getDouble("anomaly.threshold.temperature", 5.0));
        anomalyThresholds.put("humidity", config.getDouble("anomaly.threshold.humidity", 10.0));
        anomalyThresholds.put("soil_moisture", config.getDouble("anomaly.threshold.soil_moisture", 15.0));
        anomalyThresholds.put("light_intensity", config.getDouble("anomaly.threshold.light_intensity", 200.0));
        anomalyThresholds.put("co2", config.getDouble("anomaly.threshold.co2", 100.0));
    }
    
    /**
     * 检测传感器数据是否异常
     * <p>
     * 目前使用简单的阈值比较：如果值超出预设阈值，则认为异常。
     * 未来可以扩展为更复杂的异常检测算法。
     * </p>
     * 
     * @param sensorType 传感器类型
     * @param value 传感器读数值
     * @return 如果数据异常则返回true，否则返回false
     */
    @Override
    public boolean detectAnomaly(String sensorType, double value) {
        Double threshold = anomalyThresholds.get(sensorType);
        if (threshold == null) {
            logger.warn("未找到传感器类型 '{}' 的异常阈值配置，使用默认值10.0", sensorType);
            threshold = 10.0; // 默认阈值
        }
        
        return Math.abs(value) > threshold;
    }
    
    /**
     * 获取指定传感器类型的异常阈值
     * 
     * @param sensorType 传感器类型
     * @return 异常阈值，如果没有找到则返回null
     */
    @Override
    public double getThreshold(String sensorType) {
        Double threshold = anomalyThresholds.get(sensorType);
        return threshold != null ? threshold : 10.0; // 默认阈值
    }
    
    /**
     * 设置指定传感器类型的异常阈值
     * 
     * @param sensorType 传感器类型
     * @param threshold 异常阈值
     */
    @Override
    public void setThreshold(String sensorType, double threshold) {
        if (threshold <= 0) {
            logger.warn("异常阈值必须大于0，忽略设置 '{}' 的值 {}", sensorType, threshold);
            return;
        }
        anomalyThresholds.put(sensorType, threshold);
        logger.info("已更新传感器类型 '{}' 的异常阈值为 {}", sensorType, threshold);
    }
} 