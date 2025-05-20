<template>
  <div class="service-status">
    <div class="status-header">
      <h3>服务状态监控</h3>
      <div class="header-actions">
        <button @click="refreshAllStatus" class="refresh-all-btn" :disabled="refreshing">
          <span class="btn-icon refresh-icon"></span>
          <span>{{ refreshing ? '刷新中...' : '刷新全部' }}</span>
        </button>
        <button @click="runDiagnostics" class="diagnostic-btn" :disabled="runningDiagnostic">
          <span class="btn-icon diagnostic-icon"></span>
          <span>{{ runningDiagnostic ? '诊断中...' : '系统诊断' }}</span>
        </button>
      </div>
    </div>
    
    <div class="status-line">
      <!-- HDFS状态 -->
      <div class="service-item" :class="{ 'connected': hdfsStatus, 'disconnected': !hdfsStatus }">
        <div class="service-icon hdfs-icon"></div>
        <div class="service-info">
          <span class="service-name">HDFS</span>
          <span class="status-badge" :class="{ 'status-online': hdfsStatus, 'status-offline': !hdfsStatus }">
            {{ hdfsStatus ? '运行中' : '未连接' }}
          </span>
        </div>
        <div class="service-url">{{ hdfsUri }}</div>
        <button @click="checkHdfsStatus" class="refresh-btn" title="刷新HDFS状态">
          <span class="refresh-icon"></span>
        </button>
      </div>
      
      <!-- Hive状态 -->
      <div class="service-item" :class="{ 'connected': hiveStatus, 'disconnected': !hiveStatus }">
        <div class="service-icon hive-icon"></div>
        <div class="service-info">
          <span class="service-name">Hive</span>
          <span class="status-badge" :class="{ 'status-online': hiveStatus, 'status-offline': !hiveStatus }">
            {{ hiveStatus ? '运行中' : '未连接' }}
          </span>
        </div>
        <div class="service-url">{{ hiveUrl }}</div>
        <button @click="checkHiveStatus" class="refresh-btn" title="刷新Hive状态">
          <span class="refresh-icon"></span>
        </button>
      </div>
      
      <!-- Spark状态 -->
      <div class="service-item connected">
        <div class="service-icon spark-icon"></div>
        <div class="service-info">
          <span class="service-name">Spark</span>
          <span class="status-badge status-online">运行中</span>
        </div>
        <div class="service-url">http://localhost:4040</div>
        <button class="refresh-btn" title="刷新Spark状态">
          <span class="refresh-icon"></span>
        </button>
      </div>
      
      <!-- Kafka状态 -->
      <div class="service-item connected">
        <div class="service-icon kafka-icon"></div>
        <div class="service-info">
          <span class="service-name">Kafka</span>
          <span class="status-badge status-online">运行中</span>
        </div>
        <div class="service-url">localhost:9092</div>
        <button class="refresh-btn" title="刷新Kafka状态">
          <span class="refresh-icon"></span>
        </button>
      </div>
    </div>
    
    <!-- 诊断结果区域 -->
    <div v-if="diagnosticResults.length > 0" class="diagnostic-results">
      <div class="diagnostic-header">
        <h4>诊断结果</h4>
        <button @click="diagnosticResults = []" class="close-btn">
          <span class="close-icon"></span>
        </button>
      </div>
      <ul class="diagnostic-list">
        <li v-for="(result, index) in diagnosticResults" :key="index"
            :class="{ 'result-success': result.status === 'success', 
                     'result-warning': result.status === 'warning', 
                     'result-error': result.status === 'error',
                     'result-info': result.status === 'info' }">
          <span class="result-icon"></span>
          <span class="result-text">{{ result.message }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { HDFSService, HiveService } from '@/services';

const hdfsStatus = ref(false);
const hdfsUri = ref('');
const hdfsError = ref('');

const hiveStatus = ref(false);
const hiveUrl = ref('');
const hiveError = ref('');

const refreshing = ref(false);
const runningDiagnostic = ref(false);
const diagnosticResults = ref([]);

// 初始加载时检查状态
onMounted(() => {
  refreshAllStatus();
});

// 刷新所有服务状态
const refreshAllStatus = async () => {
  refreshing.value = true;
  try {
    await Promise.all([
      checkHdfsStatus(),
      checkHiveStatus()
    ]);
  } finally {
    refreshing.value = false;
  }
};

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
</script>

<style scoped>
.service-status {
  padding: 1.5rem;
  background-color: var(--bg-light);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-md);
  margin-bottom: 1rem;
}

.status-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  gap: 1rem;
}

