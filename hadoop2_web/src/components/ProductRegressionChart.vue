<template>
  <div class="product-regression-chart">
    <div class="chart-controls">
      <div class="control-group">
        <label for="cropType">作物类型</label>
        <select id="cropType" v-model="selectedCrop" class="form-control" @change="fetchData" :disabled="loading">
          <option v-for="crop in crops" :key="crop" :value="crop">{{ crop }}</option>
        </select>
      </div>
      
      <div class="control-group">
        <label for="factorType">影响因素</label>
        <select id="factorType" v-model="selectedFactor" class="form-control" @change="updateChart" :disabled="loading">
          <option value="Rainfall">降雨量</option>
          <option value="Temperature">温度</option>
          <option value="Ph">土壤酸碱度</option>
        </select>
      </div>
      
      <div class="control-group">
        <label>&nbsp;</label>
        <button class="generate-btn" @click="fetchData" :disabled="loading">
          <span v-if="!loading">生成图表</span>
          <span v-else class="loading-spinner"></span>
        </button>
      </div>
    </div>
    
    <div v-if="error" class="error-message">
      {{ error }}
    </div>
    
    <div class="chart-container" :class="{ 'loading-overlay': loading }">
      <canvas ref="scatterChart"></canvas>
      <div v-if="loading" class="loading-indicator">
        <div class="spinner"></div>
        <p>加载数据中...</p>
      </div>
    </div>
    
    <div class="regression-info" v-if="regressionInfo">
      <h4>回归分析结果</h4>
      <p><strong>相关系数 (R):</strong> {{ regressionInfo.correlation.toFixed(4) }}</p>
      <p><strong>回归方程:</strong> y = {{ regressionInfo.slope.toFixed(4) }}x + {{ regressionInfo.intercept.toFixed(4) }}</p>
      <p><strong>决定系数 (R²):</strong> {{ regressionInfo.r2.toFixed(4) }}</p>
    </div>
  </div>
</template>

<script>
import Chart from 'chart.js/auto'
import axios from 'axios'

