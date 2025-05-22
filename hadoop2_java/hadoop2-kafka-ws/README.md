# 农业大数据平台 - Kafka和WebSocket模块 (Scala实现)

## 项目概述

本模块利用Scala语言和Spark Streaming技术实现了农业传感器数据实时处理系统，通过Kafka消费传感器数据，进行实时异常检测，并将结果保存到MySQL数据库及通过WebSocket推送给前端展示。

## 技术栈

- Scala 2.12
- Apache Spark 3.3.2
- Apache Kafka 3.4.0
- Java-WebSocket
- MySQL

## 功能特性

- 从Kafka实时消费农业传感器数据
- 使用Spark Streaming进行数据流处理
- 实时检测数据异常 (温度、湿度、土壤湿度、光照强度、CO2浓度)
- 将数据和异常记录保存到MySQL数据库
- 通过WebSocket实时推送数据和异常告警到前端

## 系统架构

```
传感器设备 -> Kafka -> Spark Streaming -> 数据处理 -> MySQL存储
                                       \-> 异常检测 -> WebSocket -> 前端展示
```

## 配置说明

配置文件位于`src/main/resources/application.yml`，主要配置项包括：

- Kafka配置 (服务器、主题、消费组)
- Spark配置 (主节点、序列化设置等)
- WebSocket服务器配置
- MySQL数据库配置
- 异常检测阈值配置

## 编译与运行

### 编译

```bash
cd hadoop2_java/hadoop2-kafka-ws
mvn clean package
```

编译后会在`target`目录下生成可执行的JAR文件。

### 运行

方式一：直接使用`java`命令启动：

```bash
java -jar target/hadoop2-kafka-ws-0.0.1.jar
```

方式二：使用`spark-submit`提交到Spark集群：

```bash
spark-submit \
  --class org.shiguang.AgricultureSensorApp \
  --master spark://shiguang:7077 \
  --deploy-mode client \
  --conf spark.serializer=org.apache.spark.serializer.KryoSerializer \
  --conf spark.kryo.registrator=org.shiguang.spark.KafkaKryoRegistrator \
  --conf spark.executor.memory=2g \
  target/hadoop2-kafka-ws-0.0.1.jar
```

## 数据格式

Kafka消息中传感器数据的JSON格式示例：

```json
{
  "deviceId": "sensor001",
  "timestamp": 1684209600000,
  "temperature": 28.5,
  "humidity": 70.2,
  "soilMoisture": 65.8,
  "lightIntensity": 120.5,
  "co2": 420.3
}
```

## WebSocket接口

WebSocket服务器会推送两种类型的数据：

1. 普通数据流 - type: "data"
2. 异常告警 - type: "anomaly"

前端可以通过WebSocket连接获取实时数据和告警信息，如：

```javascript
const ws = new WebSocket("ws://localhost:8090");
ws.onmessage = function(event) {
  const data = JSON.parse(event.data);
  if (data.type === "anomaly") {
    // 处理异常告警
  } else {
    // 处理普通数据
  }
};
```

## Kryo序列化器配置说明

为优化性能，本模块采用了Kryo序列化器进行序列化优化。Scala版本天然支持更好的序列化，避免了Java 8 Lambda表达式在序列化时的问题。

### 主要改进

1. **使用Kryo序列化器**：
   - 替代默认的Java序列化器，提高序列化性能
   - 显式注册所有相关类，避免序列化问题

2. **增强的类注册系统**：
   - 完善的KafkaKryoRegistrator类，负责注册所有需要序列化的类

3. **优化的Spark配置**：
   - 调整Kryo缓冲区大小，支持大对象序列化
   - 配置类加载优先级，解决类加载冲突

## 注意事项

- 确保Kafka、Spark和MySQL服务已正确配置并运行
- 对于大规模数据处理，请适当调整Spark相关参数
- 系统启动时会自动创建所需的数据库表 