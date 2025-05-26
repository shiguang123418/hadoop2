#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
气象数据生成脚本
读取CSV文件并发送数据到Kafka，用于实时气象数据分析
"""

import csv
import json
import time
import argparse
import sys
import os
from datetime import datetime

try:
    from kafka import KafkaProducer
except ImportError:
    print("未安装kafka-python库。请运行: pip install kafka-python")
    sys.exit(1)

# 命令行参数
parser = argparse.ArgumentParser(description='气象数据生成器')
parser.add_argument('--bootstrap-servers', default='shiguang:9092,nas:9092', help='Kafka服务器地址')
parser.add_argument('--topic', default='weather-data', help='Kafka主题')
parser.add_argument('--interval', type=float, default=1.0, help='数据发送间隔(秒)')
parser.add_argument('--file', default='../data/temprainfall.csv', help='CSV数据文件路径')
parser.add_argument('--repeat', action='store_true', help='是否循环发送数据')
args = parser.parse_args()

# 检查文件是否存在
if not os.path.exists(args.file):
    print(f"错误: 文件 {args.file} 不存在")
    sys.exit(1)

# 读取CSV数据
def read_csv_data(file_path):
    data = []
    with open(file_path, 'r') as file:
        csv_reader = csv.DictReader(file)
        for row in csv_reader:
            # 确保数字类型的字段正确转换
            try:
                row['maxtemp'] = float(row['maxtemp'])
                row['mintemp'] = float(row['mintemp'])
                row['rainfall'] = float(row['rainfall'])
                data.append(row)
            except ValueError as e:
                print(f"数据转换错误: {e}, 行: {row}")
                continue
    return data

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

# 读取CSV数据
print(f"正在读取CSV文件: {args.file}")
csv_data = read_csv_data(args.file)
if not csv_data:
    print("错误: CSV文件为空或格式错误")
    sys.exit(1)

print(f"成功读取 {len(csv_data)} 条天气数据记录")

# 发送表头 (可选，如果Kafka使用场景需要)
headers = list(csv_data[0].keys())
producer.send(args.topic, key="header", value={"headers": headers})
print(f"已发送数据表头: {headers}")

# 生成数据并发送到Kafka
try:
    count = 0
    while True:
        for row in csv_data:
            # 添加时间戳
            data = dict(row)
            data['timestamp'] = int(time.time() * 1000)  # 毫秒级时间戳
            
            # 发送到Kafka
            future = producer.send(args.topic, key=data['city'], value=data)
            count += 1
            
            # 每10条输出一次日志
            if count % 10 == 0:
                print(f"已发送 {count} 条数据记录到主题 {args.topic}")
            
            # 等待指定的间隔时间
            time.sleep(args.interval)
        
        # 刷新确保所有消息发送
        producer.flush()
        
        # 如果不循环，发送一轮后退出
        if not args.repeat:
            break
            
except KeyboardInterrupt:
    print("\n中断数据生成")
finally:
    producer.close()
    print(f"总共发送了 {count} 条天气数据记录")
    print("数据生成器已停止") 