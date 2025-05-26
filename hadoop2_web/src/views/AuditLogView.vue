<template>
  <div class="audit-log-container">
    <h1>系统操作审计日志</h1>
    
    <!-- 搜索过滤器 -->
    <div class="filter-box">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="用户名">
          <el-input v-model="filters.username" placeholder="输入用户名搜索"></el-input>
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="filters.operationType" placeholder="选择操作类型" clearable>
            <el-option v-for="item in operationTypes" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="资源类型">
          <el-select v-model="filters.resourceType" placeholder="选择资源类型" clearable>
            <el-option v-for="item in resourceTypes" :key="item.value" :label="item.label" :value="item.value"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="YYYY-MM-DD HH:mm:ss"
            value-format="YYYY-MM-DDTHH:mm:ss"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchLogs">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 日志表格 -->
    <el-table
      v-loading="loading"
      :data="auditLogs"
      border
      style="width: 100%"
      @sort-change="handleSortChange"
    >
      <el-table-column prop="id" label="ID" width="80" sortable></el-table-column>
      <el-table-column prop="username" label="用户名" width="120"></el-table-column>
      <el-table-column prop="operation" label="操作" width="160"></el-table-column>
      <el-table-column prop="operationType" label="操作类型" width="100">
        <template #default="scope">
          <el-tag :type="getTagType(scope.row.operationType)">{{ scope.row.operationType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="resourceType" label="资源类型" width="120"></el-table-column>
      <el-table-column prop="resourceId" label="资源ID" width="100"></el-table-column>
      <el-table-column prop="clientIp" label="客户端IP" width="140"></el-table-column>
      <el-table-column prop="createdAt" label="时间" width="180" sortable>
        <template #default="scope">
          {{ formatDateTime(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="详情" min-width="200">
        <template #default="scope">
          <div class="details-cell">{{ scope.row.details }}</div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="viewDetails(scope.row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
    
    <!-- 分页器 -->
    <div class="pagination-container">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="pagination.currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pagination.pageSize"
        layout="total, sizes, prev, pager, next, jumper"
        :total="pagination.total"
      ></el-pagination>
      
      <el-button type="success" icon="el-icon-download" @click="exportLogs">导出日志</el-button>
    </div>
    
    <!-- 详情对话框 -->
    <el-dialog title="日志详情" v-model="detailsVisible" width="650px">
      <div v-if="selectedLog" class="log-details">
        <div class="log-detail-item">
          <span class="label">ID:</span>
          <span>{{ selectedLog.id }}</span>
        </div>
        <div class="log-detail-item">
          <span class="label">用户名:</span>
          <span>{{ selectedLog.username }}</span>
        </div>
        <div class="log-detail-item">
          <span class="label">操作:</span>
          <span>{{ selectedLog.operation }}</span>
        </div>
        <div class="log-detail-item">
          <span class="label">操作类型:</span>
          <span>
            <el-tag :type="getTagType(selectedLog.operationType)">{{ selectedLog.operationType }}</el-tag>
          </span>
        </div>
        <div class="log-detail-item">
          <span class="label">资源类型:</span>
          <span>{{ selectedLog.resourceType }}</span>
        </div>
        <div class="log-detail-item">
          <span class="label">资源ID:</span>
          <span>{{ selectedLog.resourceId || '-' }}</span>
        </div>
        <div class="log-detail-item">
          <span class="label">客户端IP:</span>
          <span>{{ selectedLog.clientIp }}</span>
        </div>
        <div class="log-detail-item">
          <span class="label">时间:</span>
          <span>{{ formatDateTime(selectedLog.createdAt) }}</span>
        </div>
        <div class="log-detail-item full-width">
          <span class="label">详情:</span>
          <div class="detail-content">{{ selectedLog.details }}</div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted, computed } from 'vue'
import { format } from 'date-fns'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

export default defineComponent({
  name: 'AuditLogView',
  setup() {
    // 数据状态
    const loading = ref(false)
    const auditLogs = ref([])
    const selectedLog = ref(null)
    const detailsVisible = ref(false)
    const dateRange = ref([])

    // 分页数据
    const pagination = reactive({
      currentPage: 1,
      pageSize: 20,
      total: 0
    })

    // 排序数据
    const sorting = reactive({
      field: 'createdAt',
      order: 'desc'
    })

    // 筛选条件
    const filters = reactive({
      username: '',
      operationType: '',
      resourceType: ''
    })

    // 操作类型选项
    const operationTypes = [
      { value: 'CREATE', label: '创建' },
      { value: 'UPDATE', label: '更新' },
      { value: 'DELETE', label: '删除' },
      { value: 'QUERY', label: '查询' }
    ]

    // 资源类型选项
    const resourceTypes = [
      { value: 'USER', label: '用户' },
      { value: 'CONFIGURATION', label: '配置' },
      { value: 'FILE', label: '文件' },
      { value: 'HDFS', label: 'HDFS' },
      { value: 'HIVE', label: 'Hive' },
      { value: 'AUDIT_LOG', label: '审计日志' }
    ]

    // 获取审计日志数据
    const fetchAuditLogs = async () => {
      loading.value = true
      try {
        let url = '/api/audit-logs'
        let params = {
          page: pagination.currentPage - 1,
          size: pagination.pageSize,
          sortBy: sorting.field,
          sortDirection: sorting.order
        }

        if (filters.username || filters.operationType || filters.resourceType || dateRange.value?.length === 2) {
          url = '/api/audit-logs/search'
          
          if (filters.username) params.username = filters.username
          if (filters.operationType) params.operationType = filters.operationType
          if (filters.resourceType) params.resourceType = filters.resourceType

          if (dateRange.value?.length === 2) {
            params.startTime = dateRange.value[0]
            params.endTime = dateRange.value[1]
          }
        }

        const response = await axios.get(url, { params })
        auditLogs.value = response.data.content
        pagination.total = response.data.totalElements
        pagination.currentPage = response.data.currentPage + 1
      } catch (error) {
        ElMessage.error('获取审计日志失败: ' + error.message)
      } finally {
        loading.value = false
      }
    }

    // 搜索日志
    const searchLogs = () => {
      pagination.currentPage = 1
      fetchAuditLogs()
    }

    // 重置筛选条件
    const resetFilters = () => {
      filters.username = ''
      filters.operationType = ''
      filters.resourceType = ''
      dateRange.value = []
      searchLogs()
    }

    // 查看详情
    const viewDetails = (log) => {
      selectedLog.value = log
      detailsVisible.value = true
    }

    // 处理页码变化
    const handleCurrentChange = (page) => {
      pagination.currentPage = page
      fetchAuditLogs()
    }

    // 处理每页数量变化
    const handleSizeChange = (size) => {
      pagination.pageSize = size
      pagination.currentPage = 1
      fetchAuditLogs()
    }

    // 处理排序变化
    const handleSortChange = ({ prop, order }) => {
      if (prop) {
        sorting.field = prop
        sorting.order = order === 'ascending' ? 'asc' : 'desc'
      } else {
        sorting.field = 'createdAt'
        sorting.order = 'desc'
      }
      fetchAuditLogs()
    }

    // 导出日志
    const exportLogs = async () => {
      try {
        loading.value = true
        
        // 构建导出URL和参数
        // 注意：这里假设后端有一个导出API，实际实现可能需要调整
        let url = '/api/audit-logs/export'
        let params = { ...filters }

        if (dateRange.value?.length === 2) {
          params.startTime = dateRange.value[0]
          params.endTime = dateRange.value[1]
        }

        // 发送导出请求，获取二进制数据
        const response = await axios.get(url, { 
          params,
          responseType: 'blob'
        })
        
        // 创建Blob对象并下载
        const blob = new Blob([response.data], { type: 'application/vnd.ms-excel' })
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = `audit-logs-${format(new Date(), 'yyyy-MM-dd-HHmmss')}.xlsx`
        link.click()
        URL.revokeObjectURL(link.href)
        
        ElMessage.success('导出成功')
      } catch (error) {
        ElMessage.error('导出失败: ' + error.message)
      } finally {
        loading.value = false
      }
    }

    // 格式化日期时间
    const formatDateTime = (dateTimeStr) => {
      if (!dateTimeStr) return '-'
      try {
        return format(new Date(dateTimeStr), 'yyyy-MM-dd HH:mm:ss')
      } catch (e) {
        return dateTimeStr
      }
    }

    // 根据操作类型获取标签类型
    const getTagType = (type) => {
      switch (type) {
        case 'CREATE':
          return 'success'
        case 'UPDATE':
          return 'warning'
        case 'DELETE':
          return 'danger'
        case 'QUERY':
          return 'info'
        default:
          return ''
      }
    }

    // 组件挂载时获取数据
    onMounted(() => {
      fetchAuditLogs()
    })

    return {
      loading,
      auditLogs,
      pagination,
      filters,
      operationTypes,
      resourceTypes,
      dateRange,
      detailsVisible,
      selectedLog,
      fetchAuditLogs,
      searchLogs,
      resetFilters,
      viewDetails,
      handleCurrentChange,
      handleSizeChange,
      handleSortChange,
      exportLogs,
      formatDateTime,
      getTagType
    }
  }
})
</script>

<style scoped>
.audit-log-container {
  padding: 20px;
}

.filter-box {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
}

.details-cell {
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.log-details {
  display: flex;
  flex-wrap: wrap;
}

.log-detail-item {
  width: 50%;
  margin-bottom: 15px;
  display: flex;
}

.log-detail-item.full-width {
  width: 100%;
  flex-direction: column;
}

.log-detail-item .label {
  font-weight: bold;
  width: 100px;
}

.log-detail-item .detail-content {
  margin-top: 10px;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
  font-family: monospace;
  white-space: pre-wrap;
}
</style> 