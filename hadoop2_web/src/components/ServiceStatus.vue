<template>
  <div class="service-status">
    <h3>服务状态</h3>
    
    <div class="status-container">
      <div class="status-card" :class="{ 'connected': hdfsStatus, 'disconnected': !hdfsStatus }">
        <div class="service-icon">📂</div>
        <div class="service-info">
          <div class="service-name">HDFS</div>
          <div class="status-text">{{ hdfsStatus ? '已连接' : '未连接' }}</div>
          <div class="service-url" v-if="hdfsStatus">{{ hdfsUri }}</div>
          <div class="error-message" v-if="hdfsError">{{ hdfsError }}</div>
        </div>
        <button @click="checkHdfsStatus" class="refresh-btn">刷新</button>
      </div>
      
      <div class="status-card" :class="{ 'connected': hiveStatus, 'disconnected': !hiveStatus }">
        <div class="service-icon">🗄️</div>
        <div class="service-info">
          <div class="service-name">Hive</div>
          <div class="status-text">{{ hiveStatus ? '已连接' : '未连接' }}</div>
          <div class="service-url" v-if="hiveStatus">{{ hiveUrl }}</div>
          <div class="error-message" v-if="hiveError">{{ hiveError }}</div>
        </div>
        <button @click="checkHiveStatus" class="refresh-btn">刷新</button>
      </div>
    </div>
    
    <div class="diagnostic-tools">
      <button @click="runDiagnostics" class="diagnostic-btn" :disabled="runningDiagnostic">
        {{ runningDiagnostic ? '诊断中...' : '运行诊断' }}
      </button>
      
      <div v-if="diagnosticResults.length > 0" class="diagnostic-results">
        <h4>诊断结果</h4>
        <ul>
          <li v-for="(result, index) in diagnosticResults" :key="index"
              :class="{ 'success': result.status === 'success', 'warning': result.status === 'warning', 'error': result.status === 'error' }">
            {{ result.message }}
          </li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { HDFSService, HiveService } from '@/services';

export default {
  name: 'ServiceStatus',
  
  setup() {
    const hdfsStatus = ref(false);
    const hdfsUri = ref('');
    const hdfsError = ref('');
    
    const hiveStatus = ref(false);
    const hiveUrl = ref('');
    const hiveError = ref('');
    
    const runningDiagnostic = ref(false);
    const diagnosticResults = ref([]);
    
    // 初始加载时检查状态
    onMounted(() => {
      checkHdfsStatus();
      checkHiveStatus();
    });
    
    // 检查HDFS状态
    const checkHdfsStatus = async () => {
      try {
        const response = await HDFSService.getStatus();
        hdfsStatus.value = response.connected;
        hdfsUri.value = response.uri || '';
        hdfsError.value = '';
      } catch (err) {
        console.error('获取HDFS状态失败:', err);
        hdfsStatus.value = false;
        hdfsUri.value = '';
        hdfsError.value = err.response?.data?.error || err.message || '连接服务器失败';
      }
    };
    
    // 检查Hive状态
    const checkHiveStatus = async () => {
      try {
        const response = await HiveService.getStatus();
        hiveStatus.value = response.connected;
        hiveUrl.value = response.url || '';
        hiveError.value = '';
      } catch (err) {
        console.error('获取Hive状态失败:', err);
        hiveStatus.value = false;
        hiveUrl.value = '';
        hiveError.value = err.response?.data?.error || err.message || '连接服务器失败';
      }
    };
    
    // 运行诊断
    const runDiagnostics = async () => {
      runningDiagnostic.value = true;
      diagnosticResults.value = [];
      
      try {
        // 检查API服务可达性
        try {
          await HDFSService.getStatus();
          diagnosticResults.value.push({
            status: 'success',
            message: 'HDFS API服务可达'
          });
        } catch (err) {
          diagnosticResults.value.push({
            status: 'error',
            message: `HDFS API服务不可达: ${err.message}`
          });
        }
        
        try {
          await HiveService.getStatus();
          diagnosticResults.value.push({
            status: 'success',
            message: 'Hive API服务可达'
          });
        } catch (err) {
          diagnosticResults.value.push({
            status: 'error',
            message: `Hive API服务不可达: ${err.message}`
          });
        }
        
        // 检查HDFS连接状态
        if (hdfsStatus.value) {
          diagnosticResults.value.push({
            status: 'success',
            message: `HDFS连接正常: ${hdfsUri.value}`
          });
          
          // 测试列出根目录
          try {
            const response = await HDFSService.listFiles('/');
            diagnosticResults.value.push({
              status: 'success',
              message: `HDFS根目录列表获取成功，包含 ${response.length} 个项目`
            });
          } catch (err) {
            diagnosticResults.value.push({
              status: 'warning',
              message: `HDFS根目录列表获取失败: ${err.response?.data?.error || err.message}`
            });
          }
        } else {
          diagnosticResults.value.push({
            status: 'error',
            message: `HDFS连接失败: ${hdfsError.value}`
          });
          
          // 提供可能的解决方案
          diagnosticResults.value.push({
            status: 'warning',
            message: '可能原因: 1. HDFS服务未运行 2. 连接地址错误 3. 没有权限访问'
          });
        }
        
        // 检查Hive连接状态
        if (hiveStatus.value) {
          diagnosticResults.value.push({
            status: 'success',
            message: `Hive连接正常: ${hiveUrl.value}`
          });
          
          // 测试获取数据库列表
          try {
            const response = await HiveService.getDatabases();
            diagnosticResults.value.push({
              status: 'success',
              message: `Hive数据库列表获取成功，包含 ${response.length} 个数据库`
            });
          } catch (err) {
            diagnosticResults.value.push({
              status: 'warning',
              message: `Hive数据库列表获取失败: ${err.response?.data?.error || err.message}`
            });
          }
        } else {
          diagnosticResults.value.push({
            status: 'error',
            message: `Hive连接失败: ${hiveError.value}`
          });
          
          // 提供可能的解决方案
          diagnosticResults.value.push({
            status: 'warning',
            message: '可能原因: 1. Hive服务未运行 2. 连接地址错误 3. JDBC驱动问题'
          });
        }
        
        // 检查网络连接
        diagnosticResults.value.push({
          status: 'info',
          message: `当前网络信息: IP=${window.location.hostname}, 端口=${window.location.port}, 协议=${window.location.protocol}`
        });
        
      } catch (err) {
        console.error('运行诊断失败:', err);
        diagnosticResults.value.push({
          status: 'error',
          message: `诊断过程出错: ${err.message}`
        });
      } finally {
        runningDiagnostic.value = false;
      }
    };
    
    return {
      hdfsStatus,
      hdfsUri,
      hdfsError,
      hiveStatus,
      hiveUrl,
      hiveError,
      runningDiagnostic,
      diagnosticResults,
      checkHdfsStatus,
      checkHiveStatus,
      runDiagnostics
    };
  }
};
</script>

