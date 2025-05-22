#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
农业传感器数据生成脚本
生成模拟的农业传感器数据并发送到Kafka
"""

import json
import random
import time
from datetime import datetime
import argparse
import sys

try:
    from kafka import KafkaProducer
except ImportError:
    print("未安装kafka-python库。请运行: pip install kafka-python")
    sys.exit(1)
# 修改默认地址
host="localhost"

# 命令行参数
parser = argparse.ArgumentParser(description='农业传感器数据生成器')
parser.add_argument('--bootstrap-servers', default=f'{host}:9092', help='Kafka服务器地址')
parser.add_argument('--topic', default='agriculture_sensors', help='Kafka主题')
parser.add_argument('--interval', type=float, default=1.0, help='数据生成间隔(秒)')
parser.add_argument('--count', type=int, default=0, help='生成数据条数(0表示无限)')
parser.add_argument('--anomaly-rate', type=float, default=0.05, help='异常数据生成率(0-1)')
args = parser.parse_args()

# 传感器ID列表
SENSOR_IDS = ["sensor-001", "sensor-002", "sensor-003", "sensor-004", "sensor-005"]

# 初始化Kafka生产者
try:
    producer = KafkaProducer(
        bootstrap_servers=args.bootstrap_servers,
        value_serializer=lambda v: json.dumps(v).encode('utf-8'),
        key_serializer=lambda v: v.encode('utf-8') if v else None
    )
    print(f"已连接到Kafka: {args.bootstrap_servers}")
except Exception as e:
    print(f"连接Kafka失败: {e}")
    sys.exit(1)

# 生成数据并发送到Kafka
try:
    count = 0
    while args.count == 0 or count < args.count:
        for sensor_id in SENSOR_IDS:
            # 确定是否生成异常数据
            is_anomaly = random.random() < args.anomaly_rate
            
            # 根据是否为异常数据生成不同范围的值
            if is_anomaly:
                temperature = random.uniform(5, 40)
                humidity = random.uniform(20, 95)
                soil_moisture = random.uniform(5, 95)
                light_intensity = random.uniform(100, 1200)
            else:
                temperature = random.uniform(15, 28)
                humidity = random.uniform(40, 70)
                soil_moisture = random.uniform(20, 45)
                light_intensity = random.uniform(400, 700)
            
            # 生成Scala应用兼容的传感器数据格式
            data = {
                "sensorId": sensor_id,
                "timestamp": int(time.time() * 1000),  # 毫秒级时间戳
                "temperature": round(temperature, 2),
                "humidity": round(humidity, 2),
                "soilMoisture": round(soil_moisture, 2),
                "lightIntensity": round(light_intensity, 2),
                "location": f"Field-{sensor_id.split('-')[1]}",
                "batteryLevel": round(random.uniform(50, 100), 2)
            }
            
            # 发送到Kafka
            future = producer.send(args.topic, key=sensor_id, value=data)
            count += 1
            
            # 打印发送的数据
            print(f"发送数据: {json.dumps(data)}")
            
        # 批量发送
        producer.flush()
        
        if count % 20 == 0:
            print(f"已发送 {count} 条数据记录到主题 {args.topic}")
        
        # 如果指定了条数且已达到，则退出
        if args.count > 0 and count >= args.count:
            break
            
        # 等待指定的间隔时间
        time.sleep(args.interval)
        
except KeyboardInterrupt:
    print("\n中断数据生成")
finally:
    producer.close()
    print(f"总共发送了 {count} 条数据记录")
    print("数据生成器已停止") 