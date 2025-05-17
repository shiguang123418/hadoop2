package org.shiguang.service.impl;

import org.shiguang.service.SparkService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SparkService的模拟实现
 * 当spark.use-mock=true时使用此实现
 */
@Service
@Primary
@ConditionalOnProperty(name = "spark.use-mock", havingValue = "true")
public class MockSparkServiceImpl implements SparkService {

    @Override
    public Map<String, Object> calculateCropStatistics(String cropName) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("平均产量", 85.6);
        stats.put("产量标准差", 12.8);
        stats.put("最低产量", 62.1);
        stats.put("最高产量", 98.7);
        stats.put("数据量", 24L);
        
        List<Map<String, Object>> trend = new ArrayList<>();
        int baseYear = 2020;
        
        for (int i = 0; i < 5; i++) {
            trend.add(Map.of(
                "年份", baseYear + i,
                "平均产量", 75.0 + i * 3.5
            ));
        }
        
        stats.put("年度趋势", trend);
        stats.put("模拟数据", true);
        return stats;
    }

    @Override
    public List<Map<String, Object>> analyzeCorrelation(String cropName) {
        List<Map<String, Object>> correlations = new ArrayList<>();
        
        Map<String, Object> temp = new HashMap<>();
        temp.put("factor", "温度");
        temp.put("correlation", 0.82);
        correlations.add(temp);
        
        Map<String, Object> rain = new HashMap<>();
        rain.put("factor", "降雨量");
        rain.put("correlation", 0.45);
        correlations.add(rain);
        
        Map<String, Object> ph = new HashMap<>();
        ph.put("factor", "土壤酸碱度");
        ph.put("correlation", -0.34);
        correlations.add(ph);
        
        return correlations;
    }

    @Override
    public List<Map<String, Object>> forecastProduction(String cropName, int periods) {
        List<Map<String, Object>> forecast = new ArrayList<>();
        int baseYear = 2025;
        double baseProduction = 90.5;
        
        for (int i = 0; i < periods; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("year", baseYear + i);
            item.put("forecasted_production", baseProduction + i * 2.3);
            forecast.add(item);
        }
        
        return forecast;
    }

    @Override
    public List<Map<String, Object>> aggregateByGroup(String groupBy) {
        List<Map<String, Object>> result = new ArrayList<>();
        
        if ("crop".equalsIgnoreCase(groupBy)) {
            String[] crops = {"小麦", "玉米", "水稻", "大豆"};
            for (String crop : crops) {
                Map<String, Object> item = new HashMap<>();
                item.put("分组", crop);
                item.put("平均产量", 80 + new Random().nextDouble() * 20);
                result.add(item);
            }
        } else if ("region".equalsIgnoreCase(groupBy)) {
            String[] regions = {"华北", "华东", "华南", "西北", "西南"};
            for (String region : regions) {
                Map<String, Object> item = new HashMap<>();
                item.put("分组", region);
                item.put("平均产量", 75 + new Random().nextDouble() * 25);
                result.add(item);
            }
        } else {
            Map<String, Object> item = new HashMap<>();
            item.put("error", "不支持的分组类型");
            result.add(item);
        }
        
        return result;
    }

    @Override
    public Map<String, Object> findOptimalGrowingConditions(String cropName) {
        Map<String, Object> conditions = new HashMap<>();
        conditions.put("作物", cropName);
        conditions.put("温度范围", Map.of("最小值", 22.5, "最大值", 28.2));
        conditions.put("降雨量范围", Map.of("最小值", 800.0, "最大值", 1200.0));
        conditions.put("PH值范围", Map.of("最小值", 6.2, "最大值", 7.8));
        conditions.put("预期平均产量", 95.3);
        conditions.put("模拟数据", true);
        return conditions;
    }

    @Override
    public Map<String, Object> clusterCrops(int numClusters) {
        Map<String, Object> results = new HashMap<>();
        results.put("聚类数", numClusters);
        
        List<Map<String, Object>> clusters = new ArrayList<>();
        String[] cropTypes = {"小麦", "玉米", "水稻", "大豆", "马铃薯"};
        
        for (int i = 0; i < numClusters; i++) {
            Map<String, Object> cluster = new HashMap<>();
            cluster.put("聚类ID", i);
            cluster.put("记录数", 10 + i * 5);
            
            List<String> crops = new ArrayList<>();
            for (int j = 0; j < (i+1) * 2 && j < cropTypes.length; j++) {
                crops.add(cropTypes[j]);
            }
            cluster.put("作物", crops);
            
            cluster.put("平均温度", 20 + i * 2.5);
            cluster.put("平均降雨量", 750 + i * 150.0);
            cluster.put("平均PH值", 5.5 + i * 0.5);
            cluster.put("平均产量", 75.0 + i * 10.0);
            
            clusters.add(cluster);
        }
        
        results.put("聚类结果", clusters);
        results.put("评估指标", Map.of("轮廓系数近似值", 0.65));
        results.put("模拟数据", true);
        
        return results;
    }
} 