package org.shiguang.websocket

import java.net.InetSocketAddress
import java.nio.ByteBuffer

import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.{WebSocketServer => JWebSocketServer}

import scala.collection.JavaConverters._

/**
 * WebSocket服务器，用于实时发送传感器数据到前端
 */
class WebSocketServer(host: String, port: Int, path: String) extends JWebSocketServer(new InetSocketAddress(host, port)) {
  private val connections = collection.mutable.Set[WebSocket]()
  // 确保路径以斜杠开头
  private val normalizedPath = if (path.startsWith("/")) path else s"/$path"

  /**
   * 当WebSocket连接打开时调用
   */
  override def onOpen(conn: WebSocket, handshake: ClientHandshake): Unit = {
    val resourcePath = handshake.getResourceDescriptor
    // 打印接收到的路径信息，便于调试
    println(s"接收到WebSocket连接请求，路径: $resourcePath，期望路径: $normalizedPath")
    
    // 检查请求路径，允许完全匹配或者以路径开头的请求
    if (resourcePath.equals(normalizedPath) || resourcePath.startsWith(s"$normalizedPath?")) {
      connections += conn
      println(s"新WebSocket连接建立: ${conn.getRemoteSocketAddress.getAddress.getHostAddress}")
    } else {
      println(s"WebSocket路径不匹配，关闭连接。请求路径: $resourcePath, 配置路径: $normalizedPath")
      conn.close(1008, s"Invalid path: $resourcePath, expected: $normalizedPath")
    }
  }

  /**
   * 当WebSocket连接关闭时调用
   */
  override def onClose(conn: WebSocket, code: Int, reason: String, remote: Boolean): Unit = {
    connections -= conn
    println(s"WebSocket连接关闭: ${conn.getRemoteSocketAddress.getAddress.getHostAddress}, 代码: $code, 原因: $reason, 远程: $remote")
  }

  /**
   * 当接收到消息时调用
   */
  override def onMessage(conn: WebSocket, message: String): Unit = {
    println(s"收到消息: $message 来自: ${conn.getRemoteSocketAddress.getAddress.getHostAddress}")
    // 这里我们可以处理来自前端的消息
  }

  /**
   * 当接收到二进制消息时调用
   */
  override def onMessage(conn: WebSocket, message: ByteBuffer): Unit = {
    // 暂不处理二进制消息
  }

  /**
   * 当发生错误时调用
   */
  override def onError(conn: WebSocket, ex: Exception): Unit = {
    if (conn != null) {
      connections -= conn
      println(s"WebSocket连接错误: ${conn.getRemoteSocketAddress.getAddress.getHostAddress}")
    }
    ex.printStackTrace()
  }

  /**
   * 当服务器启动时调用
   */
  override def onStart(): Unit = {
    println(s"WebSocket服务器启动成功，监听地址: $host:$port, 路径: $normalizedPath")
  }

  /**
   * 广播消息给所有连接的客户端
   */
  override def broadcast(message: String): Unit = {
    if (connections.nonEmpty) {
      println(s"广播消息到 ${connections.size} 个客户端")
      connections.foreach(conn => {
        if (conn.isOpen) {
          conn.send(message)
        }
      })
    } else {
      println("没有已连接的WebSocket客户端，消息未广播")
    }
  }

  /**
   * 获取当前连接数
   */
  def getConnectionCount: Int = connections.size
} 