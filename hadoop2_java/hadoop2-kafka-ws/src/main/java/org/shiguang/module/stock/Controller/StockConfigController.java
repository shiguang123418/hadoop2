package org.shiguang.module.stock.Controller;

import org.shiguang.module.stock.config.StockConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 股票配置控制器
 * 提供股票数据爬取和处理相关的配置管理API
 */
@RestController
@RequestMapping("/stock/config")
public class StockConfigController {

    private static final Logger logger = LoggerFactory.getLogger(StockConfigController.class);
    
    @Autowired
    private StockConfig stockConfig;
    
    /**
     * 获取当前股票配置
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        Map<String, Object> config = new HashMap<>();
        
        // 爬取器配置
        Map<String, Object> crawlerConfig = new HashMap<>();
        crawlerConfig.put("enabled", stockConfig.isCrawlerEnabled());
        crawlerConfig.put("interval", stockConfig.getCrawlerInterval());
        crawlerConfig.put("defaultCode", stockConfig.getDefaultStockCode());
        config.put("crawler", crawlerConfig);
        
        // 处理器配置
        Map<String, Object> processorConfig = new HashMap<>();
        processorConfig.put("enabled", stockConfig.isProcessorEnabled());
        processorConfig.put("batchSize", stockConfig.getProcessorBatchSize());
        config.put("processor", processorConfig);
        
        // 分析器配置
        Map<String, Object> analysisConfig = new HashMap<>();
        analysisConfig.put("enabled", stockConfig.isAnalysisEnabled());
        analysisConfig.put("volatilityThreshold", stockConfig.getVolatilityThreshold());
        config.put("analysis", analysisConfig);
        
        return ResponseEntity.ok(config);
    }
    
    /**
     * 修改爬取器启用状态
     */
    @PutMapping("/crawler/enabled")
    public ResponseEntity<Map<String, Object>> setCrawlerEnabled(@RequestParam boolean enabled) {
        stockConfig.setCrawlerEnabled(enabled);
        return createSuccessResponse("爬取器状态更新为: " + (enabled ? "启用" : "禁用"));
    }
    
    /**
     * 修改爬取间隔
     */
    @PutMapping("/crawler/interval")
    public ResponseEntity<Map<String, Object>> setCrawlerInterval(@RequestParam int interval) {
        stockConfig.setCrawlerInterval(interval);
        return createSuccessResponse("爬取间隔更新为: " + stockConfig.getCrawlerInterval() + " 毫秒");
    }
    
    /**
     * 修改默认股票代码
     */
    @PutMapping("/crawler/default-code")
    public ResponseEntity<Map<String, Object>> setDefaultStockCode(@RequestParam String code) {
        stockConfig.setDefaultStockCode(code);
        return createSuccessResponse("默认股票代码更新为: " + code);
    }
    
    /**
     * 修改处理器启用状态
     */
    @PutMapping("/processor/enabled")
    public ResponseEntity<Map<String, Object>> setProcessorEnabled(@RequestParam boolean enabled) {
        stockConfig.setProcessorEnabled(enabled);
        return createSuccessResponse("处理器状态更新为: " + (enabled ? "启用" : "禁用"));
    }
    
    /**
     * 修改处理批次大小
     */
    @PutMapping("/processor/batch-size")
    public ResponseEntity<Map<String, Object>> setProcessorBatchSize(@RequestParam int size) {
        stockConfig.setProcessorBatchSize(size);
        return createSuccessResponse("处理批次大小更新为: " + size);
    }
    
    /**
     * 修改分析器启用状态
     */
    @PutMapping("/analysis/enabled")
    public ResponseEntity<Map<String, Object>> setAnalysisEnabled(@RequestParam boolean enabled) {
        stockConfig.setAnalysisEnabled(enabled);
        return createSuccessResponse("分析器状态更新为: " + (enabled ? "启用" : "禁用"));
    }
    
    /**
     * 修改波动率阈值
     */
    @PutMapping("/analysis/threshold")
    public ResponseEntity<Map<String, Object>> setVolatilityThreshold(@RequestParam double threshold) {
        stockConfig.setVolatilityThreshold(threshold);
        return createSuccessResponse("波动率阈值更新为: " + threshold + "%");
    }
    
    /**
     * 创建成功响应
     */
    private ResponseEntity<Map<String, Object>> createSuccessResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("config", getConfig().getBody());
        return ResponseEntity.ok(response);
    }
} 