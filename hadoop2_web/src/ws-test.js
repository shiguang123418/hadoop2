/**
 * WebSocket连接测试脚本
 * 
 * 使用方法:
 * 1. 在Vue组件中导入: import wsTest from './ws-test.js';
 * 2. 调用测试: wsTest.testConnection();
 */

const wsTest = {
  /**
   * 测试WebSocket连接
   * @param {Object} options 连接选项
   * @param {string} options.host 主机名（默认为当前页面主机名）
   * @param {number} options.port 端口号（默认为8020）
   * @param {string} options.path 路径（默认为'/kafka'）
   * @param {Function} options.onMessage 消息回调
   * @param {Function} options.onOpen 连接打开回调
   * @param {Function} options.onClose 连接关闭回调
   * @param {Function} options.onError 错误回调
   * @returns {WebSocket} WebSocket实例
   */
  testConnection(options = {}) {
    const host = options.host || window.location.hostname;
    const port = options.port || 8080;
    const path = options.path || '/kafka';
    
    console.log(`正在连接WebSocket: ws://${host}:${port}${path}`);
    
    try {
      // 创建WebSocket连接
      const ws = new WebSocket(`ws://${host}:${port}${path}`);
      
      // 设置事件处理
      ws.onopen = (event) => {
        console.log('WebSocket连接已打开!', event);
        
        // 发送测试消息
        ws.send(JSON.stringify({
          type: 'ping',
          timestamp: Date.now(),
          client: 'WebSocket测试客户端'
        }));
        
        if (typeof options.onOpen === 'function') {
          options.onOpen(event, ws);
        }
      };
      
      ws.onmessage = (event) => {
        console.log('WebSocket收到消息:', event.data);
        try {
          const data = JSON.parse(event.data);
          console.log('解析后的数据:', data);
          
          if (typeof options.onMessage === 'function') {
            options.onMessage(data, event, ws);
          }
        } catch (err) {
          console.error('解析WebSocket消息失败:', err);
        }
      };
      
      ws.onclose = (event) => {
        console.log(`WebSocket连接已关闭: 代码=${event.code}, 原因=${event.reason || '未知'}`);
        
        if (typeof options.onClose === 'function') {
          options.onClose(event, ws);
        }
      };
      
      ws.onerror = (error) => {
        console.error('WebSocket连接错误:', error);
        
        if (typeof options.onError === 'function') {
          options.onError(error, ws);
        }
      };
      
      return ws;
    } catch (err) {
      console.error('创建WebSocket连接失败:', err);
      if (typeof options.onError === 'function') {
        options.onError(err);
      }
      return null;
    }
  },
  
  /**
   * 关闭WebSocket连接
   * @param {WebSocket} ws WebSocket实例
   */
  closeConnection(ws) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      console.log('正在关闭WebSocket连接...');
      ws.close(1000, '手动关闭');
    }
  }
};

export default wsTest; 