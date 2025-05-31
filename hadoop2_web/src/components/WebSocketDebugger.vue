<template>
  <div class="websocket-debugger">
    <el-card class="debug-card">
      <template #header>
        <div class="card-header">
          <h3>WebSocket 连接调试</h3>
          <el-tag :type="isConnected ? 'success' : 'danger'">{{ isConnected ? '已连接' : '未连接' }}</el-tag>
        </div>
      </template>
      
      <div class="connection-info">
        <h4>连接信息</h4>
        <div class="info-item">
          <span class="label">连接地址:</span>
          <span class="value">{{ wsUrl }}</span>
        </div>
        <div class="info-item">
          <span class="label">连接状态:</span>
          <span class="value" :class="{ connected: isConnected, disconnected: !isConnected }">
            {{ isConnected ? '已连接' : '未连接' }}
          </span>
        </div>
        <div class="info-item">
          <span class="label">已订阅主题:</span>
          <span class="value">{{ Object.keys(subscriptions).join(', ') || '无' }}</span>
        </div>
      </div>
      
      <div class="action-buttons">
        <el-button type="primary" @click="connect" :disabled="isConnected">连接</el-button>
        <el-button type="danger" @click="disconnect" :disabled="!isConnected">断开</el-button>
        <el-button type="success" @click="sendTestData" :disabled="!isConnected">发送测试数据</el-button>
      </div>
      
      <div class="logs-section">
        <div class="logs-header">
          <h4>连接日志</h4>
          <el-button size="small" @click="clearLogs">清除日志</el-button>
        </div>
        
        <div class="logs-container">
          <div v-for="(log, index) in logs" :key="index" class="log-item" :class="log.type">
            <span class="log-time">{{ log.time }}</span>
            <span class="log-content">{{ log.message }}</span>
          </div>
        </div>
      </div>
      
      <div class="technical-info">
        <h4>技术信息</h4>
        <div class="info-item">
          <span class="label">主机名:</span>
          <span class="value">{{ window.location.hostname }}</span>
        </div>
        <div class="info-item">
          <span class="label">端口:</span>
          <span class="value">{{ window.location.port }}</span>
        </div>
        <div class="info-item">
          <span class="label">协议:</span>
          <span class="value">{{ window.location.protocol }}</span>
        </div>
        <div class="info-item">
          <span class="label">完整URL:</span>
          <span class="value">{{ window.location.href }}</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import websocketManager from '../utils/websocket';
import { SensorApi } from '../api/sensor';

// WebSocket连接地址
const wsUrl = ref('/api_ws');

// 连接状态
const isConnected = ref(false);

// 已订阅的主题
const subscriptions = ref({});

// 日志
const logs = ref([]);

// 添加日志
function addLog(message, type = 'info') {
  logs.value.push({
    time: new Date().toLocaleTimeString(),
    message,
    type
  });
  
  // 限制日志数量
  if (logs.value.length > 50) {
    logs.value.shift();
  }
}

// 清除日志
function clearLogs() {
  logs.value = [];
}

// 连接WebSocket
async function connect() {
  try {
    addLog('正在连接WebSocket...', 'info');
    await websocketManager.connect();
    isConnected.value = websocketManager.isConnected();
    
    if (isConnected.value) {
      addLog('WebSocket连接成功!', 'success');
      
      // 订阅默认主题
      await subscribeToTopics();
    } else {
      addLog('WebSocket连接失败，但未抛出错误', 'error');
    }
  } catch (error) {
    addLog(`WebSocket连接失败: ${error.message || '未知错误'}`, 'error');
    console.error('WebSocket连接异常:', error);
  }
}

// 断开WebSocket连接
function disconnect() {
  try {
    websocketManager.disconnect();
    isConnected.value = false;
    addLog('WebSocket连接已断开', 'info');
  } catch (error) {
    addLog(`WebSocket断开失败: ${error.message || '未知错误'}`, 'error');
    console.error('WebSocket断开异常:', error);
  }
}

