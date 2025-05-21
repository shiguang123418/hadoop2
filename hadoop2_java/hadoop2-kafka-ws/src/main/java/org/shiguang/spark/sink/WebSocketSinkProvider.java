package org.shiguang.spark.sink;

import org.apache.spark.sql.ForeachWriter;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.gson.Gson;
import org.shiguang.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket服务提供者，用于将处理后的传感器数据流式传输到前端
 * <p>
 * 该组件负责维护与所有客户端的WebSocket连接，并向它们广播消息。
 * 它也为Spark Streaming提供了ForeachWriter接口以方便数据输出。
 * </p>
 */
@Component
public class WebSocketSinkProvider implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSinkProvider.class);
    
    private static transient WebSocketSinkProvider instance;
    private transient WebSocketServer server;
    private final transient CopyOnWriteArraySet<WebSocket> clients = new CopyOnWriteArraySet<>();
    private final transient Gson gson = new Gson();
    private static final int DEFAULT_PORT = 8090;
    private static final String DEFAULT_HOST = "0.0.0.0";
    
    @Autowired
    private transient AppConfig config;

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
        
        logger.info("初始化WebSocket服务器，主机={}, 端口={}", host, port);
        
        server = new WebSocketServer(new InetSocketAddress(host, port)) {
            @Override
            public void onOpen(WebSocket conn, ClientHandshake handshake) {
                clients.add(conn);
                logger.info("新WebSocket连接已建立: {}", conn.getRemoteSocketAddress());
            }

            @Override
            public void onClose(WebSocket conn, int code, String reason, boolean remote) {
                clients.remove(conn);
                logger.info("WebSocket连接已关闭: {}, 退出代码: {}, 原因: {}", 
                        conn.getRemoteSocketAddress(), code, reason);
            }

            @Override
            public void onMessage(WebSocket conn, String message) {
                // 如果需要，处理传入的消息
                logger.debug("收到WebSocket客户端消息: {}", message);
            }

            @Override
            public void onError(WebSocket conn, Exception ex) {
                if (conn != null) {
                    clients.remove(conn);
                    logger.error("WebSocket连接错误: {}", conn.getRemoteSocketAddress(), ex);
                } else {
                    logger.error("WebSocket服务器错误", ex);
                }
            }

            @Override
            public void onStart() {
                logger.info("WebSocket服务器在 {}:{} 上成功启动", host, port);
            }
        };
        
        // 在单独的线程中启动WebSocket服务器
        Thread serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    server.start();
                } catch (Exception e) {
                    logger.error("启动WebSocket服务器失败", e);
                }
            }
        }, "WebSocketServerThread");
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
        if (gson == null || clients == null) {
            logger.warn("广播失败: gson或clients为null");
            return;
        }
        
        String jsonMessage = gson.toJson(message);
        int sentCount = 0;
        
        for (WebSocket client : clients) {
            if (client.isOpen()) {
                client.send(jsonMessage);
                sentCount++;
            }
        }
        
        if (sentCount > 0) {
            logger.debug("广播消息到 {} 个客户端", sentCount);
        }
    }

    /**
     * 创建可用于将处理后的数据发送到WebSocket的ForeachWriter
     * 使用静态内部类实现提高序列化兼容性
     * @param <T> 数据类型
     * @return ForeachWriter实现
     */
    public <T> ForeachWriter<T> createForeachWriter() {
        // 使用静态内部类而非匿名内部类
        return new WebSocketForeachWriter<T>();
    }
    
    /**
     * 可序列化的静态ForeachWriter实现类
     */
    public static class WebSocketForeachWriter<T> extends ForeachWriter<T> implements Serializable {
        private static final long serialVersionUID = 1L;
        private transient int count = 0;

        public WebSocketForeachWriter() {
            // 空构造函数
        }

        @Override
        public boolean open(long partitionId, long epochId) {
            count = 0;
            return true; // 总是返回true
        }

        @Override
        public void process(T value) {
            // 获取单例实例
            WebSocketSinkProvider provider = WebSocketSinkProvider.getInstance();
            if (provider != null) {
                provider.broadcast(value);
                count++;
            }
        }

        @Override
        public void close(Throwable errorOrNull) {
            // 不执行任何操作
        }
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
                logger.info("正在停止WebSocket服务器...");
                server.stop();
                logger.info("WebSocket服务器已停止");
            }
        } catch (InterruptedException e) {
            logger.error("停止WebSocket服务器时被中断", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 获取当前连接的客户端数量
     * @return 连接的客户端数量
     */
    public int getClientCount() {
        return clients != null ? clients.size() : 0;
    }
} 