.status-header h3 {
  margin: 0;
  color: var(--text-color);
  font-size: 1.4rem;
  font-weight: 600;
}

.header-actions {
  display: flex;
  gap: 0.8rem;
}

.refresh-all-btn, .diagnostic-btn {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1rem;
  border: none;
  border-radius: 4px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-all-btn {
  background-color: var(--primary-light);
  color: var(--primary-dark);
}

.refresh-all-btn:hover {
  background-color: var(--primary-color);
  color: white;
}

.diagnostic-btn {
  background-color: #E3F2FD;
  color: #1976D2;
}

.diagnostic-btn:hover {
  background-color: #1976D2;
  color: white;
}

.btn-icon {
  width: 16px;
  height: 16px;
  display: inline-block;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.refresh-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='currentColor'%3E%3Cpath d='M17.65 6.35a7.95 7.95 0 0 0-6.15-2.85c-4.42 0-8 3.58-8 8s3.58 8 8 8c3.73 0 6.84-2.55 7.73-6h-2.08A5.99 5.99 0 0 1 11.5 17.5c-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 10h7V3l-2.35 3.35z'/%3E%3C/svg%3E");
}

.diagnostic-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='currentColor'%3E%3Cpath d='M15.9 5c-.17 0-.32.09-.41.23l-.07.15-5.18 11.65c-.16.29-.26.64-.26 1.01 0 1.1.9 2 2 2 .96 0 1.77-.68 1.96-1.59l.01-.03 1.93-6.78c.04-.12.18-.88.18-.88.04-.13.16-.23.3-.23s.26.1.3.23l.18.88 1.93 6.78.01.03c.2.91 1 1.59 1.96 1.59 1.1 0 2-.9 2-2 0-.37-.1-.72-.25-1.01l-5.18-11.65-.07-.15c-.09-.14-.24-.23-.41-.23-1.19 0-2.18.91-2.29 2.08l-.95 7.93-.7-7.92c-.12-1.17-1.1-2.09-2.29-2.09zm-10.10 6.52l1.33 1.33c.31-.3.44-.44.87-.44 1.06 0 1.92.86 1.92 1.92 0 1.07-.86 1.92-1.92 1.92-1.07 0-1.92-.85-1.92-1.92 0-.44.14-.63.44-.87l-1.33-1.33c-.14-.12-.29-.17-.45-.17a.637.637 0 0 0-.64.64c0 .16.05.31.14.45 0 0 1.32 1.32 1.32 1.32-1.15 1.19-1.64 2.43-1.62 3.89.2.16.08.32.19.44.11.12.26.19.43.19h1c.36.1.62-.26.62-.62.05-1.37.46-2.35 1.18-2.99h.24c1.41 0 2.56 1.15 2.56 2.56s-1.15 2.56-2.56 2.56h-1.88c-.23 0-.46.09-.63.26-.17.16-.26.39-.26.63 0 .35.07.69.26.94.1.09.9.81.9.81.29.28.67.43 1.07.43h1.32c.5 0 .98-.09 1.42-.25 1.97-.71 3.38-2.58 3.38-4.77 0-2.2-1.42-4.08-3.38-4.79-.51-.19-1.07-.29-1.65-.29-.86 0-1.68.21-2.41.58-.71.36-1.31.88-1.79 1.53l-1.33-1.33c-.14-.09-.29-.14-.45-.14a.637.637 0 0 0-.64.64c0 .16.05.32.17.45z'/%3E%3C/svg%3E");
}

/* Status-line*/
.status-line {
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  flex-wrap: nowrap;
  background-color: #f8f9fa;
  border-radius: 8px;
  padding: 0.8rem;
  margin-bottom: 1rem;
  box-shadow: var(--shadow-sm);
  gap: 1rem;
}

.service-item {
  display: flex;
  align-items: center;
  padding: 0.5rem 1rem;
  background-color: white;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  min-width: 0;
  flex: 1;
  gap: 0.8rem;
}

.service-item.connected {
  border-left: 3px solid #4CAF50;
}

.service-item.disconnected {
  border-left: 3px solid #F44336;
}

.service-icon {
  width: 24px;
  height: 24px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  flex-shrink: 0;
}

.hdfs-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23FF5722'%3E%3Cpath d='M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z'/%3E%3C/svg%3E");
}

.hive-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23FFC107'%3E%3Cpath d='M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 15.5h-1.5V14h-1v3H8v-3H7v4.5H5.5v-5c0-.55.45-1 1-1H11c.55 0 1 .45 1 1v5zm3.5 0H14v-6h3.5c.55 0 1 .45 1 1V16c0 .55-.45 1-1 1h-2v1.5zM10 5.5v6H8.5V7H7V5.5h3zm5 6.5h-1.75L14.62 15H12V9h3.5c.55 0 1 .45 1 1v1c0 .55-.45 1-1 1z'/%3E%3C/svg%3E");
}

