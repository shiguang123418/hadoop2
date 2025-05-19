# 农业大数据可视化平台

基于Spark分析的农业大数据可视化平台，利用大数据技术实现对农业生产数据的采集、存储、处理和分析。

## 功能特点

- 农业数据采集与管理
- Spark批处理数据分析
- Kafka + Spark Streaming实时数据处理
- 农业数据可视化展示
- 异常数据检测与预警

## 技术栈

- **后端**: Spring Boot 3.3.2
- **大数据处理**: 
  - Apache Hadoop 3.4.0
  - Apache Spark 3.3.1
  - Apache Hive 3.1.2
  - Apache Kafka
- **数据库**: MySQL 8.0
- **前端**: Vue.js, ECharts (在hadoop2_web项目中)

## 系统架构

```
+-------------+      +----------+      +---------------+      +------------+
| 数据源       |  ->  | Kafka    |  ->  | Spark         |  ->  | 数据库      |
| (传感器等)   |      | 主题     |      | Streaming     |      | (MySQL)    |
+-------------+      +----------+      +---------------+      +------------+
                                              |
                                              v
                                       +---------------+
                                       | Spark分析     |
                                       | 批处理        |
                                       +---------------+
                                              |
                                              v
+-------------+      +---------------+      +------------+
| 用户        |  <-  | Web前端       |  <-  | REST API   |
| (浏览器)    |      | (Vue.js)     |      | 接口       |
+-------------+      +---------------+      +------------+
```

## Kafka + Spark Streaming 实时数据处理

系统使用Kafka作为实时数据流的消息队列，结合Spark Streaming进行实时数据处理。主要实现：

1. 农业传感器数据实时接入Kafka
2. Spark Streaming消费Kafka数据
3. 实时数据分析（均值计算、异常检测等）
4. 分析结果存储到数据库
5. 异常数据实时预警

### Kafka主题

- `agriculture-sensor-data`: 传感器数据主题
- `agriculture-weather-data`: 气象数据主题
- `agriculture-market-data`: 市场数据主题

### 接口文档

#### Kafka流控制接口

- 启动Kafka流处理: `POST /api/kafka/start?topics=topic1,topic2`
- 停止Kafka流处理: `POST /api/kafka/stop`
- 查询流处理状态: `GET /api/kafka/status`

#### 数据生产接口（测试用）

- 发送单条随机数据: `POST /api/producer/send`
- 发送批量随机数据: `POST /api/producer/send-batch?count=10`

#### 数据分析接口

- 获取最新传感器数据: `GET /api/analytics/latest`
- 获取区域平均值: `GET /api/analytics/region-averages`
- 获取作物平均值: `GET /api/analytics/crop-averages`
- 获取异常数据: `GET /api/analytics/anomalies`
- 获取数据摘要: `GET /api/analytics/summary`

## 部署指南

### 前置要求

- JDK 21+
- Apache Hadoop 3.x
- Apache Kafka
- MySQL 8.0+

### 配置说明

项目使用YAML格式的配置文件，主要配置文件为`application-dev.yaml`，包含以下配置：

- 服务器配置
- 数据库配置
- Hadoop配置
- Hive配置
- Spark配置
- Kafka配置

### 启动步骤

1. 确保Hadoop、Kafka服务已启动
2. 创建相应的MySQL数据库和表
3. 配置`application-dev.yaml`中的连接参数
4. 运行应用：`mvn spring-boot:run -Dspring.profiles.active=dev`

## Spark数据分析功能

- 统计分析：计算农作物产量均值、方差等基本统计量
- 关联分析：挖掘不同数据指标之间的关系
- 时间序列分析：分析农业数据的历史趋势
- 聚类分析：对作物生长条件进行聚类分析

# 农业大数据平台

## 配置文件说明

本项目支持多环境配置，通过Spring Profiles实现。默认使用开发环境配置。

### 可用的配置文件

- `application.yaml`: 主配置文件，包含通用配置
- `application-dev.yaml`: 开发环境配置
- `application-test.yaml`: 测试环境配置
- `application-prod.yaml`: 生产环境配置

### 如何切换配置文件

#### 方法1: 通过命令行参数

```bash
# 使用开发环境配置
java -jar springboot-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 使用测试环境配置
java -jar springboot-0.0.1-SNAPSHOT.jar --spring.profiles.active=test

# 使用生产环境配置
java -jar springboot-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

#### 方法2: 通过环境变量

```bash
# Windows
set SPRING_PROFILES_ACTIVE=dev
java -jar springboot-0.0.1-SNAPSHOT.jar

# Linux/Mac
export SPRING_PROFILES_ACTIVE=dev
java -jar springboot-0.0.1-SNAPSHOT.jar
```

#### 方法3: 通过application.yaml配置

在`application.yaml`中设置默认激活的配置文件：

```yaml
spring:
  profiles:
    active: dev
```

### 配置文件差异

#### 开发环境 (dev)
- 使用MySQL数据库
- 显示SQL语句
- 自动更新数据库结构
- 详细的日志输出

#### 测试环境 (test)
- 使用H2内存数据库
- 每次启动重新创建表
- 详细的日志输出
- 适合单元测试和集成测试

#### 生产环境 (prod)
- 使用MySQL数据库
- 不显示SQL语句
- 不自动更新数据库结构
- 日志输出到文件
- 更安全的配置

### 查看当前配置

启动应用后，可以通过以下API查看当前配置：

- `/api/profile/current`: 查看当前激活的配置文件
- `/api/profile/info`: 查看配置信息（不包含敏感信息） 

---时光123418