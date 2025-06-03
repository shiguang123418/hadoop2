# 农业大数据平台 - Java后端

本模块是农业大数据平台的主要后端服务，基于Hadoop、Spark和Spring Boot构建。

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
- Kafka 3.x
- Log4j2

## 配置说明

### application.yaml

主配置文件包含通用设置：

```yaml
spring:
  application:
    name: hadoop2-java-backend
  profiles:
    active: dev
    
server:
  port: 8000
  servlet:
    context-path: /api
```

### application-dev.yaml

开发环境配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/agri_data?useSSL=false&serverTimezone=UTC
    username: root
    password: password
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true

# 日志配置
logging:
  level:
    root: INFO
    org.shiguang: DEBUG
```

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

### 使用Maven编译项目

```bash
mvn clean package
```

### 运行开发环境

```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

### 运行生产环境

```bash
java -jar target/hadoop2-java-backend-1.0.0.jar --spring.profiles.active=prod
```

## API接口说明

### HDFS相关接口

```
GET /api/hdfs/list?path={path} - 列出指定路径下的文件和目录
GET /api/hdfs/file?path={path} - 获取文件内容
POST /api/hdfs/upload - 上传文件到HDFS
DELETE /api/hdfs/delete?path={path} - 删除文件或目录
```

### Hive相关接口

```
GET /api/hive/tables - 获取所有表
GET /api/hive/schema?table={tableName} - 获取表结构
POST /api/hive/query - 执行HiveQL查询
POST /api/hive/create-table - 创建表
```

## 模块使用示例

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

## 部署说明

### 1. 设置环境变量

```bash
export HADOOP_HOME=/path/to/hadoop
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export SPARK_HOME=/path/to/spark
export HIVE_HOME=/path/to/hive
```

### 2. 配置数据库

```sql
CREATE DATABASE agri_data;
USE agri_data;
-- 创建必要的表
```

### 3. 构建并部署

```bash
mvn clean package
nohup java -jar target/hadoop2-java-backend-1.0.0.jar --spring.profiles.active=prod > app.log &
```

### 4. 使用Nginx反向代理

配置Nginx将`/api`路径代理到后端服务：

```nginx
location /api/ {
    proxy_pass http://localhost:8000/api/;
    proxy_set_header Host $host;
    proxy_set_header X-Real-IP $remote_addr;
}
``` 