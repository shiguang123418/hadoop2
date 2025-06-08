package org.shiguang.module.sensor.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.shiguang.config.SparkContextManager;
import org.shiguang.module.sensor.config.SensorConfig;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.time.Instant;
import java.util.stream.Collectors;
import org.apache.spark.streaming.Duration;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/**
 * Spark流处理服务
 * 使用Spark Streaming处理从Kafka接收的数据并计算统计信息
 */
@Service
public class SensorStreamAnalysisService implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(SensorStreamAnalysisService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 用于存储最近的数据点，用于时间窗口分析
    private final Map<String, List<TimestampedValue>> recentDataPoints = new ConcurrentHashMap<>();
    // 存储最近的统计数据，用于趋势分析
    private final Map<String, List<TimestampedStat>> recentStats = new ConcurrentHashMap<>();
    // 数据保留时间（毫秒）
    private static final long DATA_RETENTION_MS = 15 * 60 * 1000; // 15分钟
    // 时间窗口定义（毫秒）
    private static final long[] TIME_WINDOWS = {60 * 1000, 5 * 60 * 1000, 15 * 60 * 1000}; // 1分钟, 5分钟, 15分钟

    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;

    @Autowired
    private transient SensorConfig sensorConfig;

    @Autowired
    private transient SimpMessagingTemplate messagingTemplate;

    @Autowired
    private transient SparkContextManager sparkContextManager;

    private transient JavaStreamingContext streamingContext;
    private transient Thread streamingThread;
    private transient AtomicBoolean running = new AtomicBoolean(false);

    /**
     * 初始化并启动Spark Streaming
     */
    @PostConstruct
    public void init() {
        if (sensorConfig.isEnabled()) {
            initSparkStreaming();
        } else {
            logger.info("传感器 Spark Streaming 已禁用");
        }
    }

    /**
     * 初始化Spark Streaming
     */
    private void initSparkStreaming() {
        try {
            // 检查SparkContextManager是否已初始化
            if (!sparkContextManager.isInitialized()) {
                logger.error("SparkContextManager未初始化，无法创建Spark流处理");
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
            kafkaParams.put("group.id", "spark-streaming-group");
            kafkaParams.put("auto.offset.reset", "latest");
            kafkaParams.put("enable.auto.commit", "true");

            // 使用配置中的Kafka主题
            String kafkaTopic = sensorConfig.getKafkaTopic();

            // 创建Kafka流
            JavaDStream<org.apache.kafka.clients.consumer.ConsumerRecord<String, String>> stream =
                    KafkaUtils.createDirectStream(
                            streamingContext,
                            LocationStrategies.PreferConsistent(),
                            ConsumerStrategies.Subscribe(Collections.singletonList(kafkaTopic), kafkaParams)
                    );

            // 处理接收到的记录
            JavaDStream<String> values = stream.map(record -> record.value());

            // 解析JSON并执行统计分析
            values.foreachRDD(rdd -> {
                try {
                    // 在每个执行器上创建ObjectMapper的本地实例
                    // 在RDD上执行操作，而不是在DStream级别
                    // 解析JSON并转换为带有时间戳的数据
                    JavaPairRDD<String, TimestampedValue> timestampedData = rdd.mapToPair(jsonStr -> {
                        // 在执行器上创建本地ObjectMapper
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode json = mapper.readTree(jsonStr);
                        String sensorType = json.path("sensorType").asText();
                        double value = json.path("value").asDouble();
                        String sensorId = json.path("sensorId").asText("");
                        String location = json.path("location").asText("");
                        long timestamp = json.path("timestamp").asLong(System.currentTimeMillis());
                        
                        // 数据过滤 - 排除明显异常值
                        if (isValidSensorValue(sensorType, value)) {
                            TimestampedValue tsValue = new TimestampedValue(value, timestamp, sensorId, location);
                            return new Tuple2<>(sensorType, tsValue);
                        } else {
                            logger.warn("检测到异常数据: 类型={}, 值={}", sensorType, value);
                            return new Tuple2<>(sensorType + "_invalid", new TimestampedValue(value, timestamp, sensorId, location));
                        }
                    }).filter(tuple -> !tuple._1.endsWith("_invalid"));
                    
                    // 收集并保存最新数据点用于时间窗口分析
                    Map<String, Iterable<TimestampedValue>> newDataPoints = timestampedData.groupByKey().collectAsMap();
                    updateRecentDataPoints(newDataPoints);
                    
                    // 按传感器类型分组进行标准统计
                    timestampedData.groupByKey().collectAsMap().forEach((sensorType, values2) -> {
                        // 计算基本统计数据
                        List<TimestampedValue> valuesList = new ArrayList<>();
                        values2.forEach(valuesList::add);

                        if (!valuesList.isEmpty()) {
                            // 基本统计计算
                            Map<String, Double> stats = calculateBasicStats(sensorType, valuesList);
                            
                            // 数据分类
                            Map<String, Integer> classifications = classifySensorData(sensorType, valuesList);
                            stats.putAll(convertMapToDoubleValues(classifications));
                            
                            // 趋势分析
                            Map<String, Double> trends = analyzeTrends(sensorType, stats);
                            stats.putAll(trends);
                            
                            // 异常检测增强
                            Map<String, Double> anomalyStats = enhancedAnomalyDetection(sensorType, valuesList);
                            stats.putAll(anomalyStats);
                            
                            // 时间窗口统计
                            Map<String, Map<String, Double>> windowedStats = calculateTimeWindowStats(sensorType);
                            
                            // 准备完整的数据包
                            Map<String, Object> completeStats = new HashMap<>();
                            completeStats.put("currentStats", stats);
                            completeStats.put("windowedStats", windowedStats);
                            
                            // 记录本次统计结果用于趋势分析
                            updateRecentStats(sensorType, stats);
                            
                            // 相关性分析（如果有足够的数据）
                            if (recentDataPoints.size() > 1) {
                                Map<String, Double> correlations = calculateCorrelations(sensorType);
                                completeStats.put("correlations", correlations);
                            }

                            // 在驱动器上处理和发送WebSocket消息，防止序列化问题
                            if (isWebSocketAvailable()) {
                                try {
                                    Map<String, Map<String, Object>> statsMap = new HashMap<>();
                                    statsMap.put(sensorType, completeStats);
                                    String jsonStats = objectMapper.writeValueAsString(statsMap);
                                    messagingTemplate.convertAndSend("/topic/spark-stats", jsonStats);
                                    logger.debug("发送Spark统计数据到WebSocket: {}", jsonStats);
                                } catch (Exception e) {
                                    logger.error("发送WebSocket消息时出错: {}", e.getMessage(), e);
                                }
                            }
                        }
                    });
                    
                    // 清理过时数据
                    cleanupOldData();
                    
                } catch (Exception e) {
                    logger.error("处理Spark RDD时出错: {}", e.getMessage(), e);
                }
            });

            // 不在这里启动Spark Streaming，由SparkContextManager统一管理
            // 设置运行状态为true
            running.set(true);
            logger.info("Spark Streaming传感器数据处理已初始化");
        } catch (Exception e) {
            logger.error("初始化Spark Streaming时出错: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 计算基本统计数据
     */
    private Map<String, Double> calculateBasicStats(String sensorType, List<TimestampedValue> valuesList) {
        // 提取原始值列表
        List<Double> rawValues = valuesList.stream()
                .map(TimestampedValue::getValue)
                .collect(Collectors.toList());
                
        double min = Collections.min(rawValues);
        double max = Collections.max(rawValues);
        double sum = 0;
        double sumSquares = 0;
        int anomalyCount = 0;

        // 获取正常范围
        double[] normalRange = getNormalRange(sensorType);
        double minNormal = normalRange[0];
        double maxNormal = normalRange[1];

        for (double v : rawValues) {
            sum += v;
            sumSquares += v * v;
            if (v < minNormal || v > maxNormal) {
                anomalyCount++;
            }
        }

        double avg = sum / rawValues.size();
        double variance = (sumSquares / rawValues.size()) - (avg * avg);
        double stdDev = Math.sqrt(variance);

        // 存储统计结果
        Map<String, Double> stats = new HashMap<>();
        stats.put("min", min);
        stats.put("max", max);
        stats.put("avg", avg);
        stats.put("stdDev", stdDev);
        stats.put("count", (double) rawValues.size());
        stats.put("anomalyCount", (double) anomalyCount);
        stats.put("anomalyRate", rawValues.isEmpty() ? 0.0 : (double) anomalyCount / rawValues.size());
        
        return stats;
    }
    
    /**
     * 获取传感器数据的正常范围
     */
    private double[] getNormalRange(String sensorType) {
        double minNormal = 0;
        double maxNormal = 100;
        
        if ("temperature".equals(sensorType)) {
            minNormal = -10;
            maxNormal = 50;
        } else if ("humidity".equals(sensorType)) {
            minNormal = 0;
            maxNormal = 100;
        } else if ("pressure".equals(sensorType)) {
            minNormal = 800;
            maxNormal = 1100;
        } else if ("co2".equals(sensorType)) {
            minNormal = 300;
            maxNormal = 5000;
        } else if ("light".equals(sensorType)) {
            minNormal = 0;
            maxNormal = 10000;
        } else if ("soil_moisture".equals(sensorType)) {
            minNormal = 0;
            maxNormal = 100;
        }
        
        return new double[]{minNormal, maxNormal};
    }
    
    /**
     * 判断传感器值是否在有效范围内
     */
    private boolean isValidSensorValue(String sensorType, double value) {
        // 传感器数据的有效范围（比正常范围更宽松一些）
        if ("temperature".equals(sensorType)) {
            return value >= -50 && value <= 100;
        } else if ("humidity".equals(sensorType)) {
            return value >= 0 && value <= 100;
        } else if ("pressure".equals(sensorType)) {
            return value >= 500 && value <= 1500;
        } else if ("co2".equals(sensorType)) {
            return value >= 0 && value <= 10000;
        } else if ("light".equals(sensorType)) {
            return value >= 0 && value <= 100000;
        } else if ("soil_moisture".equals(sensorType)) {
            return value >= 0 && value <= 100;
        }
        
        // 默认情况下，只要不是NaN或无穷大就接受
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
    
    /**
     * 更新最近的数据点
     */
    private synchronized void updateRecentDataPoints(Map<String, Iterable<TimestampedValue>> newDataPoints) {
        newDataPoints.forEach((sensorType, values) -> {
            List<TimestampedValue> sensorData = recentDataPoints.getOrDefault(sensorType, new ArrayList<>());
            values.forEach(sensorData::add);
            recentDataPoints.put(sensorType, sensorData);
        });
    }
    
    /**
     * 更新最近的统计数据
     */
    private synchronized void updateRecentStats(String sensorType, Map<String, Double> stats) {
        List<TimestampedStat> sensorStats = recentStats.getOrDefault(sensorType, new ArrayList<>());
        sensorStats.add(new TimestampedStat(stats, System.currentTimeMillis()));
        recentStats.put(sensorType, sensorStats);
    }
    
    /**
     * 清理过时数据
     */
    private synchronized void cleanupOldData() {
        long cutoffTime = System.currentTimeMillis() - DATA_RETENTION_MS;
        
        // 清理数据点
        recentDataPoints.forEach((sensorType, values) -> {
            List<TimestampedValue> filteredValues = values.stream()
                    .filter(v -> v.getTimestamp() > cutoffTime)
                    .collect(Collectors.toList());
            recentDataPoints.put(sensorType, filteredValues);
        });
        
        // 清理统计数据
        recentStats.forEach((sensorType, values) -> {
            List<TimestampedStat> filteredValues = values.stream()
                    .filter(v -> v.getTimestamp() > cutoffTime)
                    .collect(Collectors.toList());
            recentStats.put(sensorType, filteredValues);
        });
    }
    
    /**
     * 计算时间窗口统计数据
     */
    private Map<String, Map<String, Double>> calculateTimeWindowStats(String sensorType) {
        Map<String, Map<String, Double>> result = new HashMap<>();
        List<TimestampedValue> values = recentDataPoints.getOrDefault(sensorType, new ArrayList<>());
        
        if (values.isEmpty()) {
            return result;
        }
        
        long currentTime = System.currentTimeMillis();
        
        // 为每个时间窗口计算统计数据
        for (long windowMs : TIME_WINDOWS) {
            long windowStart = currentTime - windowMs;
            List<TimestampedValue> windowValues = values.stream()
                    .filter(v -> v.getTimestamp() >= windowStart)
                    .collect(Collectors.toList());
            
            if (!windowValues.isEmpty()) {
                Map<String, Double> windowStats = calculateBasicStats(sensorType, windowValues);
                result.put(formatDuration(windowMs), windowStats);
            }
        }
        
        return result;
    }
    
    /**
     * 格式化持续时间
     */
    private String formatDuration(long ms) {
        if (ms < 60 * 1000) {
            return ms / 1000 + "s";
        } else if (ms < 60 * 60 * 1000) {
            return ms / (60 * 1000) + "m";
        } else {
            return ms / (60 * 60 * 1000) + "h";
        }
    }
    
    /**
     * 数据分类
     */
    private Map<String, Integer> classifySensorData(String sensorType, List<TimestampedValue> values) {
        Map<String, Integer> classification = new HashMap<>();
        classification.put("low", 0);
        classification.put("normal", 0);
        classification.put("high", 0);
        classification.put("critical", 0);
        
        double[] ranges = getClassificationRanges(sensorType);
        double lowThreshold = ranges[0];
        double highThreshold = ranges[1];
        double criticalThreshold = ranges[2];
        
        for (TimestampedValue value : values) {
            double v = value.getValue();
            if (v <= lowThreshold) {
                classification.put("low", classification.get("low") + 1);
            } else if (v <= highThreshold) {
                classification.put("normal", classification.get("normal") + 1);
            } else if (v <= criticalThreshold) {
                classification.put("high", classification.get("high") + 1);
            } else {
                classification.put("critical", classification.get("critical") + 1);
            }
        }
        
        return classification;
    }
    
    /**
     * 获取分类阈值
     */
    private double[] getClassificationRanges(String sensorType) {
        if ("temperature".equals(sensorType)) {
            return new double[]{10.0, 30.0, 40.0}; // 低、高、危险阈值
        } else if ("humidity".equals(sensorType)) {
            return new double[]{30.0, 70.0, 90.0};
        } else if ("pressure".equals(sensorType)) {
            return new double[]{900.0, 1000.0, 1050.0};
        } else if ("co2".equals(sensorType)) {
            return new double[]{800.0, 1200.0, 2000.0};
        } else if ("light".equals(sensorType)) {
            return new double[]{100.0, 5000.0, 8000.0};
        } else if ("soil_moisture".equals(sensorType)) {
            return new double[]{20.0, 60.0, 80.0};
        }
        
        // 默认值
        return new double[]{25.0, 75.0, 90.0};
    }
    
    /**
     * 趋势分析
     */
    private Map<String, Double> analyzeTrends(String sensorType, Map<String, Double> currentStats) {
        Map<String, Double> trends = new HashMap<>();
        trends.put("trend", 0.0); // 默认：无变化
        
        List<TimestampedStat> recentStatsList = recentStats.getOrDefault(sensorType, new ArrayList<>());
        if (recentStatsList.size() < 2) {
            return trends;
        }
        
        // 计算平均值的趋势
        List<Double> averages = recentStatsList.stream()
                .map(stat -> stat.getStats().get("avg"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (averages.size() >= 3) {
            // 简单线性回归计算趋势斜率
            double slope = calculateLinearTrendSlope(averages);
            trends.put("trend", slope);
            
            // 归一化趋势值到[-1, 1]区间
            double normalizedTrend = normalizeTrend(sensorType, slope);
            trends.put("normalizedTrend", normalizedTrend);
            
            // 预测下一个值
            double prediction = averages.get(averages.size() - 1) + slope;
            trends.put("prediction", prediction);
        }
        
        return trends;
    }
    
    /**
     * 计算线性趋势斜率
     */
    private double calculateLinearTrendSlope(List<Double> values) {
        int n = values.size();
        if (n < 2) return 0.0;
        
        // 简化版线性回归斜率计算
        double sumX = 0;
        double sumY = 0;
        double sumXY = 0;
        double sumXX = 0;
        
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = values.get(i);
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumXX += x * x;
        }
        
        return (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    }
    
    /**
     * 归一化趋势值
     */
    private double normalizeTrend(String sensorType, double slope) {
        // 为不同的传感器类型使用不同的缩放因子
        double scaleFactor = 1.0;
        
        if ("temperature".equals(sensorType)) {
            scaleFactor = 10.0;  // 假设每单位时间10度为极端变化
        } else if ("humidity".equals(sensorType)) {
            scaleFactor = 20.0;
        } else if ("pressure".equals(sensorType)) {
            scaleFactor = 50.0;
        } else if ("co2".equals(sensorType)) {
            scaleFactor = 500.0;
        }
        
        // 将斜率缩放到[-1,1]范围
        double normalized = slope / scaleFactor;
        return Math.max(-1.0, Math.min(1.0, normalized)); // 限制在[-1,1]范围内
    }
    
    /**
     * 增强的异常检测
     */
    private Map<String, Double> enhancedAnomalyDetection(String sensorType, List<TimestampedValue> values) {
        Map<String, Double> anomalyStats = new HashMap<>();
        
        if (values.isEmpty()) {
            return anomalyStats;
        }
        
        // 提取原始值
        List<Double> rawValues = values.stream()
                .map(TimestampedValue::getValue)
                .collect(Collectors.toList());
        
        // 计算Z-score以检测异常
        double mean = rawValues.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = Math.sqrt(rawValues.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average().orElse(0.0));
        
        // 避免除以零
        if (stdDev < 0.0001) stdDev = 0.0001;
        
        // 使用Z-score > 2.0作为异常检测阈值
        int zscoreAnomalies = 0;
        for (Double value : rawValues) {
            double zscore = Math.abs((value - mean) / stdDev);
            if (zscore > 2.0) {
                zscoreAnomalies++;
            }
        }
        
        anomalyStats.put("zscoreAnomalyCount", (double) zscoreAnomalies);
        anomalyStats.put("zscoreAnomalyRate", (double) zscoreAnomalies / rawValues.size());
        
        // 检测异常趋势 - 连续三个点上升或下降
        int trendAnomalies = 0;
        if (rawValues.size() >= 3) {
            for (int i = 0; i < rawValues.size() - 2; i++) {
                // 检测连续三个点严格上升或严格下降
                if ((rawValues.get(i) < rawValues.get(i+1) && rawValues.get(i+1) < rawValues.get(i+2)) ||
                    (rawValues.get(i) > rawValues.get(i+1) && rawValues.get(i+1) > rawValues.get(i+2))) {
                    trendAnomalies++;
                }
            }
        }
        
        anomalyStats.put("trendAnomalyCount", (double) trendAnomalies);
        
        return anomalyStats;
    }
    
    /**
     * 计算传感器之间的相关性
     */
    private Map<String, Double> calculateCorrelations(String targetSensorType) {
        Map<String, Double> correlations = new HashMap<>();
        
        // 确保目标传感器类型存在数据
        if (!recentDataPoints.containsKey(targetSensorType) || 
            recentDataPoints.get(targetSensorType).isEmpty()) {
            return correlations;
        }
        
        // 获取目标传感器最近的时间戳
        List<Long> targetTimestamps = recentDataPoints.get(targetSensorType).stream()
                .map(TimestampedValue::getTimestamp)
                .sorted()
                .collect(Collectors.toList());
        
        // 为目标传感器创建时间戳-值映射
        Map<Long, Double> targetTimestampToValue = new HashMap<>();
        for (TimestampedValue tv : recentDataPoints.get(targetSensorType)) {
            targetTimestampToValue.put(tv.getTimestamp(), tv.getValue());
        }
        
        // 对每个其他传感器类型计算相关性
        for (String otherSensorType : recentDataPoints.keySet()) {
            if (otherSensorType.equals(targetSensorType)) continue;
            
            // 创建要比较的数据集
            List<Double> targetValues = new ArrayList<>();
            List<Double> otherValues = new ArrayList<>();
            
            // 对齐时间戳 (简化版 - 使用最近的时间戳)
            Map<Long, Double> otherTimestampToValue = new HashMap<>();
            for (TimestampedValue tv : recentDataPoints.get(otherSensorType)) {
                otherTimestampToValue.put(tv.getTimestamp(), tv.getValue());
            }
            
            // 仅使用匹配相近时间戳的值对
            for (Long timestamp : targetTimestamps) {
                if (targetTimestampToValue.containsKey(timestamp) && otherTimestampToValue.containsKey(timestamp)) {
                    targetValues.add(targetTimestampToValue.get(timestamp));
                    otherValues.add(otherTimestampToValue.get(timestamp));
                }
            }
            
            // 确保有足够的数据点计算相关性（至少4个点）
            if (targetValues.size() >= 4) {
                try {
                    double[] xArray = targetValues.stream().mapToDouble(Double::doubleValue).toArray();
                    double[] yArray = otherValues.stream().mapToDouble(Double::doubleValue).toArray();
                    
                    // 计算皮尔逊相关系数
                    PearsonsCorrelation correlationCalculator = new PearsonsCorrelation();
                    double correlation = correlationCalculator.correlation(xArray, yArray);
                    
                    // 检查结果是否有效
                    if (!Double.isNaN(correlation) && !Double.isInfinite(correlation)) {
                        correlations.put(otherSensorType, correlation);
                    }
                } catch (Exception e) {
                    logger.warn("计算 {} 和 {} 的相关性时出错: {}", 
                            targetSensorType, otherSensorType, e.getMessage());
                }
            }
        }
        
        return correlations;
    }

    /**
     * 将Map<String, Integer>转换为Map<String, Double>
     */
    private Map<String, Double> convertMapToDoubleValues(Map<String, Integer> intMap) {
        return intMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> (double) e.getValue()
                ));
    }

    /**
     * 检查WebSocket是否可用
     */
    private boolean isWebSocketAvailable() {
        return messagingTemplate != null;
    }
    
    /**
     * 关闭Spark Streaming
     */
    @PreDestroy
    public void shutdown() {
        // 不再需要关闭，由SparkContextManager统一管理
        running.set(false);
        logger.info("Spark Streaming传感器数据处理已关闭");
    }
    
    /**
     * 带时间戳的传感器值类
     */
    public static class TimestampedValue implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final double value;
        private final long timestamp;
        private final String sensorId;
        private final String location;
        
        public TimestampedValue(double value, long timestamp, String sensorId, String location) {
            this.value = value;
            this.timestamp = timestamp;
            this.sensorId = sensorId;
            this.location = location;
        }
        
        public double getValue() {
            return value;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public String getSensorId() {
            return sensorId;
        }
        
        public String getLocation() {
            return location;
        }
    }
    
    /**
     * 带时间戳的统计数据类
     */
    public static class TimestampedStat implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final Map<String, Double> stats;
        private final long timestamp;
        
        public TimestampedStat(Map<String, Double> stats, long timestamp) {
            this.stats = new HashMap<>(stats);
            this.timestamp = timestamp;
        }
        
        public Map<String, Double> getStats() {
            return stats;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
    }
} 