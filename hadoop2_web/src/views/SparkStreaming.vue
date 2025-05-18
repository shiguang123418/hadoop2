<template>
  <div class="spark-streaming">
    <h2>实时数据流处理监控</h2>
    
    <!-- 连接状态指示器 -->
    <div class="connection-status">
      <div class="status-dot" :class="{ active: wsConnected }"></div>
      <span>WebSocket状态: {{ connectionStatus }}</span>
      <span v-if="lastUpdated" class="last-updated">
        上次更新: {{ formatTime(lastUpdated) }}
      </span>
    </div>
    
    <!-- Kafka流处理控制面板 -->
    <div class="control-panel">
      <div class="status-section">
        <div class="status-indicator">
          <div class="status-dot" :class="{ active: streamingActive }"></div>
          <span>状态：{{ streamingActive ? '运行中' : '已停止' }}</span>
        </div>
        
        <div class="control-actions">
          <button 
            class="btn" 
            :class="{ active: streamingActive }"
            @click="toggleStreaming"
            :disabled="loading"
          >
            {{ streamingActive ? '停止流处理' : '启动流处理' }}
          </button>
        </div>
      </div>
      
      <div class="topic-selector">
        <h4>订阅主题</h4>
        <div class="topic-options">
          <label class="topic-option" v-for="topic in availableTopics" :key="topic.id">
            <input 
              type="checkbox" 
              :value="topic.id" 
              v-model="selectedTopics"
              :disabled="streamingActive"
            >
            <span>{{ topic.name }}</span>
            <span class="topic-desc">{{ topic.description }}</span>
          </label>
        </div>
      </div>
    </div>
    
    <div class="alert error" v-if="error">
      {{ error }}
    </div>
    
    <!-- 传感器数据看板 -->
    <SensorDataDashboard 
      ref="dashboard" 
      :auto-refresh="dashboardAutoRefresh" 
      :refresh-interval="10000"
      @connection-status-change="updateConnectionStatus" 
    />

    <!-- 数据统计卡片 -->
    <div class="statistics-cards">
      <div class="statistic-card">
        <div class="card-value">{{ totalMessages }}</div>
        <div class="card-title">总数据量</div>
      </div>
      
      <div class="statistic-card">
        <div class="card-value">{{ processedMessages }}</div>
        <div class="card-title">异常数据</div>
      </div>
      
      <div class="statistic-card">
        <div class="card-value">{{ processingRate }}%</div>
        <div class="card-title">异常率</div>
      </div>
      
      <div class="statistic-card">
        <div class="card-value">{{ regionsCount }}</div>
        <div class="card-title">区域数量</div>
      </div>
      
      <div class="statistic-card">
        <div class="card-value">{{ sensorTypesCount }}</div>
        <div class="card-title">作物类型</div>
      </div>
    </div>
  </div>
</template>

<script>
import KafkaAnalyticsService from '../services/KafkaAnalyticsService';
import SensorDataDashboard from '../components/SensorDataDashboard.vue';
import { format } from 'date-fns';

