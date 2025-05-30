<template>
  <el-card :class="['sensor-card', { 'anomaly': sensor.isAnomaly }]">
    <template #header>
      <div class="sensor-header">
        <span>{{ getSensorTypeDisplay(sensor.type) }} {{ getSensorNumber(id) }}</span>
        <el-tag :type="sensor.isAnomaly ? 'danger' : 'success'" size="small">
          {{ sensor.isAnomaly ? '异常' : '正常' }}
        </el-tag>
      </div>
    </template>
    
    <div class="sensor-value">
      {{ formatSensorValue(sensor.lastValue, sensor.type) }} <span class="unit">{{ getCorrectUnit(sensor.type) }}</span>
      <span :class="getTrendClass(sensor.trend)">{{ getTrendIcon(sensor.trend) }}</span>
    </div>
    
    <div class="sensor-info">
      <div><strong>位置:</strong> {{ sensor.location }}</div>
      <div><strong>更新时间:</strong> {{ sensor.lastTime }}</div>
      <div v-if="sensor.movingAverage !== undefined">
        <strong>移动平均值:</strong> {{ formatSensorValue(sensor.movingAverage, sensor.type) }} {{ getCorrectUnit(sensor.type) }}
      </div>
    </div>
    
    <!-- 图表 -->
    <div class="chart-container" :id="`chart-${id}`" ref="chartContainer"></div>
  </el-card>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

