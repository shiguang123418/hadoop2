#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
WebSocket连接测试脚本
"""

import sys
import socket
import argparse
import websocket
import json
import time

def check_host_port(host, port, timeout=2):
    """检查主机端口是否开放"""
    sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    sock.settimeout(timeout)
    try:
        result = sock.connect_ex((host, port))
        if result == 0:
            print(f"✅ 主机 {host}:{port} 端口开放")
            return True
        else:
            print(f"❌ 主机 {host}:{port} 端口未开放")
            return False
    except socket.error as e:
        print(f"❌ 连接错误: {e}")
        return False
    finally:
        sock.close()

def test_websocket_connection(ws_url):
    """测试WebSocket连接"""
    print(f"尝试连接到WebSocket: {ws_url}")
    
    try:
        # 禁用websocket库的警告日志
        websocket.enableTrace(False)
        
        # 创建WebSocket连接
        ws = websocket.create_connection(ws_url, timeout=5)
        
        # 发送一个测试消息
        test_message = json.dumps({"type": "ping", "timestamp": time.time()})
        ws.send(test_message)
        print(f"✅ 成功发送消息: {test_message}")
        
        # 尝试接收响应
        try:
            result = ws.recv()
            print(f"✅ 收到响应: {result}")
        except websocket.WebSocketTimeoutException:
            print("⚠️ 未收到响应,但连接已建立")
        
        # 关闭连接
        ws.close()
        print("✅ WebSocket连接成功!")
        return True
    
    except Exception as e:
        print(f"❌ WebSocket连接失败: {e}")
        return False

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='WebSocket连接测试工具')
    parser.add_argument('--host', default='hadoop', help='WebSocket主机地址')
    parser.add_argument('--port', type=int, default=8090, help='WebSocket端口')
    args = parser.parse_args()
    
    # 先测试端口是否开放
    if check_host_port(args.host, args.port):
        # 再测试WebSocket连接
        ws_url = f"ws://{args.host}:{args.port}"
        test_websocket_connection(ws_url)
    else:
        print(f"建议检查以下几点:")
        print(f"1. 确认WebSocket服务是否已启动")
        print(f"2. 检查防火墙设置是否允许{args.port}端口访问")
        print(f"3. 检查主机名'{args.host}'是否正确解析")
        print(f"4. 如果使用容器化部署,确认端口映射配置正确") 