#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
产品数据生成脚本
读取产品CSV文件并发送数据到Kafka，用于实时产品数据分析
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
parser = argparse.ArgumentParser(description='产品数据生成器')
parser.add_argument('--bootstrap-servers', default='shiguang:9092,nas:9092', help='Kafka服务器地址')
parser.add_argument('--topic', default='product-data', help='Kafka主题')
parser.add_argument('--interval', type=float, default=0.5, help='数据发送间隔(秒)')
parser.add_argument('--file', default='../data/product_regressiondb.csv', help='CSV数据文件路径')
parser.add_argument('--limit', type=int, default=1000, help='最大发送条数(默认1000条)')
parser.add_argument('--repeat', action='store_true', help='是否循环发送数据')
args = parser.parse_args()

# 检查文件是否存在
if not os.path.exists(args.file):
    print(f"错误: 文件 {args.file} 不存在")
    sys.exit(1)

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

# 读取CSV数据(分批读取，避免内存问题)
def process_csv_file(file_path, batch_size=100):
    headers = None
    batch = []
    total_sent = 0
    
    try:
        with open(file_path, 'r') as file:
            # 读取表头
            headers = next(csv.reader(file))
            
            # 发送表头
            producer.send(args.topic, key="header", value={"headers": headers})
            print(f"已发送数据表头: {headers}")
            
            # 读取数据并分批处理
            reader = csv.DictReader(file, fieldnames=headers)
            
            for row in reader:
                if total_sent >= args.limit:
                    break
                    
                # 处理数据
                data = dict(row)
                data['timestamp'] = int(time.time() * 1000)  # 添加时间戳
                
                # 添加唯一标识作为key
                if 'id' in data:
                    key = str(data['id'])
                else:
                    key = str(total_sent)
                
                # 发送到Kafka
                producer.send(args.topic, key=key, value=data)
                total_sent += 1
                
                # 每批次处理完后刷新
                if total_sent % batch_size == 0:
                    producer.flush()
                    print(f"已发送 {total_sent} 条记录")
                    # 批次控制
                    time.sleep(args.interval * batch_size)
                    
            # 最后一次刷新
            producer.flush()
            print(f"完成发送批次，总共发送 {total_sent} 条记录")
            
    except Exception as e:
        print(f"处理CSV文件时出错: {e}")
    
    return total_sent, headers

# 主程序
print(f"正在处理CSV文件: {args.file}")

try:
    total_count = 0
    
    while True:
        count, headers = process_csv_file(args.file)
        total_count += count
        
        print(f"已完成一轮数据发送，总共发送 {total_count} 条产品数据记录")
        
        # 如果不循环或达到限制，则退出
        if not args.repeat or total_count >= args.limit:
            break
        
        # 循环发送前等待
        if args.repeat:
            print(f"等待5秒后开始下一轮发送...")
            time.sleep(5)

except KeyboardInterrupt:
    print("\n中断数据生成")
finally:
    producer.close()
    print(f"总共发送了 {total_count} 条产品数据记录")
    print("数据生成器已停止") 