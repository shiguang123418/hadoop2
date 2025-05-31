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
              <el-button type="danger" @click="goToDashboard">
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

    <div class="service-section">
      <div class="section-header">
        <h2 class="section-title">服务状态监控</h2>
        <el-button size="small" type="primary" :loading="refreshingServices" @click="refreshAllServices">
          <el-icon><Refresh /></el-icon>
          <span>刷新状态</span>
        </el-button>
      </div>
      
      <el-row :gutter="20" v-loading="loadingServices">
        <el-col :span="6" v-for="service in services" :key="service.name">
          <el-card class="service-card" shadow="hover" :body-style="{ padding: '0' }">
            <div class="service-header" :class="{ 'running': service.status === 'running', 'stopped': service.status !== 'running' }">
              <div class="service-icon">
                <el-icon>
                  <component :is="service.icon"></component>
                </el-icon>
              </div>
              <span class="service-name">{{ service.name }}</span>
              <el-tag :type="getStatusType(service.status)" size="small" effect="dark" class="status-tag">
                {{ service.status === 'running' ? '运行中' : '已停止' }}
              </el-tag>
            </div>
            
            <div class="service-content">
              <div class="service-info">
                <div class="info-item">
                  <span class="label">状态：</span>
                  <span class="value" :class="service.status">
                    <el-icon class="status-icon">
                      <CircleCheckFilled v-if="service.status === 'running'" />
                      <CircleCloseFilled v-else />
                    </el-icon>
                    {{ service.status === 'running' ? '正常' : '异常' }}
                  </span>
                </div>
                <div class="info-item">
                  <span class="label">地址：</span>
                  <span class="value url">{{ service.url }}</span>
                </div>
                <div class="info-item" v-if="service.health !== undefined">
                  <span class="label">健康度：</span>
                  <el-progress 
                    :percentage="service.health" 
                    :status="service.health > 80 ? 'success' : service.health > 60 ? 'warning' : 'exception'"
                    :stroke-width="8"
                    :show-text="false"
                  />
                  <span class="health-value">{{ service.health }}%</span>
                </div>
                <div class="info-item" v-if="service.lastChecked">
                  <span class="label">最后检查：</span>
                  <span class="value time">{{ formatTime(service.lastChecked) }}</span>
                </div>
              </div>
              
              <div class="service-actions">
                <el-tooltip :content="service.status === 'running' ? '测试连接' : '启动服务'" placement="top">
                  <el-button 
                    circle 
                    size="small" 
                    :type="service.status === 'running' ? 'primary' : 'success'"
                    :loading="service.loading"
                    @click="testServiceConnection(service)"
                  >
                    <el-icon>
                      <Connection v-if="service.status === 'running'" />
                      <VideoPlay v-else />
                    </el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="重启服务" placement="top" v-if="service.status === 'running'">
                  <el-button 
                    circle 
                    size="small" 
                    type="warning"
                    :loading="service.restarting"
                    @click="restartService(service)"
                  >
                    <el-icon><RefreshRight /></el-icon>
                  </el-button>
                </el-tooltip>
                <el-tooltip content="查看详情" placement="top">
                  <el-button 
                    circle 
                    size="small" 
                    @click="viewServiceDetails(service)"
                  >
                    <el-icon><InfoFilled /></el-icon>
                  </el-button>
                </el-tooltip>
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
    
    <!-- 服务详情对话框 -->
    <el-dialog
      v-model="serviceDetailsVisible"
      :title="`${currentService?.name || ''} 服务详情`"
      width="600px"
    >
      <div class="service-details-content" v-if="currentService">
        <div class="details-item">
          <span class="details-label">服务名称：</span>
          <span class="details-value">{{ currentService.name }}</span>
        </div>
        <div class="details-item">
          <span class="details-label">服务状态：</span>
          <span class="details-value" :class="currentService.status">
            {{ currentService.status === 'running' ? '运行中' : '已停止' }}
          </span>
        </div>
        <div class="details-item">
          <span class="details-label">连接地址：</span>
          <span class="details-value">{{ currentService.url }}</span>
        </div>
        <div class="details-item" v-if="currentService.health !== undefined">
          <span class="details-label">健康状况：</span>
          <el-progress 
            :percentage="currentService.health" 
            :status="currentService.health > 80 ? 'success' : currentService.health > 60 ? 'warning' : 'exception'"
          />
        </div>
        <div class="details-item" v-if="currentService.lastChecked">
          <span class="details-label">最后检查时间：</span>
          <span class="details-value">{{ new Date(currentService.lastChecked).toLocaleString() }}</span>
        </div>
        
        <!-- 健康报告详情 -->
        <div class="details-section" v-if="currentService.healthReport && currentService.healthReport.checks">
          <div class="details-header">
            <h3>健康检查报告</h3>
          </div>
          <el-table :data="currentService.healthReport.checks" stripe style="width: 100%">
            <el-table-column prop="name" label="检查项" width="150" />
            <el-table-column prop="value" label="结果" />
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag
                  :type="scope.row.status === 'passed' ? 'success' : scope.row.status === 'warning' ? 'warning' : 'danger'"
                  size="small"
                  effect="dark"
                >
                  {{ scope.row.status === 'passed' ? '通过' : scope.row.status === 'warning' ? '警告' : '失败' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        
        <!-- 服务基本信息 -->
        <div class="details-section" v-if="currentService.details">
          <div class="details-header">
            <h3>服务详情</h3>
          </div>
          <el-descriptions border :column="1" size="small">
            <el-descriptions-item v-for="(value, key) in currentService.details" :key="key" :label="formatLabel(key)">
              {{ value }}
            </el-descriptions-item>
          </el-descriptions>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="serviceDetailsVisible = false">关闭</el-button>
          <el-button 
            type="primary" 
            :loading="currentService?.loading"
            @click="testServiceConnection(currentService)"
          >
            {{ currentService?.status === 'running' ? '测试连接' : '启动服务' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Folder, 
  DataAnalysis, 
  Refresh, 
  VideoPlay, 
  CaretTop, 
  CaretBottom, 
  Minus,
  Connection,
  Document,
  Setting,
  User,
  PictureFilled,
  InfoFilled,
  CircleCheckFilled,
  CircleCloseFilled,
  RefreshRight,
  DataBoard
} from '@element-plus/icons-vue'
import { HDFSService } from '../services'
import { HiveService } from '../services'
import { SparkService } from '../services'
import { KafkaService } from '../services'
import ServiceStatusApi from '../api/service'

export default {
  name: 'HomePage',
  components: {
    Folder,
    DataAnalysis,
    Refresh,
    VideoPlay,
    CaretTop,
    CaretBottom,
    Minus,
    Connection,
    Document,
    Setting,
    User,
    PictureFilled,
    InfoFilled,
    CircleCheckFilled,
    CircleCloseFilled,
    RefreshRight,
    DataBoard
  },
  setup() {
    const router = useRouter()
    const loadingServices = ref(true)
    const refreshingServices = ref(false)
    const serviceDetailsVisible = ref(false)
    const currentService = ref(null)
    const autoRefreshInterval = ref(null)
    
    // 响应式服务状态
    const services = ref([
      { 
        id: 1, 
        name: 'HDFS', 
        status: 'loading', 
        url: 'hdfs://shiguang:9000', 
        icon: 'Folder',
        health: 0,
        loading: false,
        restarting: false,
        lastChecked: null,
        service: HDFSService
      },
      { 
        id: 2,
        name: 'Hive', 
        status: 'loading', 
        url: 'jdbc:hive2://localhost:10000', 
        icon: 'Document',
        health: 0,
        loading: false,
        restarting: false,
        lastChecked: null,
        service: HiveService
      },
      { 
        id: 3,
        name: 'Spark', 
        status: 'loading', 
        url: 'local[*]', 
        icon: 'DataAnalysis',
        health: 0,
        loading: false,
        restarting: false,
        lastChecked: null,
        service: SparkService
      },
      { 
        id: 4,
        name: 'Kafka', 
        status: 'loading', 
        url: 'shiguang:9092', 
        icon: 'Connection',
        health: 0,
        loading: false,
        restarting: false,
        lastChecked: null,
        service: KafkaService
      }
    ])

    const stats = ref([
      { title: '数据源总数', value: '24', icon: 'DataLine', color: '#409EFF', trend: 8 },
      { title: '数据表总数', value: '156', icon: 'Document', color: '#67C23A', trend: 12 },
      { title: '总存储容量', value: '2.4TB', icon: 'Folder', color: '#E6A23C', trend: 5 },
      { title: '分析任务数', value: '48', icon: 'DataAnalysis', color: '#F56C6C', trend: -3 }
    ])

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
      },
      { 
        title: '农业大数据平台', 
        description: '高科技风格的大数据监控大屏', 
        icon: 'DataBoard',
        route: '/agriculture-dashboard'
      }
    ]

    // 获取所有服务状态
    const fetchAllServiceStatus = async () => {
      loadingServices.value = true
      
      try {
        // 使用ServiceStatusApi获取所有服务状态
        const result = await ServiceStatusApi.getAllStatus();
        
        if (result && result.services && Array.isArray(result.services)) {
          // 使用API返回的数据更新本地服务状态
          result.services.forEach(serviceData => {
            const serviceIndex = services.value.findIndex(s => s.name === serviceData.name);
            if (serviceIndex !== -1) {
              // 更新现有服务的状态信息
              const service = services.value[serviceIndex];
              service.status = serviceData.status;
              service.health = serviceData.health || 0;
              service.url = serviceData.url || service.url;
              service.lastChecked = serviceData.lastChecked;
              service.details = serviceData.details;
            }
          });
          
          ElMessage.success('服务状态已更新');
        } else {
          // 如果无法获取状态数据，使用模拟数据
          for (const service of services.value) {
            await mockServiceStatus(service);
          }
        }
      } catch (error) {
        console.error('获取服务状态失败:', error);
        
        // 出错时使用模拟数据
        for (const service of services.value) {
          await mockServiceStatus(service);
        }
        
        ElMessage.warning('无法连接到服务状态API，使用模拟数据');
      } finally {
        loadingServices.value = false;
      }
    }
    
    // 模拟获取服务状态 (仅在API请求失败时使用)
    const mockServiceStatus = async (service) => {
      // 模拟网络延迟
      await new Promise(resolve => setTimeout(resolve, 300 + Math.random() * 500))
      
      // 80%概率服务正常运行
      service.status = Math.random() > 0.2 ? 'running' : 'stopped'
      service.health = service.status === 'running' ? (70 + Math.floor(Math.random() * 30)) : 0
      service.lastChecked = new Date().toISOString()
    }
    
    // 刷新所有服务状态
    const refreshAllServices = async () => {
      refreshingServices.value = true
      await fetchAllServiceStatus()
      refreshingServices.value = false
    }
    
    // 测试服务连接
    const testServiceConnection = async (service) => {
      service.loading = true
      
      try {
        if (service.status !== 'running') {
          // 如果服务未运行，显示确认对话框
          try {
            await ElMessageBox.confirm(
              `${service.name}服务当前未运行，是否尝试启动？`,
              '服务启动',
              {
                confirmButtonText: '启动',
                cancelButtonText: '取消',
                type: 'warning'
              }
            )
            
            // 用户确认启动服务
            ElMessage({
              type: 'info',
              message: `正在尝试启动${service.name}服务...`
            })
            
            // 调用API启动服务
            const result = await ServiceStatusApi.startService(service.name);
            
            if (result && result.success) {
              // 更新服务状态
              service.status = 'running';
              service.health = result.service?.health || 85;
              service.lastChecked = new Date().toISOString();
              
              ElMessage.success(`${service.name}服务已启动`);
            } else {
              throw new Error(`启动${service.name}服务失败`);
            }
          } catch (error) {
            if (error !== 'cancel') {
              ElMessage.error(`启动${service.name}服务失败: ${error.message || '未知错误'}`);
            }
            return;
          }
        } else {
          // 服务已运行，测试连接
          // 获取最新服务状态
          const result = await ServiceStatusApi.getServiceStatus(service.name);
          
          if (result && result.status === 'running') {
            ElMessage.success(`${service.name}连接测试成功`);
            
            // 更新服务详情
            service.health = result.health || service.health;
            service.lastChecked = result.lastChecked || new Date().toISOString();
            service.details = result.details || service.details;
          } else {
            ElMessage.error(`${service.name}连接测试失败，请检查服务状态`);
          }
        }
      } catch (error) {
        console.error(`测试${service.name}连接失败:`, error);
        ElMessage.error(`测试${service.name}连接失败: ${error.message || '未知错误'}`);
      } finally {
        service.loading = false;
      }
    }
    
    // 重启服务
    const restartService = async (service) => {
      try {
        await ElMessageBox.confirm(
          `确定要重启${service.name}服务吗？`,
          '重启服务',
          {
            confirmButtonText: '重启',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        service.restarting = true;
        
        // 调用API重启服务
        const result = await ServiceStatusApi.restartService(service.name);
        
        if (result && result.success) {
          // 更新服务状态
          service.status = 'running';
          service.health = result.service?.health || 90;
          service.lastChecked = new Date().toISOString();
          
          ElMessage.success(`${service.name}服务已重启`);
        } else {
          throw new Error(`重启${service.name}服务失败`);
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error(`重启${service.name}服务失败:`, error);
          ElMessage.error(`重启${service.name}服务失败: ${error.message || '未知错误'}`);
        }
      } finally {
        service.restarting = false;
      }
    }
    
    // 查看服务详情
    const viewServiceDetails = async (service) => {
      try {
        // 获取服务健康报告
        const healthReport = await ServiceStatusApi.getServiceHealthReport(service.name);
        
        // 创建一个完整的服务详情对象
        const detailedService = { 
          ...service,
          healthReport: healthReport
        };
        
        currentService.value = detailedService;
        serviceDetailsVisible.value = true;
      } catch (error) {
        console.error(`获取${service.name}健康报告失败:`, error);
        
        // 如果无法获取健康报告，仍然显示基本信息
        currentService.value = { ...service };
        serviceDetailsVisible.value = true;
      }
    }
    
    // 获取状态类型
    const getStatusType = (status) => {
      if (status === 'loading') return 'info'
      return status === 'running' ? 'success' : 'danger'
    }
    
    // 格式化时间
    const formatTime = (timeString) => {
      const date = new Date(timeString)
      return date.toLocaleTimeString()
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
      router.push('/agriculture-monitor')
    }

    const goToDashboard = () => {
      router.push('/agriculture-dashboard')
    }

    const goToRoute = (route) => {
      router.push(route)
    }
    
    // 设置自动刷新服务状态
    const setupAutoRefresh = () => {
      // 每60秒自动刷新一次服务状态
      autoRefreshInterval.value = setInterval(async () => {
        await fetchAllServiceStatus()
      }, 60000)
    }
    
    // 格式化详情标签
    const formatLabel = (key) => {
      // 将驼峰命名或下划线命名转换为友好的显示文本
      const formatted = key.replace(/([A-Z])/g, ' $1')
                          .replace(/_/g, ' ')
                          .toLowerCase();
      // 首字母大写
      return formatted.charAt(0).toUpperCase() + formatted.slice(1);
    }
    
    onMounted(async () => {
      // 初始加载服务状态
      await fetchAllServiceStatus()
      
      // 设置自动刷新
      setupAutoRefresh()
    })
    
    onUnmounted(() => {
      // 清除自动刷新定时器
      if (autoRefreshInterval.value) {
        clearInterval(autoRefreshInterval.value)
      }
    })

    return {
      services,
      stats,
      quickLinks,
      loadingServices,
      refreshingServices,
      currentService,
      serviceDetailsVisible,
      getStatusType,
      refreshAllServices,
      testServiceConnection,
      restartService,
      viewServiceDetails,
      formatTime,
      goToHdfs,
      goToHive,
      goToAnalysis,
      goToMonitor,
      goToDashboard,
      goToRoute,
      formatLabel
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
  margin: 0;
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

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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

.welcome-actual-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 服务状态区域样式 */
.service-card {
  border-radius: 8px;
  margin-bottom: 20px;
  transition: all 0.3s;
  overflow: hidden;
}

.service-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
}

.service-header {
  display: flex;
  align-items: center;
  padding: 15px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.service-header.running {
  background: linear-gradient(to right, #ecf5ff, #f5f7fa);
  border-bottom: 1px solid #d9ecff;
}

.service-header.stopped {
  background: linear-gradient(to right, #fef0f0, #f5f7fa);
  border-bottom: 1px solid #fde2e2;
}

.service-icon {
  width: 32px;
  height: 32px;
  background-color: white;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.service-icon .el-icon {
  font-size: 18px;
  color: #409EFF;
}

.service-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  flex: 1;
}

.status-tag {
  margin-left: 10px;
}

.service-content {
  padding: 15px;
}

.service-info {
  margin-bottom: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.info-item .label {
  width: 70px;
  color: #909399;
  font-size: 13px;
}

.info-item .value {
  flex: 1;
  font-size: 13px;
  color: #606266;
}

.info-item .value.running {
  color: #67C23A;
}

.info-item .value.stopped {
  color: #F56C6C;
}

.info-item .value.url {
  font-family: monospace;
  background-color: #f5f7fa;
  padding: 2px 5px;
  border-radius: 3px;
  font-size: 12px;
}

.info-item .status-icon {
  margin-right: 5px;
}

.service-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.health-value {
  margin-left: 5px;
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

/* 服务详情对话框样式 */
.service-details-content {
  padding: 10px 0;
}

.details-item {
  display: flex;
  margin-bottom: 15px;
}

.details-label {
  width: 120px;
  color: #909399;
}

.details-value {
  flex: 1;
  color: #606266;
}

.details-value.running {
  color: #67C23A;
}

.details-value.stopped {
  color: #F56C6C;
}

.details-code {
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  overflow-x: auto;
  margin-top: 5px;
}

.details-section {
  margin-top: 20px;
  border-top: 1px solid #ebeef5;
  padding-top: 15px;
}

.details-header {
  margin-bottom: 10px;
}

.details-header h3 {
  font-size: 16px;
  color: #303133;
  margin: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
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