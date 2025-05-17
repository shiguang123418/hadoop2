<template>
  <div class="kafka-control-panel">
    <h3>Kafka流处理控制</h3>
    <div class="control-container">
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
        
        <button 
          class="btn generate-btn" 
          @click="generateRandomData"
          :disabled="loading || !streamingActive"
        >
          生成测试数据
        </button>
      </div>
      
      <div v-if="showTopicSelect" class="topic-selector">
        <h4>选择订阅主题</h4>
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

    <div class="data-generator-panel" v-if="showDataGenerator">
      <h3>数据生成器</h3>
      <div class="generator-controls">
        <div class="form-group">
          <label>数据条数：</label>
          <input type="number" v-model.number="dataCount" min="1" max="100" step="1">
        </div>
        
        <div class="form-group">
          <button class="btn generate-btn" @click="sendBatchData" :disabled="generatorLoading">
            {{ generatorLoading ? '生成中...' : '发送批量数据' }}
          </button>
        </div>
      </div>
      
      <div class="generator-result" v-if="generatorResult">
        <div class="alert" :class="generatorResult.success ? 'success' : 'error'">
          {{ generatorResult.message }}
        </div>
      </div>
    </div>
    
    <div class="alert error" v-if="error">
      {{ error }}
    </div>
  </div>
</template>

<script>
import KafkaAnalyticsService from '../services/KafkaAnalyticsService';

export default {
  name: 'KafkaControlPanel',
  props: {
    showTopicSelect: {
      type: Boolean,
      default: true
    },
    showDataGenerator: {
      type: Boolean,
      default: true
    }
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
      dataCount: 10,
      generatorLoading: false,
      generatorResult: null
    };
  },
  async created() {
    try {
      await this.checkStreamingStatus();
    } catch (err) {
      console.error('Failed to get streaming status:', err);
      this.error = '无法获取流处理状态';
    }
  },
  methods: {
    async checkStreamingStatus() {
      this.loading = true;
      try {
        const response = await KafkaAnalyticsService.getStreamingStatus();
        this.streamingActive = response.data.active;
        this.error = null;
      } catch (err) {
        console.error('Error checking streaming status:', err);
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
          // Stop streaming
          const response = await KafkaAnalyticsService.stopStreaming();
          if (response.data.success) {
            this.streamingActive = false;
            this.$emit('streaming-stopped');
          } else {
            this.error = '停止流处理失败：' + response.data.message;
          }
        } else {
          // Start streaming
          const response = await KafkaAnalyticsService.startStreaming(this.selectedTopics);
          if (response.data.success) {
            this.streamingActive = true;
            this.$emit('streaming-started', this.selectedTopics);
          } else {
            this.error = '启动流处理失败：' + response.data.message;
          }
        }
      } catch (err) {
        console.error('Error toggling streaming:', err);
        this.error = '操作流处理失败：' + (err.response?.data?.message || err.message);
      } finally {
        this.loading = false;
      }
    },
    
    async generateRandomData() {
      try {
        await KafkaAnalyticsService.sendSingleData();
        this.$emit('data-generated');
      } catch (err) {
        console.error('Error generating random data:', err);
        this.error = '生成测试数据失败：' + (err.response?.data?.message || err.message);
      }
    },
    
    async sendBatchData() {
      this.generatorLoading = true;
      this.generatorResult = null;
      
      try {
        const response = await KafkaAnalyticsService.sendBatchData(this.dataCount);
        this.generatorResult = {
          success: response.data.success,
          message: response.data.message
        };
        this.$emit('batch-data-generated', this.dataCount);
      } catch (err) {
        console.error('Error sending batch data:', err);
        this.generatorResult = {
          success: false,
          message: '生成批量数据失败：' + (err.response?.data?.message || err.message)
        };
      } finally {
        this.generatorLoading = false;
      }
    }
  }
};
</script>

<style scoped>
.kafka-control-panel {
  background-color: #f9f9f9;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-weight: 500;
}

h4 {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 500;
  font-size: 14px;
}

.control-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  background-color: #ff4d4f;
  display: inline-block;
}

.status-dot.active {
  background-color: #52c41a;
  box-shadow: 0 0 8px rgba(82, 196, 26, 0.5);
}

.control-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.btn {
  padding: 8px 16px;
  border-radius: 4px;
  border: none;
  background-color: #1890ff;
  color: white;
  cursor: pointer;
  transition: all 0.3s;
}

.btn:hover {
  background-color: #40a9ff;
}

.btn:disabled {
  background-color: #d9d9d9;
  cursor: not-allowed;
}

.btn.active {
  background-color: #ff4d4f;
}

.btn.active:hover {
  background-color: #ff7875;
}

.btn.generate-btn {
  background-color: #52c41a;
}

.btn.generate-btn:hover {
  background-color: #73d13d;
}

.btn.generate-btn:disabled {
  background-color: #d9d9d9;
}

.topic-selector {
  margin-top: 8px;
}

.topic-options {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.topic-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.topic-desc {
  color: #999;
  font-size: 12px;
  margin-left: 4px;
}

.data-generator-panel {
  margin-top: 16px;
  border-top: 1px solid #eee;
  padding-top: 16px;
}

.generator-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: center;
}

.form-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-group input {
  width: 80px;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #d9d9d9;
}

.generator-result {
  margin-top: 12px;
}

.alert {
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 12px;
}

.alert.error {
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  color: #f5222d;
}

.alert.success {
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #52c41a;
}

@media (max-width: 768px) {
  .control-actions {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style> 