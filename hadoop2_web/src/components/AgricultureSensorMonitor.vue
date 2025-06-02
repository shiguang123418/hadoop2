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
        // 使用正确的单位显示
        const unit = getCorrectUnit(data.sensorType)
        const typeDisplay = getSensorTypeDisplay(data.sensorType)
        addMessage('received', `${typeDisplay}传感器(${data.sensorId})：${data.value}${unit} - ${data.location}`)
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
      try {
        // 验证数据合法性
        if (!data || !data.sensorId || !data.sensorType) {
          console.warn('收到无效传感器数据:', data)
          return
        }
        
      const sensorId = data.sensorId
      const sensorType = data.sensorType
        
        // 验证传感器类型与ID的一致性
        if (sensorData[sensorId] && sensorData[sensorId].type !== sensorType) {
          console.warn(`传感器${sensorId}类型变更: ${sensorData[sensorId].type} -> ${sensorType}，将创建新的数据记录`)
          // 如果传感器类型发生变化，我们删除旧记录（这种情况应该不会发生，因为后端已修复为固定类型）
          delete sensorData[sensorId]
        }
        
        // 验证传感器值的合理性
        const value = validateSensorValue(data.value, sensorType)
        const movingAverage = validateSensorValue(data.movingAverage, sensorType)
      
      // 如果是新传感器，初始化数据结构
      if (!sensorData[sensorId]) {
        sensorData[sensorId] = {
          type: sensorType,
          values: [],
          timestamps: [],
            location: data.location,
            // 添加类型说明，便于UI显示
            typeDisplay: getSensorTypeDisplay(sensorType),
            unit: getCorrectUnit(sensorType)
        }
      }
      
      // 保持最多20个数据点
      if (sensorData[sensorId].values.length >= 20) {
        sensorData[sensorId].values.shift()
        sensorData[sensorId].timestamps.shift()
      }
      
        // 更新数据 - 使用验证后的值
        sensorData[sensorId].values.push(value)
      sensorData[sensorId].timestamps.push(data.readableTime)
        sensorData[sensorId].lastValue = value
        sensorData[sensorId].lastUnit = getCorrectUnit(sensorType) // 使用前端确定的单位
      sensorData[sensorId].lastTime = data.readableTime
      sensorData[sensorId].isAnomaly = data.isAnomaly
        sensorData[sensorId].movingAverage = movingAverage
      sensorData[sensorId].trend = data.stats?.trend || calculateTrend(sensorData[sensorId].values)
      sensorData[sensorId].location = data.location
      } catch (error) {
        console.error('处理传感器数据时出错:', error)
        addMessage('error', `处理传感器数据时出错: ${error.message}`)
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
    
    // 获取正确的传感器单位
    const getCorrectUnit = (type) => {
      switch (type) {
        case 'temperature': return '°C'
        case 'humidity': return '%'
        case 'soilMoisture': return '%'
        case 'light': return 'lux'
        case 'co2': return 'ppm'
        default: return ''
      }
    }
    
    // 验证传感器值的合理性
    const validateSensorValue = (value, type) => {
      if (value === undefined || value === null || isNaN(Number(value))) {
        // 返回一个合理的默认值
        switch (type) {
          case 'temperature': return 25
          case 'humidity': return 60
          case 'soilMoisture': return 50
          case 'light': return 5000
          case 'co2': return 400
          default: return 0
        }
      }
      
      // 将值转换为数字
      let numValue = Number(value)
      
      // 定义各类型传感器的合理值范围
      const ranges = {
        temperature: { min: -20, max: 50 },
        humidity: { min: 0, max: 100 },
        soilMoisture: { min: 0, max: 100 },
        light: { min: 0, max: 100000 },
        co2: { min: 300, max: 5000 }
      }
      
      if (!ranges[type]) return numValue
      
      // 限制值在合理范围内
      if (numValue < ranges[type].min) return ranges[type].min
      if (numValue > ranges[type].max) return ranges[type].max
      
      return numValue
    }
    
    // 更新Spark统计数据
    const updateSparkStats = (data) => {
      try {
        // 验证数据
        if (!data || typeof data !== 'object') {
          console.warn('收到无效的Spark统计数据:', data)
          return
        }
        
      // 更新所有传感器类型的统计数据
      Object.keys(data).forEach(sensorType => {
          // 验证数据有效性
          if (data[sensorType] && typeof data[sensorType] === 'object') {
            // 验证统计值
            const stats = { ...data[sensorType] }
            
            // 确保数值在合理范围内
            if (stats.min !== undefined) stats.min = validateSensorValue(stats.min, sensorType)
            if (stats.max !== undefined) stats.max = validateSensorValue(stats.max, sensorType)
            if (stats.avg !== undefined) stats.avg = validateSensorValue(stats.avg, sensorType)
            if (stats.count !== undefined) stats.count = Math.max(0, Number(stats.count) || 0)
            
            sparkStats[sensorType] = stats
          }
      })
      } catch (error) {
        console.error('处理Spark统计数据时出错:', error)
        addMessage('error', `处理Spark统计数据时出错: ${error.message}`)
      }
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
    
    // 组件挂载时
    onMounted(() => {
      console.log('AgricultureSensorMonitor 组件已挂载，自动连接WebSocket');
      // 自动连接WebSocket
      connectWebSocket();
    })
    
    // 组件卸载时
    onUnmounted(() => {
      console.log('AgricultureSensorMonitor 组件卸载，断开WebSocket连接');
      // 断开WebSocket连接
      disconnectWebSocket();
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