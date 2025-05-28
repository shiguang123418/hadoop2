<template>
  <div class="agriculture-monitor">
    <h2>农业传感器实时监控</h2>
    
    <!-- 连接状态和控制面板 -->
    <el-card class="control-panel">
      <div class="row">
        <div class="connection-status">
          <h4>
            WebSocket连接状态: 
            <el-tag :type="isConnected ? 'success' : 'danger'" size="small">
              {{ isConnected ? '已连接' : '未连接' }}
            </el-tag>
          </h4>
          <div class="mt-2">
            <el-button type="primary" @click="connectWebSocket" :disabled="isConnected">连接</el-button>
            <el-button type="danger" @click="disconnectWebSocket" :disabled="!isConnected">断开</el-button>
          </div>
        </div>
        
        <div class="topics">
          <h4>订阅主题</h4>
          <div class="topic-selection">
            <el-checkbox-group v-model="subscribedTopics">
              <el-checkbox v-for="topic in availableTopics" :key="topic.value" :label="topic.value">
                {{ topic.label }}
              </el-checkbox>
            </el-checkbox-group>
          </div>
        </div>
        
        <div class="actions">
          <el-button type="success" @click="sendTestData" :disabled="!isConnected">发送测试数据</el-button>
        </div>
      </div>
    </el-card>
    
    <!-- 传感器数据卡片 -->
    <div class="sensor-dashboard">
      <el-card v-for="(sensor, id) in sensorData" :key="id" class="sensor-card" 
               :class="{ 'anomaly': sensor.isAnomaly }">
        <template #header>
          <div class="sensor-header">
            <span>{{ getSensorTypeDisplay(sensor.type) }} {{ id }}</span>
            <el-tag :type="sensor.isAnomaly ? 'danger' : 'success'" size="small">
              {{ sensor.isAnomaly ? '异常' : '正常' }}
            </el-tag>
          </div>
        </template>
        
        <div class="sensor-value">
          {{ sensor.lastValue }} <span class="unit">{{ sensor.lastUnit }}</span>
          <span :class="getTrendClass(sensor.trend)">{{ getTrendIcon(sensor.trend) }}</span>
        </div>
        
        <div class="sensor-info">
          <div><strong>位置:</strong> {{ sensor.location }}</div>
          <div><strong>更新时间:</strong> {{ sensor.lastTime }}</div>
          <div v-if="sensor.movingAverage !== undefined">
            <strong>移动平均值:</strong> {{ Number(sensor.movingAverage).toFixed(2) }}
          </div>
        </div>
        
        <!-- 图表 -->
        <div class="chart-container" :id="`chart-${id}`"></div>
      </el-card>
    </div>
    
    <!-- 统计数据面板 -->
    <el-card v-if="Object.keys(sparkStats).length > 0" class="stats-panel">
      <template #header>
        <div class="stats-header">
          <span>Spark统计数据</span>
        </div>
      </template>
      
      <div class="stats-content">
        <el-card v-for="(stats, type) in sparkStats" :key="type" class="stat-item">
          <template #header>
            <span>{{ getSensorTypeDisplay(type) }}</span>
          </template>
          <div class="stats-details">
            <div><strong>最小值:</strong> {{ stats.min.toFixed(2) }}</div>
            <div><strong>最大值:</strong> {{ stats.max.toFixed(2) }}</div>
            <div><strong>平均值:</strong> {{ stats.avg.toFixed(2) }}</div>
            <div><strong>标准差:</strong> {{ stats.stdDev.toFixed(2) }}</div>
            <div><strong>样本数:</strong> {{ stats.count }}</div>
            <div><strong>异常数:</strong> {{ stats.anomalyCount }}</div>
            <div><strong>异常率:</strong> {{ (stats.anomalyRate * 100).toFixed(2) }}%</div>
          </div>
        </el-card>
      </div>
    </el-card>
    
    <!-- 消息列表 -->
    <el-card class="message-panel">
      <template #header>
        <div class="message-header">
          <span>消息日志</span>
          <el-button type="primary" size="small" @click="clearMessages">清空</el-button>
        </div>
      </template>
      
      <div class="message-list" ref="messageContainer">
        <div v-for="(message, index) in messages" :key="index" 
             :class="['message', message.type]">
          <strong>{{ message.time }}</strong>: {{ message.content }}
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

