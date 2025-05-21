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
        <el-option v-for="type in sensorTypes" :key="type" :label="type" :value="type"></el-option>
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
          <el-table :data="latestData" height="250" border style="width: 100%">
            <el-table-column prop="sensorId" label="传感器ID" width="120"></el-table-column>
            <el-table-column prop="sensorType" label="类型" width="100"></el-table-column>
            <el-table-column prop="value" label="数值" width="100">
              <template slot-scope="scope">
                <span :class="{'anomaly-value': scope.row.isAnomaly}">
                  {{ scope.row.value }} {{ scope.row.unit }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="region" label="区域" width="100"></el-table-column>
            <el-table-column prop="cropType" label="作物" width="100"></el-table-column>
            <el-table-column prop="timestamp" label="时间" width="180"></el-table-column>
            <el-table-column prop="isAnomaly" label="异常" width="80">
              <template slot-scope="scope">
                <el-tag :type="scope.row.isAnomaly ? 'danger' : 'success'">
                  {{ scope.row.isAnomaly ? '是' : '否' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import { formatDistanceToNow } from 'date-fns';
import { zhCN } from 'date-fns/locale';

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
      wsUrl: 'ws://localhost:8085'
    };
  },
  mounted() {
    this.initCharts();
    this.connectWebSocket();
    
    // 监听窗口大小变化，调整图表大小
    window.addEventListener('resize', this.resizeCharts);
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
      // 处理欢迎消息
      if (message.type === 'welcome') {
        console.log('收到欢迎消息');
        return;
      }
      
      // 处理传感器数据
      if (message.type === 'sensor_data' && Array.isArray(message.data)) {
        this.processSensorData(message.data);
      }
    },
    
    processSensorData(data) {
      // 更新统计信息
      this.stats.messageCount += data.length;
      this.stats.lastUpdate = new Date().toLocaleTimeString();
      
      // 处理每条数据
      data.forEach(item => {
        // 添加到传感器数据数组
        this.sensorData.push(item);
        
        // 限制数组大小
        if (this.sensorData.length > 1000) {
          this.sensorData.shift();
        }
        
        // 更新区域、传感器类型和作物类型列表
        if (!this.regions.includes(item.region)) {
          this.regions.push(item.region);
        }
        
        if (!this.sensorTypes.includes(item.sensorType)) {
          this.sensorTypes.push(item.sensorType);
        }
        
        if (!this.cropTypes.includes(item.cropType)) {
          this.cropTypes.push(item.cropType);
        }
        
        // 更新图表数据
        this.updateChartData(item);
      });
      
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
      const timestamp = new Date(item.timestamp);
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
      if (!this.chartData.anomalies[item.region]) {
        this.chartData.anomalies[item.region] = 0;
      }
      
      if (item.isAnomaly) {
        this.chartData.anomalies[item.region]++;
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
      
      // 为每种传感器类型创建一个系列
      Object.keys(this.chartData.values).forEach(sensorType => {
        // 如果有过滤器，只显示选中的传感器类型
        if (this.selectedSensorType && this.selectedSensorType !== sensorType) {
          return;
        }
        
        const data = this.chartData.values[sensorType].map(point => {
          // 如果显示异常值，将正常值设为null
          if (this.showOnlyAnomalies && !point.isAnomaly) {
            return [point.time, null];
          }
          return [point.time, point.value];
        });
        
        series.push({
          name: sensorType,
          type: 'line',
          smooth: true,
          showSymbol: false,
          data: data
        });
      });
      
      const option = {
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            let result = params[0].axisValue + '<br/>';
            params.forEach(param => {
              result += param.marker + param.seriesName + ': ' + param.value[1] + '<br/>';
            });
            return result;
          }
        },
        legend: {
          data: Object.keys(this.chartData.values),
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
      
      this.trendChart.setOption(option);
    },
    
    // 更新异常分布图
    updateAnomalyChart() {
      if (!this.anomalyChart) return;
      
      const data = Object.keys(this.chartData.anomalies).map(region => {
        return {
          value: this.chartData.anomalies[region],
          name: region
        };
      });
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left',
          data: Object.keys(this.chartData.anomalies)
        },
        series: [
          {
            name: '异常分布',
            type: 'pie',
            radius: '65%',
            center: ['50%', '50%'],
            data: data,
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      };
      
      this.anomalyChart.setOption(option);
    },
    
    // 更新区域地图
    updateRegionMap() {
      if (!this.regionMap) return;
      
      // 简单的中国区域地图，用不同颜色表示不同区域
      const data = this.regions.map(region => {
        // 统计这个区域的传感器数量和异常数量
        const sensorsInRegion = this.sensorData.filter(s => s.region === region);
        const anomaliesInRegion = sensorsInRegion.filter(s => s.isAnomaly).length;
        const anomalyRate = sensorsInRegion.length > 0 ? anomaliesInRegion / sensorsInRegion.length : 0;
        
        return {
          name: region,
          value: anomalyRate,
          itemStyle: {
            color: this.getColorByAnomalyRate(anomalyRate)
          }
        };
      });
      
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            return params.name + '<br/>异常率: ' + (params.value * 100).toFixed(1) + '%';
          }
        },
        series: [
          {
            type: 'treemap',
            data: data,
            label: {
              show: true,
              formatter: '{b}'
            },
            breadcrumb: {
              show: false
            }
          }
        ]
      };
      
      this.regionMap.setOption(option);
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
        .sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp))
        .slice(0, 100);  // 只显示最新的100条数据
      
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
}

.connection-status.connected {
  background-color: #67c23a;
}

.dashboard-filters {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
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
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 5px;
}

.stat-title {
  font-size: 14px;
  color: #606266;
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
}

.map-and-table {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.region-map-card {
  flex: 1;
  min-width: 300px;
}

.data-table-card {
  flex: 2;
  min-width: 600px;
}

.anomaly-value {
  color: #c23531;
  font-weight: bold;
}

@media (max-width: 768px) {
  .chart-card, .region-map-card, .data-table-card {
    flex: 1 1 100%;
  }
}
</style> 