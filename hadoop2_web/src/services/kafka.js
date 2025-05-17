/**
 * Kafka服务 - 通过WebSocket与后端Kafka服务进行通信
 */
import SockJS from 'sockjs-client';
import Stomp from 'webstomp-client';

class KafkaService {
  constructor() {
    this.socket = null;
    this.stompClient = null;
    this.messageHandlers = new Map();
    this.connected = false;
    this.reconnectAttempts = 0;
    this.maxReconnectAttempts = 5;
    this.reconnectTimeout = null;
    this.subscriptions = new Map();
    this.connectPromise = null;
    this.isConnecting = false;
    // 备用连接地址
    this.backupUrls = [
      'ws://192.168.1.192:8080/api/ws/kafka',
      'ws://localhost:8080/api/ws/kafka',
      'ws://127.0.0.1:8080/api/ws/kafka',
    ];
    this.currentUrlIndex = 0;
    // 后端 WebSocket 地址 (SockJS 需要 HTTP URL)
    this.websocketUrl = '/api/ws/kafka';
    console.log('KafkaService instance created.');
  }

  /**
   * 连接到WebSocket服务
   * @param {string} url WebSocket服务URL，如果不提供则使用默认地址
   */
  connect(url) {
    console.log('[KafkaService] connect called.');
    if (this.connected) {
      console.log('[KafkaService] Already connected.');
      return Promise.resolve();
    }
    if (this.isConnecting && this.connectPromise) {
      console.log('[KafkaService] Connection already in progress.');
      return this.connectPromise;
    }

    this.isConnecting = true;
    console.log(`[KafkaService] Attempting to connect to ${this.websocketUrl}...`);

    this.connectPromise = new Promise((resolve, reject) => {
      try {
        console.log('[KafkaService] Importing dependencies...');
        // Ensure imports worked
        if (typeof SockJS !== 'function') throw new Error('SockJS not loaded');
        if (typeof Stomp !== 'object' || !Stomp || typeof Stomp.over !== 'function') throw new Error('Stomp not loaded');
        
        console.log('[KafkaService] Creating SockJS instance...');
        this.socket = new SockJS(this.websocketUrl);

        this.socket.onopen = () => console.log('[KafkaService] SockJS connection opened.');
        this.socket.onerror = (error) => {
          console.error('[KafkaService] SockJS connection error:', error);
          // Note: Stomp connect error handler usually catches this too
        };
        this.socket.onclose = () => {
          console.log('[KafkaService] SockJS connection closed.');
          this.connected = false;
          this.isConnecting = false;
          this.stompClient = null;
          this.subscriptions.clear(); // Clear subscriptions on close
          // Optionally add reconnect logic here if desired later
        };

        console.log('[KafkaService] Creating STOMP client over SockJS...');
        this.stompClient = Stomp.over(this.socket);

        // Disable STOMP debugging by default for cleaner logs
        this.stompClient.debug = (str) => { /* console.log('[STOMP Debug]', str) */ };
        
        console.log('[KafkaService] Initiating STOMP connect...');
        this.stompClient.connect(
          {}, // Headers (optional)
          (frame) => { // Connect Callback
            console.log('[KafkaService] STOMP Connected:', frame);
            this.connected = true;
            this.isConnecting = false;
            // Re-establish any subscriptions if needed (or handle in subscribe method)
            this._resubscribeTopics();
            resolve(frame);
          },
          (error) => { // Error Callback
            console.error('[KafkaService] STOMP Connect Error:', error);
            this.connected = false;
            this.isConnecting = false;
            if (this.socket) {
              try { this.socket.close(); } catch (e) { /* ignore */ }
            }
            reject(error);
          }
        );

      } catch (error) {
        console.error('[KafkaService] Error during connect setup:', error);
        this.isConnecting = false;
        reject(error);
      }
    });
    
    return this.connectPromise;
  }

