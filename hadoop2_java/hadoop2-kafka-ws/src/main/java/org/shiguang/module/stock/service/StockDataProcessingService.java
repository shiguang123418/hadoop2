package org.shiguang.module.stock.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.shiguang.config.SparkContextManager;
import org.shiguang.module.stock.config.StockConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 股票数据处理服务
 * 使用Spark Streaming处理从Kafka接收的股票数据
 */
@Service
public class StockDataProcessingService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(StockDataProcessingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    
    @Value("${kafka.topics.stock-data:stock-data}")
    private String stockDataTopic;
    
    @Value("${spark.enabled:true}")
    private boolean sparkEnabled;
    
    @Autowired
    private transient SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private transient SparkContextManager sparkContextManager;
    
    @Autowired
    private transient StockConfig stockConfig;
    
    private transient JavaStreamingContext streamingContext;
    private transient Thread streamingThread;
    private transient AtomicBoolean running = new AtomicBoolean(false);
    private transient AtomicInteger processedCount = new AtomicInteger(0);
    private transient AtomicInteger errorCount = new AtomicInteger(0);
    
    /**
     * 初始化并启动Spark Streaming
     */
    @PostConstruct
    public void init() {
        if (sparkEnabled && stockConfig.isProcessorEnabled()) {
            initSparkStreamingForStockData();
        } else {
            logger.info("股票数据Spark Streaming已禁用 (sparkEnabled={}, processorEnabled={})",
                    sparkEnabled, stockConfig.isProcessorEnabled());
        }
    }
    
    /**
     * 初始化用于处理股票数据的Spark Streaming
     */
    private void initSparkStreamingForStockData() {
        try {
            // 检查SparkContextManager是否已初始化
            if (!sparkContextManager.isInitialized()) {
                logger.error("SparkContextManager未初始化，无法创建股票数据处理流");
                return;
            }
            
            // 获取共享的JavaStreamingContext
            streamingContext = sparkContextManager.getStreamingContext();
            
            if (streamingContext == null) {
                logger.error("获取StreamingContext失败");
                return;
            }
            
            // 配置Kafka消费者
            Map<String, Object> kafkaParams = new HashMap<>();
            kafkaParams.put("bootstrap.servers", bootstrapServers);
            kafkaParams.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaParams.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaParams.put("group.id", "spark-stock-data-group");
            kafkaParams.put("auto.offset.reset", "latest");
            kafkaParams.put("enable.auto.commit", "true");
            
            // 创建Kafka流
            JavaInputDStream<org.apache.kafka.clients.consumer.ConsumerRecord<String, String>> stream =
                    KafkaUtils.createDirectStream(
                            streamingContext,
                            LocationStrategies.PreferConsistent(),
                            ConsumerStrategies.Subscribe(Collections.singletonList(stockDataTopic), kafkaParams)
                    );
            
            // 处理接收的股票数据
            JavaDStream<String> stockDataJsonStream = stream.map(record -> record.value());
            
            stockDataJsonStream.foreachRDD(rdd -> {
                if (!rdd.isEmpty()) {
                    try {
                        // 获取SparkSession
                        SparkSession spark = sparkContextManager.getSparkSession();
                        if (spark == null) {
                            logger.error("无法获取SparkSession");
                            return;
                        }
                        
                        // 将RDD转换为DataFrame
                        Dataset<Row> stockDataDF = spark.read().json(rdd);
                        
                        // 注册为临时视图，以便使用SQL
                        stockDataDF.createOrReplaceTempView("stock_data");
                        
                        // 执行SQL查询计算统计信息
                        Dataset<Row> statsDF = spark.sql(
                                "SELECT code, name, " +
                                "MIN(lowPrice) as minPrice, " +
                                "MAX(highPrice) as maxPrice, " +
                                "AVG(closePrice) as avgPrice, " +
                                "AVG(volume) as avgVolume, " +
                                "AVG(amount) as avgAmount, " +
                                "AVG(changePercent) as avgChangePercent, " +
                                "SUM(volume) as totalVolume, " +
                                "SUM(amount) as totalAmount " +
                                "FROM stock_data GROUP BY code, name"
                        );
                        
                        // 计算涨跌幅区间分布
                        Dataset<Row> changePercentDistributionDF = spark.sql(
                                "SELECT " +
                                "CASE " +
                                "  WHEN changePercent < -5 THEN '低于-5%' " +
                                "  WHEN changePercent >= -5 AND changePercent < -2 THEN '-5%到-2%' " +
                                "  WHEN changePercent >= -2 AND changePercent < 0 THEN '-2%到0%' " +
                                "  WHEN changePercent >= 0 AND changePercent < 2 THEN '0%到2%' " +
                                "  WHEN changePercent >= 2 AND changePercent < 5 THEN '2%到5%' " +
                                "  ELSE '高于5%' " +
                                "END as changeRange, " +
                                "COUNT(*) as count " +
                                "FROM stock_data " +
                                "GROUP BY changeRange"
                        );
                        
                        // 从配置中获取波动率阈值
                        double volatilityThreshold = stockConfig.getVolatilityThreshold();
                        
                        // 识别波动率超过阈值的股票
                        Dataset<Row> volatileStocksDF = spark.sql(
                                String.format(
                                "SELECT code, name, MAX(highPrice) - MIN(lowPrice) as priceRange, " +
                                "((MAX(highPrice) - MIN(lowPrice)) / MIN(lowPrice)) * 100 as volatilityPercent " +
                                "FROM stock_data " +
                                "GROUP BY code, name " +
                                "HAVING volatilityPercent > %.2f", volatilityThreshold)
                        );
                        
                        // 计算成交量的均值和标准差
                        Dataset<Row> volumeStatsDF = spark.sql(
                                "SELECT " +
                                "AVG(volume) as meanVolume, " +
                                "SQRT(AVG(POW(volume - " +
                                "  (SELECT AVG(volume) FROM stock_data), 2))) as stdDevVolume " +
                                "FROM stock_data"
                        );
                        
                        // 计算近期价格趋势
                        Dataset<Row> trendDF = spark.sql(
                                "SELECT code, name, " +
                                "FIRST_VALUE(openPrice) OVER (ORDER BY tradeDate) as firstOpen, " +
                                "LAST_VALUE(closePrice) OVER (ORDER BY tradeDate ROWS BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING) as lastClose " +
                                "FROM stock_data"
                        );
                        
                        // 收集计算结果
                        List<Row> statsRows = statsDF.collectAsList();
                        List<Row> changeDistRows = changePercentDistributionDF.collectAsList();
                        List<Row> volatileStocksRows = volatileStocksDF.collectAsList();
                        List<Row> volumeStatRows = volumeStatsDF.collectAsList();
                        List<Row> trendRows = trendDF.collectAsList();
                        
                        // 构建结果JSON
                        Map<String, Object> resultMap = new HashMap<>();
                        
                        if (!statsRows.isEmpty()) {
                            Row statsRow = statsRows.get(0);
                            Map<String, Object> basicStats = new HashMap<>();
                            basicStats.put("code", statsRow.getString(0));
                            basicStats.put("name", statsRow.getString(1));
                            basicStats.put("minPrice", statsRow.getDouble(2));
                            basicStats.put("maxPrice", statsRow.getDouble(3));
                            basicStats.put("avgPrice", statsRow.getDouble(4));
                            basicStats.put("avgVolume", statsRow.getDouble(5));
                            basicStats.put("avgAmount", statsRow.getDouble(6));
                            basicStats.put("avgChangePercent", statsRow.getDouble(7));
                            basicStats.put("totalVolume", statsRow.getLong(8));
                            basicStats.put("totalAmount", statsRow.getDouble(9));
                            resultMap.put("basicStats", basicStats);
                        }
                        
                        List<Map<String, Object>> changeDist = new ArrayList<>();
                        for (Row row : changeDistRows) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("range", row.getString(0));
                            item.put("count", row.getLong(1));
                            changeDist.add(item);
                        }
                        resultMap.put("changePercentDistribution", changeDist);
                        
                        // 添加高波动率股票信息
                        List<Map<String, Object>> volatileStocks = new ArrayList<>();
                        for (Row row : volatileStocksRows) {
                            Map<String, Object> stock = new HashMap<>();
                            stock.put("code", row.getString(0));
                            stock.put("name", row.getString(1));
                            stock.put("priceRange", row.getDouble(2));
                            stock.put("volatilityPercent", row.getDouble(3));
                            volatileStocks.add(stock);
                        }
                        resultMap.put("volatileStocks", volatileStocks);
                        resultMap.put("volatilityThreshold", stockConfig.getVolatilityThreshold());
                        
                        if (!volumeStatRows.isEmpty()) {
                            Row volumeRow = volumeStatRows.get(0);
                            Map<String, Object> volumeStats = new HashMap<>();
                            volumeStats.put("mean", volumeRow.getDouble(0));
                            volumeStats.put("stdDev", volumeRow.getDouble(1));
                            resultMap.put("volumeStats", volumeStats);
                        }
                        
                        if (!trendRows.isEmpty()) {
                            Row trendRow = trendRows.get(0);
                            Map<String, Object> trend = new HashMap<>();
                            double firstOpen = trendRow.getDouble(2);
                            double lastClose = trendRow.getDouble(3);
                            double change = lastClose - firstOpen;
                            double changePercent = (change / firstOpen) * 100;
                            
                            trend.put("firstOpen", firstOpen);
                            trend.put("lastClose", lastClose);
                            trend.put("change", change);
                            trend.put("changePercent", changePercent);
                            trend.put("trend", change > 0 ? "上涨" : (change < 0 ? "下跌" : "持平"));
                            resultMap.put("trend", trend);
                        }
                        
                        // 转换为JSON并发送到WebSocket
                        ObjectMapper localMapper = new ObjectMapper(); // 创建本地ObjectMapper
                        String resultJson = localMapper.writeValueAsString(resultMap);
                        messagingTemplate.convertAndSend("/topic/stock-data-analytics", resultJson);
                        
                        processedCount.incrementAndGet();
                        // 添加更详细的日志输出
                        if (!statsRows.isEmpty()) {
                            Row statsRow = statsRows.get(0);
                            logger.info("处理股票数据 - 代码: {}, 名称: {}, 最低价: {}, 最高价: {}, 平均价: {}, 总量: {}", 
                                statsRow.getString(0), 
                                statsRow.getString(1),
                                statsRow.getDouble(2),
                                statsRow.getDouble(3),
                                statsRow.getDouble(4),
                                statsRow.getLong(8));
                            
                            if (!trendRows.isEmpty()) {
                                Row trendRow = trendRows.get(0);
                                double firstOpen = trendRow.getDouble(2);
                                double lastClose = trendRow.getDouble(3);
                                double change = lastClose - firstOpen;
                                double changePercent = (change / firstOpen) * 100;
                                logger.info("股票趋势 - 开盘: {}, 收盘: {}, 变化: {}, 变化率: {}%, 趋势: {}", 
                                    firstOpen,
                                    lastClose,
                                    String.format("%.2f", change),
                                    String.format("%.2f", changePercent),
                                    change > 0 ? "上涨" : (change < 0 ? "下跌" : "持平"));
                            }
                            
                            // 输出涨跌幅分布
                            if (!changeDistRows.isEmpty()) {
                                StringBuilder distBuilder = new StringBuilder("涨跌幅分布:");
                                for (Row row : changeDistRows) {
                                    distBuilder.append(" ").append(row.getString(0)).append(": ").append(row.getLong(1));
                                }
                                logger.info(distBuilder.toString());
                            }
                            
                            // 输出高波动率股票信息
                            if (!volatileStocksRows.isEmpty()) {
                                logger.info("发现{}支高波动率股票 (阈值: {}%):", volatileStocksRows.size(), stockConfig.getVolatilityThreshold());
                                for (Row row : volatileStocksRows) {
                                    logger.info("  {} ({}) - 波动幅度: {}元, 波动率: {}%", 
                                        row.getString(1), row.getString(0), 
                                        String.format("%.2f", row.getDouble(2)),
                                        String.format("%.2f", row.getDouble(3)));
                                }
                            } else {
                                logger.info("未发现高波动率股票 (阈值: {}%)", stockConfig.getVolatilityThreshold());
                            }
                        } else {
                            logger.info("未能解析到股票数据统计信息");
                        }
                        
                        logger.debug("已将股票数据分析结果发送到WebSocket: {}", resultJson);
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        logger.error("处理股票数据时发生错误: {}", e.getMessage(), e);
                    }
                }
            });
            
            // 不在这里启动Spark Streaming，由SparkContextManager统一管理
            // 设置运行状态为true
            running.set(true);
            logger.info("股票数据Spark Streaming已初始化");
        } catch (Exception e) {
            logger.error("初始化股票数据Spark Streaming失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 启动Spark Streaming
     */
    public void start() {
        // 不再需要单独启动，由SparkContextManager统一管理
        if (!running.get()) {
            logger.info("股票数据Spark Streaming未初始化，无法启动");
        }
    }
    
    /**
     * 关闭Spark Streaming
     */
    @PreDestroy
    public void shutdown() {
        // 不再需要关闭，由SparkContextManager统一管理
        running.set(false);
        logger.info("股票数据Spark Streaming已关闭");
    }
    
    /**
     * 获取服务统计信息
     */
    public String getServiceStats() {
        return String.format("处理批次: %d, 错误: %d", 
                processedCount.get(), errorCount.get());
    }
} 