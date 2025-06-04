package org.shiguang.module.sensor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import scala.Tuple2;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Spark流处理服务
 * 使用Spark Streaming处理从Kafka接收的数据并计算统计信息
 */
@Service
public class SparkStreamingService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SparkStreamingService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    
    @Value("${kafka.topics.agriculture-sensor-data}")
    private String sensorTopic;
    
    @Value("${spark.master:local[2]}")
    private String sparkMaster;
    
    @Value("${spark.batch.duration:5}")
    private int batchDuration;
    
    @Value("${spark.enabled:true}")
    private boolean sparkEnabled;
    
    @Autowired(required = false)
    private transient SimpMessagingTemplate messagingTemplate;
    
    private transient JavaStreamingContext streamingContext;
    private transient JavaSparkContext sparkContext;
    private transient SparkSession sparkSession;
    private transient Thread streamingThread;
    private transient AtomicBoolean running = new AtomicBoolean(false);
    
    /**
     * 初始化并启动Spark Streaming
     */
    @PostConstruct
    public void init() {
        if (sparkEnabled) {
            initSparkStreaming();
        } else {
            logger.info("Spark Streaming已禁用");
        }
    }
    
    /**
     * 初始化Spark Streaming
     */
    private void initSparkStreaming() {
        try {
            // 创建Spark配置
            SparkConf conf = new SparkConf()
                    .setMaster(sparkMaster)
                    .setAppName("AgricultureDataProcessing")
                    .set("spark.streaming.stopGracefullyOnShutdown", "true");
            
            // 创建Spark Streaming上下文
            streamingContext = new JavaStreamingContext(conf, Durations.seconds(batchDuration));
            sparkContext = streamingContext.sparkContext();
            
            // 创建SparkSession (用于SQL操作)
            sparkSession = SparkSession.builder()
                    .config(sparkContext.getConf())
                    .getOrCreate();
            
            // 配置Kafka消费者
            Map<String, Object> kafkaParams = new HashMap<>();
            kafkaParams.put("bootstrap.servers", bootstrapServers);
            kafkaParams.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaParams.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
            kafkaParams.put("group.id", "spark-streaming-group");
            kafkaParams.put("auto.offset.reset", "latest");
            kafkaParams.put("enable.auto.commit", "true");
            
            // 创建Kafka流
            JavaDStream<org.apache.kafka.clients.consumer.ConsumerRecord<String, String>> stream =
                    KafkaUtils.createDirectStream(
                            streamingContext,
                            LocationStrategies.PreferConsistent(),
                            ConsumerStrategies.Subscribe(Collections.singletonList(sensorTopic), kafkaParams)
                    );
            
            // 处理接收到的记录
            JavaDStream<String> values = stream.map(record -> record.value());
            
            // 解析JSON并执行统计分析
            values.foreachRDD(rdd -> {
                if (!rdd.isEmpty()) {
                    try {
                        // 在每个执行器上创建ObjectMapper的本地实例
                        // 在RDD上执行操作，而不是在DStream级别
                        rdd.mapToPair(jsonStr -> {
                            // 在执行器上创建本地ObjectMapper
                            ObjectMapper mapper = new ObjectMapper();
                            JsonNode json = mapper.readTree(jsonStr);
                            String sensorType = json.path("sensorType").asText();
                            double value = json.path("value").asDouble();
                            return new Tuple2<>(sensorType, value);
                        }).groupByKey().collectAsMap().forEach((sensorType, values2) -> {
                            // 计算统计数据
                            List<Double> valuesList = new ArrayList<>();
                            values2.forEach(valuesList::add);
                            
                            if (!valuesList.isEmpty()) {
                                // 计算最小值、最大值、平均值
                                double min = Collections.min(valuesList);
                                double max = Collections.max(valuesList);
                                double sum = 0;
                                double sumSquares = 0;
                                int anomalyCount = 0;
                                
                                // 假设正常范围
                                double minNormal = 0;
                                double maxNormal = 100;
                                if ("temperature".equals(sensorType)) {
                                    minNormal = -10;
                                    maxNormal = 50;
                                } else if ("co2".equals(sensorType)) {
                                    minNormal = 300;
                                    maxNormal = 5000;
                                }
                                
                                for (double v : valuesList) {
                                    sum += v;
                                    sumSquares += v * v;
                                    if (v < minNormal || v > maxNormal) {
                                        anomalyCount++;
                                    }
                                }
                                
                                double avg = sum / valuesList.size();
                                double variance = (sumSquares / valuesList.size()) - (avg * avg);
                                double stdDev = Math.sqrt(variance);
                                
                                // 存储统计结果
                                Map<String, Double> stats = new HashMap<>();
                                stats.put("min", min);
                                stats.put("max", max);
                                stats.put("avg", avg);
                                stats.put("stdDev", stdDev);
                                stats.put("count", (double) valuesList.size());
                                stats.put("anomalyCount", (double) anomalyCount);
                                stats.put("anomalyRate", (double) anomalyCount / valuesList.size());
                                
                                // 在驱动器上处理和发送WebSocket消息，防止序列化问题
                                if (isWebSocketAvailable()) {
                                    try {
                                        Map<String, Map<String, Double>> statsMap = new HashMap<>();
                                        statsMap.put(sensorType, stats);
                                        String jsonStats = objectMapper.writeValueAsString(statsMap);
                                        messagingTemplate.convertAndSend("/topic/spark-stats", jsonStats);
                                        logger.debug("发送Spark统计数据到WebSocket: {}", jsonStats);
                                    } catch (Exception e) {
                                        logger.error("发送WebSocket消息时出错: {}", e.getMessage(), e);
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        logger.error("处理Spark RDD时出错: {}", e.getMessage(), e);
                    }
                }
            });
            
            // 启动Spark Streaming
            start();
            logger.info("Spark Streaming已初始化并启动");
        } catch (Exception e) {
            logger.error("初始化Spark Streaming时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查WebSocket是否可用
     */
    private boolean isWebSocketAvailable() {
        return messagingTemplate != null;
    }
    
    /**
     * 启动Spark Streaming
     */
    public void start() {
        if (running.compareAndSet(false, true)) {
            streamingThread = new Thread(() -> {
                try {
                    streamingContext.start();
                    streamingContext.awaitTermination();
                } catch (Exception e) {
                    logger.error("Spark Streaming运行时出错: {}", e.getMessage(), e);
                }
            });
            
            streamingThread.setDaemon(true);
            streamingThread.start();
        }
    }
    
    /**
     * 关闭Spark Streaming
     */
    @PreDestroy
    public void shutdown() {
        if (running.compareAndSet(true, false)) {
            try {
                if (streamingContext != null) {
                    streamingContext.stop(true, true);
                }
                
                if (sparkSession != null) {
                    sparkSession.close();
                }
                
                if (streamingThread != null) {
                    streamingThread.interrupt();
                }
                
                logger.info("Spark Streaming已关闭");
            } catch (Exception e) {
                logger.error("关闭Spark Streaming时出错: {}", e.getMessage(), e);
            }
        }
    }
} 