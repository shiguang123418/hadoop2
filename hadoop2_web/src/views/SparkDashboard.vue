<template>
  <div class="big-data-dashboard">
    <div class="dashboard-header">
      <div class="title-section">
        农业传感器大数据可视化监控平台
        <div class="connection-status" :class="streamFetchStatus">
          <span v-if="streamFetchStatus === 'idle'">等待连接数据流...</span>
          <span v-else-if="streamFetchStatus === 'loading'">正在连接到Kafka数据流...</span>
          <span v-else-if="streamFetchStatus === 'success'">已连接 - 实时数据流</span>
          <span v-else-if="streamFetchStatus === 'error'">连接失败 - 使用模拟数据</span>
          <button v-if="streamFetchStatus === 'error'" @click="startRealTimeKafkaProcessing" class="retry-button">重试连接</button>
        </div>
      </div>
      <div class="stats-row">
        <div class="stat-box-container left">
          <div class="stats-section">
            <div class="stat-row">
              <div class="stat-box cyan">
                <div class="stat-value">{{ statsData.sensorCounts.temperature }}</div>
                <div class="stat-label">温度传感器</div>
              </div>
              <div class="stat-box green">
                <div class="stat-value">{{ statsData.sensorCounts.humidity }}</div>
                <div class="stat-label">湿度传感器</div>
              </div>
              <div class="stat-box purple">
                <div class="stat-value">{{ statsData.sensorCounts.ph }}</div>
                <div class="stat-label">pH值传感器</div>
              </div>
            </div>
            <div class="stat-row">
              <div class="stat-box orange">
                <div class="stat-value">{{ statsData.sensorCounts.wind }}</div>
                <div class="stat-label">风速传感器</div>
              </div>
              <div class="stat-box yellow">
                <div class="stat-value">{{ statsData.sensorCounts.light }}</div>
                <div class="stat-label">光照传感器</div>
              </div>
              <div class="stat-box blue">
                <div class="stat-value">{{ statsData.sensorCounts.co2 }}</div>
                <div class="stat-label">二氧化碳传感器</div>
              </div>
              <div class="stat-box red">
                <div class="stat-value">{{ statsData.sensorCounts.soil }}</div>
                <div class="stat-label">土壤湿度传感器</div>
              </div>
            </div>
          </div>
        </div>

        <div class="center-stats">
          <div class="center-title">监测作物种类</div>
          <div class="center-numbers">
            <div class="center-number">{{ Object.keys(sensorDistribution.crops).length }}</div>
          </div>
        </div>

        <div class="stat-box-container right">
          <div class="stats-section">
            <div class="stat-row">
              <div class="stat-box cyan">
                <div class="stat-value">{{ statsData.totalReadings }}</div>
                <div class="stat-label">实时数据量</div>
              </div>
              <div class="stat-box green">
                <div class="stat-value">{{ statsData.dailyTotal }}</div>
                <div class="stat-label">今日数据</div>
              </div>
            </div>
            <div class="stat-row">
              <div class="stat-box blue">
                <div class="stat-value">{{ statsData.monitoredRegions }}</div>
                <div class="stat-label">监测区域</div>
              </div>
              <div class="stat-box purple">
                <div class="stat-value">{{ statsData.totalSensors }}</div>
                <div class="stat-label">传感器总数</div>
              </div>
              <div class="stat-box orange">
                <div class="stat-value">{{ statsData.anomalies }}</div>
                <div class="stat-label">异常数据</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="dashboard-content">
      <div class="chart-row">
        <div class="chart-card">
          <div class="chart-title">土壤湿度分布</div>
          <div ref="soilMoistureChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <div class="chart-title">作物分布占比</div>
          <div ref="cropDistributionChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <div class="chart-title">区域分布占比</div>
          <div ref="regionDistributionChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <div class="chart-title">传感器类型占比</div>
          <div ref="sensorTypeChart" class="chart-container"></div>
        </div>
      </div>

      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-title">平均温度</div>
          <div class="kpi-value">{{ kpiData.avgTemperature }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-title">平均湿度</div>
          <div class="kpi-value">{{ kpiData.avgHumidity }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-title">平均光照</div>
          <div class="kpi-value">{{ kpiData.avgLight }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-title">平均风速</div>
          <div class="kpi-value">{{ kpiData.avgWind }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-title">平均CO₂</div>
          <div class="kpi-value">{{ kpiData.avgCO2 }}</div>
        </div>
      </div>

      <div class="chart-row bottom-charts">
        <div class="chart-card">
          <div class="chart-title">实时数据流</div>
          <div class="data-list">
            <div class="data-item" v-for="(item, index) in latestSensorData" :key="index">
              <div class="data-content">
                <span class="sensor-id">{{ item.sensorId }}</span>
                <span class="sensor-type">{{ item.sensorType }}</span>
                <span class="value">{{ item.value }}{{ item.unit }}</span>
              </div>
              <div class="data-meta">
                <span class="data-region">{{ item.region }}</span>
                <span class="data-crop">{{ item.cropType }}</span>
                <span class="data-time">{{ item.timestamp }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="chart-card">
          <div class="chart-title">传感器分布热力图</div>
          <div ref="sensorHeatmapChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <div class="chart-title">异常数据检测</div>
          <div class="data-list">
            <div class="data-item anomaly" v-for="(item, index) in anomalyData" :key="index">
              <div class="data-content">
                <span class="sensor-id">{{ item.sensorId }}</span>
                <span class="sensor-type">{{ item.sensorType }}</span>
                <span class="value">{{ item.value }}{{ item.unit }}</span>
              </div>
              <div class="data-meta">
                <span class="data-region">{{ item.region }}</span>
                <span class="data-crop">{{ item.cropType }}</span>
                <span class="data-time">{{ item.timestamp }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-row visualization-charts">
        <div class="chart-card">
          <div class="chart-title">作物生长环境评分</div>
          <div ref="cropEnvironmentChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <div class="chart-title">各区域气温变化趋势</div>
          <div ref="temperatureTrendChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <div class="chart-title">水肥一体化监测</div>
          <div ref="irrigationMonitorChart" class="chart-container"></div>
        </div>

        <div class="chart-card">
          <div class="chart-title">环境质量指数</div>
          <div ref="environmentIndexChart" class="chart-container"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import KafkaAnalyticsService from '@/services/KafkaAnalyticsService';
import apiConfig from '@/config/api.config';

export default {
  name: 'AgricultureDashboard',
  data() {
    return {
      charts: {},
      latestSensorData: [],
      anomalyData: [],
      kafkaStreamActive: false,
      statsData: {
        sensorCounts: {
          temperature: 34,
          humidity: 28,
          ph: 23,
          wind: 24,
          light: 42,
          co2: 19,
          soil: 21
        },
        totalReadings: 203,
        dailyTotal: 1367,
        monitoredRegions: 6,
        totalSensors: 191,
        anomalies: 34
      },
      sensorDistribution: {
        sensors: {
          "温度传感器": 34,
          "湿度传感器": 28,
          "pH值传感器": 23,
          "风速传感器": 24,
          "光照传感器": 42,
          "二氧化碳传感器": 19,
          "土壤湿度传感器": 21
        },
        regions: {
          "华中": 42,
          "西北": 32,
          "华北": 37,
          "东北": 24,
          "华东": 29,
          "华南": 27
        },
        crops: {
          "水稻": 30,
          "小麦": 35,
          "玉米": 25,
          "大豆": 28,
          "马铃薯": 32,
          "棉花": 24,
          "甜菜": 17
        }
      },
      environmentalData: {
        temperature: {
          "华中": [22.5, 23.1, 24.2, 25.3, 26.1, 26.8, 27.2, 26.5, 25.8, 24.6, 23.3, 22.0],
          "西北": [18.2, 19.5, 21.3, 23.6, 25.7, 27.8, 28.9, 28.2, 25.6, 22.1, 19.4, 17.8],
          "华北": [19.4, 20.8, 22.6, 24.9, 26.2, 27.5, 28.1, 27.6, 25.9, 23.4, 20.7, 18.6],
          "东北": [16.8, 18.2, 20.7, 23.8, 25.9, 27.2, 27.9, 27.1, 24.8, 21.3, 18.5, 16.2],
          "华东": [21.3, 22.5, 23.8, 25.2, 26.7, 27.9, 28.4, 28.0, 26.5, 24.8, 22.7, 21.0],
          "华南": [23.8, 24.6, 25.7, 26.9, 27.8, 28.5, 28.9, 28.7, 27.9, 26.5, 25.2, 24.0]
        },
        soilMoisture: {
          "水稻": 72.4,
          "小麦": 52.6,
          "玉米": 58.9,
          "大豆": 61.2,
          "马铃薯": 64.8,
          "棉花": 48.7,
          "甜菜": 56.2
        },
        environmentScore: {
          "水稻": [82, 75, 68, 90, 78],
          "小麦": [75, 85, 70, 65, 80],
          "玉米": [65, 70, 80, 75, 68],
          "大豆": [70, 65, 75, 82, 78],
          "马铃薯": [85, 80, 65, 75, 82],
          "棉花": [78, 72, 85, 80, 68],
          "甜菜": [72, 75, 82, 68, 76]
        }
      },
      kpiData: {
        avgTemperature: '24.7°C',
        avgHumidity: '45.2%',
        avgLight: '682.3lux',
        avgWind: '8.4m/s',
        avgCO2: '1321ppm'
      },
      dataPollingInterval: null,
      pollingRate: apiConfig.kafka.pollingInterval,
      streamFetchStatus: 'idle'
    };
  },
  async mounted() {
    this.initCharts();
    window.addEventListener('resize', this.handleResize);
    
    try {
      // 先测试Kafka连接
      this.streamFetchStatus = 'loading';
      const connectionAvailable = await KafkaAnalyticsService.testKafkaConnection();
      
      if (connectionAvailable) {
        // 连接成功，启动实时处理
        await this.startRealTimeKafkaProcessing();
      } else {
        // 连接失败，使用模拟数据
        console.warn('Kafka服务不可用，使用模拟数据');
        this.streamFetchStatus = 'error';
        this.setupSimulatedKafkaData();
      }
    } catch (e) {
      console.error('初始化出错:', e);
      this.streamFetchStatus = 'error';
      this.setupSimulatedKafkaData();
    }
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.handleResize);
    this.disposeCharts();
    this.stopRealTimeKafkaProcessing();
  },
  methods: {
    initCharts() {
      this.$nextTick(() => {
        // Initialize all charts
        this.initSoilMoistureChart();
        this.initCropDistributionChart();
        this.initRegionDistributionChart();
        this.initSensorTypeChart();
        this.initSensorHeatmapChart();
        this.initCropEnvironmentChart();
        this.initTemperatureTrendChart();
        this.initIrrigationMonitorChart();
        this.initEnvironmentIndexChart();
      });
    },

    handleResize() {
      for (const key in this.charts) {
        if (this.charts[key]) {
          this.charts[key].resize();
        }
      }
    },

    disposeCharts() {
      for (const key in this.charts) {
        if (this.charts[key]) {
          this.charts[key].dispose();
        }
      }
    },

    async startRealTimeKafkaProcessing() {
      try {
        // First check Kafka stream status
        const statusResponse = await KafkaAnalyticsService.getStreamingStatus();
        this.kafkaStreamActive = statusResponse.data.active;
        
        if (!this.kafkaStreamActive) {
          // Start Kafka stream processing if not active
          await KafkaAnalyticsService.startStreaming(apiConfig.kafka.defaultTopics);
          this.kafkaStreamActive = true;
        }
        
        // Fetch initial data
        await this.fetchLatestSensorData();
        await this.fetchAnomalies();
        await this.fetchSummaryData();
        
        // Set up polling for real-time updates
        this.setupDataPolling();
        
        console.log('Kafka real-time processing started successfully');
        this.streamFetchStatus = 'success';
      } catch (error) {
        console.error('Failed to start Kafka processing:', error);
        // Fallback to simulation if Kafka service is not available
        this.streamFetchStatus = 'error';
        this.setupSimulatedKafkaData();
      }
    },
    
    async stopRealTimeKafkaProcessing() {
      if (this.dataPollingInterval) {
        clearInterval(this.dataPollingInterval);
        this.dataPollingInterval = null;
      }
      
      if (this.kafkaStreamActive) {
        try {
          await KafkaAnalyticsService.stopStreaming();
          console.log('Kafka stream processing stopped');
        } catch (error) {
          console.error('Error stopping Kafka stream:', error);
        }
      }
    },
    
    setupDataPolling() {
      // Clear any existing interval
      if (this.dataPollingInterval) {
        clearInterval(this.dataPollingInterval);
      }
      
      // Set up polling interval for real-time data
      this.dataPollingInterval = setInterval(async () => {
        this.streamFetchStatus = 'loading';
        try {
          await Promise.all([
            this.fetchLatestSensorData(),
            this.fetchAnomalies(),
            this.updateKpiData()
          ]);
          this.streamFetchStatus = 'success';
        } catch (error) {
          console.error('Error fetching real-time data:', error);
          this.streamFetchStatus = 'error';
        }
      }, this.pollingRate);
    },
    
    async fetchLatestSensorData() {
      try {
        const response = await KafkaAnalyticsService.getLatestReadings();
        if (response.data && Array.isArray(response.data)) {
          // Update latest sensor data
          this.latestSensorData = response.data.map(item => ({
            sensorId: item.sensorId,
            sensorType: this.formatSensorType(item.sensorType),
            region: item.region,
            cropType: item.cropType,
            value: parseFloat(item.value).toFixed(2),
            unit: this.getUnitForSensorType(item.sensorType),
            timestamp: this.formatTimestamp(item.timestamp)
          }));
          
          // Update stats counter
          this.statsData.totalReadings = response.data.length;
          
          // Update charts with new data
          this.updateChartsWithNewData();
        }
      } catch (error) {
        console.error('Error fetching latest sensor readings:', error);
      }
    },
    
    async fetchAnomalies() {
      try {
        const response = await KafkaAnalyticsService.getAnomalies();
        if (response.data && Array.isArray(response.data)) {
          this.anomalyData = response.data.map(item => ({
            sensorId: item.sensorId,
            sensorType: this.formatSensorType(item.sensorType),
            region: item.region,
            cropType: item.cropType,
            value: parseFloat(item.value).toFixed(2),
            unit: this.getUnitForSensorType(item.sensorType),
            timestamp: this.formatTimestamp(item.timestamp)
          }));
          
          // Update anomaly counter
          this.statsData.anomalies = this.anomalyData.length;
        }
      } catch (error) {
        console.error('Error fetching anomalies:', error);
      }
    },
    
    async fetchSummaryData() {
      try {
        const response = await KafkaAnalyticsService.getSummary();
        if (response.data) {
          const summary = response.data;
          
          // Update sensor distribution
          if (summary.sensorCounts) {
            this.sensorDistribution.sensors = {
              "温度传感器": summary.sensorCounts.temperature || 0,
              "湿度传感器": summary.sensorCounts.humidity || 0,
              "pH值传感器": summary.sensorCounts.ph || 0,
              "风速传感器": summary.sensorCounts.wind || 0,
              "光照传感器": summary.sensorCounts.light || 0,
              "二氧化碳传感器": summary.sensorCounts.co2 || 0,
              "土壤湿度传感器": summary.sensorCounts.soil || 0
            };
          }
          
          // Update regions and crops data if available
          if (summary.regionCounts) {
            this.sensorDistribution.regions = summary.regionCounts;
          }
          
          if (summary.cropCounts) {
            this.sensorDistribution.crops = summary.cropCounts;
          }
          
          // Update KPI data
          if (summary.averages) {
            this.kpiData = {
              avgTemperature: `${summary.averages.temperature || 0}°C`,
              avgHumidity: `${summary.averages.humidity || 0}%`,
              avgLight: `${summary.averages.light || 0}lux`,
              avgWind: `${summary.averages.wind || 0}m/s`,
              avgCO2: `${summary.averages.co2 || 0}ppm`
            };
          }
          
          // Update the charts with new summary data
          this.updateSensorDistributionCharts();
        }
      } catch (error) {
        console.error('Error fetching summary data:', error);
      }
    },
    
    async updateKpiData() {
      try {
        // Get region averages for the KPI cards
        const response = await KafkaAnalyticsService.getRegionAverages();
        if (response.data) {
          const averages = response.data;
          
          // Calculate overall averages across all regions
          let tempSum = 0, tempCount = 0;
          let humiditySum = 0, humidityCount = 0;
          let lightSum = 0, lightCount = 0;
          let windSum = 0, windCount = 0;
          let co2Sum = 0, co2Count = 0;
          
          // Process all region data
          Object.values(averages).forEach(region => {
            if (region.temperature) { tempSum += region.temperature; tempCount++; }
            if (region.humidity) { humiditySum += region.humidity; humidityCount++; }
            if (region.light) { lightSum += region.light; lightCount++; }
            if (region.wind) { windSum += region.wind; windCount++; }
            if (region.co2) { co2Sum += region.co2; co2Count++; }
          });
          
          // Update KPI data with calculated averages
          this.kpiData = {
            avgTemperature: `${(tempCount > 0 ? (tempSum / tempCount).toFixed(1) : 0)}°C`,
            avgHumidity: `${(humidityCount > 0 ? (humiditySum / humidityCount).toFixed(1) : 0)}%`,
            avgLight: `${(lightCount > 0 ? (lightSum / lightCount).toFixed(1) : 0)}lux`,
            avgWind: `${(windCount > 0 ? (windSum / windCount).toFixed(1) : 0)}m/s`,
            avgCO2: `${(co2Count > 0 ? (co2Sum / co2Count).toFixed(0) : 0)}ppm`
          };
        }
      } catch (error) {
        console.error('Error updating KPI data:', error);
      }
    },
    
    updateChartsWithNewData() {
      // Update charts based on latest data
      this.$nextTick(() => {
        // Re-render charts with updated data
        if (this.charts.soilMoisture) {
          this.updateSoilMoistureChart();
        }
        
        if (this.charts.cropDistribution) {
          this.updateCropDistributionChart();
        }
        
        if (this.charts.regionDistribution) {
          this.updateRegionDistributionChart();
        }
        
        if (this.charts.sensorType) {
          this.updateSensorTypeChart();
        }
      });
    },
    
    updateSensorDistributionCharts() {
      // Update all sensor distribution related charts
      this.updateCropDistributionChart();
      this.updateRegionDistributionChart();
      this.updateSensorTypeChart();
    },
    
    // Utility functions for formatting and unit conversion
    formatSensorType(type) {
      const typeMap = {
        'temperature': '温度传感器',
        'humidity': '湿度传感器',
        'ph': 'pH值传感器',
        'wind': '风速传感器',
        'light': '光照传感器',
        'co2': '二氧化碳传感器',
        'soil': '土壤湿度传感器'
      };
      return typeMap[type] || type;
    },
    
    getUnitForSensorType(type) {
      const unitMap = {
        'temperature': '°C',
        'humidity': '%',
        'ph': '',
        'wind': 'm/s',
        'light': 'lux',
        'co2': 'ppm',
        'soil': '%'
      };
      return unitMap[type] || '';
    },
    
    formatTimestamp(timestamp) {
      if (!timestamp) return '';
      const date = new Date(timestamp);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      });
    },
    
    // Fallback to simulation if real Kafka service is not available
    setupSimulatedKafkaData() {
      console.log("Setting up simulated Kafka data stream");
      
      // Simulate sending data to Kafka
      setInterval(() => {
        KafkaAnalyticsService.sendSingleData().catch(err => {
          console.warn('Error sending simulated data:', err);
        });
      }, 3000);
      
      // Simulate real-time data with interval
      this.kafkaInterval = setInterval(() => {
        const newData = this.generateSensorData();
        this.processKafkaMessage(newData);
        
        // Add to latest data list
        this.latestSensorData.unshift(newData);
        if (this.latestSensorData.length > 5) {
          this.latestSensorData.pop();
        }
        
        // Randomly add anomalies
        if (Math.random() < 0.2) {
          const anomaly = this.generateAnomalyData();
          this.anomalyData.unshift(anomaly);
          if (this.anomalyData.length > 3) {
            this.anomalyData.pop();
          }
        }
        
        // Update stats
        this.statsData.totalReadings++;
        this.statsData.dailyTotal++;
        
        // Update charts
        this.updateChartsWithNewData();
        
      }, this.pollingRate);
    },

    processKafkaMessage(data) {
      // Process incoming Kafka message
      console.log("Processing Kafka message:", data);
      
      // Update sensor stats
      if (data.sensorType) {
        const sensorTypeKey = this.getSensorTypeKey(data.sensorType);
        if (sensorTypeKey && this.statsData.sensorCounts[sensorTypeKey]) {
          // Increment values for visualization only (actual counts come from backend)
        }
      }
    },
    
    getSensorTypeKey(type) {
      const typeKeys = {
        '温度传感器': 'temperature',
        '湿度传感器': 'humidity',
        'pH值传感器': 'ph',
        '风速传感器': 'wind',
        '光照传感器': 'light',
        '二氧化碳传感器': 'co2',
        '土壤湿度传感器': 'soil'
      };
      return typeKeys[type] || type;
    },
    
    // Keep the existing simulation methods as fallback
    generateSensorData() {
      // ... existing code ...
    },
    
    generateAnomalyData() {
      // Generate an anomaly data point
      const sensorData = this.generateSensorData();
      
      // Make it an anomaly by pushing it to extreme values
      switch (sensorData.sensorType) {
        case "温度传感器":
          sensorData.value = (Math.random() < 0.5 ? -10 : 45) + Math.random() * 5;
          break;
        case "湿度传感器":
          sensorData.value = (Math.random() < 0.5 ? 0 : 95) + Math.random() * 5;
          break;
        case "pH值传感器":
          sensorData.value = (Math.random() < 0.5 ? 1 : 13) + Math.random();
          break;
        case "风速传感器":
          sensorData.value = 30 + Math.random() * 20;
          break;
        case "光照传感器":
          sensorData.value = (Math.random() < 0.5 ? 0 : 10000) + Math.random() * 1000;
          break;
        case "二氧化碳传感器":
          sensorData.value = 3000 + Math.random() * 2000;
          break;
        case "土壤湿度传感器":
          sensorData.value = (Math.random() < 0.5 ? 0 : 95) + Math.random() * 5;
          break;
      }
      
      sensorData.value = parseFloat(sensorData.value).toFixed(2);
      return sensorData;
    },

    initSoilMoistureChart() {
      this.charts.soilMoisture = echarts.init(this.$refs.soilMoistureChart);
      const option = {
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}%'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: Object.keys(this.environmentalData.soilMoisture),
          axisLine: {
            lineStyle: {
              color: '#0c95e8'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10
          }
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 100,
          axisLine: {
            show: true,
            lineStyle: {
              color: '#0c95e8'
            }
          },
          splitLine: {
            show: false
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10,
            formatter: '{value}%'
          }
        },
        series: [{
          data: Object.values(this.environmentalData.soilMoisture),
          type: 'bar',
          barWidth: '40%',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#00fcff' },
              { offset: 1, color: '#006bff' }
            ])
          }
        }]
      };
      this.charts.soilMoisture.setOption(option);
    },

    initCropDistributionChart() {
      this.charts.cropDistribution = echarts.init(this.$refs.cropDistributionChart);
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: '5%',
          top: 'center',
          textStyle: {
            color: '#fff',
            fontSize: 10
          }
        },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['40%', '50%'],
            avoidLabelOverlap: false,
            label: {
              show: false
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 14,
                fontWeight: 'bold'
              }
            },
            data: Object.entries(this.sensorDistribution.crops).map(([name, value]) => ({
              name,
              value
            })),
            itemStyle: {
              normal: {
                color: function(params) {
                  const colorList = ['#4bfffc', '#ffa800', '#00ff9c', '#df29ff', '#ff5e7a', '#41ff40', '#905aff'];
                  return colorList[params.dataIndex % colorList.length];
                }
              }
            }
          }
        ]
      };
      this.charts.cropDistribution.setOption(option);
    },

    initRegionDistributionChart() {
      this.charts.regionDistribution = echarts.init(this.$refs.regionDistributionChart);
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          orient: 'vertical',
          right: '5%',
          top: 'center',
          textStyle: {
            color: '#fff',
            fontSize: 10
          }
        },
        series: [
          {
            type: 'pie',
            radius: ['40%', '70%'],
            center: ['40%', '50%'],
            avoidLabelOverlap: false,
            label: {
              show: false
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 14,
                fontWeight: 'bold'
              }
            },
            data: Object.entries(this.sensorDistribution.regions).map(([name, value]) => ({
              name,
              value
            })),
            itemStyle: {
              normal: {
                color: function(params) {
                  const colorList = ['#ffa800', '#4bfffc', '#00ff9c', '#df29ff', '#ff5e7a', '#41ff40'];
                  return colorList[params.dataIndex % colorList.length];
                }
              }
            }
          }
        ]
      };
      this.charts.regionDistribution.setOption(option);
    },

    initSensorTypeChart() {
      this.charts.sensorType = echarts.init(this.$refs.sensorTypeChart);
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        series: [
          {
            type: 'pie',
            radius: '70%',
            center: ['50%', '50%'],
            data: Object.entries(this.sensorDistribution.sensors).map(([name, value]) => ({
              name,
              value
            })),
            label: {
              color: '#fff',
              fontSize: 10
            },
            itemStyle: {
              normal: {
                color: function(params) {
                  const colorList = ['#00ff9c', '#4bfffc', '#ffa800', '#df29ff', '#ff5e7a', '#41ff40', '#905aff'];
                  return colorList[params.dataIndex % colorList.length];
                }
              }
            }
          }
        ]
      };
      this.charts.sensorType.setOption(option);
    },

    initSensorHeatmapChart() {
      this.charts.sensorHeatmap = echarts.init(this.$refs.sensorHeatmapChart);
      
      // Generate heatmap data
      const regions = Object.keys(this.sensorDistribution.regions);
      const sensorTypes = Object.keys(this.sensorDistribution.sensors);
      const data = [];
      
      regions.forEach((region, regionIndex) => {
        sensorTypes.forEach((type, typeIndex) => {
          // Generate random distribution for demo
          data.push([
            typeIndex,
            regionIndex,
            Math.round(Math.random() * 10 + 5)
          ]);
        });
      });
      
      const option = {
        tooltip: {
          position: 'top',
          formatter: function (params) {
            return `${regions[params.value[1]]}<br>${sensorTypes[params.value[0]]}: ${params.value[2]}`;
          }
        },
        grid: {
          left: '3%',
          right: '7%',
          bottom: '10%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: sensorTypes,
          axisLine: {
            lineStyle: {
              color: '#0c95e8'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10,
            rotate: 45
          }
        },
        yAxis: {
          type: 'category',
          data: regions,
          axisLine: {
            lineStyle: {
              color: '#0c95e8'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10
          }
        },
        visualMap: {
          min: 0,
          max: 15,
          calculable: true,
          orient: 'horizontal',
          left: 'center',
          bottom: '0%',
          textStyle: {
            color: '#fff'
          },
          inRange: {
            color: ['#313695', '#4575b4', '#74add1', '#abd9e9', '#e0f3f8', '#fee090', '#fdae61', '#f46d43', '#d73027', '#a50026']
          }
        },
        series: [{
          name: '传感器分布',
          type: 'heatmap',
          data: data,
          label: {
            show: false
          },
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }]
      };
      this.charts.sensorHeatmap.setOption(option);
    },

    initCropEnvironmentChart() {
      this.charts.cropEnvironment = echarts.init(this.$refs.cropEnvironmentChart);
      const option = {
        radar: {
          indicator: [
            { name: '土壤湿度', max: 100 },
            { name: '温度适宜性', max: 100 },
            { name: '光照充足性', max: 100 },
            { name: '肥力指数', max: 100 },
            { name: '病虫害风险', max: 100 }
          ],
          splitNumber: 4,
          axisName: {
            color: '#fff',
            fontSize: 10
          },
          splitArea: {
            areaStyle: {
              color: ['rgba(12, 97, 148, 0.2)', 'rgba(12, 97, 148, 0.4)', 'rgba(12, 97, 148, 0.6)', 'rgba(12, 97, 148, 0.8)']
            }
          },
          axisLine: {
            lineStyle: {
              color: 'rgba(12, 149, 232, 0.5)'
            }
          },
          splitLine: {
            lineStyle: {
              color: 'rgba(12, 149, 232, 0.5)'
            }
          }
        },
        legend: {
          data: Object.keys(this.environmentalData.environmentScore),
          bottom: '0%',
          textStyle: {
            color: '#fff',
            fontSize: 10
          }
        },
        series: [{
          type: 'radar',
          data: Object.entries(this.environmentalData.environmentScore).map(([crop, scores]) => ({
            value: scores,
            name: crop,
            areaStyle: {
              opacity: 0.6
            }
          }))
        }]
      };
      this.charts.cropEnvironment.setOption(option);
    },

    initTemperatureTrendChart() {
      this.charts.temperatureTrend = echarts.init(this.$refs.temperatureTrendChart);
      const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
      const option = {
        tooltip: {
          trigger: 'axis'
        },
        legend: {
          data: Object.keys(this.environmentalData.temperature),
          textStyle: {
            color: '#fff',
            fontSize: 10
          },
          bottom: '0%'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: months,
          axisLine: {
            lineStyle: {
              color: '#0c95e8'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10
          }
        },
        yAxis: {
          type: 'value',
          name: '温度(°C)',
          axisLine: {
            lineStyle: {
              color: '#0c95e8'
            }
          },
          splitLine: {
            show: false
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10
          }
        },
        series: Object.entries(this.environmentalData.temperature).map(([region, data]) => ({
          name: region,
          type: 'line',
          data: data,
          smooth: true
        }))
      };
      this.charts.temperatureTrend.setOption(option);
    },

    initIrrigationMonitorChart() {
      this.charts.irrigationMonitor = echarts.init(this.$refs.irrigationMonitorChart);
      
      // Generate sample irrigation data
      const crops = Object.keys(this.environmentalData.soilMoisture);
      const waterData = crops.map(() => Math.round(Math.random() * 500 + 200));
      const fertilizerData = crops.map(() => Math.round(Math.random() * 200 + 50));
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        legend: {
          data: ['用水量(L)', '施肥量(kg)'],
          textStyle: {
            color: '#fff',
            fontSize: 10
          },
          bottom: '0%'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '15%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: crops,
          axisLine: {
            lineStyle: {
              color: '#0c95e8'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10,
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          axisLine: {
            lineStyle: {
              color: '#0c95e8'
            }
          },
          splitLine: {
            show: false
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10
          }
        },
        series: [
          {
            name: '用水量(L)',
            type: 'bar',
            data: waterData,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#00fcff' },
                { offset: 1, color: '#006bff' }
              ])
            }
          },
          {
            name: '施肥量(kg)',
            type: 'bar',
            data: fertilizerData,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                { offset: 0, color: '#00ff9c' },
                { offset: 1, color: '#00c156' }
              ])
            }
          }
        ]
      };
      this.charts.irrigationMonitor.setOption(option);
    },

    initEnvironmentIndexChart() {
      this.charts.environmentIndex = echarts.init(this.$refs.environmentIndexChart);
      
      // Generate environment quality indexes for each region
      const regions = Object.keys(this.sensorDistribution.regions);
      const indexes = regions.map(() => Math.round(Math.random() * 40 + 60));
      
      const option = {
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          max: 100,
          axisLine: {
            lineStyle: {
              color: '#0c95e8'
            }
          },
          splitLine: {
            show: false
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10
          }
        },
        yAxis: {
          type: 'category',
          data: regions,
          axisLine: {
            lineStyle: {
              color: '#0c95e8'
            }
          },
          axisLabel: {
            color: '#fff',
            fontSize: 10
          }
        },
        series: [
          {
            data: indexes,
            type: 'bar',
            barWidth: '40%',
            itemStyle: {
              color: function(params) {
                // Color based on index value
                if (params.value >= 80) {
                  return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: '#00ff9c' },
                    { offset: 1, color: '#00c156' }
                  ]);
                } else if (params.value >= 60) {
                  return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: '#4bfffc' },
                    { offset: 1, color: '#0066e8' }
                  ]);
                } else {
                  return new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: '#ff365a' },
                    { offset: 1, color: '#aa0028' }
                  ]);
                }
              }
            },
            label: {
              show: true,
              position: 'right',
              formatter: '{c}',
              fontSize: 12,
              color: '#fff'
            }
          }
        ]
      };
      this.charts.environmentIndex.setOption(option);
    }
  }
};
</script>

