package org.shiguang.websocket;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;

import org.shiguang.model.SensorData;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * WebSocket服务器，用于实时发送传感器数据到前端
 */
public class WebSocketServer extends org.java_websocket.server.WebSocketServer implements Serializable {
    private static final long serialVersionUID = 1L;
    
    // 使用transient标记不应该被序列化的字段
    private transient Set<WebSocket> connections;
    // 确保路径以斜杠开头
    private final String normalizedPath;
    // JSON处理工具
    private final transient ObjectMapper objectMapper;

    public WebSocketServer(String host, int port, String path) {
        super(new InetSocketAddress(host, port));
        this.connections = new HashSet<>();
        this.normalizedPath = path.startsWith("/") ? path : "/" + path;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 当WebSocket连接打开时调用
     */
    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        String resourcePath = handshake.getResourceDescriptor();
        // 打印接收到的路径信息，便于调试
        System.out.println("接收到WebSocket连接请求，路径: " + resourcePath + "，期望路径: " + normalizedPath);
        
        // 检查请求路径，允许完全匹配或者以路径开头的请求
        if (resourcePath.equals(normalizedPath) || resourcePath.startsWith(normalizedPath + "?")) {
            connections.add(conn);
            System.out.println("新WebSocket连接建立: " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
        } else {
            System.out.println("WebSocket路径不匹配，关闭连接。请求路径: " + resourcePath + ", 配置路径: " + normalizedPath);
            conn.close(1008, "Invalid path: " + resourcePath + ", expected: " + normalizedPath);
        }
    }

    /**
     * 当WebSocket连接关闭时调用
     */
    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        connections.remove(conn);
        System.out.println("WebSocket连接关闭: " + conn.getRemoteSocketAddress().getAddress().getHostAddress() + 
                          ", 代码: " + code + ", 原因: " + reason + ", 远程: " + remote);
    }

    /**
     * 当接收到消息时调用
     */
    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("收到消息: " + message + " 来自: " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
        // 这里我们可以处理来自前端的消息
    }

    /**
     * 当接收到二进制消息时调用
     */
    @Override
    public void onMessage(WebSocket conn, ByteBuffer message) {
        // 暂不处理二进制消息
    }

    /**
     * 当发生错误时调用
     */
    @Override
    public void onError(WebSocket conn, Exception ex) {
        if (conn != null) {
            connections.remove(conn);
            System.out.println("WebSocket连接错误: " + conn.getRemoteSocketAddress().getAddress().getHostAddress());
        }
        ex.printStackTrace();
    }

    /**
     * 当服务器启动时调用
     */
    @Override
    public void onStart() {
        System.out.println("WebSocket服务器启动成功，监听地址: " + this.getAddress().getHostString() + 
                          ":" + this.getAddress().getPort() + ", 路径: " + normalizedPath);
    }

    /**
     * 广播消息给所有连接的客户端
     */
    @Override
    public void broadcast(String message) {
        if (!connections.isEmpty()) {
            System.out.println("广播消息到 " + connections.size() + " 个客户端");
            for (WebSocket conn : connections) {
                if (conn.isOpen()) {
                    conn.send(message);
                }
            }
        } else {
            System.out.println("没有已连接的WebSocket客户端，消息未广播");
        }
    }

    /**
     * 获取当前连接数
     */
    public int getConnectionCount() {
        return connections.size();
    }

    /**
     * 发送传感器数据到所有连接的客户端
     */
    public void broadcastSensorData(SensorData sensorData) {
        try {
            // 创建JSON对象
            ObjectNode json = objectMapper.createObjectNode();
            json.put("sensorId", sensorData.getSensorId());
            json.put("timestamp", sensorData.getTimestamp());
            json.put("temperature", sensorData.getTemperature());
            json.put("humidity", sensorData.getHumidity());
            json.put("soilMoisture", sensorData.getSoilMoisture());
            json.put("lightIntensity", sensorData.getLightIntensity());
            json.put("location", sensorData.getLocation());
            json.put("region", sensorData.getRegion());
            json.put("cropType", sensorData.getCropType());
            json.put("temperatureUnit", "°C");
            json.put("humidityUnit", "%");
            json.put("soilMoistureUnit", "%");
            json.put("lightIntensityUnit", "lux");
            json.put("batteryLevel", sensorData.getBatteryLevel());
            json.put("batteryLevelUnit", "%");
            
            // 判断各传感器数据是否异常
            double tempMin = 10.0;  // 简化处理，直接使用固定值
            double tempMax = 35.0;
            double humidityMin = 30.0;
            double humidityMax = 80.0;
            double soilMoistureMin = 15.0;
            double soilMoistureMax = 60.0;
            double lightIntensityMin = 300.0;
            double lightIntensityMax = 900.0;
            double batteryLevelMin = 20.0;
            
            boolean tempAnomaly = sensorData.getTemperature() < tempMin || sensorData.getTemperature() > tempMax;
            boolean humidityAnomaly = sensorData.getHumidity() < humidityMin || sensorData.getHumidity() > humidityMax;
            boolean soilMoistureAnomaly = sensorData.getSoilMoisture() < soilMoistureMin || sensorData.getSoilMoisture() > soilMoistureMax;
            boolean lightIntensityAnomaly = sensorData.getLightIntensity() < lightIntensityMin || sensorData.getLightIntensity() > lightIntensityMax;
            boolean batteryLevelAnomaly = sensorData.getBatteryLevel() < batteryLevelMin;
            
            json.put("temperatureAnomaly", tempAnomaly);
            json.put("humidityAnomaly", humidityAnomaly);
            json.put("soilMoistureAnomaly", soilMoistureAnomaly);
            json.put("lightIntensityAnomaly", lightIntensityAnomaly);
            json.put("batteryLevelAnomaly", batteryLevelAnomaly);
            json.put("isAnomalyDetected", tempAnomaly || humidityAnomaly || soilMoistureAnomaly || 
                                          lightIntensityAnomaly || batteryLevelAnomaly);
            
            // 转换为JSON字符串
            String jsonStr = objectMapper.writeValueAsString(json);
            
            // 广播消息到所有连接
            synchronized (this) {
                Set<WebSocket> activeConnections = new HashSet<>(connections);  // 创建一个副本以避免并发修改
                
                int connCount = 0;
                for (WebSocket conn : activeConnections) {
                    if (conn.isOpen()) {
                        conn.send(jsonStr);
                        connCount++;
                    }
                }
                
                System.out.println("广播消息到 " + connCount + " 个客户端");
            }
        } catch (Exception e) {
            System.out.println("发送传感器数据时出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
} 