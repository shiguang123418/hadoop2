package org.shiguang.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 模拟数据服务
 * 当HDFS不可用或读取数据失败时，提供模拟数据
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MockDataService {

    private final Random random = new Random();
    
    @Value("${hadoop.mock.enabled:true}")
    private boolean mockEnabled;
    
    /**
     * 检查是否启用模拟数据
     */
    public boolean isMockEnabled() {
        return mockEnabled;
    }
    
    /**
     * 生成模拟作物列表
     */
    public List<String> getMockCrops() {
        if (!mockEnabled) {
            return List.of();
        }
        
        log.info("生成模拟作物列表");
        List<String> crops = new ArrayList<>();
        crops.add("水稻");
        crops.add("小麦");
        crops.add("玉米");
        crops.add("大豆");
        crops.add("棉花");
        crops.add("马铃薯");
        return crops;
    }
    
    /**
     * 生成模拟产量数据
     */
    public Map<String, Object> getMockProductionData(String crop) {
        if (!mockEnabled) {
            return Map.of();
        }
        
        log.info("生成作物'{}'的模拟产量数据", crop);
        int dataPoints = 20;
        List<Double> rainfall = new ArrayList<>();
        List<Double> temperature = new ArrayList<>();
        List<Double> ph = new ArrayList<>();
        List<Double> production = new ArrayList<>();
        
        for (int i = 0; i < dataPoints; i++) {
            // 根据不同作物生成合理范围的模拟数据
            double baseTemp = getBaseTempForCrop(crop);
            double baseRain = getBaseRainForCrop(crop);
            double basePh = getBasePhForCrop(crop);
            double baseProd = getBaseProdForCrop(crop);
            
            rainfall.add(baseRain + random.nextDouble() * 200 - 100);
            temperature.add(baseTemp + random.nextDouble() * 6 - 3);
            ph.add(basePh + random.nextDouble() * 1.4 - 0.7);
            production.add(baseProd + random.nextDouble() * 15 - 5);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("Rainfall", rainfall);
        result.put("Temperature", temperature);
        result.put("Ph", ph);
        result.put("Production", production);
        result.put("isMockData", true);
        
        return result;
    }
    
    /**
     * 生成模拟平均产量数据
     */
    public List<Map<String, Object>> getMockAverageProduction() {
        if (!mockEnabled) {
            return List.of();
        }
        
        log.info("生成模拟平均产量数据");
        List<Map<String, Object>> result = new ArrayList<>();
        List<String> crops = getMockCrops();
        
        for (String crop : crops) {
            Map<String, Object> item = new HashMap<>();
            item.put("crop", crop);
            item.put("averageProduction", getBaseProdForCrop(crop));
            result.add(item);
        }
        
        result.sort((a, b) -> Double.compare((double) b.get("averageProduction"), (double) a.get("averageProduction")));
        return result;
    }
    
    /**
     * 生成模拟最佳生长条件
     */
    public Map<String, Object> getMockOptimalConditions(String crop) {
        if (!mockEnabled) {
            return Map.of();
        }
        
        log.info("生成作物'{}'的模拟最佳生长条件", crop);
        Map<String, Object> conditions = new HashMap<>();
        
        conditions.put("rainfall", getBaseRainForCrop(crop) + 50);
        conditions.put("temperature", getBaseTempForCrop(crop) + 1.5);
        conditions.put("ph", getBasePhForCrop(crop) + 0.3);
        conditions.put("production", getBaseProdForCrop(crop) + 10);
        conditions.put("isMockData", true);
        
        return conditions;
    }
    
    /**
     * 根据作物类型获取基础温度
     */
    private double getBaseTempForCrop(String crop) {
        switch (crop.toLowerCase()) {
            case "水稻": return 25.0;
            case "小麦": return 18.0;
            case "玉米": return 22.0;
            case "大豆": return 20.0;
            case "棉花": return 26.0;
            case "马铃薯": return 16.0;
            default: return 20.0;
        }
    }
    
    /**
     * 根据作物类型获取基础降水量
     */
    private double getBaseRainForCrop(String crop) {
        switch (crop.toLowerCase()) {
            case "水稻": return 1200.0;
            case "小麦": return 450.0;
            case "玉米": return 600.0;
            case "大豆": return 550.0;
            case "棉花": return 700.0;
            case "马铃薯": return 400.0;
            default: return 500.0;
        }
    }
    
    /**
     * 根据作物类型获取基础pH值
     */
    private double getBasePhForCrop(String crop) {
        switch (crop.toLowerCase()) {
            case "水稻": return 5.5;
            case "小麦": return 6.8;
            case "玉米": return 6.5;
            case "大豆": return 6.2;
            case "棉花": return 7.0;
            case "马铃薯": return 5.8;
            default: return 6.5;
        }
    }
    
    /**
     * 根据作物类型获取基础产量
     */
    private double getBaseProdForCrop(String crop) {
        switch (crop.toLowerCase()) {
            case "水稻": return 85.0;
            case "小麦": return 65.0;
            case "玉米": return 90.0;
            case "大豆": return 40.0;
            case "棉花": return 30.0;
            case "马铃薯": return 75.0;
            default: return 60.0;
        }
    }
} 