<style scoped>
.big-data-dashboard {
  width: 100%;
  min-height: 100vh;
  background-color: #001529;
  color: #fff;
  padding: 15px;
  box-sizing: border-box;
  overflow-x: hidden;
}

.dashboard-header {
  width: 100%;
  max-width: 1800px;
  margin: 0 auto;
}

.title-section {
  text-align: center;
  font-size: 24px;
  font-weight: bold;
  margin-bottom: 20px;
  color: #00fcff;
  background: url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mN88ODBfwAJCANtQz7GkwAAAABJRU5ErkJggg==') no-repeat center;
  background-size: contain;
  padding: 10px 0;
}

.stats-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.stat-box-container {
  width: 35%;
}

.stats-section {
  border: 1px solid #0a5299;
  padding: 10px;
  border-radius: 5px;
  background-color: rgba(12, 97, 148, 0.1);
}

.stat-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.stat-box {
  width: 30%;
  padding: 8px;
  text-align: center;
  border-radius: 3px;
}

.stat-value {
  font-size: 18px;
  font-weight: bold;
}

.stat-label {
  font-size: 12px;
  margin-top: 5px;
}

.cyan {
  background-color: rgba(0, 211, 255, 0.2);
  color: #00d3ff;
}

.green {
  background-color: rgba(65, 255, 64, 0.2);
  color: #41ff40;
}

