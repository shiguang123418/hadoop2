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
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    private final Gson gson = new Gson();
    
    // Spark master URL格式验证的正则表达式
    private static final Pattern LOCAL_PATTERN = Pattern.compile("local(\\[.*\\])?");
    private static final Pattern SPARK_PATTERN = Pattern.compile("spark://([^:]+)(:\\d+)?");
    
    @Autowired
    private AppConfig config;
    
    @Autowired
    private WebSocketSinkProvider webSocketSinkProvider;

    /**
     * 验证Spark master URL格式
     * @param master Spark master URL
     * @return 验证后的合法master URL
     */
    private String validateMasterUrl(String master) {
        if (master == null || master.trim().isEmpty()) {
            System.out.println("Spark master URL为空，使用默认值local[2]");
            return "local[2]";
        }
        
        // 如果是IP地址或主机名但没有协议前缀，添加spark://协议
        if (master.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || 
            (!master.contains("://") && !LOCAL_PATTERN.matcher(master).matches())) {
            String url = "spark://" + master + ":7077";
            System.out.println("转换Spark master URL从 " + master + " 到 " + url);
            return url;
        }
        
        // 检查是否是有效的local模式
        if (LOCAL_PATTERN.matcher(master).matches()) {
            return master;
        }
        
        // 检查是否是有效的spark://模式
        if (SPARK_PATTERN.matcher(master).matches()) {
            return master;
        }
        
        // 检查是否是有效的yarn模式
        if ("yarn".equals(master)) {
            return master;
        }
        
        // 检查是否是有效的kubernetes模式
        if (master.startsWith("k8s://")) {
            return master;
        }
        
        // 未知格式，使用默认值
        System.out.println("无效的Spark master URL: " + master + "，使用默认值local[2]");
        return "local[2]";
    }

    /**
     * 初始化Spark配置
     */
    public void init() {
        // 设置当前线程的类加载器，解决Spark类加载问题
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        
        // 加载异常阈值
        loadAnomalyThresholds();
        
        // 获取并验证Spark master URL
        String rawMasterUrl = config.getString("spark.master", "local[2]");
        String validMasterUrl = validateMasterUrl(rawMasterUrl);
        
        // 配置Spark
        SparkConf sparkConf = new SparkConf()
                .setAppName(config.getString("spark.app.name", "AgricultureSensorProcessor"))
                .setMaster(validMasterUrl)
                .set("spark.serializer", config.getString("spark.serializer", "org.apache.spark.serializer.KryoSerializer"))
                // 添加配置解决类加载问题
                .set("spark.driver.userClassPathFirst", "true")
                .set("spark.executor.userClassPathFirst", "true");
                
        
        int batchDuration = config.getInt("spark.streaming.batch.duration", 5);
        streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));
        
        // 创建SparkSession，但暂时不使用SQL功能
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

        // 处理流 - 使用传统的RDD API而不是DataFrame/SQL API
        kafkaStream.foreachRDD(rdd -> {
            if (!rdd.isEmpty()) {
                // 收集JSON字符串
                List<String> jsonStrings = rdd.map(ConsumerRecord::value).collect();
                List<Map<String, Object>> processedData = new ArrayList<>();
                
                // 手动解析和处理JSON
                for (String json : jsonStrings) {
                    try {
                        // 解析JSON
                        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
                        
                        // 手动验证和提取字段
                        if (validateJsonFields(jsonObject)) {
                            Map<String, Object> sensorData = new HashMap<>();
                            
                            // 提取基本字段
                            String sensorId = jsonObject.get("sensor_id").getAsString();
                            String sensorType = jsonObject.get("sensor_type").getAsString();
                            double value = jsonObject.get("value").getAsDouble();
                            String timestamp = jsonObject.get("timestamp").getAsString();
                            String unit = jsonObject.has("unit") ? jsonObject.get("unit").getAsString() : "";
                            String location = jsonObject.has("location") ? jsonObject.get("location").getAsString() : "";
                            String farmId = jsonObject.has("farm_id") ? jsonObject.get("farm_id").getAsString() : "";
                            String cropType = jsonObject.has("crop_type") ? jsonObject.get("crop_type").getAsString() : "";
                            String region = jsonObject.has("region") ? jsonObject.get("region").getAsString() : "";
                            
                            // 复制所有字段
                            sensorData.put("sensor_id", sensorId);
                            sensorData.put("sensor_type", sensorType);
                            sensorData.put("value", value);
                            sensorData.put("timestamp", timestamp);
                            sensorData.put("unit", unit);
                            sensorData.put("location", location);
                            sensorData.put("farm_id", farmId);
                            sensorData.put("crop_type", cropType);
                            sensorData.put("region", region);
                            
                            // 检测异常
                            boolean isAnomaly = detectAnomaly(sensorType, value);
                            sensorData.put("is_anomaly", isAnomaly);
                            
                            processedData.add(sensorData);
                        }
                    } catch (Exception e) {
                        System.err.println("解析JSON出错: " + e.getMessage() + " for JSON: " + json);
                    }
                }
                
                // 发送处理后的数据到WebSocket
                for (Map<String, Object> data : processedData) {
                    String jsonResult = gson.toJson(data);
                    webSocketSinkProvider.broadcast(jsonResult);
                }
                
                System.out.println("处理了 " + processedData.size() + " 条记录");
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
     * 验证JSON字段是否包含所需的所有字段
     * @param jsonObject JSON对象
     * @return 如果包含所有必需字段则为true
     */
    private boolean validateJsonFields(JsonObject jsonObject) {
        String[] requiredFields = {"sensor_id", "sensor_type", "value", "timestamp"};
        for (String field : requiredFields) {
            if (!jsonObject.has(field)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检测传感器数据是否异常
     * @param sensorType 传感器类型
     * @param value 传感器值
     * @return 如果是异常值则为true
     */
    private boolean detectAnomaly(String sensorType, double value) {
        switch (sensorType) {
            case "temperature":
                return Math.abs(value) > anomalyThresholds.get("temperature");
            case "humidity":
                return value < 0 || value > 100 || Math.abs(value - 50) > anomalyThresholds.get("humidity");
            case "soil_moisture":
                return Math.abs(value - 30) > anomalyThresholds.get("soil_moisture");
            case "light_intensity":
                return Math.abs(value - 500) > anomalyThresholds.get("light_intensity");
            case "co2":
                return Math.abs(value - 400) > anomalyThresholds.get("co2");
            default:
                return false;
        }
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