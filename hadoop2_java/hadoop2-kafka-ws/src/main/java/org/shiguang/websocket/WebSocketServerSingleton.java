package org.shiguang.websocket;

/**
 * WebSocket服务器单例类，避免序列化问题
 */
public class WebSocketServerSingleton {
    private static WebSocketServer instance;
    
    private WebSocketServerSingleton() {
        // 私有构造方法，防止实例化
    }
    
    /**
     * 初始化WebSocket服务器
     */
    public static synchronized WebSocketServer initialize(String host, int port, String path) {
        if (instance == null) {
            instance = new WebSocketServer(host, port, path);
            instance.start();
            System.out.println("WebSocket服务器已启动，监听地址: " + host + ":" + port + ", 路径: " + path);
        }
        return instance;
    }
    
    /**
     * 获取WebSocket服务器实例
     */
    public static WebSocketServer getInstance() {
        if (instance == null) {
            throw new IllegalStateException("WebSocket服务器尚未初始化，请先调用initialize方法");
        }
        return instance;
    }
    
    /**
     * 停止WebSocket服务器
     */
    public static void stop() {
        if (instance != null) {
            try {
                instance.stop();
                System.out.println("WebSocket服务器已停止");
            } catch (Exception e) {
                System.out.println("停止WebSocket服务器出错: " + e.getMessage());
                e.printStackTrace();
            } finally {
                instance = null;
            }
        }
    }
} 