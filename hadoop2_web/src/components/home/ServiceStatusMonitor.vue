<template>
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
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Refresh, 
  VideoPlay, 
  Connection,
  InfoFilled,
  CircleCheckFilled,
  CircleCloseFilled,
  RefreshRight
} from '@element-plus/icons-vue'
import { HDFSService } from '@/services'
import { HiveService } from '@/services'
import { SparkService } from '@/services'
import { KafkaService } from '@/services'
import ServiceStatusApi from '@/api/service'

export default {
  name: 'ServiceStatusMonitor',
  components: {
    Refresh,
    VideoPlay,
    Connection,
    InfoFilled,
    CircleCheckFilled,
    CircleCloseFilled,
    RefreshRight
  },
  props: {
    // 是否自动刷新
    autoRefresh: {
      type: Boolean,
      default: true
    },
    // 自动刷新间隔（毫秒）
    refreshInterval: {
      type: Number,
      default: 60000
    }
  },
  emits: ['service-status-change'],
  setup(props, { emit }) {
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

    // 获取所有服务状态
    const fetchAllServicesStatus = async () => {
      try {
        loadingServices.value = true
        
        // 使用模拟数据（开发环境或API请求失败时）
        const useMockData = true; // 设置为true使用模拟数据，false则使用实际API
        
        if (useMockData) {
          // 使用模拟数据
          services.value.forEach((service) => {
            service.status = 'running';
            service.health = Math.floor(Math.random() * 20) + 80; // 80-100之间的随机健康度
            service.details = {
              version: '3.2.0',
              runningTime: '5天16小时',
              nodeCount: '3',
              memoryUsage: '65%'
            };
            service.lastChecked = new Date();
          });
          
          // 触发服务状态变更事件
          emit('service-status-change', services.value);
          return;
        }
        
        // 以下是原来的实际API调用逻辑
        // 并行请求各服务状态
        await Promise.all(services.value.map(async (service) => {
          try {
            const response = await service.service.checkStatus()
            
            // 更新服务状态
            service.status = response.status || 'stopped'
            service.health = response.health || 0
            service.details = response.details || {}
            service.lastChecked = new Date()
            
            if (response.healthReport) {
              service.healthReport = response.healthReport
            }
          } catch (error) {
            console.error(`获取${service.name}状态失败:`, error)
            // 设置服务状态为异常而非停止（这样用户可以看到更友好的信息）
            service.status = 'stopped'
            service.health = 0
          }
        }))
        
        // 触发服务状态变更事件
        emit('service-status-change', services.value)
      } catch (error) {
        console.error('获取服务状态失败:', error)
      } finally {
        loadingServices.value = false
      }
    }

    // 刷新所有服务状态
    const refreshAllServices = async () => {
      try {
        refreshingServices.value = true
        await fetchAllServicesStatus()
        ElMessage.success('服务状态已刷新')
      } catch (error) {
        ElMessage.error('刷新服务状态失败')
      } finally {
        refreshingServices.value = false
      }
    }

    // 测试服务连接
    const testServiceConnection = async (service) => {
      if (!service) return
      
      try {
        // 更新按钮状态
        service.loading = true
        
        let result
        if (service.status === 'running') {
          // 测试连接
          result = await service.service.testConnection()
          ElMessage.success(`${service.name}连接测试成功`)
        } else {
          // 启动服务
          result = await service.service.startService()
          
          if (result.success) {
            ElMessage.success(`${service.name}服务启动成功`)
            service.status = 'running'
            service.health = result.health || 75
            
            // 触发服务状态变更事件
            emit('service-status-change', services.value)
          } else {
            ElMessage.error(result.message || `${service.name}服务启动失败`)
          }
        }
      } catch (error) {
        console.error(`${service.status === 'running' ? '测试连接' : '启动服务'}失败:`, error)
        ElMessage.error(`${service.name}${service.status === 'running' ? '连接测试' : '服务启动'}失败`)
      } finally {
        service.loading = false
      }
    }

    // 重启服务
    const restartService = async (service) => {
      try {
        if (service.restarting) return

        const confirmResult = await ElMessageBox.confirm(
          `确定要重启 ${service.name} 服务吗？这可能会导致正在进行的操作中断。`,
          '确认重启',
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        if (confirmResult === 'confirm') {
          service.restarting = true
          
          // 调用重启接口
          const result = await service.service.restartService()
          
          if (result.success) {
            ElMessage.success(`${service.name}服务重启成功`)
            
            // 刷新服务状态
            service.status = 'running'
            service.health = result.health || 80
            service.lastChecked = new Date()
            
            // 触发服务状态变更事件
            emit('service-status-change', services.value)
          } else {
            ElMessage.error(result.message || `${service.name}服务重启失败`)
          }
        }
      } catch (error) {
        if (error !== 'cancel') {
          console.error('重启服务失败:', error)
          ElMessage.error(`重启${service.name}服务失败`)
        }
      } finally {
        service.restarting = false
      }
    }

    // 查看服务详情
    const viewServiceDetails = (service) => {
      currentService.value = service
      serviceDetailsVisible.value = true
    }

    // 获取状态类型
    const getStatusType = (status) => {
      switch (status) {
        case 'running':
          return 'success'
        case 'loading':
          return 'info'
        case 'warning':
          return 'warning'
        default:
          return 'danger'
      }
    }

    // 格式化标签
    const formatLabel = (key) => {
      if (!key) return ''
      
      // 将驼峰命名转换为空格分隔的词组，并首字母大写
      const formatted = key.replace(/([A-Z])/g, ' $1')
      return formatted.charAt(0).toUpperCase() + formatted.slice(1)
    }

    // 格式化时间
    const formatTime = (timestamp) => {
      if (!timestamp) return '未知'
      
      const date = new Date(timestamp)
      const now = new Date()
      const diffMs = now - date
      
      // 如果在1小时内，显示xx分钟前
      if (diffMs < 60 * 60 * 1000) {
        const minutes = Math.floor(diffMs / (60 * 1000))
        return `${minutes}分钟前`
      }
      
      // 如果是今天，显示今天 HH:MM
      if (date.toDateString() === now.toDateString()) {
        return `今天 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}`
      }
      
      // 其他情况，显示完整日期时间
      return date.toLocaleString()
    }

    // 设置自动刷新
    const setupAutoRefresh = () => {
      if (props.autoRefresh && props.refreshInterval > 0) {
        autoRefreshInterval.value = setInterval(() => {
          fetchAllServicesStatus()
        }, props.refreshInterval)
      }
    }

    // 清除自动刷新
    const clearAutoRefresh = () => {
      if (autoRefreshInterval.value) {
        clearInterval(autoRefreshInterval.value)
        autoRefreshInterval.value = null
      }
    }

    // 组件挂载时获取服务状态
    onMounted(() => {
      fetchAllServicesStatus()
      setupAutoRefresh()
    })

    // 组件卸载时清除自动刷新
    onUnmounted(() => {
      clearAutoRefresh()
    })

    return {
      services,
      loadingServices,
      refreshingServices,
      currentService,
      serviceDetailsVisible,
      refreshAllServices,
      testServiceConnection,
      restartService,
      viewServiceDetails,
      getStatusType,
      formatLabel,
      formatTime
    }
  }
}
</script>

<style scoped>
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
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

.service-card {
  border-radius: 8px;
  margin-bottom: 20px;
  overflow: hidden;
  transition: all 0.3s;
}

.service-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
}