  _resubscribeTopics() {
    console.log('[KafkaService] Resubscribing to topics...');
    this.subscriptions.clear(); // Clear old STOMP subscriptions before resubscribing
    for (const topic of this.messageHandlers.keys()) {
      this._performStompSubscribe(topic);
    }
  }

  /**
   * 尝试下一个备用URL
   * @param {function} reject Promise的reject函数
   * @param {Error} error 当前连接的错误
   */
  tryNextUrl(reject, error) {
    this.currentUrlIndex = (this.currentUrlIndex + 1) % this.backupUrls.length;
    
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      console.log(`尝试使用下一个URL: ${this.backupUrls[this.currentUrlIndex]}`);
      this.reconnectAttempts++;
      
      setTimeout(() => {
        this.connect().then(() => {
          console.log('使用备用URL连接成功');
        }).catch(err => {
          console.error('备用URL连接失败:', err);
        });
      }, 1000);
    }
    
    if (error) {
      reject(error);
    } else {
      reject(new Error('WebSocket连接失败'));
    }
  }

  /**
   * 尝试重新连接
   */
  attemptReconnect() {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('达到最大重连次数');
      return;
    }

    this.reconnectAttempts++;
    const delay = Math.min(30000, Math.pow(2, this.reconnectAttempts) * 1000);
    
    console.log(`尝试在${delay}ms后重新连接...`);
    clearTimeout(this.reconnectTimeout);
    
    this.reconnectTimeout = setTimeout(() => {
      console.log(`重新连接尝试 ${this.reconnectAttempts}...`);
      this.connect();
    }, delay);
  }

  /**
   * 订阅Kafka主题
   * @param {string} topic Kafka主题
   * @param {function} callback 收到消息时的回调函数
   */
  subscribe(topic, callback) {
    console.log(`[KafkaService] subscribe called for topic: ${topic}`);
    if (!topic || typeof callback !== 'function') {
      console.error('[KafkaService] Invalid arguments for subscribe.');
      return () => {}; // Return no-op unsub function
    }

    if (!this.messageHandlers.has(topic)) {
      this.messageHandlers.set(topic, new Set());
    }
    this.messageHandlers.get(topic).add(callback);
    
    // Only subscribe via STOMP if connected and not already subscribed
    if (this.connected && !this.subscriptions.has(topic)) {
      this._performStompSubscribe(topic);
    }

    // Return an unsubscribe function
    return () => {
      this.unsubscribe(topic, callback);
    };
  }

  _performStompSubscribe(topic) {
    if (!this.stompClient || !this.connected || this.subscriptions.has(topic)) {
      console.warn(`[KafkaService] Cannot perform STOMP subscribe for ${topic}. Connected: ${this.connected}, Already subscribed: ${this.subscriptions.has(topic)}`);
      return;
    }
    
    const destination = `/topic/${topic}`;
    console.log(`[KafkaService] Performing STOMP subscribe to ${destination}`);
    try {
      const stompSubscription = this.stompClient.subscribe(destination, (message) => {
        // console.log(`[KafkaService] Received raw message for topic ${topic}:`, message.body);
        try {
          const messageData = JSON.parse(message.body);
          // Assuming backend sends message in format { type: 'MESSAGE', topic: '...', data: ... }
          if (messageData.topic === topic && this.messageHandlers.has(topic)) {
            // console.log(`[KafkaService] Distributing message data for topic ${topic}:`, messageData.data);
            this.messageHandlers.get(topic).forEach(handler => {
              try {
                handler(messageData.data);
              } catch (handlerError) {
                console.error(`[KafkaService] Error in message handler for topic ${topic}:`, handlerError);
              }
            });
          }
        } catch (parseError) {
          console.error(`[KafkaService] Error parsing message body for topic ${topic}:`, parseError, `Body: ${message.body}`);
        }
      });
      this.subscriptions.set(topic, stompSubscription);
      console.log(`[KafkaService] Successfully subscribed to ${destination}`);
      
      // Inform backend about the subscription (optional, depends on backend logic)
      // this.sendMessageToServer({ type: 'SUBSCRIBE', topic: topic });
      
    } catch (error) {
      console.error(`[KafkaService] Error during STOMP subscribe to ${destination}:`, error);
    }
  }

  /**
   * 取消订阅
   * @param {string} topic Kafka主题
   * @param {function} callback 要移除的回调函数
   */
  unsubscribe(topic, callback) {
    console.log(`[KafkaService] unsubscribe called for topic: ${topic}`);
    if (!this.messageHandlers.has(topic)) {
      return;
    }

    const handlers = this.messageHandlers.get(topic);
    handlers.delete(callback);

    if (handlers.size === 0) {
      console.log(`[KafkaService] No more handlers for topic ${topic}, unsubscribing from STOMP.`);
      this.messageHandlers.delete(topic);
      const stompSubscription = this.subscriptions.get(topic);
      if (stompSubscription) {
        try {
          stompSubscription.unsubscribe();
          console.log(`[KafkaService] Unsubscribed from STOMP destination /topic/${topic}`);
        } catch (error) {
          console.error(`[KafkaService] Error during STOMP unsubscribe for topic ${topic}:`, error);
        }
        this.subscriptions.delete(topic);
        
        // Inform backend about the unsubscription (optional)
        // this.sendMessageToServer({ type: 'UNSUBSCRIBE', topic: topic });
      }
    }
  }

  /**
   * 发送消息到Kafka主题
   * @param {string} topic Kafka主题
   * @param {object} data 要发送的数据
   */
  publish(topic, data) {
    console.log(`[KafkaService] publish called for topic: ${topic}`);
    if (!topic || data === undefined) {
      console.error('[KafkaService] Invalid arguments for publish.');
      return Promise.reject(new Error('Invalid arguments for publish'));
    }
    const message = {
      type: 'PUBLISH', // Assuming backend expects this structure
      topic: topic,
      data: data
    };
    return this.sendMessageToServer(message);
  }

  sendMessageToServer(message) {
    if (!this.connected || !this.stompClient) {
      console.error('[KafkaService] Cannot send message, WebSocket not connected.');
      return Promise.reject(new Error('WebSocket not connected'));
    }

    const destination = '/app/kafka'; // Backend STOMP endpoint
    console.log(`[KafkaService] Sending message to ${destination}:`, message);
    return new Promise((resolve, reject) => {
      try {
        this.stompClient.send(destination, JSON.stringify(message), {});
        console.log('[KafkaService] Message sent successfully.');
        resolve();
      } catch (error) {
        console.error('[KafkaService] Failed to send message via STOMP:', error);
        reject(error);
      }
    });
  }

  /**
   * 关闭连接
   */
  disconnect() {
    console.log('[KafkaService] disconnect called.');
    if (this.stompClient && this.connected) {
      console.log('[KafkaService] Disconnecting STOMP client...');
      try {
        this.stompClient.disconnect(() => {
          console.log('[KafkaService] STOMP disconnected callback fired.');
          this._cleanupConnection();
        });
      } catch (error) {
        console.error('[KafkaService] Error during STOMP disconnect:', error);
        this._cleanupConnection(); // Ensure cleanup even if disconnect throws
      }
    } else if (this.socket) {
      console.log('[KafkaService] Closing SockJS socket directly...');
      this._cleanupConnection();
    } else {
      console.log('[KafkaService] Already disconnected.');
    }
    this.isConnecting = false; // Stop any connection attempts
  }

  _cleanupConnection() {
    this.connected = false;
    this.isConnecting = false;
    if(this.socket) {
      try { this.socket.close(); } catch(e) { /* ignore */ }
      this.socket = null;
    }
    this.stompClient = null;
    this.subscriptions.clear();
    console.log('[KafkaService] Connection cleaned up.');
  }
}

// Export a single instance (Singleton pattern)
const kafkaServiceInstance = new KafkaService();
export default kafkaServiceInstance; 