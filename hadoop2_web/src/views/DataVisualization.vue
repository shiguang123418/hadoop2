<template>
  <div class="data-visualization">
    <div class="page-header">
      <h2>数据可视化</h2>
      <p class="subtitle">通过直观的图表了解农业数据趋势和分布</p>
    </div>

    
    
    <!-- <div class="dashboard-grid">
      <div class="dashboard-card">
        <div class="card-header">
          <h3>产量趋势</h3>
          <div class="card-actions">
            <button class="icon-button refresh-btn" title="刷新数据" @click="refreshChart('yieldTrend')">
              <i class="refresh-icon"></i>
            </button>
            <button class="icon-button" title="更多选项">
              <i class="more-icon"></i>
            </button>
          </div>
        </div>
        <div class="chart-container">
          <canvas ref="yieldTrendChart"></canvas>
        </div>
      </div>
      
      <div class="dashboard-card">
        <div class="card-header">
          <h3>区域分布</h3>
          <div class="card-actions">
            <button class="icon-button refresh-btn" title="刷新数据" @click="refreshChart('regionDistribution')">
              <i class="refresh-icon"></i>
            </button>
            <button class="icon-button" title="更多选项">
              <i class="more-icon"></i>
            </button>
          </div>
        </div>
        <div class="chart-container">
          <canvas ref="regionDistributionChart"></canvas>
        </div>
      </div>
      
      <div class="dashboard-card">
        <div class="card-header">
          <h3>作物类型占比</h3>
          <div class="card-actions">
            <button class="icon-button refresh-btn" title="刷新数据" @click="refreshChart('cropType')">
              <i class="refresh-icon"></i>
            </button>
            <button class="icon-button" title="更多选项">
              <i class="more-icon"></i>
            </button>
          </div>
        </div>
        <div class="chart-container">
          <canvas ref="cropTypeChart"></canvas>
        </div>
      </div>
      
      <div class="dashboard-card">
        <div class="card-header">
          <h3>气候与产量关系</h3>
          <div class="card-actions">
            <button class="icon-button refresh-btn" title="刷新数据" @click="refreshChart('climateYield')">
              <i class="refresh-icon"></i>
            </button>
            <button class="icon-button" title="更多选项">
              <i class="more-icon"></i>
            </button>
          </div>
        </div>
        <div class="chart-container">
          <canvas ref="climateYieldChart"></canvas>
        </div>
      </div>
    </div> -->
    
    <!-- <div class="visualization-tools">
      <div class="tools-header">
        <h3>自定义图表</h3>
        <p>选择数据类型和图表形式，生成自定义报表</p>
      </div>
      
      <div class="chart-builder">
        <div class="chart-options">
          <div class="option-group">
            <label for="chartType">图表类型</label>
            <select id="chartType" v-model="chartType" class="form-control">
              <option value="bar">柱状图</option>
              <option value="line">折线图</option>
              <option value="pie">饼图</option>
              <option value="scatter">散点图</option>
            </select>
          </div>
          
          <div class="option-group">
            <label for="dataMetric">数据指标</label>
            <select id="dataMetric" v-model="dataMetric" class="form-control">
              <option value="yield">产量</option>
              <option value="area">种植面积</option>
              <option value="precipitation">降水量</option>
              <option value="temperature">温度</option>
            </select>
          </div>
          
          <div class="option-group">
            <label for="groupBy">分组依据</label>
            <select id="groupBy" v-model="groupBy" class="form-control">
              <option value="crop_type">作物类型</option>
              <option value="region">区域</option>
              <option value="year">年份</option>
              <option value="season">季节</option>
            </select>
          </div>
          
          <button 
            class="generate-btn" 
            @click="generateChart"
            :disabled="loading"
          >
            <span v-if="!loading">生成图表</span>
            <span v-else class="loading-spinner"></span>
          </button>
        </div>
        
        <div class="custom-chart-container">
          <canvas ref="chartCanvas"></canvas>
        </div>
      </div>
    </div> -->
    
    <!-- 气象数据可视化部分 -->
    <div class="weather-section">
      <div class="section-header">
        <h3>气象数据可视化</h3>
        <p>查看不同地区的气象数据变化趋势</p>
      </div>
      
      <div class="weather-chart-wrapper">
        <div class="weather-placeholder">
          <p>气象数据组件已移除</p>
        </div>
      </div>
    </div>
    
    <!-- 产品回归分析部分 -->
    <div class="product-regression-section">
      <div class="section-header">
        <h3>产品回归分析</h3>
        <p>分析不同环境因素对作物产量的影响</p>
      </div>
      
      <div class="product-regression-wrapper">
        <ProductRegressionChart />
      </div>
    </div>
  </div>
</template>
  
<script>
import Chart from 'chart.js/auto'
import ProductRegressionChart from '@/components/ProductRegressionChart.vue'
  
