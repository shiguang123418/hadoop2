<template>
  <div class="kafka-demo">
    <h2>Kafka 消息演示</h2>
    
    <div class="connection-status">
      <span>连接状态: </span>
      <span :class="{ 'connected': isConnected, 'disconnected': !isConnected }">
        {{ isConnected ? '已连接' : '未连接' }}
      </span>
      <button @click="toggleConnection" :disabled="connecting">
        {{ isConnected ? '断开连接' : '连接' }}
      </button>
    </div>
    
    <div class="kafka-controls">
      <div class="topic-input">
        <label for="topic">主题:</label>
        <input id="topic" v-model="topic" placeholder="输入Kafka主题" />
      </div>
      
      <div class="subscribe-controls">
        <button @click="subscribe" :disabled="!isConnected || isSubscribed">订阅</button>
        <button @click="unsubscribe" :disabled="!isConnected || !isSubscribed">取消订阅</button>
        <span v-if="isSubscribed" class="subscribed-label">已订阅</span>
      </div>
    </div>
    
    <div class="message-sender">
      <div class="message-input">
        <label for="message">消息内容:</label>
        <textarea id="message" v-model="messageContent" placeholder="输入要发送的消息内容" rows="3"></textarea>
      </div>
      <button @click="sendMessage" :disabled="!isConnected || !topic || !messageContent">发送消息</button>
    </div>
    
    <div class="message-list">
      <h3>接收到的消息 ({{ messages.length }})</h3>
      <button @click="clearMessages" :disabled="messages.length === 0">清空消息</button>
      
      <div class="messages" ref="messageContainer" v-if="messages.length > 0">
        <div v-for="(msg, index) in messages" :key="index" class="message-item">
          <div class="message-header">
            <span class="message-topic">{{ msg.topic }}</span>
            <span class="message-time">{{ msg.time }}</span>
          </div>
          <div class="message-content">
            <pre>{{ msg.content }}</pre>
          </div>
        </div>
      </div>
      <div v-else class="no-messages">
        暂无消息
      </div>
    </div>
  </div>
</template>

<script>
import kafkaService from '../services/kafka'; // 导入单例

export default {
  name: 'KafkaDemo',
  
  data() {
    return {
      isConnected: kafkaService.connected, // 从 service 获取初始状态
      connecting: false,
      topic: 'test-topic',
      isSubscribed: false,
      messageContent: '',
      messages: []
    }
  },
  
  created() {
    // 可以在这里添加监听 service 状态变化的回调，如果需要实时更新的话
    // 例如: kafkaService.on('connectionChange', (status) => { this.isConnected = status; });
    // 但对于简单场景，按钮点击时检查即可
    console.log('KafkaDemo created. Initial connection status:', this.isConnected);
  },
  
  beforeUnmount() {
    // 组件销毁前尝试取消所有订阅
    if (this.isSubscribed) {
      this.unsubscribeFromTopic();
    }
    // 注意：通常不在此处断开连接，除非确定是应用退出
    // kafkaService.disconnect(); 
  },
  
  methods: {
    checkConnectionStatus() {
      // 手动检查状态
      this.isConnected = kafkaService.connected;
    },
    
    toggleConnection() {
      this.checkConnectionStatus(); // 更新当前状态
      if (this.isConnected) {
        console.log('[KafkaDemo] Attempting to disconnect...');
        kafkaService.disconnect();
        this.isConnected = false; // Assume disconnect is immediate for UI feedback
        this.isSubscribed = false; // Disconnect implies unsubscribe
      } else {
        if (this.connecting) return; // 防止重复点击
        console.log('[KafkaDemo] Attempting to connect...');
        this.connecting = true;
        kafkaService.connect()
          .then(() => {
            console.log('[KafkaDemo] Connection successful.');
            this.isConnected = true;
          })
          .catch(error => {
            console.error('[KafkaDemo] Connection failed:', error);
            this.isConnected = false; // 确保状态更新
            alert(`连接Kafka服务失败：${error.message || '未知错误，请检查控制台日志'}`);
          })
          .finally(() => {
            this.connecting = false;
          });
      }
    },
    
    subscribe() {
      if (!this.topic || !this.isConnected || this.isSubscribed) return;
      console.log(`[KafkaDemo] Subscribing to topic: ${this.topic}`);
      try {
        // Service 的 subscribe 返回一个取消订阅函数，但我们这里直接管理 isSubscribed 状态
        kafkaService.subscribe(this.topic, this.handleIncomingMessage); 
        this.isSubscribed = true;
      } catch (error) {
         console.error(`[KafkaDemo] Failed to subscribe to topic ${this.topic}:`, error);
         alert(`订阅主题 ${this.topic} 失败: ${error.message}`);
      }
    },
    
    unsubscribeFromTopic() { // 重命名以更清晰
      if (!this.topic || !this.isSubscribed) return;
      console.log(`[KafkaDemo] Unsubscribing from topic: ${this.topic}`);
      try {
          // 调用 service 的 unsubscribe，需要传递之前注册的回调函数
          kafkaService.unsubscribe(this.topic, this.handleIncomingMessage); 
          this.isSubscribed = false;
      } catch (error) {
          console.error(`[KafkaDemo] Failed to unsubscribe from topic ${this.topic}:`, error);
          alert(`取消订阅主题 ${this.topic} 失败: ${error.message}`);
      }
    },
    
    sendMessage() {
      if (!this.topic || !this.messageContent || !this.isConnected) return;
      console.log(`[KafkaDemo] Sending message to topic ${this.topic}`);
      const messageData = {
        text: this.messageContent,
        sender: 'web-client',
        timestamp: new Date().toISOString()
      };
      kafkaService.publish(this.topic, messageData)
        .then(() => {
          console.log('[KafkaDemo] Message sent successfully.');
          this.messageContent = ''; // 清空输入框
        })
        .catch(error => {
          console.error('[KafkaDemo] Failed to send message:', error);
          alert(`发送消息失败: ${error.message}`);
        });
    },
    
    handleIncomingMessage(data) {
      console.log('[KafkaDemo] Received message:', data);
      const newMessage = {
        topic: this.topic, // 注意：如果同时订阅多个主题，这里可能需要从消息体获取
        content: typeof data === 'object' ? JSON.stringify(data, null, 2) : data, // 格式化JSON
        time: new Date().toLocaleTimeString()
      };
      this.messages.unshift(newMessage);
      
      // 可以在这里限制消息数量，例如只保留最近100条
      if (this.messages.length > 100) {
          this.messages.pop();
      }
      
      // 确保消息容器滚动到底部，显示最新消息
      this.$nextTick(() => {
        const container = this.$refs.messageContainer;
        if (container) {
          container.scrollTop = container.scrollHeight;
        }
      });
    },
    
    clearMessages() {
      console.log('[KafkaDemo] Clearing messages.');
      this.messages = [];
    }
  }
}
</script>