// 订阅主题
async function subscribeToTopics() {
  try {
    // 订阅传感器数据主题
    await websocketManager.subscribe('/topic/agriculture-sensor-data', (data) => {
      addLog(`收到传感器数据: ${JSON.stringify(data).substring(0, 100)}...`, 'received');
      subscriptions.value['/topic/agriculture-sensor-data'] = true;
    });
    addLog('已订阅传感器数据主题', 'success');
    
    // 订阅Spark统计数据主题
    await websocketManager.subscribe('/topic/spark-stats', (data) => {
      addLog(`收到Spark统计数据: ${JSON.stringify(data).substring(0, 100)}...`, 'received');
      subscriptions.value['/topic/spark-stats'] = true;
    });
    addLog('已订阅Spark统计数据主题', 'success');
    
    // 订阅系统通知主题
    await websocketManager.subscribe('/topic/system-notifications', (data) => {
      addLog(`收到系统通知: ${JSON.stringify(data)}`, 'received');
      subscriptions.value['/topic/system-notifications'] = true;
    });
    addLog('已订阅系统通知主题', 'success');
  } catch (error) {
    addLog(`订阅主题失败: ${error.message || '未知错误'}`, 'error');
    console.error('订阅主题异常:', error);
  }
}

// 发送测试数据
async function sendTestData() {
  try {
    addLog('正在发送测试数据...', 'info');
    const response = await SensorApi.sendTestData();
    addLog(`测试数据发送成功: ${JSON.stringify(response.data || {})}`, 'success');
  } catch (error) {
    addLog(`测试数据发送失败: ${error.message || '未知错误'}`, 'error');
    console.error('测试数据发送异常:', error);
  }
}

// 监听WebSocket事件
websocketManager.on('connect', () => {
  isConnected.value = true;
  addLog('WebSocket连接成功(事件通知)', 'success');
});

websocketManager.on('disconnect', () => {
  isConnected.value = false;
  addLog('WebSocket连接已断开(事件通知)', 'info');
});

websocketManager.on('error', (error) => {
  addLog(`WebSocket错误: ${error.message || '未知错误'}`, 'error');
});

// 组件挂载时
onMounted(() => {
  // 添加初始日志
  addLog('调试器已加载', 'info');
  addLog(`当前URL: ${window.location.href}`, 'info');
  addLog(`WebSocket连接地址: ${wsUrl.value}`, 'info');
  
  // 自动连接
  connect();
});

// 组件卸载时
onUnmounted(() => {
  // 断开连接
  if (isConnected.value) {
    disconnect();
  }
});
</script>

<style scoped>
.websocket-debugger {
  padding: 20px;
}

.debug-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}

.connection-info {
  margin-bottom: 20px;
}

.connection-info h4,
.logs-section h4,
.technical-info h4 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: bold;
}

.info-item {
  display: flex;
  margin-bottom: 8px;
}

.info-item .label {
  width: 120px;
  color: #606266;
}

.info-item .value {
  flex: 1;
}

.info-item .value.connected {
  color: #67C23A;
  font-weight: bold;
}

.info-item .value.disconnected {
  color: #F56C6C;
  font-weight: bold;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}

.logs-section {
  margin-bottom: 20px;
}

.logs-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.logs-container {
  height: 300px;
  overflow-y: auto;
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  background-color: #F5F7FA;
  padding: 10px;
}

.log-item {
  padding: 5px;
  margin-bottom: 5px;
  border-left: 3px solid;
  background-color: #FFF;
  border-radius: 2px;
}

.log-item.info {
  border-left-color: #409EFF;
}

.log-item.success {
  border-left-color: #67C23A;
}

.log-item.error {
  border-left-color: #F56C6C;
}

.log-item.received {
  border-left-color: #E6A23C;
}

.log-time {
  color: #909399;
  font-size: 12px;
  margin-right: 10px;
}

.technical-info {
  background-color: #F5F7FA;
  border-radius: 4px;
  padding: 10px;
}
</style> 