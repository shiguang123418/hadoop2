<template>
  <div class="multi-api-demo">
    <h2>多后端API示例</h2>
    
    <el-card class="api-card">
      <template #header>
        <div class="card-header">
          <h3>API 配置信息</h3>
        </div>
      </template>
      
      <div class="config-info">
        <h4>服务器配置:</h4>
        <pre>{{ JSON.stringify(apiServers, null, 2) }}</pre>
        
        <h4>服务配置:</h4>
        <pre>{{ JSON.stringify(apiServices, null, 2) }}</pre>
      </div>
    </el-card>
    
    <el-divider />
    
    <div class="api-test-section">
      <h3>API 测试</h3>
      
      <el-form :model="apiTestForm" label-width="120px">
        <el-form-item label="选择服务">
          <el-select v-model="apiTestForm.service" placeholder="选择要测试的服务">
            <el-option 
              v-for="(service, key) in apiServices" 
              :key="key" 
              :label="`${key} (${service.server})`" 
              :value="key" 
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="端点路径">
          <el-input v-model="apiTestForm.endpoint" placeholder="输入端点路径，例如 /status"/>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="testApi">测试请求</el-button>
          <el-button @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
      
      <div v-if="apiTestResult" class="api-result">
        <h4>请求结果:</h4>
        <el-alert
          :title="apiTestResult.success ? '请求成功' : '请求失败'"
          :type="apiTestResult.success ? 'success' : 'error'"
          :description="apiTestResult.message"
          show-icon
        />
        
        <div v-if="apiTestResult.data" class="result-data">
          <h4>响应数据:</h4>
          <pre>{{ JSON.stringify(apiTestResult.data, null, 2) }}</pre>
        </div>
      </div>
    </div>
    
    <el-divider />
    
    <div class="api-logs">
      <h3>API 请求日志</h3>
      <el-button size="small" @click="clearLogs">清除日志</el-button>
      
      <div class="log-container">
        <pre v-for="(log, index) in apiLogs" :key="index" :class="'log-' + log.type">{{ log.message }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import axios from 'axios'
import apiConfig from '../config/api.config'
import { buildApiPath } from '../utils/service-helper'

// API配置
const apiServers = computed(() => apiConfig.servers)
const apiServices = computed(() => apiConfig.services)

// API测试表单
const apiTestForm = reactive({
  service: 'hdfs',
  endpoint: '/status'
})

// API测试结果
const apiTestResult = ref(null)
const apiLogs = ref([])

// 添加日志
function addLog(message, type = 'info') {
  apiLogs.value.unshift({
    message,
    type,
    timestamp: new Date().toISOString()
  })
  
  // 限制日志数量
  if (apiLogs.value.length > 20) {
    apiLogs.value = apiLogs.value.slice(0, 20)
  }
}

// 清除日志
function clearLogs() {
  apiLogs.value = []
}

// 重置表单
function resetForm() {
  apiTestForm.service = 'hdfs'
  apiTestForm.endpoint = '/status'
  apiTestResult.value = null
}

// 测试API请求
async function testApi() {
  try {
    // 构建API路径
    const apiPath = buildApiPath(apiTestForm.service, apiTestForm.endpoint)
    addLog(`发起请求: ${apiPath}`, 'info')
    
    // 发送请求
    const response = await axios.get(apiPath)
    
    // 设置结果
    apiTestResult.value = {
      success: true,
      message: `请求成功，状态码: ${response.status}`,
      data: response.data
    }
    
    addLog(`请求成功: ${apiPath}`, 'success')
    addLog(`响应数据: ${JSON.stringify(response.data)}`, 'success')
  } catch (error) {
    // 设置错误结果
    apiTestResult.value = {
      success: false,
      message: `请求失败: ${error.message || '未知错误'}`,
      data: error.response?.data
    }
    
    addLog(`请求失败: ${error.message}`, 'error')
    if (error.response) {
      addLog(`错误响应: ${JSON.stringify(error.response.data)}`, 'error')
    }
  }
}
</script>

<style scoped>
.multi-api-demo {
  padding: 20px;
}

.api-card {
  margin-bottom: 20px;
}

.config-info {
  font-size: 14px;
}

.config-info pre {
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
}

.api-test-section {
  margin-bottom: 20px;
}

.api-result {
  margin-top: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.result-data {
  margin-top: 15px;
}

.result-data pre {
  background-color: #fff;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}

.log-container {
  margin-top: 10px;
  max-height: 300px;
  overflow-y: auto;
  background-color: #1e1e1e;
  color: #d4d4d4;
  padding: 10px;
  border-radius: 4px;
  font-family: monospace;
}

.log-info {
  color: #d4d4d4;
}

.log-success {
  color: #4caf50;
}

.log-error {
  color: #f44336;
}
</style> 