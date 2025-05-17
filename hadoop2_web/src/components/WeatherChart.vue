<template>
  <div class="weather-chart-container">
    <h2>气象数据可视化</h2>

    <div class="controls">
      <div class="form-group">
        <label>城市:</label>
        <select v-model="selectedCity" class="form-control">
          <option v-for="cityInfo in cities" :key="cityInfo.city" :value="cityInfo.city">
            {{ cityInfo.city }} ({{ cityInfo.state }})
          </option>
        </select>
      </div>

      <div class="form-group">
        <label>指标:</label>
        <select v-model="selectedMetric" class="form-control">
          <option value="all">全部指标</option>
          <option value="maxtemp">最高温度</option>
          <option value="mintemp">最低温度</option>
          <option value="rainfall">降雨量</option>
        </select>
      </div>

      <button @click="generateChart" class="btn btn-primary">生成图表</button>
    </div>

    <div ref="chartContainer" class="chart-container"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import weatherService from '@/services/WeatherService';

export default {
  name: 'WeatherChart',
  data() {
    return {
      cities: [
        { city: 'Bangalore', state: 'Karnataka' },
        { city: 'Bareilly', state: 'Uttar Pradesh' }
      ],
      selectedCity: 'Bangalore',
      selectedMetric: 'rainfall',
      chartInstance: null,
      loading: false,
      error: null
    };
  },
  async mounted() {
    try {
      // 获取城市列表
      const cities = await weatherService.getCities();
      if (cities && cities.length > 0) {
        this.cities = cities;
        this.selectedCity = cities[0].city;
      }
    } catch (error) {
      console.error('获取城市列表失败:', error);
      // 使用默认值
    }

    // 初始化图表实例
    this.initChart();
  },
  methods: {
    // 将英文月份转换为简短的显示形式
    formatMonth(month) {
      const monthMap = {
        'January': '1月',
        'February': '2月',
        'March': '3月',
        'April': '4月',
        'May': '5月',
        'June': '6月',
        'July': '7月',
        'August': '8月',
        'September': '9月',
        'October': '10月',
        'November': '11月',
        'December': '12月'
      };
      
      // 尝试映射月份，如果不存在则返回原始值
      return monthMap[month.trim()] || month;
    },
    
    initChart() {
      // 在组件挂载后初始化ECharts实例
      if (!this.chartInstance) {
        this.chartInstance = echarts.init(this.$refs.chartContainer);
        // 监听窗口大小变化自动调整图表大小
        window.addEventListener('resize', this.resizeChart);
      }
    },
    resizeChart() {
      if (this.chartInstance) {
        this.chartInstance.resize();
      }
    },
    async generateChart() {
      if (!this.selectedCity) {
        this.error = '请选择城市';
        return;
      }

      this.loading = true;
      this.error = null;

      try {
        if (this.selectedMetric === 'all') {
          // 请求所有三个指标的数据
          await this.generateMultiMetricChart();
        } else {
          // 请求单个指标的数据
          await this.generateSingleMetricChart();
        }
      } catch (error) {
        console.error('生成图表失败:', error);
        this.error = '获取数据失败，请稍后重试';
      } finally {
        this.loading = false;
      }
    },
    async generateSingleMetricChart() {
      // 从服务获取数据
      const data = await weatherService.getWeatherData(
        this.selectedCity, 
        this.selectedMetric
      );

      // 根据指标类型设置单位和颜色
      let unit = '';
      let color = '#5470c6';
      
      if (this.selectedMetric === 'maxtemp' || this.selectedMetric === 'mintemp') {
        unit = '°C';
        color = this.selectedMetric === 'maxtemp' ? '#e74c3c' : '#3498db';
      } else if (this.selectedMetric === 'rainfall') {
        unit = 'mm';
        color = '#27ae60';
      }

      // 处理月份显示
      const formattedMonths = data.months.map(month => this.formatMonth(month));

      // 查找当前城市的州/邦信息
      const cityInfo = this.cities.find(c => c.city === this.selectedCity);
      const stateInfo = cityInfo ? ` (${cityInfo.state})` : '';
      
      // 图表配置
      const option = {
        title: { 
          text: `${this.selectedCity}${stateInfo} ${this.getMetricName()}趋势`,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: (params) => {
            const param = params[0];
            return `${param.name}：${param.value} ${unit}`;
          }
        },
        xAxis: { 
          type: 'category',
          data: formattedMonths,
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: `{value} ${unit}`
          }
        },
        series: [{
          name: this.getMetricName(),
          type: 'line',
          data: data.values,
          itemStyle: {
            color: color
          },
          lineStyle: {
            width: 3,
            color: color
          },
          areaStyle: {
            color: {
              type: 'linear',
              x: 0,
              y: 0,
              x2: 0,
              y2: 1,
              colorStops: [{
                offset: 0, 
                color: color
              }, {
                offset: 1, 
                color: 'rgba(255, 255, 255, 0.3)'
              }]
            }
          },
          smooth: true
        }],
        grid: {
          top: 80,
          left: 60,
          right: 50,
          bottom: 80
        }
      };

      // 设置图表选项
      this.chartInstance.setOption(option);
    },
    async generateMultiMetricChart() {
      try {
        // 使用新API一次获取所有气象指标数据
        const allData = await weatherService.getAllWeatherData(this.selectedCity);
        
        // 处理月份显示
        const formattedMonths = allData.months.map(month => this.formatMonth(month));
        
        // 查找当前城市的州/邦信息
        const cityInfo = this.cities.find(c => c.city === this.selectedCity);
        const stateInfo = cityInfo ? ` (${cityInfo.state})` : '';
        const stateText = allData.state || (cityInfo ? cityInfo.state : '');
        
        // 图表配置
        const option = {
          title: { 
            text: `${this.selectedCity}${stateInfo} 气象数据综合趋势`,
            left: 'center'
          },
          tooltip: {
            trigger: 'axis',
            formatter: (params) => {
              let result = params[0].name + '<br/>';
              
              params.forEach(param => {
                let unit = param.seriesName.includes('温度') ? '°C' : 'mm';
                let marker = `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:10px;height:10px;background-color:${param.color};"></span>`;
                result += marker + param.seriesName + ': ' + param.value + ' ' + unit + '<br/>';
              });
              
              return result;
            }
          },
          legend: {
            data: ['最高温度', '最低温度', '降雨量'],
            top: 40
          },
          grid: {
            top: 100,
            left: 60,
            right: 50,
            bottom: 80
          },
          xAxis: {
            type: 'category',
            data: formattedMonths,
            axisLabel: {
              rotate: 45
            }
          },
          yAxis: [
            {
              type: 'value',
              name: '温度 (°C)',
              position: 'left',
              axisLine: {
                show: true,
                lineStyle: {
                  color: '#5470c6'
                }
              },
              axisLabel: {
                formatter: '{value} °C'
              }
            },
            {
              type: 'value',
              name: '降雨量 (mm)',
              position: 'right',
              axisLine: {
                show: true,
                lineStyle: {
                  color: '#27ae60'
                }
              },
              axisLabel: {
                formatter: '{value} mm'
              }
            }
          ],
          series: [
            {
              name: '最高温度',
              type: 'line',
              data: allData.metrics.maxtemp,
              yAxisIndex: 0,
              itemStyle: {
                color: '#e74c3c'
              },
              lineStyle: {
                width: 2
              },
              smooth: true
            },
            {
              name: '最低温度',
              type: 'line',
              data: allData.metrics.mintemp,
              yAxisIndex: 0,
              itemStyle: {
                color: '#3498db'
              },
              lineStyle: {
                width: 2
              },
              smooth: true
            },
            {
              name: '降雨量',
              type: 'bar',
              data: allData.metrics.rainfall,
              yAxisIndex: 1,
              itemStyle: {
                color: '#27ae60'
              },
              barWidth: '50%',
              opacity: 0.7
            }
          ]
        };

        // 设置图表选项
        this.chartInstance.setOption(option);
      } catch (error) {
        console.error('获取所有气象数据失败:', error);
        // 如果新API失败，回退到使用多个请求
        this.fallbackToMultipleRequests();
      }
    },
    // 回退方法：使用多个请求获取数据
    async fallbackToMultipleRequests() {
      console.log('使用多个请求获取数据...');
      
      // 获取所有三个指标的数据
      const maxTempData = await weatherService.getWeatherData(this.selectedCity, 'maxtemp');
      const minTempData = await weatherService.getWeatherData(this.selectedCity, 'mintemp');
      const rainfallData = await weatherService.getWeatherData(this.selectedCity, 'rainfall');

      // 处理月份显示
      const formattedMonths = maxTempData.months.map(month => this.formatMonth(month));
      
      // 查找当前城市的州/邦信息
      const cityInfo = this.cities.find(c => c.city === this.selectedCity);
      const stateInfo = cityInfo ? ` (${cityInfo.state})` : '';

      // 图表配置
      const option = {
        title: { 
          text: `${this.selectedCity}${stateInfo} 气象数据综合趋势`,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: (params) => {
            let result = params[0].name + '<br/>';
            
            params.forEach(param => {
              let unit = param.seriesName.includes('温度') ? '°C' : 'mm';
              let marker = `<span style="display:inline-block;margin-right:5px;border-radius:10px;width:10px;height:10px;background-color:${param.color};"></span>`;
              result += marker + param.seriesName + ': ' + param.value + ' ' + unit + '<br/>';
            });
            
            return result;
          }
        },
        legend: {
          data: ['最高温度', '最低温度', '降雨量'],
          top: 40
        },
        grid: {
          top: 100,
          left: 60,
          right: 50,
          bottom: 80
        },
        xAxis: {
          type: 'category',
          data: formattedMonths,
          axisLabel: {
            rotate: 45
          }
        },
        yAxis: [
          {
            type: 'value',
            name: '温度 (°C)',
            position: 'left',
            axisLine: {
              show: true,
              lineStyle: {
                color: '#5470c6'
              }
            },
            axisLabel: {
              formatter: '{value} °C'
            }
          },
          {
            type: 'value',
            name: '降雨量 (mm)',
            position: 'right',
            axisLine: {
              show: true,
              lineStyle: {
                color: '#27ae60'
              }
            },
            axisLabel: {
              formatter: '{value} mm'
            }
          }
        ],
        series: [
          {
            name: '最高温度',
            type: 'line',
            data: maxTempData.values,
            yAxisIndex: 0,
            itemStyle: {
              color: '#e74c3c'
            },
            lineStyle: {
              width: 2
            },
            smooth: true
          },
          {
            name: '最低温度',
            type: 'line',
            data: minTempData.values,
            yAxisIndex: 0,
            itemStyle: {
              color: '#3498db'
            },
            lineStyle: {
              width: 2
            },
            smooth: true
          },
          {
            name: '降雨量',
            type: 'bar',
            data: rainfallData.values,
            yAxisIndex: 1,
            itemStyle: {
              color: '#27ae60'
            },
            barWidth: '50%',
            opacity: 0.7
          }
        ]
      };

      // 设置图表选项
      this.chartInstance.setOption(option);
    },
    getMetricName() {
      switch(this.selectedMetric) {
        case 'maxtemp': return '最高温度';
        case 'mintemp': return '最低温度';
        case 'rainfall': return '降雨量';
        case 'all': return '全部指标';
        default: return this.selectedMetric;
      }
    }
  },
  beforeDestroy() {
    // 清理图表实例和事件监听器
    if (this.chartInstance) {
      window.removeEventListener('resize', this.resizeChart);
      this.chartInstance.dispose();
      this.chartInstance = null;
    }
  }
};
</script>

<style scoped>
.weather-chart-container {
  padding: 20px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.controls {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  align-items: flex-end;
}

.form-group {
  display: flex;
  flex-direction: column;
  min-width: 150px;
}

.form-group label {
  margin-bottom: 5px;
  font-weight: bold;
}

.chart-container {
  height: 400px;
  width: 100%;
  margin-top: 20px;
}

.btn-primary {
  background-color: #007bff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
}

.btn-primary:hover {
  background-color: #0056b3;
}
</style> 