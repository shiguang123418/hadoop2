<template>
  <div class="notification-manage">
    <div class="page-header">
      <h2>通知管理</h2>
      <div class="actions">
        <el-button type="primary" @click="showCreateSystemDialog">创建系统通知</el-button>
        <el-button type="success" @click="showCreateUserDialog">创建用户通知</el-button>
      </div>
    </div>

    <!-- 高级搜索 -->
    <el-collapse>
      <el-collapse-item title="高级搜索" name="1">
        <el-form :inline="true" :model="searchForm" class="search-form" @submit.native.prevent>
          <el-form-item label="用户ID">
            <el-input v-model="searchForm.userId" placeholder="用户ID" clearable></el-input>
          </el-form-item>
          <el-form-item label="通知类型">
            <el-select v-model="searchForm.type" placeholder="通知类型" clearable>
              <el-option label="信息" value="INFO"></el-option>
              <el-option label="警告" value="WARNING"></el-option>
              <el-option label="错误" value="ERROR"></el-option>
              <el-option label="成功" value="SUCCESS"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="通知级别">
            <el-select v-model="searchForm.level" placeholder="通知级别" clearable>
              <el-option label="低" value="LOW"></el-option>
              <el-option label="中" value="MEDIUM"></el-option>
              <el-option label="高" value="HIGH"></el-option>
              <el-option label="紧急" value="URGENT"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="通知来源">
            <el-select v-model="searchForm.source" placeholder="通知来源" clearable>
              <el-option label="系统" value="SYSTEM"></el-option>
              <el-option label="安全" value="SECURITY"></el-option>
              <el-option label="用户管理" value="USER"></el-option>
              <el-option label="任务管理" value="TASK"></el-option>
              <el-option label="数据管理" value="DATA"></el-option>
              <el-option label="审计日志" value="AUDIT"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="阅读状态">
            <el-select v-model="searchForm.read" placeholder="阅读状态" clearable>
              <el-option label="已读" :value="true"></el-option>
              <el-option label="未读" :value="false"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="确认状态">
            <el-select v-model="searchForm.confirmed" placeholder="确认状态" clearable>
              <el-option label="已确认" :value="true"></el-option>
              <el-option label="未确认" :value="false"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="需要确认">
            <el-select v-model="searchForm.needConfirm" placeholder="需要确认" clearable>
              <el-option label="是" :value="true"></el-option>
              <el-option label="否" :value="false"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="创建时间">
            <el-date-picker
              v-model="dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              format="YYYY-MM-DD"
              value-format="YYYY-MM-DD HH:mm:ss"
              :default-time="['00:00:00', '23:59:59']"
            ></el-date-picker>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </el-form-item>
        </el-form>
      </el-collapse-item>
    </el-collapse>

    <!-- 表格 -->
    <div class="table-container">
      <el-table
        v-loading="loading"
        :data="notificationList"
        border
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="id" label="ID" width="80"></el-table-column>
        <el-table-column prop="title" label="标题" show-overflow-tooltip></el-table-column>
        <el-table-column prop="type" label="类型" width="100">
          <template #default="scope">
            <el-tag :type="getTagType(scope.row.type)">{{ getTypeText(scope.row.type) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="level" label="级别" width="100">
          <template #default="scope">
            <el-tag :type="getLevelType(scope.row.level)" effect="dark">{{ getLevelText(scope.row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="接收用户" width="120">
          <template #default="scope">
            <span v-if="scope.row.userId">{{ scope.row.username || scope.row.userId }}</span>
            <span v-else>系统通知</span>
          </template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="120"></el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="scope">
            <div>
              <el-tag size="small" :type="scope.row.read ? 'success' : 'info'">
                {{ scope.row.read ? '已读' : '未读' }}
              </el-tag>
              <el-tag v-if="scope.row.needConfirm" size="small" :type="scope.row.confirmed ? 'success' : 'warning'" class="ml-5">
                {{ scope.row.confirmed ? '已确认' : '未确认' }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column fixed="right" label="操作" width="220">
          <template #default="scope">
            <el-button type="primary" size="small" @click="viewNotification(scope.row)">查看</el-button>
            <el-button 
              type="danger" 
              size="small" 
              @click="deleteNotification(scope.row.id)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          :current-page.sync="currentPage"
          :page-sizes="[10, 20, 50]"
          :page-size.sync="pageSize"
          layout="total, sizes, prev, pager, next"
          :total="totalElements"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        ></el-pagination>
      </div>
      
      <!-- 批量操作 -->
      <div class="batch-actions" v-if="selectedNotifications.length > 0">
        <el-button type="danger" @click="batchDelete">批量删除 ({{ selectedNotifications.length }})</el-button>
      </div>
    </div>

    <!-- 查看通知详情对话框 -->
    <el-dialog title="通知详情" v-model="detailDialogVisible" width="600px">
      <div v-if="currentNotification" class="notification-detail">
        <div class="notification-header">
          <div>
            <el-tag :type="getTagType(currentNotification.type)">{{ getTypeText(currentNotification.type) }}</el-tag>
            <el-tag :type="getLevelType(currentNotification.level)" effect="dark" class="ml-5">{{ getLevelText(currentNotification.level) }}</el-tag>
          </div>
          <div>{{ formatDate(currentNotification.createdAt) }}</div>
        </div>
        
        <h3>{{ currentNotification.title }}</h3>
        
        <el-divider></el-divider>
        
        <div class="detail-item">
          <span class="label">内容:</span>
          <pre class="content">{{ currentNotification.content }}</pre>
        </div>
        
        <div class="detail-item">
          <span class="label">来源:</span>
          <span>{{ currentNotification.source }}</span>
        </div>
        
        <div class="detail-item" v-if="currentNotification.userId">
          <span class="label">接收用户:</span>
          <span>{{ currentNotification.username || currentNotification.userId }}</span>
        </div>
        
        <div class="detail-item" v-else>
          <span class="label">接收用户:</span>
          <span>系统通知 (所有用户)</span>
        </div>
        
        <div v-if="currentNotification.resourceType" class="detail-item">
          <span class="label">资源类型:</span>
          <span>{{ currentNotification.resourceType }}</span>
        </div>
        
        <div v-if="currentNotification.resourceId" class="detail-item">
          <span class="label">资源ID:</span>
          <span>{{ currentNotification.resourceId }}</span>
        </div>
        
        <div v-if="currentNotification.link" class="detail-item">
          <span class="label">相关链接:</span>
          <el-link type="primary" :href="currentNotification.link" target="_blank">{{ currentNotification.link }}</el-link>
        </div>
        
        <el-divider></el-divider>
        
        <div class="detail-item">
          <span class="label">阅读状态:</span>
          <el-tag size="small" :type="currentNotification.read ? 'success' : 'info'">
            {{ currentNotification.read ? '已读' : '未读' }}
          </el-tag>
          <span v-if="currentNotification.readAt" class="ml-5">
            ({{ formatDate(currentNotification.readAt) }})
          </span>
        </div>
        
        <div v-if="currentNotification.needConfirm" class="detail-item">
          <span class="label">确认状态:</span>
          <el-tag size="small" :type="currentNotification.confirmed ? 'success' : 'warning'">
            {{ currentNotification.confirmed ? '已确认' : '未确认' }}
          </el-tag>
          <span v-if="currentNotification.confirmedAt" class="ml-5">
            ({{ formatDate(currentNotification.confirmedAt) }})
          </span>
        </div>
      </div>
    </el-dialog>

    <!-- 创建系统通知对话框 -->
    <el-dialog title="创建系统通知" v-model="systemDialogVisible" width="600px">
      <el-form :model="systemForm" :rules="notificationRules" ref="systemFormRef" label-width="100px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="systemForm.title" placeholder="请输入通知标题"></el-input>
        </el-form-item>
        
        <el-form-item label="内容" prop="content">
          <el-input v-model="systemForm.content" type="textarea" :rows="4" placeholder="请输入通知内容"></el-input>
        </el-form-item>
        
        <el-form-item label="通知类型" prop="type">
          <el-select v-model="systemForm.type" placeholder="请选择通知类型" style="width: 100%">
            <el-option label="信息" value="INFO"></el-option>
            <el-option label="警告" value="WARNING"></el-option>
            <el-option label="错误" value="ERROR"></el-option>
            <el-option label="成功" value="SUCCESS"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="通知级别" prop="level">
          <el-select v-model="systemForm.level" placeholder="请选择通知级别" style="width: 100%">
            <el-option label="低" value="LOW"></el-option>
            <el-option label="中" value="MEDIUM"></el-option>
            <el-option label="高" value="HIGH"></el-option>
            <el-option label="紧急" value="URGENT"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="通知来源" prop="source">
          <el-select v-model="systemForm.source" placeholder="请选择通知来源" style="width: 100%">
            <el-option label="系统" value="SYSTEM"></el-option>
            <el-option label="安全" value="SECURITY"></el-option>
            <el-option label="用户管理" value="USER"></el-option>
            <el-option label="任务管理" value="TASK"></el-option>
            <el-option label="数据管理" value="DATA"></el-option>
            <el-option label="审计日志" value="AUDIT"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="systemDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createSystemNotification">创建</el-button>
      </template>
    </el-dialog>
    
    <!-- 创建用户通知对话框 -->
    <el-dialog title="创建用户通知" v-model="userDialogVisible" width="600px">
      <el-form :model="userForm" :rules="userNotificationRules" ref="userFormRef" label-width="100px">
        <el-form-item label="用户ID" prop="userId">
          <el-input v-model.number="userForm.userId" placeholder="请输入用户ID" type="number"></el-input>
        </el-form-item>
        
        <el-form-item label="用户名称" prop="username">
          <el-input v-model="userForm.username" placeholder="请输入用户名称（可选）"></el-input>
        </el-form-item>
        
        <el-form-item label="标题" prop="title">
          <el-input v-model="userForm.title" placeholder="请输入通知标题"></el-input>
        </el-form-item>
        
        <el-form-item label="内容" prop="content">
          <el-input v-model="userForm.content" type="textarea" :rows="4" placeholder="请输入通知内容"></el-input>
        </el-form-item>
        
        <el-form-item label="通知类型" prop="type">
          <el-select v-model="userForm.type" placeholder="请选择通知类型" style="width: 100%">
            <el-option label="信息" value="INFO"></el-option>
            <el-option label="警告" value="WARNING"></el-option>
            <el-option label="错误" value="ERROR"></el-option>
            <el-option label="成功" value="SUCCESS"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="通知级别" prop="level">
          <el-select v-model="userForm.level" placeholder="请选择通知级别" style="width: 100%">
            <el-option label="低" value="LOW"></el-option>
            <el-option label="中" value="MEDIUM"></el-option>
            <el-option label="高" value="HIGH"></el-option>
            <el-option label="紧急" value="URGENT"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="通知来源" prop="source">
          <el-select v-model="userForm.source" placeholder="请选择通知来源" style="width: 100%">
            <el-option label="系统" value="SYSTEM"></el-option>
            <el-option label="安全" value="SECURITY"></el-option>
            <el-option label="用户管理" value="USER"></el-option>
            <el-option label="任务管理" value="TASK"></el-option>
            <el-option label="数据管理" value="DATA"></el-option>
            <el-option label="审计日志" value="AUDIT"></el-option>
          </el-select>
        </el-form-item>
        
        <el-form-item label="需要确认">
          <el-switch v-model="userForm.needConfirm"></el-switch>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="userDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createUserNotification">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, computed, toRefs } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import dayjs from 'dayjs'
import NotificationService from '../../services/notification.service'

export default {
  name: 'NotificationManage',
  
  setup() {
    const systemFormRef = ref(null)
    const userFormRef = ref(null)
    
    const state = reactive({
      loading: false,
      notificationList: [],
      totalElements: 0,
      currentPage: 1,
      pageSize: 10,
      
      selectedNotifications: [],
      
      dateRange: [],
      searchForm: {
        userId: '',
        type: '',
        level: '',
        source: '',
        read: null,
        needConfirm: null,
        confirmed: null,
        startDate: null,
        endDate: null
      },
      
      detailDialogVisible: false,
      systemDialogVisible: false,
      userDialogVisible: false,
      currentNotification: null,
      
      systemForm: {
        title: '',
        content: '',
        type: 'INFO',
        level: 'MEDIUM',
        source: 'SYSTEM'
      },
      
      userForm: {
        userId: '',
        username: '',
        title: '',
        content: '',
        type: 'INFO',
        level: 'MEDIUM',
        source: 'SYSTEM',
        needConfirm: false
      }
    })
    
    const notificationRules = {
      title: [
        { required: true, message: '请输入通知标题', trigger: 'blur' },
        { min: 2, max: 100, message: '标题长度在2-100个字符之间', trigger: 'blur' }
      ],
      content: [
        { required: true, message: '请输入通知内容', trigger: 'blur' }
      ],
      type: [
        { required: true, message: '请选择通知类型', trigger: 'change' }
      ],
      level: [
        { required: true, message: '请选择通知级别', trigger: 'change' }
      ],
      source: [
        { required: true, message: '请选择通知来源', trigger: 'change' }
      ]
    }
    
    const userNotificationRules = {
      ...notificationRules,
      userId: [
        { required: true, message: '请输入用户ID', trigger: 'blur' },
        { type: 'number', message: '用户ID必须为数字', trigger: 'blur' }
      ]
    }
    
    const loadNotifications = async () => {
      try {
        state.loading = true
        
        // 设置日期
        if (state.dateRange && state.dateRange.length === 2) {
          state.searchForm.startDate = state.dateRange[0]
          state.searchForm.endDate = state.dateRange[1]
        } else {
          state.searchForm.startDate = null
          state.searchForm.endDate = null
        }
        
        const response = await NotificationService.searchNotifications(
          state.searchForm,
          state.currentPage - 1,
          state.pageSize,
          'createdAt',
          'DESC'
        )
        
        if (response.success) {
          state.notificationList = response.data.content || []
          state.totalElements = response.data.totalElements || 0
        } else {
          ElMessage.error('获取通知失败: ' + response.message)
        }
      } catch (error) {
        ElMessage.error('获取通知异常: ' + error.message)
      } finally {
        state.loading = false
      }
    }
    
    const handleSearch = () => {
      state.currentPage = 1
      loadNotifications()
    }
    
    const resetSearch = () => {
      state.searchForm = {
        userId: '',
        type: '',
        level: '',
        source: '',
        read: null,
        needConfirm: null,
        confirmed: null,
        startDate: null,
        endDate: null
      }
      state.dateRange = []
      state.currentPage = 1
      loadNotifications()
    }
    
    const handleSelectionChange = (selection) => {
      state.selectedNotifications = selection
    }
    
    const handleSizeChange = (val) => {
      state.pageSize = val
      loadNotifications()
    }
    
    const handleCurrentChange = (val) => {
      state.currentPage = val
      loadNotifications()
    }
    
    const viewNotification = (notification) => {
      state.currentNotification = notification
      state.detailDialogVisible = true
    }
    
    const deleteNotification = async (id) => {
      try {
        await ElMessageBox.confirm('确定要删除该通知吗？此操作不可恢复！', '删除确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        // 确保ID有效
        if (!id) {
          ElMessage.warning('无效的通知ID')
          return
        }
        
        const response = await NotificationService.deleteNotification(id)
        if (response.success) {
          ElMessage.success('删除成功')
          loadNotifications()
        } else {
          ElMessage.error('删除失败: ' + response.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('操作异常: ' + error.message)
        }
      }
    }
    
    const batchDelete = async () => {
      if (state.selectedNotifications.length === 0) {
        ElMessage.warning('请至少选择一条通知')
        return
      }
      
      try {
        await ElMessageBox.confirm(
          `确定要删除选中的${state.selectedNotifications.length}条通知吗？此操作不可恢复！`, 
          '批量删除确认', 
          {
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            type: 'warning'
          }
        )
        
        const ids = state.selectedNotifications.map(item => item.id).filter(id => id)
        if (ids.length === 0) {
          ElMessage.warning('无有效通知ID可删除')
          return
        }
        
        const response = await NotificationService.batchDeleteNotifications(ids)
        
        if (response.success) {
          ElMessage.success(`成功删除${response.data}条通知`)
          loadNotifications()
        } else {
          ElMessage.error('批量删除失败: ' + response.message)
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error('操作异常: ' + error.message)
        }
      }
    }
    
    const showCreateSystemDialog = () => {
      state.systemForm = {
        title: '',
        content: '',
        type: 'INFO',
        level: 'MEDIUM',
        source: 'SYSTEM'
      }
      state.systemDialogVisible = true
    }
    
    const showCreateUserDialog = () => {
      state.userForm = {
        userId: '',
        username: '',
        title: '',
        content: '',
        type: 'INFO',
        level: 'MEDIUM',
        source: 'SYSTEM',
        needConfirm: false
      }
      state.userDialogVisible = true
    }
    
    const createSystemNotification = async () => {
      try {
        await systemFormRef.value.validate()
        
        const response = await NotificationService.createSystemNotification(state.systemForm)
        if (response.success) {
          ElMessage.success('系统通知创建成功')
          state.systemDialogVisible = false
          loadNotifications()
        } else {
          ElMessage.error('创建失败: ' + response.message)
        }
      } catch (error) {
        // 表单验证错误会在界面展示，不需要额外处理
        if (error.message) {
          ElMessage.error('创建异常: ' + error.message)
        }
      }
    }
    
    const createUserNotification = async () => {
      try {
        await userFormRef.value.validate()
        
        const response = await NotificationService.createUserNotification(state.userForm)
        if (response.success) {
          ElMessage.success('用户通知创建成功')
          state.userDialogVisible = false
          loadNotifications()
        } else {
          ElMessage.error('创建失败: ' + response.message)
        }
      } catch (error) {
        // 表单验证错误会在界面展示，不需要额外处理
        if (error.message) {
          ElMessage.error('创建异常: ' + error.message)
        }
      }
    }
    
    const getTagType = (type) => {
      switch (type) {
        case 'INFO': return ''
        case 'WARNING': return 'warning'
        case 'ERROR': return 'danger'
        case 'SUCCESS': return 'success'
        default: return ''
      }
    }
    
    const getLevelType = (level) => {
      switch (level) {
        case 'LOW': return 'info'
        case 'MEDIUM': return 'warning'
        case 'HIGH': return 'danger'
        case 'URGENT': return 'danger'
        default: return 'info'
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
    
    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      return dayjs(dateStr).format('YYYY-MM-DD HH:mm:ss')
    }
    
    onMounted(() => {
      loadNotifications()
    })
    
    return {
      ...toRefs(state),
      systemFormRef,
      userFormRef,
      notificationRules,
      userNotificationRules,
      loadNotifications,
      handleSearch,
      resetSearch,
      handleSelectionChange,
      handleSizeChange,
      handleCurrentChange,
      viewNotification,
      deleteNotification,
      batchDelete,
      showCreateSystemDialog,
      showCreateUserDialog,
      createSystemNotification,
      createUserNotification,
      getTagType,
      getLevelType,
      getTypeText,
      getLevelText,
      formatDate
    }
  }
}
</script>

<style scoped>
.notification-manage {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.search-form {
  margin: 15px 0;
}

.table-container {
  margin-top: 20px;
}

.batch-actions {
  margin-top: 15px;
  text-align: left;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.notification-detail {
  padding: 0 15px;
}

.notification-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.detail-item {
  margin-bottom: 10px;
  display: flex;
  align-items: flex-start;
}

.label {
  font-weight: bold;
  min-width: 80px;
  margin-right: 10px;
}

.content {
  white-space: pre-wrap;
  font-family: inherit;
  background: #f8f8f8;
  padding: 10px;
  border-radius: 4px;
  margin: 0;
  max-height: 200px;
  overflow-y: auto;
}

.ml-5 {
  margin-left: 5px;
}
</style> 