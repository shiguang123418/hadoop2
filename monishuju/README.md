# 农业传感器数据模拟生成器

这个Python脚本用于模拟生成农业传感器数据，并将数据发送到Kafka以供后端处理。

## 功能特点

- 支持多种传感器类型：温度、湿度、光照、土壤湿度、pH值、二氧化碳、风速
- 支持不同农业区域和作物类型
- 可以生成正常数据和异常数据
- 支持批量生成数据和持续生成数据
- 可配置数据生成间隔和异常数据比例

## 目录结构

```
monishuju/
├── sensor_data_producer.py - 传感器数据生成脚本
├── requirements.txt - Python依赖
├── start_simulation.sh - 启动模拟脚本
├── stop_simulation.sh - 停止模拟脚本
└── docker-compose.yml - Kafka环境Docker配置
```

## 安装依赖

```bash
pip install -r requirements.txt
```

## 使用Docker启动Kafka环境

提供了Docker Compose配置文件，可以快速启动Kafka环境：

```bash
# 启动Kafka和ZooKeeper
docker-compose up -d

# 查看容器状态
docker-compose ps

# 停止Kafka环境
docker-compose down
```

**注意**：请确保修改`docker-compose.yml`中的`KAFKA_ADVERTISED_LISTENERS`配置为你的实际IP地址。

## 使用方法

### 使用自动化脚本

```bash
# 启动数据模拟器
./start_simulation.sh

# 自定义Kafka服务器和发送间隔
./start_simulation.sh --bootstrap-servers localhost:9092 --interval 2.0

# 停止数据模拟器
./stop_simulation.sh
```

### 手动运行Python脚本

```bash
# 持续发送数据到默认Kafka服务器(192.168.1.192:9092)
python sensor_data_producer.py

# 发送10条数据，间隔2秒
python sensor_data_producer.py --count 10 --interval 2

# 指定不同的Kafka服务器
python sensor_data_producer.py --bootstrap-servers localhost:9092

# 指定不同的主题
python sensor_data_producer.py --topic custom-topic
```

### 命令行参数

```
-b, --bootstrap-servers  Kafka服务器地址 (默认: 192.168.1.192:9092)
-t, --topic              Kafka主题名称 (默认: agriculture-sensor-data)
-c, --count              发送的数据条数，0表示持续发送 (默认: 0)
-i, --interval           发送间隔，单位秒 (默认: 1.0)
-a, --anomaly-rate       异常值比例，0-1之间 (默认: 0.05)
```

## 数据格式

发送到Kafka的数据格式为JSON，包含以下字段：

```json
{
  "sensorId": "SENSOR-1234",
  "sensorType": "温度",
  "region": "华北",
  "cropType": "小麦",
  "value": 25.6,
  "timestamp": "2023-05-20T14:30:45.123456",
  "unit": "°C",
  "description": "温度传感器读数 - 华北 - 小麦"
}
```

## 与Java后端集成

这个模拟器会将数据发送到Kafka，Java后端通过Kafka消费者接收数据并使用Spark Streaming进行实时处理。
请确保Java后端的`application-dev.yaml`中已启用Kafka：

```yaml
kafka:
  enabled: true
  bootstrap-servers: 192.168.1.192:9092
  # 其他配置...
```

## 连接问题排查

如果连接到Kafka失败，请检查：

1. Kafka服务是否正在运行
2. `bootstrap-servers`参数是否正确
3. 网络连接是否正常
4. Kafka主题是否存在

可以使用以下命令检查Kafka主题：

```bash
# 列出所有主题
docker exec -it kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# 创建新主题
docker exec -it kafka kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 3 --topic agriculture-sensor-data
``` 