<style scoped>
.service-status {
  padding: 1rem;
  background-color: #f9f9f9;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

h3 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: #333;
}

.status-container {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.status-card {
  flex: 1;
  min-width: 250px;
  display: flex;
  align-items: center;
  padding: 1rem;
  border-radius: 8px;
  background-color: white;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.connected {
  border-left: 4px solid #4CAF50;
}

.disconnected {
  border-left: 4px solid #f44336;
}

.service-icon {
  font-size: 2rem;
  margin-right: 1rem;
}

.service-info {
  flex: 1;
}

.service-name {
  font-weight: bold;
  color: #333;
}

.status-text {
  font-size: 0.9rem;
  margin-top: 0.25rem;
}

.connected .status-text {
  color: #4CAF50;
}

.disconnected .status-text {
  color: #f44336;
}

.service-url {
  font-size: 0.8rem;
  color: #666;
  margin-top: 0.25rem;
  word-break: break-all;
}

.error-message {
  font-size: 0.8rem;
  color: #f44336;
  margin-top: 0.25rem;
}

.refresh-btn {
  padding: 0.5rem;
  background-color: #f1f1f1;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.refresh-btn:hover {
  background-color: #e0e0e0;
}

.diagnostic-tools {
  margin-top: 1.5rem;
}

.diagnostic-btn {
  width: 100%;
  padding: 0.75rem;
  background-color: #2196F3;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

.diagnostic-btn:hover:not(:disabled) {
  background-color: #0b7dda;
}

.diagnostic-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.diagnostic-results {
  margin-top: 1rem;
  padding: 1rem;
  background-color: white;
  border-radius: 4px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.diagnostic-results h4 {
  margin-top: 0;
  margin-bottom: 0.5rem;
  color: #333;
}

.diagnostic-results ul {
  padding-left: 1.5rem;
  margin-bottom: 0;
}

.diagnostic-results li {
  margin-bottom: 0.5rem;
}

.diagnostic-results li:last-child {
  margin-bottom: 0;
}

.diagnostic-results .success {
  color: #4CAF50;
}

.diagnostic-results .warning {
  color: #ff9800;
}

.diagnostic-results .error {
  color: #f44336;
}

.diagnostic-results .info {
  color: #2196F3;
}
</style> 