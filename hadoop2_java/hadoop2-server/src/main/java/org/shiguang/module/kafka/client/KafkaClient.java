package org.shiguang.module.kafka.client;

import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Kafka客户端工具类
 * 提供Kafka操作的便捷方法
 */
@Component
public class KafkaClient {
    private static final Logger logger = LoggerFactory.getLogger(KafkaClient.class);
    
    @Value("${kafka.bootstrap.servers}")
    private String bootstrapServers;
    
    @Value("${kafka.consumer.group.id:hadoop-consumer-group}")
    private String consumerGroupId;
    
    private AdminClient adminClient;
    private KafkaProducer<String, String> producer;
    private Map<String, KafkaConsumer<String, String>> consumers = new HashMap<>();
    
    /**
     * 初始化Kafka连接
     */
    @PostConstruct
    public void init() {
        try {
            // 创建AdminClient
            Properties adminProps = new Properties();
            adminProps.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            adminClient = AdminClient.create(adminProps);
            
            // 创建生产者
            Properties producerProps = new Properties();
            producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
            producerProps.put(ProducerConfig.ACKS_CONFIG, "all");
            producer = new KafkaProducer<>(producerProps);
            
            logger.info("Kafka客户端初始化完成，连接到: {}", bootstrapServers);
        } catch (Exception e) {
            logger.error("Kafka客户端初始化失败: {}", e.getMessage(), e);
            throw new RuntimeException("初始化Kafka客户端失败", e);
        }
    }
    
    /**
     * 关闭Kafka连接
     */
    @PreDestroy
    public void close() {
        if (adminClient != null) {
            adminClient.close();
            logger.info("Kafka AdminClient已关闭");
        }
        
        if (producer != null) {
            producer.close();
            logger.info("Kafka生产者已关闭");
        }
        
        consumers.forEach((group, consumer) -> {
            consumer.close();
            logger.info("Kafka消费者(组: {})已关闭", group);
        });
        consumers.clear();
    }
    
    /**
     * 获取AdminClient实例
     */
    public AdminClient getAdminClient() {
        return adminClient;
    }
    
    /**
     * 获取生产者实例
     */
    public KafkaProducer<String, String> getProducer() {
        return producer;
    }
    
    /**
     * 获取所有主题列表
     * @return 主题列表
     */
    public List<String> listTopics() throws ExecutionException, InterruptedException {
        ListTopicsResult topics = adminClient.listTopics();
        return new ArrayList<>(topics.names().get());
    }
    
