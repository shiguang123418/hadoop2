<template>
  <div class="sensor-dashboard">
    <div class="dashboard-header">
      <h3>实时传感器数据分析</h3>
      <div class="reload-action">
        <button class="btn reload-btn" @click="loadData" :disabled="loading">
          <span class="reload-icon" :class="{ loading }"></span>
          刷新数据
        </button>
      </div>
    </div>
    
    <div class="test-panel">
      <h4>系统测试</h4>
      <div class="test-actions">
        <button class="btn test-btn" @click="testPing">测试API</button>
        <button class="btn test-btn" @click="testDb">测试数据库</button>
        <button class="btn test-btn" @click="testSaveData">保存测试数据</button>
        <button class="btn test-btn" @click="startKafkaStream">启动Kafka流处理</button>
        <button class="btn test-btn" @click="stopKafkaStream">停止Kafka流处理</button>
      </div>
      <div class="test-result" v-if="testResult">
        <pre>{{ testResult }}</pre>
      </div>
    </div>
    
    <div class="metrics-summary" v-if="summary">
      <div class="metric-card">
        <div class="metric-value">{{ summary.totalReadings || 0 }}</div>
        <div class="metric-label">总数据量</div>
      </div>
      
      <div class="metric-card">
        <div class="metric-value">{{ summary.anomalies || 0 }}</div>
        <div class="metric-label">异常数据</div>
      </div>
      
      <div class="metric-card">
        <div class="metric-value">{{ formatPercentage(summary.anomalyPercentage) }}%</div>
        <div class="metric-label">异常率</div>
      </div>
      
      <div class="metric-card">
        <div class="metric-value">{{ summary.regions || 0 }}</div>
        <div class="metric-label">区域数量</div>
      </div>
      
      <div class="metric-card">
        <div class="metric-value">{{ summary.cropTypes || 0 }}</div>
        <div class="metric-label">作物类型</div>
      </div>
    </div>
    
    <div class="dashboard-content">
      <div class="data-section" v-if="!loading && latestReadings.length > 0">
        <h3>最新传感器数据</h3>
        <div class="readings-table">
          <table>
            <thead>
              <tr>
                <th>传感器ID</th>
                <th>传感器类型</th>
                <th>区域</th>
                <th>作物类型</th>
                <th>数值</th>
                <th>时间</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(reading, index) in latestReadings" :key="index" :class="{ anomaly: reading.anomaly }">
                <td>{{ reading.sensorId }}</td>
                <td>{{ reading.sensorType }}</td>
                <td>{{ reading.region }}</td>
                <td>{{ reading.cropType }}</td>
                <td>{{ reading.value }} {{ reading.unit }}</td>
                <td>{{ formatDateTime(reading.timestamp) }}</td>
                <td>
                  <span class="status-tag" :class="{ anomaly: reading.anomaly }">
                    {{ reading.anomaly ? '异常' : '正常' }}
                  </span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <div class="data-section" v-if="!loading && regionAverages.length > 0">
        <h3>区域数据平均值</h3>
        <div ref="regionChart" class="chart-container"></div>
      </div>
      
      <div class="data-section" v-if="!loading && cropAverages.length > 0">
        <h3>作物数据平均值</h3>
        <div ref="cropChart" class="chart-container"></div>
      </div>
      
      <div class="data-section" v-if="!loading && anomalies.length > 0">
        <h3>异常数据列表</h3>
        <div class="anomalies-list">
          <div v-for="(anomaly, index) in anomalies" :key="index" class="anomaly-card">
            <div class="anomaly-header">
              <span class="anomaly-tag">异常</span>
              <span class="anomaly-time">{{ formatDateTime(anomaly.timestamp) }}</span>
            </div>
            <div class="anomaly-content">
              <div class="anomaly-detail">
                <strong>传感器:</strong> {{ anomaly.sensorId }} ({{ anomaly.sensorType }})
              </div>
              <div class="anomaly-detail">
                <strong>区域:</strong> {{ anomaly.region }}
              </div>
              <div class="anomaly-detail">
                <strong>作物:</strong> {{ anomaly.cropType }}
              </div>
              <div class="anomaly-detail">
                <strong>数值:</strong> {{ anomaly.value }} {{ anomaly.unit }}
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <div class="no-data" v-if="!loading && !hasData">
        暂无传感器数据，请确保Kafka流处理已启动并生成了一些测试数据。
      </div>
      
      <div class="loading-overlay" v-if="loading">
        <div class="loading-spinner"></div>
        <div class="loading-text">加载数据中...</div>
      </div>
    </div>
    
    <div class="error-message" v-if="error">
      {{ error }}
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import { format, parseISO } from 'date-fns';
import KafkaAnalyticsService from '../services/KafkaAnalyticsService';

