#!/bin/bash

# 农业传感器数据模拟器停止脚本

PID_FILE="simulation.pid"

if [ ! -f "$PID_FILE" ]; then
    echo "未找到模拟器进程ID文件，模拟器可能未启动"
    exit 1
fi

PID=$(cat "$PID_FILE")

if ! ps -p "$PID" > /dev/null; then
    echo "模拟器进程(PID: $PID)已不存在，可能已停止"
    rm -f "$PID_FILE"
    exit 0
fi

echo "停止模拟器进程(PID: $PID)..."
kill "$PID"

# 等待进程停止
COUNT=0
MAX_WAIT=10
while ps -p "$PID" > /dev/null; do
    COUNT=$((COUNT + 1))
    if [ $COUNT -ge $MAX_WAIT ]; then
        echo "进程未能在规定时间内停止，尝试强制终止..."
        kill -9 "$PID"
        break
    fi
    echo "等待进程停止... ($COUNT/$MAX_WAIT)"
    sleep 1
done

# 再次检查进程是否已停止
if ps -p "$PID" > /dev/null; then
    echo "无法停止模拟器进程，请手动终止: kill -9 $PID"
    exit 1
else
    echo "模拟器进程已停止"
    rm -f "$PID_FILE"
    exit 0
fi 