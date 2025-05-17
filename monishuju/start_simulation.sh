#!/bin/bash

# 农业传感器数据模拟器启动脚本

# 默认参数
BOOTSTRAP_SERVERS="192.168.1.192:9092"
TOPIC="agriculture-sensor-data"
INTERVAL="1.0"
ANOMALY_RATE="0.05"
PID_FILE="simulation.pid"
LOG_FILE=""  # 默认不输出到日志文件

# 帮助信息
show_help() {
    echo "农业传感器数据模拟器启动脚本"
    echo "用法: $0 [选项]"
    echo ""
    echo "选项:"
    echo "  -b, --bootstrap-servers <地址>  Kafka服务器地址(默认: 192.168.1.192:9092)"
    echo "  -t, --topic <主题>              Kafka主题(默认: agriculture-sensor-data)"
    echo "  -i, --interval <秒>             发送间隔，秒(默认: 1.0)"
    echo "  -a, --anomaly-rate <比例>       异常值比例，0-1之间(默认: 0.05)"
    echo "  -l, --log-file <文件路径>       日志文件路径(默认: 不输出日志文件)"
    echo "  -h, --help                      显示此帮助信息"
    echo ""
    echo "示例:"
    echo "  $0 --bootstrap-servers localhost:9092 --interval 2.0"
}

# 解析命令行参数
while [[ $# -gt 0 ]]; do
    key="$1"
    case $key in
        -b|--bootstrap-servers)
            BOOTSTRAP_SERVERS="$2"
            shift
            shift
            ;;
        -t|--topic)
            TOPIC="$2"
            shift
            shift
            ;;
        -i|--interval)
            INTERVAL="$2"
            shift
            shift
            ;;
        -a|--anomaly-rate)
            ANOMALY_RATE="$2"
            shift
            shift
            ;;
        -l|--log-file)
            LOG_FILE="$2"
            shift
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            echo "未知选项: $1"
            show_help
            exit 1
            ;;
    esac
done

# 检查是否已经在运行
if [ -f "$PID_FILE" ]; then
    PID=$(cat "$PID_FILE")
    if ps -p "$PID" > /dev/null; then
        echo "模拟器已经在运行，进程ID: $PID"
        echo "如需重新启动，请先执行 ./stop_simulation.sh"
        exit 1
    else
        echo "上次模拟器进程异常退出，将重新启动"
        rm -f "$PID_FILE"
    fi
fi

# 构建命令
CMD="./sensor_data_producer.py"
CMD+=" --bootstrap-servers $BOOTSTRAP_SERVERS"
CMD+=" --topic $TOPIC"
CMD+=" --interval $INTERVAL"
CMD+=" --anomaly-rate $ANOMALY_RATE"

# 启动模拟器进程
echo "启动农业传感器数据模拟器..."
echo "Kafka服务器: $BOOTSTRAP_SERVERS"
echo "主题: $TOPIC"
echo "发送间隔: $INTERVAL 秒"
echo "异常值比例: $ANOMALY_RATE"

if [ -z "$LOG_FILE" ]; then
    # 不输出到日志文件，直接在后台运行
    nohup python $CMD > /dev/null 2>&1 &
else
    echo "日志输出到: $LOG_FILE"
    nohup python $CMD > "$LOG_FILE" 2>&1 &
fi

# 保存进程ID
echo $! > "$PID_FILE"
echo "模拟器已启动，进程ID: $(cat "$PID_FILE")"
echo "可通过 ./stop_simulation.sh 停止模拟器" 