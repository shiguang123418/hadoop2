package org.shiguang.module.spark.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.shiguang.module.spark.client.SparkClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class SparkStreamingService {
    private static final Logger logger = LoggerFactory.getLogger(SparkStreamingService.class);
    
    @Autowired
    private SparkClient sparkClient;
    
    @Autowired(required = false)  // 将required设置为false，使其变为可选
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Value("${kafka.bootstrap.servers:localhost:9092}")
    private String kafkaBootstrapServers;
    
    private JavaStreamingContext streamingContext;
    private AtomicBoolean isRunning = new AtomicBoolean(false);
    
    /**
     * 启动天气数据的实时流处理
     * @param topicName Kafka主题名称
     * @param groupId 消费者组ID
     * @param batchIntervalSeconds 批处理间隔(秒)
     */
    public void startWeatherDataStreaming(String topicName, String groupId, int batchIntervalSeconds) {
        if (isRunning.get()) {
            logger.info("Spark Streaming作业已在运行中");
            return;
        }
        
        try {
            // 详细记录参数信息
            logger.info("开始启动天气数据流处理 - 主题: {}, 消费者组: {}, 批处理间隔: {}秒, Kafka地址: {}", 
                    topicName, groupId, batchIntervalSeconds, kafkaBootstrapServers);
            
            // 创建Spark Streaming上下文
            final SparkSession sparkSession;
            try {
                sparkSession = sparkClient.getSparkSession();
                logger.info("成功获取SparkSession: {}", sparkSession);
            } catch (Exception e) {
                logger.error("获取SparkSession失败: {}", e.getMessage(), e);
                throw new RuntimeException("无法创建Spark会话", e);
            }
            
            try {
                streamingContext = new JavaStreamingContext(
                        sparkSession.sparkContext().conf(),
                        Durations.seconds(batchIntervalSeconds)
                );
                logger.info("成功创建JavaStreamingContext");
            } catch (Exception e) {
                logger.error("创建JavaStreamingContext失败: {}", e.getMessage(), e);
                throw new RuntimeException("无法创建流处理上下文", e);
            }
            
            // 配置Kafka消费者
            Map<String, Object> kafkaParams = new HashMap<>();
            kafkaParams.put("bootstrap.servers", kafkaBootstrapServers);
            kafkaParams.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaParams.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaParams.put("group.id", groupId);
            kafkaParams.put("auto.offset.reset", "latest");
            kafkaParams.put("enable.auto.commit", false);
            
            logger.info("已配置Kafka参数: {}", kafkaParams);
            
            // 创建直接流
            Collection<String> topics = Collections.singletonList(topicName);
            JavaInputDStream<ConsumerRecord<String, String>> directStream = null;
            
            try {
                directStream = KafkaUtils.createDirectStream(
                        streamingContext,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(topics, kafkaParams)
                );
                logger.info("成功创建Kafka直接流");
            } catch (Exception e) {
                logger.error("创建Kafka直接流失败: {}", e.getMessage(), e);
                throw new RuntimeException("无法创建从Kafka的数据流", e);
            }
            
            // 处理每个RDD
            directStream.foreachRDD((rdd, time) -> {
                logger.info("处理批次时间: {}, RDD分区数: {}", time, rdd.getNumPartitions());
                
                if (!rdd.isEmpty()) {
                    // 将RDD转换为数据的JavaRDD
                    JavaRDD<String> weatherDataRDD = rdd.map(ConsumerRecord::value);
                    logger.info("RDD非空, 记录数: {}", weatherDataRDD.count());
                    
                    if (!weatherDataRDD.isEmpty()) {
                        try {
                            // 获取数据结构和类型
                            // 首先提取CSV表头
                            String headerLine = weatherDataRDD.first();
                            String[] columns = headerLine.split(",");
                            logger.info("CSV表头: {}", Arrays.toString(columns));
                            
                            // 定义Schema
                            List<StructField> fields = new ArrayList<>();
                            fields.add(DataTypes.createStructField("city", DataTypes.StringType, true));
                            fields.add(DataTypes.createStructField("state", DataTypes.StringType, true));
                            fields.add(DataTypes.createStructField("month", DataTypes.StringType, true));
                            fields.add(DataTypes.createStructField("maxtemp", DataTypes.DoubleType, true));
                            fields.add(DataTypes.createStructField("mintemp", DataTypes.DoubleType, true));
                            fields.add(DataTypes.createStructField("rainfall", DataTypes.DoubleType, true));
                            StructType schema = DataTypes.createStructType(fields);
                            
                            // 过滤掉表头并将每行CSV转换为Row对象
                            JavaRDD<Row> rowRDD = weatherDataRDD
                                .filter(line -> !line.equals(headerLine))
                                .map(line -> {
                                    try {
                                        String[] parts = line.split(",");
                                        if (parts.length != 6) {
                                            logger.warn("CSV行格式不正确: {}", line);
                                            return null;
                                        }
                                        return RowFactory.create(
                                            parts[0], // city
                                            parts[1], // state
                                            parts[2], // month
                                            Double.parseDouble(parts[3]), // maxtemp
                                            Double.parseDouble(parts[4]), // mintemp
                                            Double.parseDouble(parts[5])  // rainfall
                                        );
                                    } catch (NumberFormatException e) {
                                        logger.warn("无法解析数字字段: {}", line, e);
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull);
                            
                            // 使用schema和rowRDD创建DataFrame
                            Dataset<Row> weatherDF = sparkSession.createDataFrame(rowRDD, schema);
                            
                            // 注册为临时视图以便SQL查询
                            weatherDF.createOrReplaceTempView("weather_data");
                            
                            // 执行分析：按城市计算平均温度和总降雨量
                            Dataset<Row> results = sparkSession.sql(
                                    "SELECT city, state, " +
                                    "AVG(maxtemp) as avg_max_temp, " +
                                    "AVG(mintemp) as avg_min_temp, " +
                                    "SUM(rainfall) as total_rainfall " +
                                    "FROM weather_data " +
                                    "GROUP BY city, state"
                            );
                            
                            // 限制结果数量，避免过多数据传输
                            results = results.limit(50);
                            
                            // 转换结果为JSON字符串列表
                            List<String> jsonResults = new ArrayList<>();
                            results.toJavaRDD().collect().forEach(row -> {
                                try {
                                    Map<String, Object> resultMap = new HashMap<>();
                                    resultMap.put("city", row.getString(0));
                                    resultMap.put("state", row.getString(1));
                                    resultMap.put("avgMaxTemp", row.getDouble(2));
                                    resultMap.put("avgMinTemp", row.getDouble(3));
                                    resultMap.put("totalRainfall", row.getDouble(4));
                                    
                                    jsonResults.add(objectMapper.writeValueAsString(resultMap));
                                } catch (Exception e) {
                                    logger.error("转换结果为JSON失败", e);
                                }
                            });
                            
                            // 将结果发送到WebSocket
                            try {
                                if (isWebSocketAvailable()) {
                                    messagingTemplate.convertAndSend("/topic/weather-stats", jsonResults);
                                    logger.info("已通过WebSocket发送 {} 条天气统计数据", jsonResults.size());
                                }
                            } catch (Exception e) {
                                logger.error("发送数据到WebSocket失败: {}", e.getMessage(), e);
                            }
                            
                        } catch (Exception e) {
                            logger.error("处理天气数据失败: {}", e.getMessage(), e);
                        }
                    } else {
                        logger.info("接收到的RDD为空");
                    }
                }
            });
            
            // 启动流处理
            try {
                streamingContext.start();
                isRunning.set(true);
                logger.info("Spark Streaming作业已成功启动: 主题={}, 组={}, 间隔={}秒", 
                        topicName, groupId, batchIntervalSeconds);
            } catch (Exception e) {
                logger.error("启动Spark Streaming上下文失败: {}", e.getMessage(), e);
                throw new RuntimeException("启动流处理失败", e);
            }
            
            // 在单独的线程中等待终止以不阻塞主线程
            new Thread(() -> {
                try {
                    streamingContext.awaitTermination();
                } catch (InterruptedException e) {
                    logger.error("Spark Streaming作业被中断", e);
                }
            }).start();
            
        } catch (Exception e) {
            logger.error("启动Spark Streaming作业失败: {}", e.getMessage(), e);
            throw new RuntimeException("启动实时数据处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 启动产品数据的实时流处理
     * @param topicName Kafka主题名称
     * @param groupId 消费者组ID
     * @param batchIntervalSeconds 批处理间隔(秒)
     */
    public void startProductDataStreaming(String topicName, String groupId, int batchIntervalSeconds) {
        if (isRunning.get()) {
            logger.info("Spark Streaming作业已在运行中");
            return;
        }
        
        try {
            // 详细记录参数信息
            logger.info("开始启动产品数据流处理 - 主题: {}, 消费者组: {}, 批处理间隔: {}秒, Kafka地址: {}", 
                    topicName, groupId, batchIntervalSeconds, kafkaBootstrapServers);
            
            // 创建Spark Streaming上下文
            final SparkSession sparkSession;
            try {
                sparkSession = sparkClient.getSparkSession();
                logger.info("成功获取SparkSession: {}", sparkSession);
            } catch (Exception e) {
                logger.error("获取SparkSession失败: {}", e.getMessage(), e);
                throw new RuntimeException("无法创建Spark会话", e);
            }
            
            try {
                streamingContext = new JavaStreamingContext(
                        sparkSession.sparkContext().conf(),
                        Durations.seconds(batchIntervalSeconds)
                );
                logger.info("成功创建JavaStreamingContext");
            } catch (Exception e) {
                logger.error("创建JavaStreamingContext失败: {}", e.getMessage(), e);
                throw new RuntimeException("无法创建流处理上下文", e);
            }
            
            // 配置Kafka消费者
            Map<String, Object> kafkaParams = new HashMap<>();
            kafkaParams.put("bootstrap.servers", kafkaBootstrapServers);
            kafkaParams.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaParams.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaParams.put("group.id", groupId);
            kafkaParams.put("auto.offset.reset", "latest");
            kafkaParams.put("enable.auto.commit", false);
            
            logger.info("已配置Kafka参数: {}", kafkaParams);
            
            // 创建直接流
            Collection<String> topics = Collections.singletonList(topicName);
            JavaInputDStream<ConsumerRecord<String, String>> directStream = null;
            
            try {
                directStream = KafkaUtils.createDirectStream(
                        streamingContext,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(topics, kafkaParams)
                );
                logger.info("成功创建Kafka直接流");
            } catch (Exception e) {
                logger.error("创建Kafka直接流失败: {}", e.getMessage(), e);
                throw new RuntimeException("无法创建从Kafka的数据流", e);
            }
            
            // 处理每个RDD
            directStream.foreachRDD((rdd, time) -> {
                logger.info("处理批次时间: {}, RDD分区数: {}", time, rdd.getNumPartitions());
                
                if (!rdd.isEmpty()) {
                    // 将RDD转换为数据的JavaRDD
                    JavaRDD<String> productDataRDD = rdd.map(ConsumerRecord::value);
                    logger.info("RDD非空, 记录数: {}", productDataRDD.count());
                    
                    if (!productDataRDD.isEmpty()) {
                        try {
                            // 获取数据结构和类型
                            String headerLine = productDataRDD.first();
                            String[] columns = headerLine.split(",");
                            logger.info("CSV表头: {}", Arrays.toString(columns));
                            
                            // 创建动态Schema
                            List<StructField> fields = new ArrayList<>();
                            for (int i = 0; i < columns.length; i++) {
                                fields.add(DataTypes.createStructField(
                                        columns[i], DataTypes.StringType, true));
                            }
                            StructType schema = DataTypes.createStructType(fields);
                            
                            // 过滤掉表头并将每行CSV转换为Row对象
                            JavaRDD<Row> rowRDD = productDataRDD
                                .filter(line -> !line.equals(headerLine))
                                .map(line -> {
                                    String[] parts = line.split(",");
                                    Object[] values = new Object[parts.length];
                                    for (int i = 0; i < parts.length; i++) {
                                        values[i] = parts[i];
                                    }
                                    return RowFactory.create(values);
                                });
                            
                            // 使用schema和rowRDD创建DataFrame
                            Dataset<Row> productDF = sparkSession.createDataFrame(rowRDD, schema);
                            
                            // 注册为临时视图以便SQL查询
                            productDF.createOrReplaceTempView("product_data");
                            
                            // 执行自定义分析：按农作物分析平均产量和各项参数的关系
                            Dataset<Row> results = sparkSession.sql(
                                    "SELECT Crop, " +
                                    "AVG(CAST(Rainfall AS DOUBLE)) as avg_rainfall, " +
                                    "AVG(CAST(Temperature AS DOUBLE)) as avg_temperature, " +
                                    "AVG(CAST(Ph AS DOUBLE)) as avg_ph, " +
                                    "AVG(CAST(Production AS DOUBLE)) as avg_production, " +
                                    "COUNT(*) as sample_count " +
                                    "FROM product_data " +
                                    "GROUP BY Crop " +
                                    "ORDER BY avg_production DESC"
                            );
                            
                            // 限制结果以避免过多数据传输
                            results = results.limit(20);
                            
                            // 转换结果为JSON字符串
                            List<String> jsonResults = new ArrayList<>();
                            results.toJavaRDD().collect().forEach(row -> {
                                try {
                                    Map<String, Object> resultMap = new HashMap<>();
                                    for (int i = 0; i < row.size(); i++) {
                                        resultMap.put(columns[i], row.get(i));
                                    }
                                    jsonResults.add(objectMapper.writeValueAsString(resultMap));
                                } catch (Exception e) {
                                    logger.error("转换结果为JSON失败", e);
                                }
                            });
                            
                            // 将结果发送到WebSocket
                            try {
                                if (isWebSocketAvailable()) {
                                    messagingTemplate.convertAndSend("/topic/product-stats", jsonResults);
                                    logger.info("已通过WebSocket发送 {} 条产品统计数据", jsonResults.size());
                                }
                            } catch (Exception e) {
                                logger.error("发送数据到WebSocket失败: {}", e.getMessage(), e);
                            }
                            
                        } catch (Exception e) {
                            logger.error("处理产品数据失败", e);
                        }
                    } else {
                        logger.info("接收到的RDD为空");
                    }
                }
            });
            
            // 启动流处理
            try {
                streamingContext.start();
                isRunning.set(true);
                logger.info("Spark Streaming作业已成功启动: 主题={}, 组={}, 间隔={}秒", 
                        topicName, groupId, batchIntervalSeconds);
            } catch (Exception e) {
                logger.error("启动Spark Streaming上下文失败: {}", e.getMessage(), e);
                throw new RuntimeException("启动流处理失败", e);
            }
            
            // 在单独的线程中等待终止以不阻塞主线程
            new Thread(() -> {
                try {
                    streamingContext.awaitTermination();
                } catch (InterruptedException e) {
                    logger.error("Spark Streaming作业被中断", e);
                }
            }).start();
            
        } catch (Exception e) {
            logger.error("启动Spark Streaming作业失败: {}", e.getMessage(), e);
            throw new RuntimeException("启动实时数据处理失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 停止Spark Streaming作业
     */
    public void stopStreaming() {
        if (streamingContext != null && isRunning.get()) {
            streamingContext.stop(true, true);
            isRunning.set(false);
            logger.info("Spark Streaming作业已停止");
        }
    }
    
    /**
     * 检查Streaming作业是否在运行
     * @return 是否运行中
     */
    public boolean isStreamingRunning() {
        return isRunning.get();
    }
    
    // 增加一个方法来检查WebSocket是否可用
    private boolean isWebSocketAvailable() {
        return messagingTemplate != null;
    }
} 