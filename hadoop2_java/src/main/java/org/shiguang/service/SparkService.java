package org.shiguang.service;

import java.util.List;
import java.util.Map;

/**
 * Spark数据分析服务接口
 */
public interface SparkService {
    
    /**
     * 统计分析 - 计算作物产量基本统计信息
     * @param cropName 作物名称
     * @return 统计结果Map
     */
    Map<String, Object> calculateCropStatistics(String cropName);
    
    /**
     * 相关性分析 - 分析环境因素与产量的相关性
     * @param cropName 作物名称
     * @return 相关性分析结果
     */
    List<Map<String, Object>> analyzeCorrelation(String cropName);
    
    /**
     * 时间序列分析 - 预测未来作物产量趋势
     * @param cropName 作物名称
     * @param periods 预测期数
     * @return 预测结果
     */
    List<Map<String, Object>> forecastProduction(String cropName, int periods);
    
    /**
     * 分组聚合分析 - 按照区域/时间分析产量
     * @param groupBy 分组字段
     * @return 分组聚合结果
     */
    List<Map<String, Object>> aggregateByGroup(String groupBy);
    
    /**
     * 最佳种植条件分析 - 根据历史数据找出最佳种植参数
     * @param cropName 作物名称
     * @return 最佳种植条件
     */
    Map<String, Object> findOptimalGrowingConditions(String cropName);
    
    /**
     * 聚类分析 - 将作物按照特征分组
     * @param numClusters 聚类数量
     * @return 聚类结果
     */
    Map<String, Object> clusterCrops(int numClusters);
} 