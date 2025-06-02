<template>
  <div class="agriculture-dashboard">
    <header class="dashboard-header">
      <h1 class="title">农业传感器大数据可视化监控平台</h1>
      <div class="connection-status">
        <span :class="['status-indicator', isConnected ? 'connected' : 'disconnected']"></span>
        {{ isConnected ? '已连接' : '未连接' }}
        <el-button v-if="!isConnected" type="primary" size="small" @click="connectWebSocket">连接</el-button>
        <el-button v-else type="danger" size="small" @click="disconnectWebSocket">断开</el-button>
      </div>
      <div class="time-display">{{ currentTime }}</div>
      <div class="back-button">
        <el-button type="info" size="small" @click="goBack">
          <i class="el-icon-back"></i> 返回
        </el-button>
      </div>
    </header>

    <div class="dashboard-content">
      <!-- 数据总览区 -->
      <div class="dashboard-summary">
        <div class="data-card temperature">
          <div class="card-title">温度数据</div>
          <div class="card-value">{{ (sensorDataCounts && sensorDataCounts.temperature) || 1520 }}</div>
          <div class="card-icon"><i class="el-icon-thermometer"></i></div>
        </div>
        <div class="data-card humidity">
          <div class="card-title">湿度数据</div>
          <div class="card-value">{{ (sensorDataCounts && sensorDataCounts.humidity) || 1480 }}</div>
          <div class="card-icon"><i class="el-icon-cloudy"></i></div>
        </div>
        <div class="data-card soil">
          <div class="card-title">土壤数据</div>
          <div class="card-value">{{ (sensorDataCounts && sensorDataCounts.soilMoisture) || 1850 }}</div>
          <div class="card-icon"><i class="el-icon-crop"></i></div>
        </div>
        <div class="data-card light">
          <div class="card-title">光照数据</div>
          <div class="card-value">{{ (sensorDataCounts && sensorDataCounts.light) || 1210 }}</div>
          <div class="card-icon"><i class="el-icon-sunrise"></i></div>
        </div>
        <div class="data-card co2">
          <div class="card-title">CO₂数据</div>
          <div class="card-value">{{ (sensorDataCounts && sensorDataCounts.co2) || 1380 }}</div>
          <div class="card-icon"><i class="el-icon-wind-power"></i></div>
        </div>
        <div class="data-card total">
          <div class="card-title">数据总量</div>
          <div class="card-value">{{ totalDataPoints || 7440 }}</div>
          <div class="card-trend">
            <span class="trend-up">↑</span> {{ dataIncreaseRate }}%
          </div>
        </div>
      </div>

      <!-- 上层图表区域 -->
      <div class="charts-row upper-row">
        <!-- 传感器分布图 -->
        <div class="chart-container distribution-chart">
          <h3>实时数据分布</h3>
          <div ref="sensorDistributionChart" class="chart"></div>
        </div>
        
        <!-- 地图 -->
        <div class="chart-container map-container">
          <h3>传感器地理分布</h3>
          <div ref="sensorMapChart" class="map-chart" style="width: 100%; height: 300px;"></div>
        </div>
        
        <!-- 异常监测图 -->
        <div class="chart-container anomaly-chart">
          <h3>异常数据监测</h3>
          <div ref="anomalyChart" class="chart"></div>
        </div>
      </div>

      <!-- 下层图表区域 -->
      <div class="charts-row bottom-row">
        <!-- 实时数据流 -->
        <div class="chart-container realtime-data">
          <h3>实时数据流</h3>
          <div class="data-stream">
            <div v-for="(message, index) in recentMessages" :key="index" class="data-item" :class="message.type">
              <span class="time">{{ message.time }}</span>
              <span class="content">{{ message.content }}</span>
            </div>
          </div>
        </div>
        
        <!-- 实时警报与预警系统 (新增) -->
        <div class="chart-container realtime-alerts">
          <h3>实时预警监控</h3>
          <div class="alerts-wrapper">
            <div class="alerts-header">
              <div class="alert-stats">
                <div class="alert-stat-item critical">
                  <span class="alert-count">{{ alertStats.critical }}</span>
                  <span class="alert-label">严重</span>
                </div>
                <div class="alert-stat-item warning">
                  <span class="alert-count">{{ alertStats.warning }}</span>
                  <span class="alert-label">警告</span>
                </div>
                <div class="alert-stat-item normal">
                  <span class="alert-count">{{ alertStats.normal }}</span>
                  <span class="alert-label">正常</span>
                </div>
              </div>
              <div class="refresh-button">
                <el-button type="primary" size="small" icon="el-icon-refresh" circle @click="refreshAlerts"></el-button>
              </div>
            </div>
            <div class="alerts-container">
              <div v-for="(alert, index) in activeAlerts" :key="index" 
                   class="alert-item" :class="alert.severity">
                <div class="alert-icon">
                  <i :class="getAlertIcon(alert.severity)"></i>
                </div>
                <div class="alert-content">
                  <div class="alert-title">{{ alert.title }}</div>
                  <div class="alert-message">{{ alert.message }}</div>
                  <div class="alert-meta">
                    <span class="alert-sensor">{{ alert.sensor }}</span>
                    <span class="alert-time">{{ alert.time }}</span>
                  </div>
                </div>
                <div class="alert-value" :class="alert.trend">
                  {{ alert.value }}{{ alert.unit }}
                  <i v-if="alert.trend === 'increasing'" class="el-icon-top"></i>
                  <i v-else-if="alert.trend === 'decreasing'" class="el-icon-bottom"></i>
                </div>
              </div>
              <div v-if="activeAlerts.length === 0" class="no-alerts">
                <i class="el-icon-success"></i>
                <span>所有传感器数据正常</span>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 数据分析图表 - 百分比数据 -->
        <div class="chart-container data-analysis-percent">
          <h3>传感器百分比数据</h3>
          <div ref="dataAnalysisPercentChart" class="chart"></div>
        </div>
        
        <!-- 数据分析图表 - 比较数据 -->
        <div class="chart-container data-analysis-comparison">
          <h3>传感器数值数据</h3>
          <div ref="dataAnalysisComparisonChart" class="chart"></div>
        </div>
        
        <!-- 传感器状态 -->
        <div class="chart-container status-chart">
          <h3>传感器状态分布</h3>
          <div ref="sensorStatusChart" class="chart"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted, computed, nextTick } from 'vue'
// 导入全部echarts
import * as echarts from 'echarts'
// 导入echarts中国地图数据 - 使用更完整的地图数据
import chinaGeoJson from '../assets/map/full/china.json'
import websocketManager from '../utils/websocket'
import { calculateTrend } from '../utils/sensorUtils'
import { SensorApi } from '../api/sensor'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

// 确保echarts正确导入
if (!echarts || typeof echarts.registerMap !== 'function') {
  console.error('Echarts 或 registerMap 方法未定义，检查导入是否正确');
}

// 防止地图数据错误
if (!chinaGeoJson || !chinaGeoJson.features || !Array.isArray(chinaGeoJson.features)) {
  console.error('中国地图数据格式不正确', chinaGeoJson);
}

