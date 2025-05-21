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
parser.add_argument('--topic', default='agriculture-sensor-data', help='Kafka主题')
parser.add_argument('--interval', type=float, default=1.0, help='数据生成间隔(秒)')
parser.add_argument('--farms', type=int, default=3, help='农场数量')
parser.add_argument('--sensors-per-farm', type=int, default=5, help='每个农场的传感器数量')
parser.add_argument('--anomaly-rate', type=float, default=0.05, help='异常数据生成率(0-1)')
args = parser.parse_args()

# 传感器类型定义
SENSOR_TYPES = {
    'temperature': {
        'unit': '°C',
        'normal_range': (15, 28),
        'anomaly_range': (5, 40),
    },
    'humidity': {
        'unit': '%',
        'normal_range': (40, 70),
        'anomaly_range': (20, 95),
    },
    'soil_moisture': {
        'unit': '%',
        'normal_range': (20, 45),
        'anomaly_range': (5, 80),
    },
    'light_intensity': {
        'unit': 'lux',
        'normal_range': (400, 700),
        'anomaly_range': (100, 1200),
    },
    'co2': {
        'unit': 'ppm',
        'normal_range': (350, 450),
        'anomaly_range': (300, 800),
    },
    'ph': {
        'unit': 'pH',
        'normal_range': (6.0, 7.5),
        'anomaly_range': (4.0, 9.0),
    }
}

# 农作物类型
CROP_TYPES = ['corn', 'wheat', 'rice', 'soybean', 'cotton', 'vegetables']

# 地区
REGIONS = ['north', 'south', 'east', 'west', 'central']

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

# 生成传感器列表
sensors = []
for farm_id in range(1, args.farms + 1):
    farm_id_str = f"farm_{farm_id:03d}"
    crop_type = random.choice(CROP_TYPES)
    region = random.choice(REGIONS)
    
    for sensor_id in range(1, args.sensors_per_farm + 1):
        for sensor_type in SENSOR_TYPES.keys():
            sensor_id_str = f"sensor_{farm_id:03d}_{sensor_id:03d}_{sensor_type}"
            sensors.append({
                'sensor_id': sensor_id_str,
                'sensor_type': sensor_type,
                'farm_id': farm_id_str,
                'crop_type': crop_type,
                'region': region,
                'location': f"Field-{sensor_id:02d}"
            })

print(f"已生成 {len(sensors)} 个传感器")

# 生成数据并发送到Kafka
try:
    count = 0
    while True:
        for sensor in sensors:
            # 确定是否生成异常数据
            is_anomaly = random.random() < args.anomaly_rate
            sensor_type = sensor['sensor_type']
            sensor_info = SENSOR_TYPES[sensor_type]
            
            # 根据是否异常选择不同的值范围
            value_range = sensor_info['anomaly_range'] if is_anomaly else sensor_info['normal_range']
            
            # 生成传感器数据
            data = {
                'sensor_id': sensor['sensor_id'],
                'timestamp': datetime.now().isoformat(),
                'sensor_type': sensor_type,
                'value': round(random.uniform(*value_range), 2),
                'unit': sensor_info['unit'],
                'location': sensor['location'],
                'farm_id': sensor['farm_id'],
                'crop_type': sensor['crop_type'],
                'region': sensor['region']
            }
            
            # 发送到Kafka
            future = producer.send(args.topic, key=sensor['sensor_id'], value=data)
            count += 1
            
        # 批量发送
        producer.flush()
        
        if count % 100 == 0:
            print(f"已发送 {count} 条数据记录到主题 {args.topic}")
        
        # 等待指定的间隔时间
        time.sleep(args.interval)
        
except KeyboardInterrupt:
    print("\n中断数据生成")
finally:
    producer.close()
    print(f"总共发送了 {count} 条数据记录")
    print("数据生成器已停止") 