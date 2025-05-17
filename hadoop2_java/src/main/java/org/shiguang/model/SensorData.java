package org.shiguang.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 农业传感器数据模型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // 传感器ID
    private String sensorId;
    
    // 传感器类型（温度、湿度、光照等）
    private String sensorType;
    
    // 传感器所在区域
    private String region;
    
    // 作物类型
    private String cropType;
    
    // 传感器值
    private double value;
    
    // 读取时间
    private LocalDateTime timestamp;
    
    // 传感器单位
    private String unit;
    
    // 可选描述
    private String description;
} 