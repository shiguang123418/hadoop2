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
          <div class="stat-header">
            <span>{{ getSensorTypeDisplay(type) }}</span>
            <el-tabs v-model="activeTabs[type]" type="card" class="stat-tabs">
              <el-tab-pane label="基础统计" name="basic"></el-tab-pane>
              <el-tab-pane label="窗口分析" name="windows" v-if="stats.windowedStats"></el-tab-pane>
              <el-tab-pane label="趋势分析" name="trends" v-if="stats.trendAnalysis || stats.trend"></el-tab-pane>
              <el-tab-pane label="分类分析" name="classifications" v-if="stats.classifications || stats.low !== undefined"></el-tab-pane>
              <el-tab-pane label="相关性分析" name="correlations" v-if="stats.correlations"></el-tab-pane>
              <el-tab-pane label="异常检测" name="anomalies" v-if="stats.zscoreAnomalyCount !== undefined"></el-tab-pane>
            </el-tabs>
          </div>
        </template>

        <!-- 基础统计数据 -->
        <div v-if="activeTabs[type] === 'basic'" class="stats-details">
          <div><strong>最小值:</strong> {{ stats.min?.toFixed(2) || '暂无数据' }}</div>
          <div><strong>最大值:</strong> {{ stats.max?.toFixed(2) || '暂无数据' }}</div>
          <div><strong>平均值:</strong> {{ stats.avg?.toFixed(2) || '暂无数据' }}</div>
          <div><strong>标准差:</strong> {{ stats.stdDev?.toFixed(2) || '暂无数据' }}</div>
          <div><strong>样本数:</strong> {{ stats.count || '0' }}</div>
          <div><strong>异常数:</strong> {{ stats.anomalyCount || '0' }}</div>
          <div><strong>异常率:</strong> {{ ((stats.anomalyRate || 0) * 100).toFixed(2) }}%</div>
        </div>

        <!-- 时间窗口分析 -->
        <div v-if="activeTabs[type] === 'windows' && stats.windowedStats" class="stats-details">
          <div v-for="(windowStats, windowName) in stats.windowedStats" :key="windowName" class="window-stats">
            <h4>{{ windowName }} 窗口</h4>
            <div><strong>最小值:</strong> {{ windowStats.min?.toFixed(2) || '暂无数据' }}</div>
            <div><strong>最大值:</strong> {{ windowStats.max?.toFixed(2) || '暂无数据' }}</div>
            <div><strong>平均值:</strong> {{ windowStats.avg?.toFixed(2) || '暂无数据' }}</div>
            <div><strong>标准差:</strong> {{ windowStats.stdDev?.toFixed(2) || '暂无数据' }}</div>
            <div><strong>样本数:</strong> {{ windowStats.count || '0' }}</div>
          </div>
        </div>

        <!-- 趋势分析 -->
        <div v-if="activeTabs[type] === 'trends'" class="stats-details">
          <div v-if="stats.trendAnalysis">
            <div><strong>趋势:</strong> {{ formatTrend(stats.trendAnalysis.trend) }}</div>
            <div><strong>归一化趋势:</strong> {{ formatTrend(stats.trendAnalysis.normalizedTrend) }}</div>
            <div><strong>预测值:</strong> {{ stats.trendAnalysis.prediction?.toFixed(2) || '暂无数据' }}</div>
          </div>
          <div v-else-if="stats.trend !== undefined">
            <div><strong>趋势:</strong> {{ formatTrend(stats.trend) }}</div>
            <div><strong>归一化趋势:</strong> {{ formatTrend(stats.normalizedTrend) }}</div>
            <div><strong>预测值:</strong> {{ stats.prediction?.toFixed(2) || '暂无数据' }}</div>
          </div>
          <div v-else>
            <p>暂无趋势数据</p>
          </div>
        </div>

        <!-- 分类分析 -->
        <div v-if="activeTabs[type] === 'classifications'" class="stats-details">
          <div v-if="stats.classifications">
            <div class="classification-bar">
              <div class="bar-segment low" :style="{width: getClassificationPercentage(stats.classifications, 'low')}">
                低 ({{ stats.classifications.low || 0 }})
              </div>
              <div class="bar-segment normal" :style="{width: getClassificationPercentage(stats.classifications, 'normal')}">
                正常 ({{ stats.classifications.normal || 0 }})
              </div>
              <div class="bar-segment high" :style="{width: getClassificationPercentage(stats.classifications, 'high')}">
                高 ({{ stats.classifications.high || 0 }})
              </div>
              <div class="bar-segment critical" :style="{width: getClassificationPercentage(stats.classifications, 'critical')}">
                危险 ({{ stats.classifications.critical || 0 }})
              </div>
            </div>
          </div>
          <div v-else-if="stats.low !== undefined">
            <div class="classification-bar">
              <div class="bar-segment low" :style="{width: getBasicClassificationPercentage(stats, 'low')}">
                低 ({{ stats.low || 0 }})
              </div>
              <div class="bar-segment normal" :style="{width: getBasicClassificationPercentage(stats, 'normal')}">
                正常 ({{ stats.normal || 0 }})
              </div>
              <div class="bar-segment high" :style="{width: getBasicClassificationPercentage(stats, 'high')}">
                高 ({{ stats.high || 0 }})
              </div>
              <div class="bar-segment critical" :style="{width: getBasicClassificationPercentage(stats, 'critical')}">
                危险 ({{ stats.critical || 0 }})
              </div>
            </div>
          </div>
          <div v-else>
            <p>暂无分类数据</p>
          </div>
        </div>

        <!-- 相关性分析 -->
        <div v-if="activeTabs[type] === 'correlations' && stats.correlations" class="stats-details">
          <div v-for="(correlation, otherType) in stats.correlations" :key="otherType" class="correlation-item">
            <div class="correlation-label">与{{ getSensorTypeDisplay(otherType) }}的相关性:</div>
            <div class="correlation-value" :class="getCorrelationClass(correlation)">
              {{ correlation.toFixed(2) }}
              <span class="correlation-desc">{{ describeCorrelation(correlation) }}</span>
            </div>
          </div>
          <p v-if="Object.keys(stats.correlations).length === 0">暂无相关性数据</p>
        </div>

        <!-- 异常检测 -->
        <div v-if="activeTabs[type] === 'anomalies'" class="stats-details">
          <div v-if="stats.zscoreAnomalyCount !== undefined">
            <div><strong>Z-score异常数:</strong> {{ stats.zscoreAnomalyCount }}</div>
            <div><strong>Z-score异常率:</strong> {{ ((stats.zscoreAnomalyRate || 0) * 100).toFixed(2) }}%</div>
          </div>
          <div v-if="stats.trendAnomalyCount !== undefined">
            <div><strong>趋势异常数:</strong> {{ stats.trendAnomalyCount }}</div>
          </div>
          <div v-if="stats.anomalyCount !== undefined">
            <div><strong>常规异常数:</strong> {{ stats.anomalyCount }}</div>
            <div><strong>常规异常率:</strong> {{ ((stats.anomalyRate || 0) * 100).toFixed(2) }}%</div>
          </div>
          <p v-if="stats.zscoreAnomalyCount === undefined && stats.anomalyCount === undefined">暂无异常检测数据</p>
        </div>
      </el-card>
    </div>
  </el-card>
