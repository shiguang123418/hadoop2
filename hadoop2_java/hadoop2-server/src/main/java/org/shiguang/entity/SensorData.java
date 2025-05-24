package org.shiguang.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Column;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 传感器数据实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sensor_data")
public class SensorData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "sensor_id", nullable = false)
    private String sensorId;
    
    @Column(name = "sensor_type")
    private String sensorType;
    
    @Column(name = "value", nullable = false)
    private Double value;
    
    @Column(name = "unit")
    private String unit;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "is_alert")
    private Boolean isAlert;
    
    @Column(name = "status")
    private String status;
} 