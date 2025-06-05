package org.shiguang.module.sensor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据处理服务
 * 用于处理传感器数据，执行数据分析和异常检测
 */
@Service
public class DataProcessingService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 存储传感器数据的历史值，用于计算移动平均和异常检测
    private final Map<String, List<Double>> sensorHistory = new ConcurrentHashMap<>();
    
    // 传感器类型及其正常范围
    private final Map<String, Map<String, Double>> sensorRanges = new HashMap<>();
    
    // 用于计算统计数据
    private int messageCount = 0;
    private int anomalyCount = 0;

    public DataProcessingService() {
        initSensorRanges();
    }

    /**
     * 初始化传感器正常范围配置
     */
    private void initSensorRanges() {
        // 温度传感器范围 (摄氏度)
        Map<String, Double> temperatureRange = new HashMap<>();
        temperatureRange.put("min", -10.0);
        temperatureRange.put("max", 50.0);
        sensorRanges.put("temperature", temperatureRange);
        
        // 湿度传感器范围 (%)
        Map<String, Double> humidityRange = new HashMap<>();
        humidityRange.put("min", 0.0);
        humidityRange.put("max", 100.0);
        sensorRanges.put("humidity", humidityRange);
        
        // 土壤湿度范围 (%)
        Map<String, Double> soilMoistureRange = new HashMap<>();
        soilMoistureRange.put("min", 0.0);
        soilMoistureRange.put("max", 100.0);
        sensorRanges.put("soilMoisture", soilMoistureRange);
        
        // 光照强度范围 (lux)
        Map<String, Double> lightRange = new HashMap<>();
        lightRange.put("min", 0.0);
        lightRange.put("max", 100000.0);
        sensorRanges.put("light", lightRange);
        
        // 二氧化碳浓度范围 (ppm)
        Map<String, Double> co2Range = new HashMap<>();
        co2Range.put("min", 300.0);
        co2Range.put("max", 5000.0);
        sensorRanges.put("co2", co2Range);
        
        // 电池电量范围 (%)
        Map<String, Double> batteryRange = new HashMap<>();
        batteryRange.put("min", 0.0);
        batteryRange.put("max", 100.0);
        sensorRanges.put("batteryLevel", batteryRange);
    }

    /**
     * 处理原始传感器数据
     * 
     * @param rawData 从Kafka接收的原始JSON数据
     * @return 处理后的JSON数据
     */
    public String processRawData(String rawData) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(rawData);
        String sensorId = jsonNode.path("sensorId").asText();
        String sensorType = jsonNode.path("sensorType").asText();
        double value = jsonNode.path("value").asDouble();
        
        // 更新消息计数
        messageCount++;
        
        // 检查数据是否异常
        boolean isAnomaly = detectAnomaly(sensorType, value);
        if (isAnomaly) {
            anomalyCount++;
        }
        
        // 更新传感器历史数据
        updateSensorHistory(sensorId, value);
        
        // 计算统计数据
        double anomalyRate = (messageCount > 0) ? (double) anomalyCount / messageCount : 0;
        
        // 创建增强后的JSON对象
        ObjectNode enhancedData = objectMapper.createObjectNode();
        // 复制原始数据的所有字段
        jsonNode.fields().forEachRemaining(entry -> enhancedData.set(entry.getKey(), entry.getValue()));
        
        // 添加处理结果
        enhancedData.put("isAnomaly", isAnomaly);
        enhancedData.put("processedTime", System.currentTimeMillis());
        
        // 添加移动平均值
        List<Double> history = sensorHistory.get(sensorId);
        if (history != null && !history.isEmpty()) {
            enhancedData.put("movingAverage", calculateMovingAverage(history));
        }
        
        // 创建统计信息对象
        ObjectNode statsNode = objectMapper.createObjectNode();
        statsNode.put("messageCount", messageCount);
        statsNode.put("anomalyCount", anomalyCount);
        statsNode.put("anomalyRate", anomalyRate);
        
        // 如果历史数据足够，添加趋势信息
        if (history != null && history.size() >= 3) {
            String trend = calculateTrend(history);
            statsNode.put("trend", trend);
        }
        
        // 将统计数据添加到结果
        enhancedData.set("stats", statsNode);
        
        return objectMapper.writeValueAsString(enhancedData);
    }
    
    /**
     * 检测异常值
     */
    private boolean detectAnomaly(String sensorType, double value) {
        Map<String, Double> range = sensorRanges.get(sensorType);
        if (range != null) {
            double min = range.get("min");
            double max = range.get("max");
            return value < min || value > max;
        }
        return false;
    }
    
    /**
     * 更新传感器历史数据
     */
    private void updateSensorHistory(String sensorId, double value) {
        List<Double> history = sensorHistory.computeIfAbsent(sensorId, k -> new ArrayList<>());
        
        // 保持历史数据不超过20个点
        if (history.size() >= 20) {
            history.remove(0);
        }
        
        history.add(value);
    }
    
    /**
     * 计算移动平均值
     */
    private double calculateMovingAverage(List<Double> history) {
        if (history.isEmpty()) {
            return 0;
        }
        
        double sum = 0;
        for (double value : history) {
            sum += value;
        }
        
        return sum / history.size();
    }
    
    /**
     * 计算趋势
     */
    private String calculateTrend(List<Double> history) {
        if (history.size() < 3) {
            return "stable";
        }
        
        double lastValue = history.get(history.size() - 1);
        double secondLastValue = history.get(history.size() - 2);
        double thirdLastValue = history.get(history.size() - 3);
        
        if (lastValue > secondLastValue && secondLastValue > thirdLastValue) {
            return "rising";
        } else if (lastValue < secondLastValue && secondLastValue < thirdLastValue) {
            return "falling";
        } else {
            return "stable";
        }
    }
    
    /**
     * 获取聚合统计数据
     */
    public String getAggregatedStats() throws JsonProcessingException {
        ObjectNode statsNode = objectMapper.createObjectNode();
        
        statsNode.put("messageCount", messageCount);
        statsNode.put("anomalyCount", anomalyCount);
        statsNode.put("anomalyRate", (messageCount > 0) ? (double) anomalyCount / messageCount : 0);
        statsNode.put("sensorCount", sensorHistory.size());
        
        // 按传感器类型分组的异常计数
        ObjectNode anomaliesByType = objectMapper.createObjectNode();
        // 这里可以实现按类型分组的统计逻辑
        
        statsNode.set("anomaliesByType", anomaliesByType);
        
        return objectMapper.writeValueAsString(statsNode);
    }
} 