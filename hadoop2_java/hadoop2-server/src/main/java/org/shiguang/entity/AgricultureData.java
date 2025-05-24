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
 * 农业数据实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "agriculture_data")
public class AgricultureData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "location", nullable = false)
    private String location;
    
    @Column(name = "crop_type")
    private String cropType;
    
    @Column(name = "soil_moisture")
    private Double soilMoisture;
    
    @Column(name = "temperature")
    private Double temperature;
    
    @Column(name = "humidity")
    private Double humidity;
    
    @Column(name = "light_intensity")
    private Double lightIntensity;
    
    @Column(name = "rainfall")
    private Double rainfall;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "notes", length = 1000)
    private String notes;
} 