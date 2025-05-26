<template>
  <div class="realtime-visualization">
    <h1>大数据实时可视化平台</h1>
    
    <!-- 连接状态和控制面板 -->
    <el-card class="control-panel">
      <div class="row">
        <div class="col">
          <h4>
            WebSocket连接状态: 
            <el-tag :type="isConnected ? 'success' : 'danger'" size="small">
              {{ isConnected ? '已连接' : '未连接' }}
            </el-tag>
          </h4>
          <div class="mt-3">
            <el-button type="primary" @click="connectWebSocket" :disabled="isConnected">连接WebSocket</el-button>
            <el-button type="danger" @click="disconnectWebSocket" :disabled="!isConnected">断开连接</el-button>
          </div>
        </div>
        <div class="col">
          <h4>数据加载与处理</h4>
          <div class="controls">
            <el-form :inline="true">
              <el-form-item label="数据类型">
                <el-select v-model="dataType" placeholder="选择数据类型">
                  <el-option label="天气数据" value="weather"></el-option>
                  <el-option label="产品数据" value="product"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="Kafka主题">
                <el-input v-model="topicName" placeholder="主题名称"></el-input>
              </el-form-item>
            </el-form>
            <div class="buttons">
              <el-button type="success" @click="loadData">加载数据</el-button>
              <el-button type="warning" @click="startStreaming">启动流处理</el-button>
              <el-button type="danger" @click="stopStreaming">停止流处理</el-button>
            </div>
          </div>
        </div>
      </div>
    </el-card>
    
    <!-- 天气数据可视化 -->
    <div v-if="dataType === 'weather'" class="visualization-section">
      <div class="charts-container">
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>各城市平均温度 (°C)</span>
            </div>
          </template>
          <div id="temp-chart" class="chart"></div>
        </el-card>
        <el-card class="chart-card">
          <template #header>
            <div class="card-header">
              <span>各城市总降雨量 (mm)</span>
            </div>
          </template>
          <div id="rain-chart" class="chart"></div>
        </el-card>
      </div>
      
      <el-card class="data-table-card">
        <template #header>
          <div class="card-header">
            <span>天气数据表格</span>
            <el-tag type="info">{{ weatherData.length }}条记录</el-tag>
          </div>
        </template>
        <el-table :data="weatherData" stripe style="width: 100%" height="300">
          <el-table-column prop="city" label="城市" width="120"></el-table-column>
          <el-table-column prop="state" label="所在州" width="150"></el-table-column>
          <el-table-column prop="avgMaxTemp" label="平均最高温度" width="150">
            <template #default="scope">
              {{ parseFloat(scope.row.avgMaxTemp).toFixed(1) }} °C
            </template>
          </el-table-column>
          <el-table-column prop="avgMinTemp" label="平均最低温度" width="150">
            <template #default="scope">
              {{ parseFloat(scope.row.avgMinTemp).toFixed(1) }} °C
            </template>
          </el-table-column>
          <el-table-column prop="totalRainfall" label="总降雨量">
            <template #default="scope">
              {{ parseFloat(scope.row.totalRainfall).toFixed(1) }} mm
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
    
    <!-- 产品数据可视化 -->
    <div v-if="dataType === 'product'" class="visualization-section">
      <el-card class="data-table-card">
        <template #header>
          <div class="card-header">
            <span>产品数据表格</span>
            <el-tag type="info">{{ productData.length }}条记录</el-tag>
          </div>
        </template>
        <el-table :data="productData" stripe style="width: 100%" height="300">
          <el-table-column v-for="(column, index) in productColumns" 
            :key="index" 
            :prop="column" 
            :label="column" 
            :width="column === 'id' ? 100 : ''">
          </el-table-column>
        </el-table>
      </el-card>
    </div>
    
    <!-- 消息通知 -->
    <el-dialog
      v-model="notificationVisible"
      title="操作结果"
      width="30%">
      <span>{{ notificationMessage }}</span>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="notificationVisible = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import apiConfig from '../config/api.config'
// 直接导入SockJS和Stomp库
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