export default {
  name: 'DataVisualization',
  components: {
    ProductRegressionChart
  },
  data() {
    return {
      chartType: 'bar',
      dataMetric: 'yield',
      groupBy: 'crop_type',
      loading: false,
      chartData: null,
      chartTitle: '',
      chart: null,
      dashboardCharts: {
        yieldTrend: null,
        regionDistribution: null,
        cropType: null,
        climateYield: null
      }
    }
  },
  mounted() {
    this.initDashboardCharts()
  },
  methods: {
    async generateChart() {
      this.loading = true
      
      try {
        // Generate SQL query based on selected options
        const query = `SELECT ${this.groupBy}, AVG(${this.dataMetric}) as avg_value 
                      FROM crop_data 
                      GROUP BY ${this.groupBy} 
                      ORDER BY avg_value DESC`
        
        const response = await this.axios.post('/agriculture/query', query)
        
        // Process data for chart
        const labels = response.data.map(item => item[this.groupBy])
        const values = response.data.map(item => item.avg_value)
        
        this.chartData = {
          labels,
          datasets: [{
            label: this.getMetricLabel(this.dataMetric),
            data: values,
            backgroundColor: this.generateColors(labels.length),
            borderColor: this.chartType === 'line' ? '#3498db' : undefined,
            borderWidth: 1
          }]
        }
        
        this.chartTitle = `${this.getGroupByLabel(this.groupBy)}的${this.getMetricLabel(this.dataMetric)}对比`
        
        // Render chart
        this.$nextTick(() => {
          this.renderChart()
        })
      } catch (error) {
        console.error('Error generating chart:', error)
        alert(`生成图表失败: ${error.response?.data || error.message}`)
      } finally {
        this.loading = false
      }
    },
    renderChart() {
      const ctx = this.$refs.chartCanvas.getContext('2d')
      
      // Destroy previous chart if exists
      if (this.chart) {
        this.chart.destroy()
      }
      
      // Create new chart
      this.chart = new Chart(ctx, {
        type: this.chartType,
        data: this.chartData,
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'top',
            },
            title: {
              display: true,
              text: this.chartTitle
            }
          }
        }
      })
    },
    initDashboardCharts() {
      // Sample data for dashboard charts
      this.$nextTick(() => {
        this.renderYieldTrendChart()
        this.renderRegionDistributionChart()
        this.renderCropTypeChart()
        this.renderClimateYieldChart()
      })
    },
    refreshChart(chartType) {
      // 添加刷新动画
      const button = event.currentTarget;
      button.classList.add('refreshing');
      
      // 在实际应用中，这里应该重新获取数据
      setTimeout(() => {
        // 模拟数据变化
        switch(chartType) {
          case 'yieldTrend':
            this.dashboardCharts.yieldTrend.data.datasets[0].data = 
              this.dashboardCharts.yieldTrend.data.datasets[0].data.map(
                val => val * (0.9 + Math.random() * 0.2)
              );
            this.dashboardCharts.yieldTrend.update();
            break;
          case 'regionDistribution':
            this.dashboardCharts.regionDistribution.data.datasets[0].data = 
              this.dashboardCharts.regionDistribution.data.datasets[0].data.map(
                val => val * (0.9 + Math.random() * 0.2)
              );
            this.dashboardCharts.regionDistribution.update();
            break;
          case 'cropType':
            this.dashboardCharts.cropType.data.datasets[0].data = 
              this.dashboardCharts.cropType.data.datasets[0].data.map(
                val => val * (0.9 + Math.random() * 0.2)
              );
            this.dashboardCharts.cropType.update();
            break;
          case 'climateYield':
            this.dashboardCharts.climateYield.data.datasets.forEach(dataset => {
              dataset.data = dataset.data.map(
                val => val * (0.9 + Math.random() * 0.2)
              );
            });
            this.dashboardCharts.climateYield.update();
            break;
        }
        
        // 移除刷新动画
        setTimeout(() => {
          button.classList.remove('refreshing');
        }, 500);
      }, 800);
    },
    renderYieldTrendChart() {
      const ctx = this.$refs.yieldTrendChart.getContext('2d')
      
      // Sample data - in a real app, this would come from an API
      const data = {
        labels: ['1月', '2月', '3月', '4月', '5月', '6月'],
        datasets: [{
          label: '平均产量',
          data: [4.2, 4.5, 5.1, 5.3, 5.8, 6.2],
          borderColor: 'rgba(52, 152, 219, 1)',
          backgroundColor: 'rgba(52, 152, 219, 0.2)',
          tension: 0.3,
          fill: true
        }]
      }
      
      this.dashboardCharts.yieldTrend = new Chart(ctx, {
        type: 'line',
        data: data,
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              display: false
            }
          },
          scales: {
            y: {
              beginAtZero: false,
              grid: {
                display: true,
                color: 'rgba(200, 200, 200, 0.1)'
              }
            },
            x: {
              grid: {
                display: false
              }
            }
          }
        }
      })
    },
    renderRegionDistributionChart() {
      const ctx = this.$refs.regionDistributionChart.getContext('2d')
      
      // Sample data
      const data = {
        labels: ['北部', '南部', '东部', '西部', '中部'],
        datasets: [{
          data: [30, 25, 20, 15, 10],
          backgroundColor: [
            'rgba(52, 152, 219, 0.8)', 
            'rgba(46, 204, 113, 0.8)', 
            'rgba(243, 156, 18, 0.8)', 
            'rgba(231, 76, 60, 0.8)', 
            'rgba(155, 89, 182, 0.8)'
          ],
          borderWidth: 0
        }]
      }
      
      this.dashboardCharts.regionDistribution = new Chart(ctx, {
        type: 'pie',
        data: data,
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: {
              position: 'right',
              labels: {
                boxWidth: 15,
                padding: 15
              }
            }
          }
        }
      })
    },
    renderCropTypeChart() {
      const ctx = this.$refs.cropTypeChart.getContext('2d')
      
      // Sample data
      const data = {
        labels: ['水稻', '小麦', '玉米', '大豆', '蔬菜'],
        datasets: [{
          label: '种植面积（万亩）',
          data: [120, 90, 80, 50, 40],
          backgroundColor: 'rgba(46, 204, 113, 0.7)',
          borderColor: 'rgba(46, 204, 113, 1)',
          borderWidth: 1
        }]
      }
      
      this.dashboardCharts.cropType = new Chart(ctx, {
        type: 'bar',
        data: data,
        options: {
          responsive: true,
          maintainAspectRatio: false,
          indexAxis: 'y',
          plugins: {
            legend: {
              display: false
            }
          },
          scales: {
            x: {
              beginAtZero: true,
              grid: {
                display: true,
                color: 'rgba(200, 200, 200, 0.1)'
              }
            },
            y: {
              grid: {
                display: false
              }
            }
          }
        }
      })
    },
    renderClimateYieldChart() {
      const ctx = this.$refs.climateYieldChart.getContext('2d')
      
      // Sample data
      const data = {
        labels: ['1月', '2月', '3月', '4月', '5月', '6月'],
        datasets: [
          {
            label: '产量',
            data: [4.2, 4.5, 5.1, 5.3, 5.8, 6.2],
            borderColor: 'rgba(52, 152, 219, 1)',
            backgroundColor: 'rgba(52, 152, 219, 0.1)',
            type: 'line',
            fill: true,
            yAxisID: 'y',
            tension: 0.3
          },
          {
            label: '降水量',
            data: [20, 25, 30, 35, 40, 50],
            backgroundColor: 'rgba(243, 156, 18, 0.7)',
            borderWidth: 0,
            type: 'bar',
            yAxisID: 'y1'
          }
        ]
      }
      
      this.dashboardCharts.climateYield = new Chart(ctx, {
        type: 'bar',
        data: data,
        options: {
          responsive: true,
          maintainAspectRatio: false,
          interaction: {
            mode: 'index',
            intersect: false,
          },
          plugins: {
            legend: {
              position: 'top',
              labels: {
                boxWidth: 12,
                padding: 15
              }
            }
          },
          scales: {
            y: {
              type: 'linear',
              display: true,
              position: 'left',
              title: {
                display: true,
                text: '产量',
                color: 'rgba(52, 152, 219, 1)'
              },
              grid: {
                display: false
              }
            },
            y1: {
              type: 'linear',
              display: true,
              position: 'right',
              title: {
                display: true,
                text: '降水量(mm)',
                color: 'rgba(243, 156, 18, 1)'
              },
              grid: {
                display: false
              }
            }
          }
        }
      })
    },
    getMetricLabel(metric) {
      const labels = {
        yield: '产量',
        area: '种植面积',
        precipitation: '降水量',
        temperature: '温度'
      }
      return labels[metric] || metric
    },
    getGroupByLabel(groupBy) {
      const labels = {
        crop_type: '作物类型',
        region: '区域',
        year: '年份',
        season: '季节'
      }
      return labels[groupBy] || groupBy
    },
    generateColors(count) {
      const colors = [
        'rgba(52, 152, 219, 0.7)', // blue
        'rgba(46, 204, 113, 0.7)', // green
        'rgba(243, 156, 18, 0.7)', // yellow
        'rgba(231, 76, 60, 0.7)',  // red
        'rgba(155, 89, 182, 0.7)'  // purple
      ]
      
      if (count <= colors.length) {
        return colors.slice(0, count)
      }
      
      // If we need more colors than available in our predefined array
      const result = [...colors]
      for (let i = colors.length; i < count; i++) {
        result.push(`rgba(${Math.floor(Math.random() * 255)}, ${Math.floor(Math.random() * 255)}, ${Math.floor(Math.random() * 255)}, 0.7)`)
      }
      
      return result
    }
  }
}
</script>
  
