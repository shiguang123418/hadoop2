package org.shiguang.service;

import org.shiguang.model.SensorData;
import org.shiguang.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SensorDataService {

    @Autowired
    private SensorDataRepository sensorDataRepository;
    
    /**
     * 保存传感器数据
     * @param sensorData 传感器数据
     * @return 保存后的数据（包含ID）
     */
    @Transactional
    public SensorData save(SensorData sensorData) {
        return sensorDataRepository.save(sensorData);
    }
    
    /**
     * 查询指定时间范围内的传感器数据
     * @param sensorId 传感器ID
     * @param startTime 开始时间戳
     * @param endTime 结束时间戳
     * @return 传感器数据列表
     */
    public List<SensorData> findByTimeRange(String sensorId, long startTime, long endTime) {
        return sensorDataRepository.findBySensorIdAndTimestampBetweenOrderByTimestampDesc(
            sensorId, startTime, endTime);
    }
} 