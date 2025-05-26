<template>
  <div class="notification-list">
    <div class="page-header">
      <div class="title-container">
        <h2>消息通知中心</h2>
        <span class="unread-badge" v-if="unreadCount > 0">{{ unreadCount }} 条未读</span>
      </div>
      <div class="actions">
        <el-button type="primary" @click="markAllAsRead" :disabled="unreadCount === 0">
          标记全部已读
        </el-button>
        <el-button type="danger" @click="clearAllNotifications" :disabled="notifications.length === 0">
          清空通知
        </el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab" @tab-click="handleTabChange">
      <el-tab-pane label="所有通知" name="all"></el-tab-pane>
      <el-tab-pane label="未读通知" name="unread"></el-tab-pane>
    </el-tabs>

    <el-form :inline="true" class="search-form" @submit.native.prevent>
      <el-form-item>
        <el-input v-model="searchKeyword" placeholder="搜索通知..." @keyup.enter.native="loadNotifications" clearable>
          <template #append>
            <el-button @click="loadNotifications">搜索</el-button>
          </template>
        </el-input>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchType" placeholder="通知类型" clearable @change="loadNotifications">
          <el-option label="信息" value="INFO"></el-option>
          <el-option label="警告" value="WARNING"></el-option>
          <el-option label="错误" value="ERROR"></el-option>
          <el-option label="成功" value="SUCCESS"></el-option>
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-select v-model="searchLevel" placeholder="通知级别" clearable @change="loadNotifications">
          <el-option label="低" value="LOW"></el-option>
          <el-option label="中" value="MEDIUM"></el-option>
          <el-option label="高" value="HIGH"></el-option>
          <el-option label="紧急" value="URGENT"></el-option>
        </el-select>
      </el-form-item>
    </el-form>

    <div v-if="loading" class="loading-container">
      <el-skeleton :rows="5" animated />
    </div>
    
    <el-empty v-else-if="notifications.length === 0" description="暂无通知消息"></el-empty>

    <div v-else class="notification-container">
      <el-card v-for="notification in notifications" :key="notification.id" 
               class="notification-card" 
               :class="{
                 'unread': !notification.read,
                 'confirmed': notification.confirmed,
                 'need-confirm': notification.needConfirm && !notification.confirmed
               }">
        <div class="notification-header">
          <div class="notification-type-badge" :class="getBadgeClass(notification.type)">
            {{ getTypeText(notification.type) }}
            <span class="notification-level" :class="getLevelClass(notification.level)">{{ getLevelText(notification.level) }}</span>
          </div>
          <div class="notification-time">{{ formatDate(notification.createdAt) }}</div>
        </div>
        
        <div class="notification-title" :class="{'bold': !notification.read}">{{ notification.title }}</div>
        <div class="notification-content">{{ notification.content }}</div>
        
        <div class="notification-source" v-if="notification.source">来源: {{ notification.source }}</div>
        
        <div v-if="notification.resourceType && notification.resourceId" class="notification-resource">
          资源: {{ notification.resourceType }} ({{ notification.resourceId }})
          <el-link v-if="notification.link" type="primary" :href="notification.link" target="_blank">查看详情</el-link>
        </div>
        
        <div class="notification-footer">
          <el-button size="small" v-if="!notification.read" @click="markAsRead(notification)">标记为已读</el-button>
          <el-button type="primary" size="small" v-if="notification.needConfirm && !notification.confirmed" @click="confirmNotification(notification)">
            确认
          </el-button>
          <el-popconfirm title="确定要删除这条通知吗？" @confirm="deleteNotification(notification.id)">
            <template #reference>
              <el-button size="small" type="danger" plain>删除</el-button>
            </template>
          </el-popconfirm>
        </div>
      </el-card>
    </div>

    <div class="pagination-container" v-if="notifications.length > 0">
      <el-pagination
        :current-page.sync="currentPage"
        :page-sizes="[10, 20, 50]"
        :page-size.sync="pageSize"
        layout="total, sizes, prev, pager, next"
        :total="totalElements"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange">
      </el-pagination>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed, toRefs } from 'vue'