<style scoped>
.data-visualization {
  padding: 1rem 0;
}

.page-header {
  text-align: center;
  margin-bottom: 2rem;
}

.page-header h2 {
  font-size: 2rem;
  color: var(--text-color);
  margin-bottom: 0.5rem;
}

.subtitle {
  color: var(--text-light);
  font-size: 1.1rem;
}

.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(450px, 1fr));
  gap: 1.5rem;
  margin-bottom: 3rem;
}

.dashboard-card {
  background-color: var(--bg-light);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  overflow: hidden;
  transition: var(--transition);
}

.dashboard-card:hover {
  box-shadow: var(--shadow-lg);
  transform: translateY(-3px);
}

.card-header {
  padding: 1rem 1.5rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid var(--border-color);
}

.card-header h3 {
  margin: 0;
  font-size: 1.2rem;
  color: var(--text-color);
}

.card-actions {
  display: flex;
  gap: 0.5rem;
}

.icon-button {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: var(--transition);
}

.icon-button:hover {
  background-color: var(--bg-dark);
}

.refresh-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%233498db'%3E%3Cpath d='M17.65 6.35C16.2 4.9 14.21 4 12 4c-4.42 0-7.99 3.58-7.99 8s3.57 8 7.99 8c3.73 0 6.84-2.55 7.73-6h-2.08c-.82 2.33-3.04 4-5.65 4-3.31 0-6-2.69-6-6s2.69-6 6-6c1.66 0 3.14.69 4.22 1.78L13 11h7V4l-2.35 2.35z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  transition: transform 0.5s ease;
}

