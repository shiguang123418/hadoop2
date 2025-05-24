<template>
  <div class="home-page">
    <div class="welcome-section">
      <el-card class="welcome-card" shadow="hover">
        <div class="welcome-content">
          <div class="welcome-text">
            <h1 class="welcome-title">欢迎使用农业大数据平台</h1>
            <p class="welcome-description">
              本平台整合了HDFS、Hadoop、Hive、Spark和Kafka等大数据技术，为农业数据提供全面的存储、处理和分析能力。
            </p>
            <div class="welcome-actions">
              <el-button type="primary" @click="goToHdfs">
                <el-icon><Folder /></el-icon>
                <span>HDFS文件浏览器</span>
              </el-button>
              <el-button type="success" @click="goToHive">
                <el-icon><DataAnalysis /></el-icon>
                <span>Hive数据查询</span>
              </el-button>
              <el-button type="warning" @click="goToAnalysis">
                <el-icon><TrendCharts /></el-icon>
                <span>产量预测分析</span>
              </el-button>
              <el-button type="info" @click="goToMonitor">
                <el-icon><Monitor /></el-icon>
                <span>实时传感器监控</span>
              </el-button>
            </div>
          </div>
          <div class="welcome-image">
            <div class="image-placeholder">
              <el-icon><PictureFilled /></el-icon>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="service-section">
      <h2 class="section-title">服务状态监控</h2>
      <el-row :gutter="20">
        <el-col :span="6" v-for="service in services" :key="service.name">
          <el-card class="service-card" shadow="hover">
            <div class="service-status">
              <div class="service-icon" :class="{ 'active': service.status === 'running' }">
                <el-icon>
                  <component :is="service.icon"></component>
                </el-icon>
              </div>
              <div class="service-info">
                <h3 class="service-name">{{ service.name }}</h3>
                <div class="service-details">
                  <el-tag :type="getStatusType(service.status)" size="small" effect="dark">
                    {{ service.status === 'running' ? '运行中' : '已停止' }}
                  </el-tag>
                  <span class="service-url">{{ service.url }}</span>
                </div>
              </div>
              <div class="service-action">
                <el-button circle size="small" :type="service.status === 'running' ? 'success' : 'danger'">
                  <el-icon>
                    <Refresh v-if="service.status === 'running'" />
                    <VideoPlay v-else />
                  </el-icon>
                </el-button>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

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

    <div class="quick-links-section">
      <h2 class="section-title">快速链接</h2>
      <el-row :gutter="20">
        <el-col :span="8" v-for="link in quickLinks" :key="link.title">
          <el-card class="quick-link-card" shadow="hover" @click="goToRoute(link.route)">
            <div class="quick-link-content">
              <div class="quick-link-icon">
                <el-icon>
                  <component :is="link.icon"></component>
                </el-icon>
              </div>
              <div class="quick-link-info">
                <h3 class="quick-link-title">{{ link.title }}</h3>
                <p class="quick-link-description">{{ link.description }}</p>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script>
import { useRouter } from 'vue-router'
import { 
  Folder, 
  DataAnalysis, 
  TrendCharts, 
  Monitor, 
  Refresh, 
  VideoPlay, 
  CaretTop, 
  CaretBottom, 
  Minus,
  Connection,
  Document,
  Setting,
  User,
  PictureFilled
} from '@element-plus/icons-vue'

export default {
  name: 'HomePage',
  components: {
    Folder,
    DataAnalysis,
    TrendCharts,
    Monitor,
    Refresh,
    VideoPlay,
    CaretTop,
    CaretBottom,
    Minus,
    Connection,
    Document,
    Setting,
    User,
    PictureFilled
  },
  setup() {
    const router = useRouter()

    const services = [
      { name: 'HDFS', status: 'running', url: 'hdfs://shiguang:9000', icon: 'Folder' },
      { name: 'Hive', status: 'running', url: 'jdbc:hive2://localhost:10000', icon: 'Document' },
      { name: 'Spark', status: 'running', url: 'local[*]', icon: 'DataAnalysis' },
      { name: 'Kafka', status: 'running', url: 'shiguang:9092', icon: 'Connection' }
    ]

    const stats = [
      { title: '数据源总数', value: '24', icon: 'DataLine', color: '#409EFF', trend: 8 },
      { title: '数据表总数', value: '156', icon: 'Document', color: '#67C23A', trend: 12 },
      { title: '总存储容量', value: '2.4TB', icon: 'Folder', color: '#E6A23C', trend: 5 },
      { title: '分析任务数', value: '48', icon: 'DataAnalysis', color: '#F56C6C', trend: -3 }
    ]

    const quickLinks = [
      { 
        title: '数据源管理', 
        description: '管理系统数据源配置', 
        icon: 'DataLine',
        route: '/datasource'
      },
      { 
        title: '用户管理', 
        description: '管理系统用户和权限', 
        icon: 'User',
        route: '/user-management'
      },
      { 
        title: '系统设置', 
        description: '配置系统参数和服务', 
        icon: 'Setting',
        route: '/system-settings'
      }
    ]

    const getStatusType = (status) => {
      return status === 'running' ? 'success' : 'danger'
    }

    const goToHdfs = () => {
      router.push('/hdfs-explorer')
    }

    const goToHive = () => {
      router.push('/hive-explorer')
    }

    const goToAnalysis = () => {
      router.push('/crop-analysis')
    }

    const goToMonitor = () => {
      router.push('/sensor-monitor')
    }

    const goToRoute = (route) => {
      router.push(route)
    }

    return {
      services,
      stats,
      quickLinks,
      getStatusType,
      goToHdfs,
      goToHive,
      goToAnalysis,
      goToMonitor,
      goToRoute
    }
  }
}
</script>

<style scoped>
.home-page {
  padding-bottom: 30px;
}

.section-title {
  font-size: 18px;
  color: #303133;
  margin: 30px 0 20px;
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

.image-placeholder {
  width: 100%;
  height: 100%;
  background-color: #f0f2f5;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
}

.image-placeholder .el-icon {
  font-size: 48px;
  color: #c0c4cc;
}

/* 服务状态区域样式 */
.service-card {
  border-radius: 8px;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
}

.service-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
}

.service-status {
  display: flex;
  align-items: center;
}

.service-icon {
  width: 40px;
  height: 40px;
  background-color: #f5f7fa;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.service-icon .el-icon {
  font-size: 20px;
  color: #909399;
}

.service-icon.active {
  background-color: #ecf5ff;
}

.service-icon.active .el-icon {
  color: #409EFF;
}

.service-info {
  flex: 1;
}

.service-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.service-details {
  display: flex;
  align-items: center;
}

.service-url {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
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

/* 快速链接区域样式 */
.quick-link-card {
  border-radius: 8px;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
  height: 100px;
}

.quick-link-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
}

.quick-link-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.quick-link-icon {
  width: 50px;
  height: 50px;
  background-color: #ecf5ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.quick-link-icon .el-icon {
  font-size: 24px;
  color: #409EFF;
}

.quick-link-info {
  flex: 1;
}

.quick-link-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.quick-link-description {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
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
  
  .el-col {
    width: 100% !important;
  }
}
</style> 