import { ElMessage, ElNotification } from 'element-plus'
import dayjs from 'dayjs'
import NotificationService from '../../services/notification.service'

export default {
  name: 'NotificationList',
  
  setup() {
    const state = reactive({
      notifications: [],
      activeTab: 'all',
      searchKeyword: '',
      searchType: '',
      searchLevel: '',
      currentPage: 1,
      pageSize: 10,
      totalElements: 0,
      loading: false,
      unreadCount: 0
    })
    
    const loadNotifications = async () => {
      try {
        state.loading = true
        
        const filters = {};
        
        // 添加搜索参数
        if (state.searchKeyword) filters.keyword = state.searchKeyword
        if (state.searchType) filters.type = state.searchType
        if (state.searchLevel) filters.level = state.searchLevel
        
        let response = null;
        
        if (state.activeTab === 'all') {
          // 分页加载所有通知
          response = await NotificationService.getMyNotificationsPage(
            state.currentPage - 1, // 后端从0开始计数
            state.pageSize,
            'createdAt',
            'DESC',
            filters
          )
          
          if (response.success) {
            state.notifications = response.data.content || []
            state.totalElements = response.data.totalElements || 0
          }
        } else {
          // 加载未读通知
          response = await NotificationService.getMyUnreadNotifications(filters)
          
          if (response.success) {
            // 确保响应数据是数组
            state.notifications = Array.isArray(response.data) ? response.data : []
            state.totalElements = state.notifications.length
          }
        }
        
        if (!response.success) {
          ElMessage.error('获取通知失败: ' + response.message)
        }
        
        // 加载未读通知数量
        await loadUnreadCount()
      } catch (error) {
        ElMessage.error('获取通知异常: ' + error.message)
      } finally {
        state.loading = false
      }
    }
    
    const loadUnreadCount = async () => {
      try {
        const response = await NotificationService.getUnreadCount()
        if (response.success) {
          state.unreadCount = response.data
        }
      } catch (error) {
        console.error('获取未读通知数量失败:', error)
      }
    }
    
    const markAsRead = async (notification) => {
      try {
        // 确保notification和id有效
        if (!notification || !notification.id) {
          ElMessage.warning('无效的通知ID')
          return
        }
        
        const response = await NotificationService.markAsRead(notification.id)
        if (response.success) {
          notification.read = true
          notification.readAt = new Date().toISOString()
          loadUnreadCount()
          ElMessage.success('已标记为已读')
        } else {
          ElMessage.error('操作失败: ' + response.message)
        }
      } catch (error) {
        ElMessage.error('操作异常: ' + error.message)
      }
    }
    
    const confirmNotification = async (notification) => {
      try {
        // 确保notification和id有效
        if (!notification || !notification.id) {
          ElMessage.warning('无效的通知ID')
          return
        }
        
        const response = await NotificationService.markAsConfirmed(notification.id)
        if (response.success) {
          notification.confirmed = true
          notification.confirmedAt = new Date().toISOString()
          ElMessage.success('已确认通知')
        } else {
          ElMessage.error('操作失败: ' + response.message)
        }
      } catch (error) {
        ElMessage.error('操作异常: ' + error.message)
      }
    }
    
    const deleteNotification = async (id) => {
      try {
        // 确保id有效
        if (!id) {
          ElMessage.warning('无效的通知ID')
          return
        }
        
        const response = await NotificationService.deleteNotification(id)
        if (response.success) {
          state.notifications = state.notifications.filter(n => n.id !== id)
          ElMessage.success('删除通知成功')
          loadUnreadCount()
        } else {
          ElMessage.error('删除失败: ' + response.message)
        }
      } catch (error) {
        ElMessage.error('操作异常: ' + error.message)
      }
    }
    
    const markAllAsRead = async () => {
      try {
        const response = await NotificationService.markAllAsRead()
        if (response.success) {
          const count = response.data
          ElMessage.success(`已将${count}条通知标记为已读`)
          state.notifications.forEach(n => {
            if (!n.read) {
              n.read = true
              n.readAt = new Date().toISOString()
            }
          })
          loadUnreadCount()
        } else {
          ElMessage.error('操作失败: ' + response.message)
        }
      } catch (error) {
        ElMessage.error('操作异常: ' + error.message)
      }
    }
    
    const clearAllNotifications = async () => {
      try {
        const response = await NotificationService.clearAllNotifications()
        if (response.success) {
          const count = response.data
          state.notifications = []
          state.unreadCount = 0
          ElMessage.success(`已清空${count}条通知`)
        } else {
          ElMessage.error('操作失败: ' + response.message)
        }
      } catch (error) {
        ElMessage.error('操作异常: ' + error.message)
      }
    }
    
    const handleTabChange = () => {
      state.currentPage = 1
      loadNotifications()
    }
    
    const handleSizeChange = (val) => {
      state.pageSize = val
      loadNotifications()
    }
    
    const handleCurrentChange = (val) => {
      state.currentPage = val
      loadNotifications()
    }
    
    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
    }
    
    const getBadgeClass = (type) => {
      switch (type) {
        case 'INFO': return 'type-info'
        case 'WARNING': return 'type-warning'
        case 'ERROR': return 'type-error'
        case 'SUCCESS': return 'type-success'
        default: return 'type-info'
      }
    }
    
    const getLevelClass = (level) => {
      switch (level) {
        case 'LOW': return 'level-low'
        case 'MEDIUM': return 'level-medium'
        case 'HIGH': return 'level-high'
        case 'URGENT': return 'level-urgent'
        default: return 'level-medium'
      }
    }
    
    const getTypeText = (type) => {
      switch (type) {
        case 'INFO': return '信息'
        case 'WARNING': return '警告'
        case 'ERROR': return '错误'
        case 'SUCCESS': return '成功'
        default: return '信息'
      }
    }
    
    const getLevelText = (level) => {
      switch (level) {
        case 'LOW': return '低'
        case 'MEDIUM': return '中'
        case 'HIGH': return '高'
        case 'URGENT': return '紧急'
        default: return '中'
      }
    }
    
    onMounted(() => {
      loadNotifications()
    })
    
    return {
      ...toRefs(state),
      loadNotifications,
      markAsRead,
      confirmNotification,
      deleteNotification,
      markAllAsRead,
      clearAllNotifications,
      handleTabChange,
      handleSizeChange,
      handleCurrentChange,
      formatDate,
      getBadgeClass,
      getLevelClass,
      getTypeText,
      getLevelText
    }
  }
}
</script>

