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
import org.shiguang.config.SparkEnabledCondition;
import org.shiguang.entity.SensorReading;
import org.shiguang.model.SensorData;
import org.shiguang.repository.SensorReadingRepository;
import org.shiguang.service.KafkaStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import scala.Tuple2;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Conditional(SparkEnabledCondition.class)
public class KafkaStreamingServiceImpl implements KafkaStreamingService {

    private static final Logger logger = LoggerFactory.getLogger(KafkaStreamingServiceImpl.class);
    private ObjectMapper objectMapper;
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

    public KafkaStreamingServiceImpl() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule()); // 支持Java 8日期时间类型
    }

    /**
     * 检查Spark是否可用
     */
    private boolean isSparkAvailable() {
        try {
            return sparkEnabled && sparkSession != null && !sparkSession.sparkContext().isStopped();
        } catch (Exception e) {
            logger.warn("检查SparkSession可用性时出错", e);
            return false;
        }
    }

    @Override
    public boolean startStreamProcessing(String[] topics) {
        if (!isSparkAvailable()) {
            logger.error("Spark未启用，无法启动Kafka流处理");
            return false;
        }
        
        if (isStreaming.get()) {
            logger.warn("流处理已经在运行中");
            return true;
        }
        
        try {
            logger.info("开始启动Kafka流处理，订阅主题: {}", Arrays.toString(topics));
            
            // 创建JavaStreamingContext
            streamingContext = new JavaStreamingContext(javaSparkContext, new Duration(batchDuration * 1000));
            
            // 创建Kafka输入流
            JavaInputDStream<ConsumerRecord<String, String>> stream = KafkaUtils.createDirectStream(
                    streamingContext,
                    LocationStrategies.PreferConsistent(),
                    ConsumerStrategies.Subscribe(Arrays.asList(topics), kafkaParams)
            );
            
            // 转换消息格式
            JavaPairDStream<String, SensorData> sensorStream = stream.mapToPair(record -> {
                SensorData sensorData = null;
                try {
                    sensorData = objectMapper.readValue(record.value(), SensorData.class);
                    return new Tuple2<>(record.key(), sensorData);
                } catch (JsonProcessingException e) {
                    logger.error("解析传感器数据失败: {}", record.value(), e);
                    return new Tuple2<>(record.key(), new SensorData());
                }
            });
            
            // 处理传感器数据流
            processStreamData(sensorStream);
            
            // 启动Streaming Context
            streamingContext.start();
            isStreaming.set(true);
            
            logger.info("Kafka流处理启动成功");
            return true;
        } catch (Exception e) {
            logger.error("启动Kafka流处理失败", e);
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
            return false;
        }
    }

    @Override
    public boolean isStreamingActive() {
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
            try {
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
                        
                        // 如果是异常值，可以考虑发送警报
                        if (isAnomaly) {
                            logger.warn("异常传感器数据: 传感器 {}, 区域 {}, 作物 {}, 值 {}, 时间 {}",
                                    data.getSensorId(), data.getRegion(), data.getCropType(),
                                    data.getValue(), data.getTimestamp());
                            // TODO: 实现警报通知机制
                        }
                    } catch (Exception e) {
                        logger.error("保存传感器数据失败", e);
                    }
                }
            } catch (Exception e) {
                logger.error("处理传感器数据失败", e);
            }
        });
    }
} 