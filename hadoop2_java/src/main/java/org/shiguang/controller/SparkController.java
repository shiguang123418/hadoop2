package org.shiguang.controller;

import org.shiguang.service.SparkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/spark")
@CrossOrigin(originPatterns = "*")
public class SparkController {

    private static final Logger logger = LoggerFactory.getLogger(SparkController.class);

    @Autowired
    private SparkService sparkService;

    /**
     * 获取作物统计信息
     * @param crop 作物名称
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getCropStatistics(@RequestParam String crop) {
        logger.info("获取作物'{}'的统计信息", crop);
        Map<String, Object> statistics = sparkService.calculateCropStatistics(crop);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 获取作物相关性分析
     * @param crop 作物名称
     * @return 相关性分析结果
     */
    @GetMapping("/correlation")
    public ResponseEntity<?> getCorrelationAnalysis(@RequestParam String crop) {
        logger.info("获取作物'{}'的相关性分析", crop);
        List<Map<String, Object>> correlation = sparkService.analyzeCorrelation(crop);
        return ResponseEntity.ok(correlation);
    }

    /**
     * 获取作物产量预测
     * @param crop 作物名称
     * @param periods 预测期数
     * @return 预测结果
     */
    @GetMapping("/forecast")
    public ResponseEntity<?> getForecastProduction(
            @RequestParam String crop,
            @RequestParam(defaultValue = "5") int periods) {
        logger.info("获取作物'{}'未来{}期的产量预测", crop, periods);
        List<Map<String, Object>> forecast = sparkService.forecastProduction(crop, periods);
        return ResponseEntity.ok(forecast);
    }

    /**
     * 获取分组聚合分析
     * @param groupBy 分组字段
     * @return 分组聚合结果
     */
    @GetMapping("/aggregate")
    public ResponseEntity<?> getAggregateAnalysis(
            @RequestParam(defaultValue = "Crop") String groupBy) {
        logger.info("按'{}'字段进行分组聚合分析", groupBy);
        List<Map<String, Object>> aggregation = sparkService.aggregateByGroup(groupBy);
        return ResponseEntity.ok(aggregation);
    }

    /**
     * 获取作物最佳种植条件
     * @param crop 作物名称
     * @return 最佳种植条件
     */
    @GetMapping("/optimal-conditions")
    public ResponseEntity<?> getOptimalConditions(@RequestParam String crop) {
        logger.info("获取作物'{}'的最佳种植条件", crop);
        Map<String, Object> conditions = sparkService.findOptimalGrowingConditions(crop);
        return ResponseEntity.ok(conditions);
    }

    /**
     * 获取作物聚类分析
     * @param clusters 聚类数量
     * @return 聚类分析结果
     */
    @GetMapping("/clusters")
    public ResponseEntity<?> getCropClusters(
            @RequestParam(defaultValue = "3") int clusters) {
        logger.info("执行作物聚类分析，聚类数：{}", clusters);
        Map<String, Object> clusterResult = sparkService.clusterCrops(clusters);
        return ResponseEntity.ok(clusterResult);
    }
} 