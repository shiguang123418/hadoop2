package org.shiguang.repository;

import org.shiguang.model.SensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorDataRepository extends JpaRepository<SensorData, Long> {
    
    /**
     * 查找指定传感器ID和时间范围内的数据
     */
    List<SensorData> findBySensorIdAndTimestampBetweenOrderByTimestampDesc(
        String sensorId, long startTime, long endTime);
} 