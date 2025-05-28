<template>
  <div class="agriculture-monitor">
    <h2>农业传感器实时监控</h2>
    
    <!-- 控制面板 -->
    <ControlPanel 
      :is-connected="isConnected"
      :available-topics="availableTopics"
      v-model:subscribedTopics="subscribedTopics"
      @connect="connectWebSocket"
      @disconnect="disconnectWebSocket"
      @send-test="sendTestData"
    />
    
    <!-- 传感器数据卡片 -->
    <div class="sensor-dashboard">
      <SensorCard 
        v-for="(sensor, id) in sensorData" 
        :key="id" 
        :id="id" 
        :sensor="sensor"
      />
    </div>
    
    <!-- 统计数据面板 -->
    <StatsPanel 
      v-if="Object.keys(sparkStats).length > 0" 
      :stats="sparkStats" 
    />
    
    <!-- 消息列表 -->
    <MessageLog 
      :messages="messages" 
      @clear="clearMessages" 
    />
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, watch } from 'vue'
import ControlPanel from './agriculture/ControlPanel.vue'
import SensorCard from './agriculture/SensorCard.vue'
import StatsPanel from './agriculture/StatsPanel.vue'
import MessageLog from './agriculture/MessageLog.vue'
import websocketManager from '../utils/websocket'
import sensorApi from '../api/sensor'
import { calculateTrend } from '../utils/sensorUtils'

export default {
  name: 'AgricultureSensorMonitor',
  components: {
    ControlPanel,
    SensorCard,
    StatsPanel,
    MessageLog
  },
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
    
    // 连接WebSocket
    const connectWebSocket = async () => {
      try {
        // 连接WebSocket
        await websocketManager.connect()
        
        // 更新连接状态
        isConnected.value = websocketManager.isConnected()
        
        // 添加连接成功消息
        addMessage('system', '已连接到WebSocket服务器!')
        
        // 订阅所有选中的主题
        subscribedTopics.value.forEach(topic => {
          subscribeToTopic(topic)
        })
      } catch (e) {
        console.error('WebSocket连接失败:', e)
        addMessage('error', `连接失败: ${e.message || '未知错误'}`)
      }
    }
    
    // 断开WebSocket连接
    const disconnectWebSocket = () => {
      websocketManager.disconnect()
      isConnected.value = false
      addMessage('system', '已断开WebSocket连接')
    }
    
    // 订阅主题
    const subscribeToTopic = async (topic) => {
      try {
        await websocketManager.subscribe(topic, (data) => {
          handleMessage(topic, data)
        })
        addMessage('system', `已订阅主题: ${topic}`)
      } catch (e) {
        addMessage('error', `订阅失败: ${e.message || '未知错误'}`)
      }
    }
    
    // 处理收到的消息
    const handleMessage = (topic, data) => {
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
    }
    
    // 更新Spark统计数据
    const updateSparkStats = (data) => {
      // 更新所有传感器类型的统计数据
      Object.keys(data).forEach(sensorType => {
        sparkStats[sensorType] = data[sensorType]
      })
    }
    
    // 添加消息到日志
    const addMessage = (type, content) => {
      const time = new Date().toLocaleTimeString()
      messages.value.push({ type, content, time })
      
      // 保持最多50条消息
      if (messages.value.length > 50) {
        messages.value.shift()
      }
    }
    
    // 清空消息列表
    const clearMessages = () => {
      messages.value = []
    }
    
    // 发送测试数据
    const sendTestData = async () => {
      try {
        const response = await sensorApi.sendTestData()
        addMessage('system', `测试数据已发送: ${response.data.message || 'success'}`)
      } catch (error) {
        console.error('发送测试数据失败:', error)
        addMessage('error', `发送测试数据失败: ${error.message}`)
      }
    }
    
    // 监听WebSocket事件
    websocketManager.on('connect', () => {
      isConnected.value = true
    })
    
    websocketManager.on('disconnect', () => {
      isConnected.value = false
    })
    
    websocketManager.on('error', (error) => {
      addMessage('error', `WebSocket错误: ${error.message}`)
    })
    
    // 监听订阅主题变化
    watch(subscribedTopics, (newTopics, oldTopics) => {
      if (!isConnected.value) return
      
      // 取消订阅已移除的主题
      oldTopics.forEach(topic => {
        if (!newTopics.includes(topic)) {
          websocketManager.unsubscribe(topic)
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
      // 自动连接WebSocket
      connectWebSocket()
    })
    
    // 组件卸载
    onUnmounted(() => {
      // 断开WebSocket连接
      disconnectWebSocket()
    })
    
    return {
      isConnected,
      availableTopics,
      subscribedTopics,
      sensorData,
      sparkStats,
      messages,
      connectWebSocket,
      disconnectWebSocket,
      sendTestData,
      clearMessages
    }
  }
}
</script>

<style scoped>
.agriculture-monitor {
  padding: 20px;
}

.sensor-dashboard {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 20px;
}
</style> 