export default {
  name: 'ProductRegressionChart',
  data() {
    return {
      crops: [],
      selectedCrop: '',
      selectedFactor: 'Rainfall',
      chartData: null,
      chart: null,
      regressionInfo: null,
      loading: false,
      error: null
    }
  },
  async mounted() {
    try {
      await this.fetchCrops()
      if (this.crops.length > 0) {
        this.selectedCrop = this.crops[0]
        await this.fetchData()
      }
    } catch (error) {
      console.error('初始化图表失败:', error)
      this.error = '加载数据失败，请检查网络连接或稍后重试'
    }
  },
  methods: {
    async fetchCrops() {
      this.loading = true
      try {
        console.log('正在获取作物类型...')
        const response = await axios.get('/production/crops')
        console.log('获取作物类型成功:', response.data)
        this.crops = response.data || []
        this.error = null
      } catch (error) {
        console.error('获取作物类型失败:', error)
        this.error = '获取作物类型失败，请检查网络连接或稍后重试'
        this.crops = ['Bajra', 'Rice', 'Wheat', 'Maize'] // 使用示例数据
      } finally {
        this.loading = false
      }
    },
    async fetchData() {
      this.loading = true
      this.error = null
      try {
        if (!this.selectedCrop) {
          this.error = '请先选择作物类型'
          this.loading = false
          return
        }

        console.log(`正在获取作物 ${this.selectedCrop} 的数据...`)
        this.error = '正在加载数据，处理大量数据可能需要一些时间...'
        
        const response = await axios.get(`/production/data?crop=${this.selectedCrop}&limit=100`)
        console.log('获取产量数据成功:', response.data)
        
        if (!response.data || 
            !response.data.Rainfall || 
            !response.data.Temperature || 
            !response.data.Ph || 
            !response.data.Production) {
          console.warn('返回的数据格式不完整:', response.data)
          throw new Error('返回的数据格式不完整')
        }
        
        this.chartData = JSON.parse(JSON.stringify(response.data))
        this.error = null
        
        if (response.data.Rainfall.length > 100) {
          console.log(`加载了大量数据: ${response.data.Rainfall.length}条记录`)
          this.error = `成功加载${response.data.Rainfall.length}条数据，正在渲染图表...`
        }
        
        this.$nextTick(() => {
          this.updateChart()
        })
      } catch (error) {
        console.error('获取产量数据失败:', error)
        this.error = '获取产量数据失败，使用示例数据进行展示'
        
        this.chartData = this.getExampleData()
        this.$nextTick(() => {
          this.updateChart()
        })
      } finally {
        this.loading = false
      }
    },
    getExampleData() {
      // 示例数据，用于API失败时显示
      return {
        "Rainfall": [400.15, 412.23, 425.78, 436.12, 450.87, 465.32, 478.91, 492.34, 505.67, 518.23],
        "Temperature": [20, 21, 22, 23, 24, 25, 26, 27, 28, 29],
        "Ph": [6.2, 6.3, 6.5, 6.7, 6.8, 7.0, 7.1, 7.3, 7.5, 7.7],
        "Production": [0.8, 1.2, 1.5, 1.7, 2.0, 2.1, 2.3, 2.4, 2.6, 2.8]
      }
    },
    updateChart() {
      try {
        if (!this.chartData) {
          console.error('没有数据可以渲染图表')
          this.error = '没有数据可以渲染图表'
          return
        }

        console.log('更新图表中...')
        const factor = this.selectedFactor
        const xValues = this.chartData[factor]
        const yValues = this.chartData.Production
        
        if (!xValues || !yValues || xValues.length === 0 || yValues.length === 0) {
          console.error('数据不完整，无法绘制图表', {
            factor,
            xValuesLength: xValues?.length,
            yValuesLength: yValues?.length
          })
          this.error = '数据不完整，无法绘制图表'
          return
        }
        
        // 检查数据类型是否正确
        if (!xValues.every(val => typeof val === 'number') || 
            !yValues.every(val => typeof val === 'number')) {
          console.error('数据类型错误，无法绘制图表', {
            xValuesType: typeof xValues[0],
            yValuesType: typeof yValues[0]
          })
          this.error = '数据类型错误，无法绘制图表'
          return
        }
        
        // 数据抽样，限制数据点数量
        let sampledData = this.sampleData(xValues, yValues, 100)
        let sampledXValues = sampledData.xValues
        let sampledYValues = sampledData.yValues
        
        // 计算回归线
        try {
          this.calculateRegression(sampledXValues, sampledYValues)
        } catch (error) {
          console.error('计算回归线失败:', error)
          this.error = '计算回归线失败，但仍然显示数据点'
          // 继续显示数据点，即使回归计算失败
        }
        
        // 生成回归线的数据点
        let regressionPoints = []
        if (this.regressionInfo) {
          const minX = Math.min(...sampledXValues)
          const maxX = Math.max(...sampledXValues)
          regressionPoints = [
            { x: minX, y: this.regressionInfo.slope * minX + this.regressionInfo.intercept },
            { x: maxX, y: this.regressionInfo.slope * maxX + this.regressionInfo.intercept }
          ]
        }
        
        // 准备图表数据
        const dataPoints = []
        for (let i = 0; i < sampledXValues.length; i++) {
          dataPoints.push({ x: sampledXValues[i], y: sampledYValues[i] })
        }
        
        const data = {
          datasets: [
            {
              label: '产量数据点',
              data: dataPoints,
              backgroundColor: 'rgba(75, 192, 192, 0.5)',
              borderColor: 'rgba(75, 192, 192, 1)',
              pointRadius: 5,
              pointHoverRadius: 7,
              showLine: false
            }
          ]
        }
        
        // 如果有回归线数据，添加回归线数据集
        if (this.regressionInfo && regressionPoints.length > 0) {
          data.datasets.push({
            label: '回归线',
            data: regressionPoints,
            borderColor: 'rgba(255, 99, 132, 1)',
            borderWidth: 2,
            pointRadius: 0,
            fill: false,
            showLine: true
          })
        }
        
        // 销毁旧图表
        if (this.chart) {
          this.chart.destroy()
          this.chart = null
        }
        
        // 确保canvas元素存在
        if (!this.$refs.scatterChart) {
          console.error('找不到canvas元素')
          this.error = '图表渲染失败：找不到canvas元素'
          return
        }
        
        // 创建新图表
        const ctx = this.$refs.scatterChart.getContext('2d')
        if (!ctx) {
          console.error('无法获取canvas上下文')
          this.error = '图表渲染失败：无法获取canvas上下文'
          return
        }
        
        try {
          this.chart = new Chart(ctx, {
            type: 'scatter',
            data: data,
            options: {
              responsive: true,
              maintainAspectRatio: false,
              animation: {
                duration: 0 // 禁用动画以提高性能
              },
              elements: {
                point: {
                  radius: xValues.length > 100 ? 2 : 5, // 数据点多时减小点大小
                  hoverRadius: xValues.length > 100 ? 4 : 7
                },
                line: {
                  tension: 0 // 禁用线条动画
                }
              },
              plugins: {
                title: {
                  display: true,
                  text: `${this.selectedCrop}产量与${this.getFactorName(factor)}的关系`
                },
                tooltip: {
                  callbacks: {
                    label: (context) => {
                      return `${this.getFactorName(factor)}: ${context.parsed.x.toFixed(2)}, 产量: ${context.parsed.y.toFixed(4)}`
                    }
                  }
                }
              },
              scales: {
                x: {
                  title: {
                    display: true,
                    text: this.getFactorName(factor)
                  }
                },
                y: {
                  title: {
                    display: true,
                    text: '产量'
                  }
                }
              }
            }
          })
          
          console.log('图表已成功更新')
          this.error = null
        } catch (chartError) {
          console.error('创建图表实例失败:', chartError)
          this.error = '创建图表实例失败'
        }
      } catch (error) {
        console.error('图表更新过程中发生错误:', error)
        this.error = '图表更新失败: ' + (error.message || '未知错误')
      }
    },
    calculateRegression(xValues, yValues) {
      // 确保输入数据是有效的数字数组
      if (!xValues || !yValues || xValues.length !== yValues.length || xValues.length < 2) {
        throw new Error('无效的数据用于回归计算')
      }
      
      // 简单线性回归计算
      const n = xValues.length
      
      // 计算均值
      const xMean = xValues.reduce((sum, x) => sum + x, 0) / n
      const yMean = yValues.reduce((sum, y) => sum + y, 0) / n
      
      // 计算斜率和截距
      let numerator = 0
      let denominator = 0
      
      for (let i = 0; i < n; i++) {
        numerator += (xValues[i] - xMean) * (yValues[i] - yMean)
        denominator += Math.pow(xValues[i] - xMean, 2)
      }
      
      // 防止除以零
      if (Math.abs(denominator) < 1e-10) {
        throw new Error('回归计算中除以零错误')
      }
      
      const slope = numerator / denominator
      const intercept = yMean - slope * xMean
      
      // 计算相关系数和决定系数
      let totalSumSquares = 0
      let residualSumSquares = 0
      
      for (let i = 0; i < n; i++) {
        totalSumSquares += Math.pow(yValues[i] - yMean, 2)
        const prediction = slope * xValues[i] + intercept
        residualSumSquares += Math.pow(yValues[i] - prediction, 2)
      }
      
      // 防止除以零
      if (Math.abs(totalSumSquares) < 1e-10) {
        throw new Error('回归计算中除以零错误（总平方和为零）')
      }
      
      const r2 = 1 - (residualSumSquares / totalSumSquares)
      const correlation = Math.sqrt(Math.abs(r2)) * (slope > 0 ? 1 : -1)
      
      this.regressionInfo = {
        slope,
        intercept,
        correlation,
        r2
      }
    },
    getFactorName(factor) {
      const names = {
        'Rainfall': '降雨量',
        'Temperature': '温度',
        'Ph': '土壤酸碱度'
      }
      return names[factor] || factor
    },
    // 新增数据抽样方法
    sampleData(xValues, yValues, maxPoints) {
      const totalPoints = xValues.length
      
      // 如果数据点数量少于最大点数，直接返回原始数据
      if (totalPoints <= maxPoints) {
        return { xValues, yValues }
      }
      
      console.log(`数据点数量(${totalPoints})超过限制(${maxPoints})，进行抽样处理`)
      
      // 计算抽样间隔
      const step = Math.ceil(totalPoints / maxPoints)
      
      // 抽样数据
      const sampledXValues = []
      const sampledYValues = []
      
      for (let i = 0; i < totalPoints; i += step) {
        sampledXValues.push(xValues[i])
        sampledYValues.push(yValues[i])
      }
      
      console.log(`抽样后数据点数量: ${sampledXValues.length}`)
      return { xValues: sampledXValues, yValues: sampledYValues }
    }
  }
}
</script>

<style scoped>
.product-regression-chart {
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.chart-controls {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  align-items: flex-end;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 200px;
}

.form-control {
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.generate-btn {
  padding: 8px 16px;
  background-color: #3498db;
  color: white;
  border: none;
  border-radius: 4px;
  font-weight: 500;
  cursor: pointer;
  transition: background-color 0.3s;
  width: 100%;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.generate-btn:hover {
  background-color: #2980b9;
}

.generate-btn:disabled {
  background-color: #95a5a6;
  cursor: not-allowed;
}

.chart-container {
  height: 350px;
  margin-bottom: 20px;
  position: relative;
}

.loading-overlay {
  opacity: 0.7;
}

.loading-indicator {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top-color: #3498db;
  animation: spin 1s linear infinite;
  margin: 0 auto 10px;
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

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  padding: 10px;
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
  border-radius: 4px;
  margin-bottom: 15px;
}

.regression-info {
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 6px;
  margin-top: 15px;
}

.regression-info h4 {
  margin-top: 0;
  color: #333;
}
</style> 