export default {
  name: 'RealtimeVisualization',
  setup() {
    // 数据和状态
    const isConnected = ref(false)
    const dataType = ref('weather')
    const topicName = ref('weather-data')
    const weatherData = ref([])
    const productData = ref([])
    const productColumns = ref([])
    const notificationVisible = ref(false)
    const notificationMessage = ref('')
    
    // WebSocket客户端
    let stompClient = null
    // 图表实例
    let tempChart = null
    let rainChart = null
    
    // 监听数据类型变化
    watch(dataType, (newValue) => {
      if (newValue === 'weather') {
        topicName.value = 'weather-data'
      } else {
        topicName.value = 'product-data'
      }
    })
    
    // 初始化图表
    const initCharts = () => {
      // 温度图表
      tempChart = echarts.init(document.getElementById('temp-chart'))
      tempChart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['最高温度', '最低温度']
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: [],
          axisLabel: {
            interval: 0,
            rotate: 30
          }
        },
        yAxis: {
          type: 'value',
          name: '温度 (°C)'
        },
        series: [
          {
            name: '最高温度',
            type: 'bar',
            data: [],
            itemStyle: {
              color: '#FF6B6B'
            }
          },
          {
            name: '最低温度',
            type: 'bar',
            data: [],
            itemStyle: {
              color: '#4ECDC4'
            }
          }
        ]
      })
      
      // 降雨量图表
      rainChart = echarts.init(document.getElementById('rain-chart'))
      rainChart.setOption({
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: [],
          axisLabel: {
            interval: 0,
            rotate: 30
          }
        },
        yAxis: {
          type: 'value',
          name: '降雨量 (mm)'
        },
        series: [
          {
            type: 'bar',
            data: [],
            itemStyle: {
              color: '#5D9CEC'
            }
          }
        ]
      })
      
      // 监听窗口大小变化，调整图表大小
      window.addEventListener('resize', () => {
        tempChart && tempChart.resize()
        rainChart && rainChart.resize()
      })
    }
    
    // 更新温度图表
    const updateTempChart = (data) => {
      if (!tempChart) return
      
      const cities = data.map(item => item.city)
      const maxTemps = data.map(item => parseFloat(item.avgMaxTemp).toFixed(1))
      const minTemps = data.map(item => parseFloat(item.avgMinTemp).toFixed(1))
      
      tempChart.setOption({
        xAxis: { data: cities },
        series: [
          { name: '最高温度', data: maxTemps },
          { name: '最低温度', data: minTemps }
        ]
      })
    }
    
    // 更新降雨量图表
    const updateRainChart = (data) => {
      if (!rainChart) return
      
      const cities = data.map(item => item.city)
      const rainfall = data.map(item => parseFloat(item.totalRainfall).toFixed(1))
      
      rainChart.setOption({
        xAxis: { data: cities },
        series: [{ data: rainfall }]
      })
    }
    
    // 连接WebSocket
    const connectWebSocket = () => {
      try {
        // 使用直接导入的SockJS
        const socket = new SockJS(`${apiConfig.baseUrl}/ws`)
        
        // 创建STOMP客户端
        stompClient = new Client({
          webSocketFactory: () => socket,
          onConnect: frame => {
            isConnected.value = true
            console.log('已连接到WebSocket: ' + frame)
            
            // 订阅天气数据主题
            stompClient.subscribe('/topic/weather-stats', message => {
              try {
                const weatherStatsData = JSON.parse(message.body)
                weatherData.value = []
                
                // 解析每一条JSON字符串
                weatherStatsData.forEach(jsonStr => {
                  try {
                    const data = JSON.parse(jsonStr)
                    weatherData.value.push(data)
                  } catch (error) {
                    console.error('解析天气数据失败:', error)
                  }
                })
                
                // 更新图表
                updateTempChart(weatherData.value)
                updateRainChart(weatherData.value)
              } catch (error) {
                console.error('处理消息失败:', error)
              }
            })
            
            // 订阅产品数据主题
            stompClient.subscribe('/topic/product-stats', message => {
              try {
                const productStatsData = JSON.parse(message.body)
                productData.value = []
                
                // 解析每一条JSON字符串
                productStatsData.forEach(jsonStr => {
                  try {
                    const data = JSON.parse(jsonStr)
                    productData.value.push(data)
                    
                    // 提取列名
                    if (productData.value.length === 1) {
                      productColumns.value = Object.keys(data)
                    }
                  } catch (error) {
                    console.error('解析产品数据失败:', error)
                  }
                })
              } catch (error) {
                console.error('处理消息失败:', error)
              }
            })
          },
          onStompError: error => {
            console.error('STOMP错误:', error)
            isConnected.value = false
            showNotification('STOMP连接错误: ' + error.headers.message)
          }
        })
        
        // 启动连接
        stompClient.activate()
      } catch (e) {
        console.error('创建WebSocket连接异常:', e)
        showNotification('创建WebSocket连接异常: ' + e.message)
      }
    }
    
    // 断开WebSocket连接
    const disconnectWebSocket = () => {
      if (stompClient) {
        stompClient.deactivate()
        stompClient = null
        isConnected.value = false
        console.log('已断开WebSocket连接')
      }
    }
    
    // 加载数据到Kafka
    const loadData = async () => {
      try {
        let endpoint = ''
        let filePath = ''
        
        if (dataType.value === 'weather') {
          endpoint = '/api/realtime/load/weather'
          filePath = 'data/temprainfall.csv'
        } else {
          endpoint = '/api/realtime/load/products'
          filePath = 'data/product_regressiondb.csv'
        }
        
        const response = await axios.post(
          endpoint, 
          null, 
          { params: { filePath, topicName: topicName.value } }
        )
        
        console.log('加载数据响应:', response.data)
        showNotification(`成功加载 ${response.data.recordCount} 条数据到Kafka主题: ${topicName.value}`)
      } catch (error) {
        console.error('加载数据失败:', error)
        showNotification('加载数据失败: ' + (error.response?.data?.message || error.message))
      }
    }
    
    // 启动流处理
    const startStreaming = async () => {
      try {
        let endpoint = ''
        
        if (dataType.value === 'weather') {
          endpoint = '/api/realtime/start/weather'
        } else {
          endpoint = '/api/realtime/start/products'
        }
        
        const response = await axios.post(
          endpoint, 
          null, 
          { params: { topicName: topicName.value } }
        )
        
        console.log('启动流处理响应:', response.data)
        showNotification(`${response.data.message}: ${topicName.value}`)
      } catch (error) {
        console.error('启动流处理失败:', error)
        showNotification('启动流处理失败: ' + (error.response?.data?.message || error.message))
      }
    }
    
    // 停止流处理
    const stopStreaming = async () => {
      try {
        const response = await axios.post('/api/realtime/stop')
        console.log('停止流处理响应:', response.data)
        showNotification(response.data.message)
      } catch (error) {
        console.error('停止流处理失败:', error)
        showNotification('停止流处理失败: ' + (error.response?.data?.message || error.message))
      }
    }
    
    // 显示通知消息
    const showNotification = (message) => {
      notificationMessage.value = message
      notificationVisible.value = true
    }
    
    // 生命周期钩子
    onMounted(() => {
      // 初始化图表
      setTimeout(() => {
        initCharts()
      }, 100)
    })
    
    onUnmounted(() => {
      // 组件卸载时断开连接
      disconnectWebSocket()
      
      // 销毁图表实例
      tempChart && tempChart.dispose()
      rainChart && rainChart.dispose()
    })
    
    return {
      isConnected,
      dataType,
      topicName,
      weatherData,
      productData,
      productColumns,
      notificationVisible,
      notificationMessage,
      connectWebSocket,
      disconnectWebSocket,
      loadData,
      startStreaming,
      stopStreaming
    }
  }
}
</script>

<style scoped>
.realtime-visualization {
  padding: 20px;
}

.control-panel {
  margin-bottom: 20px;
}

.row {
  display: flex;
  flex-wrap: wrap;
}

.col {
  flex: 1;
  min-width: 300px;
  padding: 10px;
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.buttons {
  display: flex;
  gap: 10px;
}

.chart {
  height: 400px;
  width: 100%;
}

.charts-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(500px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.visualization-section {
  margin-top: 20px;
}

.chart-card, .data-table-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style> 