<style scoped>
.kafka-demo {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
  border: 1px solid #eaeaea;
  border-radius: 8px;
  background-color: #f9f9f9;
}

h2 {
  margin-top: 0;
  color: #333;
  border-bottom: 2px solid #e0e0e0;
  padding-bottom: 10px;
}

.connection-status {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.connected {
  color: green;
  font-weight: bold;
  margin: 0 10px;
}

.disconnected {
  color: red;
  font-weight: bold;
  margin: 0 10px;
}

.kafka-controls {
  display: flex;
  margin-bottom: 20px;
  align-items: center;
  flex-wrap: wrap;
}

.topic-input {
  margin-right: 20px;
  flex: 1;
}

.subscribe-controls {
  display: flex;
  align-items: center;
}

.subscribed-label {
  margin-left: 10px;
  color: green;
  font-weight: bold;
}

.message-sender {
  margin-bottom: 20px;
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 4px;
  background-color: #fff;
}

.message-input {
  margin-bottom: 10px;
}

textarea, input {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  box-sizing: border-box;
}

button {
  background-color: #4CAF50;
  color: white;
  border: none;
  padding: 8px 15px;
  text-align: center;
  text-decoration: none;
  display: inline-block;
  font-size: 14px;
  margin: 4px 2px;
  cursor: pointer;
  border-radius: 4px;
}

button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.message-list {
  border: 1px solid #ddd;
  border-radius: 4px;
  padding: 15px;
  background-color: #fff;
}

.message-list h3 {
  margin-top: 0;
  display: inline-block;
}

.messages {
  max-height: 300px;
  overflow-y: auto;
  margin-top: 10px;
}

.message-item {
  border-bottom: 1px solid #eee;
  padding: 10px 0;
}

.message-header {
  display: flex;
  justify-content: space-between;
  font-size: 0.8em;
  color: #666;
  margin-bottom: 5px;
}

.message-topic {
  font-weight: bold;
}

.message-content {
  background-color: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-content pre {
  background-color: #f5f5f5;
  padding: 8px;
  border-radius: 4px;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0; /* 重置 pre 标签可能有的默认 margin */
  font-family: monospace; /* 使用等宽字体显示 JSON */
  font-size: 0.9em;
}

.no-messages {
  text-align: center;
  color: #999;
  padding: 20px;
}
</style> 