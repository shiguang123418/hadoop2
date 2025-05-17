# 变更记录：从 Java 模拟数据迁移到 Python 数据模拟

## 移除的 Java 文件

以下 Java 模拟数据相关的文件已被移除：

1. `hadoop2_java/src/main/java/org/shiguang/service/impl/MockProducerService.java`
2. `hadoop2_java/src/main/java/org/shiguang/util/MockKafkaProducerUtil.java`
3. `hadoop2_java/src/main/java/org/shiguang/config/MockKafkaConfig.java`
4. `hadoop2_java/src/main/java/org/shiguang/config/MockHiveConfig.java`
5. `hadoop2_java/src/main/java/org/shiguang/config/MockHadoopConfig.java`

## 保留/重新添加的模拟实现（为解决兼容性问题）

由于Java 21与Spark 3.3.1之间的兼容性问题，以下模拟实现被保留/重新添加：

1. `hadoop2_java/src/main/java/org/shiguang/config/MockSparkConfig.java`
2. `hadoop2_java/src/main/java/org/shiguang/service/impl/MockKafkaStreamingServiceImpl.java`

这些文件提供了必要的模拟实现，使得应用程序能够在没有完全兼容的Spark环境下运行。

## 更新的 Java 文件

以下 Java 文件已更新，移除了对模拟服务的引用：

1. `hadoop2_java/src/main/java/org/shiguang/controller/KafkaProducerController.java`
   - 移除了对 `MockProducerService` 的依赖
   - 简化了数据生成逻辑，仅使用 `KafkaProducerUtil`

## 更新的配置文件

1. `hadoop2_java/src/main/resources/application.properties`
   - 启用了 Kafka：`kafka.enabled=true`
   - 启用了模拟Spark：`spark.use-mock=true`

2. `hadoop2_java/src/main/resources/application-dev.yaml`
   - 启用了 Kafka：`kafka.enabled: true`
   - 启用了 Kafka 流自动启动：`kafka.streaming.auto-start: true`
   - 启用了模拟Spark：`spark.use-mock: true`

## 新增的 Python 文件

以下 Python 文件已创建，用于模拟生成农业传感器数据：

1. `monishuju/sensor_data_producer.py`
   - 实现了农业传感器数据生成器
   - 支持多种传感器类型、区域和作物类型
   - 可生成正常和异常数据
   - 将数据发送至 Kafka

2. `monishuju/requirements.txt`
   - Python 依赖：kafka-python 和 python-dateutil

3. `monishuju/start_simulation.sh` 和 `monishuju/stop_simulation.sh`
   - 用于启动和停止数据模拟脚本的自动化脚本

4. `monishuju/docker-compose.yml`
   - 用于快速启动 Kafka 和 ZooKeeper 环境的 Docker Compose 配置

5. `monishuju/README.md`
   - 详细说明如何使用 Python 数据模拟器

## 架构更改

1. **数据生成方式变更**：
   - 之前：Java 后端模拟数据，直接写入数据库或发送至 Kafka
   - 现在：独立的 Python 服务生成数据，发送至 Kafka，Java 后端负责处理

2. **Kafka 配置**：
   - 启用了真实 Kafka 连接
   - 启用了自动启动 Kafka 流处理

3. **Spark 配置**：
   - 使用模拟 Spark 实现，解决 Java 21 兼容性问题
   - 保持流处理逻辑，但不实际初始化 Spark 上下文

4. **关键流程**：
   - Python 生成传感器数据 → 发送至 Kafka → Java 服务消费 Kafka 数据 → 模拟的Spark处理流程 → 数据存储与分析

## 下一步工作

1. 确保 Kafka 服务器正在运行
2. 如有必要，更新 Kafka 服务器地址
3. 启动 Python 数据模拟器
4. 启动 Java 后端服务
5. 验证数据是否正确流转
6. 后续可考虑升级Spark版本以兼容Java 21，或降级Java版本以兼容Spark 