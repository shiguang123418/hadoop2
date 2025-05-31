<template>
  <div class="datasource-list">
    <div class="header">
      <h2>数据源管理</h2>
      <el-button type="primary" @click="handleAdd">添加数据源</el-button>
    </div>

    <el-table :data="dataSources" style="width: 100%" v-loading="loading">
      <el-table-column prop="name" label="名称" width="180" />
      <el-table-column prop="type" label="类型" width="120">
        <template #default="scope">
          <el-tag>{{ scope.row.type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180">
        <template #default="scope">
          {{ formatDate(scope.row.createTime) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="250">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="success" @click="handleTest(scope.row)">测试连接</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      :title="dialogTitle"
      v-model="dialogVisible"
      width="500px"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择数据源类型">
            <el-option label="传感器" value="SENSOR" />
            <el-option label="气象站" value="WEATHER_STATION" />
            <el-option label="市场数据" value="MARKET_DATA" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input type="textarea" v-model="form.description" />
        </el-form-item>
        <el-form-item label="连接信息" prop="connectionInfo">
          <el-input type="textarea" v-model="form.connectionInfo" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="handleSubmit">确定</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

export default {
  name: 'DataSourceList',
  setup() {
    const dataSources = ref([])
    const loading = ref(false)
    const dialogVisible = ref(false)
    const dialogTitle = ref('')
    const formRef = ref(null)
    const form = ref({
      id: null,
      name: '',
      type: '',
      description: '',
      connectionInfo: ''
    })

    const rules = {
      name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
      type: [{ required: true, message: '请选择数据源类型', trigger: 'change' }]
    }

    // 获取数据源列表
    const fetchDataSources = async () => {
      loading.value = true
      try {
        const response = await axios.get('/datasources')
        dataSources.value = response.data
      } catch (error) {
        ElMessage.error('获取数据源列表失败')
      } finally {
        loading.value = false
      }
    }

    // 添加数据源
    const handleAdd = () => {
      dialogTitle.value = '添加数据源'
      form.value = {
        id: null,
        name: '',
        type: '',
        description: '',
        connectionInfo: ''
      }
      dialogVisible.value = true
    }

    // 编辑数据源
    const handleEdit = (row) => {
      dialogTitle.value = '编辑数据源'
      form.value = { ...row }
      dialogVisible.value = true
    }

    // 删除数据源
    const handleDelete = (row) => {
      ElMessageBox.confirm('确定要删除该数据源吗？', '提示', {
        type: 'warning'
      }).then(async () => {
        try {
          await axios.delete(`/api/datasources/${row.id}`)
          ElMessage.success('删除成功')
          fetchDataSources()
        } catch (error) {
          ElMessage.error('删除失败')
        }
      })
    }

    // 测试连接
    const handleTest = async (row) => {
      try {
        const response = await axios.post(`/api/datasources/${row.id}/test-connection`)
        if (response.data) {
          ElMessage.success('连接测试成功')
        } else {
          ElMessage.error('连接测试失败')
        }
      } catch (error) {
        ElMessage.error('连接测试失败')
      }
    }

    // 提交表单
    const handleSubmit = async () => {
      if (!formRef.value) return
      await formRef.value.validate(async (valid) => {
        if (valid) {
          try {
            if (form.value.id) {
              await axios.put(`/api/datasources/${form.value.id}`, form.value)
              ElMessage.success('更新成功')
            } else {
              await axios.post('/api/datasources', form.value)
              ElMessage.success('添加成功')
            }
            dialogVisible.value = false
            fetchDataSources()
          } catch (error) {
            ElMessage.error(form.value.id ? '更新失败' : '添加失败')
          }
        }
      })
    }

    // 格式化日期
    const formatDate = (date) => {
      return new Date(date).toLocaleString()
    }

    // 获取状态标签类型
    const getStatusType = (status) => {
      const types = {
        'ACTIVE': 'success',
        'INACTIVE': 'info',
        'ERROR': 'danger'
      }
      return types[status] || 'info'
    }

    onMounted(() => {
      fetchDataSources()
    })

    return {
      dataSources,
      loading,
      dialogVisible,
      dialogTitle,
      form,
      formRef,
      rules,
      handleAdd,
      handleEdit,
      handleDelete,
      handleTest,
      handleSubmit,
      formatDate,
      getStatusType
    }
  }
}
</script>

<style scoped>
.datasource-list {
  padding: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style> 