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
import socket

# 配置日志
logging.basicConfig(
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    level=logging.INFO,
    handlers=[logging.StreamHandler()]  # 输出到控制台，不输出到日志文件
)
logger = logging.getLogger('sensor_data_producer')

# 传感器类型与单位
SENSOR_TYPES = {
    "temperature": "°C",
    "humidity": "%",
    "light_intensity": "lux",
    "soil_moisture": "%",
    "ph": "",
    "co2": "ppm",
    "wind_speed": "m/s"
}

# 区域
REGIONS = ["north", "east", "south", "west", "central", "northeast", "southwest"]

# 作物类型
CROP_TYPES = ["wheat", "rice", "corn", "soybean", "cotton", "potato", "tomato"]

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
        self.farm_ids = [f"farm_{i:03d}" for i in range(1, 6)]  # 农场ID列表
        self.locations = [f"Field-{i:02d}" for i in range(1, 11)]  # 地点列表
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
        
        # 选择农场和位置
        farm_id = random.choice(self.farm_ids)
        location = random.choice(self.locations)
        
        # 为每个农场分配唯一的传感器ID
        sensor_id = f"sensor_{farm_id}_{sensor_type}"
        
        # 随机选择区域和作物类型
        region = random.choice(REGIONS)
        crop_type = random.choice(CROP_TYPES)
        
        # 生成时间戳
        timestamp = datetime.datetime.now().isoformat()
        
        # 判断是否生成异常数据
        is_anomaly = random.random() < anomaly_rate
        
        # 根据传感器类型和是否异常生成对应的值
        value = self.generate_value(sensor_type, is_anomaly)
        
        # 构建数据 (使用蛇形命名法，与Java后端一致)
        data = {
            "sensor_id": sensor_id,
            "sensor_type": sensor_type,
            "timestamp": timestamp,
            "value": value,
            "unit": unit,
            "location": location,
            "farm_id": farm_id,
            "crop_type": crop_type,
            "region": region
        }
        
        # 设置异常标志，便于后端判断 (可省略，由backend计算)
        if is_anomaly:
            data["is_anomaly"] = True
        
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
            "temperature": (15, 28),      # 15-28°C
            "humidity": (40, 70),         # 40-70%
            "light_intensity": (400, 700), # 400-700 lux
            "soil_moisture": (20, 45),    # 20-45%
            "ph": (6.0, 7.5),             # 6.0-7.5
            "co2": (350, 450),            # 350-450 ppm
            "wind_speed": (0, 10)         # 0-10 m/s
        }
        
        # 异常范围的随机值
        anomaly_ranges = {
            "temperature": [(-5, 5), (35, 45)],
            "humidity": [(0, 20), (85, 100)],
            "light_intensity": [(0, 200), (900, 1500)],
            "soil_moisture": [(0, 10), (60, 100)],
            "ph": [(3.0, 5.0), (8.0, 11.0)],
            "co2": [(100, 200), (600, 1000)],
            "wind_speed": [(15, 30)]
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
            key = data["sensor_id"]
            future = self.producer.send(self.topic, key=key, value=data)
            
            # 等待发送结果
            record_metadata = future.get(timeout=10)
            
            logger.info(
                f"数据已发送到 {record_metadata.topic} [分区:{record_metadata.partition}] "
                f"偏移量:{record_metadata.offset} 传感器:{data['sensor_id']} 类型:{data['sensor_type']} 值:{data['value']}{data['unit']}"
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

def get_local_ip():
    """获取本机IP地址"""
    try:
        # 创建一个临时socket连接来获取本机IP
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.connect(("8.8.8.8", 80))
        ip = s.getsockname()[0]
        s.close()
        return ip
    except Exception:
        return "localhost"

def main():
    """主函数，处理命令行参数并运行数据生产者"""
    # 默认服务器地址
    default_host = "192.168.1.192:9092"
    
    parser = argparse.ArgumentParser(description="农业传感器数据模拟生成器")
    parser.add_argument("-b", "--bootstrap-servers", default=default_host,
                        help=f"Kafka 服务器地址 (默认: {default_host})")
    parser.add_argument("-t", "--topic", default="agriculture-sensor-data",
                        help="Kafka 主题 (默认: agriculture-sensor-data)")
    parser.add_argument("-c", "--count", type=int, default=0,
                        help="发送的数据条数，0表示持续发送 (默认: 0)")
    parser.add_argument("-i", "--interval", type=float, default=1.0,
                        help="发送间隔，单位秒 (默认: 1.0)")
    parser.add_argument("-a", "--anomaly-rate", type=float, default=0.05,
                        help="异常值比例，0-1之间 (默认: 0.05)")
    
    args = parser.parse_args()
    
    # 显示配置信息
    logger.info(f"使用以下配置：")
    logger.info(f"Kafka服务器: {args.bootstrap_servers}")
    logger.info(f"主题: {args.topic}")
    logger.info(f"数据条数: {'持续发送' if args.count == 0 else args.count}")
    logger.info(f"发送间隔: {args.interval}秒")
    logger.info(f"异常值比例: {args.anomaly_rate * 100}%")
    
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

if __name__ == "__main__":
    main() 