package org.shiguang.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 存储传感器读数的实体类
 */
@Entity
@Table(name = "sensor_readings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SensorReading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sensor_id", nullable = false)
    private String sensorId;
    
    @Column(name = "sensor_type", nullable = false)
    private String sensorType;
    
    @Column(name = "region", nullable = false)
    private String region;
    
    @Column(name = "crop_type", nullable = false)
    private String cropType;
    
    @Column(name = "value", nullable = false)
    private double value;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "unit")
    private String unit;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "is_anomaly")
    private boolean anomaly;
} 