export default {
  name: 'SensorDataDashboard',
  props: {
    autoRefresh: {
      type: Boolean,
      default: true
    },
    refreshInterval: {
      type: Number,
      default: 5000 // 改为5秒刷新一次
    }
  },
  data() {
    return {
      loading: false,
      error: null,
      latestReadings: [],
      regionAverages: [],
      cropAverages: [],
      anomalies: [],
      summary: null,
      charts: {
        region: null,
        crop: null
      },
      refreshTimer: null,
      testResult: null
    };
  },
  computed: {
    hasData() {
      return this.latestReadings.length > 0 || 
             this.regionAverages.length > 0 || 
             this.cropAverages.length > 0;
    }
  },
  mounted() {
    this.loadData();
    this.startAutoRefresh();
    window.addEventListener('resize', this.resizeCharts);
  },
  beforeUnmount() {
    this.stopAutoRefresh();
    this.destroyCharts();
    window.removeEventListener('resize', this.resizeCharts);
  },
  methods: {
    async testPing() {
      try {
        this.testResult = "正在测试API连接...";
        const response = await KafkaAnalyticsService.ping();
        this.testResult = JSON.stringify(response.data, null, 2);
      } catch (err) {
        this.testResult = `API连接失败: ${err.message}\n${err.stack}`;
        console.error('API连接测试失败:', err);
      }
    },
    
    async testDb() {
      try {
        this.testResult = "正在测试数据库连接...";
        const response = await KafkaAnalyticsService.testDb();
        this.testResult = JSON.stringify(response.data, null, 2);
      } catch (err) {
        this.testResult = `数据库连接失败: ${err.message}\n${err.stack}`;
        console.error('数据库连接测试失败:', err);
      }
    },
    
    async testSaveData() {
      try {
        this.testResult = "正在保存测试数据...";
        const response = await KafkaAnalyticsService.saveTestData();
        this.testResult = JSON.stringify(response.data, null, 2);
        await this.loadData(); // 刷新数据显示
      } catch (err) {
        this.testResult = `保存测试数据失败: ${err.message}\n${err.stack}`;
        console.error('保存测试数据失败:', err);
      }
    },
    
    async startKafkaStream() {
      try {
        this.testResult = "正在启动Kafka流处理...";
        const response = await KafkaAnalyticsService.startStreaming(['agriculture-sensor-data']);
        this.testResult = JSON.stringify(response.data, null, 2);
      } catch (err) {
        this.testResult = `启动Kafka流处理失败: ${err.message}\n${err.stack}`;
        console.error('启动Kafka流处理失败:', err);
      }
    },
    
    async stopKafkaStream() {
      try {
        this.testResult = "正在停止Kafka流处理...";
        const response = await KafkaAnalyticsService.stopStreaming();
        this.testResult = JSON.stringify(response.data, null, 2);
      } catch (err) {
        this.testResult = `停止Kafka流处理失败: ${err.message}\n${err.stack}`;
        console.error('停止Kafka流处理失败:', err);
      }
    },
    
    startAutoRefresh() {
      if (this.autoRefresh) {
        this.refreshTimer = setInterval(() => {
          this.loadData();
        }, this.refreshInterval);
      }
    },
    
    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    },
    
    async loadData() {
      if (this.loading) return;
      
      this.loading = true;
      this.error = null;
      
      try {
        await Promise.all([
          this.loadLatestReadings(),
          this.loadRegionAverages(),
          this.loadCropAverages(),
          this.loadAnomalies(),
          this.loadSummary()
        ]);
        
        this.$nextTick(() => {
          this.renderCharts();
        });
      } catch (err) {
        console.error('Error loading sensor data:', err);
        this.error = '加载数据失败: ' + (err.response?.data?.message || err.message);
      } finally {
        this.loading = false;
      }
    },
    
    async loadLatestReadings() {
      try {
        const response = await KafkaAnalyticsService.getLatestReadings();
        this.latestReadings = response.data || [];
      } catch (err) {
        console.error('Error loading latest readings:', err);
        this.latestReadings = [];
        throw err;
      }
    },
    
    async loadRegionAverages() {
      try {
        const response = await KafkaAnalyticsService.getRegionAverages();
        this.regionAverages = response.data || [];
      } catch (err) {
        console.error('Error loading region averages:', err);
        this.regionAverages = [];
        throw err;
      }
    },
    
    async loadCropAverages() {
      try {
        const response = await KafkaAnalyticsService.getCropAverages();
        this.cropAverages = response.data || [];
      } catch (err) {
        console.error('Error loading crop averages:', err);
        this.cropAverages = [];
        throw err;
      }
    },
    
    async loadAnomalies() {
      try {
        const response = await KafkaAnalyticsService.getAnomalies();
        this.anomalies = response.data || [];
      } catch (err) {
        console.error('Error loading anomalies:', err);
        this.anomalies = [];
        throw err;
      }
    },
    
    async loadSummary() {
      try {
        const response = await KafkaAnalyticsService.getSummary();
        this.summary = response.data || {
          totalReadings: 0,
          anomalies: 0,
          anomalyPercentage: 0,
          regions: 0,
          cropTypes: 0
        };
      } catch (err) {
        console.error('Error loading summary:', err);
        this.summary = null;
        throw err;
      }
    },
    
    renderCharts() {
      this.renderRegionChart();
      this.renderCropChart();
    },
    
    renderRegionChart() {
      if (this.regionAverages.length === 0) return;
      
      // Process data for chart
      const regions = [...new Set(this.regionAverages.map(item => item.region))];
      const sensorTypes = [...new Set(this.regionAverages.map(item => item.sensorType))];
      
      // Create series for each sensor type
      const series = sensorTypes.map(sensorType => {
        const data = regions.map(region => {
          const item = this.regionAverages.find(avg => 
            avg.region === region && avg.sensorType === sensorType
          );
          return item ? item.avgValue : 0;
        });
        
        return {
          name: sensorType,
          type: 'bar',
          data,
          label: {
            show: true,
            formatter: '{c}',
            position: 'top'
          }
        };
      });
      
      // Initialize chart if it doesn't exist
      if (!this.charts.region) {
        this.charts.region = echarts.init(this.$refs.regionChart);
      }
      
      // Set chart options
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: sensorTypes,
          top: 'top'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: regions,
          axisLabel: {
            rotate: 45,
            interval: 0
          }
        },
        yAxis: {
          type: 'value',
          name: '平均值'
        },
        series
      };
      
      this.charts.region.setOption(option);
    },
    
    renderCropChart() {
      if (this.cropAverages.length === 0) return;
      
      // Process data for chart
      const crops = [...new Set(this.cropAverages.map(item => item.cropType))];
      const sensorTypes = [...new Set(this.cropAverages.map(item => item.sensorType))];
      
      // Initialize chart if it doesn't exist
      if (!this.charts.crop) {
        this.charts.crop = echarts.init(this.$refs.cropChart);
      }
      
      // Create series for each sensor type
      const series = sensorTypes.map(sensorType => {
        const data = crops.map(crop => {
          const item = this.cropAverages.find(avg => 
            avg.cropType === crop && avg.sensorType === sensorType
          );
          return item ? item.avgValue : 0;
        });
        
        return {
          name: sensorType,
          type: 'bar',
          data,
          label: {
            show: true,
            formatter: '{c}',
            position: 'top'
          }
        };
      });
      
      // Set chart options
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: sensorTypes,
          top: 'top'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: crops,
          axisLabel: {
            rotate: 45,
            interval: 0
          }
        },
        yAxis: {
          type: 'value',
          name: '平均值'
        },
        series
      };
      
      this.charts.crop.setOption(option);
    },
    
    resizeCharts() {
      Object.values(this.charts).forEach(chart => {
        if (chart) {
          chart.resize();
        }
      });
    },
    
    destroyCharts() {
      Object.values(this.charts).forEach(chart => {
        if (chart) {
          chart.dispose();
        }
      });
      this.charts = {
        region: null,
        crop: null
      };
    },
    
    formatDateTime(timestamp) {
      if (!timestamp) return '';
      try {
        const date = typeof timestamp === 'string' ? parseISO(timestamp) : new Date(timestamp);
        return format(date, 'yyyy-MM-dd HH:mm:ss');
      } catch (e) {
        console.error('Error formatting date:', e, timestamp);
        return timestamp;
      }
    },
    
    formatPercentage(value) {
      if (value === undefined || value === null) return '0.00';
      return Number(value).toFixed(2);
    }
  }
};
</script>

