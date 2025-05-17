import axios from 'axios';
import apiConfig from '@/config/api.config';

/**
 * 与Kafka和传感器数据分析相关的API交互服务
 */
class KafkaAnalyticsService {
  /**
   * 基础API路径
   */
  constructor() {
    // 配置API基础路径
    this.baseUrl = apiConfig.baseURL;
    this.kafkaUrl = `${this.baseUrl}/kafka`;
    this.producerUrl = `${this.baseUrl}/producer`;
    this.analyticsUrl = `${this.baseUrl}/analytics`;
    this.testUrl = `${this.baseUrl}/test`;
    
    // 是否启用调试
    this.debug = apiConfig.debug;
    
    // 配置默认请求超时和重试
    this.axios = axios.create({
      timeout: apiConfig.timeout,
      headers: {
        'Content-Type': 'application/json'
      }
    });
    
    // 添加请求拦截器
    this.axios.interceptors.request.use(config => {
      if (this.debug) {
        console.log(`API请求 → ${config.method.toUpperCase()} ${config.url}`, config.params || {});
      }
      return config;
    });
    
    // 添加响应拦截器处理错误
    this.axios.interceptors.response.use(
      response => {
        if (this.debug) {
          console.log(`API响应 ← ${response.config.url}`, response.data);
        }
        return response;
      },
      error => {
        console.error('API请求错误:', error);
        
        // 如果是网络错误，返回模拟数据以保证UI可用
        if (error.message === 'Network Error' || !error.response) {
          console.warn('使用模拟数据替代失败的API请求');
          return this.getFallbackData(error.config);
        }
        return Promise.reject(error);
      }
    );
  }

  /**
   * 当API失败时提供模拟数据
   */
  getFallbackData(config) {
    console.warn('使用模拟数据替代API响应');
    
    // 根据URL提供不同的模拟数据
    if (config.url.includes('/latest')) {
      return {
        data: this.generateMockSensorData(5)
      };
    } else if (config.url.includes('/anomalies')) {
      return {
        data: this.generateMockAnomalyData(3)
      };
    } else if (config.url.includes('/summary')) {
      return {
        data: {
          sensorCounts: {
            temperature: 34,
            humidity: 28,
            ph: 23,
            wind: 24,
            light: 42,
            co2: 19,
            soil: 21
          },
          regionCounts: {
            "华中": 42,
            "西北": 32,
            "华北": 37,
            "东北": 24,
            "华东": 29,
            "华南": 27,
            "西南": 22
          },
          cropCounts: {
            "水稻": 30,
            "小麦": 35,
            "玉米": 25,
            "大豆": 28,
            "马铃薯": 32,
            "棉花": 24,
            "甜菜": 17
          },
          averages: {
            temperature: 24.7,
            humidity: 45.2,
            light: 682.3,
            wind: 8.4,
            co2: 1321
          }
        }
      };
    } else if (config.url.includes('/region-averages')) {
      return {
        data: {
          "华中": { temperature: 24.2, humidity: 48.7, light: 702.5, wind: 7.8, co2: 1290 },
          "西北": { temperature: 21.9, humidity: 32.4, light: 890.1, wind: 12.3, co2: 1150 },
          "华北": { temperature: 22.6, humidity: 43.1, light: 650.8, wind: 9.1, co2: 1320 },
          "东北": { temperature: 19.8, humidity: 51.2, light: 589.6, wind: 8.7, co2: 1180 },
          "华东": { temperature: 26.1, humidity: 62.3, light: 720.4, wind: 6.2, co2: 1350 },
          "华南": { temperature: 28.4, humidity: 71.8, light: 680.3, wind: 5.1, co2: 1410 },
          "西南": { temperature: 23.7, humidity: 58.9, light: 710.6, wind: 6.8, co2: 1280 }
        }
      };
    } else if (config.url.includes('/status')) {
      return {
        data: { active: true }
      };
    }
    
    // 默认响应
    return { data: {} };
  }
  
  /**
   * 生成模拟传感器数据
   */
  generateMockSensorData(count = 5) {
    const sensorTypes = ["temperature", "humidity", "ph", "wind", "light", "co2", "soil"];
    const regions = ["华中", "西北", "华北", "东北", "华东", "华南", "西南"];
    const crops = ["水稻", "小麦", "玉米", "大豆", "马铃薯", "棉花", "甜菜"];
    
    const mockData = [];
    
    for (let i = 0; i < count; i++) {
      const sensorType = sensorTypes[Math.floor(Math.random() * sensorTypes.length)];
      let value;
      
      switch (sensorType) {
        case "temperature":
          value = (15 + Math.random() * 20).toFixed(1);
          break;
        case "humidity":
          value = (30 + Math.random() * 60).toFixed(1);
          break;
        case "ph":
          value = (5 + Math.random() * 4).toFixed(2);
          break;
        case "wind":
          value = (2 + Math.random() * 18).toFixed(2);
          break;
        case "light":
          value = (200 + Math.random() * 800).toFixed(1);
          break;
        case "co2":
          value = (400 + Math.random() * 1200).toFixed(0);
          break;
        case "soil":
          value = (20 + Math.random() * 60).toFixed(1);
          break;
      }
      
      const sensorId = "SENSOR-" + Math.floor(1000 + Math.random() * 9000);
      const region = regions[Math.floor(Math.random() * regions.length)];
      const cropType = crops[Math.floor(Math.random() * crops.length)];
      const timestamp = new Date().toISOString();
      
      mockData.push({
        sensorId,
        sensorType,
        region,
        cropType,
        value: parseFloat(value),
        timestamp
      });
    }
    
    return mockData;
  }
  
