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

} 