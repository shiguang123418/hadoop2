package org.shiguang.ws.controller;

import org.shiguang.ws.model.StockData;
import org.shiguang.ws.service.StockCrawlerService;
import org.shiguang.ws.service.StockDataProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 股票数据控制器
 * 提供股票数据的REST API接口
 */
@RestController
@RequestMapping("/stock")
public class StockDataController {

    private static final Logger logger = LoggerFactory.getLogger(StockDataController.class);

    @Autowired
    private StockCrawlerService stockCrawlerService;

    @Autowired
    private StockDataProcessingService stockDataProcessingService;

    /**
     * 获取指定股票的数据
     *
     * @param code   股票代码
     * @param market 市场代码（0表示深圳，1表示上海）
     * @return 股票数据列表
     */
    @GetMapping("/data")
    public ResponseEntity<List<StockData>> getStockData(
            @RequestParam("code") String code,
            @RequestParam(value = "market", defaultValue = "0") int market) {
        logger.info("获取股票数据: code={}, market={}", code, market);
        List<StockData> stockDataList = stockCrawlerService.crawlStockData(code, market);
        return ResponseEntity.ok(stockDataList);
    }

    /**
     * 获取服务状态信息
     *
     * @return 服务状态信息
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, String>> getServiceStatus() {
        Map<String, String> status = new HashMap<>();
        status.put("crawler", stockCrawlerService.getServiceStats());
        status.put("processor", stockDataProcessingService.getServiceStats());
        return ResponseEntity.ok(status);
    }

    /**
     * 手动触发爬取股票数据
     *
     * @param code   股票代码
     * @param market 市场代码（0表示深圳，1表示上海）
     * @return 操作结果
     */
    @PostMapping("/crawl")
    public ResponseEntity<Map<String, Object>> crawlStockData(
            @RequestParam("code") String code,
            @RequestParam(value = "market", defaultValue = "0") int market) {
        logger.info("手动触发爬取股票数据: code={}, market={}", code, market);
        
        Map<String, Object> result = new HashMap<>();
        try {
            List<StockData> stockDataList = stockCrawlerService.crawlStockData(code, market);
            result.put("success", true);
            result.put("message", String.format("成功爬取 %d 条股票数据", stockDataList.size()));
            result.put("count", stockDataList.size());
        } catch (Exception e) {
            logger.error("手动触发爬取股票数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "爬取股票数据失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
} 