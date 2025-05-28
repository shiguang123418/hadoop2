<template>
  <el-card class="stats-panel">
    <template #header>
      <div class="stats-header">
        <span>Spark统计数据</span>
      </div>
    </template>
    
    <div class="stats-content">
      <el-card v-for="(stats, type) in stats" :key="type" class="stat-item">
        <template #header>
          <span>{{ getSensorTypeDisplay(type) }}</span>
        </template>
        <div class="stats-details">
          <div><strong>最小值:</strong> {{ stats.min.toFixed(2) }}</div>
          <div><strong>最大值:</strong> {{ stats.max.toFixed(2) }}</div>
          <div><strong>平均值:</strong> {{ stats.avg.toFixed(2) }}</div>
          <div><strong>标准差:</strong> {{ stats.stdDev.toFixed(2) }}</div>
          <div><strong>样本数:</strong> {{ stats.count }}</div>
          <div><strong>异常数:</strong> {{ stats.anomalyCount }}</div>
          <div><strong>异常率:</strong> {{ (stats.anomalyRate * 100).toFixed(2) }}%</div>
        </div>
      </el-card>
    </div>
  </el-card>
</template>

<script>
export default {
  name: 'StatsPanel',
  props: {
    stats: {
      type: Object,
      required: true
    }
  },
  setup() {
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
    
    return {
      getSensorTypeDisplay
    }
  }
}
</script>

<style scoped>
.stats-panel {
  margin-bottom: 20px;
}

.stats-content {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-item {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stats-details {
  font-size: 14px;
  line-height: 1.8;
}
</style> 