.refresh-btn.refreshing .refresh-icon {
  animation: spin 1s infinite linear;
}

@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.more-icon {
  display: inline-block;
  width: 16px;
  height: 16px;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%232c3e50'%3E%3Cpath d='M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.chart-container {
  height: 250px;
  padding: 1.5rem;
  position: relative;
}

.visualization-tools {
  background-color: var(--bg-light);
  border-radius: var(--border-radius);
  box-shadow: var(--shadow);
  padding: 2rem;
  margin-bottom: 2rem;
}

.tools-header {
  margin-bottom: 2rem;
  text-align: center;
}

.tools-header h3 {
  font-size: 1.5rem;
  margin-bottom: 0.5rem;
  color: var(--text-color);
}

.tools-header p {
  color: var(--text-light);
}

.chart-builder {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 2rem;
}

.chart-options {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.option-group {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.option-group label {
  color: var(--text-color);
  font-weight: 500;
}

.form-control {
  padding: 0.75rem 1rem;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  background-color: var(--bg-light);
  transition: var(--transition);
}

.form-control:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.15);
}

.generate-btn {
  margin-top: 1rem;
  padding: 0.75rem 1rem;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
  display: flex;
  align-items: center;
  justify-content: center;
}

.generate-btn:hover {
  background-color: var(--primary-dark);
  transform: translateY(-2px);
}

.generate-btn:active {
  transform: translateY(0);
}

.generate-btn:disabled {
  background-color: rgba(52, 152, 219, 0.6);
  cursor: not-allowed;
}

.loading-spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 1s infinite linear;
}

.custom-chart-container {
  height: 400px;
  background-color: rgba(255, 255, 255, 0.5);
  border-radius: var(--border-radius);
  border: 1px solid var(--border-color);
  padding: 1rem;
}

@media (max-width: 992px) {
  .dashboard-grid {
    grid-template-columns: 1fr;
  }
  
  .chart-builder {
    grid-template-columns: 1fr;
  }
  
  .custom-chart-container {
    height: 300px;
  }
}

/* 气象图表部分样式 */
.weather-section {
  margin-top: 40px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.section-header {
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 1.4rem;
  margin-bottom: 8px;
  color: #333;
}

.section-header p {
  color: #666;
  font-size: 0.95rem;
}

.weather-chart-wrapper {
  margin-top: 20px;
}

.product-regression-section {
  margin-top: 40px;
  background-color: #f9f9f9;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
}

.product-regression-wrapper {
  margin-top: 20px;
}

.section-header {
  margin-bottom: 20px;
}

.section-header h3 {
  font-size: 1.5rem;
  color: #2c3e50;
  margin-bottom: 8px;
}

.section-header p {
  color: #666;
  margin: 0;
}

.weather-placeholder {
  background-color: #f5f5f5;
  border: 1px dashed #ccc;
  border-radius: 8px;
  padding: 30px;
  text-align: center;
  color: #666;
  min-height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.weather-placeholder p {
  font-size: 1.1rem;
  margin: 0;
}
</style>