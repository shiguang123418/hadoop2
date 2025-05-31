<template>
  <div class="agriculture-dashboard">
    <header class="dashboard-header">
      <h1 class="title">农业传感器大数据可视化监控平台</h1>
      <div class="connection-status">
        <span :class="['status-indicator', isConnected ? 'connected' : 'disconnected']"></span>
        {{ isConnected ? '已连接' : '未连接' }}
        <el-button v-if="!isConnected" type="primary" size="small" @click="connectWebSocket">连接</el-button>
        <el-button v-else type="danger" size="small" @click="disconnectWebSocket">断开</el-button>
        <el-button type="success" size="small" @click="sendTestData" :disabled="!isConnected">发送测试数据</el-button>
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
            // 根据传感器类型分布更新地图数据
            const mapData = [
              { name: '北京', value: 320 },
              { name: '天津', value: 280 },
              { name: '上海', value: 350 },
              { name: '重庆', value: 270 },
              { name: '河北', value: 310 },
              { name: '山西', value: 250 },
              { name: '江苏', value: 380 },
              { name: '浙江', value: 420 },
              { name: '广东', value: 370 },
              { name: '四川', value: 290 },
              { name: '山东', value: 340 },
              { name: '河南', value: 320 },
              { name: '湖北', value: 280 },
              { name: '陕西', value: 240 },
              { name: '安徽', value: 310 },
              { name: '福建', value: 280 },
              { name: '云南', value: 260 },
              { name: '黑龙江', value: 230 },
              { name: '湖南', value: 300 },
              { name: '辽宁', value: 280 },
              { name: '吉林', value: 260 },
              { name: '江西', value: 270 },
              { name: '广西', value: 250 },
              { name: '海南', value: 210 },
              { name: '贵州', value: 240 },
              { name: '内蒙古', value: 280 },
              { name: '甘肃', value: 220 },
              { name: '青海', value: 190 },
              { name: '新疆', value: 270 },
              { name: '西藏', value: 160 },
              { name: '宁夏', value: 180 },
              { name: '台湾', value: 250 },
              { name: '香港', value: 240 },
              { name: '澳门', value: 180 }
            ];
            
            mapChartInstance.setOption({
              series: [{
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
    
    // 发送测试数据
    const sendTestData = async () => {
      if (!isConnected.value) {
        addMessage('error', '请先连接WebSocket');
        return;
      }
      
      try {
        console.log('AgricultureDataDashboard: 开始发送测试数据...');
        const response = await SensorApi.sendTestData();
        console.log('测试数据发送响应:', response);
        
        if (response.data && response.data.status === 'success') {
          addMessage('system', `测试数据已发送: ${response.data.message || 'success'}`);
        } else {
          throw new Error(response.data?.message || '未知错误');
        }
      } catch (error) {
        console.error('发送测试数据失败:', error);
        addMessage('error', `发送测试数据失败: ${error.message}`);
        
        // 如果遇到网络错误可能是WebSocket断开，尝试重新连接
        if (error.message.includes('Network Error') && !isConnected.value) {
          addMessage('system', '正在尝试重新连接WebSocket...');
          await connectWebSocket();
        }
      }
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
        
        // 设置地图配置
        const option = {
          backgroundColor: 'transparent',
          tooltip: {
            trigger: 'item',
            formatter: '{b}<br/>数据量: {c}',
            backgroundColor: 'rgba(0, 21, 41, 0.8)',
            borderColor: '#003a8c',
            textStyle: { color: '#fff' }
          },
          visualMap: {
            show: true,
            min: 0,
            max: 500,
            left: 'left',
            bottom: 20,
            text: ['高', '低'],
            calculable: true,
            inRange: { 
              color: ['#0e2a47', '#1a91ff', '#52c41a', '#fa8c16', '#eb2f96'] 
            },
            textStyle: { color: '#fff' }
          },
          series: [{
            name: '传感器分布',
            type: 'map',
            map: 'china',
            roam: true,
            zoom: 1.0,
            aspectScale: 0.8,
            layoutCenter: ['50%', '50%'],
            layoutSize: '100%',
            label: {
              show: true,
              color: '#fff',
              fontSize: 10,
              fontWeight: 'bold'
            },
            itemStyle: {
              areaColor: '#0e2a47',
              borderColor: '#1a91ff',
              borderWidth: 1,
              shadowColor: 'rgba(24, 144, 255, 0.3)',
              shadowBlur: 10
            },
            emphasis: {
              label: {
                show: true,
                color: '#fff',
                fontSize: 12
              },
              itemStyle: {
                areaColor: '#1890ff',
                borderColor: '#fff',
                borderWidth: 1,
                shadowColor: 'rgba(24, 144, 255, 0.7)',
                shadowBlur: 20
              }
            },
            data: [
              { name: '北京', value: 320 },
              { name: '天津', value: 280 },
              { name: '上海', value: 350 },
              { name: '重庆', value: 270 },
              { name: '河北', value: 310 },
              { name: '山西', value: 250 },
              { name: '江苏', value: 380 },
              { name: '浙江', value: 420 },
              { name: '广东', value: 370 },
              { name: '四川', value: 290 },
              { name: '山东', value: 340 },
              { name: '河南', value: 320 },
              { name: '湖北', value: 280 },
              { name: '陕西', value: 240 },
              { name: '安徽', value: 310 },
              { name: '福建', value: 280 },
              { name: '云南', value: 260 },
              { name: '黑龙江', value: 230 },
              { name: '湖南', value: 300 },
              { name: '辽宁', value: 280 },
              { name: '吉林', value: 260 },
              { name: '江西', value: 270 },
              { name: '广西', value: 250 },
              { name: '海南', value: 210 },
              { name: '贵州', value: 240 },
              { name: '内蒙古', value: 280 },
              { name: '甘肃', value: 220 },
              { name: '青海', value: 190 },
              { name: '新疆', value: 270 },
              { name: '西藏', value: 160 },
              { name: '宁夏', value: 180 },
              { name: '台湾', value: 250 },
              { name: '香港', value: 240 },
              { name: '澳门', value: 180 }
            ]
          }]
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
      sendTestData,
      goBack
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
  background-color: #001529;
  color: #fff;
  overflow: hidden;
  font-family: 'Arial', sans-serif;
  z-index: 9999; /* 确保在最上层 */
  display: flex;
  flex-direction: column;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  height: 60px;
  background: linear-gradient(90deg, rgba(0, 33, 64, 0.8) 0%, rgba(0, 82, 204, 0.8) 100%);
  border-bottom: 1px solid #003a8c;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}

.title {
  font-size: 24px;
  color: #fff;
  text-shadow: 0 0 10px rgba(24, 144, 255, 0.8);
  margin: 0;
  letter-spacing: 1px;
}

.connection-status {
  display: flex;
  align-items: center;
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 5px;
}

.connected {
  background-color: #52c41a;
  box-shadow: 0 0 8px #52c41a;
}

.disconnected {
  background-color: #f5222d;
  box-shadow: 0 0 8px #f5222d;
}

.time-display {
  font-size: 18px;
  font-family: 'Courier New', monospace;
  color: #1890ff;
  text-shadow: 0 0 5px rgba(24, 144, 255, 0.5);
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px;
  height: calc(100vh - 60px);
  overflow: hidden;
}

.dashboard-summary {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
  min-height: 110px;
  max-height: 120px;
}

.charts-row {
  display: flex;
  gap: 15px;
  margin-bottom: 15px;
}

.upper-row {
  height: 340px;
}

.bottom-row {
  height: 340px;
  flex-wrap: wrap;
}

.chart-container {
  position: relative;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
}

.chart-container h3 {
  margin-top: 0;
  margin-bottom: 10px;
  font-size: 16px;
  font-weight: 500;
  color: #e6f7ff;
}

.chart-container .chart {
  flex: 1;
  width: 100%;
  min-height: 200px;
}

.chart-container.distribution-chart,
.chart-container.map-container,
.chart-container.anomaly-chart {
  flex: 1;
  background-color: #001529;
}

.chart-container.realtime-data {
  flex: 1;
  background-color: #001529;
  min-width: 300px;
}

.chart-container.data-analysis-percent,
.chart-container.data-analysis-comparison {
  flex: 1;
  min-width: 300px;
  min-height: 280px;
  background-color: #001529;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.chart-container.status-chart {
  flex: 1;
  min-width: 300px;
  background-color: #001529;
}

.data-card {
  background: rgba(0, 33, 64, 0.7);
  border-radius: 8px;
  padding: 15px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  border: 1px solid;
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.3);
  position: relative;
  overflow: hidden;
  height: 100%;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.data-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.4);
}

.temperature {
  border-color: #ff6347;
  background: linear-gradient(135deg, rgba(255, 99, 71, 0.2) 0%, rgba(0, 33, 64, 0.7) 100%);
}

.humidity {
  border-color: #007bff;
  background: linear-gradient(135deg, rgba(0, 123, 255, 0.2) 0%, rgba(0, 33, 64, 0.7) 100%);
}

.soil {
  border-color: #6f42c1;
  background: linear-gradient(135deg, rgba(111, 66, 193, 0.2) 0%, rgba(0, 33, 64, 0.7) 100%);
}

.light {
  border-color: #ffc107;
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.2) 0%, rgba(0, 33, 64, 0.7) 100%);
}

.co2 {
  border-color: #20c997;
  background: linear-gradient(135deg, rgba(32, 201, 151, 0.2) 0%, rgba(0, 33, 64, 0.7) 100%);
}

.total {
  border-color: #1890ff;
  background: linear-gradient(135deg, rgba(24, 144, 255, 0.2) 0%, rgba(0, 33, 64, 0.7) 100%);
}

.card-title {
  font-size: 16px;
  color: #d9d9d9;
  z-index: 2;
}

.card-value {
  font-size: 32px;
  font-weight: bold;
  margin: 10px 0;
  z-index: 2;
  text-shadow: 0 0 8px rgba(255, 255, 255, 0.3);
}

.card-trend {
  font-size: 14px;
  color: #52c41a;
  z-index: 2;
}

.trend-up {
  color: #52c41a;
  margin-right: 5px;
}

.trend-down {
  color: #f5222d;
  margin-right: 5px;
}

.card-icon {
  position: absolute;
  bottom: 10px;
  right: 10px;
  font-size: 40px;
  opacity: 0.2;
}

.realtime-data {
  overflow: hidden;
}

.data-stream {
  height: calc(100% - 30px);
  overflow-y: auto;
  padding-right: 5px;
}

.data-item {
  padding: 8px 10px;
  margin-bottom: 6px;
  border-radius: 4px;
  font-size: 12px;
  border-left: 3px solid;
  background: rgba(0, 21, 41, 0.5);
  transition: all 0.3s ease;
}

.data-item:hover {
  transform: translateX(3px);
  background: rgba(0, 21, 41, 0.7);
}

.data-item.system {
  border-left-color: #1890ff;
}

.data-item.error {
  border-left-color: #f5222d;
}

.data-item.data {
  border-left-color: #52c41a;
}

.time {
  color: #8c8c8c;
  margin-right: 10px;
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
  background: #1890ff;
  border-radius: 3px;
}

::-webkit-scrollbar-thumb:hover {
  background: #40a9ff;
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
  .chart-container.data-analysis-comparison {
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
  .chart-container.data-analysis-comparison {
    flex: 0 0 100%;
  }
}

.back-button {
  margin-left: 20px;
}

.data-analysis {
  position: relative;
  overflow: hidden;
}

.data-analysis::before {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 80px;
  height: 80px;
  background: radial-gradient(circle, rgba(24, 144, 255, 0.1) 0%, rgba(0, 33, 64, 0) 70%);
  z-index: 0;
}

.map-chart {
  width: 100%;
  height: calc(100% - 30px);
  min-height: 280px;
  flex: 1;
  position: relative;
}

.chart-container.map-container {
  flex: 1;
  background-color: #001529;
  min-height: 350px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.map-chart {
  width: 100%;
  height: calc(100% - 30px);
  min-height: 320px;
  flex: 1;
  position: relative;
}
</style> 