.service-header {
  display: flex;
  align-items: center;
  padding: 12px 15px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
}

.service-header.running {
  background: linear-gradient(135deg, #ecf5ff 0%, #f0f9eb 100%);
}

.service-header.stopped {
  background: linear-gradient(135deg, #fef0f0 0%, #fff1e6 100%);
}

.service-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
}

.service-name {
  flex: 1;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.status-tag {
  font-size: 12px;
}

.service-content {
  padding: 15px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.service-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  align-items: center;
  font-size: 14px;
}

.label {
  color: #909399;
  width: 60px;
  flex-shrink: 0;
}

.value {
  color: #606266;
}

.value.running {
  color: #67c23a;
  display: flex;
  align-items: center;
}

.value.stopped {
  color: #f56c6c;
  display: flex;
  align-items: center;
}

.status-icon {
  margin-right: 4px;
  font-size: 16px;
}

.url {
  word-break: break-all;
}

.time {
  font-size: 13px;
  color: #909399;
}

.health-value {
  margin-left: 8px;
  font-size: 13px;
  font-weight: 500;
}

.service-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 5px;
}

/* 服务详情对话框 */
.service-details-content {
  padding: 10px 0;
}

.details-item {
  margin-bottom: 15px;
  display: flex;
  align-items: center;
}

.details-item:last-child {
  margin-bottom: 0;
}

.details-label {
  width: 120px;
  font-weight: 500;
  color: #606266;
}

.details-value {
  color: #303133;
}

.details-value.running {
  color: #67c23a;
}

.details-value.stopped {
  color: #f56c6c;
}

.details-section {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.details-header {
  margin-bottom: 15px;
}

.details-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 15px;
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