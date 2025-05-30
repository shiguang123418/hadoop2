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
    
    <el-card class="analysis-card" shadow="hover" v-if="selectedTable">
      <template #header>
        <div class="card-header">
          <span>选择分析类型</span>
        </div>
      </template>
      <el-select v-model="analysisType" placeholder="请选择分析类型" class="full-width">
        <el-option label="数据分布分析" value="distribution" />
        <el-option label="相关性分析" value="correlation" />
        <el-option label="时间序列分析" value="time_series" />
        <el-option label="聚类分析" value="clustering" />
        <el-option label="回归分析" value="regression" />
      </el-select>
      
      <div class="analysis-placeholder" v-if="!analysisType">
        <el-empty description="请选择一种分析类型开始数据分析" />
      </div>
      
      <div v-else class="analysis-config mt-20">
        <h3 class="section-title">分析配置</h3>
        <el-form :model="analysisConfig" label-position="top">
          <el-form-item label="选择字段">
            <el-select v-model="analysisConfig.fields" multiple placeholder="请选择分析字段" class="full-width">
              <el-option 
                v-for="field in tableFields" 
                :key="field.name" 
                :label="`${field.name}（${field.type}）`" 
                :value="field.name" />
            </el-select>
          </el-form-item>
          
          <el-form-item v-if="analysisType === 'time_series'" label="时间字段">
            <el-select v-model="analysisConfig.timeField" placeholder="请选择时间字段" class="full-width">
              <el-option 
                v-for="field in dateTimeFields" 
                :key="field.name" 
                :label="`${field.name}（${field.type}）`" 
                :value="field.name" />
            </el-select>
          </el-form-item>
          
          <el-form-item label="图表类型">
            <el-radio-group v-model="analysisConfig.chartType">
              <el-radio label="bar">柱状图</el-radio>
              <el-radio label="line">折线图</el-radio>
              <el-radio label="pie">饼图</el-radio>
              <el-radio label="scatter">散点图</el-radio>
            </el-radio-group>
          </el-form-item>
          
          <el-form-item>
            <el-button type="primary" @click="runAnalysis" :loading="loading">
              <el-icon><DataAnalysis /></el-icon>
              <span>运行分析</span>
            </el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
    
    <el-card v-if="selectedTable && !analysisType" class="preview-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>数据预览</span>
        </div>
      </template>
      <el-table 
        v-if="tableData.length > 0" 
        :data="tableData" 
        border 
        style="width: 100%"
        max-height="400">
        <el-table-column 
          v-for="field in tableFields" 
          :key="field.name"
          :prop="field.name" 
          :label="field.name"
          :width="150">
        </el-table-column>
      </el-table>
      <div v-else class="empty-data">
        <el-empty description="暂无数据" />
      </div>
    </el-card>
    
    <el-card v-if="showResult" class="result-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>分析结果</span>
          <div class="header-actions">
            <el-button type="primary" size="small" @click="exportResult">
              <el-icon><Download /></el-icon>
              <span>导出</span>
            </el-button>
            <el-button type="success" size="small" @click="saveResult">
              <el-icon><Check /></el-icon>
              <span>保存</span>
            </el-button>
          </div>
        </div>
      </template>
      <div class="chart-container">
        <div class="chart" ref="chartRef"></div>
      </div>
    </el-card>
    
    <el-card v-if="tasks.length > 0" class="tasks-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>分析任务状态</span>
          <div class="header-actions">
            <el-button type="primary" size="small" @click="refreshTasks">
              <el-icon><Refresh /></el-icon>
              <span>刷新</span>
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="tasks" stripe style="width: 100%">
        <el-table-column prop="taskId" label="任务ID" width="120">
          <template #default="scope">
            <el-tooltip :content="scope.row.taskId" placement="top">
              <span>{{ scope.row.taskId.substring(0, 8) }}...</span>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column prop="analysisType" label="分析类型" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="scope">
            <el-tag :type="getStatusType(scope.row.status)">
              {{ getStatusText(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="progress" label="进度" width="120">
          <template #default="scope">
            <el-progress :percentage="scope.row.progress || 0" :status="getProgressStatus(scope.row.status)" />
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="endTime" label="完成时间" width="180">
          <template #default="scope">
            {{ scope.row.endTime ? formatTime(scope.row.endTime) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="scope">
            <el-button 
              size="small" 
              type="primary"
              :disabled="scope.row.status !== 'COMPLETED'"
              @click="viewResult(scope.row)">
              查看结果
            </el-button>
            <el-button 
              size="small" 
              type="danger"
              :disabled="!['SUBMITTED', 'RUNNING'].includes(scope.row.status)"
              @click="cancelTask(scope.row.taskId)">
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
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
      chartType: 'bar'
    })
    
    const databases = ref([])
    const tables = ref([])
    const tableFields = ref([])
    
    // 任务列表
    const tasks = ref([])
    
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
        const type = field.type ? field.type.toLowerCase() : ''
        return type.includes('date') || type.includes('time') || type.includes('timestamp')
      })
    })
    
    // 运行分析
    const runAnalysis = async () => {
      if (!analysisConfig.fields.length) {
        ElMessage.warning('请至少选择一个分析字段')
        return
      }
      
      if (analysisType.value === 'time_series' && !analysisConfig.timeField) {
        ElMessage.warning('时间序列分析请选择时间字段')
        return
      }
      
      loading.value = true
      
      try {
        const requestData = {
          analysisType: analysisType.value,
          database: selectedDatabase.value,
          table: selectedTable.value,
          fields: analysisConfig.fields
        }
        
        // 根据分析类型添加特定参数
        switch (analysisType.value) {
          case 'distribution':
            requestData.field = analysisConfig.fields[0] // 分布分析只需要一个字段
            break
          case 'time_series':
            requestData.timeField = analysisConfig.timeField
            requestData.valueField = analysisConfig.fields[0]
            break
          case 'correlation':
          case 'clustering':
          case 'regression':
            // 这些分析类型至少需要两个字段
            if (analysisConfig.fields.length < 2) {
              ElMessage.warning(`${analysisType.value}分析至少需要选择两个字段`)
              loading.value = false
              return
            }
            break
        }
        
        // 提交异步任务
        const response = await axios.post('/hive/analytics/submit', requestData, {
          timeout: 30000 // 30秒超时
        })
        
        if (response.data.code === 200) {
          const result = response.data.data
          const taskId = result.taskId
          
          ElMessage.info(`任务已提交，ID: ${taskId}，正在获取结果...`)
          
          // 轮询任务状态
          await pollTaskStatus(taskId)
        } else {
          ElMessage.error('任务提交失败')
        }
      } catch (error) {
        ElMessage.error(`执行分析异常: ${error.message}`)
      } finally {
        loading.value = false
      }
    }
    
    // 轮询任务状态
    const pollTaskStatus = async (taskId) => {
      let completed = false
      let attempts = 0
      const maxAttempts = 60 // 最多轮询60次
      const pollingInterval = 2000 // 2秒轮询一次
      
      while (!completed && attempts < maxAttempts) {
        try {
          const statusResponse = await axios.get(`/hive/analytics/task/${taskId}`)
          
          if (statusResponse.data.code === 200) {
            const taskStatus = statusResponse.data.data
            const status = taskStatus.status
            const progress = taskStatus.progress || 0
            
            // 更新进度条
            if (progress > 0) {
              // 这里可以添加进度条UI更新
              console.log(`任务进度: ${progress}%`)
            }
            
            if (status === 'COMPLETED') {
              completed = true
              showResult.value = true
              
              // 获取结果并渲染图表
              const result = taskStatus.result || []
              
              // 下一个tick渲染图表
              nextTick(() => {
                renderChart(result)
              })
              
              ElMessage.success('分析完成')
              return
            } else if (status === 'FAILED') {
              ElMessage.error(`任务执行失败: ${taskStatus.error || '未知错误'}`)
              return
            } else if (status === 'CANCELLED') {
              ElMessage.warning('任务已取消')
              return
            }
          } else {
            ElMessage.error('获取任务状态失败')
            return
          }
        } catch (error) {
          console.error('轮询任务状态失败:', error)
          // 继续轮询，不中断
        }
        
        attempts++
        if (!completed) {
          await new Promise(resolve => setTimeout(resolve, pollingInterval))
        }
      }
      
      if (!completed) {
        ElMessage.warning('任务执行时间过长，请稍后查看结果')
      }
    }
    
    // 初始化图表
    const renderChart = (data) => {
      if (chartInstance.value) {
        chartInstance.value.dispose()
      }
      
      const chartDom = chartRef.value
      if (!chartDom) return
      
      chartInstance.value = echarts.init(chartDom)
      
      let option = {}
      
      switch (analysisType.value) {
        case 'distribution':
          option = getDistributionChartOption(data)
          break
        case 'correlation':
          option = getCorrelationChartOption(data)
          break
        case 'time_series':
          option = getTimeSeriesChartOption(data)
          break
        case 'clustering':
          option = getClusteringChartOption(data)
          break
        case 'regression':
          option = getRegressionChartOption(data)
          break
      }
      
      chartInstance.value.setOption(option)
    }
    
    // 分布图表配置
    const getDistributionChartOption = (data) => {
      const field = analysisConfig.fields[0]
      const categories = data.map(item => item[field])
      const values = data.map(item => item['count'])
      
      return {
        title: {
          text: `${field} 数据分布`,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          }
        },
        xAxis: {
          type: 'category',
          data: categories,
          axisLabel: {
            interval: 0,
            rotate: 45
          }
        },
        yAxis: {
          type: 'value'
        },
        series: [
          {
            name: '数量',
            type: analysisConfig.chartType === 'pie' ? 'pie' : analysisConfig.chartType,
            data: analysisConfig.chartType === 'pie' ? 
              categories.map((cat, index) => ({ name: cat, value: values[index] })) :
              values
          }
        ]
      }
    }
    
    // 相关性图表配置
    const getCorrelationChartOption = (data) => {
      const field1 = analysisConfig.fields[0]
      const field2 = analysisConfig.fields[1]
      
      return {
        title: {
          text: `${field1} 与 ${field2} 的相关性分析`,
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        xAxis: {
          type: 'value',
          name: field1
        },
        yAxis: {
          type: 'value',
          name: field2
        },
        series: [
          {
            name: '数据点',
            type: 'scatter',
            data: data.map(item => [item[field1], item[field2]])
          }
        ]
      }
    }
    
    // 时间序列图表配置
    const getTimeSeriesChartOption = (data) => {
      const timeField = analysisConfig.timeField
      const valueField = analysisConfig.fields[0]
      
      return {
        title: {
          text: `${valueField} 随时间变化趋势`,
          left: 'center'
        },
        tooltip: {
          trigger: 'axis'
        },
        xAxis: {
          type: 'category',
          data: data.map(item => item[timeField]),
          axisLabel: {
            interval: data.length > 30 ? Math.floor(data.length / 15) : 0,
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          name: valueField
        },
        series: [
          {
            name: valueField,
            type: 'line',
            data: data.map(item => item[valueField])
          }
        ]
      }
    }
    
    // 聚类分析图表配置
    const getClusteringChartOption = (data) => {
      if (analysisConfig.fields.length < 2) return {}
      
      const field1 = analysisConfig.fields[0]
      const field2 = analysisConfig.fields[1]
      
      return {
        title: {
          text: '聚类分析',
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        xAxis: {
          type: 'value',
          name: field1
        },
        yAxis: {
          type: 'value',
          name: field2
        },
        series: [
          {
            name: '数据点',
            type: 'scatter',
            data: data.map(item => [item[field1], item[field2]])
          }
        ]
      }
    }
    
    // 回归分析图表配置
    const getRegressionChartOption = (data) => {
      if (analysisConfig.fields.length < 2) return {}
      
      const field1 = analysisConfig.fields[0]
      const field2 = analysisConfig.fields[1]
      
      // 提取数据点
      const points = data.map(item => [parseFloat(item[field1]), parseFloat(item[field2])])
        .filter(point => !isNaN(point[0]) && !isNaN(point[1]))
      
      // 简单线性回归
      let sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0
      const n = points.length
      
      for (let i = 0; i < n; i++) {
        sumX += points[i][0]
        sumY += points[i][1]
        sumXY += points[i][0] * points[i][1]
        sumX2 += points[i][0] * points[i][0]
      }
      
      const meanX = sumX / n
      const meanY = sumY / n
      
      const slope = (sumXY - n * meanX * meanY) / (sumX2 - n * meanX * meanX)
      const intercept = meanY - slope * meanX
      
      // 生成回归线
      const minX = Math.min(...points.map(p => p[0]))
      const maxX = Math.max(...points.map(p => p[0]))
      
      const lineStart = [minX, slope * minX + intercept]
      const lineEnd = [maxX, slope * maxX + intercept]
      
      return {
        title: {
          text: `${field2} = ${slope.toFixed(4)} × ${field1} + ${intercept.toFixed(4)}`,
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        xAxis: {
          type: 'value',
          name: field1
        },
        yAxis: {
          type: 'value',
          name: field2
        },
        series: [
          {
            name: '数据点',
            type: 'scatter',
            data: points
          },
          {
            name: '回归线',
            type: 'line',
            data: [lineStart, lineEnd],
            showSymbol: false,
            lineStyle: {
              type: 'solid',
              color: '#f00'
            }
          }
        ]
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
    
    // 获取所有任务
    const fetchTasks = async () => {
      try {
        const response = await axios.get('/hive/analytics/tasks')
        if (response.data.code === 200) {
          const taskMap = response.data.data || {}
          tasks.value = Object.keys(taskMap).map(taskId => {
            return {
              taskId,
              ...taskMap[taskId]
            }
          }).sort((a, b) => b.startTime - a.startTime) // 按开始时间降序排序
        }
      } catch (error) {
        console.error('获取任务列表失败:', error)
      }
    }
    
    // 刷新任务列表
    const refreshTasks = () => {
      fetchTasks()
    }
    
    // 查看任务结果
    const viewResult = (task) => {
      if (task.status === 'COMPLETED' && task.result) {
        analysisType.value = task.analysisType
        analysisConfig.fields = task.params.fields || []
        if (task.analysisType === 'time_series') {
          analysisConfig.timeField = task.params.timeField
        }
        
        showResult.value = true
        nextTick(() => {
          renderChart(task.result)
        })
      } else {
        ElMessage.warning('任务尚未完成或没有结果')
      }
    }
    
    // 取消任务
    const cancelTask = async (taskId) => {
      try {
        const response = await axios.delete(`/hive/analytics/task/${taskId}`)
        if (response.data.code === 200) {
          ElMessage.success('任务已取消')
          refreshTasks()
        } else {
          ElMessage.error('取消任务失败')
        }
      } catch (error) {
        ElMessage.error(`取消任务失败: ${error.message}`)
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
    
    // 获取进度条状态
    const getProgressStatus = (status) => {
      switch (status) {
        case 'RUNNING': return ''
        case 'COMPLETED': return 'success'
        case 'FAILED': return 'exception'
        case 'CANCELLED': return 'warning'
        default: return ''
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
      fetchTasks()
      
      // 每30秒自动刷新任务列表
      setInterval(() => {
        if (tasks.value.some(task => ['SUBMITTED', 'RUNNING'].includes(task.status))) {
          fetchTasks()
        }
      }, 30000)
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
      
      // 计算属性
      dateTimeFields: computed(() => {
        return tableFields.value.filter(field => {
          const type = field.type ? field.type.toLowerCase() : ''
          return type.includes('date') || type.includes('time') || type.includes('timestamp')
        })
      }),
      
      // 方法
      testConnection,
      handleDatabaseChange,
      handleTableChange,
      refreshData,
      runAnalysis,
      exportResult,
      saveResult,
      
      // 任务相关
      tasks,
      refreshTasks,
      viewResult,
      cancelTask,
      getStatusType,
      getStatusText,
      getProgressStatus,
      formatTime
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
</style> 