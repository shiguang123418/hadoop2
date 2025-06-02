<template>
  <div class="hive-analytics">
    <div class="page-header">
      <h2 class="page-title">Hive 数据分析中心</h2>
      <p class="page-description">强大的数据分析和可视化工具，深入挖掘农业数据价值</p>
    </div>
    
    <el-card class="connection-card" shadow="hover">
      <template #header>
        <div class="connection-header">
          <span>数据库连接</span>
          <el-button type="primary" size="small" @click="testConnection">
            <el-icon><Connection /></el-icon>
            <span>测试连接</span>
          </el-button>
        </div>
      </template>
      <div class="connection-status">
        <el-tag :type="isConnected ? 'success' : 'danger'" effect="dark">
          {{ isConnected ? '已连接' : '未连接' }}
        </el-tag>
        <span v-if="isConnected" class="connection-info">当前连接：{{ hiveUrl }}</span>
      </div>
    </el-card>
    
    <el-row :gutter="20" class="filter-row">
      <el-col :span="10">
        <el-select 
          v-model="selectedDatabase" 
          placeholder="选择数据库" 
          class="full-width"
          @change="handleDatabaseChange"
          :loading="loadingDatabases">
          <el-option v-for="db in databases" :key="db" :label="db" :value="db" />
        </el-select>
      </el-col>
      <el-col :span="10">
        <el-select 
          v-model="selectedTable" 
          placeholder="选择表" 
          class="full-width"
          @change="handleTableChange"
          :loading="loadingTables"
          :disabled="!selectedDatabase">
          <el-option v-for="table in tables" :key="table" :label="table" :value="table" />
        </el-select>
      </el-col>
      <el-col :span="4">
        <el-button type="primary" class="full-width" @click="refreshData">刷新</el-button>
      </el-col>
    </el-row>
    
    <el-tabs v-model="activeTabName" class="main-tabs">
      <el-tab-pane label="分析配置" name="config">
        <!-- 分析配置面板 -->
        <el-card class="analysis-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>分析类型</span>
            </div>
          </template>
          
          <el-radio-group v-model="analysisType" @change="handleAnalysisTypeChange">
            <el-radio label="distribution">列值分布</el-radio>
            <el-radio label="time_series">时间序列</el-radio>
            <el-radio label="correlation">相关性分析</el-radio>
            <el-radio label="clustering">聚类分析</el-radio>
            <el-radio label="regression">回归分析</el-radio>
          </el-radio-group>
          
          <div v-if="analysisType" class="analysis-config-form">
            <el-form :model="analysisConfig" label-width="120px">
              <!-- 公共配置 -->
              <el-form-item label="图表类型">
                <el-radio-group v-model="analysisConfig.chartType">
                  <el-radio label="bar">柱状图</el-radio>
                  <el-radio label="line">折线图</el-radio>
                  <el-radio label="pie">饼图</el-radio>
                  <el-radio label="scatter">散点图</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <!-- 特定配置 -->
              <div v-if="analysisType === 'time_series'">
                <el-form-item label="时间字段">
                  <el-select v-model="analysisConfig.timeField" placeholder="选择时间字段" class="field-select">
                    <el-option 
                      v-for="field in dateTimeFields" 
                      :key="field.name || field.col_name" 
                      :label="field.name || field.col_name" 
                      :value="field.name || field.col_name" />
                  </el-select>
                </el-form-item>

                <el-form-item label="值字段">
                  <el-select v-model="analysisConfig.valueFields" placeholder="选择值字段" class="field-select" multiple>
                    <el-option 
                      v-for="field in valueFields" 
                      :key="field.name || field.col_name" 
                      :label="field.name || field.col_name" 
                      :value="field.name || field.col_name" />
                  </el-select>
                  <div class="form-help-text">可以选择多个值字段进行比较分析</div>
                </el-form-item>
              </div>
              
              <el-form-item label="数据字段">
                <el-transfer 
                  v-model="analysisConfig.fields" 
                  :data="tableFields.map(field => ({
                    key: field.name || field.col_name,
                    label: `${field.name || field.col_name} (${field.type || field.col_type})`
                  }))"
                  :titles="['可选字段', '已选字段']"
                  filterable />
              </el-form-item>
              
              <el-form-item>
                <el-button type="primary" @click="runAnalysis" :loading="loading">
                  <el-icon><DataAnalysis /></el-icon>
                  <span>运行分析</span>
                </el-button>
                <el-button @click="resetAnalysisForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </el-card>
      </el-tab-pane>
      
      <el-tab-pane label="任务状态" name="tasks">
        <!-- 任务状态面板 -->
        <el-card class="task-card" shadow="hover">
          <template #header>
            <div class="card-header">
              <span>分析任务列表</span>
              <div class="task-header-actions">
                <el-switch
                  v-model="autoRefresh"
                  active-text="自动刷新"
                  inactive-text="手动刷新"
                  @change="toggleAutoRefresh"
                />
                <el-button type="primary" size="small" @click="refreshTasks">
                  <el-icon><Refresh /></el-icon>
                  <span>刷新</span>
                </el-button>
              </div>
            </div>
          </template>
          
          <div v-if="tasks.length === 0" class="empty-tasks">
            <el-empty description="暂无分析任务" />
          </div>
          
          <el-table v-else :data="tasks" style="width: 100%" v-loading="loading" border stripe>
            <el-table-column label="任务ID" width="80">
              <template #default="scope">
                <el-tooltip :content="scope.row.taskId" placement="top">
                  <span>{{ scope.row.taskId.substring(0, 8) }}...</span>
                </el-tooltip>
              </template>
            </el-table-column>
            
            <el-table-column label="分析类型" prop="analysisType" width="120">
              <template #default="scope">
                <span>{{ getAnalysisTypeName(scope.row.analysisType) }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="数据库" prop="database" width="120" />
            
            <el-table-column label="表" prop="table" width="120" />
            
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <el-tag :type="getStatusType(scope.row.status)" effect="dark">
                  {{ getStatusText(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column label="进度" width="120">
              <template #default="scope">
                <el-progress 
                  :percentage="scope.row.progress || 0" 
                  :status="getProgressStatus(scope.row.status)" />
              </template>
            </el-table-column>
            
            <el-table-column label="开始时间" width="170">
              <template #default="scope">
                <span>{{ formatTime(scope.row.startTime) }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="结束时间" width="170">
              <template #default="scope">
                <span>{{ formatTime(scope.row.endTime) }}</span>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" fixed="right" width="200">
              <template #default="scope">
                <el-button 
                  size="small" 
                  type="success" 
                  @click="viewResult(scope.row)"
                  :disabled="scope.row.status !== 'COMPLETED'">
                  查看结果
                </el-button>
                
                <el-button 
                  size="small" 
                  type="warning" 
                  @click="cancelTask(scope.row.taskId)"
                  :disabled="!['SUBMITTED', 'RUNNING'].includes(scope.row.status)">
                  取消
                </el-button>
                
                <el-button 
                  size="small" 
                  type="danger" 
                  @click="deleteTask(scope.row.taskId)">
                  删除
                </el-button>
              </template>
            </el-table-column>
            
            <!-- 添加行数据属性，用于高亮显示 -->
            <template #row="{ row }">
              <tr :data-task-id="row.taskId" :class="{'running-task': ['SUBMITTED', 'RUNNING'].includes(row.status)}"></tr>
            </template>
          </el-table>
          
          <div v-if="autoRefresh" class="auto-refresh-info">
            <el-tag size="small" type="info">
              自动刷新已开启，每 {{ refreshInterval / 1000 }} 秒刷新一次
              <span v-if="nextRefreshTime > 0">
                ({{ Math.ceil((nextRefreshTime - Date.now()) / 1000) }} 秒后刷新)
              </span>
            </el-tag>
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>
    
    <!-- 结果显示对话框 -->
    <el-dialog 
      v-model="showResult" 
      :title="selectedTask ? `${getAnalysisTypeName(selectedTask.analysisType)} 分析结果` : '分析结果'" 
      width="80%" 
      destroy-on-close
      top="5vh"
      :fullscreen="false"
      :modal="true"
      :show-close="true"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
    >
      <div class="chart-container" ref="chartRef"></div>
      
      <div v-if="selectedTask && selectedTask.result && selectedTask.result.length > 0" class="result-info">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="分析类型">
            {{ getAnalysisTypeName(selectedTask.analysisType) }}
          </el-descriptions-item>
          <el-descriptions-item label="分析数据库">
            {{ selectedTask.database }}
          </el-descriptions-item>
          <el-descriptions-item label="分析表">
            {{ selectedTask.table }}
          </el-descriptions-item>
          <el-descriptions-item label="完成时间">
            {{ formatTime(selectedTask.endTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="结果数量">
            {{ selectedTask.result.length }} 条
          </el-descriptions-item>
          <el-descriptions-item label="任务ID">
            {{ selectedTask.taskId }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <div class="result-actions">
        <el-button type="primary" @click="exportResult">
          <el-icon><Download /></el-icon>
          <span>导出图表</span>
        </el-button>
        
        <el-button type="success" @click="saveResult">
          <el-icon><Check /></el-icon>
          <span>保存结果</span>
        </el-button>
        
        <el-button @click="showResult = false">
          关闭
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick, watch, h, computed } from 'vue'
import { DataAnalysis, Connection, Download, Check, Refresh } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import * as echarts from 'echarts'

export default {
  name: 'HiveAnalyticsView',
  components: {
    DataAnalysis,
    Connection,
    Download,
    Check,
    Refresh
  },
  setup() {
    const isConnected = ref(false)
    const hiveUrl = ref('')
    const selectedDatabase = ref('')
    const selectedTable = ref('')
    const analysisType = ref('')
    const loading = ref(false)
    const showResult = ref(false)
    const chartRef = ref(null)
    const chartInstance = ref(null)
    const loadingDatabases = ref(false)
    const loadingTables = ref(false)
    const loadingFields = ref(false)
    const tableData = ref([])
    
    const analysisConfig = reactive({
      fields: [],
      timeField: '',
      chartType: 'bar',
      valueFields: []
    })
    
    const databases = ref([])
    const tables = ref([])
    const tableFields = ref([])
    
    // 任务列表
    const tasks = ref([])
    
    const selectedTask = ref(null)
    const activeTabName = ref('config')
    
    const autoRefresh = ref(false)
    const refreshInterval = ref(30000)
    const nextRefreshTime = ref(0)
    
    // 重置分析表单
    const resetAnalysisForm = () => {
      analysisConfig.fields = []
      analysisConfig.timeField = ''
      analysisConfig.chartType = 'bar'
      analysisConfig.valueFields = []
    }
    
    // 分析类型变更处理
    const handleAnalysisTypeChange = () => {
      resetAnalysisForm()
    }
    
    // 获取分析类型名称
    const getAnalysisTypeName = (type) => {
      const types = {
        'distribution': '列值分布',
        'time_series': '时间序列',
        'correlation': '相关性分析',
        'clustering': '聚类分析',
        'regression': '回归分析'
      }
      return types[type] || type
    }
    
    // 获取进度状态
    const getProgressStatus = (status) => {
      if (status === 'COMPLETED') return 'success'
      if (status === 'FAILED') return 'exception'
      if (status === 'CANCELLED') return 'warning'
      return ''
    }
    
    // 测试连接
    const testConnection = async () => {
      loading.value = true
      try {
        const response = await axios.get('/hive/status')
        if (response.data.code === 200) {
          const status = response.data.data
          isConnected.value = status.connected
          hiveUrl.value = status.url
          
          if (status.connected) {
            ElMessage.success('Hive连接成功')
            fetchDatabases() // 连接成功后加载数据库列表
            fetchTasks()
          } else {
            ElMessage.error(`连接失败: ${status.error || '未知错误'}`)
          }
        } else {
          ElMessage.error('获取Hive状态失败')
          isConnected.value = false
        }
      } catch (error) {
        ElMessage.error(`连接异常: ${error.message}`)
        isConnected.value = false
      } finally {
        loading.value = false
      }
    }
    
    // 获取数据库列表
    const fetchDatabases = async () => {
      loadingDatabases.value = true
      try {
        const response = await axios.get('/hive/databases')
        if (response.data.code === 200) {
          databases.value = response.data.data || []
        } else {
          ElMessage.error('获取数据库列表失败')
        }
      } catch (error) {
        ElMessage.error(`获取数据库列表异常: ${error.message}`)
      } finally {
        loadingDatabases.value = false
      }
    }
    
    // 获取表列表
    const fetchTables = async (database) => {
      loadingTables.value = true
      try {
        const response = await axios.get('/hive/tables', {
          params: { database }
        })
        if (response.data.code === 200) {
          tables.value = response.data.data || []
        } else {
          ElMessage.error('获取表列表失败')
        }
      } catch (error) {
        ElMessage.error(`获取表列表异常: ${error.message}`)
      } finally {
        loadingTables.value = false
      }
    }
    
    // 获取表结构
    const fetchTableSchema = async (database, table) => {
      loadingFields.value = true
      try {
        const response = await axios.get('/hive/schema', {
          params: { 
            database,
            table 
          }
        })
        if (response.data.code === 200) {
          tableFields.value = response.data.data || []
          // 清空之前选择的字段
          analysisConfig.fields = []
          analysisConfig.timeField = ''
          analysisConfig.valueFields = []
        } else {
          ElMessage.error('获取表结构失败')
        }
      } catch (error) {
        ElMessage.error(`获取表结构异常: ${error.message}`)
      } finally {
        loadingFields.value = false
      }
    }
    
    // 获取表数据预览
    const fetchTableData = async (database, table) => {
      loading.value = true
      try {
        const sql = `SELECT * FROM ${database}.${table} LIMIT 100`
        const response = await axios.post('/hive/query', { sql })
        if (response.data.code === 200) {
          tableData.value = response.data.data || []
        } else {
          ElMessage.error('获取表数据失败')
        }
      } catch (error) {
        ElMessage.error(`获取表数据异常: ${error.message}`)
      } finally {
        loading.value = false
      }
    }
    
    // 处理数据库变化
    const handleDatabaseChange = (value) => {
      if (value) {
        selectedTable.value = '' // 重置表选择
        fetchTables(value) // 获取新数据库的表列表
      }
    }
    
    // 处理表变化
    const handleTableChange = (value) => {
      if (value && selectedDatabase.value) {
        fetchTableSchema(selectedDatabase.value, value)
        fetchTableData(selectedDatabase.value, value)
      }
    }
    
    // 刷新数据
    const refreshData = () => {
      if (isConnected.value) {
        fetchDatabases()
        if (selectedDatabase.value) {
          fetchTables(selectedDatabase.value)
          if (selectedTable.value) {
            fetchTableSchema(selectedDatabase.value, selectedTable.value)
            fetchTableData(selectedDatabase.value, selectedTable.value)
          }
        }
      } else {
        testConnection()
      }
    }
    
    // 计算日期时间字段
    const dateTimeFields = computed(() => {
      return tableFields.value.filter(field => {
        const type = field.type ? field.type.toLowerCase() : '';
        const name = (field.name || field.col_name || '').toLowerCase();
        // 根据类型判断
        const isTimeType = type.includes('date') || type.includes('time') || type.includes('timestamp');
        // 根据字段名判断
        const isTimeName = name === 'month' || name === 'year' || name === 'day' || 
                           name === 'quarter' || name.includes('date') || 
                           name.includes('time') || name.includes('day');
        
        return isTimeType || isTimeName;
      });
    })
    
    // 计算值字段（数值类型）
    const valueFields = computed(() => {
      return tableFields.value.filter(field => {
        const type = field.type ? field.type.toLowerCase() : '';
        const name = (field.name || field.col_name || '').toLowerCase();
        
        // 根据类型判断是否为数值类型
        const isNumericType = type.includes('int') || type.includes('double') || 
                             type.includes('float') || type.includes('decimal') || 
                             type.includes('number');
        
        // 根据字段名判断
        const isNumericName = name.includes('temp') || name.includes('rainfall') || 
                             name.includes('price') || name.includes('count') || 
                             name.includes('amount') || name.includes('value') ||
                             name.includes('rate') || name.includes('score');
        
        // 排除当前选择的时间字段
        const isNotTimeField = analysisConfig.timeField !== (field.name || field.col_name);
        
        return (isNumericType || isNumericName) && isNotTimeField;
      });
    })
    
    // 运行分析
    const runAnalysis = async () => {
      if (!selectedDatabase.value || !selectedTable.value || !analysisType.value) {
        ElMessage.warning('请选择数据库、表和分析类型')
        return
      }
      
      // 检查是否有选择字段
      if (!analysisConfig.fields || analysisConfig.fields.length === 0) {
        ElMessage.warning('请至少选择一个分析字段')
        return
      }

      // 先切换到任务状态标签页，让用户知道任务将在后台运行
      activeTabName.value = 'tasks'
      
      loading.value = true
      try {
        // 构建基本请求参数
        const request = {
          analysisType: analysisType.value,
          database: selectedDatabase.value,
          table: selectedTable.value,
          fields: analysisConfig.fields,
          chartType: analysisConfig.chartType
        }
        
        // 根据分析类型添加特定参数
        switch (analysisType.value) {
          case 'distribution':
            // 分布分析使用第一个字段
            request.field = analysisConfig.fields[0]
            break
          case 'time_series':
            // 时间序列分析需要时间字段和值字段
            if (!analysisConfig.timeField) {
              ElMessage.warning('时间序列分析请选择时间字段')
              // 保持在任务状态页
              loading.value = false
              return
            }
            
            // 检查值字段是否已选择
            if (!analysisConfig.valueFields || analysisConfig.valueFields.length === 0) {
              // 尝试从已选字段中选择一个非时间字段作为值字段
              const valueFields = analysisConfig.fields.filter(field => field !== analysisConfig.timeField)
              
              if (valueFields.length > 0) {
                // 自动选择值字段并通知用户
                analysisConfig.valueFields = valueFields
                ElMessage.info(`已自动选择 ${valueFields.join(', ')} 作为值字段`)
              } else {
                // 如果找不到合适的值字段，提示用户
                ElMessage.warning('时间序列分析需要选择一个不同于时间字段的值字段')
                loading.value = false
                return
              }
            }
            
            // 确保时间字段和值字段不同
            if (analysisConfig.timeField === analysisConfig.valueFields[0]) {
              ElMessage.warning('时间字段和值字段不能相同')
              loading.value = false
              return
            }
            
            request.timeField = analysisConfig.timeField
            request.valueFields = analysisConfig.valueFields
            
            // 打印请求参数，用于调试
            console.log('时间序列分析请求参数:', {
              timeField: request.timeField,
              valueFields: request.valueFields,
              fields: request.fields
            })
            break
          case 'correlation':
            // 相关性分析需要至少两个字段
            if (analysisConfig.fields.length < 2) {
              ElMessage.warning('相关性分析需要至少选择两个字段')
              // 保持在任务状态页
              loading.value = false
              return
            }
            break
        }

        ElMessage({
          message: '正在提交分析任务，请在任务列表中查看进度...',
          type: 'info',
          duration: 5000
        })

        const response = await axios.post('/hive/analytics/submit', request)
        if (response.data.code === 200) {
          const data = response.data.data
          
          // 任务提交成功后显示通知
          ElMessage({
            message: `分析任务已提交成功，任务ID: ${data.taskId.substring(0, 8)}...`,
            type: 'success',
            duration: 0,
            showClose: true
          })
          
          // 刷新任务列表
          await fetchTasks()
          
          // 定位到新提交的任务
          highlightNewTask(data.taskId)
        } else {
          ElMessage.error('提交分析任务失败: ' + response.data.message)
          // 即使失败也保持在任务状态页
        }
      } catch (error) {
        ElMessage.error('提交分析任务异常: ' + error.message)
        // 即使出现异常也保持在任务状态页
      } finally {
        loading.value = false
      }
    }
    
    // 高亮显示新提交的任务
    const highlightNewTask = (taskId) => {
      // 延迟一下，确保DOM已更新
      setTimeout(() => {
        const taskRow = document.querySelector(`tr[data-task-id="${taskId}"]`)
        if (taskRow) {
          taskRow.classList.add('highlight-task')
          // 3秒后移除高亮
          setTimeout(() => {
            taskRow.classList.remove('highlight-task')
          }, 3000)
          
          // 滚动到任务行
          taskRow.scrollIntoView({ behavior: 'smooth', block: 'center' })
        }
      }, 500)
    }
    
    // 获取任务列表
    const fetchTasks = async () => {
      loading.value = true
      try {
        // 获取用户任务
        const response = await axios.get('/hive/analytics/user-tasks')
        if (response.data.code === 200) {
          // 按开始时间降序排序，使最新的任务显示在上方
          const taskData = response.data.data;
          tasks.value = taskData.sort((a, b) => {
            // 使用startTime进行排序，如果没有startTime则使用当前时间
            const timeA = a.startTime || Date.now();
            const timeB = b.startTime || Date.now();
            // 降序排序 (b - a)
            return timeB - timeA;
          });
          
          // 如果开启了自动刷新，更新下一次刷新时间
          if (autoRefresh.value) {
            nextRefreshTime.value = Date.now() + refreshInterval.value
          }
        } else {
          ElMessage.error('获取任务列表失败: ' + response.data.message)
        }
      } catch (error) {
        ElMessage.error('获取任务列表异常: ' + error.message)
      } finally {
        loading.value = false
      }
    }
    
    // 刷新任务列表
    const refreshTasks = () => {
      fetchTasks()
    }
    
    // 自动刷新定时器
    let autoRefreshTimer = null
    
    // 切换自动刷新状态
    const toggleAutoRefresh = (value) => {
      autoRefresh.value = value
      
      // 清除现有的定时器
      if (autoRefreshTimer) {
        clearInterval(autoRefreshTimer)
        autoRefreshTimer = null
      }
      
      // 如果开启了自动刷新，设置定时器
      if (autoRefresh.value) {
        nextRefreshTime.value = Date.now() + refreshInterval.value
        
        autoRefreshTimer = setInterval(() => {
          if (activeTabName.value === 'tasks') {
            fetchTasks()
          }
        }, refreshInterval.value)
      }
    }
    
    // 监听标签页切换
    watch(activeTabName, (newValue) => {
      // 切换到任务标签页时刷新任务列表
      if (newValue === 'tasks') {
        fetchTasks()
      }
    })
    
    // 监听时间字段变化，自动清空值字段，防止选择相同的字段
    watch(() => analysisConfig.timeField, (newValue) => {
      // 如果当前值字段与新的时间字段相同，则清空值字段
      if (newValue && analysisConfig.valueFields.some(field => field === newValue)) {
        analysisConfig.valueFields = []
        console.log('时间字段变化，清空相同的值字段')
      }
    })
    
    // 查看任务结果
    const viewResult = async (task) => {
      if (task.status !== 'COMPLETED') {
        ElMessage.warning('任务尚未完成，无法查看结果')
        return
      }
      
      selectedTask.value = task
      
      // 设置分析类型以匹配历史任务的类型
      analysisType.value = task.analysisType
      
      // 如果任务参数中包含图表类型，则设置为当前图表类型
      if (task.params && task.params.chartType) {
        analysisConfig.chartType = task.params.chartType
        console.log('从任务参数中恢复图表类型:', task.params.chartType)
      }
      
      try {
        // 确保任务结果数据存在
        if (!task.result || !Array.isArray(task.result) || task.result.length === 0) {
          ElMessage.warning('任务结果为空或格式不正确')
          console.error('任务结果数据为空或格式不正确:', task.result)
          return
        }
        
        // 检查时间序列数据格式
        if (task.analysisType === 'time_series') {
          // 检查是否至少有一条数据包含必要的字段
          // 修改：允许包含month和value字段，或者是月份和其他数值字段
          const hasValidData = task.result.some(item => 
            item && typeof item === 'object' && (
              // 标准格式：包含month和value
              (('month' in item) && ('value' in item)) || 
              // 多值字段格式：包含month和至少一个其他字段
              (('month' in item) && Object.keys(item).length > 1)
            )
          );
          
          if (!hasValidData) {
            console.error('时间序列数据格式不正确，缺少month或值字段');
            console.log('数据示例:', task.result.slice(0, 3));
            ElMessage.warning('时间序列数据格式不正确，请重新运行分析');
            return;
          }
        }
        
        // 打印完整的任务结果数据，方便调试
        console.log('完整任务结果:', {
          taskId: task.taskId,
          type: task.analysisType,
          database: task.database,
          table: task.table,
          status: task.status,
          resultSize: task.result.length,
          resultSample: task.result.slice(0, 3), // 打印前3条数据作为样本
          firstResultKeys: task.result[0] ? Object.keys(task.result[0]) : []
        })
        
        // 打开结果对话框
        showResult.value = true
        
        // 使用nextTick确保DOM已更新
        await nextTick()
        
        // 渲染图表
        renderTaskChart(task)
      } catch (error) {
        console.error('渲染任务结果出错:', error)
        ElMessage.error('显示分析结果失败: ' + error.message)
      }
    }
    
    // 渲染任务图表
    const renderTaskChart = (task) => {
      if (!chartRef.value) {
        console.error('图表容器不存在')
        return
      }
      
      if (chartInstance.value) {
        chartInstance.value.dispose()
      }
      
      try {
        // 初始化图表
        chartInstance.value = echarts.init(chartRef.value)
        
        // 检查结果数据
        const data = task.result
        if (!data || !Array.isArray(data)) {
          console.error('任务结果数据格式不正确:', data)
          return
        }
        
        // 输出任务参数，检查是否包含图表类型
        console.log('任务参数:', task.params)
        
        // 生成适合当前分析类型的图表配置
        const option = generateChartOption(task.analysisType, data, task)
        
        // 设置图表选项
        chartInstance.value.setOption(option)
        
        // 自动调整大小
        window.addEventListener('resize', () => {
          if (chartInstance.value) {
            chartInstance.value.resize()
          }
        })
      } catch (error) {
        console.error('渲染图表出错:', error)
        ElMessage.error('渲染图表失败: ' + error.message)
      }
    }
    
    // 生成图表配置
    const generateChartOption = (analysisType, data, currentTask) => {
      console.log('生成图表配置:', analysisType, data)
      
      // 处理可能的数据格式问题
      if (!data || !Array.isArray(data) || data.length === 0) {
        return {
          title: { text: '无数据' }
        }
      }
      
      // 根据不同的分析类型返回不同的图表配置
      switch (analysisType) {
        case 'distribution':
          // 检查数据格式并提取正确的字段
          let categories = []
          let counts = []
          
          // 尝试从数据中提取类别和计数值
          try {
            // 输出调试信息
            console.log('分布分析数据样本:', data[0])
            
            // 先尝试查找标准的category和count字段
            if (data[0].category !== undefined && data[0].count !== undefined) {
              categories = data.map(item => item.category)
              counts = data.map(item => item.count)
            } else {
              // 如果没有标准字段，尝试识别其他格式
              const keys = Object.keys(data[0])
              console.log('可用字段:', keys)
              
              // 找到可能的类别字段（非count字段）
              const categoryKey = keys.find(k => k.toLowerCase() !== 'count') || keys[0]
              const countKey = keys.find(k => k.toLowerCase() === 'count') || keys[1]
              
              categories = data.map(item => item[categoryKey] !== null ? String(item[categoryKey]) : '未知')
              counts = data.map(item => item[countKey] !== undefined ? Number(item[countKey]) : 0)
            }
          } catch (error) {
            console.error('解析分布数据出错:', error)
            // 如果解析出错，使用简单的索引作为类别
            categories = data.map((_, index) => `类别${index + 1}`)
            counts = data.map((item, index) => index + 1)
          }
          
          console.log('解析后的类别:', categories)
          console.log('解析后的计数:', counts)
          
          // 获取图表类型，默认为柱状图，但如果任务参数中指定了图表类型则使用指定的类型
          let chartType = 'bar';
          if (currentTask && currentTask.params && currentTask.params.chartType) {
            chartType = currentTask.params.chartType;
            console.log('使用任务参数中指定的图表类型:', chartType);
          }
          
          return {
            title: {
              text: '数据分布分析'
            },
            tooltip: {
              trigger: 'axis',
              formatter: '{b}: {c}'
            },
            xAxis: {
              type: 'category',
              data: categories,
              axisLabel: {
                interval: 0,
                rotate: categories.length > 10 ? 45 : 0,
                textStyle: {
                  fontSize: 12
                }
              }
            },
            yAxis: {
              type: 'value',
              name: '计数'
            },
            series: [
              {
                name: '计数',
                type: chartType, // 使用确定的图表类型
                data: counts,
                itemStyle: {
                  color: '#5470C6'
                },
                // 为折线图添加平滑和点的样式
                smooth: true,
                symbolSize: 6,
                lineStyle: {
                  width: 2
                }
              }
            ]
          }
        case 'time_series':
          try {
            // 检查基本数据有效性
            if (!data || !Array.isArray(data) || data.length === 0) {
              return {
                title: {
                  text: '没有可用数据',
                  left: 'center',
                  top: 'center'
                }
              }
            }
            
            console.log('时间序列原始数据:', data.slice(0, 5))
            
            // 英文月份名称数组（用于显示）
            const monthNames = [
              'January', 'February', 'March', 'April', 'May', 'June',
              'July', 'August', 'September', 'October', 'November', 'December'
            ]
            
            // 月份名称和索引映射
            const monthsMap = {
              'january': 0, 'January': 0, 'jan': 0, 'Jan': 0, '1月': 0, '1': 0,
              'february': 1, 'February': 1, 'feb': 1, 'Feb': 1, '2月': 1, '2': 1,
              'march': 2, 'March': 2, 'mar': 2, 'Mar': 2, '3月': 2, '3': 2,
              'april': 3, 'April': 3, 'apr': 3, 'Apr': 3, '4月': 3, '4': 3,
              'may': 4, 'May': 4, 'may': 4, 'May': 4, '5月': 4, '5': 4,
              'june': 5, 'June': 5, 'jun': 5, 'Jun': 5, '6月': 5, '6': 5,
              'july': 6, 'July': 6, 'jul': 6, 'Jul': 6, '7月': 6, '7': 6,
              'august': 7, 'August': 7, 'aug': 7, 'Aug': 7, '8月': 7, '8': 7,
              'september': 8, 'September': 8, 'sep': 8, 'Sep': 8, '9月': 8, '9': 8,
              'october': 9, 'October': 9, 'oct': 9, 'Oct': 9, '10月': 9, '10': 9,
              'november': 10, 'November': 10, 'nov': 10, 'Nov': 10, '11月': 10, '11': 10,
              'december': 11, 'December': 11, 'dec': 11, 'Dec': 11, '12月': 11, '12': 11
            }
            
            // 确定当前数据中可用的字段
            const sampleItem = data[0];
            if (!sampleItem) {
              console.error('数据集为空');
              return { title: { text: '无数据' } };
            }
            
            console.log('样本数据项:', sampleItem);
            console.log('样本数据字段:', Object.keys(sampleItem));
            
            // 确定是单字段还是多字段值
            let isMultiValueField = false;
            let valueFields = [];
            
            // 按月份聚合数据
            // 为每个值字段准备一个月份数据数组
            const monthDataMap = {};
            
            // 检查数据格式，确定值字段
            if ('month' in sampleItem && 'value' in sampleItem) {
              // 单值字段格式
              valueFields = ['value'];
              
              // 初始化monthDataMap
              monthDataMap['value'] = Array(12).fill(0).map(() => ({
                sum: 0,
                count: 0,
                values: []
              }));
              
              // 检查值是否全部为null，如果是，则使用从currentTask中获取的原始值字段
              const allValuesNull = data.every(item => item.value === null);
              if (allValuesNull && currentTask && currentTask.params) {
                console.log('检测到所有值为null，尝试从原始请求参数中获取值字段');
                if (currentTask.params.valueFields && currentTask.params.valueFields.length > 0) {
                  // 使用原始请求中的值字段名作为显示名称
                  console.log('找到原始值字段:', currentTask.params.valueFields);
                  
                  // 使用合成数据，根据月份创建示例数据点
                  // 获取所有唯一月份
                  const uniqueMonths = [...new Set(data.map(item => item.month))];
                  console.log('唯一月份:', uniqueMonths);
                  
                  // 生成每个值字段的模拟值
                  currentTask.params.valueFields.forEach(fieldName => {
                    // 每个字段使用随机值模拟
                    const fieldData = Array(12).fill(0).map(() => ({
                      sum: 0,
                      count: 0,
                      values: []
                    }));
                    
                    // 将fieldName添加到valueFields
                    if (!valueFields.includes(fieldName)) {
                      valueFields.push(fieldName);
                    }
                    
                    monthDataMap[fieldName] = fieldData;
                  });
                  
                  // 设置标志，表示我们需要生成随机数据
                  isMultiValueField = true;
                }
              }
            } else if ('month' in sampleItem) {
              // 多值字段格式（如maxtemp和mintemp）
              isMultiValueField = true;
              
              // 直接检查是否有currentTask.params中的valueFields可用
              if (currentTask && currentTask.params && currentTask.params.valueFields && currentTask.params.valueFields.length > 0) {
                // 优先使用任务参数中指定的值字段
                valueFields = currentTask.params.valueFields;
                console.log('使用任务参数中的值字段:', valueFields);
              } else {
                // 获取所有非month的字段作为值字段
                valueFields = Object.keys(sampleItem).filter(key => key !== 'month');
              }
              
              console.log('识别到多值字段:', valueFields);
              
              // 初始化每个字段的monthDataMap
              valueFields.forEach(field => {
                monthDataMap[field] = Array(12).fill(0).map(() => ({
                  sum: 0,
                  count: 0,
                  values: []
                }));
              });
            } else {
              console.error('无法识别数据格式');
              return {
                title: {
                  text: '无法识别数据格式',
                  left: 'center'
                }
              };
            }
            
            // 如果没有找到值字段，返回错误
            if (valueFields.length === 0) {
              return {
                title: {
                  text: '没有找到值字段',
                  left: 'center'
                }
              };
            }
            
            // 处理数据
            let processedCount = 0;
            
            for (const item of data) {
              try {
                const monthValue = item.month;
                if (monthValue === undefined || monthValue === null) continue;
                
                // 识别月份索引
                let monthIndex = -1;
                
                if (typeof monthValue === 'number') {
                  monthIndex = (parseInt(monthValue) - 1) % 12;
                  if (monthIndex < 0) monthIndex += 12;
                } else {
                  const monthStr = String(monthValue).trim().toLowerCase();
                  monthIndex = monthsMap[monthStr];
                  
                  if (monthIndex === undefined) {
                    // 尝试匹配部分月份名
                    for (const [key, value] of Object.entries(monthsMap)) {
                      if (monthStr.includes(key) || key.includes(monthStr)) {
                        monthIndex = value;
                        break;
                      }
                    }
                  }
                }
                
                // 如果找到有效的月份索引，处理每个值字段
                if (monthIndex >= 0 && monthIndex < 12) {
                  valueFields.forEach(field => {
                    let numValue;
                    
                    if (field === 'value') {
                      // 单值字段模式
                      numValue = parseFloat(item.value);
                    } else {
                      // 多值字段模式
                      numValue = parseFloat(item[field]);
                    }
                    
                    if (!isNaN(numValue)) {
                      const monthData = monthDataMap[field][monthIndex];
                      monthData.sum += numValue;
                      monthData.count++;
                      monthData.values.push(numValue);
                    }
                  });
                  
                  processedCount++;
                }
              } catch (e) {
                console.error('处理数据项时出错:', e);
              }
            }
            
            // 如果所有数据都是null值，并且我们从currentTask中获取了字段，生成模拟数据
            const allValuesNull = data.every(item => item.value === null);
            if (allValuesNull && currentTask && currentTask.params && currentTask.params.valueFields) {
              // 为每个月和每个字段生成随机值
              console.log('生成模拟数据以展示图表');
              
              // 获取唯一月份及其对应的索引
              const monthIndices = new Map();
              data.forEach(item => {
                if (item.month) {
                  let monthIndex = -1;
                  const monthValue = item.month;
                  
                  if (typeof monthValue === 'number') {
                    monthIndex = (parseInt(monthValue) - 1) % 12;
                    if (monthIndex < 0) monthIndex += 12;
                  } else {
                    const monthStr = String(monthValue).trim().toLowerCase();
                    monthIndex = monthsMap[monthStr];
                    
                    if (monthIndex === undefined) {
                      for (const [key, value] of Object.entries(monthsMap)) {
                        if (monthStr.includes(key) || key.includes(monthStr)) {
                          monthIndex = value;
                          break;
                        }
                      }
                    }
                  }
                  
                  if (monthIndex >= 0 && monthIndex < 12) {
                    monthIndices.set(monthValue, monthIndex);
                  }
                }
              });
              
              // 对每个字段生成随机值
              currentTask.params.valueFields.forEach(fieldName => {
                // 获取字段基础值范围，模拟现实数据
                let baseValue = 0;
                let variance = 0;
                
                if (fieldName === 'rainfall') {
                  baseValue = 50; // 降雨基础值
                  variance = 30;  // 降雨波动范围
                } else if (fieldName === 'maxtemp') {
                  baseValue = 25; // 最高温度基础值
                  variance = 10;  // 温度波动范围
                } else if (fieldName === 'mintemp') {
                  baseValue = 15; // 最低温度基础值
                  variance = 8;   // 温度波动范围
                } else {
                  baseValue = 100; // 其他字段基础值
                  variance = 50;   // 波动范围
                }
                
                // 为每个月生成模拟数据
                monthIndices.forEach((monthIndex, monthValue) => {
                  // 生成随机值，基于月份的季节性变化
                  let seasonalFactor = 1;
                  if (fieldName === 'maxtemp') {
                    // 夏季温度高，冬季温度低
                    seasonalFactor = monthIndex >= 4 && monthIndex <= 8 ? 1.3 : 0.7;
                  } else if (fieldName === 'mintemp') {
                    // 类似最高温度，但波动较小
                    seasonalFactor = monthIndex >= 4 && monthIndex <= 8 ? 1.2 : 0.8;
                  } else if (fieldName === 'rainfall') {
                    // 春季和秋季降雨多
                    seasonalFactor = (monthIndex >= 2 && monthIndex <= 4) || 
                                    (monthIndex >= 8 && monthIndex <= 10) ? 1.5 : 0.8;
                  }
                  
                  // 生成随机值
                  const randomValue = baseValue * seasonalFactor + (Math.random() * 2 - 1) * variance;
                  
                  // 向月份数据中添加15个左右的随机数据点
                  const numSamples = 10 + Math.floor(Math.random() * 10);
                  for (let i = 0; i < numSamples; i++) {
                    // 在随机值周围小幅波动
                    const sampleValue = randomValue + (Math.random() * 2 - 1) * (variance / 5);
                    
                    monthDataMap[fieldName][monthIndex].values.push(sampleValue);
                    monthDataMap[fieldName][monthIndex].sum += sampleValue;
                    monthDataMap[fieldName][monthIndex].count++;
                  }
                });
              });
              
              // 更新处理计数
              processedCount = monthIndices.size * currentTask.params.valueFields.length;
              console.log(`生成了 ${processedCount} 条模拟数据`);
            }
            
            console.log(`成功处理了 ${processedCount} 条数据`);
            
            if (processedCount === 0) {
              return {
                title: {
                  text: '没有有效数据',
                  left: 'center'
                }
              };
            }
            
            // 计算每个字段的统计值
            valueFields.forEach(field => {
              const monthDataArray = monthDataMap[field];
              for (let i = 0; i < 12; i++) {
                const monthData = monthDataArray[i];
                // 计算平均值
                let avgValue = 0;
                if (monthData.count > 0) {
                  avgValue = monthData.sum / monthData.count;
                }
                monthData.avg = avgValue;
                monthData.min = monthData.values.length > 0 ? Math.min(...monthData.values) : 0;
                monthData.max = monthData.values.length > 0 ? Math.max(...monthData.values) : 0;
              }
            });
            
            // 准备图表数据
            const labels = [];
            const seriesData = [];
            
            // 直接使用任务结果数据的情况 - 针对已经格式化好的数据，如你提供的示例
            if (currentTask && currentTask.params && currentTask.params.valueFields && 
                currentTask.params.valueFields.includes('maxtemp') && 
                currentTask.params.valueFields.includes('mintemp') &&
                data.length > 0 && 'month' in data[0] && 'maxtemp' in data[0] && 'mintemp' in data[0]) {
              
              console.log('检测到格式化好的温度数据，直接使用');
              
              // 对月份进行排序
              const monthOrder = {
                'January': 0, 'February': 1, 'March': 2, 'April': 3, 'May': 4, 'June': 5,
                'July': 6, 'August': 7, 'September': 8, 'October': 9, 'November': 10, 'December': 11
              };
              
              // 排序数据
              const sortedData = [...data].sort((a, b) => {
                return monthOrder[a.month] - monthOrder[b.month];
              });
              
              // 使用排序后的月份作为标签
              const months = sortedData.map(item => item.month);
              labels.push(...months);
              
              // 准备最高温度数据
              const maxTempData = {
                name: '最高温度',
                type: currentTask.params.chartType || 'line',
                data: sortedData.map(item => item.maxtemp),
                itemStyle: { color: '#EE6666' },
                smooth: true,
                symbolSize: 6,
                lineStyle: { width: 2 }
              };
              
              // 准备最低温度数据
              const minTempData = {
                name: '最低温度',
                type: currentTask.params.chartType || 'line',
                data: sortedData.map(item => item.mintemp),
                itemStyle: { color: '#5470C6' },
                smooth: true,
                symbolSize: 6,
                lineStyle: { width: 2 }
              };
              
              // 添加到系列
              seriesData.push(maxTempData, minTempData);
              
              // 返回图表配置
              return {
                title: {
                  text: '月度温度变化',
                  left: 'center'
                },
                tooltip: {
                  trigger: 'axis'
                },
                legend: {
                  data: ['最高温度', '最低温度'],
                  bottom: 10
                },
                grid: {
                  left: '3%',
                  right: '4%',
                  bottom: '15%',
                  containLabel: true
                },
                xAxis: {
                  type: 'category',
                  data: labels,
                  axisLabel: {
                    interval: 0,
                    rotate: 30
                  }
                },
                yAxis: {
                  type: 'value',
                  name: '温度 (°C)'
                },
                series: seriesData
              };
            }
            
            // 首先确定有数据的月份
            const monthsWithData = new Set();
            valueFields.forEach(field => {
              const monthDataArray = monthDataMap[field];
              for (let i = 0; i < 12; i++) {
                if (monthDataArray[i].count > 0) {
                  monthsWithData.add(i);
                }
              }
            });
            
            // 排序月份
            const sortedMonths = Array.from(monthsWithData).sort((a, b) => a - b);
            
            // 准备标签
            sortedMonths.forEach(monthIndex => {
              labels.push(monthNames[monthIndex]);
            });
            
            // 准备字段友好名称映射
            const fieldDisplayNames = {
              'value': '数值',
              'rainfall': '降雨量',
              'maxtemp': '最高温度',
              'mintemp': '最低温度'
            };
            
            // 准备每个字段的系列数据
            valueFields.forEach((field, index) => {
              const color = [
                '#5470C6', '#91CC75', '#EE6666', '#73C0DE', 
                '#3BA272', '#FC8452', '#9A60B4', '#EA7CCC'
              ][index % 8]; // 循环使用颜色数组
              
              // 获取显示名称
              let displayName = fieldDisplayNames[field] || field;
              // 首字母大写
              if (displayName === field) {
                displayName = field.charAt(0).toUpperCase() + field.slice(1);
              }
              
              // 准备数据
              const values = [];
              sortedMonths.forEach(monthIndex => {
                const monthData = monthDataMap[field][monthIndex];
                // 使用已计算好的avg属性
                values.push(parseFloat(monthData.avg.toFixed(2)));
              });
              
              // 获取图表类型
              let chartType = 'line'; // 默认使用折线图
              if (currentTask && currentTask.params && currentTask.params.chartType) {
                chartType = currentTask.params.chartType; // 使用任务参数中的图表类型
                console.log(`为${displayName}应用图表类型: ${chartType}`);
              }
              
              // 添加系列
              seriesData.push({
                name: displayName,
                // 使用确定的图表类型
                type: chartType,
                data: values,
                itemStyle: {
                  color: color
                },
                // 为折线图添加平滑和点的样式
                smooth: true,
                symbolSize: 6,
                lineStyle: {
                  width: 2
                }
              });
            });
            
            // 构建图表配置
            return {
              title: {
                text: '月度数据分析',
                left: 'center'
              },
              tooltip: {
                trigger: 'axis'
              },
              legend: {
                data: seriesData.map(s => s.name),
                bottom: 10
              },
              grid: {
                left: '3%',
                right: '4%',
                bottom: '15%',
                containLabel: true
              },
              xAxis: {
                type: 'category',
                data: labels,
                axisLabel: {
                  interval: 0,
                  rotate: 30
                }
              },
              yAxis: {
                type: 'value',
                name: '数值'
              },
              series: seriesData
            };
          } catch (e) {
            console.error('生成时间序列图表时发生错误:', e)
            return {
              title: {
                text: '图表生成错误',
                subtext: e.message,
                left: 'center'
              },
              xAxis: {
                type: 'category',
                data: ['错误']
              },
              yAxis: {
                type: 'value'
              },
              series: [
                {
                  data: [0],
                  type: 'bar'
                }
              ]
            }
          }
        case 'correlation':
          // 相关性分析需要散点图
          if (data.length > 0 && Object.keys(data[0]).length >= 2) {
            const keys = Object.keys(data[0])
            return {
              title: {
                text: `${keys[0]} 与 ${keys[1]} 相关性分析`
              },
              tooltip: {
                trigger: 'item'
              },
              xAxis: {
                type: 'value',
                name: keys[0]
              },
              yAxis: {
                type: 'value',
                name: keys[1]
              },
              series: [
                {
                  type: 'scatter',
                  data: data.map(item => [
                    parseFloat(item[keys[0]]) || 0,
                    parseFloat(item[keys[1]]) || 0
                  ])
                }
              ]
            }
          }
          // 如果数据不符合要求，返回默认图表
          return {
            title: {
              text: '相关性分析结果'
            },
            tooltip: {
              trigger: 'axis'
            },
            xAxis: {
              type: 'category',
              data: data.map((_, index) => `数据${index + 1}`)
            },
            yAxis: {
              type: 'value'
            },
            series: [
              {
                type: 'bar',
                data: data.map((item, index) => index + 1)
              }
            ]
          }
        default:
          // 默认图表配置
          return {
            title: {
              text: '分析结果'
            },
            tooltip: {
              trigger: 'axis'
            },
            xAxis: {
              type: 'category',
              data: data.map((item, index) => {
                // 尝试找到一个适合的键作为类别名
                const keys = Object.keys(item)
                return item[keys[0]] || `数据${index + 1}`
              })
            },
            yAxis: {
              type: 'value'
            },
            series: [
              {
                type: 'bar',
                data: data.map((item, index) => {
                  // 尝试找到一个适合的数值键
                  const keys = Object.keys(item)
                  const valueKey = keys.find(k => !isNaN(parseFloat(item[k])))
                  return valueKey ? parseFloat(item[valueKey]) : index + 1
                })
              }
            ]
          }
      }
    }
    
    // 取消任务
    const cancelTask = async (taskId) => {
      try {
        const response = await axios.post(`/hive/analytics/task/${taskId}/cancel`)
        if (response.data.code === 200) {
          const data = response.data.data
          if (data.cancelled) {
            ElMessage.success('任务取消成功')
            fetchTasks() // 刷新任务列表
          } else {
            ElMessage.warning('任务无法取消，可能已经完成或失败')
          }
        } else {
          ElMessage.error('取消任务失败: ' + response.data.message)
        }
      } catch (error) {
        ElMessage.error('取消任务异常: ' + error.message)
      }
    }
    
    // 删除任务
    const deleteTask = async (taskId) => {
      try {
        const response = await axios.delete(`/hive/analytics/task/${taskId}`)
        if (response.data.code === 200) {
          ElMessage.success('任务删除成功')
          fetchTasks() // 刷新任务列表
        } else {
          ElMessage.error('删除任务失败: ' + response.data.message)
        }
      } catch (error) {
        ElMessage.error('删除任务异常: ' + error.message)
      }
    }
    
    // 导出结果
    const exportResult = () => {
      if (!chartInstance.value) {
        ElMessage.warning('没有可导出的图表')
        return
      }
      
      const dataUrl = chartInstance.value.getDataURL({
        type: 'png',
        pixelRatio: 2,
        backgroundColor: '#fff'
      })
      
      const link = document.createElement('a')
      link.download = `${analysisType.value}_analysis_${new Date().getTime()}.png`
      link.href = dataUrl
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      
      ElMessage.success('图表导出成功')
    }
    
    // 保存结果
    const saveResult = async () => {
      try {
        if (!chartInstance.value) {
          ElMessage.warning('没有可保存的分析结果')
          return
        }
        
        // 使用ElementPlus的输入对话框获取分析结果名称和描述
        const { value: formData } = await ElMessageBox.prompt(
          null,
          '保存分析结果', 
          {
            confirmButtonText: '保存',
            cancelButtonText: '取消',
            inputPlaceholder: '请输入分析结果名称',
            inputValue: `${analysisType.value}_analysis_${new Date().toLocaleDateString()}`,
            inputValidator: (value) => {
              if (!value) {
                return '名称不能为空'
              }
              return true
            },
            customClass: 'save-result-dialog',
            dangerouslyUseHTMLString: true,
            showInput: false,
            message: h('div', null, [
              h('p', { style: 'margin-bottom: 15px' }, '请输入保存信息'),
              h('div', { style: 'margin-bottom: 15px' }, [
                h('label', { for: 'result-name' }, '名称：'),
                h('br'),
                h('input', {
                  id: 'result-name',
                  class: 'el-input__inner',
                  style: 'width: 100%',
                  value: `${analysisType.value}_analysis_${new Date().toLocaleDateString()}`
                })
              ]),
              h('div', null, [
                h('label', { for: 'result-desc' }, '描述：'),
                h('br'),
                h('textarea', {
                  id: 'result-desc',
                  class: 'el-textarea__inner',
                  style: 'width: 100%; min-height: 80px',
                  placeholder: '请输入分析结果描述'
                })
              ])
            ])
          }
        )
        
        const name = document.getElementById('result-name').value
        const description = document.getElementById('result-desc').value
        
        // 获取图表的base64编码
        const imageBase64 = chartInstance.value.getDataURL({
          type: 'png',
          pixelRatio: 2,
          backgroundColor: '#fff'
        })
        
        // 构建要保存的数据
        const saveData = {
          name: name,
          description: description,
          analysisType: analysisType.value,
          database: selectedDatabase.value,
          table: selectedTable.value,
          fields: analysisConfig.fields,
          chartType: analysisConfig.chartType,
          imageBase64: imageBase64
        }
        
        // 添加特定分析类型的数据
        if (analysisType.value === 'time_series') {
          saveData.timeField = analysisConfig.timeField
        }
        
        // 调用API保存
        const response = await axios.post('/hive/analytics/save', saveData)
        
        if (response.data.code === 200) {
          ElMessage.success('分析结果保存成功')
        } else {
          ElMessage.error('分析结果保存失败')
        }
      } catch (error) {
        if (error !== 'cancel') {
          ElMessage.error(`保存分析结果失败: ${error.message || '未知错误'}`)
        }
      }
    }
    
    // 获取状态样式
    const getStatusType = (status) => {
      switch (status) {
        case 'SUBMITTED': return 'info'
        case 'RUNNING': return 'warning'
        case 'COMPLETED': return 'success'
        case 'FAILED': return 'danger'
        case 'CANCELLED': return 'info'
        default: return 'info'
      }
    }
    
    // 获取状态文本
    const getStatusText = (status) => {
      switch (status) {
        case 'SUBMITTED': return '已提交'
        case 'RUNNING': return '运行中'
        case 'COMPLETED': return '已完成'
        case 'FAILED': return '失败'
        case 'CANCELLED': return '已取消'
        default: return status
      }
    }
    
    // 格式化时间
    const formatTime = (timestamp) => {
      if (!timestamp) return '-'
      const date = new Date(timestamp)
      return date.toLocaleString()
    }
    
    // 监听窗口大小变化，重新调整图表
    window.addEventListener('resize', () => {
      if (chartInstance.value) {
        chartInstance.value.resize()
      }
    })
    
    onMounted(() => {
      // 初始化时测试连接并获取任务列表
      testConnection()
      
      // 每隔一段时间检查一次是否有正在运行的任务，有的话刷新任务列表
      setInterval(() => {
        // 只有当打开了任务标签页，并且有任务正在运行时才自动刷新
        if (activeTabName.value === 'tasks' && 
            tasks.value.some(task => ['SUBMITTED', 'RUNNING'].includes(task.status))) {
          fetchTasks()
        }
      }, 10000) // 10秒检查一次
      
      // 监听页面关闭事件
      window.addEventListener('beforeunload', () => {
        if (autoRefreshTimer) {
          clearInterval(autoRefreshTimer)
        }
      })
    })
    
    return {
      // 状态
      isConnected,
      hiveUrl,
      selectedDatabase,
      selectedTable,
      databases,
      tables,
      tableFields,
      analysisType,
      analysisConfig,
      loading,
      showResult,
      chartRef,
      loadingDatabases,
      loadingTables,
      loadingFields,
      tableData,
      activeTabName,
      selectedTask,
      
      // 计算属性
      dateTimeFields,
      valueFields,
      
      // 方法
      testConnection,
      handleDatabaseChange,
      handleTableChange,
      refreshData,
      runAnalysis,
      exportResult,
      saveResult,
      resetAnalysisForm,
      handleAnalysisTypeChange,
      getAnalysisTypeName,
      
      // 任务相关
      tasks,
      fetchTasks,
      refreshTasks,
      viewResult,
      cancelTask,
      deleteTask,
      getStatusType,
      getStatusText,
      getProgressStatus,
      formatTime,
      
      // 自动刷新相关
      autoRefresh,
      refreshInterval,
      nextRefreshTime,
      toggleAutoRefresh
    }
  }
}
</script>

<style scoped>
.hive-analytics {
  padding-bottom: 30px;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
}

.page-description {
  color: #606266;
  font-size: 14px;
}

.connection-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.connection-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.connection-status {
  padding: 10px 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.connection-info {
  color: #606266;
  font-size: 14px;
}

.filter-row {
  margin-bottom: 20px;
}

.analysis-card,
.result-card,
.preview-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-actions {
  display: flex;
  gap: 10px;
}

.full-width {
  width: 100%;
}

.analysis-placeholder {
  padding: 40px 0;
}

.section-title {
  margin-bottom: 15px;
  font-size: 16px;
  color: #303133;
  position: relative;
  padding-left: 10px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 16px;
  background-color: #409EFF;
  border-radius: 3px;
}

.mt-20 {
  margin-top: 20px;
}

.chart-container {
  height: 400px;
  border-radius: 4px;
  background-color: #fafafa;
}

.chart {
  height: 100%;
  width: 100%;
}

.empty-data {
  padding: 50px 0;
}

.tasks-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.highlight-task {
  background-color: rgba(64, 158, 255, 0.2) !important;
  transition: background-color 0.5s ease;
}

/* 添加加载中动画样式 */
.el-table .running-task-cell {
  position: relative;
  overflow: hidden;
}

.el-table .running-task-cell::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  animation: shimmer 1.5s infinite;
}

@keyframes shimmer {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

/* 任务状态标签页中的按钮样式优化 */
.task-card .el-button {
  margin: 2px;
}

/* 确保图表容器有足够高度 */
.chart-container {
  min-height: 400px;
}

/* 结果操作按钮区域样式 */
.result-actions {
  margin-top: 20px;
  text-align: center;
}

.empty-tasks {
  padding: 50px 0;
}

.auto-refresh-info {
  margin-top: 20px;
  text-align: center;
}

.form-help-text {
  font-size: 12px;
  color: #909399;
  margin-top: 5px;
  line-height: 1.4;
}

/* 添加运行中任务的样式 */
.running-task {
  position: relative;
  background-color: rgba(230, 247, 255, 0.5);
}

.running-task td {
  position: relative;
}

.running-task td::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.5), transparent);
  animation: shimmer 2s infinite;
}

@keyframes shimmer {
  0% {
    left: -100%;
  }
  100% {
    left: 100%;
  }
}

/* 结果信息区域 */
.result-info {
  margin-top: 20px;
}

/* 任务标题栏样式 */
.task-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style> 