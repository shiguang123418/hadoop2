# 农业大数据可视化平台

基于Spark分析的农业大数据可视化平台，利用大数据技术实现对农业生产数据的采集、存储、处理和分析。

## 项目结构

本项目由以下三个主要部分组成：

- **hadoop2_java**: 主后端服务，处理HDFS和Hive相关功能
  - 后端API控制器：处理来自前端的请求，提供数据访问接口
  - 业务逻辑服务：实现核心业务功能和数据处理逻辑
  - Spark批处理分析实现：基于Spark进行数据批量分析的核心类
  - 数据模型：定义系统使用的数据结构和实体类
  - 工具类：提供通用功能和辅助方法
  
- **hadoop2_ws**: WebSocket后端服务，处理Kafka和实时数据流
  - Kafka消息处理：处理Kafka消息的生产和消费
  - Spark Streaming实现：利用Spark Streaming进行实时数据处理
  - WebSocket服务实现：提供WebSocket通信功能，实现实时数据推送
  - 测试数据生产者：用于生成测试数据的模拟器

- **hadoop2_web**: 前端Vue应用，负责数据可视化展示
  - 页面视图组件：实现各个功能页面的视图
  - 通用组件：可复用的UI组件
  - 图表组件：使用ECharts实现的各类数据可视化组件
  - 后端API调用：封装与后端的通信逻辑

## 功能特点

### 1. 农业数据采集与管理
项目实现了多源农业数据的采集与管理功能，包括：
- 传感器数据采集（土壤湿度、温度等）
- 农事操作记录（种植、施肥、灌溉等）
- 市场交易数据（农产品价格、销量等）

主要实现：
- 数据采集服务：负责从各种数据源收集农业数据
- 传感器数据模型：定义传感器数据的结构和格式

### 2. 数据预处理
实现了农业数据的清洗、转换和集成功能：
- 异常值处理：识别并修正土壤湿度、温度等指标中的异常值
- 缺失值填充：基于历史数据或统计方法填充缺失的传感器数据
- 数据格式转换：统一不同来源数据的格式，如日期标准化

主要实现：
- 数据预处理器：实现数据清洗、转换和集成的核心逻辑
- 数据清洗工具：提供各种数据清洗和预处理方法

### 3. Spark批处理数据分析
基于Spark实现对农业数据的批量分析处理，包括：
- 统计分析：计算各地区农作物产量均值、方差等统计指标
- 关联分析：分析施肥量与作物产量的相关性，挖掘最佳农事操作参数
- 时间序列分析：分析气象数据趋势，为农业生产提供预测支持

主要实现：
- 统计分析模块：实现各种统计指标的计算
- 关联分析模块：分析不同因素间的相关性
- 时间序列分析模块：处理时间序列数据并进行趋势预测
- 分析结果API：提供分析结果的查询接口

### 4. Kafka + Spark Streaming实时数据处理
实现基于Kafka和Spark Streaming的实时数据流处理框架，实现：
- 实时数据接收与解析
- 滑动窗口计算与统计
- 异常数据实时检测
- 预警信息生成与推送

主要实现：
- Spark Streaming作业：定义和配置Spark Streaming处理任务
- 异常检测器：实时检测数据异常并生成预警
- Kafka控制API：提供Kafka流处理的控制接口

### 5. 农业数据可视化展示
基于Vue和ECharts实现丰富的农业数据可视化界面：
- 折线图：展示作物生长周期内的关键指标变化趋势
- 柱状图：对比不同地区、不同品种农作物的产量指标
- 地图可视化：展示不同地区的土壤肥力、病虫害分布情况
- 实时仪表盘：显示温室环境参数等实时监测数据

主要实现：
- 主控制面板：系统的主要入口，展示关键指标和监控数据
- 作物生长趋势图：展示农作物生长过程中的各项指标变化
- 地区分布地图：使用地图形式展示区域农业数据
- 实时监控组件：实时显示传感器数据和环境参数

## 技术栈

- **后端**: Spring Boot 2.7.17
- **大数据处理**:  
  - Apache Hadoop 3.4.0
  - Apache Spark 3.5.2
  - Apache Hive 3.1.2
  - Apache Kafka
