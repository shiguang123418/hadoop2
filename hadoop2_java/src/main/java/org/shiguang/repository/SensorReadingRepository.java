package org.shiguang.repository;

import org.shiguang.entity.SensorReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 传感器数据存储库
 */
@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReading, Long> {

    /**
     * 根据传感器ID查找读数
     */
    List<SensorReading> findBySensorId(String sensorId);
    
    /**
     * 根据传感器类型查找读数
     */
    List<SensorReading> findBySensorType(String sensorType);
    
    /**
     * 根据区域查找读数
     */
    List<SensorReading> findByRegion(String region);
    
    /**
     * 根据作物类型查找读数
     */
    List<SensorReading> findByCropType(String cropType);
    
    /**
     * 查找指定时间范围内的读数
     */
    List<SensorReading> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * 查找异常值
     */
    List<SensorReading> findByAnomaly(boolean isAnomaly);
    
    /**
     * 计算每个区域的平均值（按传感器类型分组）
     */
    @Query("SELECT sr.region as region, sr.sensorType as sensorType, AVG(sr.value) as avgValue " +
           "FROM SensorReading sr GROUP BY sr.region, sr.sensorType")
    List<Map<String, Object>> findAverageByRegionAndType();
    
    /**
     * 计算每种作物的平均值（按传感器类型分组）
     */
    @Query("SELECT sr.cropType as cropType, sr.sensorType as sensorType, AVG(sr.value) as avgValue " +
           "FROM SensorReading sr GROUP BY sr.cropType, sr.sensorType")
    List<Map<String, Object>> findAverageByCropAndType();
    
    /**
     * 查找最近的传感器读数
     */
    @Query("SELECT sr FROM SensorReading sr WHERE sr.timestamp = " +
           "(SELECT MAX(s.timestamp) FROM SensorReading s WHERE s.sensorId = sr.sensorId)")
    List<SensorReading> findLatestReadings();
} 