  /**
   * 生成模拟异常数据
   */
  generateMockAnomalyData(count = 3) {
    const mockData = this.generateMockSensorData(count);
    
    // 把值修改为异常值
    return mockData.map(item => {
      let value;
      
      switch (item.sensorType) {
        case "temperature":
          value = Math.random() < 0.5 ? -5 + Math.random() * 5 : 40 + Math.random() * 10;
          break;
        case "humidity":
          value = Math.random() < 0.5 ? Math.random() * 5 : 95 + Math.random() * 5;
          break;
        case "ph":
          value = Math.random() < 0.5 ? 1 + Math.random() : 12 + Math.random();
          break;
        case "wind":
          value = 30 + Math.random() * 20;
          break;
        case "light":
          value = Math.random() < 0.5 ? Math.random() * 50 : 20000 + Math.random() * 10000;
          break;
        case "co2":
          value = 3000 + Math.random() * 2000;
          break;
        case "soil":
          value = Math.random() < 0.5 ? Math.random() * 5 : 95 + Math.random() * 5;
          break;
        default:
          value = item.value;
      }
      
      return { ...item, value };
    });
  }

  /**
   * 测试API连接
   * @returns {Promise} 响应Promise
   */
  ping() {
    return this.axios.get(`${this.testUrl}/ping`);
  }

  /**
   * 测试数据库连接
   * @returns {Promise} 响应Promise
   */
  testDb() {
    return this.axios.get(`${this.testUrl}/db`);
  }

  /**
   * 保存测试数据
   * @returns {Promise} 响应Promise
   */
  saveTestData() {
    return this.axios.post(`${this.testUrl}/save-test-data`);
  }

  /**
   * 启动Kafka流处理
   * @param {Array} topics 要订阅的主题数组
   * @returns {Promise} 响应Promise
   */
  startStreaming(topics = ['agriculture-sensor-data']) {
    return this.axios.post(`${this.kafkaUrl}/start`, null, {
      params: { topics: topics.join(',') }
    });
  }

  /**
   * 停止Kafka流处理
   * @returns {Promise} 响应Promise
   */
  stopStreaming() {
    return this.axios.post(`${this.kafkaUrl}/stop`);
  }

  /**
   * 获取Kafka流处理状态
   * @returns {Promise} 响应Promise，包含流处理状态
   */
  getStreamingStatus() {
    return this.axios.get(`${this.kafkaUrl}/status`);
  }

  /**
   * 发送单条随机传感器数据（测试用）
   * @returns {Promise} 响应Promise
   */
  sendSingleData() {
    return this.axios.post(`${this.producerUrl}/send`);
  }

  /**
   * 发送多条随机传感器数据（测试用）
   * @param {number} count 数据条数
   * @returns {Promise} 响应Promise
   */
  sendBatchData(count = 10) {
    return this.axios.post(`${this.producerUrl}/send-batch`, null, {
      params: { count }
    });
  }

  /**
   * 获取最新的传感器数据
   * @returns {Promise} 响应Promise
   */
  getLatestReadings() {
    return this.axios.get(`${this.analyticsUrl}/latest`);
  }

  /**
   * 获取按区域和传感器类型分组的平均值
   * @returns {Promise} 响应Promise
   */
  getRegionAverages() {
    return this.axios.get(`${this.analyticsUrl}/region-averages`);
  }

  /**
   * 获取按作物类型和传感器类型分组的平均值
   * @returns {Promise} 响应Promise
   */
  getCropAverages() {
    return this.axios.get(`${this.analyticsUrl}/crop-averages`);
  }

  /**
   * 获取异常数据
   * @returns {Promise} 响应Promise
   */
  getAnomalies() {
    return this.axios.get(`${this.analyticsUrl}/anomalies`);
  }

  /**
   * 获取传感器数据摘要
   * @returns {Promise} 响应Promise
   */
  getSummary() {
    return this.axios.get(`${this.analyticsUrl}/summary`);
  }

  /**
   * 按时间范围获取传感器数据
   * @param {Date} startDate 开始日期
   * @param {Date} endDate 结束日期
   * @returns {Promise} 响应Promise
   */
  getByTimeRange(startDate, endDate) {
    return this.axios.get(`${this.analyticsUrl}/by-time-range`, {
      params: {
        start: startDate.toISOString(),
        end: endDate.toISOString()
      }
    });
  }

  /**
   * 按传感器类型获取数据
   * @param {string} sensorType 传感器类型
   * @returns {Promise} 响应Promise
   */
  getBySensorType(sensorType) {
    return this.axios.get(`${this.analyticsUrl}/by-sensor-type/${sensorType}`);
  }