<style scoped>
.notification-list {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.title-container {
  display: flex;
  align-items: center;
}

.unread-badge {
  background-color: #f56c6c;
  color: white;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  margin-left: 10px;
}

.search-form {
  margin-bottom: 20px;
}

.notification-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.notification-card {
  transition: all 0.3s;
  border-left: 4px solid transparent;
}

.notification-card.unread {
  border-left-color: #409EFF;
  background-color: #f5f7fa;
}

.notification-card.need-confirm {
  border-left-color: #E6A23C;
}

.notification-card.confirmed {
  border-left-color: #67C23A;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.notification-type-badge {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.type-info {
  background-color: #e6f1fc;
  color: #409EFF;
}

.type-warning {
  background-color: #fdf6ec;
  color: #E6A23C;
}

.type-error {
  background-color: #fef0f0;
  color: #F56C6C;
}

.type-success {
  background-color: #f0f9eb;
  color: #67C23A;
}

.notification-level {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
}

.level-low {
  background-color: #e6f1fc;
  color: #409EFF;
}

.level-medium {
  background-color: #fdf6ec;
  color: #E6A23C;
}

.level-high {
  background-color: #fef0f0;
  color: #F56C6C;
}

.level-urgent {
  background-color: #f56c6c;
  color: white;
}

.notification-title {
  font-size: 16px;
  margin-bottom: 10px;
}

.bold {
  font-weight: bold;
}

.notification-content {
  color: #606266;
  margin-bottom: 10px;
  white-space: pre-line;
}

.notification-source,
.notification-resource {
  color: #909399;
  font-size: 12px;
  margin-bottom: 5px;
}

.notification-footer {
  margin-top: 15px;
  display: flex;
  gap: 10px;
}

.notification-time {
  color: #909399;
  font-size: 12px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.loading-container {
  padding: 20px 0;
}
</style> 