.purple {
  background-color: rgba(144, 90, 255, 0.2);
  color: #905aff;
}

.orange {
  background-color: rgba(255, 149, 0, 0.2);
  color: #ff9500;
}

.yellow {
  background-color: rgba(255, 252, 0, 0.2);
  color: #fffc00;
}

.blue {
  background-color: rgba(0, 149, 232, 0.2);
  color: #0095e8;
}

.red {
  background-color: rgba(255, 54, 90, 0.2);
  color: #ff365a;
}

.center-stats {
  width: 25%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.center-title {
  font-size: 16px;
  margin-bottom: 15px;
  color: #00fcff;
}

.center-numbers {
  display: flex;
  justify-content: center;
  width: 100%;
}

.center-number {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(12, 97, 148, 0.3);
  border: 1px solid #0095e8;
  color: #00fcff;
  font-size: 20px;
  font-weight: bold;
  border-radius: 5px;
  margin: 0 5px;
}

.dashboard-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
  max-width: 1800px;
  margin: 0 auto;
}

.chart-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.chart-card {
  flex: 1;
  background-color: rgba(12, 97, 148, 0.1);
  border: 1px solid #0a5299;
  border-radius: 5px;
  padding: 10px;
  height: 250px;
  overflow: hidden;
}

.chart-title {
  font-size: 14px;
  margin-bottom: 10px;
  color: #fff;
  text-align: center;
}

