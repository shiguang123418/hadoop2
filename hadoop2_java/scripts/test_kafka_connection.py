#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
Kafka连接测试脚本
"""

import sys
import argparse
from kafka.admin import KafkaAdminClient
from kafka.errors import KafkaError

def test_kafka_connection(bootstrap_servers):
    """测试Kafka连接"""
    print(f"尝试连接到Kafka: {bootstrap_servers}")
    
    try:
        # 尝试创建管理客户端连接
        admin_client = KafkaAdminClient(
            bootstrap_servers=bootstrap_servers,
            client_id='kafka-connection-tester'
        )
        
        # 获取集群信息
        cluster_metadata = admin_client.describe_cluster()
        
        # 获取主题列表
        topics = admin_client.list_topics()
        
        print("✅ 成功连接到Kafka服务器!")
        print(f"集群ID: {cluster_metadata['cluster_id']}")
        print(f"可用的主题: {', '.join(topics) if topics else '无主题'}")
        
        # 关闭连接
        admin_client.close()
        return True
    
    except KafkaError as e:
        print(f"❌ Kafka连接失败: {e}")
        return False
    except Exception as e:
        print(f"❌ 出现未知错误: {e}")
        return False

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Kafka连接测试工具')
    parser.add_argument('--bootstrap-servers', default='192.168.110.32:9092', 
                        help='Kafka服务器地址,格式为 host:port')
    args = parser.parse_args()
    
    success = test_kafka_connection(args.bootstrap_servers)
    sys.exit(0 if success else 1) 