export default {
  name: 'SparkStreaming',
  components: {
    SensorDataDashboard
  },
  data() {
    return {
      streamingActive: false,
      loading: false,
      error: null,
      availableTopics: [
        { id: 'agriculture-sensor-data', name: '农业传感器数据', description: '温度、湿度、光照等传感器数据' },
        { id: 'agriculture-weather-data', name: '气象数据', description: '气象站收集的天气数据' },
        { id: 'agriculture-market-data', name: '市场数据', description: '农产品价格和销量数据' }
      ],
      selectedTopics: ['agriculture-sensor-data'],
      dashboardAutoRefresh: true,
      // 统计数据
      totalMessages: 2756,
      processedMessages: 800,
      processingRate: 29.03,
      regionsCount: 50,
      sensorTypesCount: 50,
      // 轮询定时器
      statusTimer: null,
      statsTimer: null,
      // WebSocket状态
      wsConnected: false,
      connectionStatus: '未连接',
      lastUpdated: null
    };
  },
  
  async created() {
    try {
      await this.checkStreamingStatus();
      this.startPolling();
      await this.fetchStats();
      // 尝试建立WebSocket连接
      this.connectWebSocket();
    } catch (err) {
      console.error('初始化失败:', err);
      this.error = '无法获取流处理状态或统计信息';
    }
  },
  
  beforeUnmount() {
    this.stopPolling();
    // 关闭WebSocket连接
    this.disconnectWebSocket();
  },
  
  methods: {
    async checkStreamingStatus() {
      this.loading = true;
      try {
        const response = await KafkaAnalyticsService.getStreamingStatus();
        this.streamingActive = response.data.active;
        this.error = null;
      } catch (err) {
        console.error('检查流处理状态出错:', err);
        this.error = '检查流处理状态失败：' + (err.response?.data?.message || err.message);
      } finally {
        this.loading = false;
      }
    },
    
    async toggleStreaming() {
      this.loading = true;
      this.error = null;
      
      try {
        if (this.streamingActive) {
          // 停止流处理
          const response = await KafkaAnalyticsService.stopStreaming();
          if (response.data.success) {
            this.streamingActive = false;
            this.dashboardAutoRefresh = false;
          } else {
            this.error = '停止流处理失败：' + response.data.message;
          }
        } else {
          // 启动流处理
          const response = await KafkaAnalyticsService.startStreaming(this.selectedTopics);
          if (response.data.success) {
            this.streamingActive = true;
            this.dashboardAutoRefresh = true;
            this.refreshDashboard();
          } else {
            this.error = '启动流处理失败：' + response.data.message;
          }
        }
      } catch (err) {
        console.error('切换流处理状态时出错:', err);
        this.error = '操作流处理失败：' + (err.response?.data?.message || err.message);
      } finally {
        this.loading = false;
      }
    },
    
    refreshDashboard() {
      if (this.$refs.dashboard) {
        this.$refs.dashboard.loadData();
      }
    },
    
    startPolling() {
      // 每30秒检查一次状态
      this.statusTimer = setInterval(() => {
        this.checkStreamingStatus();
      }, 30000);
      
      // 每分钟更新一次统计信息
      this.statsTimer = setInterval(() => {
        this.fetchStats();
      }, 60000);
    },
    
    stopPolling() {
      if (this.statusTimer) {
        clearInterval(this.statusTimer);
      }
      if (this.statsTimer) {
        clearInterval(this.statsTimer);
      }
    },
    
    async fetchStats() {
      try {
        const response = await KafkaAnalyticsService.getAnalyticsSummary();
        if (response.data) {
          const data = response.data;
          this.totalMessages = data.totalReadings || 0;
          this.processedMessages = data.anomalies || 0;
          this.processingRate = data.anomalyPercentage?.toFixed(2) || 0;
          this.regionsCount = data.regions || 0;
          this.sensorTypesCount = data.cropTypes || 0;
        }
      } catch (err) {
        console.error('获取统计数据时出错:', err);
      }
    },
    
    // WebSocket相关方法
    async connectWebSocket() {
      try {
        const connected = await KafkaAnalyticsService.connectWebSocket(this.handleWebSocketData);
        
        if (connected) {
          this.wsConnected = true;
          this.connectionStatus = '已连接';
          console.log('WebSocket连接成功');
        } else {
          this.wsConnected = false;
          this.connectionStatus = '连接失败';
          console.error('WebSocket连接失败，将使用轮询模式');
        }
      } catch (err) {
        this.wsConnected = false;
        this.connectionStatus = '连接错误';
        console.error('WebSocket连接错误:', err);
      }
    },
    
    disconnectWebSocket() {
      KafkaAnalyticsService.closeWebSocket();
      this.wsConnected = false;
      this.connectionStatus = '已断开';
    },
    
    handleWebSocketData(data) {
      console.log('收到WebSocket数据:', data);
      this.lastUpdated = new Date();
      
      if (data.type === 'summary') {
        this.totalMessages = data.summary.totalReadings || 0;
        this.processedMessages = data.summary.anomalies || 0;
        this.processingRate = data.summary.anomalyPercentage?.toFixed(2) || 0;
        this.regionsCount = data.summary.regions || 0;
        this.sensorTypesCount = data.summary.cropTypes || 0;
      } else if (data.type === 'status') {
        this.streamingActive = data.active;
      }
    },
    
    updateConnectionStatus(status) {
      this.wsConnected = status.connected;
      this.connectionStatus = status.status;
      if (status.lastUpdated) {
        this.lastUpdated = status.lastUpdated;
      }
    },
    
    formatTime(date) {
      if (!date) return '';
      return format(new Date(date), 'HH:mm:ss');
    }
  }
};
</script>

<style scoped>
.spark-streaming {
  padding: 20px;
}

h2 {
  margin-bottom: 16px;
  color: #1e293b;
}

.connection-status {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 8px 16px;
  background-color: #f8fafc;
  border-radius: 8px;
  border-left: 4px solid #64748b;
}

.connection-status .status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #ef4444;
  display: inline-block;
}

.connection-status .status-dot.active {
  background-color: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
}

.last-updated {
  margin-left: auto;
  color: #64748b;
  font-size: 14px;
}

.control-panel {
  background-color: #f8fafc;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.status-section {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #ef4444;
  display: inline-block;
}

.status-dot.active {
  background-color: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.5);
}

.control-actions {
  display: flex;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  background-color: #3b82f6;
  color: white;
  border: none;
  cursor: pointer;
  font-weight: 500;
  transition: background-color 0.2s;
}

.btn:disabled {
  background-color: #94a3b8;
  cursor: not-allowed;
}

.btn.active {
  background-color: #ef4444;
}

.btn:hover:not(:disabled) {
  background-color: #2563eb;
}

.btn.active:hover:not(:disabled) {
  background-color: #dc2626;
}

.topic-selector {
  margin-top: 16px;
}

.topic-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 8px;
}

.topic-option {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.topic-option input {
  margin-right: 8px;
}

.topic-desc {
  color: #64748b;
  margin-left: 8px;
  font-size: 13px;
}

.alert {
  padding: 12px 16px;
  border-radius: 4px;
  margin-bottom: 20px;
}

.error {
  background-color: #fee2e2;
  color: #b91c1c;
  border: 1px solid #f87171;
}

.statistics-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px;
  margin-top: 24px;
}

.statistic-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  text-align: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s, box-shadow 0.2s;
}

.statistic-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
}

.card-value {
  font-size: 28px;
  font-weight: 600;
  color: #0369a1;
  margin-bottom: 8px;
}

.card-title {
  font-size: 14px;
  color: #64748b;
}
</style> 