- **数据库**: MySQL 8.0
- **前端**: Vue 3, ElementPlus, ECharts
- **Web服务器**: Nginx

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
| (浏览器)    |      | (Vue 3)      |      | 接口       |
+-------------+      +---------------+      +------------+
```

## 数据流处理详解

### 数据收集流程
1. 从农业传感器、农业生产管理系统、市场交易平台等多源获取数据
2. 通过API网关和数据接入服务将数据写入Kafka主题
3. 数据同时保存至HDFS用于历史数据分析

主要实现：
- 数据采集服务：负责从各种数据源采集农业数据
- 传感器数据生产者：模拟或处理传感器数据并发送到Kafka

### 实时数据处理流程
1. Spark Streaming从Kafka主题消费数据
2. 实时数据清洗和转换，剔除异常值
3. 滑动窗口计算（如10分钟窗口的平均值计算）
4. 异常值检测和预警信息生成
5. 处理结果通过WebSocket推送到前端

主要实现：
- 流处理器：处理实时数据流的核心组件
- WebSocket处理器：通过WebSocket将实时数据推送给前端

### 批处理分析流程
1. 从HDFS读取历史数据
2. 执行数据清洗和预处理
3. 利用Spark SQL进行复杂查询和统计分析
4. 使用Spark MLlib进行数据挖掘和模型训练
5. 分析结果写入MySQL数据库供前端查询

主要实现：
- 批处理器：执行批量数据处理和分析
- Spark SQL处理器：使用Spark SQL进行数据查询和分析
- 模型训练器：使用机器学习算法训练预测模型

## Kafka主题

- `agriculture-sensor-data`: 传感器数据主题
  - 包含字段：sensorId, timestamp, sensorType, value, location, cropType
  - 相关模型类：传感器消息模型

- `agriculture-weather-data`: 气象数据主题
  - 包含字段：timestamp, location, temperature, humidity, rainfall, windSpeed
  - 相关模型类：天气消息模型

- `agriculture-market-data`: 市场数据主题
  - 包含字段：timestamp, cropType, price, volume, marketLocation
  - 相关模型类：市场消息模型

## API接口文档

### Kafka流控制接口

- 启动Kafka流处理: `POST /api/kafka/start?topics=topic1,topic2`
- 停止Kafka流处理: `POST /api/kafka/stop`
- 查询流处理状态: `GET /api/kafka/status`

### 数据生产接口（测试用）

- 发送单条随机数据: `POST /api/producer/send`
- 发送批量随机数据: `POST /api/producer/send-batch?count=10`

### 数据分析接口

- 获取最新传感器数据: `GET /api/analytics/latest`
- 获取区域平均值: `GET /api/analytics/region-averages`
- 获取作物平均值: `GET /api/analytics/crop-averages`
- 获取异常数据: `GET /api/analytics/anomalies`
- 获取数据摘要: `GET /api/analytics/summary`
- 获取关联分析结果: `GET /api/analytics/correlation?factorA=rainfall&factorB=yield`
- 获取时间序列预测: `GET /api/analytics/forecast?metric=temperature&days=7`

## 可视化界面说明

系统前端界面包括以下主要模块：

1. **实时监控面板**：
   - 展示各类传感器实时数据
   - 主要文件：实时监控视图组件

2. **数据分析报表**：
   - 包含产量分析、作物生长趋势等统计图表
   - 主要文件：数据分析视图组件

3. **地区分布地图**：
   - 展示不同区域的农业生产情况
   - 主要文件：地区分布视图组件

4. **异常预警中心**：
   - 显示异常数据和预警信息
   - 主要文件：预警中心视图组件

5. **数据管理界面**：
   - 提供数据查询、筛选和导出功能
   - 主要文件：数据管理视图组件

## 部署指南

### 前置要求

- JDK 1.8
- Apache Hadoop 3.x
- Apache Spark 3.x
- Apache Kafka
- MySQL 8.0+
- Nginx

### 配置说明

#### 后端配置

项目使用YAML格式的配置文件，主要配置文件为`application-dev.yaml`，包含以下配置：

- 服务器配置
  - 主要参数：server.port、server.servlet.context-path等

- 数据库配置
  - 主要参数：spring.datasource.url、spring.datasource.username等

- Hadoop配置
  - 主要参数：fs.defaultFS、hadoop.home.dir等

- Hive配置
  - 主要参数：hive.metastore.uris、hive.exec.scratchdir等

- Spark配置
  - 主要参数：spark.app.name、spark.master、spark.executor.memory等

- Kafka配置
  - 主要参数：spring.kafka.bootstrap-servers、spring.kafka.consumer.group-id等

#### WebSocket服务配置

WebSocket服务配置包括：
- WebSocket服务端口
- 连接超时设置
- 消息大小限制
- 允许的客户端来源

#### Nginx配置

前端应用通过Nginx进行部署，Nginx配置样例：

```nginx
server {
    listen 5173;
    server_name your_server_name;
    index index.html;
    root [前端构建目录路径]/dist;
    
    # 处理前端SPA路由
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API代理设置
    location /api1/ {
        proxy_pass http://localhost:8000/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    location /api_ws/ {
        proxy_pass http://localhost:8001/api/ws/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket支持
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
    
    # 静态资源缓存设置
    location ~* \.(js|css)$ {
        expires 12h;
    }
    
    location ~* \.(jpg|jpeg|png|gif|ico|svg)$ {
        expires 30d;
    }
}
```

### 启动步骤

1. 启动Hadoop、Spark和Kafka服务
   ```bash
   # 启动HDFS
   hadoop/sbin/start-dfs.sh
   # 启动YARN
   hadoop/sbin/start-yarn.sh
   # 启动Kafka
   kafka/bin/kafka-server-start.sh kafka/config/server.properties
   ```

2. 初始化数据库
   ```bash
   # 创建MySQL数据库
   mysql -u root -p < 项目路径/hadoop2_java/src/main/resources/sql/init.sql
   # 导入测试数据（可选）
   mysql -u root -p agriculture_db < 项目路径/hadoop2_java/src/main/resources/sql/test_data.sql
   ```

3. 启动主后端服务
   ```bash
   cd 项目路径/hadoop2_java
   mvn spring-boot:run -Dspring.profiles.active=dev
   ```

4. 启动WebSocket后端服务
   ```bash
   cd 项目路径/hadoop2_ws
   mvn spring-boot:run -Dspring.profiles.active=dev
   ```

5. 构建并部署前端应用
   ```bash
   cd 项目路径/hadoop2_web
   npm install
   npm run build
   ```

6. 配置并启动Nginx
   ```bash
   # 复制Nginx配置文件
   sudo cp 项目路径/config/nginx/agriculture.conf /etc/nginx/conf.d/
   # 重启Nginx使配置生效
   sudo nginx -s reload
   ```

### 大数据配置关键点

#### HDFS配置
- 确保HDFS已正确格式化并启动
- 重要目录：
  - 原始数据存储：`hdfs://localhost:9000/agriculture/raw`
  - 处理后数据：`hdfs://localhost:9000/agriculture/processed`
  - 分析结果：`hdfs://localhost:9000/agriculture/results`

#### Hive配置
- 确保Hive元存储数据库(MySQL)已配置
- 主要表：
  - `agriculture_db.sensor_data`: 传感器数据表
  - `agriculture_db.weather_data`: 气象数据表
  - `agriculture_db.crop_data`: 作物数据表
  - `agriculture_db.market_data`: 市场数据表

#### Spark作业提交
- 批处理作业提交脚本：在项目scripts目录下的submit-batch-job.sh
- 流处理作业提交脚本：在项目scripts目录下的submit-streaming-job.sh

## 多环境配置

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
java -jar 项目路径/hadoop2_java/target/springboot-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev

# 使用测试环境配置
java -jar 项目路径/hadoop2_java/target/springboot-0.0.1-SNAPSHOT.jar --spring.profiles.active=test

# 使用生产环境配置
java -jar 项目路径/hadoop2_java/target/springboot-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

#### 方法2: 通过环境变量

```bash
# Windows
set SPRING_PROFILES_ACTIVE=dev
java -jar 项目路径/hadoop2_java/target/springboot-0.0.1-SNAPSHOT.jar

# Linux/Mac
export SPRING_PROFILES_ACTIVE=dev
java -jar 项目路径/hadoop2_java/target/springboot-0.0.1-SNAPSHOT.jar
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
- Spark本地模式运行
- 较小的批处理间隔

#### 测试环境 (test)
- 使用H2内存数据库
- 每次启动重新创建表
- 详细的日志输出
- 适合单元测试和集成测试
- 模拟的Kafka生产者和消费者

#### 生产环境 (prod)
- 使用MySQL数据库
- 不显示SQL语句
- 不自动更新数据库结构
- 日志输出到文件
- 更安全的配置
- Spark集群模式运行
- 优化的批处理间隔和资源配置

### 查看当前配置

启动应用后，可以通过以下API查看当前配置：

- `/api/profile/current`: 查看当前激活的配置文件
- `/api/profile/info`: 查看配置信息（不包含敏感信息）

## 系统监控

系统提供了多层次的监控功能：

1. **应用监控**：
   - Spring Boot Actuator端点：`/actuator/health`、`/actuator/metrics`等
   - 自定义健康检查：`/api/monitor/health`

2. **Spark作业监控**：
   - Spark UI：http://localhost:4040
   - 作业统计API：`/api/spark/jobs`、`/api/spark/stages`

3. **Kafka监控**：
   - 主题状态API：`/api/kafka/topics-status`
   - 消费者组监控：`/api/kafka/consumer-groups`

4. **系统资源监控**：
   - CPU使用率：`/api/monitor/cpu`
   - 内存使用率：`/api/monitor/memory`
   - 磁盘使用率：`/api/monitor/disk`

## 常见问题解决

1. **Kafka连接问题**：
   - 检查Kafka服务是否正常运行
   - 验证配置文件中的bootstrap-servers设置
   - 查看项目日志目录下的kafka-connect.log日志

2. **Spark作业失败**：
   - 检查日志文件：项目日志目录下的spark-*.log
   - 查看Spark UI中的作业状态和错误信息
   - 验证Spark配置和资源分配

3. **前端无法连接WebSocket**：
   - 检查WebSocket服务是否正常运行
   - 验证Nginx的WebSocket代理配置
   - 查看浏览器控制台的连接错误信息

---时光123418