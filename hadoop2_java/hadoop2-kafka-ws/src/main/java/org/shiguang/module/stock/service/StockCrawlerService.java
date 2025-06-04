package org.shiguang.module.stock.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.shiguang.model.StockData;
import org.shiguang.module.stock.config.StockConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

/**
 * 股票数据爬虫服务
 * 用于从东方财富网站爬取股票数据，并发送到Kafka
 */
@Service
@Slf4j
public class StockCrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(StockCrawlerService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private KafkaProducer<String, String> producer;
    private final AtomicInteger requestCounter = new AtomicInteger(0);
    private final AtomicInteger errorCounter = new AtomicInteger(0);
    private final AtomicInteger simulatedRequestCounter = new AtomicInteger(0);
    
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    
    @Value("${kafka.topics.stock-data:stock-data}")
    private String stockDataTopic;
    
    @Autowired
    private StockConfig stockConfig;
    
    /**
     * 初始化Kafka生产者
     */
    @PostConstruct
    public void init() {
        // 创建Kafka生产者配置
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        // 创建Kafka生产者
        producer = new KafkaProducer<>(props);
        logger.info("股票数据爬虫服务已初始化，爬取功能状态: {}", stockConfig.isCrawlerEnabled() ? "已启用" : "已禁用");
    }
    
    /**
     * 关闭Kafka生产者
     */
    @PreDestroy
    public void destroy() {
        if (producer != null) {
            producer.close();
        }
        logger.info("股票数据爬虫服务已关闭");
    }
    
    /**
     * 定时爬取股票数据并发送到Kafka
     * 间隔由配置控制
     */
    @Scheduled(fixedRateString = "${stock.crawler.interval:5000}")
    public void scheduledCrawlStockData() {
        // 检查爬取功能是否启用
        if (!stockConfig.isCrawlerEnabled()) {
            logger.debug("股票数据爬取功能已禁用，跳过本次爬取");
            return;
        }

        boolean useSimulation = stockConfig.isSimulationEnabled();
        
        // 使用模拟数据还是真实API
        if (useSimulation) {
            logger.info("使用模拟数据源获取股票数据");
            // 获取模拟数据
            String code = stockConfig.getSimulationTargetCode();
            int market = code.startsWith("6") ? 1 : 0;
            
            try {
                List<StockData> stockDataList = fetchSimulatedStockData(code, market);
                for (StockData stockData : stockDataList) {
                    // 标记这是模拟数据
                    stockData.setRealtime(true);
                    sendToKafka(stockData);
                }
            } catch (Exception e) {
                errorCounter.incrementAndGet();
                logger.error("获取模拟股票数据失败: {}", e.getMessage(), e);
            }
        } else {
            logger.info("开始获取实时股票数据: {}", stockConfig.getDefaultStockCode());
            try {
                List<StockData> stockDataList = crawlStockData(stockConfig.getDefaultStockCode(), 0);
                for (StockData stockData : stockDataList) {
                    // 标记这是实时数据
                    stockData.setRealtime(true);
                    sendToKafka(stockData);
                }
            } catch (Exception e) {
                errorCounter.incrementAndGet();
                logger.error("获取实时股票数据失败: {}", e.getMessage(), e);
            }
        }
    }
    
    /**
     * 爬取指定股票的数据
     *
     * @param code   股票代码
     * @param market 市场代码（0表示深圳，1表示上海）
     * @return 股票数据列表
     */
    public List<StockData> crawlStockData(String code, int market) {
        List<StockData> result = new ArrayList<>();
        String url = buildEastMoneyApiUrl(code, market);
        
        try {
            String response = sendHttpRequest(url);
            if (response != null && !response.isEmpty()) {
                // 解析返回的JSONP数据
                String jsonData = extractJsonFromJSONP(response);
                if (jsonData != null) {
                    JsonNode rootNode = objectMapper.readTree(jsonData);
                    JsonNode dataNode = rootNode.path("data");
                    
                    if (!dataNode.isMissingNode()) {
                        String stockCode = dataNode.path("code").asText();
                        int stockMarket = dataNode.path("market").asInt();
                        String stockName = dataNode.path("name").asText();
                        
                        // 处理K线数据
                        JsonNode klinesNode = dataNode.path("klines");
                        if (klinesNode.isArray()) {
                            logger.info("获取到{}条K线数据", klinesNode.size());
                            
                            for (JsonNode klineNode : klinesNode) {
                                String klineData = klineNode.asText();
                                StockData stockData = parseKLineData(klineData, stockCode, stockMarket, stockName);
                                if (stockData != null) {
                                    result.add(stockData);
                                    logger.debug("解析K线数据：{} {} 价格: {}，成交量: {}，日期: {}", 
                                            stockData.getCode(), stockData.getName(),
                                            stockData.getClosePrice(), stockData.getVolume(),
                                            stockData.getTradeDate());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            logger.error("爬取股票数据失败: {}", e.getMessage(), e);
        }
        
        logger.info("爬取到{}条股票数据", result.size());
        return result;
    }
    
    /**
     * 构建东方财富API的URL
     *
     * @param code   股票代码
     * @param market 市场代码
     * @return API URL
     */
    private String buildEastMoneyApiUrl(String code, int market) {
        String secid = market + "." + code;
        long timestamp = System.currentTimeMillis();
        // 添加随机数，避免请求被缓存
        int random = new Random().nextInt(10000);
        
        return String.format(
                "https://push2his.eastmoney.com/api/qt/stock/kline/get" +
                "?cb=jQuery351034703624902167385_%d" +
                "&secid=%s" +
                "&ut=fa5fd1943c7b386f172d6893dbfba10b" +
                "&fields1=f1,f2,f3,f4,f5,f6" +
                "&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61" +
                // 获取最新的单条记录
                "&klt=101&fqt=1&end=20500101&lmt=1&_=%d&r=%d",
                timestamp - 100, secid, timestamp, random);
    }
    
    /**
     * 构建模拟API的URL
     *
     * @param code   股票代码
     * @param market 市场代码
     * @return 模拟API URL
     */
    private String buildSimulationApiUrl(String code, int market) {
        String secid = market + "." + code;
        long timestamp = System.currentTimeMillis();
        int random = new Random().nextInt(10000);
        String callback = "jQuery351034703624902167385_" + timestamp;
        
        return String.format("%s/api/stock-simulation/kline?cb=%s&secid=%s", 
                stockConfig.getSimulationApiHost(), callback, secid);
    }
    
    /**
     * 从模拟API获取股票数据
     *
     * @param code   股票代码
     * @param market 市场代码
     * @return 股票数据列表
     */
    public List<StockData> fetchSimulatedStockData(String code, int market) {
        List<StockData> result = new ArrayList<>();
        String url = buildSimulationApiUrl(code, market);
        
        try {
            simulatedRequestCounter.incrementAndGet();
            String response = sendHttpRequest(url);
            if (response != null && !response.isEmpty()) {
                // 解析返回的JSONP数据
                String jsonData = extractJsonFromJSONP(response);
                if (jsonData != null) {
                    JsonNode rootNode = objectMapper.readTree(jsonData);
                    JsonNode dataNode = rootNode.path("data");
                    
                    if (!dataNode.isMissingNode()) {
                        String stockCode = dataNode.path("code").asText();
                        int stockMarket = dataNode.path("market").asInt();
                        String stockName = dataNode.path("name").asText();
                        
                        // 处理K线数据
                        JsonNode klinesNode = dataNode.path("klines");
                        if (klinesNode.isArray()) {
                            logger.info("获取到{}条模拟K线数据", klinesNode.size());
                            
                            for (JsonNode klineNode : klinesNode) {
                                String klineData = klineNode.asText();
                                StockData stockData = parseKLineData(klineData, stockCode, stockMarket, stockName);
                                if (stockData != null) {
                                    result.add(stockData);
                                    logger.debug("解析模拟K线数据：{} {} 价格: {}，成交量: {}，日期: {}", 
                                            stockData.getCode(), stockData.getName(),
                                            stockData.getClosePrice(), stockData.getVolume(),
                                            stockData.getTradeDate());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            errorCounter.incrementAndGet();
            logger.error("获取模拟股票数据失败: {}", e.getMessage(), e);
        }
        
        logger.info("获取到{}条模拟股票数据", result.size());
        return result;
    }
    
    /**
     * 发送HTTP请求获取数据
     *
     * @param urlString API的URL
     * @return 响应内容
     */
    private String sendHttpRequest(String urlString) throws Exception {
        requestCounter.incrementAndGet();
        StringBuilder response = new StringBuilder();
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
        
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
            return response.toString();
        } else {
            logger.warn("HTTP请求失败，响应码: {}", responseCode);
            return null;
        }
    }
    
    /**
     * 从JSONP响应中提取JSON数据
     *
     * @param jsonp JSONP响应字符串
     * @return JSON数据字符串
     */
    private String extractJsonFromJSONP(String jsonp) {
        // 使用正则表达式提取JSON部分
        Pattern pattern = Pattern.compile("jQuery[0-9_]+\\((.*)\\);?");
        Matcher matcher = pattern.matcher(jsonp);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 解析K线数据字符串为StockData对象
     *
     * @param klineData  K线数据字符串
     * @param stockCode  股票代码
     * @param stockMarket 股票市场
     * @param stockName  股票名称
     * @return StockData对象
     */
    private StockData parseKLineData(String klineData, String stockCode, int stockMarket, String stockName) {
        try {
            // K线数据格式："2023-06-04,41.41,41.73,41.76,40.88,329038,1355729944.80,2.12,0.46,0.19,0.86"
            String[] parts = klineData.split(",");
            if (parts.length < 11) {
                logger.warn("K线数据格式不正确: {}", klineData);
                return null;
            }
            
            return StockData.builder()
                    .code(stockCode)
                    .market(stockMarket)
                    .name(stockName)
                    .tradeDate(parts[0])
                    .openPrice(Double.parseDouble(parts[1]))
                    .closePrice(Double.parseDouble(parts[2]))
                    .highPrice(Double.parseDouble(parts[3]))
                    .lowPrice(Double.parseDouble(parts[4]))
                    .volume(Long.parseLong(parts[5]))
                    .amount(Double.parseDouble(parts[6]))
                    .amplitude(Double.parseDouble(parts[7]))
                    .changePercent(Double.parseDouble(parts[8]))
                    .change(Double.parseDouble(parts[9]))
                    .turnoverRate(Double.parseDouble(parts[10]))
                    .build();
        } catch (Exception e) {
            logger.error("解析K线数据失败: {}, 数据: {}", e.getMessage(), klineData, e);
            return null;
        }
    }
    
    /**
     * 将股票数据发送到Kafka
     */
    private void sendToKafka(StockData stockData) {
        try {
            String json = objectMapper.writeValueAsString(stockData);
            producer.send(new ProducerRecord<>(stockDataTopic, stockData.getCode(), json));
        } catch (Exception e) {
            logger.error("发送股票数据到Kafka失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取服务状态统计
     */
    public String getServiceStats() {
        boolean isSimulationMode = stockConfig.isSimulationEnabled();
        
        return String.format(
                "爬取器: %s, 当前模式: %s, 请求次数: %d, 模拟请求: %d, 错误次数: %d", 
                stockConfig.isCrawlerEnabled() ? "启用" : "禁用",
                isSimulationMode ? "模拟" : "实时",
                requestCounter.get(),
                simulatedRequestCounter.get(),
                errorCounter.get());
    }
} 