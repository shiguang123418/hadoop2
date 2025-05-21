package org.shiguang.spark;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.broadcast.Broadcast;
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
import org.shiguang.spark.anomaly.AnomalyDetector;
import org.shiguang.spark.processor.SensorProcessor;
import org.shiguang.spark.sink.WebSocketSinkProvider;
import org.shiguang.util.KafkaSparkClassResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 农业传感器数据流处理器实现类（已弃用，请使用SparkJobRunner）
 * <p>
 * 该类使用Spark Streaming从Kafka消费传感器数据，进行处理和异常检测，
 * 然后通过WebSocket将结果推送给前端客户端。
 * </p>
 * @deprecated 由于Java 8 Lambda表达式在Spark中序列化问题，已被弃用。请使用SparkJobRunner类代替。
 */
@Deprecated
@ConditionalOnProperty(name = "app.use-legacy-processor", havingValue = "true", matchIfMissing = false)
@Component
public class AgricultureSensorProcessor implements SensorProcessor, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(AgricultureSensorProcessor.class);
    
    // 单例实例，用于在RDD处理过程中获取处理器
    private static AgricultureSensorProcessor instance;
    
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

    private transient JavaStreamingContext streamingContext;
    private transient SparkSession sparkSession;
    private boolean isRunning = false;
    private transient final Gson gson = new Gson();
    
    // Spark master URL格式验证的正则表达式
    private static final Pattern LOCAL_PATTERN = Pattern.compile("local(\\[.*\\])?");
    private static final Pattern SPARK_PATTERN = Pattern.compile("spark://([^:]+)(:\\d+)?");
    
    // 添加广播变量
    private transient Broadcast<AnomalyDetector> broadcastDetector;
    private transient Broadcast<WebSocketSinkProvider> broadcastWebSocketProvider;
    
    @Autowired
    private transient AppConfig config;
    
    @Autowired
    private transient WebSocketSinkProvider webSocketSinkProvider;
    
    @Autowired
    private transient AnomalyDetector anomalyDetector;
    
    /**
     * 构造函数，设置实例引用
     */
    public AgricultureSensorProcessor() {
        instance = this;
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized AgricultureSensorProcessor getInstance() {
        return instance;
    }

    /**
     * 验证Spark master URL格式
     * @param master Spark master URL
     * @return 验证后的合法master URL
     */
    private String validateMasterUrl(String master) {
        if (master == null || master.trim().isEmpty()) {
            logger.info("Spark master URL为空，使用默认值local[2]");
            return "local[2]";
        }
        
        // 如果是IP地址或主机名但没有协议前缀，添加spark://协议
        if (master.matches("\\d+\\.\\d+\\.\\d+\\.\\d+") || 
            (!master.contains("://") && !LOCAL_PATTERN.matcher(master).matches())) {
            String url = "spark://" + master + ":7077";
            logger.info("转换Spark master URL从 {} 到 {}", master, url);
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
        logger.warn("无效的Spark master URL: {}，使用默认值local[*]", master);
        return "local[*]";
    }

    /**
     * 初始化Spark配置
     */
    @Override
    public void init() {
        logger.info("初始化Spark处理器...");
        
        // 使用类加载器解析器设置当前线程的类加载器
        KafkaSparkClassResolver.setContextClassLoader();
        
        // 获取并验证Spark master URL
        String rawMasterUrl = config.getString("spark.master", "local[*]");
        String validMasterUrl = validateMasterUrl(rawMasterUrl);

        // 获取当前应用的 JAR 文件路径
        File jarFile = new ApplicationHome().getSource();
        String appJarPath = (jarFile != null && jarFile.exists()) ? jarFile.getAbsolutePath() : "";
        if (appJarPath.isEmpty()) {
            logger.warn("无法确定应用程序 JAR 文件的路径，尝试其他方法获取。");
            // 尝试获取类路径中的JAR文件
            URL jarUrl = this.getClass().getProtectionDomain().getCodeSource().getLocation();
            if (jarUrl != null) {
                try {
                    appJarPath = new File(jarUrl.toURI()).getAbsolutePath();
                    logger.info("通过类加载器获取JAR路径: {}", appJarPath);
                } catch (Exception e) {
                    logger.warn("无法从类加载器获取JAR路径", e);
                }
            }
        } else {
            logger.info("应用程序 JAR 文件路径: {}", appJarPath);
        }
        
        // 配置Spark
        SparkConf sparkConf = new SparkConf()
                .setAppName(config.getString("spark.app.name", "AgricultureSensorProcessor"))
                .setMaster(validMasterUrl);
                
        // 设置序列化配置
        Map<String, String> serializationConfig = KafkaSparkClassResolver.getSerializationConfig(new HashMap<>());
        for (Map.Entry<String, String> entry : serializationConfig.entrySet()) {
            sparkConf.set(entry.getKey(), entry.getValue());
        }
        
        // 检查是否使用Kryo序列化器
        boolean useKryo = config.getBoolean("spark.kryo.enabled", true);
        if (useKryo) {
            logger.info("使用Kryo序列化器进行高性能序列化");
            
            // 设置Kryo序列化器
            sparkConf.set("spark.serializer", "org.apache.spark.serializer.KryoSerializer");
            
            // 注册自定义Kryo注册器
            String registrator = config.getString("spark.kryo.registrator", "org.shiguang.spark.KafkaKryoRegistrator");
            sparkConf.set("spark.kryo.registrator", registrator);
            
            // Kryo设置
            sparkConf.set("spark.kryo.registrationRequired", "false");  // 不要求所有类都注册
            sparkConf.set("spark.kryoserializer.buffer", "32k");  // 初始缓冲区大小
            
            // 获取配置的最大缓冲区大小
            int kryoBufferMaxSize = config.getInt("spark.kryoserializer.buffer.max.mb", 64);
            sparkConf.set("spark.kryoserializer.buffer.max", kryoBufferMaxSize + "m");
            
            // 注册关键Java 8类
            sparkConf.registerKryoClasses(new Class<?>[] {
                java.util.function.Function.class,
                java.util.function.Predicate.class,
                java.util.function.Consumer.class,
                java.lang.invoke.SerializedLambda.class
            });
            
            logger.info("Kryo序列化器配置完成，缓冲区最大大小: {}MB", kryoBufferMaxSize);
        } else {
            // 强制使用Java序列化器，不使用Kryo
            logger.info("使用Java默认序列化器");
            sparkConf.set("spark.serializer", "org.apache.spark.serializer.JavaSerializer");
            // 设置序列化最大缓冲区
            sparkConf.set("spark.serializer.objectStreamReset", "100");
        }

        // 设置应用程序 JAR 文件路径和其依赖项
        if (!appJarPath.isEmpty()) {
            sparkConf.set("spark.jars", appJarPath);
            sparkConf.set("spark.submit.deployMode", config.getString("spark.deploy-mode", "client"));
            // 确保依赖正确打包
            sparkConf.set("spark.driver.extraClassPath", appJarPath);
            sparkConf.set("spark.executor.extraClassPath", appJarPath);
            // 添加类加载策略
            sparkConf.set("spark.executor.extraJavaOptions", "-Dfile.encoding=UTF-8");
            sparkConf.set("spark.driver.extraJavaOptions", "-Dfile.encoding=UTF-8");
        } else {
            logger.warn("无法确定JAR路径，可能会导致类加载问题");
        }
                
        // 设置额外的Java选项
        sparkConf.set("spark.driver.extraJavaOptions", config.getString("spark.driver.extraJavaOptions", "-Duser.timezone=GMT+8 -Dscala.usejavacp=true"))
                 // 禁用推测性执行
                 .set("spark.speculation", "false");
        
        // 设置更严格的序列化策略
        sparkConf.set("spark.closure.serialization", "true");
        
        // Java设置
        System.setProperty("java.io.serialFilter", "!java.lang.invoke.*;!sun.invoke.*;");
        
        // 禁用Lambda表达式的序列化优化
        System.setProperty("spark.sql.legacy.allowUntypedScalaUDF", "true");
        System.setProperty("spark.databricks.delta.optimizeWrite.enabled", "false");
        System.setProperty("spark.sql.adaptive.enabled", "false");
        
        // 禁用Java Lambda序列化支持
        System.setProperty("scala.usejavacp", "true");
        System.setProperty("spark.executor.userClassPathFirst", "true");
        System.setProperty("spark.driver.userClassPathFirst", "true");
                
        int batchDuration = config.getInt("spark.streaming.batch.duration", 5);
        streamingContext = new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));
        
        // 创建SparkSession，但暂时不使用SQL功能
        sparkSession = SparkSession.builder()
                .config(sparkConf)
                .getOrCreate();
                
        // 创建广播变量
        broadcastDetector = streamingContext.sparkContext().broadcast(anomalyDetector);
        broadcastWebSocketProvider = streamingContext.sparkContext().broadcast(webSocketSinkProvider);
                
        logger.info("Spark处理器初始化完成");
    }

    /**
     * 启动Kafka流处理
     */
    @Override
    public synchronized boolean start() {
        if (isRunning) {
            logger.warn("流处理器已经在运行中，无法再次启动");
            return false;
        }
        
        if (streamingContext == null) {
            logger.info("流处理器尚未初始化，进行初始化");
            init();
        }
        
        logger.info("启动Kafka流处理...");
        
        try {
            // 确保类加载器设置正确
            KafkaSparkClassResolver.setContextClassLoader();
            
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
    
            logger.info("订阅Kafka主题: {}", topic);
            
            // 创建Kafka直接流
            JavaInputDStream<ConsumerRecord<String, String>> kafkaStream = KafkaUtils.createDirectStream(
                    streamingContext,
                    LocationStrategies.PreferConsistent(),
                    ConsumerStrategies.Subscribe(topics, kafkaParams)
            );
            
            // 处理流 - 使用独立的静态内部类而不是匿名内部类
            kafkaStream.foreachRDD(new RDDProcessor());
    
            // 启动流上下文
            streamingContext.start();
            isRunning = true;
            logger.info("Kafka流处理启动成功");
            
            // 在单独的线程中等待终止，使用匿名内部类替代lambda
            new Thread(new StreamingContextTerminationRunnable(streamingContext), "SparkStreamingTerminationThread").start();
            
            return true;
            
        } catch (Exception e) {
            logger.error("启动Kafka流处理失败", e);
            isRunning = false;
            return false;
        }
    }
    
    /**
     * RDD处理器 - 处理每个批次的RDD
     * 使用静态类以避免序列化问题
     */
    private static class RDDProcessor implements VoidFunction<JavaRDD<ConsumerRecord<String, String>>>, Serializable {
        private static final long serialVersionUID = 1L;
        
        @Override
        public void call(JavaRDD<ConsumerRecord<String, String>> rdd) {
            if (rdd.isEmpty()) {
                return;
            }
            
            // 确保执行器端的类加载器正确
            KafkaSparkClassResolver.setContextClassLoader();
            
            // 使用静态内部类替代lambda表达式
            List<String> jsonStrings = rdd.map(new RecordValueExtractor()).collect();
            
            List<Map<String, Object>> processedData = new ArrayList<>();
            
            Logger logger = LoggerFactory.getLogger(AgricultureSensorProcessor.class);
            logger.debug("接收到 {} 条记录", jsonStrings.size());
            
            // 获取实例
            AgricultureSensorProcessor processor = getProcessorInstance();
            if (processor != null && processor.broadcastDetector != null && processor.broadcastWebSocketProvider != null) {
                // 获取广播变量的值
                AnomalyDetector detector = processor.broadcastDetector.value();
                WebSocketSinkProvider wsProvider = processor.broadcastWebSocketProvider.value();
                
                // 手动解析和处理JSON
                processJsonMessages(jsonStrings, processedData, detector, wsProvider, logger);
            } else {
                logger.error("无法获取处理器实例或广播变量为null");
            }
        }
        
        /**
         * 获取处理器实例 - 可能需要修改以适应实际情况
         */
        private AgricultureSensorProcessor getProcessorInstance() {
            // 这里可能需要通过其他方式获取实例，如Spring上下文或单例模式
            return AgricultureSensorProcessor.getInstance();
        }
        
        /**
         * 处理JSON消息
         */
        private void processJsonMessages(List<String> jsonStrings, 
                                        List<Map<String, Object>> processedData, 
                                        AnomalyDetector detector, 
                                        WebSocketSinkProvider wsProvider,
                                        Logger logger) {
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
                        
                        // 检测异常 - 使用广播变量中的检测器
                        boolean isAnomaly = detector.detectAnomaly(sensorType, value);
                        sensorData.put("is_anomaly", isAnomaly);
                        
                        if (isAnomaly) {
                            logger.info("检测到异常数据: 类型={}, 值={}, 阈值={}", 
                                    sensorType, value, detector.getThreshold(sensorType));
                        }
                        
                        processedData.add(sensorData);
                    }
                } catch (Exception e) {
                    logger.error("解析JSON出错: {} for JSON: {}", e.getMessage(), json, e);
                }
            }
            
            // 在每个执行器内创建新的Gson实例而不是使用外部类实例
            Gson localGson = new Gson();
            
            // 发送处理后的数据到WebSocket
            for (Map<String, Object> data : processedData) {
                String jsonResult = localGson.toJson(data);
                wsProvider.broadcast(jsonResult);
            }
            
            logger.info("处理并推送了 {} 条记录", processedData.size());
        }
        
        /**
         * 验证JSON字段是否包含必要的传感器数据
         */
        private boolean validateJsonFields(JsonObject jsonObject) {
            try {
                // 检查必要字段
                return jsonObject.has("sensor_id") && 
                       jsonObject.has("sensor_type") && 
                       jsonObject.has("value") && 
                       jsonObject.has("timestamp");
            } catch (Exception e) {
                LoggerFactory.getLogger(AgricultureSensorProcessor.class).error("验证JSON字段失败", e);
                return false;
            }
        }
    }
    
    /**
     * 从ConsumerRecord中提取值的函数
     */
    private static class RecordValueExtractor implements Function<ConsumerRecord<String, String>, String>, Serializable {
        private static final long serialVersionUID = 1L;
        
        @Override
        public String call(ConsumerRecord<String, String> record) {
            return record.value();
        }
    }
    
    /**
     * 流上下文终止的Runnable实现
     */
    private static class StreamingContextTerminationRunnable implements Runnable, Serializable {
        private static final long serialVersionUID = 1L;
        private final transient JavaStreamingContext context;
        
        public StreamingContextTerminationRunnable(JavaStreamingContext context) {
            this.context = context;
        }
        
        @Override
        public void run() {
            try {
                if (context != null) {
                    context.awaitTermination();
                }
            } catch (InterruptedException e) {
                LoggerFactory.getLogger(StreamingContextTerminationRunnable.class).warn("流处理器等待终止时被中断", e);
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 在Spring容器销毁前停止Spark相关资源
     */
    @PreDestroy
    @Override
    public synchronized void stop() {
        if (!isRunning || streamingContext == null) {
            logger.info("流处理器未在运行，无需停止");
            return;
        }
        
        logger.info("停止Spark流处理...");
        
        try {
            // 清理广播变量
            if (broadcastDetector != null) {
                broadcastDetector.unpersist();
            }
            if (broadcastWebSocketProvider != null) {
                broadcastWebSocketProvider.unpersist();
            }
            
            streamingContext.stop(true, true);
            sparkSession.close();
            logger.info("Spark流处理已停止");
        } catch (Exception e) {
            logger.error("停止Spark流处理时出错", e);
        } finally {
            isRunning = false;
        }
    }
    
    /**
     * 判断处理器是否正在运行
     */
    @Override
    public boolean isRunning() {
        return isRunning;
    }
} 