export default {
  name: 'AgricultureDataDashboard',
  inheritAttrs: false,
  setup() {
    // 连接状态
    const isConnected = ref(false)
    
    // 当前时间
    const currentTime = ref(new Date().toLocaleString())
    const timeInterval = ref(null)
    
    // 传感器数据
    const sensorData = reactive({})
    
    // 消息列表
    const recentMessages = ref([])
    
    // 传感器类型计数
    const sensorCounts = reactive({
      temperature: 5,
      humidity: 4,
      soilMoisture: 6,
      light: 3,
      co2: 3
    })
    
    // 每种类型传感器的数据点总量
    const sensorDataCounts = reactive({
      temperature: 1520,
      humidity: 1480,
      soilMoisture: 1850,
      light: 1210,
      co2: 1380
    })
    
    // 总数据点
    const totalDataPoints = ref(8520)
    
    // 数据增长率
    const dataIncreaseRate = ref(12.5)
    
    // 图表引用
    const sensorDistributionChart = ref(null)
    const sensorMapChart = ref(null)
    const anomalyChart = ref(null)
    const sensorStatusChart = ref(null)
    const dataAnalysisPercentChart = ref(null)
    const dataAnalysisComparisonChart = ref(null)
    
    // 图表实例
    let distributionChartInstance = null
    let mapChartInstance = null
    let anomalyChartInstance = null
    let statusChartInstance = null
    let dataAnalysisPercentChartInstance = null
    let dataAnalysisComparisonChartInstance = null
    
    // 传感器数据统计信息
    const sensorStats = reactive({
      temperature: { min: 15, max: 35, avg: 25, median: 24 },
      humidity: { min: 40, max: 90, avg: 65, median: 63 },
      soilMoisture: { min: 20, max: 80, avg: 45, median: 44 },
      light: { min: 1000, max: 50000, avg: 20000, median: 18000 },
      co2: { min: 300, max: 1500, avg: 800, median: 750 }
    })
    
    // 新增：警报统计和列表
    const alertStats = reactive({
      critical: 2,
      warning: 3,
      normal: 16
    })
    
    const activeAlerts = ref([
      {
        id: 1,
        severity: 'critical',
        title: '温度过高预警',
        message: '温室3号区域温度超过安全阈值',
        sensor: '温度传感器 #3-A',
        value: 38.5,
        unit: '°C',
        threshold: 35.0,
        trend: 'increasing',
        time: '10分钟前'
      },
      {
        id: 2,
        severity: 'critical',
        title: '湿度过低预警',
        message: '灌溉系统可能故障，土壤湿度持续下降',
        sensor: '土壤传感器 #4-B',
        value: 16.2,
        unit: '%',
        threshold: 20.0,
        trend: 'decreasing',
        time: '25分钟前'
      },
      {
        id: 3,
        severity: 'warning',
        title: '光照强度异常',
        message: '大棚2号区域光照不足',
        sensor: '光照传感器 #2-C',
        value: 8500,
        unit: 'lux',
        threshold: 10000,
        trend: 'stable',
        time: '43分钟前'
      },
      {
        id: 4,
        severity: 'warning',
        title: 'CO₂浓度波动',
        message: '通风系统运行异常，二氧化碳浓度波动',
        sensor: 'CO₂传感器 #1-D',
        value: 950,
        unit: 'ppm',
        threshold: 800,
        trend: 'increasing',
        time: '1小时前'
      },
      {
        id: 5,
        severity: 'warning',
        title: '电池电量低',
        message: '多个传感器电池电量低于30%，需要维护',
        sensor: '系统',
        value: 28,
        unit: '%',
        threshold: 30,
        trend: 'decreasing',
        time: '2小时前'
      }
    ])
    
    // 获取警报图标
    const getAlertIcon = (severity) => {
      switch (severity) {
        case 'critical': return 'el-icon-warning'
        case 'warning': return 'el-icon-bell'
        default: return 'el-icon-info'
      }
    }
    
    // 刷新警报数据
    const refreshAlerts = () => {
      addMessage('system', '已刷新预警数据')
      // 模拟警报数据更新
      activeAlerts.value = activeAlerts.value.map(alert => {
        // 随机更新时间
        const times = ['刚刚', '1分钟前', '5分钟前', '10分钟前', '30分钟前', '1小时前']
        const randomTime = times[Math.floor(Math.random() * times.length)]
        
        // 随机更新趋势
        const trends = ['increasing', 'decreasing', 'stable']
        const randomTrend = trends[Math.floor(Math.random() * trends.length)]
        
        // 随机更新值
        const delta = (Math.random() * 2 - 1) * (alert.threshold * 0.1) // 在阈值的±10%范围内浮动
        let newValue = parseFloat((alert.value + delta).toFixed(1))
        if (alert.unit === 'lux') newValue = Math.round(newValue)
        if (alert.unit === 'ppm') newValue = Math.round(newValue)
        if (alert.unit === '%') newValue = Math.min(100, Math.max(0, newValue))
        
        return {
          ...alert,
          time: randomTime,
          trend: randomTrend,
          value: newValue
        }
      })
      
      // 更新警报统计
      updateAlertStats()
    }
    
    // 更新警报统计
    const updateAlertStats = () => {
      const counts = {
        critical: 0,
        warning: 0,
        normal: 0
      }
      
      Object.values(sensorData).forEach(sensor => {
        // 根据最后测量值判断传感器状态
        if (!sensor.lastValue) return
        
        let status = 'normal'
        
        if (sensor.type === 'temperature') {
          if (sensor.lastValue > 35) status = 'critical'
          else if (sensor.lastValue > 30) status = 'warning'
        } else if (sensor.type === 'humidity') {
          if (sensor.lastValue < 30) status = 'critical'
          else if (sensor.lastValue < 40) status = 'warning'
        } else if (sensor.type === 'soilMoisture') {
          if (sensor.lastValue < 20) status = 'critical'
          else if (sensor.lastValue < 30) status = 'warning'
        }
        
        counts[status]++
      })
      
      // 有可能没有足够的实际数据，确保默认数据可显示
      alertStats.critical = counts.critical || 2
      alertStats.warning = counts.warning || 3
      alertStats.normal = counts.normal || 16
    }
    
    // 连接WebSocket
    const connectWebSocket = async () => {
      try {
        console.log('AgricultureDataDashboard: 开始连接WebSocket...')
        // 连接WebSocket
        await websocketManager.connect()
        
        // 更新连接状态
        isConnected.value = websocketManager.isConnected()
        
        // 添加连接成功消息
        addMessage('system', '已连接到WebSocket服务器!')
        
        // 订阅主题
        await subscribeToTopics()
        
        console.log('AgricultureDataDashboard: WebSocket连接成功，已订阅主题')
      } catch (e) {
        console.error('AgricultureDataDashboard: WebSocket连接失败:', e)
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
    const subscribeToTopics = async () => {
      try {
        await websocketManager.subscribe('/topic/agriculture-sensor-data', (data) => {
          handleSensorData(data)
        })
        
        await websocketManager.subscribe('/topic/spark-stats', (data) => {
          handleSparkStats(data)
        })
        
        await websocketManager.subscribe('/topic/system-notifications', (data) => {
          addMessage('system', `系统通知: ${JSON.stringify(data)}`)
        })
        
        addMessage('system', '已订阅所有主题')
      } catch (e) {
        addMessage('error', `订阅失败: ${e.message || '未知错误'}`)
      }
    }
    
    // 处理传感器数据
    const handleSensorData = (data) => {
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
        
        // 更新传感器类型计数
        if (sensorCounts[sensorType] !== undefined) {
          sensorCounts[sensorType]++
        }
      }
      
      // 保持最多100个数据点
      if (sensorData[sensorId].values.length >= 100) {
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
      
      // 更新总数据点
      totalDataPoints.value++
      
      // 更新各类型传感器数据点总量
      if (sensorDataCounts[sensorType] !== undefined) {
        sensorDataCounts[sensorType]++
      }
      
      // 添加消息
      addMessage('data', `${getSensorTypeDisplay(sensorType)}: ${data.value}${data.unit} - ${data.location}`)
      
      // 更新数据统计信息
      updateSensorStats()
      
      // 更新图表
      updateCharts()
      
      // 检查是否需要生成预警
      checkAndGenerateAlerts(data)
    }
    
    // 处理Spark统计数据
    const handleSparkStats = (data) => {
      // 更新图表
      updateCharts()
    }
    
    // 更新传感器数据统计信息
    const updateSensorStats = () => {
      try {
        console.log('开始更新传感器统计信息')
        
        // 重置统计值总和和计数
        Object.keys(sensorStats).forEach(type => {
          // 保留原始值用于比较
          const prevMin = sensorStats[type].min;
          const prevMax = sensorStats[type].max;
          
          // 重置累计值
          sensorStats[type].sum = 0;
          sensorStats[type].count = 0;
          
          // 保留最小值/最大值的历史记录，以免数据丢失
          if (type === 'temperature') {
            sensorStats[type].min = Math.min(prevMin, 15);
            sensorStats[type].max = Math.max(prevMax, 35);
          } else if (type === 'humidity' || type === 'soilMoisture') {
            sensorStats[type].min = Math.min(prevMin, 20);
            sensorStats[type].max = Math.max(prevMax, 80);
          } else if (type === 'light') {
            sensorStats[type].min = Math.min(prevMin, 1000);
            sensorStats[type].max = Math.max(prevMax, 50000);
          } else if (type === 'co2') {
            sensorStats[type].min = Math.min(prevMin, 400);
            sensorStats[type].max = Math.max(prevMax, 1500);
          }
        });
        
        // 计算每种类型传感器的统计值
        Object.values(sensorData).forEach(sensor => {
          const type = sensor.type;
          if (!sensorStats[type] || sensor.values.length === 0) return;
          
          // 过滤有效值
          const validValues = sensor.values
            .filter(val => !isNaN(Number(val)) && val !== null)
            .map(Number);
          
          if (validValues.length === 0) return;
          
          // 更新最大最小值
          sensorStats[type].min = Math.min(sensorStats[type].min, Math.min(...validValues));
          sensorStats[type].max = Math.max(sensorStats[type].max, Math.max(...validValues));
          
          // 累加平均值
          const avg = validValues.reduce((sum, val) => sum + val, 0) / validValues.length;
          sensorStats[type].sum += avg;
          sensorStats[type].count += 1;
        });
        
        // 计算平均值和中值
        Object.keys(sensorStats).forEach(type => {
          if (sensorStats[type].count > 0) {
            // 平均值
            sensorStats[type].avg = sensorStats[type].sum / sensorStats[type].count;
            
            // 中值
            const allValues = [];
            Object.values(sensorData)
              .filter(sensor => sensor.type === type)
              .forEach(sensor => {
                allValues.push(...sensor.values
                  .filter(val => !isNaN(Number(val)) && val !== null)
                  .map(Number));
              });
            
            if (allValues.length > 0) {
              allValues.sort((a, b) => a - b);
              const mid = Math.floor(allValues.length / 2);
              sensorStats[type].median = allValues.length % 2 === 0
                ? (allValues[mid - 1] + allValues[mid]) / 2
                : allValues[mid];
            } else {
              // 如果没有值，使用默认中值
              if (type === 'temperature') sensorStats[type].median = 22;
              else if (type === 'humidity') sensorStats[type].median = 60;
              else if (type === 'soilMoisture') sensorStats[type].median = 40;
              else if (type === 'light') sensorStats[type].median = 10000;
              else if (type === 'co2') sensorStats[type].median = 600;
            }
          } else {
            // 如果没有有效计数，设置默认平均值
            if (type === 'temperature') sensorStats[type].avg = 25;
            else if (type === 'humidity') sensorStats[type].avg = 65;
            else if (type === 'soilMoisture') sensorStats[type].avg = 45;
            else if (type === 'light') sensorStats[type].avg = 20000;
            else if (type === 'co2') sensorStats[type].avg = 800;
            
            // 默认中值
            if (type === 'temperature') sensorStats[type].median = 24;
            else if (type === 'humidity') sensorStats[type].median = 63;
            else if (type === 'soilMoisture') sensorStats[type].median = 43;
            else if (type === 'light') sensorStats[type].median = 18000;
            else if (type === 'co2') sensorStats[type].median = 750;
          }
        });
        
        console.log('更新后的传感器统计信息:', JSON.stringify(sensorStats));
      } catch (error) {
        console.error('更新传感器统计信息出错:', error);
      }
    }
    
    // 添加消息到日志
    const addMessage = (type, content) => {
      const time = new Date().toLocaleTimeString()
      recentMessages.value.unshift({ type, content, time })
      
      // 保持最多10条消息
      if (recentMessages.value.length > 10) {
        recentMessages.value.pop()
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
    
    // 初始化图表
    const initCharts = () => {
      try {
        console.log('开始初始化图表', {
          sensorDataCounts: JSON.stringify(sensorDataCounts)
        });
        
        // 传感器分布图表
        if (sensorDistributionChart.value) {
          console.log('初始化传感器分布图表');
          distributionChartInstance = echarts.init(sensorDistributionChart.value);
          
          // 确保sensorDataCounts在使用前已完全初始化
          let chartData = [
            { value: 1520, name: '温度数据' },
            { value: 1480, name: '湿度数据' },
            { value: 1850, name: '土壤湿度数据' },
            { value: 1210, name: '光照强度数据' },
            { value: 1380, name: 'CO₂浓度数据' }
          ];
          
          // 如果sensorDataCounts已初始化，使用其数据
          if (sensorDataCounts && typeof sensorDataCounts === 'object') {
            chartData = [
              { value: sensorDataCounts.temperature || 1520, name: '温度数据' },
              { value: sensorDataCounts.humidity || 1480, name: '湿度数据' },
              { value: sensorDataCounts.soilMoisture || 1850, name: '土壤湿度数据' },
              { value: sensorDataCounts.light || 1210, name: '光照强度数据' },
              { value: sensorDataCounts.co2 || 1380, name: 'CO₂浓度数据' }
            ];
          }
          
          distributionChartInstance.setOption({
            tooltip: {
              trigger: 'item',
              formatter: '{a} <br/>{b}: {c} 条数据 ({d}%)',
              backgroundColor: 'rgba(0, 21, 41, 0.8)',
              borderColor: '#003a8c',
              textStyle: { color: '#fff' }
            },
            legend: {
              top: '5%',
              left: 'center',
              textStyle: { color: '#fff' }
            },
            series: [{
              name: '传感器类型',
              type: 'pie',
              radius: ['40%', '70%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 10,
                borderColor: '#0e2a47',
                borderWidth: 2
              },
              label: { show: false },
              emphasis: {
                label: {
                  show: true,
                  fontSize: 16,
                  fontWeight: 'bold'
                }
              },
              labelLine: { show: false },
              data: chartData
            }]
          });
        }
        
        // 地理地图
        if (sensorMapChart.value) {
          console.log('初始化地图');
          
          // 确保地图容器大小合适
          if (sensorMapChart.value.offsetWidth <= 0 || sensorMapChart.value.offsetHeight <= 0) {
            sensorMapChart.value.style.width = '100%';
            sensorMapChart.value.style.height = '300px';
          }
          
          // 首次尝试加载地图
          try {
            initMapChart();
          } catch (e) {
            console.error('首次地图初始化失败，将在挂载后重试', e);
          }
        }
        
        // 异常数据图表
        if (anomalyChart.value) {
          console.log('初始化异常数据图表');
          anomalyChartInstance = echarts.init(anomalyChart.value);
          anomalyChartInstance.setOption({
            tooltip: {
              trigger: 'axis',
              axisPointer: { type: 'cross' },
              backgroundColor: 'rgba(0, 21, 41, 0.8)',
              borderColor: '#003a8c',
              textStyle: { color: '#fff' }
            },
            legend: {
              data: ['温度异常', '湿度异常', '土壤湿度异常', '光照异常', 'CO₂异常'],
              textStyle: { color: '#fff' }
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '3%',
              containLabel: true
            },
            xAxis: [{
              type: 'category',
              boundaryGap: false,
              data: ['00:00', '03:00', '06:00', '09:00', '12:00', '15:00', '18:00', '21:00'],
              axisLabel: { color: '#fff' }
            }],
            yAxis: [{
              type: 'value',
              axisLabel: { color: '#fff' }
            }],
            series: [
              {
                name: '温度异常',
                type: 'line',
                areaStyle: {},
                data: [3, 2, 1, 4, 7, 5, 2, 1]
              },
              {
                name: '湿度异常',
                type: 'line',
                areaStyle: {},
                data: [2, 1, 2, 3, 2, 1, 0, 1]
              },
              {
                name: '土壤湿度异常',
                type: 'line',
                areaStyle: {},
                data: [1, 0, 1, 2, 1, 3, 2, 1]
              },
              {
                name: '光照异常',
                type: 'line',
                areaStyle: {},
                data: [0, 1, 2, 1, 0, 2, 3, 2]
              },
              {
                name: 'CO₂异常',
                type: 'line',
                areaStyle: {},
                data: [1, 2, 0, 1, 2, 1, 2, 3]
              }
            ]
          });
        }
        
        // 传感器状态图表
        if (sensorStatusChart.value) {
          console.log('初始化传感器状态图表');
          statusChartInstance = echarts.init(sensorStatusChart.value);
          statusChartInstance.setOption({
            tooltip: {
              trigger: 'item',
              formatter: '{a} <br/>{b}: {c} ({d}%)',
              backgroundColor: 'rgba(0, 21, 41, 0.8)',
              borderColor: '#003a8c',
              textStyle: { color: '#fff' }
            },
            legend: {
              bottom: '0%',
              itemGap: 10,
              textStyle: {
                color: '#fff',
                fontSize: 12
              }
            },
            series: [{
              name: '传感器状态',
              type: 'pie',
              radius: ['40%', '70%'],
              center: ['50%', '40%'],
              avoidLabelOverlap: false,
              itemStyle: {
                borderRadius: 6,
                borderColor: '#0d2840',
                borderWidth: 1
              },
              label: { show: false },
              labelLine: { show: false },
              data: [
                { value: 85, name: '正常', itemStyle: { color: '#52c41a' } },
                { value: 8, name: '异常', itemStyle: { color: '#f5222d' } },
                { value: 5, name: '离线', itemStyle: { color: '#faad14' } },
                { value: 2, name: '维护中', itemStyle: { color: '#1890ff' } }
              ]
            }]
          });
        }
        
        // 数据分析图表 - 百分比数据
        if (dataAnalysisPercentChart.value) {
          console.log('初始化百分比数据分析图表');
          try {
            dataAnalysisPercentChartInstance = echarts.init(dataAnalysisPercentChart.value);
            
            const option = {
              backgroundColor: 'transparent',
              tooltip: {
                trigger: 'axis',
                axisPointer: { type: 'shadow' },
                formatter: function(params) {
                  let result = params[0].name + '<br/>';
                  
                  params.forEach(param => {
                    let color = param.color;
                    let marker = `<span style="display:inline-block;margin-right:5px;width:10px;height:10px;background-color:${color};border-radius:50%;"></span>`;
                    let value = param.value;
                    result += marker + param.seriesName + ': ' + value + '%<br/>';
                  });
                  
                  return result;
                },
                backgroundColor: 'rgba(0, 21, 41, 0.8)',
                borderColor: '#003a8c',
                textStyle: { color: '#fff' }
              },
              legend: {
                data: ['最小值', '平均值', '最大值'],
                top: 10,
                textStyle: { color: '#fff' }
              },
              grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                top: 50,
                containLabel: true
              },
              xAxis: {
                type: 'category',
                data: ['湿度', '土壤湿度', '异常率'],
                axisLabel: {
                  color: '#fff',
                  rotate: 0
                }
              },
              yAxis: {
                type: 'value',
                name: '百分比',
                nameTextStyle: { color: '#fff' },
                axisLabel: {
                  color: '#fff',
                  formatter: '{value}%'
                }
              },
              series: [
                {
                  name: '最小值',
                  type: 'bar',
                  barWidth: '15%',
                  itemStyle: { color: '#1890ff' },
                  data: [
                    sensorStats.humidity.min || 0,
                    sensorStats.soilMoisture.min || 0,
                    1.5 // 异常率最小值
                  ]
                },
                {
                  name: '平均值',
                  type: 'bar',
                  barWidth: '15%',
                  itemStyle: { color: '#52c41a' },
                  data: [
                    sensorStats.humidity.avg || 0,
                    sensorStats.soilMoisture.avg || 0,
                    3.2 // 异常率平均值
                  ]
                },
                {
                  name: '最大值',
                  type: 'bar',
                  barWidth: '15%',
                  itemStyle: { color: '#f5222d' },
                  data: [
                    sensorStats.humidity.max || 0,
                    sensorStats.soilMoisture.max || 0,
                    6.8 // 异常率最大值
                  ]
                }
              ]
            };
            
            dataAnalysisPercentChartInstance.setOption(option);
            console.log('百分比数据分析图表初始化完成');
          } catch (error) {
            console.error('百分比数据分析图表初始化失败:', error);
            dataAnalysisPercentChart.value.innerHTML = `
              <div style="color: #f5222d; text-align: center; padding: 20px;">
                <p>图表加载失败</p>
                <p style="font-size: 12px;">${error.message || '未知错误'}</p>
              </div>
            `;
          }
        }
        
        // 数据分析图表 - 比较数据
        if (dataAnalysisComparisonChart.value) {
          console.log('初始化比较数据分析图表');
          try {
            dataAnalysisComparisonChartInstance = echarts.init(dataAnalysisComparisonChart.value);
            
            const option = {
              backgroundColor: 'transparent',
              tooltip: {
                trigger: 'axis',
                axisPointer: { type: 'shadow' },
                formatter: function(params) {
                  let result = params[0].name + '<br/>';
                  
                  params.forEach(param => {
                    let color = param.color;
                    let marker = `<span style="display:inline-block;margin-right:5px;width:10px;height:10px;background-color:${color};border-radius:50%;"></span>`;
                    let value = param.value;
                    let unit = '';
                    
                    if (param.name.includes('光照')) unit = 'lux';
                    else if (param.name.includes('CO₂')) unit = 'ppm';
                    
                    result += marker + param.seriesName + ': ' + value + unit + '<br/>';
                  });
                  
                  return result;
                },
                backgroundColor: 'rgba(0, 21, 41, 0.8)',
                borderColor: '#003a8c',
                textStyle: { color: '#fff' }
              },
              legend: {
                data: ['最小值', '平均值', '最大值'],
                top: 10,
                textStyle: { color: '#fff' }
              },
              grid: {
                left: '3%',
                right: '4%',
                bottom: '3%',
                top: 50,
                containLabel: true
              },
              xAxis: {
                type: 'category',
                data: ['光照强度', 'CO₂浓度'],
                axisLabel: {
                  color: '#fff',
                  rotate: 0
                }
              },
              yAxis: {
                type: 'value',
                name: '数值',
                nameTextStyle: { color: '#fff' },
                axisLabel: {
                  color: '#fff'
                }
              },
              series: [
                {
                  name: '最小值',
                  type: 'bar',
                  barWidth: '15%',
                  itemStyle: { color: '#1890ff' },
                  data: [
                    sensorStats.light.min || 0,
                    sensorStats.co2.min || 0
                  ]
                },
                {
                  name: '平均值',
                  type: 'bar',
                  barWidth: '15%',
                  itemStyle: { color: '#52c41a' },
                  data: [
                    sensorStats.light.avg || 0,
                    sensorStats.co2.avg || 0
                  ]
                },
                {
                  name: '最大值',
                  type: 'bar',
                  barWidth: '15%',
                  itemStyle: { color: '#f5222d' },
                  data: [
                    sensorStats.light.max || 0,
                    sensorStats.co2.max || 0
                  ]
                }
              ]
            };
            
            dataAnalysisComparisonChartInstance.setOption(option);
            console.log('比较数据分析图表初始化完成');
          } catch (error) {
            console.error('比较数据分析图表初始化失败:', error);
            dataAnalysisComparisonChart.value.innerHTML = `
              <div style="color: #f5222d; text-align: center; padding: 20px;">
                <p>图表加载失败</p>
                <p style="font-size: 12px;">${error.message || '未知错误'}</p>
              </div>
            `;
          }
        }
      } catch (error) {
        console.error('图表初始化失败:', error);
      }
    }
    
    // 更新图表
    const updateCharts = () => {
      try {
        console.log('开始更新图表');
        
        // 更新传感器分布图表
        if (distributionChartInstance) {
          // 确保数据有效
          let chartData = [
            { value: sensorDataCounts.temperature || 1520, name: '温度数据' },
            { value: sensorDataCounts.humidity || 1480, name: '湿度数据' },
            { value: sensorDataCounts.soilMoisture || 1850, name: '土壤湿度数据' },
            { value: sensorDataCounts.light || 1210, name: '光照强度数据' },
            { value: sensorDataCounts.co2 || 1380, name: 'CO₂浓度数据' }
          ];
          
          distributionChartInstance.setOption({
            series: [{
              data: chartData
            }]
          });
        }
        
        // 更新地图图表
        if (mapChartInstance) {
          try {
            // 生成各省份传感器的分布数据（动态更新）
            const updateSensorDistribution = () => {
              const provinces = [
                '北京', '天津', '上海', '重庆', '河北', '山西', '江苏', '浙江', 
                '广东', '四川', '山东', '河南', '湖北', '陕西', '安徽', '福建', 
                '云南', '黑龙江', '湖南', '辽宁', '吉林', '江西', '广西', '海南', 
                '贵州', '内蒙古', '甘肃', '青海', '新疆', '西藏', '宁夏', '台湾'
              ];
              
              // 返回传感器分布点数据
              return provinces.map(province => {
                // 从地图数据中找到对应省份的坐标
                const provinceFeature = chinaGeoJson.features.find(
                  feature => feature.properties.name === province
                );
                
                if (!provinceFeature) return null;
                
                // 获取省份中心点坐标
                const coordinates = provinceFeature.geometry.coordinates[0];
                
                // 计算中心点
                let centerLng = 0, centerLat = 0;
                coordinates.forEach(coord => {
                  centerLng += coord[0];
                  centerLat += coord[1];
                });
                centerLng /= coordinates.length;
                centerLat /= coordinates.length;
                
                // 基础传感器数量（轻微波动）
                const baseValue = Math.floor(Math.random() * 40) + 180;
                
                // 为不同省份生成不同类型的传感器分布
                const sensorCounts = {};
                sensorCounts.temperature = Math.floor(Math.random() * 30) + 20;
                sensorCounts.humidity = Math.floor(Math.random() * 30) + 20;
                sensorCounts.soilMoisture = Math.floor(Math.random() * 40) + 15;
                sensorCounts.light = Math.floor(Math.random() * 25) + 15;
                sensorCounts.co2 = Math.floor(Math.random() * 20) + 15;
                
                return {
                  province,
                  centerCoord: [centerLng, centerLat],
                  value: baseValue,
                  sensorCounts
                };
              }).filter(item => item !== null);
            };
            
            // 获取更新的传感器分布数据
            const updatedDistribution = updateSensorDistribution();
            
            // 处理地图数据
            const mapData = updatedDistribution.map(item => ({
              name: item.province,
              value: item.value,
              sensorCounts: item.sensorCounts
            }));
            
            mapChartInstance.setOption({
              series: [{
                name: '区域传感器',
                data: mapData
              }]
            });
            
            setTimeout(() => {
              if (mapChartInstance) mapChartInstance.resize();
            }, 100);
          } catch (error) {
            console.error('更新地图数据失败:', error);
          }
        }
        
        // 更新数据分析图表
        if (dataAnalysisPercentChartInstance) {
          try {
            dataAnalysisPercentChartInstance.setOption({
              series: [
                {
                  name: '最小值',
                  data: [
                    sensorStats.humidity.min || 0,
                    sensorStats.soilMoisture.min || 0,
                    1.5 // 异常率最小值
                  ]
                },
                {
                  name: '平均值',
                  data: [
                    sensorStats.humidity.avg || 0,
                    sensorStats.soilMoisture.avg || 0,
                    3.2 // 异常率平均值
                  ]
                },
                {
                  name: '最大值',
                  data: [
                    sensorStats.humidity.max || 0,
                    sensorStats.soilMoisture.max || 0,
                    6.8 // 异常率最大值
                  ]
                }
              ]
            });
            
            // 确保图表正确显示
            setTimeout(() => {
              if (dataAnalysisPercentChartInstance) dataAnalysisPercentChartInstance.resize();
            }, 100);
          } catch (error) {
            console.error('更新百分比数据分析图表失败:', error);
          }
        }
        
        if (dataAnalysisComparisonChartInstance) {
          try {
            dataAnalysisComparisonChartInstance.setOption({
              series: [
                {
                  name: '最小值',
                  data: [
                    sensorStats.light.min || 0,
                    sensorStats.co2.min || 0
                  ]
                },
                {
                  name: '平均值',
                  data: [
                    sensorStats.light.avg || 0,
                    sensorStats.co2.avg || 0
                  ]
                },
                {
                  name: '最大值',
                  data: [
                    sensorStats.light.max || 0,
                    sensorStats.co2.max || 0
                  ]
                }
              ]
            });
            
            // 确保图表正确显示
            setTimeout(() => {
              if (dataAnalysisComparisonChartInstance) dataAnalysisComparisonChartInstance.resize();
            }, 100);
          } catch (error) {
            console.error('更新比较数据分析图表失败:', error);
          }
        }
      } catch (error) {
        console.error('更新图表失败:', error);
      }
    }
    
    // 初始化时间显示
    const initTimeDisplay = () => {
      timeInterval.value = setInterval(() => {
        currentTime.value = new Date().toLocaleString()
      }, 1000)
    }
    
    
    // 返回主应用
    const goBack = () => {
      window.location.href = '/'
    }
    
    // 组件挂载时
    onMounted(() => {
      // 初始化时间显示
      initTimeDisplay()
      
      // 初始化传感器数据
      initSensorData()
      
      // 确保地图容器准备好
      nextTick(() => {
        // 检查地图容器是否准备好
        console.log('检查地图容器状态:', sensorMapChart.value);
        if (sensorMapChart.value) {
          console.log('地图容器尺寸:', sensorMapChart.value.offsetWidth, sensorMapChart.value.offsetHeight);
        }
        
        // 延迟初始化图表，确保DOM已经渲染
        setTimeout(() => {
          try {
            // 初始化所有图表
            initCharts();
            
            // 确保所有图表都能正确调整大小
            window.dispatchEvent(new Event('resize'));
            
            // 针对地图额外处理
            if (mapChartInstance) {
              setTimeout(() => {
                console.log('强制调整地图尺寸');
                try {
                  mapChartInstance.resize();
                } catch(e) {
                  console.error('地图调整失败:', e);
                  // 尝试重新初始化地图
                  initMapChart();
                }
              }, 300);
            } else {
              // 如果地图实例不存在，尝试初始化
              console.log('地图实例不存在，尝试初始化');
              initMapChart();
            }
            
            // 初始刷新数据分析图表
            if (dataAnalysisPercentChartInstance) {
              setTimeout(() => {
                console.log('强制刷新百分比数据分析图表');
                updateSensorStats(); // 先更新统计数据
                dataAnalysisPercentChartInstance.resize();
                updateCharts(); // 再更新图表
              }, 300);
            }
            
            if (dataAnalysisComparisonChartInstance) {
              setTimeout(() => {
                console.log('强制刷新比较数据分析图表');
                dataAnalysisComparisonChartInstance.resize();
              }, 300);
            }
            
            // 删除底部24小时趋势分析标题
            const bottomLabels = document.querySelectorAll('.bottom-chart-labels');
            if (bottomLabels) {
              bottomLabels.forEach(label => {
                label.style.display = 'none';
              });
            }
          } catch (error) {
            console.error('图表初始化失败:', error);
          }
        }, 500);
      });
      
      // 自动连接WebSocket
      console.log('AgricultureDataDashboard: 组件挂载，自动连接WebSocket');
      connectWebSocket();
      
      // 窗口大小变化时调整图表大小
      window.addEventListener('resize', handleResize);
    })
    
    // 组件卸载时
    onUnmounted(() => {
      // 清除时间间隔
      if (timeInterval.value) {
        clearInterval(timeInterval.value)
      }
      
      // 断开WebSocket连接
      disconnectWebSocket()
      
      // 销毁图表实例
      distributionChartInstance?.dispose()
      mapChartInstance?.dispose()
      anomalyChartInstance?.dispose()
      statusChartInstance?.dispose()
      dataAnalysisPercentChartInstance?.dispose()
      dataAnalysisComparisonChartInstance?.dispose()
      
      // 移除窗口大小变化监听
      window.removeEventListener('resize', handleResize)
    })
    
    // 处理窗口大小变化
    const handleResize = () => {
      try {
        console.log('窗口大小变化，调整图表尺寸')
        if (distributionChartInstance) {
          distributionChartInstance.resize()
        }
        if (mapChartInstance) {
          console.log('调整地图大小')
          mapChartInstance.resize()
          
          // 重置地图视图，确保所有省份可见
          mapChartInstance.setOption({
            series: [{
              zoom: 1.0,
              layoutCenter: ['50%', '50%'],
              layoutSize: '100%'
            }]
          });
        }
        if (anomalyChartInstance) {
          anomalyChartInstance.resize()
        }
        if (statusChartInstance) {
          statusChartInstance.resize()
        }
        if (dataAnalysisPercentChartInstance) {
          dataAnalysisPercentChartInstance.resize()
        }
        if (dataAnalysisComparisonChartInstance) {
          dataAnalysisComparisonChartInstance.resize()
        }
        
        // 重新更新所有图表确保显示正确
        updateCharts()
      } catch (error) {
        console.error('调整图表大小失败:', error)
      }
    }
    
    // 初始化传感器数据，确保有合理的初始数据显示
    const initSensorData = () => {
      console.log('初始化传感器数据 - 开始', { 
        sensorCounts: JSON.stringify(sensorCounts),
        sensorDataCounts: JSON.stringify(sensorDataCounts)
      });
      
      // 确保传感器计数不为零
      if (sensorCounts.temperature === 0) sensorCounts.temperature = 5;
      if (sensorCounts.humidity === 0) sensorCounts.humidity = 4;
      if (sensorCounts.soilMoisture === 0) sensorCounts.soilMoisture = 6;
      if (sensorCounts.light === 0) sensorCounts.light = 3;
      if (sensorCounts.co2 === 0) sensorCounts.co2 = 3;
      
      // 确保每种类型传感器的数据点总量不为零
      if (sensorDataCounts.temperature === 0) sensorDataCounts.temperature = 1520;
      if (sensorDataCounts.humidity === 0) sensorDataCounts.humidity = 1480;
      if (sensorDataCounts.soilMoisture === 0) sensorDataCounts.soilMoisture = 1850;
      if (sensorDataCounts.light === 0) sensorDataCounts.light = 1210;
      if (sensorDataCounts.co2 === 0) sensorDataCounts.co2 = 1380;
      
      // 初始化总数据点
      if (totalDataPoints.value === 0) {
        totalDataPoints.value = sensorDataCounts.temperature + 
                               sensorDataCounts.humidity + 
                               sensorDataCounts.soilMoisture + 
                               sensorDataCounts.light + 
                               sensorDataCounts.co2;
      }
      
      console.log('初始化传感器数据 - 完成', { 
        sensorCounts: JSON.stringify(sensorCounts),
        sensorDataCounts: JSON.stringify(sensorDataCounts),
        totalDataPoints: totalDataPoints.value
      });
      
      // 更新图表数据
      updateCharts();
    };
    
    // 单独提取地图初始化函数
    const initMapChart = () => {
      if (!sensorMapChart.value) return;
      
      console.log('单独初始化地图');
      try {
        // 先清理可能存在的实例
        if (mapChartInstance) {
          mapChartInstance.dispose();
          mapChartInstance = null;
        }
        
        // 确保DOM尺寸正常
        console.log('地图容器尺寸检查:', 
          sensorMapChart.value.offsetWidth, 
          sensorMapChart.value.offsetHeight);
          
        if (sensorMapChart.value.offsetWidth <= 0 || sensorMapChart.value.offsetHeight <= 0) {
          sensorMapChart.value.style.width = '100%';
          sensorMapChart.value.style.height = '250px';
          console.warn('地图容器尺寸异常，设置默认尺寸后重试');
          setTimeout(initMapChart, 300);
          return;
        }
        
        // 先注册地图，确保中国地图数据可用
        try {
          echarts.registerMap('china', chinaGeoJson);
          console.log('地图注册成功');
        } catch (err) {
          console.error('地图注册失败:', err);
          throw new Error('地图数据注册失败，请检查地图数据是否正确');
        }
        
        // 初始化地图实例
        mapChartInstance = echarts.init(sensorMapChart.value);
        
        // 生成各省份传感器的分布数据
        const generateSensorPoints = () => {
          const provinces = [
            '北京', '天津', '上海', '重庆', '河北', '山西', '江苏', '浙江', 
            '广东', '四川', '山东', '河南', '湖北', '陕西', '安徽', '福建', 
            '云南', '黑龙江', '湖南', '辽宁', '吉林', '江西', '广西', '海南', 
            '贵州', '内蒙古', '甘肃', '青海', '新疆', '西藏', '宁夏', '台湾'
          ];
          
          // 返回传感器分布点数据
          return provinces.map(province => {
            // 从地图数据中找到对应省份的坐标
            const provinceFeature = chinaGeoJson.features.find(
              feature => feature.properties.name === province
            );
            
            if (!provinceFeature) return null;
            
            // 获取省份中心点坐标
            const coordinates = provinceFeature.geometry.coordinates[0];
            const points = [];
            
            // 计算中心点
            let centerLng = 0, centerLat = 0;
            coordinates.forEach(coord => {
              centerLng += coord[0];
              centerLat += coord[1];
            });
            centerLng /= coordinates.length;
            centerLat /= coordinates.length;
            
            // 基础传感器数量
            const baseValue = Math.floor(Math.random() * 200) + 100;
            
            // 为不同省份生成不同类型的传感器分布
            const sensorTypes = ['temperature', 'humidity', 'soilMoisture', 'light', 'co2'];
            const sensorCounts = {};
            
            // 随机生成各类型传感器数量
            sensorTypes.forEach(type => {
              sensorCounts[type] = Math.floor(Math.random() * 50) + 10;
            });
            
            // 随机生成周围的传感器点位
            const sensorPoints = [];
            const numPoints = Math.min(Math.floor(baseValue / 30), 8); // 限制点位数量
            
            for (let i = 0; i < numPoints; i++) {
              // 随机偏移
              const offsetLng = (Math.random() - 0.5) * 0.8;
              const offsetLat = (Math.random() - 0.5) * 0.8;
              
              // 随机选择传感器类型
              const randomType = sensorTypes[Math.floor(Math.random() * sensorTypes.length)];
              
              // 添加传感器点位
              sensorPoints.push({
                name: `${province}${i+1}号点`,
                value: [centerLng + offsetLng, centerLat + offsetLat, randomType],
                type: randomType,
                symbolSize: Math.random() * 10 + 5 // 5-15之间的随机大小
              });
            }
            
            return {
              province,
              centerCoord: [centerLng, centerLat],
              value: baseValue,
              sensorCounts,
              sensorPoints
            };
          }).filter(item => item !== null);
        };
        
        // 获取传感器分布数据
        const sensorDistribution = generateSensorPoints();
        
        // 处理地图数据
        const mapData = sensorDistribution.map(item => ({
          name: item.province,
          value: item.value,
          sensorCounts: item.sensorCounts
        }));
        
        // 生成散点图数据
        const scatterData = [];
        
        // 将所有省份的传感器点位合并到一个数组中
        sensorDistribution.forEach(province => {
          scatterData.push(...province.sensorPoints);
        });
        
        // 设置地图配置
        const option = {
          backgroundColor: 'transparent',
          tooltip: {
            trigger: 'item',
            formatter: function (params) {
              // 处理不同系列的提示信息
              if (params.seriesType === 'map') {
                // 地图区域提示
                const sensorCounts = params.data.sensorCounts || {};
                return `
                  <div style="padding: 5px; line-height: 1.5;">
                    <div style="font-weight: bold; margin-bottom: 5px;">${params.name}</div>
                    <div>总传感器: ${params.value || 0}个</div>
                    <div>温度传感器: ${sensorCounts.temperature || 0}个</div>
                    <div>湿度传感器: ${sensorCounts.humidity || 0}个</div>
                    <div>土壤传感器: ${sensorCounts.soilMoisture || 0}个</div>
                    <div>光照传感器: ${sensorCounts.light || 0}个</div>
                    <div>CO₂传感器: ${sensorCounts.co2 || 0}个</div>
                  </div>
                `;
              } else if (params.seriesType === 'effectScatter') {
                // 散点图提示
                const typeName = {
                  'temperature': '温度传感器',
                  'humidity': '湿度传感器',
                  'soilMoisture': '土壤传感器',
                  'light': '光照传感器',
                  'co2': 'CO₂传感器'
                };
                
                return `
                  <div style="padding: 5px;">
                    <div>${params.name}</div>
                    <div>${typeName[params.data.value[2]] || '传感器'}</div>
                    <div>状态: 正常运行</div>
                  </div>
                `;
              }
              return params.name;
            },
            backgroundColor: 'rgba(0, 21, 41, 0.8)',
            borderColor: '#003a8c',
            textStyle: { color: '#fff' }
          },
          legend: {
            data: ['区域传感器', '温度传感器', '湿度传感器', '土壤传感器', '光照传感器', 'CO₂传感器'],
            selectedMode: 'multiple',
            right: 10,
            top: 10,
            textStyle: {
              color: '#fff'
            },
            itemGap: 8,
            icon: 'circle',
            selected: {
              '区域传感器': true,
              '温度传感器': true,
              '湿度传感器': true,
              '土壤传感器': true,
              '光照传感器': true,
              'CO₂传感器': true
            }
          },
          geo: {
            map: 'china',
            roam: true,
            zoom: 1.2,
            center: [104.5, 36.5],
            label: {
              show: true,
              color: '#fff',
              fontSize: 10
            },
            itemStyle: {
              areaColor: 'rgba(10, 30, 70, 0.3)',
              borderColor: 'rgba(24, 144, 255, 0.6)',
              borderWidth: 1,
              shadowColor: 'rgba(24, 144, 255, 0.4)',
              shadowBlur: 10
            },
            emphasis: {
              itemStyle: {
                areaColor: 'rgba(24, 144, 255, 0.4)',
                shadowColor: 'rgba(24, 144, 255, 0.7)',
                shadowBlur: 20
              },
              label: {
                color: '#fff',
                fontSize: 12
              }
            }
          },
          visualMap: {
            type: 'continuous',
            show: true,
            min: 0,
            max: 500,
            left: 'left',
            bottom: 20,
            text: ['多', '少'],
            inRange: {
              color: ['#0e2a47', '#1a91ff', '#52c41a', '#fa8c16', '#eb2f96']
            },
            calculable: true,
            textStyle: { color: '#fff' }
          },
          series: [
            {
              name: '区域传感器',
              type: 'map',
              geoIndex: 0,
              data: mapData
            },
            {
              name: '温度传感器',
              type: 'effectScatter',
              coordinateSystem: 'geo',
              symbolSize: 6,
              showEffectOn: 'render',
              rippleEffect: {
                brushType: 'stroke',
                scale: 3,
                period: 4
              },
              itemStyle: {
                color: '#ff7875'
              },
              data: scatterData.filter(item => item.type === 'temperature')
            },
            {
              name: '湿度传感器',
              type: 'effectScatter',
              coordinateSystem: 'geo',
              symbolSize: 6,
              showEffectOn: 'render',
              rippleEffect: {
                brushType: 'stroke',
                scale: 3
              },
              itemStyle: {
                color: '#69c0ff'
              },
              data: scatterData.filter(item => item.type === 'humidity')
            },
            {
              name: '土壤传感器',
              type: 'effectScatter',
              coordinateSystem: 'geo',
              symbolSize: 6,
              showEffectOn: 'render',
              rippleEffect: {
                brushType: 'stroke',
                scale: 3
              },
              itemStyle: {
                color: '#b37feb'
              },
              data: scatterData.filter(item => item.type === 'soilMoisture')
            },
            {
              name: '光照传感器',
              type: 'effectScatter',
              coordinateSystem: 'geo',
              symbolSize: 6,
              showEffectOn: 'render',
              rippleEffect: {
                brushType: 'stroke',
                scale: 3
              },
              itemStyle: {
                color: '#ffd666'
              },
              data: scatterData.filter(item => item.type === 'light')
            },
            {
              name: 'CO₂传感器',
              type: 'effectScatter',
              coordinateSystem: 'geo',
              symbolSize: 6,
              showEffectOn: 'render',
              rippleEffect: {
                brushType: 'stroke',
                scale: 3
              },
              itemStyle: {
                color: '#5cdbd3'
              },
              data: scatterData.filter(item => item.type === 'co2')
            }
          ]
        };
        
        // 设置选项
        mapChartInstance.setOption(option);
        
        // 强制调整大小
        mapChartInstance.resize();
        console.log('地图初始化成功');
      } catch (error) {
        console.error('地图初始化失败:', error);
        mapChartInstance = null;
        
        // 显示错误提示
        if (sensorMapChart.value) {
          sensorMapChart.value.innerHTML = `
            <div style="color: #f5222d; text-align: center; padding: 20px;">
              <p>地图加载失败</p>
              <p style="font-size: 12px;">${error.message || '未知错误'}</p>
              <button id="mapRetryBtn" 
                      style="background: #1890ff; color: white; border: none; padding: 5px 10px; border-radius: 4px; cursor: pointer;">
                重试加载
              </button>
            </div>
          `;
          
          // 添加重试事件
          setTimeout(() => {
            const retryBtn = document.getElementById('mapRetryBtn');
            if (retryBtn) {
              retryBtn.addEventListener('click', initMapChart);
            }
          }, 100);
        }
      }
    };
    
    // 检查并生成预警
    const checkAndGenerateAlerts = (data) => {
      const sensorId = data.sensorId
      const sensorType = data.sensorType
      const value = data.value
      
      // 判断是否需要触发警报
      let alertNeeded = false
      let severity = 'normal'
      let message = ''
      
      // 根据不同类型传感器设置阈值
      if (sensorType === 'temperature') {
        if (value > 35) {
          alertNeeded = true
          severity = 'critical'
          message = `温度过高 (${value}°C)，超过安全阈值35°C`
        } else if (value > 30) {
          alertNeeded = true
          severity = 'warning'
          message = `温度偏高 (${value}°C)，接近安全阈值35°C`
        }
      } else if (sensorType === 'humidity') {
        if (value < 30) {
          alertNeeded = true
          severity = 'critical'
          message = `湿度过低 (${value}%)，低于安全阈值30%`
        } else if (value < 40) {
          alertNeeded = true
          severity = 'warning'
          message = `湿度偏低 (${value}%)，接近安全阈值30%`
        }
      } else if (sensorType === 'soilMoisture') {
        if (value < 20) {
          alertNeeded = true
          severity = 'critical'
          message = `土壤湿度过低 (${value}%)，低于安全阈值20%`
        } else if (value < 30) {
          alertNeeded = true
          severity = 'warning'
          message = `土壤湿度偏低 (${value}%)，接近安全阈值20%`
        }
      } else if (sensorType === 'co2') {
        if (value > 1200) {
          alertNeeded = true
          severity = 'critical'
          message = `二氧化碳浓度过高 (${value}ppm)，超过安全阈值1200ppm`
        } else if (value > 1000) {
          alertNeeded = true
          severity = 'warning'
          message = `二氧化碳浓度偏高 (${value}ppm)，接近安全阈值1200ppm`
        }
      } else if (sensorType === 'light') {
        if (value < 5000) {
          alertNeeded = true
          severity = 'warning'
          message = `光照强度不足 (${value}lux)，低于最佳阈值5000lux`
        }
      }
      
      // 如果需要触发警报且是严重警报，添加到活跃警报列表
      if (alertNeeded && (severity === 'critical' || severity === 'warning')) {
        // 获取传感器名称
        const sensorName = `${getSensorTypeDisplay(sensorType)}传感器 #${sensorId.substring(0, 3)}`
        
        // 检查是否已有此传感器的警报
        const existingAlertIndex = activeAlerts.value.findIndex(alert => 
          alert.sensor === sensorName && alert.title.includes(getSensorTypeDisplay(sensorType))
        )
        
        const alert = {
          id: Date.now(),
          severity,
          title: severity === 'critical' ? `${getSensorTypeDisplay(sensorType)}过${value > 0 ? '高' : '低'}预警` : `${getSensorTypeDisplay(sensorType)}异常`,
          message,
          sensor: sensorName,
          value,
          unit: data.unit || '',
          threshold: severity === 'critical' ? (value > 0 ? 35 : 30) : (value > 0 ? 30 : 40),
          trend: data.trend || 'stable',
          time: '刚刚'
        }
        
        // 如果已有此传感器警报则更新，否则添加到列表头部
        if (existingAlertIndex !== -1) {
          activeAlerts.value.splice(existingAlertIndex, 1, alert)
        } else {
          activeAlerts.value.unshift(alert)
          // 保持最多显示8个警报
          if (activeAlerts.value.length > 8) {
            activeAlerts.value.pop()
          }
        }
        
        // 如果是严重警报，添加到消息列表
        if (severity === 'critical') {
          addMessage('error', `${sensorName}: ${message}`)
        }
        
        // 更新警报统计
        updateAlertStats()
      }
    }
    
    return {
      isConnected,
      currentTime,
      sensorCounts,
      sensorDataCounts,
      totalDataPoints,
      dataIncreaseRate,
      recentMessages,
      sensorDistributionChart,
      sensorMapChart,
      anomalyChart,
      sensorStatusChart,
      dataAnalysisPercentChart,
      dataAnalysisComparisonChart,
      connectWebSocket,
      disconnectWebSocket,
      goBack,
      activeAlerts,
      alertStats,
      getAlertIcon,
      refreshAlerts
    }
  }
}
</script>

<style scoped>
.agriculture-dashboard {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: linear-gradient(135deg, #001529 0%, #00152980 50%, #001529 100%);
  color: #fff;
  overflow: hidden;
  font-family: 'Arial', sans-serif;
  z-index: 9999; /* 确保在最上层 */
  display: flex;
  flex-direction: column;
  animation: fadeIn 0.8s ease-out;
  background-image: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI1IiBoZWlnaHQ9IjUiPgo8cmVjdCB3aWR0aD0iNSIgaGVpZ2h0PSI1IiBmaWxsPSIjMDAxNTI5Ij48L3JlY3Q+CjxwYXRoIGQ9Ik0wIDVMNSAwWk02IDRMNCA2Wk0tMSAxTDEgLTFaIiBzdHJva2U9IiMwMDM2NjAiIHN0cm9rZS13aWR0aD0iMSI+PC9wYXRoPgo8L3N2Zz4=');
  background-attachment: fixed;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  height: 60px;
  background: linear-gradient(90deg, rgba(0, 33, 64, 0.95) 0%, rgba(0, 82, 204, 0.9) 100%);
  border-bottom: 2px solid rgba(24, 144, 255, 0.5);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  position: relative;
  z-index: 10;
}

.dashboard-header::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 2px;
  background: linear-gradient(90deg, transparent, rgba(80, 200, 255, 0.8), transparent);
  animation: headerGlow 4s infinite alternate;
}

@keyframes headerGlow {
  0% { opacity: 0.3; }
  100% { opacity: 0.9; }
}

.title {
  font-size: 26px;
  font-weight: 600;
  color: #fff;
  text-shadow: 0 0 15px rgba(24, 144, 255, 0.9), 0 0 30px rgba(24, 144, 255, 0.5);
  margin: 0;
  letter-spacing: 1.5px;
  background: linear-gradient(90deg, #ffffff, #69c0ff);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  animation: titlePulse 3s infinite alternate;
}

@keyframes titlePulse {
  0% { text-shadow: 0 0 10px rgba(24, 144, 255, 0.8); }
  100% { text-shadow: 0 0 20px rgba(24, 144, 255, 1), 0 0 30px rgba(24, 144, 255, 0.6); }
}

.connection-status {
  display: flex;
  align-items: center;
  background: rgba(0, 21, 41, 0.4);
  padding: 5px 10px;
  border-radius: 50px;
  backdrop-filter: blur(5px);
  border: 1px solid rgba(30, 136, 229, 0.3);
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
  transition: all 0.5s ease;
}

.connected {
  background-color: #52c41a;
  box-shadow: 0 0 12px #52c41a;
  animation: pulseConnected 2s infinite;
}

@keyframes pulseConnected {
  0% { box-shadow: 0 0 8px #52c41a; }
  50% { box-shadow: 0 0 16px #52c41a; }
  100% { box-shadow: 0 0 8px #52c41a; }
}

.disconnected {
  background-color: #f5222d;
  box-shadow: 0 0 12px #f5222d;
  animation: pulseDisconnected 2s infinite;
}

@keyframes pulseDisconnected {
  0% { box-shadow: 0 0 8px #f5222d; }
  50% { box-shadow: 0 0 16px #f5222d; }
  100% { box-shadow: 0 0 8px #f5222d; }
}

.time-display {
  font-size: 18px;
  font-family: 'Courier New', monospace;
  color: #1890ff;
  text-shadow: 0 0 10px rgba(24, 144, 255, 0.7);
  background: rgba(0, 21, 41, 0.4);
  padding: 6px 12px;
  border-radius: 6px;
  border: 1px solid rgba(24, 144, 255, 0.3);
  letter-spacing: 1px;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
  padding: 15px;
  height: calc(100vh - 60px);
  overflow: hidden;
  animation: contentFadeIn 1s ease-out;
}

@keyframes contentFadeIn {
  from { 
    opacity: 0; 
    transform: translateY(20px);
  }
  to { 
    opacity: 1;
    transform: translateY(0);
  }
}

.dashboard-summary {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 15px;
  min-height: 110px;
  max-height: 120px;
}

.charts-row {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
  opacity: 0;
  animation: slideIn 0.8s ease forwards;
  animation-delay: 0.3s;
}

.upper-row {
  height: 340px;
}

.bottom-row {
  height: 340px;
  flex-wrap: wrap;
  animation-delay: 0.6s;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateX(-20px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.chart-container {
  position: relative;
  padding: 18px;
  border-radius: 8px;
  box-shadow: 0 8px 24px 0 rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
  background: linear-gradient(135deg, rgba(0, 35, 60, 0.8) 0%, rgba(0, 21, 41, 0.9) 100%);
  border: 1px solid rgba(24, 144, 255, 0.2);
  backdrop-filter: blur(5px);
  transition: transform 0.3s ease, box-shadow 0.3s ease, border-color 0.3s ease;
  overflow: hidden;
}

.chart-container::before {
  content: '';
  position: absolute;
  top: -50%;
  left: -50%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle at center, rgba(24, 144, 255, 0.1) 0%, transparent 60%);
  opacity: 0.4;
  animation: rotateBg 30s linear infinite;
}

@keyframes rotateBg {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.chart-container:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 28px rgba(0, 0, 0, 0.3);
  border-color: rgba(24, 144, 255, 0.5);
}

.chart-container h3 {
  margin-top: 0;
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 500;
  color: #e6f7ff;
  display: flex;
  align-items: center;
  text-shadow: 0 0 8px rgba(24, 144, 255, 0.5);
}

.chart-container h3::before {
  content: '';
  display: inline-block;
  width: 6px;
  height: 16px;
  background: #1890ff;
  margin-right: 8px;
  border-radius: 3px;
  box-shadow: 0 0 8px rgba(24, 144, 255, 0.8);
}

.chart-container .chart {
  flex: 1;
  width: 100%;
  min-height: 200px;
  position: relative;
  z-index: 2;
}

.chart-container.distribution-chart,
.chart-container.map-container,
.chart-container.anomaly-chart {
  flex: 1;
  background: linear-gradient(135deg, rgba(0, 35, 70, 0.9) 0%, rgba(0, 21, 41, 0.95) 100%);
}

.chart-container.realtime-data {
  flex: 1;
  background: linear-gradient(135deg, rgba(0, 35, 70, 0.9) 0%, rgba(0, 21, 41, 0.95) 100%);
  min-width: 300px;
}

.chart-container.data-analysis-percent,
.chart-container.data-analysis-comparison {
  flex: 1;
  min-width: 300px;
  min-height: 280px;
  overflow: hidden;
}

.chart-container.status-chart {
  flex: 1;
  min-width: 300px;
}

.data-card {
  background: rgba(0, 33, 64, 0.7);
  border-radius: 10px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border: 1px solid;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;
  height: 100%;
  transition: transform 0.3s cubic-bezier(0.34, 1.56, 0.64, 1), 
              box-shadow 0.3s ease, 
              border-color 0.3s ease;
  animation: cardAppear 0.6s backwards;
}

.data-card:nth-child(1) { animation-delay: 0.1s; }
.data-card:nth-child(2) { animation-delay: 0.2s; }
.data-card:nth-child(3) { animation-delay: 0.3s; }
.data-card:nth-child(4) { animation-delay: 0.4s; }
.data-card:nth-child(5) { animation-delay: 0.5s; }
.data-card:nth-child(6) { animation-delay: 0.6s; }

@keyframes cardAppear {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.data-card::before {
  content: '';
  position: absolute;
  top: -100%;
  left: -100%;
  width: 200%;
  height: 200%;
  background: radial-gradient(circle, rgba(255, 255, 255, 0.1) 0%, rgba(0, 0, 0, 0) 70%);
  transform: rotate(45deg);
  transition: all 0.5s ease;
}

.data-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.4);
  border-color: rgba(255, 255, 255, 0.4);
  z-index: 1;
}

.data-card:hover::before {
  top: -50%;
  left: -50%;
  opacity: 0.15;
}

.temperature {
  border-color: rgba(255, 99, 71, 0.6);
  background: linear-gradient(135deg, rgba(255, 99, 71, 0.2) 0%, rgba(0, 33, 64, 0.8) 100%);
}

.humidity {
  border-color: rgba(0, 123, 255, 0.6);
  background: linear-gradient(135deg, rgba(0, 123, 255, 0.2) 0%, rgba(0, 33, 64, 0.8) 100%);
}

.soil {
  border-color: rgba(111, 66, 193, 0.6);
  background: linear-gradient(135deg, rgba(111, 66, 193, 0.2) 0%, rgba(0, 33, 64, 0.8) 100%);
}

.light {
  border-color: rgba(255, 193, 7, 0.6);
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.2) 0%, rgba(0, 33, 64, 0.8) 100%);
}

.co2 {
  border-color: rgba(32, 201, 151, 0.6);
  background: linear-gradient(135deg, rgba(32, 201, 151, 0.2) 0%, rgba(0, 33, 64, 0.8) 100%);
}

.total {
  border-color: rgba(24, 144, 255, 0.6);
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.2) 0%, rgba(0, 33, 64, 0.8) 100%);
}

.card-title {
  font-size: 15px;
  color: #d9d9d9;
  z-index: 2;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.card-value {
  font-size: 32px;
  font-weight: bold;
  margin: 10px 0;
  z-index: 2;
  text-shadow: 0 0 10px rgba(255, 255, 255, 0.4);
  animation: pulseValue 2s infinite alternate ease-in-out;
  background: linear-gradient(90deg, #ffffff, #e6f7ff);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

@keyframes pulseValue {
  0% { text-shadow: 0 0 10px rgba(255, 255, 255, 0.2); }
  100% { text-shadow: 0 0 16px rgba(255, 255, 255, 0.5); }
}

.card-trend {
  font-size: 14px;
  color: #52c41a;
  z-index: 2;
  display: flex;
  align-items: center;
}

.trend-up {
  color: #52c41a;
  margin-right: 5px;
  font-size: 16px;
  animation: trendUp 1.5s infinite alternate;
}

@keyframes trendUp {
  0% { transform: translateY(0); }
  100% { transform: translateY(-3px); }
}

.trend-down {
  color: #f5222d;
  margin-right: 5px;
  font-size: 16px;
  animation: trendDown 1.5s infinite alternate;
}

@keyframes trendDown {
  0% { transform: translateY(0); }
  100% { transform: translateY(3px); }
}

.card-icon {
  position: absolute;
  bottom: 10px;
  right: 10px;
  font-size: 40px;
  opacity: 0.15;
  transform: rotate(-5deg);
  transition: all 0.3s ease;
}

.data-card:hover .card-icon {
  opacity: 0.25;
  transform: rotate(0deg) scale(1.1);
}

.realtime-data {
  overflow: hidden;
}

.data-stream {
  height: calc(100% - 30px);
  overflow-y: auto;
  padding-right: 5px;
  scrollbar-width: thin;
  scrollbar-color: #1890ff rgba(0, 21, 41, 0.5);
}

.data-item {
  padding: 10px 12px;
  margin-bottom: 8px;
  border-radius: 6px;
  font-size: 13px;
  border-left: 4px solid;
  background: rgba(0, 21, 41, 0.5);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  transition: all 0.3s ease;
  opacity: 0;
  animation: messageAppear 0.5s forwards;
  position: relative;
}

@keyframes messageAppear {
  from {
    opacity: 0;
    transform: translateX(-10px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.data-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(90deg, rgba(255, 255, 255, 0.03) 0%, rgba(255, 255, 255, 0) 100%);
  z-index: -1;
}

.data-item:hover {
  transform: translateX(5px);
  background: rgba(0, 21, 41, 0.7);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.data-item.system {
  border-left-color: #1890ff;
  box-shadow: 0 0 5px rgba(24, 144, 255, 0.3);
}

.data-item.error {
  border-left-color: #f5222d;
  box-shadow: 0 0 5px rgba(245, 34, 45, 0.3);
}

.data-item.data {
  border-left-color: #52c41a;
  box-shadow: 0 0 5px rgba(82, 196, 26, 0.3);
}

.time {
  color: #8c8c8c;
  margin-right: 10px;
  font-family: 'Courier New', monospace;
  font-size: 12px;
}

/* 自定义滚动条样式 */
::-webkit-scrollbar {
  width: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 21, 41, 0.5);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: linear-gradient(to bottom, #1890ff, #096dd9);
  border-radius: 3px;
  box-shadow: inset 0 0 3px rgba(255, 255, 255, 0.1);
}

::-webkit-scrollbar-thumb:hover {
  background: linear-gradient(to bottom, #40a9ff, #1890ff);
}

/* 响应式调整 */
@media (min-width: 1400px) {
  .bottom-row {
    flex-wrap: nowrap;
  }
}

@media (max-width: 1399px) {
  .bottom-row {
    flex-wrap: wrap;
    justify-content: space-between;
  }
  
  .chart-container.realtime-data,
  .chart-container.status-chart,
  .chart-container.data-analysis-percent,
  .chart-container.data-analysis-comparison,
  .chart-container.realtime-alerts {
    flex: 0 0 calc(50% - 8px);
    min-height: 250px;
    margin-bottom: 15px;
  }
}

@media (max-width: 768px) {
  .upper-row,
  .bottom-row {
    flex-direction: column;
  }

  .chart-container.realtime-data,
  .chart-container.status-chart,
  .chart-container.data-analysis-percent,
  .chart-container.data-analysis-comparison,
  .chart-container.realtime-alerts {
    flex: 0 0 100%;
  }
}

.back-button {
  margin-left: 20px;
}

.el-button {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}

.el-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.map-chart {
  width: 100%;
  height: calc(100% - 30px);
  min-height: 280px;
  flex: 1;
  position: relative;
  background: radial-gradient(circle at center, rgba(24, 144, 255, 0.05) 0%, rgba(0, 0, 0, 0) 70%);
}

.chart-container.map-container {
  flex: 1;
  background-color: #001529;
  min-height: 350px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.chart-container::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(24, 144, 255, 0.5), transparent);
}

/* 实时警报样式 */
.realtime-alerts {
  flex: 1;
  min-width: 300px;
  display: flex;
  flex-direction: column;
}

.alerts-wrapper {
  display: flex;
  flex-direction: column;
  height: calc(100% - 30px);
}

.alerts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.alert-stats {
  display: flex;
  gap: 15px;
}

.alert-stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 6px 10px;
  border-radius: 6px;
  background: rgba(0, 21, 41, 0.4);
  border: 1px solid;
}

.alert-stat-item.critical {
  border-color: rgba(245, 34, 45, 0.5);
}

.alert-stat-item.warning {
  border-color: rgba(250, 173, 20, 0.5);
}

.alert-stat-item.normal {
  border-color: rgba(82, 196, 26, 0.5);
}

.alert-count {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 2px;
}

.alert-label {
  font-size: 12px;
  opacity: 0.8;
}

.alert-stat-item.critical .alert-count {
  color: #f5222d;
  text-shadow: 0 0 8px rgba(245, 34, 45, 0.5);
}

.alert-stat-item.warning .alert-count {
  color: #faad14;
  text-shadow: 0 0 8px rgba(250, 173, 20, 0.5);
}

.alert-stat-item.normal .alert-count {
  color: #52c41a;
  text-shadow: 0 0 8px rgba(82, 196, 26, 0.5);
}

.alerts-container {
  flex: 1;
  overflow-y: auto;
  padding-right: 5px;
  scrollbar-width: thin;
  scrollbar-color: #1890ff rgba(0, 21, 41, 0.5);
}

.alert-item {
  display: flex;
  padding: 12px;
  margin-bottom: 10px;
  background: rgba(0, 21, 41, 0.5);
  border-left: 4px solid;
  border-radius: 6px;
  position: relative;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
}

.alert-item:hover {
  transform: translateX(5px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
}

.alert-item.critical {
  border-left-color: #f5222d;
  background: linear-gradient(90deg, rgba(245, 34, 45, 0.15), transparent);
}

.alert-item.warning {
  border-left-color: #faad14;
  background: linear-gradient(90deg, rgba(250, 173, 20, 0.15), transparent);
}

.alert-item.info {
  border-left-color: #1890ff;
  background: linear-gradient(90deg, rgba(24, 144, 255, 0.15), transparent);
}

.alert-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  margin-right: 12px;
  font-size: 18px;
}

.alert-item.critical .alert-icon {
  color: #f5222d;
  background: rgba(245, 34, 45, 0.1);
  border: 1px solid rgba(245, 34, 45, 0.3);
  box-shadow: 0 0 10px rgba(245, 34, 45, 0.2);
}

.alert-item.warning .alert-icon {
  color: #faad14;
  background: rgba(250, 173, 20, 0.1);
  border: 1px solid rgba(250, 173, 20, 0.3);
  box-shadow: 0 0 10px rgba(250, 173, 20, 0.2);
}

.alert-item.info .alert-icon {
  color: #1890ff;
  background: rgba(24, 144, 255, 0.1);
  border: 1px solid rgba(24, 144, 255, 0.3);
  box-shadow: 0 0 10px rgba(24, 144, 255, 0.2);
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.alert-item.critical .alert-title {
  color: #ff7875;
}

.alert-item.warning .alert-title {
  color: #ffc53d;
}

.alert-message {
  font-size: 12px;
  margin-bottom: 6px;
  opacity: 0.9;
}

.alert-meta {
  display: flex;
  font-size: 11px;
  opacity: 0.7;
}

.alert-sensor {
  margin-right: 10px;
}

.alert-value {
  display: flex;
  align-items: center;
  font-size: 16px;
  font-weight: bold;
  min-width: 80px;
  justify-content: flex-end;
}

.alert-value.increasing {
  color: #f5222d;
}

.alert-value.decreasing {
  color: #1890ff;
}

.alert-value i {
  margin-left: 4px;
}

.no-alerts {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: #52c41a;
  opacity: 0.8;
}

.no-alerts i {
  font-size: 48px;
  margin-bottom: 10px;
}

.refresh-button .el-button {
  background: rgba(24, 144, 255, 0.2);
  border-color: rgba(24, 144, 255, 0.3);
  color: #1890ff;
}

.refresh-button .el-button:hover {
  background: rgba(24, 144, 255, 0.3);
  border-color: rgba(24, 144, 255, 0.5);
}
</style> 