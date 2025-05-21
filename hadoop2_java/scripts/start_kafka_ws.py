#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
启动Kafka WebSocket服务的脚本
"""

import os
import sys
import subprocess
import socket
import time
import argparse

# 配置参数
parser = argparse.ArgumentParser(description='启动Kafka WebSocket服务')
parser.add_argument('--port', type=int, default=8090, help='WebSocket服务端口')
parser.add_argument('--host', default='0.0.0.0', help='WebSocket服务主机地址')
args = parser.parse_args()

def check_port(host, port):
    """检查端口是否已被占用"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(2)
    result = sock.connect_ex((host, port))
    sock.close()
    return result == 0

def find_jar_file():
    """查找WebSocket服务的JAR文件"""
    # 项目根目录
    root_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
    kafka_ws_dir = os.path.join(root_dir, 'hadoop2_java', 'hadoop2-kafka-ws')
    target_dir = os.path.join(kafka_ws_dir, 'target')
    
    if not os.path.exists(target_dir):
        print(f"目标目录不存在: {target_dir}")
        print("请先构建项目")
        return None
    
    # 查找所有jar文件
    jar_files = [f for f in os.listdir(target_dir) if f.endswith('.jar') and not f.endswith('-sources.jar') and not f.endswith('-javadoc.jar')]
    
    if not jar_files:
        print(f"在{target_dir}中未找到JAR文件")
        return None
    
    # 找到不含"sources"和"javadoc"的最新jar文件
    jar_file = sorted(jar_files)[-1]
    return os.path.join(target_dir, jar_file)

def start_service(jar_file, host, port):
    """启动WebSocket服务"""
    if check_port(host, port):
        print(f"端口{port}已被占用，服务可能已经在运行")
        return
    
    try:
        cmd = ['java', '-jar', jar_file]
        print(f"正在启动WebSocket服务: {' '.join(cmd)}")
        
        # 使用subprocess启动服务
        process = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
        
        # 等待一段时间看是否启动成功
        time.sleep(5)
        
        if process.poll() is None:
            # 进程仍在运行
            print(f"WebSocket服务已启动，PID: {process.pid}")
            
            # 尝试检查端口是否监听
            if check_port(host, port):
                print(f"已确认服务在端口 {port} 上运行")
            else:
                print(f"警告: 服务已启动但端口 {port} 未被监听")
            
            # 输出一部分日志
            for i in range(10):
                line = process.stdout.readline().decode('utf-8').strip()
                if line:
                    print(f"日志: {line}")
                time.sleep(0.1)
            
            print("服务已在后台启动")
        else:
            # 进程已结束
            stdout, stderr = process.communicate()
            print(f"服务启动失败，退出码: {process.returncode}")
            print(f"标准输出: {stdout.decode('utf-8')}")
            print(f"错误输出: {stderr.decode('utf-8')}")
    except Exception as e:
        print(f"启动服务时发生错误: {e}")

if __name__ == "__main__":
    # 检查端口是否已经被占用
    if check_port(args.host, args.port):
        print(f"WebSocket服务已在端口 {args.port} 上运行")
        sys.exit(0)
    
    # 查找JAR文件
    jar_file = find_jar_file()
    if not jar_file:
        print("未找到WebSocket服务JAR文件，请先构建项目")
        sys.exit(1)
    
    # 启动服务
    start_service(jar_file, args.host, args.port) 