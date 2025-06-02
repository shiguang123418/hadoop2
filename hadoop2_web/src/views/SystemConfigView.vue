<template>
  <div class="system-config-container">
    <h1>系统配置管理</h1>
    
    <!-- 搜索过滤器 -->
    <div class="filter-box">
      <el-form :inline="true" :model="filters" class="filter-form">
        <el-form-item label="配置键">
          <el-input v-model="filters.configKey" placeholder="输入配置键"></el-input>
        </el-form-item>
        <el-form-item label="配置分组">
          <el-select v-model="filters.configGroup" placeholder="选择配置分组" clearable>
            <el-option v-for="group in configGroups" :key="group" :label="group" :value="group"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="值类型">
          <el-select v-model="filters.valueType" placeholder="选择值类型" clearable>
            <el-option v-for="type in valueTypes" :key="type" :label="type" :value="type"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="系统级">
          <el-select v-model="filters.system" placeholder="系统级配置" clearable>
            <el-option label="是" :value="true"></el-option>
            <el-option label="否" :value="false"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchConfigs">搜索</el-button>
          <el-button @click="resetFilters">重置</el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 操作按钮 -->
    <div class="action-bar">
      <el-button type="primary" @click="openCreateDialog">新增配置</el-button>
      <el-button type="success" @click="refreshCache">刷新缓存</el-button>
      <el-button type="info" @click="fetchGroups">刷新分组</el-button>
    </div>
    
    <!-- 配置表格 -->
    <el-table
      v-loading="loading"
      :data="configs"
      border
      style="width: 100%; margin-top: 20px"
      @sort-change="handleSortChange"
    >
      <el-table-column prop="id" label="ID" width="80" sortable></el-table-column>
      <el-table-column prop="configKey" label="配置键" width="200" sortable></el-table-column>
      <el-table-column prop="configValue" label="配置值" width="180">
        <template #default="scope">
          <div class="value-cell">{{ formatConfigValue(scope.row) }}</div>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" min-width="200"></el-table-column>
      <el-table-column prop="configGroup" label="分组" width="120" sortable></el-table-column>
      <el-table-column prop="valueType" label="值类型" width="100" sortable></el-table-column>
      <el-table-column prop="system" label="系统级" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.system ? 'warning' : 'info'">{{ scope.row.system ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="visible" label="可见" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.visible ? 'success' : 'info'">{{ scope.row.visible ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" width="180" sortable>
        <template #default="scope">
          {{ formatDateTime(scope.row.updatedAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="scope">
          <el-button size="small" @click="openEditValueDialog(scope.row)">修改值</el-button>
          <el-button size="small" type="primary" @click="openEditDialog(scope.row)" :disabled="scope.row.system">编辑</el-button>
          <el-button size="small" type="danger" @click="confirmDelete(scope.row)" :disabled="scope.row.system">删除</el-button>
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
    </div>
    
    <!-- 创建/编辑配置对话框 -->
    <el-dialog :title="dialogIsCreate ? '新增配置' : '编辑配置'" v-model="dialogVisible" width="600px">
      <el-form :model="formData" :rules="formRules" ref="configForm" label-width="100px">
        <el-form-item label="配置键" prop="configKey" :disabled="!dialogIsCreate && formData.system">
          <el-input v-model="formData.configKey" :disabled="!dialogIsCreate && formData.system"></el-input>
        </el-form-item>
        <el-form-item label="配置值" prop="configValue">
          <el-input v-model="formData.configValue" type="textarea" :rows="3" v-if="formData.valueType === 'TEXT'"></el-input>
          <el-input v-model="formData.configValue" v-else></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input v-model="formData.description" type="textarea" :rows="2"></el-input>
        </el-form-item>
        <el-form-item label="分组" prop="configGroup" :disabled="!dialogIsCreate && formData.system">
          <el-select v-model="formData.configGroup" filterable allow-create default-first-option :disabled="!dialogIsCreate && formData.system">
            <el-option v-for="group in configGroups" :key="group" :label="group" :value="group"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="值类型" prop="valueType" :disabled="!dialogIsCreate && formData.system">
          <el-select v-model="formData.valueType" :disabled="!dialogIsCreate && formData.system">
            <el-option v-for="type in valueTypes" :key="type" :label="type" :value="type"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="是否系统级" prop="system" v-if="dialogIsCreate">
          <el-switch v-model="formData.system"></el-switch>
        </el-form-item>
        <el-form-item label="是否可见" prop="visible" :disabled="!dialogIsCreate && formData.system">
          <el-switch v-model="formData.visible" :disabled="!dialogIsCreate && formData.system"></el-switch>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999"></el-input-number>
        </el-form-item>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>
    
    <!-- 修改配置值对话框 -->
    <el-dialog title="修改配置值" v-model="valueDialogVisible" width="500px">
      <el-form :model="valueForm" ref="valueConfigForm">
        <el-form-item :label="valueForm.configKey" prop="configValue">
          <el-input v-model="valueForm.configValue" v-if="valueForm.valueType !== 'TEXT'"></el-input>
          <el-input v-model="valueForm.configValue" type="textarea" :rows="5" v-else></el-input>
        </el-form-item>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="valueDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="updateConfigValue" :loading="submitLoading">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { defineComponent, ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { format } from 'date-fns'

export default defineComponent({
  name: 'SystemConfigView',
  setup() {
    // 数据状态
    const loading = ref(false)
    const submitLoading = ref(false)
    const configs = ref([])
    
    // 分页数据
    const pagination = reactive({
      currentPage: 1,
      pageSize: 10,
      total: 0
    })
    
    // 排序数据
    const sorting = reactive({
      property: 'id',
      direction: 'asc'
    })
    
    // 筛选条件
    const filters = reactive({
      configKey: '',
      configValue: '',
      description: '',
      configGroup: '',
      valueType: '',
      system: null
    })
    
    // 配置分组列表
    const configGroups = ref([])
    
    // 值类型列表
    const valueTypes = ['STRING', 'NUMBER', 'BOOLEAN', 'JSON', 'TEXT']
    
    // 对话框状态
    const dialogVisible = ref(false)
    const dialogIsCreate = ref(true)
    const valueDialogVisible = ref(false)
    
    // 表单数据
    const formData = reactive({
      id: null,
      configKey: '',
      configValue: '',
      description: '',
      configGroup: '',
      valueType: 'STRING',
      system: false,
      visible: true,
      sortOrder: 0
    })
    
    // 值修改表单
    const valueForm = reactive({
      id: null,
      configKey: '',
      configValue: '',
      valueType: 'STRING'
    })
    
    // 表单校验规则
    const formRules = {
      configKey: [
        { required: true, message: '请输入配置键', trigger: 'blur' },
        { pattern: /^[a-zA-Z0-9._]+$/, message: '配置键只能包含字母、数字、点和下划线', trigger: 'blur' }
      ],
      configValue: [
        { required: true, message: '请输入配置值', trigger: 'blur' }
      ],
      configGroup: [
        { required: true, message: '请选择或输入配置分组', trigger: 'change' }
      ],
      valueType: [
        { required: true, message: '请选择值类型', trigger: 'change' }
      ]
    }
    
    // 表单引用
    const configForm = ref(null)
    const valueConfigForm = ref(null)
    
    // 获取配置列表
    const fetchConfigs = async () => {
      loading.value = true
      try {
        const response = await axios.get('/system/configs/search', {
          params: {
            configKey: filters.configKey || null,
            configValue: filters.configValue || null,
            description: filters.description || null,
            configGroup: filters.configGroup || null,
            valueType: filters.valueType || null,
            system: filters.system,
            visible: null,
            page: pagination.currentPage - 1,
            size: pagination.pageSize,
            sortBy: sorting.property,
            sortDirection: sorting.direction
          }
        })
        
        if (response.data.code === 200) {
          configs.value = response.data.data.content
          pagination.total = response.data.data.totalElements
        } else {
          ElMessage.error(response.data.message || '获取系统配置列表失败')
        }
      } catch (error) {
        ElMessage.error('获取系统配置列表失败: ' + error.message)
      } finally {
        loading.value = false
      }
    }
    
    // 获取所有分组
    const fetchGroups = async () => {
      try {
        const response = await axios.get('/system/configs')
        
        if (response.data.code === 200) {
          const configs = response.data.data
          const groups = new Set()
          
          configs.forEach(config => {
            if (config.configGroup) {
              groups.add(config.configGroup)
            }
          })
          
          configGroups.value = Array.from(groups)
        }
      } catch (error) {
        ElMessage.error('获取配置分组失败: ' + error.message)
      }
    }
    
    // 搜索配置
    const searchConfigs = () => {
      pagination.currentPage = 1
      fetchConfigs()
    }
    
    // 重置筛选条件
    const resetFilters = () => {
      Object.keys(filters).forEach(key => {
        filters[key] = key === 'system' ? null : ''
      })
      searchConfigs()
    }
    
    // 处理页码变化
    const handleCurrentChange = (page) => {
      pagination.currentPage = page
      fetchConfigs()
    }
    
    // 处理每页数量变化
    const handleSizeChange = (size) => {
      pagination.pageSize = size
      pagination.currentPage = 1
      fetchConfigs()
    }
    
    // 处理排序变化
    const handleSortChange = ({ prop, order }) => {
      sorting.property = prop || 'id'
      sorting.direction = order === 'ascending' ? 'asc' : 'desc'
      fetchConfigs()
    }
    
    // 打开创建对话框
    const openCreateDialog = () => {
      dialogIsCreate.value = true
      Object.keys(formData).forEach(key => {
        formData[key] = key === 'visible' ? true : (key === 'sortOrder' ? 0 : (key === 'valueType' ? 'STRING' : ''))
      })
      dialogVisible.value = true
    }
    
    // 打开编辑对话框
    const openEditDialog = (row) => {
      dialogIsCreate.value = false
      Object.keys(formData).forEach(key => {
        formData[key] = row[key]
      })
      dialogVisible.value = true
    }
    
    // 打开修改值对话框
    const openEditValueDialog = (row) => {
      valueForm.id = row.id
      valueForm.configKey = row.configKey
      valueForm.configValue = row.configValue
      valueForm.valueType = row.valueType
      valueDialogVisible.value = true
    }
    
    // 提交表单
    const submitForm = async () => {
      configForm.value.validate(async valid => {
        if (valid) {
          submitLoading.value = true
          try {
            let response
            
            if (dialogIsCreate.value) {
              // 创建配置
              response = await axios.post('/system/configs', formData)
            } else {
              // 更新配置
              response = await axios.put(`/system/configs/${formData.id}`, formData)
            }
            
            if (response.data.code === 200) {
              ElMessage.success(dialogIsCreate.value ? '创建配置成功' : '更新配置成功')
              dialogVisible.value = false
              fetchConfigs()
              if (dialogIsCreate.value) {
                // 新增配置后刷新分组列表
                fetchGroups()
              }
            } else {
              ElMessage.error(response.data.message || (dialogIsCreate.value ? '创建配置失败' : '更新配置失败'))
            }
          } catch (error) {
            ElMessage.error((dialogIsCreate.value ? '创建' : '更新') + '配置失败: ' + error.message)
          } finally {
            submitLoading.value = false
          }
        }
      })
    }
    
    // 更新配置值
    const updateConfigValue = async () => {
      if (!valueForm.configValue && valueForm.configValue !== '0' && valueForm.configValue !== 'false') {
        ElMessage.warning('配置值不能为空')
        return
      }
      
      submitLoading.value = true
      try {
        const response = await axios.put(`/system/configs/${valueForm.id}/value`, {
          configValue: valueForm.configValue
        })
        
        if (response.data.code === 200) {
          ElMessage.success('更新配置值成功')
          valueDialogVisible.value = false
          fetchConfigs()
        } else {
          ElMessage.error(response.data.message || '更新配置值失败')
        }
      } catch (error) {
        ElMessage.error('更新配置值失败: ' + error.message)
      } finally {
        submitLoading.value = false
      }
    }
    
    // 确认删除
    const confirmDelete = (row) => {
      if (row.system) {
        ElMessage.warning('系统级配置不允许删除')
        return
      }
      
      ElMessageBox.confirm(`确定删除配置 "${row.configKey}" 吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const response = await axios.delete(`/system/configs/${row.id}`)
          
          if (response.data.code === 200) {
            ElMessage.success('删除配置成功')
            fetchConfigs()
          } else {
            ElMessage.error(response.data.message || '删除配置失败')
          }
        } catch (error) {
          ElMessage.error('删除配置失败: ' + error.message)
        }
      }).catch(() => {})
    }
    
    // 刷新缓存
    const refreshCache = async () => {
      try {
        const response = await axios.post('/system/configs/refresh-cache')
        
        if (response.data.code === 200) {
          ElMessage.success('刷新配置缓存成功')
        } else {
          ElMessage.error(response.data.message || '刷新配置缓存失败')
        }
      } catch (error) {
        ElMessage.error('刷新配置缓存失败: ' + error.message)
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
    
    // 格式化配置值显示
    const formatConfigValue = (row) => {
      if (row.valueType === 'TEXT' && row.configValue && row.configValue.length > 30) {
        return row.configValue.substring(0, 30) + '...'
      }
      return row.configValue
    }
    
    // 组件挂载时获取数据
    onMounted(() => {
      fetchConfigs()
      fetchGroups()
    })
    
    return {
      loading,
      submitLoading,
      configs,
      pagination,
      filters,
      configGroups,
      valueTypes,
      dialogVisible,
      dialogIsCreate,
      valueDialogVisible,
      formData,
      valueForm,
      formRules,
      configForm,
      valueConfigForm,
      fetchConfigs,
      fetchGroups,
      searchConfigs,
      resetFilters,
      handleCurrentChange,
      handleSizeChange,
      handleSortChange,
      openCreateDialog,
      openEditDialog,
      openEditValueDialog,
      submitForm,
      updateConfigValue,
      confirmDelete,
      refreshCache,
      formatDateTime,
      formatConfigValue
    }
  }
})
</script>

<style scoped>
.system-config-container {
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

.action-bar {
  margin-bottom: 20px;
  display: flex;
  gap: 10px;
}

.value-cell {
  max-height: 60px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: normal;
  word-break: break-all;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style> 