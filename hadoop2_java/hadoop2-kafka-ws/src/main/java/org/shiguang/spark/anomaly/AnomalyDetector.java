package org.shiguang.spark.anomaly;

/**
 * 异常检测器接口
 * <p>
 * 定义了传感器数据异常检测的标准接口，用于检测传感器数据是否超出正常阈值范围。
 * </p>
 */
public interface AnomalyDetector {
    
    /**
     * 检测传感器数据是否异常
     * 
     * @param sensorType 传感器类型，如温度、湿度等
     * @param value 传感器读数值
     * @return 如果读数值超出正常阈值范围则返回true，否则返回false
     */
    boolean detectAnomaly(String sensorType, double value);
    
    /**
     * 获取指定传感器类型的异常阈值
     * 
     * @param sensorType 传感器类型
     * @return 该类型传感器的异常阈值
     */
    double getThreshold(String sensorType);
    
    /**
     * 设置指定传感器类型的异常阈值
     * 
     * @param sensorType 传感器类型
     * @param threshold 异常阈值
     */
    void setThreshold(String sensorType, double threshold);
} 