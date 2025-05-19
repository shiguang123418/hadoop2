# -*- coding: utf-8 -*-
"""
农业传感器数据模拟生成器
用于生成农业传感器数据并发送至Kafka
"""

import json
import time
import random
import datetime
import argparse
from kafka import KafkaProducer
from kafka.errors import KafkaError
import logging

# 配置日志
logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO,
    handlers=[logging.StreamHandler()]  # 输出到控制台，不输出到日志文件
)
logger = logging.getLogger('sensor_data_producer')

# 传感器类型与单位
SENSOR_TYPES = {
    "温度": "°C",
    "湿度": "%",
    "光照": "lux",
    "土壤湿度": "%",
    "pH值": "",
    "二氧化碳": "ppm",
    "风速": "m/s"
}

# 区域
REGIONS = ["华北", "华东", "华南", "西北", "西南", "东北", "华中"]

# 作物类型
CROP_TYPES = ["小麦", "水稻", "玉米", "大豆", "棉花", "马铃薯", "甜菜"]

# 传感器ID前缀
SENSOR_PREFIX = "SENSOR-"

class SensorDataProducer:
    """农业传感器数据生产者"""
    
    def __init__(self, bootstrap_servers, topic="agriculture-sensor-data"):
        """
        初始化Kafka生产者
        
        Args:
            bootstrap_servers (str): Kafka服务器地址，例如 "192.168.1.192:9092"
            topic (str): Kafka主题名称
        """
        self.bootstrap_servers = bootstrap_servers
        self.topic = topic
        self.producer = None
        self.connect()
        
    def connect(self):
        """连接到Kafka服务器"""
        try:
            self.producer = KafkaProducer(
                bootstrap_servers=self.bootstrap_servers,
                value_serializer=lambda v: json.dumps(v, ensure_ascii=False).encode('utf-8'),
                key_serializer=str.encode
            )
            logger.info(f"已成功连接到Kafka服务器: {self.bootstrap_servers}")
        except Exception as e:
            logger.error(f"连接Kafka服务器失败: {e}")
            raise
    
    def close(self):
        """关闭Kafka连接"""
        if self.producer:
            self.producer.close()
            logger.info("Kafka连接已关闭")
    
    def generate_sensor_data(self, anomaly_rate=0.05):
        """
        生成随机传感器数据
        
        Args:
            anomaly_rate (float): 异常数据生成比例，0-1之间的值
            
        Returns:
            dict: 传感器数据字典
        """
        # 随机选择传感器类型
        sensor_type = random.choice(list(SENSOR_TYPES.keys()))
        unit = SENSOR_TYPES[sensor_type]
        
        # 随机生成传感器ID
        sensor_id = f"{SENSOR_PREFIX}{random.randint(1000, 9999)}"
        
        # 随机选择区域和作物类型
        region = random.choice(REGIONS)
        crop_type = random.choice(CROP_TYPES)
        
        # 生成时间戳
        timestamp = datetime.datetime.now().isoformat()
        
        # 判断是否生成异常数据
        is_anomaly = random.random() < anomaly_rate
        
        # 根据传感器类型和是否异常生成对应的值
        value = self.generate_value(sensor_type, is_anomaly)
        
        # 构建数据
        data = {
            "sensorId": sensor_id,
            "sensorType": sensor_type,
            "region": region,
            "cropType": crop_type,
            "value": value,
            "timestamp": timestamp,
            "unit": unit,
            "description": f"{sensor_type}传感器读数 - {region} - {crop_type}"
        }
        
        return data
    
    def generate_value(self, sensor_type, is_anomaly=False):
        """
        根据传感器类型生成合适的数值
        
        Args:
            sensor_type (str): 传感器类型
            is_anomaly (bool): 是否生成异常值
            
        Returns:
            float: 传感器读数值
        """
        # 正常范围内的随机值
        normal_ranges = {
            "温度": (10, 40),  # 10-40°C
            "湿度": (30, 100),  # 30-100%
            "光照": (1000, 100000),  # 1000-100000 lux
            "土壤湿度": (20, 80),  # 20-80%
            "pH值": (4, 10),  # 4-10
            "二氧化碳": (300, 1500),  # 300-1500 ppm
            "风速": (0, 20)  # 0-20 m/s
        }
        
        # 异常范围的随机值
        anomaly_ranges = {
            "温度": [(-10, 5), (45, 60)],
            "湿度": [(0, 20), (101, 110)],
            "光照": [(0, 500), (120000, 200000)],
            "土壤湿度": [(0, 15), (85, 100)],
            "pH值": [(0, 3), (11, 14)],
            "二氧化碳": [(0, 200), (2000, 5000)],
            "风速": [(25, 50)]
        }
        
        if not is_anomaly:
            # 生成正常范围内的值
            min_val, max_val = normal_ranges.get(sensor_type, (0, 100))
            return round(random.uniform(min_val, max_val), 2)
        else:
            # 生成异常范围内的值
            ranges = anomaly_ranges.get(sensor_type, [(0, 0), (200, 300)])
            selected_range = random.choice(ranges)
            min_val, max_val = selected_range
            return round(random.uniform(min_val, max_val), 2)
    
    def send_data(self, data):
        """
        发送数据到Kafka
        
        Args:
            data (dict): 要发送的传感器数据
            
        Returns:
            bool: 是否成功发送
        """
        if not self.producer:
            logger.error("Kafka生产者未初始化")
            return False
        
        try:
            # 使用传感器ID作为key
            key = data["sensorId"]
            future = self.producer.send(self.topic, key=key, value=data)
            
            # 等待发送结果
            record_metadata = future.get(timeout=10)
            
            logger.info(
                f"数据已发送到 {record_metadata.topic} [分区:{record_metadata.partition}] "
                f"偏移量:{record_metadata.offset} 传感器:{data['sensorId']} 类型:{data['sensorType']} 值:{data['value']}{data['unit']}"
            )
            return True
        
        except KafkaError as e:
            logger.error(f"发送数据到Kafka失败: {e}")
            return False
        except Exception as e:
            logger.error(f"发送数据过程中发生错误: {e}")
            return False
    
    def send_multiple_data(self, count=10, interval=1.0, anomaly_rate=0.05):
        """
        生成并发送多条数据
        
        Args:
            count (int): 数据条数
            interval (float): 发送间隔(秒)
            anomaly_rate (float): 异常值比例
            
        Returns:
            int: 成功发送的消息数
        """
        success_count = 0
        
        for i in range(count):
            data = self.generate_sensor_data(anomaly_rate)
            if self.send_data(data):
                success_count += 1
            
            if i < count - 1:  # 不在最后一次迭代后等待
                time.sleep(interval)
        
        logger.info(f"共生成 {count} 条数据，成功发送 {success_count} 条")
        return success_count
    
    def run_continuous(self, interval=5.0, anomaly_rate=0.05):
        """
        持续运行，定期发送数据
        
        Args:
            interval (float): 发送间隔(秒)
            anomaly_rate (float): 异常值比例
        """
        logger.info(f"开始持续发送数据，间隔 {interval} 秒，按Ctrl+C停止...")
        
        try:
            while True:
                data = self.generate_sensor_data(anomaly_rate)
                self.send_data(data)
                time.sleep(interval)
        except KeyboardInterrupt:
            logger.info("收到停止信号，停止发送数据")
        finally:
            self.close()

