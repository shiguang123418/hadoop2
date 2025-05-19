# 农业大数据平台

本项目是一个基于Hadoop、Spark和Kafka的农业大数据处理平台，使用Spring Boot框架构建。

## 项目结构

项目采用多模块结构，主要包含以下模块：

### 1. hadoop模块

该模块包含与Hadoop HDFS、Hive等组件的接口代码，负责数据的存储和查询。

主要功能：
- HDFS客户端：提供文件上传、下载、读写等功能
- Hive客户端：提供SQL查询、表管理等功能

目录结构：
```
hadoop/
├── src/
│   └── main/
│       ├── java/
│       │   └── org/shiguang/hadoop/
│       │       ├── HDFSClient.java     # HDFS操作客户端
│       │       ├── HiveClient.java     # Hive操作客户端
│       │       └── config/
│       │           └── HadoopConfig.java  # Hadoop配置类
│       └── resources/
│           └── hadoop-config.properties   # Hadoop配置文件
└── pom.xml  # 模块POM文件
```

### 2. hadoop_ws模块

该模块包含Kafka消息处理和WebSocket相关代码，负责实时数据流处理和前端数据推送。

主要功能：
- Kafka生产者：发送消息到Kafka主题
- Kafka消费者：从Kafka主题消费消息
- Spark Streaming集成：使用Spark处理Kafka消息流

目录结构：
```
hadoop_ws/
├── src/
│   └── main/
│       ├── java/
│       │   └── org/shiguang/kafka/
│       │       ├── KafkaConsumerClient.java        # Kafka消费者客户端
│       │       ├── KafkaProducerClient.java        # Kafka生产者客户端
│       │       ├── SparkKafkaStreamingService.java # Spark Kafka流处理服务
│       │       └── config/
│       │           └── KafkaConfig.java            # Kafka配置类
│       └── resources/
│           └── kafka-config.properties             # Kafka配置文件
└── pom.xml  # 模块POM文件
```

## 技术栈

- Java 11
- Spring Boot 2.7.17
- Hadoop 3.4.0
- Hive 3.1.2
- Spark 3.5.2
- Kafka
- Log4j2

## 配置说明

### Hadoop配置

在`hadoop/src/main/resources/hadoop-config.properties`中配置Hadoop相关参数：

```properties
# HDFS配置
hadoop.hdfs.uri=hdfs://192.168.1.192:9000
hadoop.hdfs.user=hadoop

# Hive配置
hive.url=jdbc:hive2://192.168.1.192:10000
hive.driver=org.apache.hive.jdbc.HiveDriver
hive.username=
hive.password=
hive.database=default
```

### Kafka配置

在`hadoop_ws/src/main/resources/kafka-config.properties`中配置Kafka相关参数：

```properties
# Kafka基本配置
kafka.bootstrap.servers=192.168.1.192:9092
kafka.group.id=agri-big-data-group

# 主题配置
kafka.default.topic.partitions=3
kafka.default.topic.replication=1
```

## 编译与运行

使用Maven编译项目：

```bash
mvn clean package
```

运行Spring Boot应用：

```bash
java -jar target/agri-big-data-0.0.1.jar
```

## 模块使用方式

### HDFS操作示例

```java
@Autowired
private HDFSClient hdfsClient;

// 上传文件
hdfsClient.uploadFile("/local/path/file.txt", "/hdfs/path/file.txt");

// 下载文件
hdfsClient.downloadFile("/hdfs/path/file.txt", "/local/path/file.txt");
```

### Hive查询示例

```java
@Autowired
private HiveClient hiveClient;

// 执行查询
List<Map<String, Object>> results = hiveClient.executeQuery("SELECT * FROM users LIMIT 10");
```

### Kafka消息发送示例

```java
@Autowired
private KafkaProducerClient producerClient;

// 发送消息
producerClient.sendMessage("topic-name", "Hello, Kafka!");
```

### Spark Kafka流处理示例

```java
@Autowired
private SparkKafkaStreamingService streamingService;

// 启动流处理
streamingService.startKafkaStreaming(
    Arrays.asList("topic-name"),
    message -> System.out.println("收到消息: " + message)
);
``` 