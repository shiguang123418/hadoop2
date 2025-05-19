package org.shiguang.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Kafka生产者客户端
 */
@Component
public class KafkaProducerClient {

    @Value("${kafka.bootstrap.servers:localhost:9092}")
    private String bootstrapServers;

    private KafkaProducer<String, String> producer;

    @PostConstruct
    public void init() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432); // 32MB
        
        producer = new KafkaProducer<>(props);
    }

    /**
     * 发送消息到指定主题（异步）
     * @param topic 主题名称
     * @param message 消息内容
     */
    public void sendMessage(String topic, String message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, message);
        producer.send(record, (metadata, exception) -> {
            if (exception != null) {
                exception.printStackTrace();
            }
        });
    }

    /**
     * 发送消息到指定主题（同步）
     * @param topic 主题名称
     * @param key 消息键
     * @param message 消息内容
     * @throws ExecutionException 如果消息发送失败
     * @throws InterruptedException 如果线程被中断
     */
    public void sendMessageSync(String topic, String key, String message) throws ExecutionException, InterruptedException {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, message);
        producer.send(record).get();
    }

    /**
     * 批量发送消息
     * @param topic 主题名称
     * @param messages 消息列表
     */
    public void sendMessages(String topic, Iterable<String> messages) {
        for (String message : messages) {
            sendMessage(topic, message);
        }
        producer.flush();
    }

    @PreDestroy
    public void cleanup() {
        if (producer != null) {
            producer.flush();
            producer.close();
        }
    }
} 