def main():
    """主函数，处理命令行参数并运行数据生产者"""
    parser = argparse.ArgumentParser(description="农业传感器数据模拟生成器")
    parser.add_argument("-b", "--bootstrap-servers", default="192.168.1.192:9092",
                        help="Kafka 服务器地址 (默认: 192.168.1.192:9092)")
    parser.add_argument("-t", "--topic", default="agriculture-sensor-data",
                        help="Kafka 主题 (默认: agriculture-sensor-data)")
    parser.add_argument("-c", "--count", type=int, default=0,
                        help="发送的数据条数，0表示持续发送 (默认: 0)")
    parser.add_argument("-i", "--interval", type=float, default=1.0,
                        help="发送间隔，单位秒 (默认: 1.0)")
    parser.add_argument("-a", "--anomaly-rate", type=float, default=0.05,
                        help="异常值比例，0-1之间 (默认: 0.05)")
    
    args = parser.parse_args()
    
    try:
        producer = SensorDataProducer(args.bootstrap_servers, args.topic)
        
        if args.count > 0:
            # 发送固定条数的数据
            producer.send_multiple_data(args.count, args.interval, args.anomaly_rate)
        else:
            # 持续发送数据
            producer.run_continuous(args.interval, args.anomaly_rate)
            
    except Exception as e:
        logger.error(f"发生错误: {e}")
    finally:
        logger.info("程序结束")

main() 