.spark-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%232196F3'%3E%3Cpath d='M14 12l-2 2-2-2 2-2 2 2zm-2-6l2.12 2.12 2.5-2.5L12 1 7.38 5.62l2.5 2.5L12 6zm-6 6l2.12-2.12-2.5-2.5L1 12l5.62 4.62 2.5-2.5L6 12zm12 0l-2.12 2.12 2.5 2.5L23 12l-5.62-4.62-2.5 2.5L18 12zm-6 6l-2.12-2.12-2.5 2.5L12 23l4.62-4.62-2.5-2.5L12 18z'/%3E%3C/svg%3E");
}

.kafka-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%239C27B0'%3E%3Cpath d='M21.71 11.29l-9-9a.996.996 0 0 0-1.41 0l-9 9a.996.996 0 0 0 0 1.41l9 9c.39.39 1.02.39 1.41 0l9-9a.996.996 0 0 0 0-1.41zM14 14.5V12h-4v3H8v-4c0-.55.45-1 1-1h5V7.5l3.5 3.5-3.5 3.5z'/%3E%3C/svg%3E");
}

.service-info {
  display: flex;
  flex-direction: column;
  min-width: 80px;
  flex-shrink: 0;
}

.service-name {
  font-weight: 600;
  font-size: 0.9rem;
  color: var(--text-color);
}

.status-badge {
  display: inline-block;
  font-size: 0.75rem;
  font-weight: 500;
  padding: 0.15rem 0.5rem;
  border-radius: 12px;
  margin-top: 0.2rem;
}

.status-online {
  background-color: #E8F5E9;
  color: #2E7D32;
}

.status-offline {
  background-color: #FFEBEE;
  color: #C62828;
}

.service-url {
  font-size: 0.8rem;
  color: var(--text-light);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.refresh-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background-color: #f0f0f0;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.refresh-btn:hover {
  background-color: #e0e0e0;
}

.refresh-btn .refresh-icon {
  width: 16px;
  height: 16px;
}

/* Diagnostic results */
.diagnostic-results {
  margin-top: 1.5rem;
  background-color: white;
  border-radius: 8px;
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.diagnostic-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem;
  background-color: #f5f5f5;
  border-bottom: 1px solid #e0e0e0;
}

.diagnostic-header h4 {
  margin: 0;
  font-size: 1.1rem;
  font-weight: 600;
  color: var(--text-color);
}

.close-btn {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background-color: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.close-icon {
  width: 16px;
  height: 16px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23757575'%3E%3Cpath d='M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.diagnostic-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.diagnostic-list li {
  display: flex;
  align-items: center;
  gap: 0.8rem;
  padding: 0.8rem 1rem;
  border-bottom: 1px solid #f0f0f0;
}

.diagnostic-list li:last-child {
  border-bottom: none;
}

.result-icon {
  width: 20px;
  height: 20px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  flex-shrink: 0;
}

.result-success .result-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23388E3C'%3E%3Cpath d='M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z'/%3E%3C/svg%3E");
}

.result-warning .result-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23F57C00'%3E%3Cpath d='M11 15h2v2h-2zm0-8h2v6h-2zm1-5C6.47 2 2 6.5 2 12a10 10 0 0 0 10 10 10 10 0 0 0 10-10A10 10 0 0 0 12 2zm0 18a8 8 0 0 1-8-8 8 8 0 0 1 8-8 8 8 0 0 1 8 8 8 8 0 0 1-8 8z'/%3E%3C/svg%3E");
}

.result-error .result-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23D32F2F'%3E%3Cpath d='M11 15h2v2h-2zm0-8h2v6h-2zm.99-5C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z'/%3E%3C/svg%3E");
}

.result-info .result-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%231976D2'%3E%3Cpath d='M11 7h2v2h-2zm0 4h2v6h-2zm1-9C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8z'/%3E%3C/svg%3E");
}

.result-text {
  font-size: 0.9rem;
  line-height: 1.4;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .status-header {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .header-actions {
    width: 100%;
    justify-content: space-between;
  }
  
  .refresh-all-btn, .diagnostic-btn {
    flex: 1;
    justify-content: center;
  }
  
  .status-line {
    flex-direction: column;
    gap: 0.5rem;
  }
  
  .service-item {
    width: 100%;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .status-line {
    flex-wrap: wrap;
  }
  
  .service-item {
    flex: 0 0 calc(50% - 0.5rem);
  }
}
</style> 