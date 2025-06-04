package org.shiguang.module.stock.Controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.shiguang.model.StockData;
import org.shiguang.module.stock.service.StockCrawlerService;
import org.shiguang.module.stock.service.StockDataProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 股票数据控制器
 * 提供股票数据的REST API接口
 */
@RestController
@RequestMapping("/stock")
public class StockDataController {

    private static final Logger logger = LoggerFactory.getLogger(StockDataController.class);
    private final Random random = new Random();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${kafka.topics.stock-data:stock-data}")
    private String stockDataTopic;

    @Autowired
    private StockCrawlerService stockCrawlerService;

    @Autowired
    private StockDataProcessingService stockDataProcessingService;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

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
    
    /**
     * 手动生成并发送测试股票数据
     */
    @PostMapping("/generate-test-data")
    public ResponseEntity<Map<String, Object>> generateTestData(
            @RequestParam(value = "code", defaultValue = "000001") String code,
            @RequestParam(value = "name", defaultValue = "平安银行") String name) {
        logger.info("手动生成测试股票数据: code={}, name={}", code, name);
        
        Map<String, Object> result = new HashMap<>();
        try {
            // 生成测试数据
            Map<String, Object> stockData = generateRandomStockData(code, name);
            
            // 转换为JSON
            String json = objectMapper.writeValueAsString(stockData);
            
            // 发送到Kafka
            kafkaTemplate.send(stockDataTopic, json);
            
            result.put("success", true);
            result.put("message", "成功生成并发送测试股票数据");
            result.put("data", stockData);
            
            logger.info("已发送测试股票数据到Kafka: {}", json);
        } catch (Exception e) {
            logger.error("生成测试股票数据失败: {}", e.getMessage(), e);
            result.put("success", false);
            result.put("message", "生成测试股票数据失败: " + e.getMessage());
        }
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 生成随机股票数据
     */
    private Map<String, Object> generateRandomStockData(String code, String name) {
        double basePrice = 10.0 + random.nextDouble() * 20.0; // 10-30元之间的基础价格
        double openPrice = basePrice * (1 + (random.nextDouble() - 0.5) * 0.02); // 开盘价在基础价格上下1%波动
        double closePrice = basePrice * (1 + (random.nextDouble() - 0.5) * 0.04); // 收盘价在基础价格上下2%波动
        double highPrice = Math.max(openPrice, closePrice) * (1 + random.nextDouble() * 0.02); // 最高价
        double lowPrice = Math.min(openPrice, closePrice) * (1 - random.nextDouble() * 0.02); // 最低价
        
        // 保留两位小数
        openPrice = Math.round(openPrice * 100) / 100.0;
        closePrice = Math.round(closePrice * 100) / 100.0;
        highPrice = Math.round(highPrice * 100) / 100.0;
        lowPrice = Math.round(lowPrice * 100) / 100.0;
        
        double changePercent = (closePrice - openPrice) / openPrice * 100;
        changePercent = Math.round(changePercent * 100) / 100.0;
        
        long volume = 10000 + random.nextInt(1000000); // 成交量
        double amount = volume * closePrice; // 成交金额
        
        LocalDate today = LocalDate.now();
        String tradeDate = today.format(DateTimeFormatter.ISO_DATE);
        
        Map<String, Object> data = new HashMap<>();
        data.put("code", code);
        data.put("name", name);
        data.put("openPrice", openPrice);
        data.put("closePrice", closePrice);
        data.put("highPrice", highPrice);
        data.put("lowPrice", lowPrice);
        data.put("changePercent", changePercent);
        data.put("volume", volume);
        data.put("amount", amount);
        data.put("tradeDate", tradeDate);
        data.put("timestamp", System.currentTimeMillis());
        
        return data;
    }
} 