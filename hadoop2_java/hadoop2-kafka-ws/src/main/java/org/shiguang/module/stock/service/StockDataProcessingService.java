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
                        
                        // 提取最新的一条记录，确保实时分析
                        Dataset<Row> latestDataDF = spark.sql(
                                "SELECT * FROM stock_data " +
                                "WHERE realtime = true " +
                                "ORDER BY tradeDate DESC LIMIT 1"
                        );
                        
                        if (latestDataDF.count() == 0) {
                            logger.info("未找到实时股票数据，将处理所有可用数据");
                        } else {
                            logger.info("已找到实时数据，优先处理最新记录");
                            // 将最新数据注册为单独的视图
                            latestDataDF.createOrReplaceTempView("latest_stock_data");
                        }
                        
                        // 记录日志，显示处理的记录数
                        long recordCount = stockDataDF.count();
                        if (recordCount > 0) {
                            logger.info("处理批次包含{}条股票数据记录", recordCount);
                            // 显示一些样例数据
                            if (recordCount > 0 && logger.isDebugEnabled()) {
                                logger.debug("数据样例: \n{}", stockDataDF.showString(3, 20, false));
                            }
                        } else {
                            logger.info("处理批次不包含有效数据");
                            return;
                        }
                        
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
                        
                        // 添加详细的日志输出
                        if (!statsRows.isEmpty()) {
                            logger.info("共处理了{}支股票的统计信息", statsRows.size());
                            
                            for (Row statsRow : statsRows) {
                                String code = statsRow.getString(0);
                                String name = statsRow.getString(1);
                                double minPrice = statsRow.getDouble(2);
                                double maxPrice = statsRow.getDouble(3);
                                double avgPrice = statsRow.getDouble(4);
                                long totalVolume = statsRow.getLong(8);
                                
                                logger.info("处理股票数据 - 代码: {}, 名称: {}, 最低价: {}, 最高价: {}, 平均价: {}, 总量: {}", 
                                    code, name, minPrice, maxPrice, avgPrice, totalVolume);
                                
                                // 查找对应的趋势信息
                                for (Row trendRow : trendRows) {
                                    if (trendRow.getString(0).equals(code)) {
                                        double firstOpen = trendRow.getDouble(2);
                                        double lastClose = trendRow.getDouble(3);
                                        double change = lastClose - firstOpen;
                                        double changePercent = (change / firstOpen) * 100;
                                        
                                        // 增加趋势判断
                                        String trendDirection = change > 0 ? "上涨" : (change < 0 ? "下跌" : "持平");
                                        String strengthLevel = "";
                                        if (Math.abs(changePercent) > 3) {
                                            strengthLevel = "强";
                                        } else if (Math.abs(changePercent) > 1) {
                                            strengthLevel = "中";
                                        } else {
                                            strengthLevel = "弱";
                                        }
                                        
                                        String trendAnalysis = strengthLevel + trendDirection;
                                        
                                        logger.info("股票趋势 - 开盘: {}, 收盘: {}, 变化: {}, 变化率: {}%, 趋势: {} ({})", 
                                            firstOpen,
                                            lastClose,
                                            String.format("%.2f", change),
                                            String.format("%.2f", changePercent),
                                            trendDirection,
                                            trendAnalysis);
                                            
                                        // 检查是否是实时数据，添加更多的实时分析
                                        Dataset<Row> realTimeDF = spark.sql(
                                            "SELECT * FROM stock_data WHERE realtime = true AND code = '" + code + "' LIMIT 1"
                                        );
                                        
                                        if (!realTimeDF.isEmpty()) {
                                            logger.info("实时行情分析 - 股票: {} ({}) - 实时更新", name, code);
                                            
                                            // 获取最近的成交量变化
                                            Dataset<Row> volumeChangeDF = spark.sql(
                                                "SELECT " +
                                                "(SELECT volume FROM stock_data WHERE code = '" + code + "' ORDER BY tradeDate DESC LIMIT 1) as current_volume, " +
                                                "AVG(volume) as avg_volume " +
                                                "FROM stock_data WHERE code = '" + code + "'"
                                            );
                                            
                                            Row volumeRow = volumeChangeDF.first();
                                            long currentVolume = volumeRow.getLong(0);
                                            double avgVolume = volumeRow.getDouble(1);
                                            double volumeChangePercent = ((double)currentVolume - avgVolume) / avgVolume * 100;
                                            
                                            logger.info("成交量分析: 当前={}, 平均={}, 变化率={}%, 状态={}", 
                                                currentVolume, 
                                                Math.round(avgVolume),
                                                String.format("%.2f", volumeChangePercent),
                                                volumeChangePercent > 50 ? "放量" : (volumeChangePercent < -20 ? "缩量" : "正常"));
                                                
                                            // 获取短期价格波动情况
                                            double currentPrice = lastClose;
                                            double priceVolatility = (maxPrice - minPrice) / minPrice * 100;
                                            
                                            logger.info("价格波动分析: 波动率={}%, 状态={}", 
                                                String.format("%.2f", priceVolatility),
                                                priceVolatility > stockConfig.getVolatilityThreshold() ? "高波动" : "稳定");
                                        }
                                        
                                        break;
                                    }
                                }
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
                        
                        // 转换为JSON并发送到WebSocket
                        ObjectMapper localMapper = new ObjectMapper(); // 创建本地ObjectMapper
                        String resultJson = localMapper.writeValueAsString(resultMap);
                        messagingTemplate.convertAndSend("/topic/stock-data-analytics", resultJson);
                        
                        processedCount.incrementAndGet();
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