</template>

<script>
import { reactive } from 'vue'

export default {
  name: 'StatsPanel',
  props: {
    stats: {
      type: Object,
      required: true
    }
  },
  setup(props) {
    // 每个传感器类型的活动标签页
    const activeTabs = reactive({})
    
    // 初始化每个传感器类型的默认标签页
    Object.keys(props.stats).forEach(type => {
      activeTabs[type] = 'basic'
    })
    
    // 获取传感器类型显示名
    const getSensorTypeDisplay = (type) => {
      switch (type) {
        case 'temperature': return '温度'
        case 'humidity': return '湿度'
        case 'soilMoisture': return '土壤湿度'
        case 'light': return '光照强度'
        case 'co2': return 'CO₂浓度'
        case 'pressure': return '气压'
        default: return type
      }
    }
    
    // 格式化趋势
    const formatTrend = (trend) => {
      if (trend === undefined || trend === null) return '暂无数据'
      
      const value = Number(trend)
      if (isNaN(value)) return '暂无数据'
      
      // 格式化为带符号的数值
      return value > 0 
        ? `+${value.toFixed(4)} (上升)` 
        : value < 0 
          ? `${value.toFixed(4)} (下降)` 
          : '0 (稳定)'
    }
    
    // 计算分类数据百分比 (用于分类对象)
    const getClassificationPercentage = (classifications, category) => {
      if (!classifications) return '0%'
      
      const total = Object.values(classifications).reduce((sum, val) => sum + (Number(val) || 0), 0)
      if (total === 0) return '0%'
      
      const value = Number(classifications[category]) || 0
      return `${Math.round((value / total) * 100)}%`
    }
    
    // 计算分类数据百分比 (用于独立属性)
    const getBasicClassificationPercentage = (stats, category) => {
      if (stats[category] === undefined) return '0%'
      
      const total = ['low', 'normal', 'high', 'critical'].reduce((sum, cat) => {
        return sum + (Number(stats[cat]) || 0)
      }, 0)
      
      if (total === 0) return '0%'
      
      const value = Number(stats[category]) || 0
      return `${Math.round((value / total) * 100)}%`
    }
    
    // 获取相关性等级的CSS类
    const getCorrelationClass = (correlation) => {
      const abs = Math.abs(correlation)
      if (abs >= 0.7) return 'very-strong'
      if (abs >= 0.5) return 'strong'
      if (abs >= 0.3) return 'moderate'
      if (abs >= 0.1) return 'weak'
      return 'very-weak'
    }
    
    // 描述相关性
    const describeCorrelation = (correlation) => {
      const abs = Math.abs(correlation)
      let strength
      
      if (abs >= 0.7) strength = '极强'
      else if (abs >= 0.5) strength = '强'
      else if (abs >= 0.3) strength = '中等'
      else if (abs >= 0.1) strength = '弱'
      else strength = '极弱'
      
      const direction = correlation > 0 ? '正相关' : correlation < 0 ? '负相关' : '无相关'
      
      return `${strength}${direction}`
    }
    
    return {
      activeTabs,
      getSensorTypeDisplay,
      formatTrend,
      getClassificationPercentage,
      getBasicClassificationPercentage,
      getCorrelationClass,
      describeCorrelation
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
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 15px;
}

.stats-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.stat-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-tabs {
  margin-top: 5px;
}

.stat-item {
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.stats-details {
  font-size: 14px;
  line-height: 1.8;
}

.window-stats {
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eee;
}

.window-stats h4 {
  margin: 5px 0;
  color: #409EFF;
}

.classification-bar {
  display: flex;
  height: 30px;
  margin: 10px 0;
  border-radius: 4px;
  overflow: hidden;
}

.bar-segment {
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 12px;
  min-width: 20px;
  transition: all 0.3s;
}

.bar-segment.low {
  background-color: #67C23A;
}

.bar-segment.normal {
  background-color: #409EFF;
}

.bar-segment.high {
  background-color: #E6A23C;
}

.bar-segment.critical {
  background-color: #F56C6C;
}

.correlation-item {
  display: flex;
  flex-direction: column;
  margin-bottom: 8px;
}

.correlation-label {
  font-weight: bold;
}

.correlation-value {
  padding: 4px;
  border-radius: 3px;
}

.correlation-value.very-strong {
  background-color: rgba(64, 158, 255, 0.2);
  color: #2c5ddf;
}

.correlation-value.strong {
  background-color: rgba(64, 158, 255, 0.15);
  color: #3b75dd;
}

.correlation-value.moderate {
  background-color: rgba(64, 158, 255, 0.1);
  color: #4883c7;
}

.correlation-value.weak {
  background-color: rgba(64, 158, 255, 0.05);
  color: #5a89b6;
}

.correlation-value.very-weak {
  background-color: rgba(144, 147, 153, 0.1);
  color: #606266;
}

.correlation-desc {
  margin-left: 8px;
  font-size: 12px;
  color: #606266;
}
</style> 