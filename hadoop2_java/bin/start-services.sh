#!/bin/bash

# 启动农业传感器监控系统服务脚本

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
KAFKA_BIN_DIR="$PROJECT_DIR/kafka/bin"
ZOOKEEPER_DATA_DIR="$PROJECT_DIR/data/zookeeper"
KAFKA_DATA_DIR="$PROJECT_DIR/data/kafka"
KAFKA_WS_DIR="$PROJECT_DIR/hadoop2-kafka-ws"

# 颜色定义
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # 无颜色

# 显示状态消息
show_status() {
  echo -e "${GREEN}[INFO]${NC} $1"
}

# 显示警告消息
show_warning() {
  echo -e "${YELLOW}[WARNING]${NC} $1"
}

# 显示错误消息
show_error() {
  echo -e "${RED}[ERROR]${NC} $1"
}

# 创建所需的数据目录
mkdir -p "$ZOOKEEPER_DATA_DIR"
mkdir -p "$KAFKA_DATA_DIR"

# 启动 ZooKeeper
start_zookeeper() {
  show_status "正在启动 ZooKeeper..."
  if [ -f "$KAFKA_BIN_DIR/zookeeper-server-start.sh" ]; then
    "$KAFKA_BIN_DIR/zookeeper-server-start.sh" -daemon "$PROJECT_DIR/kafka/config/zookeeper.properties"
    if [ $? -eq 0 ]; then
      show_status "ZooKeeper 启动成功"
    else
      show_error "ZooKeeper 启动失败"
      exit 1
    fi
  else
    show_error "未找到 ZooKeeper 启动脚本"
    exit 1
  fi
}

# 启动 Kafka
start_kafka() {
  show_status "正在启动 Kafka..."
  if [ -f "$KAFKA_BIN_DIR/kafka-server-start.sh" ]; then
    "$KAFKA_BIN_DIR/kafka-server-start.sh" -daemon "$PROJECT_DIR/kafka/config/server.properties"
    if [ $? -eq 0 ]; then
      show_status "Kafka 启动成功"
    else
      show_error "Kafka 启动失败"
      exit 1
    fi
  else
    show_error "未找到 Kafka 启动脚本"
    exit 1
  fi
}

# 创建所需的 Kafka 主题
create_topics() {
  show_status "正在创建 Kafka 主题..."
  if [ -f "$KAFKA_BIN_DIR/kafka-topics.sh" ]; then
    # 检查主题是否存在
    "$KAFKA_BIN_DIR/kafka-topics.sh" --bootstrap-server localhost:9092 --list | grep -q "agriculture-sensor-data"
    if [ $? -eq 0 ]; then
      show_warning "主题 'agriculture-sensor-data' 已存在"
    else
      "$KAFKA_BIN_DIR/kafka-topics.sh" --bootstrap-server localhost:9092 --create --topic agriculture-sensor-data --partitions 3 --replication-factor 1
      if [ $? -eq 0 ]; then
        show_status "主题 'agriculture-sensor-data' 创建成功"
      else
        show_error "主题创建失败"
        exit 1
      fi
    fi
  else
    show_error "未找到 Kafka 主题管理脚本"
    exit 1
  fi
}

# 启动传感器处理应用
start_sensor_processor() {
  show_status "正在启动传感器处理应用..."
  cd "$KAFKA_WS_DIR" || exit 1
  if [ -f "target/hadoop2-kafka-ws-1.0-SNAPSHOT.jar" ]; then
    nohup java -jar target/hadoop2-kafka-ws-1.0-SNAPSHOT.jar > logs/application.log 2>&1 &
    if [ $? -eq 0 ]; then
      show_status "传感器处理应用启动成功"
    else
      show_error "传感器处理应用启动失败"
      exit 1
    fi
  else
    show_warning "传感器处理应用JAR文件不存在，尝试构建项目..."
    mvn clean package -DskipTests
    if [ $? -eq 0 ]; then
      nohup java -jar target/hadoop2-kafka-ws-1.0-SNAPSHOT.jar > logs/application.log 2>&1 &
      if [ $? -eq 0 ]; then
        show_status "传感器处理应用构建并启动成功"
      else
        show_error "传感器处理应用启动失败"
        exit 1
      fi
    else
      show_error "传感器处理应用构建失败"
      exit 1
    fi
  fi
}

# 检查是否已创建日志目录
if [ ! -d "$KAFKA_WS_DIR/logs" ]; then
  mkdir -p "$KAFKA_WS_DIR/logs"
fi

# 启动所有服务
start_zookeeper
sleep 5
start_kafka
sleep 10
create_topics
sleep 5
start_sensor_processor

show_status "所有服务已启动"
echo -e "${GREEN}==================================================${NC}"
echo -e "${GREEN} 农业传感器监控系统服务已启动${NC}"
echo -e "${GREEN} WebSocket服务运行在: ws://localhost:8090${NC}"
echo -e "${GREEN} REST API服务运行在: http://localhost:8080${NC}"
echo -e "${GREEN}==================================================${NC}" 