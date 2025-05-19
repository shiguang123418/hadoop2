<template>
  <div class="websocket-test">
    <h2>WebSocket连接测试</h2>
    
    <div class="status-box" :class="connectionStatus.class">
      <div class="status-indicator"></div>
      <div class="status-text">{{ connectionStatus.text }}</div>
    </div>
    
    <div class="controls">
      <button @click="connect" :disabled="isConnected" class="connect-btn">
        连接
      </button>
      <button @click="disconnect" :disabled="!isConnected" class="disconnect-btn">
        断开
      </button>
      <button @click="sendMessage" :disabled="!isConnected" class="send-btn">
        发送消息
      </button>
    </div>
    
    <div class="settings">
      <div class="form-row">
        <label>主机名:</label>
        <input v-model="settings.host" :disabled="isConnected" />
      </div>
      <div class="form-row">
        <label>端口:</label>
        <input v-model.number="settings.port" type="number" :disabled="isConnected" />
      </div>
      <div class="form-row">
        <label>路径:</label>
        <input v-model="settings.path" :disabled="isConnected" />
      </div>
    </div>
    
    <div class="log-container">
      <h3>消息日志</h3>
      <div class="log-area">
        <div v-for="(msg, index) in messages" :key="index" class="log-item" :class="msg.type">
          <span class="log-time">{{ msg.time }}</span>
          <span class="log-message">{{ msg.text }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import wsTest from '../ws-test.js';

export default {
  name: 'WebSocketTest',
  
  data() {
    return {
      socket: null,
      isConnected: false,
      connectionState: 'disconnected', // 'disconnected', 'connecting', 'connected', 'error'
      messages: [],
      settings: {
        host: window.location.hostname,
        port: 8080,
        path: '/kafka'
      }
    };
  },
  
  computed: {
    connectionStatus() {
      const statuses = {
        disconnected: {
          text: '未连接',
          class: 'status-disconnected'
        },
        connecting: {
          text: '正在连接...',
          class: 'status-connecting'
        },
        connected: {
          text: '已连接',
          class: 'status-connected'
        },
        error: {
          text: '连接错误',
          class: 'status-error'
        }
      };
      
      return statuses[this.connectionState] || statuses.disconnected;
    }
  },
  
  methods: {
    // 添加日志消息
    addMessage(text, type = 'info') {
      const time = new Date().toLocaleTimeString();
      this.messages.unshift({ text, type, time });
      
      // 限制消息数量
      if (this.messages.length > 100) {
        this.messages.pop();
      }
    },
    
    // 连接WebSocket
    connect() {
      this.connectionState = 'connecting';
      this.addMessage(`正在连接到 ws://${this.settings.host}:${this.settings.port}${this.settings.path}...`);
      
      this.socket = wsTest.testConnection({
        host: this.settings.host,
        port: this.settings.port,
        path: this.settings.path,
        
        onOpen: (event) => {
          this.connectionState = 'connected';
          this.isConnected = true;
          this.addMessage('WebSocket连接已成功建立!', 'success');
        },
        
        onMessage: (data) => {
          this.addMessage(`收到消息: ${JSON.stringify(data)}`, 'receive');
        },
        
        onClose: (event) => {
          this.connectionState = 'disconnected';
          this.isConnected = false;
          this.addMessage(`连接已关闭: 代码=${event.code}, 原因=${event.reason || '未知'}`, 
            event.wasClean ? 'info' : 'error');
          this.socket = null;
        },
        
        onError: (error) => {
          this.connectionState = 'error';
          this.addMessage('WebSocket连接错误', 'error');
          console.error('WebSocket连接错误:', error);
        }
      });
    },
    
    // 断开连接
    disconnect() {
      if (this.socket) {
        this.addMessage('正在断开连接...');
        wsTest.closeConnection(this.socket);
        this.socket = null;
      }
    },
    
    // 发送消息
    sendMessage() {
      if (!this.socket || this.socket.readyState !== WebSocket.OPEN) {
        this.addMessage('WebSocket未连接，无法发送消息', 'error');
        return;
      }
      
      // 创建测试消息
      const message = {
        type: 'test',
        timestamp: Date.now(),
        data: '测试消息 - ' + new Date().toLocaleTimeString()
      };
      
      try {
        this.socket.send(JSON.stringify(message));
        this.addMessage(`已发送消息: ${JSON.stringify(message)}`, 'send');
      } catch (err) {
        this.addMessage(`发送消息失败: ${err.message}`, 'error');
      }
    }
  },
  
  // 组件销毁时断开连接
  beforeUnmount() {
    this.disconnect();
  }
};
</script>

<style scoped>
.websocket-test {
  padding: 20px;
  font-family: Arial, sans-serif;
  max-width: 800px;
  margin: 0 auto;
}

.status-box {
  display: flex;
  align-items: center;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 20px;
  background-color: #f5f5f5;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 10px;
}

.status-disconnected .status-indicator {
  background-color: #9e9e9e;
}

.status-connecting .status-indicator {
  background-color: #ffb74d;
  animation: blink 1s infinite;
}

.status-connected .status-indicator {
  background-color: #66bb6a;
}

.status-error .status-indicator {
  background-color: #ef5350;
}

@keyframes blink {
  0% { opacity: 0.2; }
  50% { opacity: 1; }
  100% { opacity: 0.2; }
}

.controls {
  margin-bottom: 20px;
}

button {
  padding: 8px 16px;
  margin-right: 10px;
  border: none;
  border-radius: 4px;
  color: white;
  cursor: pointer;
}

button:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.connect-btn {
  background-color: #4caf50;
}

.disconnect-btn {
  background-color: #f44336;
}

.send-btn {
  background-color: #2196f3;
}

.settings {
  background-color: #f5f5f5;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.form-row {
  display: flex;
  margin-bottom: 10px;
}

.form-row label {
  width: 80px;
  font-weight: bold;
}

.form-row input {
  flex: 1;
  padding: 6px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.log-container {
  margin-top: 20px;
}

.log-area {
  height: 300px;
  overflow-y: auto;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: #f9f9f9;
  padding: 10px;
  font-family: monospace;
}

.log-item {
  margin-bottom: 5px;
  padding: 4px;
  border-bottom: 1px solid #eee;
}

.log-time {
  color: #7e7e7e;
  margin-right: 8px;
  font-size: 0.9em;
}

.log-item.info {
  color: #2196f3;
}

.log-item.success {
  color: #4caf50;
}

.log-item.error {
  color: #f44336;
}

.log-item.send {
  color: #9c27b0;
}

.log-item.receive {
  color: #ff9800;
}
</style> 