export default {
  name: 'AgricultureSensorMonitor',
  
  setup() {
    // 连接状态
    const isConnected = ref(false)
    
    // 可用的主题
    const availableTopics = [
      { label: '传感器数据', value: '/topic/agriculture-sensor-data' },
      { label: 'Spark统计数据', value: '/topic/spark-stats' },
      { label: '系统通知', value: '/topic/system-notifications' }
    ]
    
    // 已订阅的主题
    const subscribedTopics = ref([
      '/topic/agriculture-sensor-data',
      '/topic/spark-stats'
    ])
    
    // 传感器数据
    const sensorData = reactive({})
    
    // Spark统计数据
    const sparkStats = reactive({})
    
    // 消息列表
    const messages = ref([])
    const messageContainer = ref(null)
    
    // 图表实例
    const charts = {}
    
    // WebSocket客户端
    let stompClient = null
    let subscriptions = {}
    
    // 连接WebSocket
    const connectWebSocket = () => {
      try {
        // 根据环境确定WebSocket连接地址
        let wsUrl;
        if (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1') {
          // 本地开发环境，使用代理
          wsUrl = '/api_ws';
        } else if (window.location.hostname === '192.168.1.192') {
          // 开发服务器环境，直接连接
          wsUrl = 'http://192.168.1.192:8001/api/ws';
        } else {
          // 其他环境，基于当前域名构建WebSocket地址
          const protocol = window.location.protocol === 'https:' ? 'https:' : 'http:';
          const host = window.location.hostname;
          wsUrl = `${protocol}//${host}:8001/api/ws`;
        }
        
        console.log('使用WebSocket连接地址:', wsUrl);
        const socket = new SockJS(wsUrl);
        
        stompClient = new Client({
          webSocketFactory: () => socket,
          debug: function(str) {
            console.log('STOMP: ' + str)
          },
          onConnect: frame => {
            isConnected.value = true
            addMessage('system', `已连接到WebSocket服务器!`)
            console.log('连接成功: ' + frame)
            
            // 订阅所有选中的主题
            subscribedTopics.value.forEach(topic => {
              subscribeToTopic(topic)
            })
          },
          onStompError: error => {
            console.error('STOMP错误:', error)
            isConnected.value = false
            addMessage('error', `STOMP连接错误: ${error.headers.message}`)
          }
        })
        
        stompClient.activate()
      } catch (e) {
        console.error('创建WebSocket连接异常:', e)
        addMessage('error', `连接失败: ${e.message}`)
      }
    }
    
    // 断开WebSocket连接
    const disconnectWebSocket = () => {
      if (stompClient) {
        // 取消所有订阅
        Object.values(subscriptions).forEach(subscription => {
          subscription.unsubscribe()
        })
        subscriptions = {}
        
        stompClient.deactivate()
        stompClient = null
        isConnected.value = false
        addMessage('system', '已断开WebSocket连接')
      }
    }
    
    // 订阅主题
    const subscribeToTopic = (topic) => {
      if (!stompClient || !isConnected.value) {
        addMessage('error', '请先连接WebSocket')
        return
      }
      
      // 如果已订阅，则先取消订阅
      if (subscriptions[topic]) {
        subscriptions[topic].unsubscribe()
      }
      
      // 订阅新主题
      subscriptions[topic] = stompClient.subscribe(topic, message => {
        handleMessage(topic, message.body)
      })
      
      addMessage('system', `已订阅主题: ${topic}`)
    }
    
    // 处理收到的消息
    const handleMessage = (topic, messageBody) => {
      try {
        const data = JSON.parse(messageBody)
        
        if (topic === '/topic/agriculture-sensor-data') {
          // 处理传感器数据
          updateSensorData(data)
          addMessage('received', `传感器数据: ${data.sensorId} (${data.sensorType}) = ${data.value}${data.unit}`)
        } else if (topic === '/topic/spark-stats') {
          // 处理Spark统计数据
          updateSparkStats(data)
          addMessage('received', `Spark统计数据已更新`)
        } else if (topic === '/topic/system-notifications') {
          // 处理系统通知
          addMessage('system', `系统通知: ${JSON.stringify(data)}`)
        }
      } catch (e) {
        console.error('处理消息出错:', e)
        addMessage('error', `消息解析错误: ${e.message}`)
      }
    }
    
    // 更新传感器数据
    const updateSensorData = (data) => {
      const sensorId = data.sensorId
      const sensorType = data.sensorType
      
      // 如果是新传感器，初始化数据结构
      if (!sensorData[sensorId]) {
        sensorData[sensorId] = {
          type: sensorType,
          values: [],
          timestamps: [],
          location: data.location
        }
      }
      
      // 保持最多20个数据点
      if (sensorData[sensorId].values.length >= 20) {
        sensorData[sensorId].values.shift()
        sensorData[sensorId].timestamps.shift()
      }
      
      // 更新数据
      sensorData[sensorId].values.push(data.value)
      sensorData[sensorId].timestamps.push(data.readableTime)
      sensorData[sensorId].lastValue = data.value
      sensorData[sensorId].lastUnit = data.unit
      sensorData[sensorId].lastTime = data.readableTime
      sensorData[sensorId].isAnomaly = data.isAnomaly
      sensorData[sensorId].movingAverage = data.movingAverage
      sensorData[sensorId].trend = data.stats?.trend || calculateTrend(sensorData[sensorId].values)
      sensorData[sensorId].location = data.location
      
      // 更新图表
      updateSensorChart(sensorId)
    }
    
    // 更新Spark统计数据
    const updateSparkStats = (data) => {
      // 更新所有传感器类型的统计数据
      Object.keys(data).forEach(sensorType => {
        sparkStats[sensorType] = data[sensorType]
      })
    }
    
    // 初始化传感器图表
    const initSensorChart = (sensorId) => {
      const chartDom = document.getElementById(`chart-${sensorId}`)
      if (!chartDom) return
      
      const chart = echarts.init(chartDom)
      const sensor = sensorData[sensorId]
      
      chart.setOption({
        tooltip: {
          trigger: 'axis'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: sensor.timestamps,
          axisLabel: {
            show: false
          }
        },
        yAxis: {
          type: 'value',
          scale: true
        },
        series: [
          {
            name: getSensorTypeDisplay(sensor.type),
            type: 'line',
            data: sensor.values,
            smooth: true,
            lineStyle: {
              width: 3
            },
            itemStyle: {
              color: getChartColor(sensor.type)
            },
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [{
                  offset: 0, color: getChartColor(sensor.type, 0.6)
                }, {
                  offset: 1, color: getChartColor(sensor.type, 0.1)
                }]
              }
            }
          }
        ]
      })
      
      charts[sensorId] = chart
    }
    
    // 更新传感器图表
    const updateSensorChart = (sensorId) => {
      if (!charts[sensorId]) {
        // 延迟初始化图表，确保DOM已经渲染
        setTimeout(() => {
          initSensorChart(sensorId)
        }, 100)
        return
      }
      
      const sensor = sensorData[sensorId]
      charts[sensorId].setOption({
        xAxis: {
          data: sensor.timestamps
        },
        series: [
          {
            data: sensor.values
          }
        ]
      })
    }
    
    // 计算趋势
    const calculateTrend = (values) => {
      if (values.length < 3) return 'stable'
      
      const last = values[values.length - 1]
      const secondLast = values[values.length - 2]
      const thirdLast = values[values.length - 3]
      
      if (last > secondLast && secondLast > thirdLast) {
        return 'rising'
      } else if (last < secondLast && secondLast < thirdLast) {
        return 'falling'
      } else {
        return 'stable'
      }
    }
    
    // 获取传感器类型显示名
    const getSensorTypeDisplay = (type) => {
      switch (type) {
        case 'temperature': return '温度'
        case 'humidity': return '湿度'
        case 'soilMoisture': return '土壤湿度'
        case 'light': return '光照强度'
        case 'co2': return 'CO₂浓度'
        default: return type
      }
    }
    
    // 获取趋势图标
    const getTrendIcon = (trend) => {
      switch (trend) {
        case 'rising': return '↑'
        case 'falling': return '↓'
        case 'stable': return '→'
        default: return ''
      }
    }
    
    // 获取趋势样式类
    const getTrendClass = (trend) => {
      switch (trend) {
        case 'rising': return 'trend-up'
        case 'falling': return 'trend-down'
        case 'stable': return 'trend-stable'
        default: return ''
      }
    }
    
    // 获取图表颜色
    const getChartColor = (type, alpha = 1) => {
      let color
      switch (type) {
        case 'temperature': color = 'rgb(255, 99, 71)'; break; // 红色
        case 'humidity': color = 'rgb(0, 123, 255)'; break; // 蓝色
        case 'soilMoisture': color = 'rgb(111, 66, 193)'; break; // 紫色
        case 'light': color = 'rgb(255, 193, 7)'; break; // 黄色
        case 'co2': color = 'rgb(32, 201, 151)'; break; // 青绿色
        default: color = 'rgb(108, 117, 125)'; // 灰色
      }
      
      if (alpha < 1) {
        return color.replace('rgb', 'rgba').replace(')', `, ${alpha})`)
      }
      
      return color
    }
    
    // 添加消息到日志
    const addMessage = (type, content) => {
      const time = new Date().toLocaleTimeString()
      messages.value.push({ type, content, time })
      
      // 保持最多50条消息
      if (messages.value.length > 50) {
        messages.value.shift()
      }
      
      // 滚动到底部
      setTimeout(() => {
        if (messageContainer.value) {
          messageContainer.value.scrollTop = messageContainer.value.scrollHeight
        }
      }, 100)
    }
    
    // 清空消息列表
    const clearMessages = () => {
      messages.value = []
    }
    
    // 发送测试数据
    const sendTestData = async () => {
      try {
        const response = await axios.get('/api/sensor/direct-test')
        addMessage('system', `测试数据已发送: ${response.data.message || 'success'}`)
      } catch (error) {
        console.error('发送测试数据失败:', error)
        addMessage('error', `发送测试数据失败: ${error.message}`)
      }
    }
    
    // 监听订阅主题变化
    watch(subscribedTopics, (newTopics, oldTopics) => {
      if (!isConnected.value) return
      
      // 取消订阅已移除的主题
      oldTopics.forEach(topic => {
        if (!newTopics.includes(topic) && subscriptions[topic]) {
          subscriptions[topic].unsubscribe()
          delete subscriptions[topic]
          addMessage('system', `已取消订阅: ${topic}`)
        }
      })
      
      // 订阅新增的主题
      newTopics.forEach(topic => {
        if (!oldTopics.includes(topic)) {
          subscribeToTopic(topic)
        }
      })
    })
    
    // 组件挂载
    onMounted(() => {
      // 窗口大小变化时调整图表大小
      window.addEventListener('resize', () => {
        Object.values(charts).forEach(chart => {
          chart && chart.resize()
        })
      })
      
      // 自动连接WebSocket
      connectWebSocket()
    })
    
    // 组件卸载
    onUnmounted(() => {
      // 断开WebSocket连接
      disconnectWebSocket()
      
      // 销毁图表实例
      Object.values(charts).forEach(chart => {
        chart && chart.dispose()
      })
      
      // 移除事件监听
      window.removeEventListener('resize', () => {})
    })
    
    return {
      isConnected,
      availableTopics,
      subscribedTopics,
      sensorData,
      sparkStats,
      messages,
      messageContainer,
      connectWebSocket,
      disconnectWebSocket,
      sendTestData,
      clearMessages,
      getSensorTypeDisplay,
      getTrendIcon,
      getTrendClass
    }
  }
}
</script>