<style scoped>
.sensor-dashboard {
  background-color: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.dashboard-header h3 {
  margin: 0;
  font-weight: 500;
}

.reload-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  background-color: #f0f0f0;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  color: #595959;
  cursor: pointer;
  transition: all 0.3s;
}

.reload-btn:hover {
  background-color: #e6f7ff;
  border-color: #91d5ff;
  color: #1890ff;
}

.reload-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.reload-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  border: 2px solid transparent;
  border-top-color: currentColor;
  border-radius: 50%;
}

.reload-icon.loading {
  animation: spin 1s infinite linear;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.metrics-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.metric-card {
  padding: 16px;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%);
  color: white;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.metric-card::before {
  content: '';
  position: absolute;
  top: -20px;
  right: -20px;
  width: 60px;
  height: 60px;
  background-color: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 8px;
}

.metric-label {
  font-size: 14px;
  opacity: 0.8;
}

.dashboard-content {
  position: relative;
  min-height: 300px;
}

.data-section {
  margin-bottom: 32px;
}

.data-section h3 {
  margin-top: 0;
  margin-bottom: 16px;
  font-weight: 500;
}

.readings-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

th {
  background-color: #fafafa;
  font-weight: 500;
}

tr:hover {
  background-color: #fafafa;
}

tr.anomaly {
  background-color: #fff1f0;
}

tr.anomaly:hover {
  background-color: #ffccc7;
}

.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  color: #52c41a;
}