.chart-container {
  width: 100%;
  height: calc(100% - 30px);
}

.kpi-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.kpi-card {
  flex: 1;
  background-color: rgba(12, 97, 148, 0.2);
  border: 1px solid #0c95e8;
  border-radius: 5px;
  height: 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.kpi-title {
  font-size: 14px;
  color: #0c95e8;
}

.kpi-value {
  font-size: 24px;
  font-weight: bold;
  color: #fff;
  margin-top: 5px;
}

.bottom-charts .chart-card {
  height: 220px;
}

.visualization-charts .chart-card {
  height: 280px;
}

.data-list {
  height: calc(100% - 20px);
  overflow-y: auto;
}

.data-item {
  padding: 8px;
  border-bottom: 1px dashed rgba(12, 149, 232, 0.3);
  font-size: 12px;
}

.data-item.anomaly {
  background-color: rgba(255, 54, 90, 0.1);
  border-left: 3px solid #ff365a;
}

.data-content {
  display: flex;
  justify-content: space-between;
  margin-bottom: 3px;
}

.sensor-id {
  color: #0c95e8;
  font-family: monospace;
}

.sensor-type {
  color: #4bfffc;
}

.value {
  color: #00ff9c;
  font-weight: bold;
}

.data-meta {
  display: flex;
  justify-content: space-between;
  color: rgba(255, 255, 255, 0.6);
}

.data-region, .data-crop {
  background-color: rgba(12, 149, 232, 0.2);
  padding: 2px 5px;
  border-radius: 3px;
  margin-right: 5px;
}

.data-time {
  color: rgba(255, 255, 255, 0.4);
}

::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 3px;
}