<style scoped>
.agriculture-monitor {
  padding: 20px;
}

.control-panel {
  margin-bottom: 20px;
}

.row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.sensor-dashboard {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}

.sensor-card {
  position: relative;
  transition: all 0.3s ease;
}

.sensor-card.anomaly {
  border-left: 5px solid #f56c6c;
}

.sensor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sensor-value {
  font-size: 28px;
  font-weight: bold;
  margin: 10px 0;
}

.unit {
  font-size: 16px;
  color: #909399;
  margin-left: 5px;
}

.sensor-info {
  margin-bottom: 15px;
  font-size: 14px;
  color: #606266;
}

.chart-container {
  height: 150px;
  margin-top: 10px;
}

.trend-up {
  color: #67c23a;
  margin-left: 10px;
}

.trend-down {
  color: #f56c6c;
  margin-left: 10px;
}

.trend-stable {
  color: #409eff;
  margin-left: 10px;
}

.stats-panel {
  margin-bottom: 20px;
}

.stats-content {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
}

.stats-header, .message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-panel {
  margin-bottom: 20px;
}

.message-list {
  max-height: 300px;
  overflow-y: auto;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.message {
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 4px;
  font-size: 14px;
}

.message.system {
  background-color: #e1f3d8;
  border-left: 3px solid #67c23a;
}

.message.error {
  background-color: #fef0f0;
  border-left: 3px solid #f56c6c;
}

.message.received {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}

.topic-selection {
  margin-top: 10px;
}
</style> 