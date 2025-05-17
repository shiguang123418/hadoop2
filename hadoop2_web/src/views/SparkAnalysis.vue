<template>
  <div class="spark-analysis">
    <h2>Spark农业数据分析</h2>
    
    <div class="tab-container">
      <div class="tabs">
        <div class="tab" :class="{ active: activeTab === 'statistics' }" @click="activeTab = 'statistics'">统计分析</div>
        <div class="tab" :class="{ active: activeTab === 'correlation' }" @click="activeTab = 'correlation'">相关性分析</div>
        <div class="tab" :class="{ active: activeTab === 'forecast' }" @click="activeTab = 'forecast'">产量预测</div>
        <div class="tab" :class="{ active: activeTab === 'optimal' }" @click="activeTab = 'optimal'">最佳生长条件</div>
        <div class="tab" :class="{ active: activeTab === 'cluster' }" @click="activeTab = 'cluster'">聚类分析</div>
      </div>
      
      <!-- 作物选择 -->
      <div class="crop-selector">
        <label for="crop-select">选择作物：</label>
        <select id="crop-select" v-model="selectedCrop" @change="handleCropChange">
          <option value="">-- 请选择作物 --</option>
          <option v-for="crop in crops" :key="crop" :value="crop">{{ crop }}</option>
        </select>
        
        <button class="btn" @click="loadData" :disabled="!selectedCrop || loading">
          {{ loading ? '加载中...' : '分析数据' }}
        </button>
      </div>
      
      <!-- 加载状态 -->
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <span>正在分析数据...</span>
      </div>
      
      <!-- 错误信息 -->
      <div v-if="error" class="error-message">
        <p>{{ error }}</p>
      </div>
      
      <!-- 统计分析 -->
      <div v-show="activeTab === 'statistics'" class="tab-content">
        <h3>{{ selectedCrop }}产量统计分析</h3>
        
        <div v-if="statistics" class="statistics-container">
          <div class="stats-summary">
            <div class="stat-item">
              <div class="stat-label">平均产量</div>
              <div class="stat-value">{{ formatNumber(statistics['平均产量']) }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">最低产量</div>
              <div class="stat-value">{{ formatNumber(statistics['最低产量']) }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">最高产量</div>
              <div class="stat-value">{{ formatNumber(statistics['最高产量']) }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">产量标准差</div>
              <div class="stat-value">{{ formatNumber(statistics['产量标准差']) }}</div>
            </div>
            <div class="stat-item">
              <div class="stat-label">数据量</div>
              <div class="stat-value">{{ statistics['数据量'] }}</div>
            </div>
          </div>
          
          <div class="chart-container">
            <h4>年度产量趋势</h4>
            <div ref="yearlyTrendChart" class="chart"></div>
          </div>
        </div>
      </div>
      
      <!-- 相关性分析 -->
      <div v-show="activeTab === 'correlation'" class="tab-content">
        <h3>{{ selectedCrop }}产量相关因素分析</h3>
        
        <div v-if="correlation && correlation.length > 0" class="correlation-container">
          <div class="chart-container">
            <div ref="correlationChart" class="chart"></div>
          </div>
          
          <div class="correlation-table">
            <table>
              <thead>
                <tr>
                  <th>环境因素</th>
                  <th>相关系数</th>
                  <th>相关性程度</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in correlation" :key="index">
                  <td>{{ item.factor }}</td>
                  <td>{{ formatNumber(item.correlation) }}</td>
                  <td>{{ getCorrelationLevel(item.correlation) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      
      <!-- 产量预测 -->
      <div v-show="activeTab === 'forecast'" class="tab-content">
        <h3>{{ selectedCrop }}产量预测</h3>
        
        <div class="forecast-options">
          <label>预测年数：</label>
          <input type="number" v-model.number="forecastPeriods" min="1" max="10" />
          <button class="btn" @click="loadForecastData" :disabled="loading">预测</button>
        </div>
        
        <div v-if="forecast && forecast.length > 0" class="forecast-container">
          <div class="chart-container">
            <div ref="forecastChart" class="chart"></div>
          </div>
          
          <div class="forecast-table">
            <table>
              <thead>
                <tr>
                  <th>年份</th>
                  <th>预测产量</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in forecast" :key="index">
                  <td>{{ item.year }}</td>
                  <td>{{ formatNumber(item.forecasted_production) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      
      <!-- 最佳生长条件 -->
      <div v-show="activeTab === 'optimal'" class="tab-content">
        <h3>{{ selectedCrop }}最佳生长条件</h3>
        
        <div v-if="optimalConditions" class="optimal-container">
          <div class="optimal-summary">
            <p>基于历史数据分析，{{ selectedCrop }}的最佳生长条件如下：</p>
            
            <div class="condition-item">
              <div class="condition-label">温度范围</div>
              <div class="condition-value">
                {{ formatNumber(optimalConditions['温度范围']['最小值']) }}°C ~ 
                {{ formatNumber(optimalConditions['温度范围']['最大值']) }}°C
              </div>
            </div>
            
            <div class="condition-item">
              <div class="condition-label">降雨量范围</div>
              <div class="condition-value">
                {{ formatNumber(optimalConditions['降雨量范围']['最小值']) }}mm ~ 
                {{ formatNumber(optimalConditions['降雨量范围']['最大值']) }}mm
              </div>
            </div>
            
            <div class="condition-item">
              <div class="condition-label">土壤PH值范围</div>
              <div class="condition-value">
                {{ formatNumber(optimalConditions['PH值范围']['最小值']) }} ~ 
                {{ formatNumber(optimalConditions['PH值范围']['最大值']) }}
              </div>
            </div>
            
            <div class="condition-item">
              <div class="condition-label">预期平均产量</div>
              <div class="condition-value">{{ formatNumber(optimalConditions['预期平均产量']) }}</div>
            </div>
          </div>
          
          <div class="chart-container">
            <div ref="optimalChart" class="chart"></div>
          </div>
        </div>
      </div>
      
      <!-- 聚类分析 -->
      <div v-show="activeTab === 'cluster'" class="tab-content">
        <h3>农作物聚类分析</h3>
        
        <div class="cluster-options">
          <label>聚类数量：</label>
          <input type="number" v-model.number="clusterCount" min="2" max="10" />
          <button class="btn" @click="loadClusterData" :disabled="loading">分析</button>
        </div>
        
        <div v-if="clusterData && clusterData['聚类结果']" class="cluster-container">
          <div class="cluster-table">
            <table>
              <thead>
                <tr>
                  <th>聚类ID</th>
                  <th>作物</th>
                  <th>平均温度</th>
                  <th>平均降雨量</th>
                  <th>平均PH值</th>
                  <th>平均产量</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(cluster, index) in clusterData['聚类结果']" :key="index">
                  <td>{{ cluster['聚类ID'] }}</td>
                  <td>{{ cluster['作物'].join(', ') }}</td>
                  <td>{{ formatNumber(cluster['平均温度']) }}°C</td>
                  <td>{{ formatNumber(cluster['平均降雨量']) }}mm</td>
                  <td>{{ formatNumber(cluster['平均PH值']) }}</td>
                  <td>{{ formatNumber(cluster['平均产量']) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <div class="chart-container">
            <div ref="clusterChart" class="chart"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import * as echarts from 'echarts';

export default {
  name: 'SparkAnalysis',
  data() {
    return {
      activeTab: 'statistics',
      selectedCrop: '',
      crops: [],
      loading: false,
      error: null,
      
      // 数据
      statistics: null,
      correlation: null,
      forecast: null,
      optimalConditions: null,
      clusterData: null,
      
      // 选项
      forecastPeriods: 5,
      clusterCount: 3,
      
      // 图表实例
      charts: {}
    };
  },
  
  mounted() {
    this.loadCrops();
  },
  
  methods: {
    async loadCrops() {
      try {
        this.loading = true;
        const response = await axios.get('/production/crops');
        this.crops = response.data;
        this.loading = false;
      } catch (error) {
        this.error = '加载作物列表失败: ' + (error.response?.data?.error || error.message);
        this.loading = false;
      }
    },
    
    handleCropChange() {
      this.statistics = null;
      this.correlation = null;
      this.forecast = null;
      this.optimalConditions = null;
      
      // 根据当前标签加载相应数据
      if (this.selectedCrop) {
        this.loadData();
      }
    },
    
    async loadData() {
      if (!this.selectedCrop) return;
      
      this.error = null;
      this.loading = true;
      
      try {
        switch (this.activeTab) {
          case 'statistics':
            await this.loadStatistics();
            break;
          case 'correlation':
            await this.loadCorrelation();
            break;
          case 'forecast':
            await this.loadForecastData();
            break;
          case 'optimal':
            await this.loadOptimalConditions();
            break;
          case 'cluster':
            await this.loadClusterData();
            break;
        }
      } catch (error) {
        this.error = '加载数据失败: ' + (error.response?.data?.error || error.message);
      } finally {
        this.loading = false;
      }
    },
    
    async loadStatistics() {
      const response = await axios.get(`/spark/statistics?crop=${this.selectedCrop}`);
      this.statistics = response.data;
      
      if (this.statistics && !this.statistics.error) {
        this.$nextTick(() => {
          this.renderYearlyTrendChart();
        });
      }
    },
    
    async loadCorrelation() {
      const response = await axios.get(`/spark/correlation?crop=${this.selectedCrop}`);
      this.correlation = response.data;
      
      if (this.correlation && !this.correlation.error) {
        this.$nextTick(() => {
          this.renderCorrelationChart();
        });
      }
    },
    
    async loadForecastData() {
      const response = await axios.get(`/spark/forecast?crop=${this.selectedCrop}&periods=${this.forecastPeriods}`);
      this.forecast = response.data;
      
      if (this.forecast && !this.forecast.error) {
        this.$nextTick(() => {
          this.renderForecastChart();
        });
      }
    },
    
    async loadOptimalConditions() {
      const response = await axios.get(`/spark/optimal-conditions?crop=${this.selectedCrop}`);
      this.optimalConditions = response.data;
      
      if (this.optimalConditions && !this.optimalConditions.error) {
        this.$nextTick(() => {
          this.renderOptimalChart();
        });
      }
    },
    
    async loadClusterData() {
      const response = await axios.get(`/spark/clusters?clusters=${this.clusterCount}`);
      this.clusterData = response.data;
      
      if (this.clusterData && !this.clusterData.error) {
        this.$nextTick(() => {
          this.renderClusterChart();
        });
      }
    },
    
    renderYearlyTrendChart() {
      if (!this.statistics || !this.statistics['年度趋势']) return;
      
      const chartDom = this.$refs.yearlyTrendChart;
      if (!chartDom) return;
      
      const chart = echarts.init(chartDom);
      this.charts.yearlyTrend = chart;
      
      const years = this.statistics['年度趋势'].map(item => item['年份']);
      const values = this.statistics['年度趋势'].map(item => item['平均产量']);
      
      const option = {
        title: {
          text: `${this.selectedCrop}历年产量趋势`,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: years,
          name: '年份'
        },
        yAxis: {
          type: 'value',
          name: '产量'
        },
        series: [
          {
            name: '平均产量',
            type: 'line',
            data: values,
            markPoint: {
              data: [
                { type: 'max', name: '最大值' },
                { type: 'min', name: '最小值' }
              ]
            },
            smooth: true
          }
        ],
        grid: {
          left: '5%',
          right: '5%',
          bottom: '10%',
          containLabel: true
        }
      };
      
      chart.setOption(option);
      
      // 响应式处理
      window.addEventListener('resize', () => chart.resize());
    },
    
    renderCorrelationChart() {
      if (!this.correlation || this.correlation.length === 0) return;
      
      const chartDom = this.$refs.correlationChart;
      if (!chartDom) return;
      
      const chart = echarts.init(chartDom);
      this.charts.correlation = chart;
      
      const factors = this.correlation.map(item => item.factor);
      const values = this.correlation.map(item => item.correlation);
      
      // 根据相关系数的正负设置不同颜色
      const colors = values.map(val => val >= 0 ? '#5470c6' : '#ee6666');
      
      const option = {
        title: {
          text: `${this.selectedCrop}产量相关性分析`,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: params => {
            const data = params[0];
            const val = data.value;
            const factor = data.name;
            return `${factor}: ${this.formatNumber(val)}<br>相关性: ${this.getCorrelationLevel(val)}`;
          }
        },
        xAxis: {
          type: 'category',
          data: factors,
          name: '环境因素',
          axisLabel: {
            interval: 0,
            rotate: 30
          }
        },
        yAxis: {
          type: 'value',
          name: '相关系数',
          min: -1,
          max: 1
        },
        series: [
          {
            name: '相关系数',
            type: 'bar',
            data: values,
            itemStyle: {
              color: function(params) {
                return colors[params.dataIndex];
              }
            }
          }
        ],
        grid: {
          left: '5%',
          right: '5%',
          bottom: '15%',
          containLabel: true
        }
      };
      
      chart.setOption(option);
      
      // 响应式处理
      window.addEventListener('resize', () => chart.resize());
    },
    
    renderForecastChart() {
      if (!this.forecast || this.forecast.length === 0) return;
      
      const chartDom = this.$refs.forecastChart;
      if (!chartDom) return;
      
      const chart = echarts.init(chartDom);
      this.charts.forecast = chart;
      
      const years = this.forecast.map(item => item.year);
      const values = this.forecast.map(item => item.forecasted_production);
      
      const option = {
        title: {
          text: `${this.selectedCrop}未来产量预测`,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: years,
          name: '年份'
        },
        yAxis: {
          type: 'value',
          name: '预测产量'
        },
        series: [
          {
            name: '预测产量',
            type: 'line',
            data: values,
            smooth: true,
            lineStyle: {
              color: '#5470c6'
            },
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [{
                  offset: 0, color: 'rgba(84, 112, 198, 0.5)'
                }, {
                  offset: 1, color: 'rgba(84, 112, 198, 0.1)'
                }]
              }
            }
          }
        ],
        grid: {
          left: '5%',
          right: '5%',
          bottom: '10%',
          containLabel: true
        }
      };
      
      chart.setOption(option);
      
      // 响应式处理
      window.addEventListener('resize', () => chart.resize());
    },
    
    renderOptimalChart() {
      if (!this.optimalConditions) return;
      
      const chartDom = this.$refs.optimalChart;
      if (!chartDom) return;
      
      const chart = echarts.init(chartDom);
      this.charts.optimal = chart;
      
      const temperature = [
        this.optimalConditions['温度范围']['最小值'],
        this.optimalConditions['温度范围']['最大值']
      ];
      const rainfall = [
        this.optimalConditions['降雨量范围']['最小值'],
        this.optimalConditions['降雨量范围']['最大值']
      ];
      const ph = [
        this.optimalConditions['PH值范围']['最小值'],
        this.optimalConditions['PH值范围']['最大值']
      ];
      
      const option = {
        title: {
          text: `${this.selectedCrop}最佳生长条件范围`,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            const data = params[0];
            return `${data.name}: ${data.value[0]} ~ ${data.value[1]}`;
          }
        },
        grid: {
          left: '5%',
          right: '15%',
          bottom: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          scale: true,
          name: '数值范围',
          nameLocation: 'middle',
          nameGap: 30
        },
        yAxis: {
          type: 'category',
          data: ['温度(°C)', '降雨量(mm)', 'PH值'],
          name: '环境因素',
          nameLocation: 'start'
        },
        series: [
          {
            name: '最佳范围',
            type: 'custom',
            renderItem: (params, api) => {
              const categoryIndex = api.value(0);
              const start = api.coord([api.value(1), categoryIndex]);
              const end = api.coord([api.value(2), categoryIndex]);
              const height = api.size([0, 1])[1] * 0.6;
              
              const rectShape = echarts.graphic.clipRectByRect(
                {
                  x: start[0],
                  y: start[1] - height / 2,
                  width: end[0] - start[0],
                  height: height
                },
                {
                  x: params.coordSys.x,
                  y: params.coordSys.y,
                  width: params.coordSys.width,
                  height: params.coordSys.height
                }
              );
              
              return rectShape && {
                type: 'rect',
                shape: rectShape,
                style: api.style({
                  fill: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                    { offset: 0, color: '#5470c6' },
                    { offset: 1, color: '#91cc75' }
                  ]),
                  stroke: '#5470c6'
                })
              };
            },
            data: [
              [0, temperature[0], temperature[1]],
              [1, rainfall[0], rainfall[1]],
              [2, ph[0], ph[1]]
            ],
            z: 100
          }
        ]
      };
      
      chart.setOption(option);
      
      // 响应式处理
      window.addEventListener('resize', () => chart.resize());
    },
    
    renderClusterChart() {
      if (!this.clusterData || !this.clusterData['聚类结果']) return;
      
      const chartDom = this.$refs.clusterChart;
      if (!chartDom) return;
      
      const chart = echarts.init(chartDom);
      this.charts.cluster = chart;
      
      const clusters = this.clusterData['聚类结果'];
      const option = {
        title: {
          text: '农作物聚类分析',
          left: 'center'
        },
        tooltip: {
          trigger: 'item',
          formatter: function(params) {
            const data = params.data;
            return `聚类${data[3]}: ${data[4].join(', ')}<br>
                    平均温度: ${data[0].toFixed(2)}°C<br>
                    平均降雨量: ${data[1].toFixed(2)}mm<br>
                    平均PH值: ${data[2].toFixed(2)}`;
          }
        },
        grid: {
          left: '10%',
          right: '10%',
          bottom: '15%',
          containLabel: true
        },
        xAxis: {
          type: 'value',
          name: '平均温度 (°C)',
          nameLocation: 'middle',
          nameGap: 30
        },
        yAxis: {
          type: 'value',
          name: '平均降雨量 (mm)',
          nameLocation: 'middle',
          nameGap: 30
        },
        series: [
          {
            name: '聚类',
            type: 'scatter',
            data: clusters.map(cluster => [
              cluster['平均温度'],
              cluster['平均降雨量'],
              cluster['平均PH值'],
              cluster['聚类ID'],
              cluster['作物'],
              cluster['记录数']
            ]),
            symbolSize: function(data) {
              // 根据记录数设置气泡大小
              return Math.sqrt(data[5]) * 5 + 10;
            },
            emphasis: {
              focus: 'series',
              label: {
                show: true,
                formatter: function(params) {
                  return `聚类${params.data[3]}`;
                },
                position: 'top'
              }
            },
            itemStyle: {
              opacity: 0.8
            }
          }
        ],
        visualMap: {
          type: 'continuous',
          min: 0,
          max: 14,
          dimension: 2, // 映射到第3个维度，即PH值
          orient: 'vertical',
          right: 0,
          top: 'center',
          text: ['高PH值', '低PH值'],
          calculable: true,
          inRange: {
            color: ['#f2c31a', '#24b7f2']
          }
        }
      };
      
      chart.setOption(option);
      
      // 响应式处理
      window.addEventListener('resize', () => chart.resize());
    },
    
    formatNumber(value) {
      if (value === undefined || value === null) return '-';
      return Number(value).toFixed(2);
    },
    
    getCorrelationLevel(value) {
      const absValue = Math.abs(value);
      if (absValue >= 0.8) return '强相关';
      if (absValue >= 0.5) return '中度相关';
      if (absValue >= 0.3) return '弱相关';
      return '几乎无相关';
    }
  },
  
  watch: {
    activeTab() {
      // 切换标签时加载对应数据
      if (this.selectedCrop) {
        this.loadData();
      }
    }
  },
  
  beforeDestroy() {
    // 销毁所有图表实例，避免内存泄漏
    Object.values(this.charts).forEach(chart => {
      if (chart && chart.dispose) {
        chart.dispose();
      }
    });
    
    // 移除窗口调整事件监听
    window.removeEventListener('resize', this.handleResize);
  }
};
</script>

<style scoped>
.spark-analysis {
  padding: 20px;
  background-color: #f9f9f9;
  min-height: calc(100vh - 60px);
}

h2 {
  text-align: center;
  margin-bottom: 20px;
  color: #333;
}

.tab-container {
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.tabs {
  display: flex;
  margin-bottom: 20px;
  border-bottom: 1px solid #eee;
}

.tab {
  padding: 10px 20px;
  cursor: pointer;
  border-bottom: 3px solid transparent;
  transition: all 0.3s ease;
  font-weight: 500;
}

.tab:hover {
  color: #1890ff;
}

.tab.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
}

.crop-selector {
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  gap: 10px;
}

.crop-selector select {
  padding: 8px 12px;
  border-radius: 4px;
  border: 1px solid #ccc;
  min-width: 200px;
}

.btn {
  background-color: #1890ff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.btn:hover {
  background-color: #40a9ff;
}

.btn:disabled {
  background-color: #a0cfff;
  cursor: not-allowed;
}

.loading {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 40px;
  flex-direction: column;
}

.spinner {
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-left-color: #1890ff;
  border-radius: 50%;
  width: 40px;
  height: 40px;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-message {
  padding: 10px;
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #ff4d4f;
  margin-bottom: 20px;
}

.tab-content {
  min-height: 400px;
}

h3 {
  margin-bottom: 20px;
  color: #333;
}

.chart-container {
  margin-top: 20px;
}

.chart {
  width: 100%;
  height: 400px;
}

.statistics-container, .correlation-container, .forecast-container, .optimal-container, .cluster-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-summary {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 15px;
}

.stat-item {
  background: linear-gradient(to bottom right, #f6f8ff, #f0f5ff);
  border-radius: 8px;
  padding: 15px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
}

.stat-label {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.stat-value {
  font-size: 24px;
  font-weight: 500;
  color: #333;
}

table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 20px;
}

th, td {
  padding: 12px 15px;
  text-align: left;
  border-bottom: 1px solid #eee;
}

th {
  background-color: #f5f5f5;
  font-weight: 500;
}

.correlation-table, .forecast-table, .cluster-table {
  margin-top: 20px;
  overflow-x: auto;
}

.forecast-options, .cluster-options {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.forecast-options input, .cluster-options input {
  padding: 8px 12px;
  border-radius: 4px;
  border: 1px solid #ccc;
  width: 80px;
}

.optimal-summary {
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
  border-radius: 8px;
  padding: 20px;
}

.condition-item {
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.condition-label {
  min-width: 120px;
  font-weight: 500;
}

.condition-value {
  font-size: 16px;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .tabs {
    overflow-x: auto;
    white-space: nowrap;
  }
  
  .tab {
    padding: 10px 12px;
  }
  
  .stats-summary {
    grid-template-columns: 1fr;
  }
}
</style> 