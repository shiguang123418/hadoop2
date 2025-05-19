package org.shiguang.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Kafka消费者客户端
 */
@Component
public class KafkaConsumerClient {

    @Value("${kafka.bootstrap.servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${kafka.group.id:default-group}")
    private String groupId;

    private KafkaConsumer<String, String> consumer;
    private ExecutorService executorService;
    private AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        
        consumer = new KafkaConsumer<>(props);
        executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * 订阅主题并开始消费消息
     * @param topic 主题名称
     * @param messageHandler 消息处理器
     */
    public void subscribeAndConsume(String topic, Consumer<String> messageHandler) {
        if (running.get()) {
            throw new IllegalStateException("Consumer is already running");
        }
        
        running.set(true);
        consumer.subscribe(Collections.singletonList(topic));
        
        executorService.submit(() -> {
            try {
                while (running.get()) {
                    ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                    for (ConsumerRecord<String, String> record : records) {
                        messageHandler.accept(record.value());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                consumer.close();
                running.set(false);
            }
        });
    }

    /**
     * 停止消费
     */
    public void stopConsuming() {
        running.set(false);
    }

    @PreDestroy
    public void cleanup() {
        stopConsuming();
        executorService.shutdown();
    }
} 