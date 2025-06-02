<template>
  <div ref="chartContainer" class="mini-chart-container"></div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'

export default {
  name: 'MiniChart',
  props: {
    sensorData: {
      type: Array,
      default: () => []
    },
    sensorType: {
      type: String,
      default: 'temperature'
    }
  },
  setup(props) {
    const chartContainer = ref(null)
    let chartInstance = null
    
    // 获取颜色配置
    const getColor = (type) => {
      switch(type) {
        case 'temperature': return ['#ff7875', 'rgba(245, 34, 45, 0.2)']
        case 'humidity': return ['#69c0ff', 'rgba(24, 144, 255, 0.2)']
        case 'soilMoisture': return ['#b37feb', 'rgba(179, 127, 235, 0.2)']
        case 'light': return ['#ffd666', 'rgba(250, 173, 20, 0.2)']
        case 'co2': return ['#5cdbd3', 'rgba(92, 219, 211, 0.2)']
        default: return ['#1890ff', 'rgba(24, 144, 255, 0.2)']
      }
    }
    
    // 初始化图表
    const initChart = () => {
      if (!chartContainer.value) return
      
      // 销毁可能存在的图表实例
      if (chartInstance) {
        chartInstance.dispose()
      }
      
      // 创建图表实例
      chartInstance = echarts.init(chartContainer.value)
      
      // 获取数据
      updateChart()
    }
    
    // 更新图表数据
    const updateChart = () => {
      if (!chartInstance) return
      
      const data = props.sensorData || []
      if (data.length === 0) {
        // 如果没有数据，显示空状态
        chartInstance.setOption({
          grid: {
            left: 0,
            right: 0,
            top: 10,
            bottom: 0
          },
          xAxis: {
            type: 'category',
            show: false,
            data: []
          },
          yAxis: {
            type: 'value',
            show: false
          },
          series: []
        })
        return
      }
      
      // 数据点数组
      const values = [...data]
      
      // 确定是否有异常数据
      const hasAnomaly = values.some((val, idx) => {
        if (idx < values.length - 5) return false
        
        // 简单的异常检测规则
        const avg = values.reduce((sum, v) => sum + v, 0) / values.length
        const threshold = avg * 0.25 // 偏差超过25%
        return Math.abs(val - avg) > threshold
      })
      
      // 确定显示点的数量，确保不显示太多
      let displayData = values
      if (values.length > 30) {
        // 如果数据点太多，只显示最新的20个
        displayData = values.slice(-20)
      }
      
      // X轴坐标
      const xAxisData = Array.from({ length: displayData.length }, (_, i) => i)
      
      // 计算最大最小值以设置Y轴范围，增加一点余量
      let min = Math.min(...displayData)
      let max = Math.max(...displayData)
      const range = max - min
      min = Math.max(0, min - range * 0.1) // 下限不低于0
      max = max + range * 0.1
      
      // 获取颜色配置
      const [lineColor, areaColor] = getColor(props.sensorType)
      
      // 配置项
      const option = {
        animation: false,
        grid: {
          left: 0,
          right: 0,
          top: 5,
          bottom: 0,
          containLabel: false
        },
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: xAxisData,
          show: false
        },
        yAxis: {
          type: 'value',
          min: min,
          max: max,
          show: false
        },
        series: [
          {
            type: 'line',
            data: displayData,
            symbol: 'none',
            smooth: true,
            lineStyle: {
              color: lineColor,
              width: hasAnomaly ? 2 : 1.5,
              shadowColor: hasAnomaly ? 'rgba(245, 34, 45, 0.5)' : 'rgba(24, 144, 255, 0.3)',
              shadowBlur: hasAnomaly ? 8 : 4
            },
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {
                  offset: 0,
                  color: areaColor.replace('0.2', '0.5')
                },
                {
                  offset: 1,
                  color: areaColor.replace('0.2', '0.05')
                }
              ])
            },
            emphasis: {
              focus: 'series',
              itemStyle: {
                color: lineColor
              }
            }
          }
        ]
      }
      
      // 应用配置
      chartInstance.setOption(option)
    }
    
    // 监听数据变化
    watch(() => props.sensorData, () => {
      updateChart()
    }, { deep: true })
    
    // 监听传感器类型变化
    watch(() => props.sensorType, () => {
      updateChart()
    })
    
    // 组件挂载时初始化图表
    onMounted(() => {
      // 延迟初始化确保DOM准备好
      setTimeout(() => {
        initChart()
        
        // 监听窗口大小变化，调整图表大小
        window.addEventListener('resize', handleResize)
      }, 100)
    })
    
    // 组件卸载时清理资源
    onUnmounted(() => {
      if (chartInstance) {
        chartInstance.dispose()
        chartInstance = null
      }
      window.removeEventListener('resize', handleResize)
    })
    
    // 处理窗口大小变化
    const handleResize = () => {
      if (chartInstance) {
        chartInstance.resize()
      }
    }
    
    return {
      chartContainer
    }
  }
}
</script>

<style scoped>
.mini-chart-container {
  width: 100%;
  height: 100%;
  min-height: 60px;
}
</style> 