.status-tag.anomaly {
  background-color: #fff1f0;
  border-color: #ffa39e;
  color: #ff4d4f;
}

.chart-container {
  height: 400px;
  border: 1px solid #f0f0f0;
  border-radius: 4px;
}

.anomalies-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 16px;
}

.anomaly-card {
  border: 1px solid #ffa39e;
  border-radius: 4px;
  overflow: hidden;
}

.anomaly-header {
  display: flex;
  justify-content: space-between;
  padding: 8px 12px;
  background-color: #fff1f0;
}

.anomaly-tag {
  color: #ff4d4f;
  font-weight: 500;
}

.anomaly-time {
  font-size: 12px;
  color: #595959;
}

.anomaly-content {
  padding: 12px;
}

.anomaly-detail {
  margin-bottom: 6px;
}

.no-data, .loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  background-color: rgba(255, 255, 255, 0.8);
  z-index: 10;
}

.no-data {
  color: #999;
}

.loading-spinner {
  display: inline-block;
  width: 50px;
  height: 50px;
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top-color: #1890ff;
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 16px;
}

.loading-text {
  color: #1890ff;
  font-weight: 500;
}

.error-message {
  padding: 12px;
  margin-top: 16px;
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #ff4d4f;
}

@media (max-width: 768px) {
  .metrics-summary {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 480px) {
  .metrics-summary {
    grid-template-columns: 1fr;
  }
  
  .anomalies-list {
    grid-template-columns: 1fr;
  }
}

.test-panel {
  margin: 20px 0;
  padding: 15px;
  background-color: #f5f5f5;
  border-radius: 5px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.test-panel h4 {
  margin-top: 0;
  margin-bottom: 10px;
  color: #333;
}

.test-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 10px;
}

.test-btn {
  padding: 8px 12px;
  background-color: #f0f0f0;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.test-btn:hover {
  background-color: #e0e0e0;
}

.test-result {
  padding: 10px;
  background-color: #f9f9f9;
  border: 1px solid #eee;
  border-radius: 4px;
  overflow: auto;
  max-height: 200px;
  font-family: monospace;
  font-size: 12px;
}
</style> 