package org.shiguang.spark;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.streaming.Trigger;
import org.apache.spark.sql.types.*;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.shiguang.config.AppConfig;
import org.shiguang.spark.sink.WebSocketSinkProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.TimeoutException;

/**
 * 农业传感器数据流处理器
 */
@Component
public class AgricultureSensorProcessor {

    private static final StructType SENSOR_SCHEMA = new StructType()
            .add("sensor_id", DataTypes.StringType)
            .add("timestamp", DataTypes.TimestampType)
            .add("sensor_type", DataTypes.StringType)
            .add("value", DataTypes.DoubleType)
            .add("unit", DataTypes.StringType)
            .add("location", DataTypes.StringType)
            .add("farm_id", DataTypes.StringType)
            .add("crop_type", DataTypes.StringType)
            .add("region", DataTypes.StringType);

    private JavaStreamingContext streamingContext;
    private SparkSession sparkSession;
    private final Map<String, Double> anomalyThresholds = new HashMap<>();
    private boolean isRunning = false;
    
    @Autowired
    private AppConfig config;
    
    @Autowired
    private WebSocketSinkProvider webSocketSinkProvider;

    /**
     * 初始化Spark配置
     */
    public void init() {
        // 加载异常阈值
        loadAnomalyThresholds();
        
        // 配置Spark
        SparkConf sparkConf = new SparkConf()
                .setAppName(config.getString("spark.app.name", "AgricultureSensorProcessor"))
                .setMaster(config.getString("spark.master", "local[2]"))
                .set("spark.serializer", config.getString("spark.serializer", "org.apache.spark.serializer.KryoSerializer"));
        
        int batchDuration = config.getInt("spark.streaming.batch.duration", 5);
        streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));
        
        sparkSession = SparkSession.builder()
                .config(sparkConf)
                .getOrCreate();
    }
    
    /**
     * 加载异常阈值配置
     */
    private void loadAnomalyThresholds() {
        anomalyThresholds.put("temperature", config.getDouble("anomaly.threshold.temperature", 5.0));
        anomalyThresholds.put("humidity", config.getDouble("anomaly.threshold.humidity", 10.0));
        anomalyThresholds.put("soil_moisture", config.getDouble("anomaly.threshold.soil_moisture", 15.0));
        anomalyThresholds.put("light_intensity", config.getDouble("anomaly.threshold.light_intensity", 200.0));
        anomalyThresholds.put("co2", config.getDouble("anomaly.threshold.co2", 100.0));
    }

    /**
     * 启动Kafka流处理
     */
    public synchronized boolean start() {
        if (isRunning) {
            return false;
        }
        
        if (streamingContext == null) {
            init();
        }
        
        // Kafka配置
        Map<String, Object> kafkaParams = new HashMap<>();
        kafkaParams.put("bootstrap.servers", config.getString("kafka.bootstrap.servers", "localhost:9092"));
        kafkaParams.put("key.deserializer", StringDeserializer.class);
        kafkaParams.put("value.deserializer", StringDeserializer.class);
        kafkaParams.put("group.id", config.getString("kafka.group.id", "agriculture-sensor-group"));
        kafkaParams.put("auto.offset.reset", "latest");
        kafkaParams.put("enable.auto.commit", false);

        // 消费的主题
        String topic = config.getString("kafka.topic", "agriculture-sensor-data");
        Collection<String> topics = Collections.singletonList(topic);

        // 创建Kafka直接流
        JavaInputDStream<ConsumerRecord<String, String>> kafkaStream = KafkaUtils.createDirectStream(
                streamingContext,
                LocationStrategies.PreferConsistent(),
                ConsumerStrategies.Subscribe(topics, kafkaParams)
        );

        // 处理流
        kafkaStream.foreachRDD(rdd -> {
            if (!rdd.isEmpty()) {
                // 从消费者记录中提取JSON字符串
                Dataset<String> jsonStrings = sparkSession.createDataset(
                        rdd.map(ConsumerRecord::value).collect(),
                        Encoders.STRING()
                );
                
                // 转换为DataFrame
                Dataset<Row> sensorDataDF = sparkSession.read().json(jsonStrings);

                // 验证模式并处理
                if (validateSchema(sensorDataDF)) {
                    // 检测异常
                    Dataset<Row> processedData = detectAnomalies(sensorDataDF);
                    
                    // 转换为JSON字符串并发送到WebSocket
                    List<String> jsonResults = processedData.toJSON().collectAsList();
                    for (String json : jsonResults) {
                        webSocketSinkProvider.broadcast(json);
                    }
                    
                    // 显示一些结果用于调试
                    System.out.println("处理了 " + processedData.count() + " 条记录");
                    processedData.show(10, false);
                }
            }
        });

        // 启动流上下文
        streamingContext.start();
        isRunning = true;
        
        // 在单独的线程中等待终止
        new Thread(() -> {
            try {
                streamingContext.awaitTermination();
            } catch (InterruptedException e) {
                System.err.println("流上下文被中断: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }).start();
        
        return true;
    }

    /**
     * 验证传入的数据是否符合预期模式
     * @param dataFrame 数据帧
     * @return 如果模式有效则为true
     */
    private boolean validateSchema(Dataset<Row> dataFrame) {
        try {
            // 检查是否存在所有必需的字段
            for (StructField field : SENSOR_SCHEMA.fields()) {
                if (!Arrays.asList(dataFrame.columns()).contains(field.name())) {
                    System.err.println("缺少必需的字段: " + field.name());
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            System.err.println("模式验证错误: " + e.getMessage());
            return false;
        }
    }

    /**
     * 检测传感器数据中的异常
     * @param sensorData 传感器数据帧
     * @return 带有异常检测结果的数据帧
     */
    private Dataset<Row> detectAnomalies(Dataset<Row> sensorData) {
        // 将数据帧注册为SQL临时视图
        sensorData.createOrReplaceTempView("sensor_data");
        
        // 根据传感器类型查找超出范围的值
        String anomalyDetectionSQL = 
                "SELECT *, " +
                "CASE " +
                "  WHEN sensor_type = 'temperature' AND ABS(value) > " + anomalyThresholds.get("temperature") + " THEN true " +
                "  WHEN sensor_type = 'humidity' AND (value < 0 OR value > 100 OR ABS(value - 50) > " + anomalyThresholds.get("humidity") + ") THEN true " +
                "  WHEN sensor_type = 'soil_moisture' AND ABS(value - 30) > " + anomalyThresholds.get("soil_moisture") + " THEN true " +
                "  WHEN sensor_type = 'light_intensity' AND ABS(value - 500) > " + anomalyThresholds.get("light_intensity") + " THEN true " +
                "  WHEN sensor_type = 'co2' AND ABS(value - 400) > " + anomalyThresholds.get("co2") + " THEN true " +
                "  ELSE false " +
                "END AS is_anomaly " +
                "FROM sensor_data";
        
        return sparkSession.sql(anomalyDetectionSQL);
    }

    /**
     * 停止流上下文
     */
    @PreDestroy
    public synchronized void stop() {
        if (streamingContext != null && isRunning) {
            streamingContext.stop(true, true);
            isRunning = false;
        }
    }
    
    /**
     * 检查处理器是否正在运行
     * @return 如果正在运行则为true
     */
    public boolean isRunning() {
        return isRunning;
    }
} 