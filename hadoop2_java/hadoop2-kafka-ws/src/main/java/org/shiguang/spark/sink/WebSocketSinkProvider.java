package org.shiguang.spark.sink;

import org.apache.spark.sql.ForeachWriter;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.gson.Gson;
import org.shiguang.config.AppConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket服务提供者，用于将处理后的传感器数据流式传输到前端
 */
@Component
public class WebSocketSinkProvider {

    private static WebSocketSinkProvider instance;
    private WebSocketServer server;
    private final CopyOnWriteArraySet<WebSocket> clients = new CopyOnWriteArraySet<>();
    private final Gson gson = new Gson();
    private static final int DEFAULT_PORT = 8090;
    private static final String DEFAULT_HOST = "0.0.0.0";
    
    @Autowired
    private AppConfig config;

    /**
     * 默认构造函数，供Spring使用
     */
    public WebSocketSinkProvider() {
        instance = this;
    }

    /**
     * 在Spring初始化Bean后启动WebSocket服务器
     */
    @PostConstruct
    public void init() {
        String host = config.getString("websocket.host", DEFAULT_HOST);
        int port = config.getInt("websocket.port", DEFAULT_PORT);
        
        server = new WebSocketServer(new InetSocketAddress(host, port)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                clients.add(conn);
                System.out.println("新连接已建立: " + conn.getRemoteSocketAddress());
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                clients.remove(conn);
                System.out.println("连接已关闭: " + conn.getRemoteSocketAddress() + " 退出代码: " + code);
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                // 如果需要，处理传入的消息
                System.out.println("收到客户端消息: " + message);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                if (conn != null) {
                    clients.remove(conn);
                }
                System.err.println("WebSocket错误: " + ex.getMessage());
            }

            @Override
            public void onStart() {
                System.out.println("WebSocket服务器在端口 " + port + " 上成功启动");
            }
        };
        
        // 在单独的线程中启动WebSocket服务器
        Thread serverThread = new Thread(() -> {
            server.start();
        });
        serverThread.setDaemon(true);
        serverThread.start();
    }

    /**
     * 获取单例实例
     * @return WebSocketSinkProvider实例
     */
    public static synchronized WebSocketSinkProvider getInstance() {
        return instance;
    }

    /**
     * 向所有已连接的客户端广播消息
     * @param message 要广播的消息
     */
    public void broadcast(Object message) {
        String jsonMessage = gson.toJson(message);
        for (WebSocket client : clients) {
            if (client.isOpen()) {
                client.send(jsonMessage);
            }
        }
    }

    /**
     * 创建可用于将处理后的数据发送到WebSocket的ForeachWriter
     * @param <T> 数据类型
     * @return ForeachWriter实现
     */
    public <T> ForeachWriter<T> createForeachWriter() {
        return new ForeachWriter<T>() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean open(long partitionId, long epochId) {
                return true;
            }

            @Override
            public void process(T value) {
                broadcast(value);
            }

            @Override
            public void close(Throwable errorOrNull) {
                // 每个批次不需要清理资源
            }
        };
    }

    /**
     * 在Spring销毁Bean时停止WebSocket服务器
     */
    @PreDestroy
    public void destroy() {
        stop();
    }
    
    /**
     * 停止WebSocket服务器
     */
    public void stop() {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (InterruptedException e) {
            System.err.println("停止WebSocket服务器时出错: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
} 