::-webkit-scrollbar-thumb {
  background: rgba(12, 149, 232, 0.5);
  border-radius: 3px;
}

/* Add loading indicator for real-time updates */
.chart-card {
  position: relative;
}

.chart-card.loading::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 1;
}

.chart-card.loading::after {
  content: '加载中...';
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 2;
  color: #fff;
}

/* Add animation for real-time data updates */
@keyframes newDataPulse {
  0% { background-color: rgba(76, 175, 80, 0.4); }
  100% { background-color: transparent; }
}

.data-item.new-data {
  animation: newDataPulse 1.5s ease-out;
}

.anomaly {
  background-color: rgba(244, 67, 54, 0.2);
  border-left: 3px solid #f44336;
}

.connection-status {
  margin-top: 10px;
  padding: 5px 10px;
  border-radius: 5px;
  background-color: rgba(12, 97, 148, 0.2);
  color: #00fcff;
  font-size: 14px;
}

.connection-status.idle {
  background-color: rgba(12, 97, 148, 0.2);
}

.connection-status.loading {
  background-color: rgba(255, 255, 255, 0.2);
}

.connection-status.success {
  background-color: rgba(65, 255, 64, 0.2);
}

.connection-status.error {
  background-color: rgba(255, 54, 90, 0.2);
}

.retry-button {
  background: none;
  border: none;
  color: #00fcff;
  font-size: 14px;
  cursor: pointer;
  margin-left: 10px;
}

.mode-button {
  background: none;
  border: none;
  color: #00fcff;
  font-size: 14px;
  cursor: pointer;
  margin-left: 10px;
}

.server-selector {
  margin-top: 10px;
  padding: 5px;
  border-radius: 5px;
  background-color: rgba(12, 97, 148, 0.2);
  color: #00fcff;
  font-size: 14px;
}

.server-selector select {
  background: none;
  border: none;
  color: #00fcff;
  font-size: 14px;
}

.server-selector,
.server-selector select,
.connection-actions,
.mode-button {
  display: none;
}
</style> 