    /**
     * 创建主题
     * @param topicName 主题名称
     * @param partitions 分区数
     * @param replicationFactor 副本因子
     * @return 是否创建成功
     */
    public boolean createTopic(String topicName, int partitions, short replicationFactor) {
        try {
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);
            CreateTopicsResult result = adminClient.createTopics(Collections.singleton(newTopic));
            KafkaFuture<Void> future = result.values().get(topicName);
            future.get(); // 等待完成
            logger.info("主题创建成功: {}", topicName);
            return true;
        } catch (Exception e) {
            logger.error("创建主题失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 删除主题
     * @param topicName 主题名称
     * @return 是否删除成功
     */
    public boolean deleteTopic(String topicName) {
        try {
            DeleteTopicsResult result = adminClient.deleteTopics(Collections.singleton(topicName));
            KafkaFuture<Void> future = result.values().get(topicName);
            future.get(); // 等待完成
            logger.info("主题删除成功: {}", topicName);
            return true;
        } catch (Exception e) {
            logger.error("删除主题失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取主题详情
     * @param topicName 主题名称
     * @return 主题详情
     */
    public Map<String, Object> describeTopic(String topicName) {
        try {
            DescribeTopicsResult result = adminClient.describeTopics(Collections.singleton(topicName));
            TopicDescription topicDescription = result.values().get(topicName).get();
            
            Map<String, Object> topicInfo = new HashMap<>();
            topicInfo.put("name", topicDescription.name());
            topicInfo.put("isInternal", topicDescription.isInternal());
            
            List<Map<String, Object>> partitions = new ArrayList<>();
            for (TopicPartitionInfo partition : topicDescription.partitions()) {
                Map<String, Object> partitionInfo = new HashMap<>();
                partitionInfo.put("id", partition.partition());
                partitionInfo.put("leader", partition.leader().id());
                partitionInfo.put("replicas", partition.replicas().stream()
                        .map(node -> node.id()).collect(Collectors.toList()));
                partitions.add(partitionInfo);
            }
            
            topicInfo.put("partitions", partitions);
            topicInfo.put("partitionCount", partitions.size());
            
            return topicInfo;
        } catch (Exception e) {
            logger.error("获取主题详情失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取主题详情失败", e);
        }
    }
    
    /**
     * 发送消息到主题
     * @param topicName 主题名称
     * @param key 消息键（可以为null）
     * @param value 消息值
     * @return 发送结果
     */
    public Map<String, Object> sendMessage(String topicName, String key, String value) {
        try {
            ProducerRecord<String, String> record = new ProducerRecord<>(topicName, key, value);
            RecordMetadata metadata = producer.send(record).get();
            
            Map<String, Object> result = new HashMap<>();
            result.put("topic", metadata.topic());
            result.put("partition", metadata.partition());
            result.put("offset", metadata.offset());
            result.put("timestamp", metadata.timestamp());
            result.put("success", true);
            
            return result;
        } catch (Exception e) {
            logger.error("发送消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("发送消息失败", e);
        }
    }
    
    /**
     * 批量发送消息到主题
     * @param topicName 主题名称
     * @param messages 消息列表
     * @return 发送结果
     */
    public List<Map<String, Object>> sendMessages(String topicName, List<Map<String, String>> messages) {
        List<Map<String, Object>> results = new ArrayList<>();
        
        try {
            for (Map<String, String> message : messages) {
                String key = message.get("key");
                String value = message.get("value");
                
                Map<String, Object> result = sendMessage(topicName, key, value);
                results.add(result);
            }
            
            return results;
        } catch (Exception e) {
            logger.error("批量发送消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量发送消息失败", e);
        }
    }
    
    /**
     * 消费消息
     * @param topicName 主题名称
     * @param groupId 消费者组ID
     * @param maxRecords 最大获取消息数
     * @param timeoutMs 超时时间（毫秒）
     * @return 消费到的消息列表
     */
    public List<Map<String, Object>> consumeMessages(String topicName, String groupId, int maxRecords, long timeoutMs) {
        if (groupId == null || groupId.isEmpty()) {
            groupId = consumerGroupId;
        }
        
        KafkaConsumer<String, String> consumer = getOrCreateConsumer(groupId);
        
        try {
            // 订阅主题
            consumer.subscribe(Collections.singletonList(topicName));
            
            // 消费消息
            List<Map<String, Object>> messages = new ArrayList<>();
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(timeoutMs));
            
            records.forEach(record -> {
                Map<String, Object> message = new HashMap<>();
                message.put("topic", record.topic());
                message.put("partition", record.partition());
                message.put("offset", record.offset());
                message.put("key", record.key());
                message.put("value", record.value());
                message.put("timestamp", record.timestamp());
                messages.add(message);
                
                if (messages.size() >= maxRecords) {
                    return;
                }
            });
            
            // 提交偏移量
            consumer.commitSync();
            
            return messages;
        } catch (Exception e) {
            logger.error("消费消息失败: {}", e.getMessage(), e);
            throw new RuntimeException("消费消息失败", e);
        }
    }
    
    /**
     * 获取或创建指定消费者组的消费者
     * @param groupId 消费者组ID
     * @return 消费者实例
     */
    private synchronized KafkaConsumer<String, String> getOrCreateConsumer(String groupId) {
        if (consumers.containsKey(groupId)) {
            return consumers.get(groupId);
        }
        
        // 创建新的消费者
        Properties consumerProps = new Properties();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(consumerProps);
        consumers.put(groupId, consumer);
        
        return consumer;
    }
    
    /**
     * 获取主题分区数
     * @param topicName 主题名称
     * @return 分区数
     */
    public int getPartitionCount(String topicName) {
        try {
            Map<String, Object> topicInfo = describeTopic(topicName);
            return (int) topicInfo.get("partitionCount");
        } catch (Exception e) {
            logger.error("获取主题分区数失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取主题分区数失败", e);
        }
    }
    
    /**
     * 获取主题的消费者组列表
     * @param topicName 主题名称
     * @return 消费者组列表
     */
    public List<String> getConsumerGroups(String topicName) {
        try {
            ListConsumerGroupsResult result = adminClient.listConsumerGroups();
            Collection<ConsumerGroupListing> groupsCollection = result.all().get();
            
            // 转换Collection为List并过滤出订阅了指定主题的消费者组
            List<String> groupIds = new ArrayList<>(groupsCollection).stream()
                    .map(ConsumerGroupListing::groupId)
                    .collect(Collectors.toList());
            
            return groupIds;
        } catch (Exception e) {
            logger.error("获取主题消费者组失败: {}", e.getMessage(), e);
            throw new RuntimeException("获取主题消费者组失败", e);
        }
    }
} 