package org.shiguang.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 农业传感器数据模型 - 仅包含数据定义，不包含业务逻辑
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sensor_data")
public class SensorData implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sensor_id", nullable = false)
    private String sensorId;
    
    @Column(nullable = false)
    private long timestamp;
    
    @Column(nullable = false)
    private double temperature;
    
    @Column(nullable = false)
    private double humidity;
    
    @Column(name = "soil_moisture", nullable = false)
    private double soilMoisture;
    
    @Column(name = "light_intensity", nullable = false)
    private double lightIntensity;
    
    @Column(nullable = true)
    private String location;
    
    @Column(name = "battery_level", nullable = true)
    private double batteryLevel;
    
    @Column(name = "co2_level", nullable = true)
    private double co2Level;
    
    @Transient
    private String region;
    
    @Transient
    private String cropType;
    
    /**
     * 从位置提取区域信息（不作为数据库字段）
     */
    public String getRegion() {
        if (location != null && location.contains("-")) {
            return location.split("-")[0];
        }
        return "未知区域";
    }
    
    /**
     * 根据位置信息确定作物类型（不作为数据库字段）
     */
    public String getCropType() {
        if (location == null) return "农作物";
        
        String lowerLoc = location.toLowerCase();
        if (lowerLoc.contains("稻田")) return "水稻";
        if (lowerLoc.contains("果园")) return "柑橘";
        if (lowerLoc.contains("菜地")) return "蔬菜";
        if (lowerLoc.contains("麦田")) return "小麦";
        if (lowerLoc.contains("葡萄")) return "葡萄";
        if (lowerLoc.contains("茶园")) return "茶叶";
        if (lowerLoc.contains("玉米")) return "玉米";
        if (lowerLoc.contains("棉田")) return "棉花";
        return "农作物";
    }
    
    /**
     * 数据异常模型（作为传输对象使用，不存储在数据库）
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Anomaly {
        private String parameter;
        private double value;
        private double min;
        private double max;
    }
} 