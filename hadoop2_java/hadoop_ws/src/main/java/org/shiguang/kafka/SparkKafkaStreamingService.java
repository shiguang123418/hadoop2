package org.shiguang.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Spark Kafka流式处理服务
 */
@Service
public class SparkKafkaStreamingService {

    @Value("${kafka.bootstrap.servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.group.id:agri-big-data-group}")
    private String groupId;

    @Value("${spark.app.name:Spark-Kafka-Streaming}")
    private String appName;

    @Value("${spark.master:local[2]}")
    private String master;

    @Value("${spark.streaming.batch.duration:5}")
    private long batchDuration;

    private JavaStreamingContext streamingContext;
    private ExecutorService executorService;
    private AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 创建Spark Streaming上下文
     * @return JavaStreamingContext
     */
    private JavaStreamingContext createStreamingContext() {
        SparkConf sparkConf = new SparkConf()
                .setAppName(appName)
                .setMaster(master);
        
        return new JavaStreamingContext(sparkConf, Durations.seconds(batchDuration));
    }

    /**
     * 启动Kafka流式处理
     * @param topics 要消费的主题列表
     * @param messageProcessor 消息处理器
     */
    public void startKafkaStreaming(List<String> topics, Consumer<String> messageProcessor) {
        if (running.get()) {
            throw new IllegalStateException("Streaming is already running");
        }
        
        running.set(true);
        
        executorService.submit(() -> {
            try {
                streamingContext = createStreamingContext();
                
                // 设置Kafka参数
                Map<String, Object> kafkaParams = new HashMap<>();
                kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
                kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
                kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
                kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
                
                // 创建DStream
                JavaInputDStream<ConsumerRecord<String, String>> stream = KafkaUtils.createDirectStream(
                        streamingContext,
                        LocationStrategies.PreferConsistent(),
                        ConsumerStrategies.Subscribe(topics, kafkaParams)
                );
                
                // 处理消息
                stream.foreachRDD(rdd -> {
                    rdd.foreach(record -> {
                        messageProcessor.accept(record.value());
                    });
                });
                
                // 启动流式处理
                streamingContext.start();
                streamingContext.awaitTermination();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopStreaming();
                running.set(false);
            }
        });
    }

    /**
     * 停止流式处理
     */
    public void stopStreaming() {
        if (streamingContext != null) {
            streamingContext.stop(true, true);
            streamingContext = null;
        }
        running.set(false);
    }

    @PreDestroy
    public void cleanup() {
        stopStreaming();
        executorService.shutdown();
    }
} 