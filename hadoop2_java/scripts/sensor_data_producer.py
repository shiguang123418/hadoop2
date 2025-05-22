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
host="hadoop"

# 命令行参数
parser = argparse.ArgumentParser(description='农业传感器数据生成器')
parser.add_argument('--bootstrap-servers', default=f'{host}:9092', help='Kafka服务器地址')
parser.add_argument('--topic', default='agriculture_sensors', help='Kafka主题')
parser.add_argument('--interval', type=float, default=2.0, help='数据生成间隔(秒)')
parser.add_argument('--count', type=int, default=0, help='生成数据条数(0表示无限)')
parser.add_argument('--anomaly-rate', type=float, default=0.05, help='异常数据生成率(0-1)')
args = parser.parse_args()

# 传感器ID和区域映射
SENSOR_CONFIG = [
    {"id": "sensor-001", "region": "华东", "location": "华东-稻田", "crop": "水稻"},
    {"id": "sensor-002", "region": "华南", "location": "华南-果园", "crop": "柑橘"},
    {"id": "sensor-003", "region": "华中", "location": "华中-菜地", "crop": "蔬菜"},
    {"id": "sensor-004", "region": "华北", "location": "华北-麦田", "crop": "小麦"},
    {"id": "sensor-005", "region": "西北", "location": "西北-葡萄园", "crop": "葡萄"},
    {"id": "sensor-006", "region": "西南", "location": "西南-茶园", "crop": "茶叶"},
    {"id": "sensor-007", "region": "东北", "location": "东北-玉米地", "crop": "玉米"},
    {"id": "sensor-008", "region": "新疆", "location": "新疆-棉田", "crop": "棉花"}
]

# 传感器类型及其参数范围配置
SENSOR_PARAMS = {
    "temperature": {
        "normal": (15, 28),  # 正常范围
        "anomaly": (5, 40),   # 异常范围
        "unit": "°C"
    },
    "humidity": {
        "normal": (40, 70),
        "anomaly": (20, 95),
        "unit": "%"
    },
    "soilMoisture": {
        "normal": (20, 45),
        "anomaly": (5, 95),
        "unit": "%"
    },
    "lightIntensity": {
        "normal": (400, 700),
        "anomaly": (100, 1200),
        "unit": "lux"
    },
    "co2Level": {
        "normal": (350, 450),
        "anomaly": (300, 1000),
        "unit": "ppm"
    },
    "batteryLevel": {
        "normal": (60, 100),
        "anomaly": (5, 60),
        "unit": "%"
    }
}

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
        for sensor in SENSOR_CONFIG:
            # 确定是否生成异常数据
            is_anomaly = random.random() < args.anomaly_rate
            
            # 生成传感器数据
            data = {
                "sensorId": sensor["id"],
                "timestamp": int(time.time() * 1000),  # 毫秒级时间戳
                "location": sensor["location"],
                "region": sensor["region"],
                "cropType": sensor["crop"],
                "isAnomalyDetected": is_anomaly
            }
            
            # 为每种传感器类型生成数据
            for param_name, param_config in SENSOR_PARAMS.items():
                # 根据是否为异常数据生成不同范围的值
                value_range = param_config["anomaly"] if is_anomaly else param_config["normal"]
                
                # 为某些特定类型强制生成异常值（增加多样性）
                if param_name == "batteryLevel" and sensor["id"] in ["sensor-003", "sensor-007"] and random.random() < 0.3:
                    value = random.uniform(5, 20)  # 电池电量低
                    data[param_name + "Anomaly"] = True
                elif param_name == "temperature" and sensor["id"] in ["sensor-002", "sensor-006"] and random.random() < 0.3:
                    value = random.uniform(35, 40)  # 温度过高
                    data[param_name + "Anomaly"] = True
                elif param_name == "soilMoisture" and sensor["id"] in ["sensor-004", "sensor-008"] and random.random() < 0.3:
                    value = random.uniform(5, 15)  # 土壤过干
                    data[param_name + "Anomaly"] = True
                else:
                    value = random.uniform(value_range[0], value_range[1])
                    data[param_name + "Anomaly"] = is_anomaly and (value < param_config["normal"][0] or value > param_config["normal"][1])
                
                # 添加传感器数据和单位
                data[param_name] = round(value, 2)
                data[param_name + "Unit"] = param_config["unit"]
            
            # 发送到Kafka
            future = producer.send(args.topic, key=sensor["id"], value=data)
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