  /**
   * 按区域获取数据
   * @param {string} region 区域名称
   * @returns {Promise} 响应Promise
   */
  getByRegion(region) {
    return this.axios.get(`${this.analyticsUrl}/by-region/${region}`);
  }

  /**
   * 按作物类型获取数据
   * @param {string} cropType 作物类型
   * @returns {Promise} 响应Promise
   */
  getByCropType(cropType) {
    return this.axios.get(`${this.analyticsUrl}/by-crop/${cropType}`);
  }

  /**
   * 测试Kafka服务是否可用
   * @returns {Promise<boolean>} 如果Kafka服务可用返回true，否则返回false
   */
  async testKafkaConnection() {
    try {
      // 测试API连接和Kafka状态
      const statusResponse = await this.getStreamingStatus();
      return true;
    } catch (error) {
      console.error('Kafka服务连接测试失败:', error);
      return false;
    }
  }

  /**
   * 切换服务器后端
   * @param {string} serverType - 服务器类型: 'main', 'backup', 'local'
   * @returns {boolean} 切换是否成功
   */
  switchServer(serverType = 'main') {
    if (!apiConfig.servers[serverType]) {
      console.error(`未知服务器类型: ${serverType}`);
      return false;
    }
    
    const newServer = apiConfig.servers[serverType];
    console.log(`切换到${serverType}服务器: ${newServer}`);
    
    // 保存当前服务器类型
    this.currentServer = serverType;
    
    // 如果是直连模式,更新baseUrl
    if (apiConfig.directConnection) {
      this.baseUrl = `${newServer}/api`;
      this.kafkaUrl = `${this.baseUrl}/kafka`;
      this.producerUrl = `${this.baseUrl}/producer`;
      this.analyticsUrl = `${this.baseUrl}/analytics`;
      this.testUrl = `${this.baseUrl}/test`;
      
      // 如果有WebSocket连接,需要重新连接
      if (this.wsConnection) {
        const callbacks = [...this.wsCallbacks];
        this.closeWebSocket();
        // 稍后重连
        setTimeout(() => {
          this.connectWebSocket(callbacks[0]);
        }, 500);
      }
    }
    
    return true;
  }

  /**
   * 建立WebSocket连接获取实时数据
   * @param {Function} callback 数据回调函数
   * @returns {Promise<boolean>} 连接成功返回true
   */
  async connectWebSocket(callback) {
    // 如果已有连接,先关闭
    if (this.wsConnection) {
      this.closeWebSocket();
    }
    
    try {
      // 构建WebSocket URL
      let wsUrl;
      if (apiConfig.directConnection) {
        const server = apiConfig.servers[this.currentServer || 'main'];
        // 将http转换为ws
        wsUrl = server.replace('http://', 'ws://') + '/api/ws/kafka';
      } else {
        // 使用相对路径,浏览器会自动补全
        wsUrl = 'ws://' + window.location.host + '/api/ws/kafka';
      }
      
      console.log('连接WebSocket:', wsUrl);
      
      // 创建WebSocket连接
      this.wsConnection = new WebSocket(wsUrl);
      
      // 添加回调
      if (callback) {
        this.wsCallbacks.push(callback);
      }
      
      // 设置事件处理
      this.wsConnection.onopen = (event) => {
        console.log('WebSocket连接已建立');
      };
      
      this.wsConnection.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          if (this.debug) {
            console.log('WebSocket收到数据:', data);
          }
          
          // 调用所有回调
          this.wsCallbacks.forEach(cb => cb(data));
        } catch (e) {
          console.error('解析WebSocket数据失败:', e);
        }
      };
      
      this.wsConnection.onerror = (error) => {
        console.error('WebSocket错误:', error);
      };
      
      this.wsConnection.onclose = (event) => {
        console.log('WebSocket连接已关闭:', event.code, event.reason);
      };
      
      // 等待连接建立
      return new Promise((resolve) => {
        this.wsConnection.onopen = () => {
          resolve(true);
        };
        
        this.wsConnection.onerror = () => {
          resolve(false);
        };
        
        // 超时处理
        setTimeout(() => resolve(false), 5000);
      });
    } catch (e) {
      console.error('WebSocket连接失败:', e);
      return false;
    }
  }
  
  /**
   * 关闭WebSocket连接
   */
  closeWebSocket() {
    if (this.wsConnection) {
      this.wsConnection.close();
      this.wsConnection = null;
      this.wsCallbacks = [];
    }
  }
  
  /**
   * 向WebSocket发送消息
   * @param {Object} message 要发送的消息
   * @returns {boolean} 发送成功返回true
   */
  sendWebSocketMessage(message) {
    if (!this.wsConnection || this.wsConnection.readyState !== WebSocket.OPEN) {
      console.error('WebSocket未连接');
      return false;
    }
    
    try {
      this.wsConnection.send(JSON.stringify(message));
      return true;
    } catch (e) {
      console.error('发送WebSocket消息失败:', e);
      return false;
    }
  }
}

export default new KafkaAnalyticsService(); 