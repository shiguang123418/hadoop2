<template>
  <div class="sensor-dashboard">
    <div class="dashboard-header">
      <h3>HDFS和Hive数据分析</h3>
    </div>
    
    <div class="metrics-summary">
      <div class="metric-card">
        <div class="metric-value">HDFS</div>
        <div class="metric-label">文件系统</div>
      </div>
      
      <div class="metric-card">
        <div class="metric-value">Hive</div>
        <div class="metric-label">数据仓库</div>
      </div>
    </div>
    
    <div class="dashboard-content">
      <div class="data-section">
        <h3>系统状态</h3>
        <div class="readings-table">
          <table>
            <thead>
              <tr>
                <th>服务名称</th>
                <th>状态</th>
                <th>连接URL</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td>HDFS</td>
                <td>
                  <span class="status-tag" :class="{ 'status-ok': hdfsStatus.connected, 'status-error': !hdfsStatus.connected }">
                    {{ hdfsStatus.connected ? '已连接' : '未连接' }}
                  </span>
                </td>
                <td>{{ hdfsStatus.uri || '未知' }}</td>
              </tr>
              <tr>
                <td>Hive</td>
                <td>
                  <span class="status-tag" :class="{ 'status-ok': hiveStatus.connected, 'status-error': !hiveStatus.connected }">
                    {{ hiveStatus.connected ? '已连接' : '未连接' }}
                  </span>
                </td>
                <td>{{ hiveStatus.url || '未知' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      
      <div class="action-section">
        <h3>系统操作</h3>
        <div class="action-buttons">
          <button class="btn action-btn" @click="refreshStatus" :disabled="loading">
            <span class="reload-icon" :class="{ loading }"></span>
            刷新状态
          </button>
        </div>
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
import { HDFSService, HiveService } from '@/services';

export default {
  name: 'SensorDataDashboard',
  data() {
    return {
      loading: false,
      error: null,
      hdfsStatus: {
        connected: false,
        uri: null
      },
      hiveStatus: {
        connected: false,
        url: null
      },
      refreshTimer: null
    };
  },
  mounted() {
    this.refreshStatus();
    this.startAutoRefresh();
  },
  beforeUnmount() {
    this.stopAutoRefresh();
  },
  methods: {
    async refreshStatus() {
      this.loading = true;
      this.error = null;
      
      try {
        // 获取HDFS状态
        const hdfsResponse = await HDFSService.getStatus();
        this.hdfsStatus = hdfsResponse.data;
        
        // 获取Hive状态
        const hiveResponse = await HiveService.getStatus();
        this.hiveStatus = hiveResponse.data;
      } catch (err) {
        this.error = `无法获取服务状态: ${err.message}`;
        console.error('获取服务状态失败:', err);
      } finally {
        this.loading = false;
      }
    },
    
    startAutoRefresh() {
      // 每60秒自动刷新一次状态
        this.refreshTimer = setInterval(() => {
        this.refreshStatus();
      }, 60000);
    },
    
    stopAutoRefresh() {
      if (this.refreshTimer) {
        clearInterval(this.refreshTimer);
        this.refreshTimer = null;
      }
    }
  }
};
</script>

<style scoped>
.sensor-dashboard {
  padding: 20px;
  background-color: #f5f8fa;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dashboard-header h3 {
  margin: 0;
  color: #333;
  font-size: 1.5rem;
}

.metrics-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 25px;
}

.metric-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 15px;
  min-width: 150px;
  flex: 1;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
  text-align: center;
  transition: transform 0.2s;
}

.metric-card:hover {
  transform: translateY(-2px);
}

.metric-value {
  font-size: 1.8rem;
  font-weight: bold;
  margin-bottom: 5px;
  color: #1a73e8;
}

.metric-label {
  font-size: 0.9rem;
  color: #777;
}

.data-section, .action-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.05);
}

.data-section h3, .action-section h3 {
  margin-top: 0;
  margin-bottom: 15px;
  color: #333;
  font-size: 1.2rem;
}

.readings-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background-color: #f5f7fa;
  text-align: left;
  padding: 12px 15px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #e1e4e8;
}

td {
  padding: 12px 15px;
  border-bottom: 1px solid #e1e4e8;
}

.status-tag {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-ok {
  background-color: #e6f4ea;
  color: #137333;
}

.status-error {
  background-color: #fce8e6;
  color: #c5221f;
}

.action-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.btn {
  background-color: #1a73e8;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 15px;
  cursor: pointer;
  font-size: 0.9rem;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
}

.btn:hover {
  background-color: #1765cc;
}

.btn:disabled {
  background-color: #c2c2c2;
  cursor: not-allowed;
}

.reload-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  margin-right: 6px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='white'%3E%3Cpath d='M17.65 6.35a8 8 0 1 0 1.4 1.4l-1.4-1.4zm-1.07 3.54A6 6 0 1 1 12 6v2l3-3-3-3v2a8 8 0 1 0 7.54 10.54l-1.41-1.41a6 6 0 0 1-5.55 3.87 6 6 0 0 1-6-6 6 6 0 0 1 6-6 6 6 0 0 1 2.95.77'/%3E%3C/svg%3E");
  background-size: cover;
}

.loading {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.8);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  z-index: 10;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #1a73e8;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

.loading-text {
  margin-top: 10px;
  color: #333;
}

.error-message {
  background-color: #fce8e6;
  color: #c5221f;
  padding: 15px;
  border-radius: 8px;
  margin-top: 20px;
  font-size: 0.9rem;
}
</style> 