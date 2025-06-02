<template>
  <div class="stats-section">
    <h2 class="section-title">数据概览</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="stat in stats" :key="stat.title">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon" :style="{ backgroundColor: stat.color }">
              <el-icon>
                <component :is="stat.icon"></component>
              </el-icon>
            </div>
            <div class="stat-info">
              <h3 class="stat-value">{{ stat.value }}</h3>
              <span class="stat-title">{{ stat.title }}</span>
            </div>
          </div>
          <div class="stat-footer">
            <span class="stat-trend" :class="{ 'up': stat.trend > 0, 'down': stat.trend < 0 }">
              <el-icon>
                <CaretTop v-if="stat.trend > 0" />
                <CaretBottom v-else-if="stat.trend < 0" />
                <Minus v-else />
              </el-icon>
              {{ Math.abs(stat.trend) }}% {{ stat.trend > 0 ? '增长' : stat.trend < 0 ? '下降' : '无变化' }}
            </span>
            <span class="stat-period">较上月</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref } from 'vue'
import { CaretTop, CaretBottom, Minus, DataLine, Document, Folder, DataAnalysis } from '@element-plus/icons-vue'

export default {
  name: 'DataStatsSummary',
  components: {
    CaretTop,
    CaretBottom,
    Minus,
    DataLine,
    Document,
    Folder,
    DataAnalysis
  },
  props: {
    // 可以传入自定义统计数据
    customStats: {
      type: Array,
      default: null
    }
  },
  setup(props) {
    // 默认统计数据
    const defaultStats = [
      { title: '数据源总数', value: '24', icon: 'DataLine', color: '#409EFF', trend: 8 },
      { title: '数据表总数', value: '156', icon: 'Document', color: '#67C23A', trend: 12 },
      { title: '总存储容量', value: '2.4TB', icon: 'Folder', color: '#E6A23C', trend: 5 },
      { title: '分析任务数', value: '48', icon: 'DataAnalysis', color: '#F56C6C', trend: -3 }
    ]
    
    // 如果传入了自定义数据则使用，否则使用默认数据
    const stats = ref(props.customStats || defaultStats)
    
    return {
      stats
    }
  }
}
</script>

<style scoped>
.section-title {
  font-size: 18px;
  color: #303133;
  margin: 0 0 20px;
  position: relative;
  padding-left: 12px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  background-color: #409EFF;
  border-radius: 2px;
}

/* 数据概览区域样式 */
.stat-card {
  border-radius: 8px;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.stat-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  align-items: center;
  margin-bottom: 15px;
}

.stat-icon {
  width: 50px;
  height: 50px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.stat-icon .el-icon {
  font-size: 24px;
  color: white;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.stat-title {
  font-size: 14px;
  color: #909399;
}

.stat-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 10px;
  border-top: 1px solid #f0f2f5;
}

.stat-trend {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #909399;
}

.stat-trend.up {
  color: #67C23A;
}

.stat-trend.down {
  color: #F56C6C;
}

.stat-trend .el-icon {
  margin-right: 3px;
}

.stat-period {
  font-size: 12px;
  color: #909399;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .el-col {
    width: 50% !important;
  }
}

@media (max-width: 768px) {
  .el-col {
    width: 100% !important;
  }
}
</style> 