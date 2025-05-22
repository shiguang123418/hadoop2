package org.shiguang.service;

import org.shiguang.config.AppProperties;
import org.shiguang.model.SensorData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnomalyDetectionService {
    
    @Autowired
    private AppProperties appProperties;
    
    /**
     * 检测传感器数据是否有异常
     * @param sensorData 传感器数据对象
     * @return 检测到的异常列表，如无异常则返回空列表
     */
    public List<SensorData.Anomaly> detectAnomalies(SensorData sensorData) {
        List<SensorData.Anomaly> anomalies = new ArrayList<>();
        
        // 获取阈值配置
        double tempMin = appProperties.getThreshold().getTemperature().getMin();
        double tempMax = appProperties.getThreshold().getTemperature().getMax();
        double humidityMin = appProperties.getThreshold().getHumidity().getMin();
        double humidityMax = appProperties.getThreshold().getHumidity().getMax();
        double soilMoistureMin = appProperties.getThreshold().getSoilMoisture().getMin();
        double soilMoistureMax = appProperties.getThreshold().getSoilMoisture().getMax();
        double lightIntensityMin = appProperties.getThreshold().getLightIntensity().getMin();
        double lightIntensityMax = appProperties.getThreshold().getLightIntensity().getMax();
        double co2Min = appProperties.getThreshold().getCo2().getMin();
        double co2Max = appProperties.getThreshold().getCo2().getMax();
        double batteryMin = appProperties.getThreshold().getBattery().getMin();
        
        // 检查温度异常
        if (sensorData.getTemperature() < tempMin || sensorData.getTemperature() > tempMax) {
            anomalies.add(new SensorData.Anomaly("temperature", sensorData.getTemperature(), tempMin, tempMax));
        }
        
        // 检查湿度异常
        if (sensorData.getHumidity() < humidityMin || sensorData.getHumidity() > humidityMax) {
            anomalies.add(new SensorData.Anomaly("humidity", sensorData.getHumidity(), humidityMin, humidityMax));
        }
        
        // 检查土壤湿度异常
        if (sensorData.getSoilMoisture() < soilMoistureMin || sensorData.getSoilMoisture() > soilMoistureMax) {
            anomalies.add(new SensorData.Anomaly("soilMoisture", sensorData.getSoilMoisture(), soilMoistureMin, soilMoistureMax));
        }
        
        // 检查光照强度异常
        if (sensorData.getLightIntensity() < lightIntensityMin || sensorData.getLightIntensity() > lightIntensityMax) {
            anomalies.add(new SensorData.Anomaly("lightIntensity", sensorData.getLightIntensity(), lightIntensityMin, lightIntensityMax));
        }
        
        // 检查CO2浓度异常
        if (sensorData.getCo2Level() > 0 && (sensorData.getCo2Level() < co2Min || sensorData.getCo2Level() > co2Max)) {
            anomalies.add(new SensorData.Anomaly("co2Level", sensorData.getCo2Level(), co2Min, co2Max));
        }
        
        // 检查电池电量异常
        if (sensorData.getBatteryLevel() < batteryMin) {
            anomalies.add(new SensorData.Anomaly("batteryLevel", sensorData.getBatteryLevel(), batteryMin, Double.MAX_VALUE));
        }
        
        return anomalies;
    }
} 