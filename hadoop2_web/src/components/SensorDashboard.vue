<template>
  <div class="sensor-dashboard">
    <div class="dashboard-header">
      <h1>农业传感器实时监控</h1>
      <div class="connection-status" :class="{ connected: isConnected }">
        WebSocket: {{ isConnected ? '已连接' : '未连接' }}
      </div>
    </div>

    <div class="dashboard-filters">
      <el-select v-model="selectedRegion" placeholder="选择区域" clearable @change="applyFilters">
        <el-option v-for="region in regions" :key="region" :label="region" :value="region"></el-option>
      </el-select>
      
      <el-select v-model="selectedSensorType" placeholder="传感器类型" clearable @change="applyFilters">
        <el-option v-for="type in sensorTypes" :key="type" :label="getSensorTypeName(type)" :value="type"></el-option>
      </el-select>
      
      <el-select v-model="selectedCropType" placeholder="作物类型" clearable @change="applyFilters">
        <el-option v-for="crop in cropTypes" :key="crop" :label="crop" :value="crop"></el-option>
      </el-select>
      
      <el-switch v-model="showOnlyAnomalies" active-text="仅显示异常" @change="applyFilters"></el-switch>
    </div>

    <div class="dashboard-stats">
      <el-card class="stat-card">
        <div class="stat-value">{{ stats.sensorCount }}</div>
        <div class="stat-title">传感器总数</div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-value">{{ stats.messageCount }}</div>
        <div class="stat-title">接收消息总数</div>
      </el-card>
      
      <el-card class="stat-card" :class="{'anomaly-card': stats.anomalyRate > 0.2}">
        <div class="stat-value">{{ (stats.anomalyRate * 100).toFixed(1) }}%</div>
        <div class="stat-title">异常率</div>
      </el-card>
      
      <el-card class="stat-card">
        <div class="stat-value">{{ stats.lastUpdate }}</div>
        <div class="stat-title">最后更新</div>
      </el-card>
    </div>

    <div class="dashboard-content">
      <div class="chart-container">
        <el-card class="chart-card">
          <div slot="header">实时传感器数据趋势</div>
          <div id="trend-chart" style="height: 300px;"></div>
        </el-card>
        
        <el-card class="chart-card">
          <div slot="header">异常值分布</div>
          <div id="anomaly-chart" style="height: 300px;"></div>
        </el-card>
      </div>
      
      <div class="map-and-table">
        <el-card class="region-map-card">
          <div slot="header">区域分布</div>
          <div id="region-map" style="height: 300px;"></div>
        </el-card>
        
        <el-card class="data-table-card">
          <div slot="header">
            最新传感器数据
            <el-button size="mini" type="primary" @click="clearTable">清空</el-button>
          </div>
          <el-table :data="latestData" height="250" border style="width: 100%" v-loading="!latestData.length">
            <el-table-column prop="sensorId" label="传感器ID" width="100"></el-table-column>
            <el-table-column prop="sensorType" label="类型" width="100">
              <template slot-scope="scope">
                {{ getSensorTypeName(scope.row.sensorType) }}
              </template>
            </el-table-column>
            <el-table-column prop="value" label="数值" width="80">
              <template slot-scope="scope">
                <span :class="{'anomaly-value': scope.row.isAnomaly}">
                  {{ Number(scope.row.value).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="unit" label="单位" width="60">
              <template slot-scope="scope">
                {{ scope.row.unit || '-' }}
              </template>
            </el-table-column>
            <el-table-column prop="region" label="区域" width="80">
              <template slot-scope="scope">
                <el-tag type="info" size="small">
                  {{ scope.row.region === 'Field' || scope.row.region === 'undefined' || !scope.row.region ? '未知区域' : scope.row.region }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="location" label="位置" width="120">
              <template slot-scope="scope">
                {{ scope.row.location || '未知位置' }}
              </template>
            </el-table-column>
            <el-table-column prop="cropType" label="作物" width="80">
              <template slot-scope="scope">
                <el-tag type="success" size="small">
                  {{ scope.row.cropType || '农作物' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="timestamp" label="时间" width="100"></el-table-column>
            <el-table-column prop="isAnomaly" label="状态" width="70" fixed="right">
              <template slot-scope="scope">
                <el-tag :type="scope.row.isAnomaly ? 'danger' : 'success'">
                  {{ scope.row.isAnomaly ? '异常' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" fixed="right">
              <template slot-scope="scope">
                <el-button 
                  type="text" 
                  size="small" 
                  @click="showSensorDetail(scope.row)"
                >详情</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </div>
    
    <!-- 传感器详情对话框 -->
    <el-dialog title="传感器详情" :visible.sync="sensorDetailVisible" width="30%">
      <div v-if="selectedSensor">
        <el-descriptions bordered :column="1">
          <el-descriptions-item label="传感器ID">{{ selectedSensor.sensorId }}</el-descriptions-item>
          <el-descriptions-item label="传感器类型">{{ getSensorTypeName(selectedSensor.sensorType) }}</el-descriptions-item>
          <el-descriptions-item label="当前数值">
            <span :class="{'anomaly-value': selectedSensor.isAnomaly}">
              {{ selectedSensor.value }} {{ selectedSensor.unit }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="selectedSensor.isAnomaly ? 'danger' : 'success'">
              {{ selectedSensor.isAnomaly ? '异常' : '正常' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="区域">{{ selectedSensor.region }}</el-descriptions-item>
          <el-descriptions-item label="详细位置">{{ selectedSensor.location }}</el-descriptions-item>
          <el-descriptions-item label="作物类型">{{ selectedSensor.cropType }}</el-descriptions-item>
          <el-descriptions-item label="上报时间">{{ selectedSensor.timestamp }}</el-descriptions-item>
        </el-descriptions>
        
        <div class="sensor-detail-actions">
          <el-button type="primary" size="small" @click="sensorDetailVisible = false">关闭</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import { formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';
import wsConfig from '@/config/websocket.config';

export default {
  name: 'SensorDashboard',
  data() {
    return {
      // WebSocket连接
      socket: null,
      isConnected: false,
      
      // 图表实例
      trendChart: null,
      anomalyChart: null,
      regionMap: null,
      
      // 数据存储
      sensorData: [],
      latestData: [],
      
      // 过滤器
      selectedRegion: '',
      selectedSensorType: '',
      selectedCropType: '',
      showOnlyAnomalies: false,
      
      // 选项
      regions: [],
      sensorTypes: [],
      cropTypes: [],
      
      // 统计数据
      stats: {
        sensorCount: 0,
        messageCount: 0,
        anomalyRate: 0,
        lastUpdate: '-'
      },
      
      // 图表数据
      chartData: {
        timestamps: [],
        values: {},
        anomalies: {}
      },
      
      // 地图数据
      mapData: {},
      
      // 最大数据点数
      maxDataPoints: 100,
      
      // WebSocket配置
      wsUrl: wsConfig.wsUrl,
      
      // 传感器详情对话框
      sensorDetailVisible: false,
      selectedSensor: null
    };
  },
  mounted() {
    this.initCharts();
    this.connectWebSocket();
    
    // 监听窗口大小变化，调整图表大小
    window.addEventListener('resize', this.resizeCharts);
    
    // 打印配置信息，用于调试
    console.log('WebSocket URL:', this.wsUrl);
    console.log('初始化完成');
    
    // 5秒后检查数据状态
    setTimeout(() => {
      console.log('数据检查 - 传感器数据数量:', this.sensorData.length);
      console.log('数据检查 - 表格数据数量:', this.latestData.length);
      console.log('数据检查 - 区域列表:', this.regions);
    }, 5000);
  },
  beforeDestroy() {
    this.disconnectWebSocket();
    if (this.trendChart) this.trendChart.dispose();
    if (this.anomalyChart) this.anomalyChart.dispose();
    if (this.regionMap) this.regionMap.dispose();
    window.removeEventListener('resize', this.resizeCharts);
  },
  methods: {
    // WebSocket相关方法
    connectWebSocket() {
      console.log(`正在连接WebSocket: ${this.wsUrl}`);
      this.socket = new WebSocket(this.wsUrl);
      
      this.socket.onopen = () => {
        this.isConnected = true;
        console.log('WebSocket连接已建立');
      };
      
      this.socket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data);
          this.handleWebSocketMessage(message);
        } catch (error) {
          console.error('解析WebSocket消息失败:', error);
          console.log('原始消息:', event.data);
        }
      };
      
      this.socket.onerror = (error) => {
        console.error('WebSocket错误:', error);
        this.isConnected = false;
      };
      
      this.socket.onclose = () => {
        console.log('WebSocket连接已关闭');
        this.isConnected = false;
        
        // 尝试在5秒后重新连接
        setTimeout(() => {
          this.connectWebSocket();
        }, 5000);
      };
    },
    
    disconnectWebSocket() {
      if (this.socket) {
        this.socket.close();
        this.socket = null;
      }
    },
    
    handleWebSocketMessage(message) {
      // 如果消息是字符串，尝试再次解析
      if (typeof message === 'string') {
        try {
          message = JSON.parse(message);
        } catch (error) {
          console.error('无法解析WebSocket字符串消息:', error);
          return;
        }
      }
      
      console.log('收到WebSocket消息:', message);
      
      try {
        // 处理直接发送的传感器数据格式
        if (message.sensorId) {
          console.log('处理单个传感器数据:', message);
          
          // 创建多个对应不同传感器类型的数据项
          const normalizedData = [];
          
          // 处理温度数据
          if ('temperature' in message) {
            normalizedData.push({
              sensorId: message.sensorId,
              timestamp: new Date(message.timestamp).toLocaleString(),
              sensorType: 'temperature',
              value: message.temperature,
              unit: message.temperatureUnit || '°C',
              location: message.location || '',
              region: message.region || (message.location ? message.location.split('-')[0] : '未知区域'),
              cropType: message.cropType || '农作物',
              isAnomaly: message.temperatureAnomaly || message.isAnomalyDetected || false
            });
          }
          
          // 处理湿度数据
          if ('humidity' in message) {
            normalizedData.push({
              sensorId: message.sensorId,
              timestamp: new Date(message.timestamp).toLocaleString(),
              sensorType: 'humidity',
              value: message.humidity,
              unit: message.humidityUnit || '%',
              location: message.location || '',
              region: message.region || (message.location ? message.location.split('-')[0] : '未知区域'),
              cropType: message.cropType || '农作物',
              isAnomaly: message.humidityAnomaly || message.isAnomalyDetected || false
            });
          }
          
          // 处理土壤湿度数据
          if ('soilMoisture' in message) {
            normalizedData.push({
              sensorId: message.sensorId,
              timestamp: new Date(message.timestamp).toLocaleString(),
              sensorType: 'soilMoisture',
              value: message.soilMoisture,
              unit: message.soilMoistureUnit || '%',
              location: message.location || '',
              region: message.region || (message.location ? message.location.split('-')[0] : '未知区域'),
              cropType: message.cropType || '农作物',
              isAnomaly: message.soilMoistureAnomaly || message.isAnomalyDetected || false
            });
          }
          
          // 处理光照强度数据
          if ('lightIntensity' in message) {
            normalizedData.push({
              sensorId: message.sensorId,
              timestamp: new Date(message.timestamp).toLocaleString(),
              sensorType: 'lightIntensity',
              value: message.lightIntensity,
              unit: message.lightIntensityUnit || 'lux',
              location: message.location || '',
              region: message.region || (message.location ? message.location.split('-')[0] : '未知区域'),
              cropType: message.cropType || '农作物',
              isAnomaly: message.lightIntensityAnomaly || message.isAnomalyDetected || false
            });
          }
          
          // 处理CO2浓度数据
          if ('co2Level' in message) {
            normalizedData.push({
              sensorId: message.sensorId,
              timestamp: new Date(message.timestamp).toLocaleString(),
              sensorType: 'co2Level',
              value: message.co2Level,
              unit: message.co2LevelUnit || 'ppm',
              location: message.location || '',
              region: message.region || (message.location ? message.location.split('-')[0] : '未知区域'),
              cropType: message.cropType || '农作物',
              isAnomaly: message.co2LevelAnomaly || message.isAnomalyDetected || false
            });
          }
          
          // 处理电池电量数据
          if ('batteryLevel' in message) {
            normalizedData.push({
              sensorId: message.sensorId,
              timestamp: new Date(message.timestamp).toLocaleString(),
              sensorType: 'batteryLevel',
              value: message.batteryLevel,
              unit: message.batteryLevelUnit || '%',
              location: message.location || '',
              region: message.region || (message.location ? message.location.split('-')[0] : '未知区域'),
              cropType: message.cropType || '农作物',
              isAnomaly: message.batteryLevelAnomaly || (message.batteryLevel < 20) || message.isAnomalyDetected || false
            });
          }
          
          if (normalizedData.length > 0) {
            console.log('处理后的数据:', normalizedData);
            this.processSensorData(normalizedData);
          } else {
            console.warn('传感器数据处理结果为空:', message);
          }
          return;
        }
        
        // 处理欢迎消息
        if (message.type === 'welcome') {
          console.log('收到欢迎消息');
          return;
        }
        
        // 处理异常消息
        if (message.type === 'anomaly') {
          console.log('收到异常数据:', message);
          return;
        }
        
        // 处理包装在data字段中的传感器数据
        if (message.type === 'sensor_data' && Array.isArray(message.data)) {
          console.log('收到传感器数据数组:', message.data.length);
          const normalizedData = [];
          message.data.forEach(item => {
            const processed = this.normalizeSensorData(item);
            if (processed && processed.length > 0) {
              normalizedData.push(...processed);
            }
          });
          
          if (normalizedData.length > 0) {
            this.processSensorData(normalizedData);
          } else {
            console.warn('数组数据处理结果为空:', message);
          }
          return;
        }
        
        // 处理直接发送的数组数据
        if (Array.isArray(message)) {
          console.log('直接收到数组数据:', message.length);
          const normalizedData = [];
          message.forEach(item => {
            const processed = this.normalizeSensorData(item);
            if (processed && processed.length > 0) {
              normalizedData.push(...processed);
            }
          });
          
          if (normalizedData.length > 0) {
            this.processSensorData(normalizedData);
          } else {
            console.warn('数组数据处理结果为空:', message);
          }
          return;
        }
        
        console.warn('未知消息格式:', message);
      } catch (error) {
        console.error('处理WebSocket消息时出错:', error);
      }
    },
    
    // 标准化传感器数据，处理不同的字段命名
    normalizeSensorData(data) {
      // 如果数据为空或无效，返回空数组
      if (!data || !data.sensorId) {
        console.warn('无效的传感器数据:', data);
        return [];
      }

      // 添加区域信息，优先使用数据自带的region字段
      const region = data.region || (data.location ? data.location.split('-')[0] : '未知区域');
      
      // 确定传感器类型和对应的值
      // 温度、湿度等数据都是单独的字段，需要创建多条数据记录
      const result = [];
      
      // 定义传感器类型配置
      const sensorTypeConfigs = {
        'temperature': {
          unit: data.temperatureUnit || '°C',
          anomalyThresholds: [10, 35]  // 低于10或高于35视为异常
        },
        'humidity': {
          unit: data.humidityUnit || '%',
          anomalyThresholds: [30, 80]
        },
        'soilMoisture': {
          unit: data.soilMoistureUnit || '%',
          anomalyThresholds: [15, 60]
        },
        'lightIntensity': {
          unit: data.lightIntensityUnit || 'lux',
          anomalyThresholds: [300, 900]
        },
        'co2Level': {
          unit: data.co2LevelUnit || 'ppm',
          anomalyThresholds: [350, 800]
        },
        'batteryLevel': {
          unit: data.batteryLevelUnit || '%',
          anomalyThresholds: [20, 100]  // 低于20%视为异常
        }
      };
      
      // 统一处理所有传感器类型数据
      Object.keys(sensorTypeConfigs).forEach(sensorType => {
        // 检查数据中是否包含此类型
        if (sensorType in data) {
          const config = sensorTypeConfigs[sensorType];
          // 使用直接提供的异常标记，如果没有则根据阈值判断
          const isAnomaly = data[sensorType + 'Anomaly'] !== undefined 
                          ? data[sensorType + 'Anomaly'] 
                          : data.isAnomalyDetected || 
                            (data[sensorType] < config.anomalyThresholds[0] || 
                             data[sensorType] > config.anomalyThresholds[1]);
          
          result.push({
            sensorId: data.sensorId,
            timestamp: new Date(data.timestamp).toLocaleString(),
            sensorType: sensorType,
            value: data[sensorType],
            unit: config.unit,
            location: data.location || '',
            region: region,
            cropType: data.cropType || '农作物',
            isAnomaly: isAnomaly,
            details: `${region}-${data.sensorId}`
          });
        }
      });
      
      return result;
    },
    
    processSensorData(data) {
      console.log('处理数据:', data);
      
      // 确保data是数组
      if (!Array.isArray(data)) {
        console.error('processSensorData: 数据不是数组');
        return;
      }
      
      // 处理扁平化的数据数组
      let flatData = [];
      data.forEach(item => {
        if (Array.isArray(item)) {
          flatData = flatData.concat(item);
        } else {
          flatData.push(item);
        }
      });
      
      // 确保扁平化后有数据
      if (flatData.length === 0) {
        console.warn('处理后没有数据');
        return;
      }
      
      console.log('处理后的扁平数据:', flatData);
      
      // 更新统计信息
      this.stats.messageCount += flatData.length;
      this.stats.lastUpdate = new Date().toLocaleTimeString();
      
      // 处理每条数据
      flatData.forEach(item => {
        // 确保数据有sensorId和sensorType字段
        if (!item.sensorId || !item.sensorType) {
          console.warn('缺少必要字段:', item);
          return;
        }
        
        // 添加到传感器数据数组
        this.sensorData.push(item);
        
        // 限制数组大小
        if (this.sensorData.length > 1000) {
          this.sensorData.shift();
        }
        
        // 更新区域、传感器类型和作物类型列表
        if (item.region && item.region !== 'undefined' && item.region !== 'Field' &&
            !this.regions.includes(item.region)) {
          console.log('添加新区域:', item.region);
          this.regions.push(item.region);
        }
        
        if (item.sensorType && !this.sensorTypes.includes(item.sensorType)) {
          this.sensorTypes.push(item.sensorType);
        }
        
        if (item.cropType && !this.cropTypes.includes(item.cropType)) {
          this.cropTypes.push(item.cropType);
        }
        
        // 更新图表数据
        this.updateChartData(item);
      });
      
      console.log('当前区域列表:', this.regions);
      
      // 将最新处理的数据直接添加到表格数据
      const newData = flatData.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
      
      // 将新数据并入latestData，保持最新的数据在前面
      this.latestData = [...newData, ...this.latestData].slice(0, 100);
      console.log('更新后的表格数据:', this.latestData.length);
      
      // 应用过滤器更新显示数据
      this.applyFilters();
      
      // 更新图表
      this.updateCharts();
      
      // 计算异常率
      this.calculateAnomalyRate();
      
      // 更新传感器总数
      this.updateSensorCount();
    },
    
    updateChartData(item) {
      const timestamp = new Date();
      const timeStr = timestamp.toLocaleTimeString();
      
      // 确保时间戳数组不会过长
      if (!this.chartData.timestamps.includes(timeStr)) {
        this.chartData.timestamps.push(timeStr);
        if (this.chartData.timestamps.length > this.maxDataPoints) {
          this.chartData.timestamps.shift();
        }
      }
      
      // 为每种传感器类型创建数据系列
      if (!this.chartData.values[item.sensorType]) {
        this.chartData.values[item.sensorType] = [];
      }
      
      // 添加数据点
      this.chartData.values[item.sensorType].push({
        time: timeStr,
        value: item.value,
        isAnomaly: item.isAnomaly
      });
      
      // 限制每个系列的数据点数量
      if (this.chartData.values[item.sensorType].length > this.maxDataPoints) {
        this.chartData.values[item.sensorType].shift();
      }
      
      // 按区域收集异常数据
      if (item.region) {
        if (!this.chartData.anomalies[item.region]) {
          this.chartData.anomalies[item.region] = 0;
        }
        
        if (item.isAnomaly) {
          this.chartData.anomalies[item.region]++;
        }
      }
    },
    
    // 初始化图表
    initCharts() {
      // 趋势图
      this.trendChart = echarts.init(document.getElementById('trend-chart'));
      
      // 异常分布图
      this.anomalyChart = echarts.init(document.getElementById('anomaly-chart'));
      
      // 区域地图
      this.regionMap = echarts.init(document.getElementById('region-map'));
      
      // 设置初始配置
      this.updateCharts();
    },
    
    // 更新图表
    updateCharts() {
      this.updateTrendChart();
      this.updateAnomalyChart();
      this.updateRegionMap();
    },
    
    // 更新趋势图
    updateTrendChart() {
      if (!this.trendChart) return;
      
      const series = [];
      const colors = {
        'temperature': '#c23531',
        'humidity': '#2f4554',
        'soilMoisture': '#61a0a8',
        'lightIntensity': '#d48265',
        'batteryLevel': '#91c7ae'
      };
      
      // 为每种传感器类型创建一个系列
      Object.keys(this.chartData.values).forEach((sensorType, index) => {
        // 如果有过滤器，只显示选中的传感器类型
        if (this.selectedSensorType && this.selectedSensorType !== sensorType) {
          return;
        }
        
        const data = [];
        this.chartData.timestamps.forEach(time => {
          const point = this.chartData.values[sensorType].find(p => p.time === time);
          if (point) {
            // 如果显示异常值，将正常值设为null
            if (this.showOnlyAnomalies && !point.isAnomaly) {
              data.push([time, null]);
            } else {
              data.push([time, point.value]);
            }
          } else {
            data.push([time, null]);
          }
        });
        
        series.push({
          name: this.getSensorTypeName(sensorType),
          type: 'line',
          smooth: true,
          symbol: 'circle',
          symbolSize: 5,
          sampling: 'average',
          itemStyle: {
            color: colors[sensorType] || echarts.color[index % echarts.color.length]
          },
          areaStyle: {
            opacity: 0.1
          },
          data: data
        });
      });
      
      const option = {
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = params[0].axisValue + '<br/>';
            params.forEach(param => {
              if (param.value[1] !== null) {
                result += param.marker + param.seriesName + ': ' + param.value[1] + '<br/>';
              }
            });
            return result;
          }
        },
        legend: {
          data: Object.keys(this.chartData.values).map(type => this.getSensorTypeName(type)),
          selected: {}
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: this.chartData.timestamps
        },
        yAxis: {
          type: 'value',
          scale: true
        },
        series: series
      };
      
      this.trendChart.setOption(option, true);
    },
    
    // 获取传感器类型的显示名称
    getSensorTypeName(type) {
      const nameMap = {
        'temperature': '温度',
        'humidity': '湿度',
        'soilMoisture': '土壤湿度',
        'lightIntensity': '光照强度',
        'co2Level': 'CO2浓度',
        'batteryLevel': '电池电量'
      };
      
      return nameMap[type] || type;
    },
    
    // 更新异常分布图
    updateAnomalyChart() {
      if (!this.anomalyChart) return;
      
      // 计算正常和异常数据的总数
      const totalData = this.sensorData.length;
      const anomalyCount = this.sensorData.filter(item => item.isAnomaly).length;
      const normalCount = totalData - anomalyCount;
      
      const data = [
        { value: normalCount, name: '正常数据', itemStyle: { color: '#91c7ae' } },
        { value: anomalyCount, name: '异常数据', itemStyle: { color: '#c23531' } }
      ];
      
      // 按区域分类的异常数据
      const regionData = [];
      this.regions.forEach(region => {
        const sensorsInRegion = this.sensorData.filter(s => s.region === region);
        const anomaliesInRegion = sensorsInRegion.filter(s => s.isAnomaly).length;
        
        if (anomaliesInRegion > 0) {
          regionData.push({
            value: anomaliesInRegion,
            name: `${region} 异常`,
            itemStyle: { color: this.getColorByAnomalyRate(anomaliesInRegion / sensorsInRegion.length) }
          });
        }
      });
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          type: 'scroll',
          orient: 'vertical',
          right: 10,
          top: 20,
          bottom: 20,
          data: ['正常数据', '异常数据'].concat(regionData.map(item => item.name))
        },
        series: [
          {
            name: '数据状态',
            type: 'pie',
            radius: ['0%', '40%'],
            center: ['40%', '50%'],
            selectedMode: 'single',
            label: {
              position: 'inner',
              fontSize: 14
            },
            data: data
          },
          {
            name: '区域异常分布',
            type: 'pie',
            radius: ['50%', '65%'],
            center: ['40%', '50%'],
            labelLine: {
              length: 30
            },
            label: {
              formatter: '{b}: {c} ({d}%)',
              fontSize: 12
            },
            data: regionData
          }
        ]
      };
      
      this.anomalyChart.setOption(option);
    },
    
    // 更新区域地图
    updateRegionMap() {
      if (!this.regionMap) return;
      
      // 处理不同区域数据
      const regionData = [];
      
      // 首先过滤有效的区域
      const validRegions = this.regions.filter(region => 
        region && region !== 'undefined' && region !== 'Field' && 
        region !== '未知区域' && region !== 'Field-001' && region.length > 0);
      
      console.log('有效区域列表:', validRegions);
      
      // 如果没有有效区域，显示一个提示信息
      if (validRegions.length === 0) {
        this.regionMap.setOption({
          title: {
            text: '暂无区域数据',
            left: 'center',
            top: 'center',
            textStyle: {
              fontSize: 16,
              color: '#999'
            }
          },
          series: []
        });
        return;
      }
      
      // 定义区域颜色映射
      const regionColors = {
        '华东': '#f56c6c',
        '华南': '#f8c471',
        '华中': '#67c23a',
        '华北': '#409EFF',
        '西北': '#E6A23C',
        '西南': '#9370DB',
        '东北': '#20B2AA',
        '新疆': '#FFB6C1'
      };
      
      // 处理每个有效区域
      validRegions.forEach(region => {
        // 统计这个区域的传感器数量和异常数量
        const sensorsInRegion = this.sensorData.filter(s => s.region === region);
        
        if (sensorsInRegion.length > 0) {
          const sensorTypeDistribution = {};
          
          // 统计每种传感器类型的数量
          sensorsInRegion.forEach(s => {
            if (!sensorTypeDistribution[s.sensorType]) {
              sensorTypeDistribution[s.sensorType] = 0;
            }
            sensorTypeDistribution[s.sensorType]++;
          });
          
          // 计算异常率
          const anomaliesInRegion = sensorsInRegion.filter(s => s.isAnomaly).length;
          const anomalyRate = anomaliesInRegion / sensorsInRegion.length;
          
          // 确定主要作物类型
          const cropTypes = {};
          sensorsInRegion.forEach(s => {
            if (s.cropType) {
              if (!cropTypes[s.cropType]) {
                cropTypes[s.cropType] = 0;
              }
              cropTypes[s.cropType]++;
            }
          });
          
          // 找出最常见的作物类型
          let mainCrop = '农作物';
          let maxCount = 0;
          Object.keys(cropTypes).forEach(crop => {
            if (cropTypes[crop] > maxCount) {
              maxCount = cropTypes[crop];
              mainCrop = crop;
            }
          });
          
          // 收集区域数据
          regionData.push({
            name: `${region} (${mainCrop})`,
            value: sensorsInRegion.length, // 传感器数量作为值
            anomalyRate: anomalyRate,
            anomalyCount: anomaliesInRegion,
            // 创建详细信息
            sensorTypes: Object.keys(sensorTypeDistribution).map(type => ({
              name: this.getSensorTypeName(type),
              count: sensorTypeDistribution[type]
            })),
            itemStyle: {
              color: regionColors[region] || this.getColorByAnomalyRate(anomalyRate)
            }
          });
        }
      });
      
      console.log('区域地图数据:', regionData);
      
      // 如果没有足够的数据，显示提示信息
      if (regionData.length === 0) {
        this.regionMap.setOption({
          title: {
            text: '暂无区域数据',
            left: 'center',
            top: 'center',
            textStyle: {
              fontSize: 16,
              color: '#999'
            }
          },
          series: []
        });
        return;
      }
      
      const option = {
        title: {
          text: '农业区域分布',
          left: 'center',
          top: 0,
          textStyle: {
            fontSize: 16,
            color: '#303133'
          }
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            let sensorTypeInfo = '<br>传感器类型分布:<br>';
            params.data.sensorTypes.forEach(type => {
              sensorTypeInfo += `  ${type.name}: ${type.count}条数据<br>`;
            });
            
            return `${params.name}<br>` +
                   `总数据量: ${params.value}条<br>` +
                   `异常数据: ${params.data.anomalyCount}条<br>` +
                   `异常率: ${(params.data.anomalyRate * 100).toFixed(1)}%` +
                   sensorTypeInfo;
          }
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          top: 'middle',
          selectedMode: 'multiple',
          textStyle: {
            fontSize: 12
          }
        },
        series: [
          {
            name: '区域分布',
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['60%', '50%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: true,
              formatter: function(params) {
                return `${params.name}\n${params.percent}%`;
              },
              fontSize: 12,
              fontWeight: 'bold'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 16,
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: true,
              length: 15,
              length2: 10
            },
            data: regionData
          }
        ]
      };
      
      this.regionMap.setOption(option, true);
    },
    
    // 根据异常率获取颜色
    getColorByAnomalyRate(rate) {
      if (rate >= 0.5) return '#c23531';  // 高异常率，红色
      if (rate >= 0.2) return '#e98f6f';  // 中异常率，橙色
      return '#91c7ae';  // 低异常率，绿色
    },
    
    // 应用过滤器
    applyFilters() {
      // 根据过滤条件筛选数据
      this.latestData = this.sensorData
        .filter(item => {
          // 区域过滤
          if (this.selectedRegion && item.region !== this.selectedRegion) {
            return false;
          }
          
          // 传感器类型过滤
          if (this.selectedSensorType && item.sensorType !== this.selectedSensorType) {
            return false;
          }
          
          // 作物类型过滤
          if (this.selectedCropType && item.cropType !== this.selectedCropType) {
            return false;
          }
          
          // 异常值过滤
          if (this.showOnlyAnomalies && !item.isAnomaly) {
            return false;
          }
          
          return true;
        })
        .sort((a, b) => {
          // 优先按时间戳降序排序
          const timeA = new Date(a.timestamp);
          const timeB = new Date(b.timestamp);
          return timeB - timeA;
        })
        .slice(0, 100);  // 只显示最新的100条数据
      
      console.log('过滤后的数据:', this.latestData);
      
      // 更新图表
      this.updateCharts();
    },
    
    // 计算异常率
    calculateAnomalyRate() {
      const anomalies = this.sensorData.filter(item => item.isAnomaly).length;
      this.stats.anomalyRate = this.sensorData.length > 0 ? anomalies / this.sensorData.length : 0;
    },
    
    // 更新传感器总数
    updateSensorCount() {
      // 获取唯一的传感器ID数量
      const uniqueSensors = new Set();
      this.sensorData.forEach(item => {
        uniqueSensors.add(item.sensorId);
      });
      this.stats.sensorCount = uniqueSensors.size;
    },
    
    // 清空表格
    clearTable() {
      this.latestData = [];
    },
    
    // 调整图表大小
    resizeCharts() {
      if (this.trendChart) this.trendChart.resize();
      if (this.anomalyChart) this.anomalyChart.resize();
      if (this.regionMap) this.regionMap.resize();
    },
    
    // 显示传感器详情
    showSensorDetail(sensor) {
      this.selectedSensor = sensor;
      this.sensorDetailVisible = true;
    }
  }
};
</script>

<style scoped>
.sensor-dashboard {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
}

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.connection-status {
  padding: 5px 10px;
  border-radius: 4px;
  background-color: #f56c6c;
  color: white;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
}

.connection-status.connected {
  background-color: #67c23a;
}

.dashboard-filters {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  background: white;
  padding: 15px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.dashboard-stats {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 150px;
  text-align: center;
  border-radius: 8px;
  overflow: hidden;
  transition: all 0.3s ease;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.2);
}

.stat-value {
  font-size: 28px;
  font-weight: bold;
  margin-bottom: 5px;
  color: #409EFF;
}

.stat-title {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.anomaly-card {
  background-color: #fff8f8;
}

.anomaly-card .stat-value {
  color: #c23531;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.chart-container {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.chart-card {
  flex: 1;
  min-width: 300px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.chart-card >>> .el-card__header {
  background-color: #f5f7fa;
  font-weight: bold;
  color: #303133;
}

.map-and-table {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.region-map-card {
  flex: 1;
  min-width: 300px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.data-table-card {
  flex: 2;
  min-width: 600px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.anomaly-value {
  color: #c23531;
  font-weight: bold;
}

/* 详情对话框样式 */
.sensor-detail-actions {
  margin-top: 20px;
  text-align: center;
}

@media (max-width: 768px) {
  .chart-card, .region-map-card, .data-table-card {
    flex: 1 1 100%;
  }
}
</style> 