export default {
  name: 'SensorCard',
  props: {
    id: {
      type: String,
      required: true
    },
    sensor: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    const chartContainer = ref(null)
    let chart = null
    
    // 初始化图表
    const initChart = () => {
      if (!chartContainer.value) return
      
      chart = echarts.init(chartContainer.value)
      updateChart()
    }
    
    // 更新图表
    const updateChart = () => {
      if (!chart || !props.sensor) return
      
      // 进行数据验证，确保值是合理的
      const validValues = props.sensor.values ? 
        props.sensor.values.map(v => validateSensorValue(v, props.sensor.type)) : []
      
      chart.setOption({
        tooltip: {
          trigger: 'axis',
          formatter: function(params) {
            const param = params[0]
            const value = formatSensorValue(param.value, props.sensor.type)
            return `${param.name}<br />${param.seriesName}: ${value} ${getCorrectUnit(props.sensor.type)}`
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          top: '10%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: props.sensor.timestamps || [],
          axisLabel: {
            show: false
          }
        },
        yAxis: {
          type: 'value',
          scale: true,
          axisLabel: {
            formatter: function(value) {
              return formatSensorValue(value, props.sensor.type, true)
            }
          }
        },
        series: [
          {
            name: getSensorTypeDisplay(props.sensor.type),
            type: 'line',
            data: validValues,
            smooth: true,
            lineStyle: {
              width: 3
            },
            itemStyle: {
              color: getChartColor(props.sensor.type)
            },
            areaStyle: {
              color: {
                type: 'linear',
                x: 0,
                y: 0,
                x2: 0,
                y2: 1,
                colorStops: [{
                  offset: 0, color: getChartColor(props.sensor.type, 0.6)
                }, {
                  offset: 1, color: getChartColor(props.sensor.type, 0.1)
                }]
              }
            }
          }
        ]
      })
    }
    
    // 格式化传感器值，确保数据显示正确
    const formatSensorValue = (value, type, isAxisLabel = false) => {
      if (value === undefined || value === null) return '暂无数据'
      
      // 验证并限制异常数据
      value = validateSensorValue(value, type)
      
      // 根据不同类型格式化显示
      switch (type) {
        case 'temperature':
          return Number(value).toFixed(1)
        case 'humidity':
        case 'soilMoisture':
          return Number(value).toFixed(1)
        case 'light':
          // 光照值通常较大，可以考虑缩写
          return isAxisLabel && value >= 1000 ? (value / 1000).toFixed(1) + 'k' : Number(value).toFixed(0)
        case 'co2':
          return Number(value).toFixed(0)
        default:
          return Number(value).toFixed(2)
      }
    }
    
    // 验证传感器值，确保在合理范围内
    const validateSensorValue = (value, type) => {
      if (typeof value !== 'number' || isNaN(value)) {
        return 0
      }
      
      // 定义各类型传感器的合理值范围
      const ranges = {
        temperature: { min: -20, max: 50 },
        humidity: { min: 0, max: 100 },
        soilMoisture: { min: 0, max: 100 },
        light: { min: 0, max: 100000 },
        co2: { min: 300, max: 5000 }
      }
      
      if (!ranges[type]) return value
      
      // 限制值在合理范围内
      if (value < ranges[type].min) return ranges[type].min
      if (value > ranges[type].max) return ranges[type].max
      
      return value
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
    
    // 获取趋势图标
    const getTrendIcon = (trend) => {
      switch (trend) {
        case 'rising': return '↑'
        case 'falling': return '↓'
        case 'stable': return '→'
        default: return ''
      }
    }
    
    // 获取趋势样式类
    const getTrendClass = (trend) => {
      switch (trend) {
        case 'rising': return 'trend-up'
        case 'falling': return 'trend-down'
        case 'stable': return 'trend-stable'
        default: return ''
      }
    }
    
    // 获取图表颜色
    const getChartColor = (type, alpha = 1) => {
      let color
      switch (type) {
        case 'temperature': color = 'rgb(255, 99, 71)'; break; // 红色
        case 'humidity': color = 'rgb(0, 123, 255)'; break; // 蓝色
        case 'soilMoisture': color = 'rgb(111, 66, 193)'; break; // 紫色
        case 'light': color = 'rgb(255, 193, 7)'; break; // 黄色
        case 'co2': color = 'rgb(32, 201, 151)'; break; // 青绿色
        default: color = 'rgb(108, 117, 125)'; // 灰色
      }
      
      if (alpha < 1) {
        return color.replace('rgb', 'rgba').replace(')', `, ${alpha})`)
      }
      
      return color
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
    
    // 获取传感器编号（从ID中提取数字部分）
    const getSensorNumber = (id) => {
      const match = id.match(/\d+/)
      return match ? match[0] : id
    }
    
    // 监听传感器数据变化
    watch(() => props.sensor, () => {
      updateChart()
    }, { deep: true })
    
    // 组件挂载时初始化图表
    onMounted(() => {
      // 延迟初始化，确保DOM已经渲染
      setTimeout(() => {
        initChart()
      }, 100)
      
      // 窗口大小变化时调整图表大小
      window.addEventListener('resize', () => {
        chart && chart.resize()
      })
    })
    
    // 组件卸载时销毁图表
    onUnmounted(() => {
      chart && chart.dispose()
      window.removeEventListener('resize', () => {})
    })
    
    return {
      chartContainer,
      getSensorTypeDisplay,
      getTrendIcon,
      getTrendClass,
      formatSensorValue,
      validateSensorValue,
      getCorrectUnit,
      getSensorNumber
    }
  }
}
</script>

<style scoped>
.sensor-card {
  position: relative;
  transition: all 0.3s ease;
}

.sensor-card.anomaly {
  border-left: 5px solid #f56c6c;
}

.sensor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.sensor-value {
  font-size: 28px;
  font-weight: bold;
  margin: 10px 0;
}

.unit {
  font-size: 16px;
  color: #909399;
  margin-left: 5px;
}

.sensor-info {
  margin-bottom: 15px;
  font-size: 14px;
  color: #606266;
}

.chart-container {
  height: 150px;
  margin-top: 10px;
}

.trend-up {
  color: #67c23a;
  margin-left: 10px;
}

.trend-down {
  color: #f56c6c;
  margin-left: 10px;
}

.trend-stable {
  color: #409eff;
  margin-left: 10px;
}
</style> 