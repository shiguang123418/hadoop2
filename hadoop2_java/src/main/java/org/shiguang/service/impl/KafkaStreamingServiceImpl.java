package org.shiguang.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.shiguang.entity.SensorReading;
import org.shiguang.model.SensorData;
import org.shiguang.repository.SensorReadingRepository;
import org.shiguang.service.KafkaStreamingService;
import org.shiguang.websocket.KafkaSensorWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import javax.annotation.PreDestroy;
import scala.Tuple2;
import org.apache.spark.SparkConf;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class KafkaStreamingServiceImpl implements KafkaStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamingServiceImpl.class);
    private JavaStreamingContext streamingContext;
    private final AtomicBoolean isStreaming = new AtomicBoolean(false);
    
    @Value("${kafka.streaming.batch.duration:5}")
    private int batchDuration;
    
    @Value("${spark.enabled:false}")
    private boolean sparkEnabled;

    @Autowired(required = false)
    private SparkSession sparkSession;
    
    @Autowired(required = false)
    private JavaSparkContext javaSparkContext;
    
    @Autowired
    @Qualifier("kafkaConsumerConfig")
    private Map<String, Object> kafkaParams;
    
    @Autowired
    private SensorReadingRepository sensorReadingRepository;

    @Autowired
    private KafkaSensorWebSocketHandler webSocketHandler;

    public KafkaStreamingServiceImpl() {
        // objectMapper = new ObjectMapper();
        // objectMapper.registerModule(new JavaTimeModule()); // 支持Java 8日期时间类型
    }

    /**
     * 在应用关闭时确保流处理也被正确关闭
     */
    @PreDestroy
    public void shutdown() {
        if (isStreaming.get() && streamingContext != null) {
            logger.info("应用关闭，停止Kafka流处理...");
            stopStreamProcessing();
        }
    }

    /**
     * 检查Spark是否可用
     */
    private boolean isSparkAvailable() {
        try {
            return sparkEnabled && sparkSession != null && !sparkSession.sparkContext().isStopped() 
                   && javaSparkContext != null;
        } catch (Exception e) {
            logger.warn("检查SparkSession可用性时出错", e);
            return false;
        }
    }

    /**
     * 检查Kafka所需的类是否在类路径中
     */
    private boolean checkKafkaClassesAvailable() {
        try {
            // 检查关键类是否存在
            Class.forName("org.apache.spark.streaming.kafka010.KafkaRDDPartition");
            Class.forName("org.apache.spark.streaming.kafka010.KafkaUtils");
            return true;
        } catch (ClassNotFoundException e) {
            logger.error("Kafka所需类未找到: {}", e.getMessage());
            logger.error("请确保spark-streaming-kafka-0-10相关依赖正确配置");
            return false;
        }
    }

    @Override
    public boolean startStreamProcessing(String[] topics) {
        if (!isSparkAvailable()) {
            logger.error("Spark未启用或组件不可用，无法启动Kafka流处理");
            return false;
        }
        
        if (!checkKafkaClassesAvailable()) {
            logger.error("Kafka所需类未找到，无法启动流处理");
            return false;
        }
        
        if (isStreaming.get()) {
            logger.warn("流处理已经在运行中");
            return true;
        }
        
        try {
            logger.info("开始启动Kafka流处理，订阅主题: {}", Arrays.toString(topics));
            
            // 确保类加载器配置正确，使Kafka相关类可访问
            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            ClassLoader sparkClassLoader = null;
            
            // 尝试使用SparkSession的类加载器
            if (sparkSession != null && sparkSession.sparkContext() != null) {
                sparkClassLoader = sparkSession.getClass().getClassLoader();
                logger.info("使用SparkSession的类加载器: {}", sparkClassLoader);
            }
            
            // 如果无法获取SparkSession的类加载器，则使用当前类的类加载器
            if (sparkClassLoader == null) {
                sparkClassLoader = getClass().getClassLoader();
                logger.info("使用当前类的类加载器: {}", sparkClassLoader);
            }
            
            // 设置上下文类加载器
            Thread.currentThread().setContextClassLoader(sparkClassLoader);
            
            // 如果JavaSparkContext为空，尝试使用createStreamingContext方法创建一个新的
            if (javaSparkContext == null) {
                streamingContext = createStreamingContext();
            } else {
                streamingContext = new JavaStreamingContext(javaSparkContext, new Duration(batchDuration * 1000));
            }
            
            // 确保 kafkaParams 中包含 group.id
            if (!kafkaParams.containsKey(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG) || kafkaParams.get(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG) == null) {
                String groupIdFromConfig = kafkaParams.getOrDefault("group.id", "default-spark-streaming-group").toString(); 
                kafkaParams.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, groupIdFromConfig);
                logger.warn("Kafka group.id 未在 kafkaParams 中明确设置或为null，已设置为: {}", groupIdFromConfig);
            } else {
                logger.info("Kafka group.id 已在 kafkaParams 中设置为: {}", kafkaParams.get(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG));
            }
            
            // 打印当前kafkaParams中的所有键值对，用于调试
            logger.info("Current kafkaParams keys before adjustment: {}", kafkaParams.keySet());
            
            // 创建一个新的Map来存储所有必要的Kafka参数
            Map<String, Object> sparkKafkaParams = new HashMap<>(kafkaParams);
            
            // 确保所有Spark Streaming Kafka必需的参数都存在
            
            // 1. bootstrap.servers - Kafka服务器地址
            String bootstrapServersKey = org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
            if (!sparkKafkaParams.containsKey(bootstrapServersKey) || sparkKafkaParams.get(bootstrapServersKey) == null) {
                String bootstrapServers = "192.168.1.192:9092"; // 使用实际的Kafka服务器地址
                sparkKafkaParams.put(bootstrapServersKey, bootstrapServers);
                logger.warn("bootstrap.servers 未在配置中设置，已手动设置为: {}", bootstrapServers);
            }
            
            // 2. key.deserializer - 键的反序列化器
            sparkKafkaParams.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, 
                            org.apache.kafka.common.serialization.StringDeserializer.class.getName());
            
            // 3. value.deserializer - 值的反序列化器
            sparkKafkaParams.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, 
                            org.apache.kafka.common.serialization.StringDeserializer.class.getName());
            
            // 4. auto.offset.reset - 自动偏移量重置策略
            if (!sparkKafkaParams.containsKey(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)) {
                sparkKafkaParams.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
            }
            
            // 5. enable.auto.commit - 是否自动提交偏移量
            if (!sparkKafkaParams.containsKey(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG)) {
                sparkKafkaParams.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
            }
            
            // 打印调整后的Kafka参数
            logger.info("Final Kafka parameters for Spark Streaming:");
            for (Map.Entry<String, Object> entry : sparkKafkaParams.entrySet()) {
                logger.info("  {} = {}", entry.getKey(), entry.getValue());
            }

            try {
                // 创建Kafka输入流，使用调整后的sparkKafkaParams
                JavaInputDStream<ConsumerRecord<String, String>> stream = KafkaUtils.createDirectStream(
                        streamingContext,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(Arrays.asList(topics), sparkKafkaParams)
                );
                
                // 在mapToPair的lambda表达式内部创建ObjectMapper实例
                JavaPairDStream<String, SensorData> sensorStream = stream.mapToPair(record -> {
                    ObjectMapper localObjectMapper = new ObjectMapper();
                    localObjectMapper.registerModule(new JavaTimeModule());
                    SensorData sensorData = null;
                    try {
                        sensorData = localObjectMapper.readValue(record.value(), SensorData.class);
                        return new Tuple2<>(record.key(), sensorData);
                    } catch (JsonProcessingException e) {
                        // Log a specific error for parsing, but don't stop the stream.
                        // Consider sending malformed records to a dead-letter topic or counter.
                        logger.error("解析传感器JSON数据失败: '{}', 错误: {}", record.value(), e.getMessage());
                        // 返回一个特殊的Tuple，或者根据业务逻辑过滤掉这些记录
                        // 例如，可以返回一个key和null的SensorData，然后在后续处理中过滤掉null
                        return new Tuple2<>(record.key(), null); // 或者 new SensorData() with error flag
                    }
                });

                // 过滤掉解析失败的记录 (如果上一步返回了null)
                JavaPairDStream<String, SensorData> successfullyParsedStream = sensorStream.filter(tuple -> tuple._2() != null);
                
                // 处理传感器数据流
                processStreamData(successfullyParsedStream); // 注意：这里传递的是过滤后的流
                
                streamingContext.start();
                isStreaming.set(true);
                
                // 恢复原始类加载器
                Thread.currentThread().setContextClassLoader(originalClassLoader);
                
                logger.info("Kafka流处理启动成功");
                return true;
            } catch (Exception e) {
                logger.error("创建Kafka流时出错: {}", e.getMessage(), e);
                // 恢复原始类加载器
                Thread.currentThread().setContextClassLoader(originalClassLoader);
                return false;
            }
        } catch (Exception e) {
            logger.error("启动Kafka流处理失败", e);
            if (streamingContext != null) {
                try {
                    streamingContext.stop(true, true);
                } catch (Exception stopEx) {
                    logger.error("清理StreamingContext时出错", stopEx);
                }
            }
            isStreaming.set(false);
            return false;
        }
    }

    @Override
    public boolean stopStreamProcessing() {
        if (!isStreaming.get() || streamingContext == null) {
            logger.warn("流处理未在运行");
            return false;
        }
        
        try {
            // 优雅停止，等待所有数据处理完成
            streamingContext.stop(true, true);
            isStreaming.set(false);
            logger.info("Kafka流处理已停止");
            return true;
        } catch (Exception e) {
            logger.error("停止Kafka流处理失败", e);
            // 即使出错，也标记为已停止，以便用户可以尝试重新启动
            isStreaming.set(false);
            return false;
        }
    }

    @Override
    public boolean isStreamingActive() {
        if (streamingContext != null && streamingContext.sparkContext().sc().isStopped()) {
            // SparkContext已停止，但状态仍为运行，修正状态
            if (isStreaming.get()) {
                logger.warn("检测到SparkContext已停止但流处理状态仍为运行中，正在修正状态");
                isStreaming.set(false);
            }
        }
        return isStreaming.get();
    }

    @Override
    public JavaStreamingContext getStreamingContext() {
        return streamingContext;
    }
    
    /**
     * 处理传感器数据流
     * 实现各类数据处理逻辑
     */
    private void processStreamData(JavaPairDStream<String, SensorData> stream) {
        // 分组聚合 - 按区域和传感器类型计算平均值
        stream.foreachRDD(rdd -> {
            if (rdd.isEmpty()) {
                return;
            }
            
            try {
                // 转换RDD为Dataset
                List<SensorData> dataList = rdd.values().collect();
                if (dataList.isEmpty()) {
                    return;
                }
                
                logger.info("收到 {} 条传感器数据", dataList.size());
                
                // 创建Schema
                StructType schema = DataTypes.createStructType(new StructField[] {
                    DataTypes.createStructField("sensorId", DataTypes.StringType, false),
                    DataTypes.createStructField("sensorType", DataTypes.StringType, false),
                    DataTypes.createStructField("region", DataTypes.StringType, false),
                    DataTypes.createStructField("cropType", DataTypes.StringType, false),
                    DataTypes.createStructField("value", DataTypes.DoubleType, false)
                });
                
                // 转换为Dataset
                List<Row> rows = new ArrayList<>();
                for (SensorData data : dataList) {
                    rows.add(org.apache.spark.sql.RowFactory.create(
                        data.getSensorId(),
                        data.getSensorType(),
                        data.getRegion(),
                        data.getCropType(),
                        data.getValue()
                    ));
                }
                
                Dataset<Row> df = sparkSession.createDataFrame(rows, schema);
                df.createOrReplaceTempView("sensor_data");
                
                // 计算区域平均值
                Dataset<Row> regionAvg = sparkSession.sql(
                    "SELECT region, sensorType, AVG(value) as avgValue FROM sensor_data GROUP BY region, sensorType"
                );
                
                logger.info("区域传感器数据平均值:");
                regionAvg.show();
                
                // 按作物类型聚合
                Dataset<Row> cropTypeAvg = sparkSession.sql(
                    "SELECT cropType, sensorType, AVG(value) as avgValue FROM sensor_data GROUP BY cropType, sensorType"
                );
                
                logger.info("作物传感器数据平均值:");
                cropTypeAvg.show();
                
                // 收集传感器读数和异常值列表
                List<SensorReading> readings = new ArrayList<>();
                List<SensorReading> anomalies = new ArrayList<>();
                
                // 将数据保存到数据库
                for (SensorData data : dataList) {
                    try {
                        // 异常值检测 (简单示例: 如果值超过平均值的2倍，标记为异常)
                        double avgValue = df.agg(org.apache.spark.sql.functions.avg("value"))
                                .collectAsList().get(0).getDouble(0);
                        boolean isAnomaly = data.getValue() > avgValue * 2 || data.getValue() < 0;
                        
                        // 创建实体对象并保存
                        SensorReading reading = new SensorReading();
                        reading.setSensorId(data.getSensorId());
                        reading.setSensorType(data.getSensorType());
                        reading.setRegion(data.getRegion());
                        reading.setCropType(data.getCropType());
                        reading.setValue(data.getValue());
                        reading.setTimestamp(data.getTimestamp());
                        reading.setUnit(data.getUnit());
                        reading.setDescription(data.getDescription());
                        reading.setAnomaly(isAnomaly);
                        
                        sensorReadingRepository.save(reading);
                        readings.add(reading);
                        
                        // 如果是异常值，可以考虑发送警报
                        if (isAnomaly) {
                            logger.warn("异常传感器数据: 传感器 {}, 区域 {}, 作物 {}, 值 {}, 时间 {}",
                                    data.getSensorId(), data.getRegion(), data.getCropType(),
                                    data.getValue(), data.getTimestamp());
                                    
                            anomalies.add(reading);
                        }
                    } catch (Exception e) {
                        logger.error("保存传感器数据失败", e);
                    }
                }
                
                // 通过WebSocket推送数据
                try {
                    // 尝试获取WebSocket处理器
                    if (webSocketHandler != null) {
                        // 推送传感器数据
                        webSocketHandler.broadcastSensorData(readings);
                        
                        // 如果有异常数据，推送异常数据
                        if (!anomalies.isEmpty()) {
                            webSocketHandler.broadcastAnomalies(anomalies);
                        }
                        
                        // 更新并推送摘要数据
                        Map<String, Object> summary = new HashMap<>();
                        summary.put("totalReadings", sensorReadingRepository.count());
                        summary.put("anomalies", sensorReadingRepository.countByAnomalyIsTrue());
                        summary.put("anomalyPercentage", 
                            (double) sensorReadingRepository.countByAnomalyIsTrue() / sensorReadingRepository.count() * 100);
                        summary.put("regions", sensorReadingRepository.countDistinctRegions());
                        summary.put("cropTypes", sensorReadingRepository.countDistinctCropTypes());
                        
                        webSocketHandler.broadcastSummary(summary);
                    }
                } catch (Exception e) {
                    logger.error("推送WebSocket数据失败", e);
                }
                
            } catch (Exception e) {
                logger.error("处理传感器数据失败", e);
            }
        });
    }

    private void initStreamingContext() {
        try {
            if (streamingContext == null) {
                logger.info("初始化JavaStreamingContext");
                
                // 确保Kafka类在类路径中可访问
                Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
                
                // 设置检查点目录
                String checkpointDir = "/tmp/spark-checkpoint";
                
                // 尝试创建JavaStreamingContext
                if (javaSparkContext != null) {
                    streamingContext = new JavaStreamingContext(javaSparkContext, new Duration(batchDuration * 1000));
                } else {
                    // 使用createStreamingContext方法创建
                    streamingContext = createStreamingContext();
                }
                
                // 设置检查点目录
                streamingContext.checkpoint(checkpointDir);
                
                logger.info("JavaStreamingContext初始化成功，批处理间隔: {}ms, 检查点目录: {}", batchDuration * 1000, checkpointDir);
            }
        } catch (Exception e) {
            logger.error("初始化JavaStreamingContext失败", e);
            streamingContext = null;
        }
    }

    public JavaStreamingContext createStreamingContext() {
        try {
            logger.info("创建新的JavaStreamingContext实例...");
            // 确保KafkaStreamingContext类在类路径中可访问
            // 尝试使用SparkSession的类加载器，如果可用
            ClassLoader sparkClassLoader = null;
            if (sparkSession != null && sparkSession.sparkContext() != null && sparkSession.sparkContext().ui().isDefined()) {
                sparkClassLoader = sparkSession.sparkContext().ui().get().getClass().getClassLoader();
                logger.info("尝试使用SparkSession的类加载器: {}", sparkClassLoader);
            }
            
            // 如果SparkSession的类加载器不可用，则使用当前线程的上下文类加载器或服务类的类加载器
            if (sparkClassLoader == null) {
                sparkClassLoader = Thread.currentThread().getContextClassLoader();
                if (sparkClassLoader == null) {
                    sparkClassLoader = getClass().getClassLoader();
                    logger.info("尝试使用服务类的类加载器: {}", sparkClassLoader);
                } else {
                    logger.info("尝试使用当前线程的上下文类加载器: {}", sparkClassLoader);
                }
            }
            Thread.currentThread().setContextClassLoader(sparkClassLoader);
            
            // 检查是否能找到Kafka相关的关键类
            try {
                Class<?> kafkaRDDPartitionClass = Class.forName("org.apache.spark.streaming.kafka010.KafkaRDDPartition", true, sparkClassLoader);
                logger.info("成功加载 KafkaRDDPartition 类: {}", kafkaRDDPartitionClass.getName());
            } catch (ClassNotFoundException e) {
                logger.error("无法找到 KafkaRDDPartition 类: {}", e.getMessage());
                // 获取类加载器层次结构，以便进行诊断
                ClassLoader loader = sparkClassLoader;
                StringBuilder classLoaderHierarchy = new StringBuilder();
                while (loader != null) {
                    classLoaderHierarchy.append(loader).append(" -> ");
                    loader = loader.getParent();
                }
                classLoaderHierarchy.append("null");
                logger.error("类加载器层次结构: {}", classLoaderHierarchy);
            }
            
            String appJar = System.getProperty("java.class.path");
            if (appJar != null && appJar.toLowerCase().endsWith(".jar")) {
                 logger.info("检测到应用JAR路径: {}", appJar);
            } else {
                logger.warn("未能自动检测到应用JAR路径，java.class.path: {}", appJar);
                // 尝试通过其他方式获取JAR路径，或者确保Spring Boot插件正确设置了
            }

            SparkConf sparkConf = new SparkConf()
                    .setMaster("local[2]")
                    .setAppName("AgricultureSensorDataStreaming")
                    .set("spark.task.maxFailures", "1")
                    .set("spark.executor.extraClassPath", System.getProperty("java.class.path"))
                    .set("spark.driver.extraClassPath", System.getProperty("java.class.path"))
                    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                    .set("spark.kryo.registrator", "org.shiguang.config.CustomKryoRegistrator") 
                    .set("spark.executor.userClassPathFirst", "true")
                    .set("spark.driver.userClassPathFirst", "true")
                    // 添加依赖中的Spark Streaming Kafka库到类路径
                    .set("spark.jars.packages", "org.apache.spark:spark-streaming-kafka-0-10_2.12:" + sparkSession.version());
            
            logger.info("SparkConf配置完成: Master={}, AppName={}", sparkConf.get("spark.master"), sparkConf.get("spark.app.name"));
            
            JavaSparkContext newJsc = JavaSparkContext.fromSparkContext(SparkSession.builder().config(sparkConf).getOrCreate().sparkContext());
            logger.info("新的JavaSparkContext已创建: {}", newJsc.sc().applicationId());
            
            // 尝试从类路径中加载Kafka类
            try {
                logger.info("尝试从新的JavaSparkContext加载Kafka类...");
                ClassLoader jscClassLoader = newJsc.getClass().getClassLoader();
                Class<?> kafkaUtilsClass = Class.forName("org.apache.spark.streaming.kafka010.KafkaUtils", true, jscClassLoader);
                logger.info("成功从JavaSparkContext加载KafkaUtils类: {}", kafkaUtilsClass.getName());
            } catch (ClassNotFoundException e) {
                logger.error("无法从JavaSparkContext加载KafkaUtils类: {}", e.getMessage());
            }
            
            JavaStreamingContext newSsc = new JavaStreamingContext(newJsc, new Duration(batchDuration * 1000));
            logger.info("新的JavaStreamingContext已创建，批处理间隔: {}ms", batchDuration * 1000);
            
            String checkpointDir = "/tmp/spark-checkpoint-" + UUID.randomUUID().toString();
            newSsc.checkpoint(checkpointDir);
            logger.info("JavaStreamingContext检查点目录已设置为: {}", checkpointDir);
            
            return newSsc;
        } catch (Exception e) {
            logger.error("创建JavaStreamingContext时出错", e);
            throw new RuntimeException("无法创建JavaStreamingContext", e);
        }
    }
} 