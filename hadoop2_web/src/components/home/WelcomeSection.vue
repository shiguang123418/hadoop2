<template>
  <div class="welcome-section">
    <el-card class="welcome-card" shadow="hover">
      <div class="welcome-content">
        <div class="welcome-text">
          <h1 class="welcome-title">欢迎使用农业大数据平台</h1>
          <p class="welcome-description">
            本平台整合了HDFS、Hadoop、Hive、Spark和Kafka等大数据技术，为农业数据提供全面的存储、处理和分析能力。
          </p>
          <div class="welcome-actions">
            <el-button type="primary" @click="navigateTo('hdfs')">
              <el-icon><Folder /></el-icon>
              <span>HDFS文件浏览器</span>
            </el-button>
            <el-button type="success" @click="navigateTo('hive')">
              <el-icon><DataAnalysis /></el-icon>
              <span>Hive数据查询</span>
            </el-button>
            <el-button type="warning" @click="navigateTo('analysis')">
              <el-icon><TrendCharts /></el-icon>
              <span>产量预测分析</span>
            </el-button>
            <el-button type="info" @click="navigateTo('monitor')">
              <el-icon><Monitor /></el-icon>
              <span>实时传感器监控</span>
            </el-button>
            <el-button type="danger" @click="navigateTo('dashboard')">
              <el-icon><DataBoard /></el-icon>
              <span>农业大数据平台</span>
            </el-button>
          </div>
        </div>
        <div class="welcome-image">
          <img src="/y1.jpg" alt="农业大数据平台" class="welcome-actual-image" />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { Folder, DataAnalysis, TrendCharts, Monitor, DataBoard } from '@element-plus/icons-vue'

export default {
  name: 'WelcomeSection',
  components: {
    Folder,
    DataAnalysis,
    TrendCharts,
    Monitor,
    DataBoard
  },
  emits: ['navigate'],
  setup(props, { emit }) {
    // 处理导航事件
    const navigateTo = (destination) => {
      emit('navigate', destination)
    }

    return {
      navigateTo
    }
  }
}
</script>

<style scoped>
/* 欢迎区域样式 */
.welcome-card {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 20px;
}

.welcome-content {
  display: flex;
  align-items: center;
}

.welcome-text {
  flex: 1;
  padding-right: 20px;
}

.welcome-title {
  font-size: 28px;
  color: #303133;
  margin-bottom: 15px;
  font-weight: 600;
}

.welcome-description {
  color: #606266;
  margin-bottom: 25px;
  line-height: 1.6;
}

.welcome-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.welcome-image {
  flex: 0 0 300px;
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.welcome-actual-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .welcome-content {
    flex-direction: column;
  }
  
  .welcome-text {
    padding-right: 0;
    margin-bottom: 20px;
  }
  
  .welcome-image {
    flex: 0 0 auto;
    width: 100%;
  }
}

@media (max-width: 768px) {
  .welcome-actions {
    flex-direction: column;
  }
}
</style> 