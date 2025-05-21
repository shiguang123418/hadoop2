<template>
  <div class="hive-analytics">
    <div class="status-bar">
      <span :class="['status-indicator', connected ? 'connected' : 'disconnected']"></span>
      <span class="status-text">
        {{ connected ? '已连接' : '未连接' }} {{ hiveUrl }}
      </span>
      <button @click="refreshStatus" class="refresh-btn">刷新</button>
    </div>
    
    <!-- 数据库和表选择区域 -->
    <div class="database-selection">
      <div class="selection-item">
        <label>数据库:</label>
        <select v-model="currentDatabase" @change="loadTables">
          <option v-for="db in databases" :key="db.name" :value="db.name">
            {{ db.name }}
          </option>
        </select>
        <button @click="refreshDatabases" class="refresh-btn small">刷新</button>
      </div>
      
      <div class="selection-item">
        <label>表:</label>
        <select v-model="currentTable" @change="loadTableSchema">
          <option v-for="table in tables" :key="table.name" :value="table.name">
            {{ table.name }}
          </option>
        </select>
        <button @click="loadTables" class="refresh-btn small">刷新</button>
      </div>
    </div>
    
    <!-- 分析类型选择 -->
    <div class="analytics-selection">
      <h3>选择分析类型</h3>
      <div class="analytics-types-container">
        <div class="analytics-dropdown">
          <div class="analytics-dropdown-header" @click="toggleAnalyticsMenu">
            <span>{{ currentAnalysisType ? getAnalysisTypeLabel() : '请选择分析类型' }}</span>
            <i :class="['dropdown-icon', showAnalyticsMenu ? 'up' : 'down']"></i>
          </div>
          <div class="analytics-dropdown-content" v-show="showAnalyticsMenu">
            <div 
              v-for="type in analysisTypes" 
              :key="type.id" 
              class="analytics-dropdown-item"
              :class="{ active: currentAnalysisType === type.id }"
              @click="selectAnalysisType(type.id)"
            >
              <i :class="['analytics-icon', type.icon]"></i>
              <span>{{ type.name }}</span>
            </div>
          </div>
        </div>
        
        <div v-if="currentAnalysisType" class="selected-analysis-type">
          <div class="selected-type-header">
            <h4>{{ getAnalysisTypeLabel() }}</h4>
            <div class="analysis-description">{{ getAnalysisDescription() }}</div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 无选择时的提示 -->
    <div v-if="!currentAnalysisType && !showAnalyticsMenu" class="empty-selection">
      <p>请选择一种分析类型开始数据分析</p>
    </div>
    
    <!-- 分析配置和结果区域 -->
    <div v-if="currentAnalysisType" class="analysis-section">
      <div class="section-header">
        <button @click="currentAnalysisType = ''" class="back-btn">更改分析类型</button>
      </div>
      
      <!-- 加载动画 -->
      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <p>正在执行分析...</p>
      </div>
      
      <div v-else>
        <!-- 配置表单区域 -->
        <div class="config-form">
          <!-- 聚合分析表单 -->
          <div v-if="currentAnalysisType === 'aggregate'" class="form-container">
            <div class="form-group">
              <label>聚合列:</label>
              <select v-model="aggregateForm.aggregateColumn">
                <option v-for="col in numericColumns" :key="col.col_name" :value="col.col_name">{{ col.col_name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>聚合函数:</label>
              <select v-model="aggregateForm.aggregateFunction">
                <option value="COUNT">计数</option>
                <option value="SUM">求和</option>
                <option value="AVG">平均值</option>
                <option value="MAX">最大值</option>
                <option value="MIN">最小值</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>分组列:</label>
              <select v-model="aggregateForm.groupByColumn">
                <option value="">-- 不分组 --</option>
                <option v-for="col in tableSchema" :key="col.col_name" :value="col.col_name">{{ col.col_name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>条件:</label>
              <input type="text" v-model="aggregateForm.whereClause" placeholder="WHERE 子句 (可选)">
            </div>
            
            <div class="form-group">
              <label>结果限制:</label>
              <input type="number" v-model.number="aggregateForm.limit" min="1" max="1000" placeholder="最大结果数">
            </div>
            
            <button @click="executeAggregateAnalysis" class="execute-btn">执行分析</button>
          </div>
          
          <!-- 时间序列分析表单 -->
          <div v-else-if="currentAnalysisType === 'timeseries'" class="form-container">
            <div class="form-group">
              <label>时间列:</label>
              <select v-model="timeSeriesForm.timeColumn">
                <option v-for="col in dateColumns" :key="col.col_name" :value="col.col_name">{{ col.col_name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>值列:</label>
              <select v-model="timeSeriesForm.valueColumn">
                <option v-for="col in numericColumns" :key="col.col_name" :value="col.col_name">{{ col.col_name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>时间间隔:</label>
              <select v-model="timeSeriesForm.interval">
                <option value="day">天</option>
                <option value="week">周</option>
                <option value="month">月</option>
                <option value="year">年</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>聚合函数:</label>
              <select v-model="timeSeriesForm.aggregateFunction">
                <option value="COUNT">计数</option>
                <option value="SUM">求和</option>
                <option value="AVG">平均值</option>
                <option value="MAX">最大值</option>
                <option value="MIN">最小值</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>条件:</label>
              <input type="text" v-model="timeSeriesForm.whereClause" placeholder="WHERE 子句 (可选)">
            </div>
            
            <div class="form-group">
              <label>结果限制:</label>
              <input type="number" v-model.number="timeSeriesForm.limit" min="1" max="1000" placeholder="最大结果数">
            </div>
            
            <button @click="executeTimeSeriesAnalysis" class="execute-btn">执行分析</button>
          </div>
          
          <!-- 列值分布分析表单 -->
          <div v-else-if="currentAnalysisType === 'distribution'" class="form-container">
            <div class="form-group">
              <label>目标列:</label>
              <select v-model="distributionForm.columnName">
                <option v-for="col in tableSchema" :key="col.col_name" :value="col.col_name">{{ col.col_name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>结果限制:</label>
              <input type="number" v-model.number="distributionForm.limit" min="1" max="1000" placeholder="最大结果数">
            </div>
            
            <button @click="executeDistributionAnalysis" class="execute-btn">执行分析</button>
          </div>
          
          <!-- 统计信息分析表单 -->
          <div v-else-if="currentAnalysisType === 'statistics'" class="form-container">
            <div class="form-group">
              <label>目标列:</label>
              <select v-model="statisticsForm.columnName">
                <option v-for="col in numericColumns" :key="col.col_name" :value="col.col_name">{{ col.col_name }}</option>
              </select>
            </div>
            
            <button @click="executeStatisticsAnalysis" class="execute-btn">执行分析</button>
          </div>
          
          <!-- 相关性分析表单 -->
          <div v-else-if="currentAnalysisType === 'correlation'" class="form-container">
            <div class="form-group">
              <label>列 1:</label>
              <select v-model="correlationForm.column1">
                <option v-for="col in numericColumns" :key="col.col_name" :value="col.col_name">{{ col.col_name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>列 2:</label>
              <select v-model="correlationForm.column2">
                <option v-for="col in numericColumns" :key="col.col_name" :value="col.col_name">{{ col.col_name }}</option>
              </select>
            </div>
            
            <button @click="executeCorrelationAnalysis" class="execute-btn">执行分析</button>
          </div>
          
          <!-- 直方图分析表单 -->
          <div v-else-if="currentAnalysisType === 'histogram'" class="form-container">
            <div class="form-group">
              <label>目标列:</label>
              <select v-model="histogramForm.columnName">
                <option v-for="col in numericColumns" :key="col.col_name" :value="col.col_name">{{ col.col_name }}</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>分桶数:</label>
              <input type="number" v-model.number="histogramForm.numBuckets" min="2" max="100" placeholder="分桶数">
            </div>
            
            <button @click="executeHistogramAnalysis" class="execute-btn">执行分析</button>
          </div>
        </div>
        
        <!-- 分析结果区域 -->
        <div v-if="analysisResults.length > 0 || Object.keys(analysisResult).length > 0" class="results-section">
          <h3>分析结果</h3>
          
          <!-- 表格结果 -->
          <div v-if="analysisResults.length > 0" class="result-table">
            <table>
              <thead>
                <tr>
                  <th v-for="(_, key) in analysisResults[0]" :key="key">{{ key }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, i) in analysisResults" :key="i">
                  <td v-for="(value, key) in row" :key="key">{{ value }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- 单一对象结果 (如统计分析) -->
          <div v-else-if="Object.keys(analysisResult).length > 0" class="result-object">
            <div v-for="(value, key) in analysisResult" :key="key" class="result-item">
              <span class="result-key">{{ key }}:</span>
              <span class="result-value">{{ value }}</span>
            </div>
          </div>
          
          <!-- 结果可视化 -->
          <div v-if="showVisualization" class="visualization">
            <div class="chart-container">
              <!-- 图表渲染容器 -->
              <div ref="chartContainer" class="chart"></div>
            </div>
          </div>
          
          <!-- 导出按钮 -->
          <div class="export-options">
            <button @click="exportResults('csv')" class="export-btn">导出 CSV</button>
            <button @click="exportResults('json')" class="export-btn">导出 JSON</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from 'vue';
import HiveService from '../services/HiveService';

export default {
  name: 'HiveAnalytics',
  
  setup() {
    // 连接状态
    const connected = ref(false);
    const hiveUrl = ref('');
    
    // 数据库和表
    const databases = ref([]);
    const tables = ref([]);
    const currentDatabase = ref('');
    const currentTable = ref('');
    const tableSchema = ref([]);
    
    // 分析类型下拉菜单控制
    const showAnalyticsMenu = ref(false);
    
    // 分析类型
    const analysisTypes = [
      { id: 'aggregate', name: '聚合分析', icon: 'aggregate-icon', description: '对数据进行分组和聚合计算，例如求和、平均值、计数等' },
      { id: 'timeseries', name: '时间序列分析', icon: 'timeseries-icon', description: '分析数据随时间变化的趋势和模式' },
      { id: 'distribution', name: '列值分布', icon: 'distribution-icon', description: '分析列中值的分布情况和频率' },
      { id: 'statistics', name: '统计信息', icon: 'statistics-icon', description: '计算基本统计指标，如均值、中位数、标准差等' },
      { id: 'correlation', name: '相关性分析', icon: 'correlation-icon', description: '分析两列数值之间的相关性' },
      { id: 'histogram', name: '直方图', icon: 'histogram-icon', description: '将数值数据分成若干组并显示各组的频率分布' }
    ];
    const currentAnalysisType = ref('');
    
    // 加载状态
    const loading = ref(false);
    
    // 分析结果
    const analysisResults = ref([]);
    const analysisResult = ref({});
    const showVisualization = ref(false);
    
    // 图表容器引用
    const chartContainer = ref(null);
    
    // 聚合分析表单
    const aggregateForm = ref({
      aggregateColumn: '',
      aggregateFunction: 'COUNT',
      groupByColumn: '',
      whereClause: '',
      limit: 50
    });
    
    // 时间序列分析表单
    const timeSeriesForm = ref({
      timeColumn: '',
      valueColumn: '',
      interval: 'day',
      aggregateFunction: 'AVG',
      whereClause: '',
      limit: 100
    });
    
    // 列值分布分析表单
    const distributionForm = ref({
      columnName: '',
      limit: 50
    });
    
    // 统计信息分析表单
    const statisticsForm = ref({
      columnName: ''
    });
    
    // 相关性分析表单
    const correlationForm = ref({
      column1: '',
      column2: ''
    });
    
    // 直方图分析表单
    const histogramForm = ref({
      columnName: '',
      numBuckets: 10
    });
    
    // 计算属性：数值类型列
    const numericColumns = computed(() => {
      return tableSchema.value.filter(col => {
        const type = col.data_type ? col.data_type.toLowerCase() : '';
        return type.includes('int') || 
               type.includes('double') || 
               type.includes('float') || 
               type.includes('decimal') || 
               type.includes('numeric');
      });
    });
    
    // 计算属性：日期类型列
    const dateColumns = computed(() => {
      return tableSchema.value.filter(col => {
        const type = col.data_type ? col.data_type.toLowerCase() : '';
        return type.includes('date') || 
               type.includes('time') || 
               type.includes('timestamp');
      });
    });
    
    // 初始化
    onMounted(async () => {
      await refreshStatus();
      if (connected.value) {
        await refreshDatabases();
      }
    });
    
    // 当表改变时，重置分析类型和结果
    watch(currentTable, () => {
      currentAnalysisType.value = '';
      resetAnalysisResults();
    });
    
    // 当分析类型改变时，重置结果
    watch(currentAnalysisType, () => {
      resetAnalysisResults();
      initializeFormForAnalysisType();
    });
    
    // 初始化特定分析类型的表单
    const initializeFormForAnalysisType = () => {
      if (currentAnalysisType.value === 'aggregate') {
        if (numericColumns.value.length > 0) {
          aggregateForm.value.aggregateColumn = numericColumns.value[0].col_name;
        }
      } else if (currentAnalysisType.value === 'timeseries') {
        if (dateColumns.value.length > 0) {
          timeSeriesForm.value.timeColumn = dateColumns.value[0].col_name;
        }
        if (numericColumns.value.length > 0) {
          timeSeriesForm.value.valueColumn = numericColumns.value[0].col_name;
        }
      } else if (currentAnalysisType.value === 'distribution') {
        if (tableSchema.value.length > 0) {
          distributionForm.value.columnName = tableSchema.value[0].col_name;
        }
      } else if (currentAnalysisType.value === 'statistics') {
        if (numericColumns.value.length > 0) {
          statisticsForm.value.columnName = numericColumns.value[0].col_name;
        }
      } else if (currentAnalysisType.value === 'correlation') {
        if (numericColumns.value.length > 0) {
          correlationForm.value.column1 = numericColumns.value[0].col_name;
          if (numericColumns.value.length > 1) {
            correlationForm.value.column2 = numericColumns.value[1].col_name;
          } else {
            correlationForm.value.column2 = numericColumns.value[0].col_name;
          }
        }
      } else if (currentAnalysisType.value === 'histogram') {
        if (numericColumns.value.length > 0) {
          histogramForm.value.columnName = numericColumns.value[0].col_name;
        }
      }
    };
    
    // 刷新Hive连接状态
    const refreshStatus = async () => {
      try {
        const response = await HiveService.getStatus();
        connected.value = response.connected;
        hiveUrl.value = response.url || '';
        return connected.value;
      } catch (err) {
        console.error('获取Hive状态失败:', err);
        connected.value = false;
        hiveUrl.value = '';
        return false;
      }
    };
    
    // 刷新数据库列表
    const refreshDatabases = async () => {
      try {
        const response = await HiveService.getDatabases();
        databases.value = response.map(db => ({ name: db }));
        if (databases.value.length > 0 && !currentDatabase.value) {
          currentDatabase.value = databases.value[0].name;
          await loadTables();
        }
      } catch (err) {
        console.error('获取数据库列表失败:', err);
        databases.value = [];
      }
    };
    
    // 加载表列表
    const loadTables = async () => {
      if (!connected.value || !currentDatabase.value) {
        tables.value = [];
        return;
      }
      
      try {
        const response = await HiveService.getTables(currentDatabase.value);
        tables.value = response.map(table => ({ name: table }));
        if (tables.value.length > 0) {
          currentTable.value = tables.value[0].name;
          await loadTableSchema();
        }
      } catch (err) {
        console.error('获取表列表失败:', err);
        tables.value = [];
      }
    };
    
    // 加载表结构
    const loadTableSchema = async () => {
      if (!connected.value || !currentTable.value) {
        tableSchema.value = [];
        return;
      }
      
      try {
        const response = await HiveService.getTableSchema(currentTable.value, currentDatabase.value);
        tableSchema.value = response;
      } catch (err) {
        console.error('获取表结构失败:', err);
        tableSchema.value = [];
      }
    };
    
    // 分析类型下拉菜单开关
    const toggleAnalyticsMenu = () => {
      showAnalyticsMenu.value = !showAnalyticsMenu.value;
    };
    
    // 选择分析类型
    const selectAnalysisType = (typeId) => {
      currentAnalysisType.value = typeId;
      showAnalyticsMenu.value = false; // 选择后自动关闭下拉菜单
    };
    
    // 获取当前分析类型的标签
    const getAnalysisTypeLabel = () => {
      const type = analysisTypes.find(t => t.id === currentAnalysisType.value);
      return type ? type.name : '未选择分析类型';
    };
    
    // 获取当前分析类型的描述
    const getAnalysisDescription = () => {
      const type = analysisTypes.find(t => t.id === currentAnalysisType.value);
      return type ? type.description : '';
    };
    
    // 重置分析结果
    const resetAnalysisResults = () => {
      analysisResults.value = [];
      analysisResult.value = {};
      showVisualization.value = false;
    };
    
    // 执行聚合分析
    const executeAggregateAnalysis = async () => {
      if (!validateAnalysisForm('aggregate')) return;
      
      loading.value = true;
      resetAnalysisResults();
      
      try {
        const { aggregateColumn, aggregateFunction, groupByColumn, whereClause, limit } = aggregateForm.value;
        
        const results = await HiveService.executeAggregateAnalysis(
          currentTable.value,
          aggregateColumn,
          aggregateFunction,
          groupByColumn,
          whereClause,
          limit
        );
        
        analysisResults.value = results;
        
        // 如果有结果，准备可视化
        if (results.length > 0) {
          showVisualization.value = true;
          setTimeout(() => {
            renderChart('aggregate', results);
          }, 100);
        }
      } catch (err) {
        console.error('执行聚合分析失败:', err);
        alert('执行聚合分析失败: ' + (err.message || '未知错误'));
      } finally {
        loading.value = false;
      }
    };
    
    // 执行时间序列分析
    const executeTimeSeriesAnalysis = async () => {
      if (!validateAnalysisForm('timeseries')) return;
      
      loading.value = true;
      resetAnalysisResults();
      
      try {
        const { timeColumn, valueColumn, interval, aggregateFunction, whereClause, limit } = timeSeriesForm.value;
        
        const results = await HiveService.executeTimeSeriesAnalysis(
          currentTable.value,
          timeColumn,
          valueColumn,
          interval,
          aggregateFunction,
          whereClause,
          limit
        );
        
        analysisResults.value = results;
        
        // 如果有结果，准备可视化
        if (results.length > 0) {
          showVisualization.value = true;
          setTimeout(() => {
            renderChart('timeseries', results);
          }, 100);
        }
      } catch (err) {
        console.error('执行时间序列分析失败:', err);
        alert('执行时间序列分析失败: ' + (err.message || '未知错误'));
      } finally {
        loading.value = false;
      }
    };
    
    // 执行列值分布分析
    const executeDistributionAnalysis = async () => {
      if (!validateAnalysisForm('distribution')) return;
      
      loading.value = true;
      resetAnalysisResults();
      
      try {
        const { columnName, limit } = distributionForm.value;
        
        const results = await HiveService.analyzeColumnDistribution(
          currentTable.value,
          columnName,
          limit
        );
        
        analysisResults.value = results;
        
        // 如果有结果，准备可视化
        if (results.length > 0) {
          showVisualization.value = true;
          setTimeout(() => {
            renderChart('distribution', results);
          }, 100);
        }
      } catch (err) {
        console.error('执行列值分布分析失败:', err);
        alert('执行列值分布分析失败: ' + (err.message || '未知错误'));
      } finally {
        loading.value = false;
      }
    };
    
    // 执行统计信息分析
    const executeStatisticsAnalysis = async () => {
      if (!validateAnalysisForm('statistics')) return;
      
      loading.value = true;
      resetAnalysisResults();
      
      try {
        const { columnName } = statisticsForm.value;
        
        const result = await HiveService.calculateColumnStatistics(
          currentTable.value,
          columnName
        );
        
        analysisResult.value = result;
      } catch (err) {
        console.error('执行统计信息分析失败:', err);
        alert('执行统计信息分析失败: ' + (err.message || '未知错误'));
      } finally {
        loading.value = false;
      }
    };
    
    // 执行相关性分析
    const executeCorrelationAnalysis = async () => {
      if (!validateAnalysisForm('correlation')) return;
      
      loading.value = true;
      resetAnalysisResults();
      
      try {
        const { column1, column2 } = correlationForm.value;
        
        const result = await HiveService.calculateCorrelation(
          currentTable.value,
          column1,
          column2
        );
        
        analysisResult.value = result;
      } catch (err) {
        console.error('执行相关性分析失败:', err);
        alert('执行相关性分析失败: ' + (err.message || '未知错误'));
      } finally {
        loading.value = false;
      }
    };
    
    // 执行直方图分析
    const executeHistogramAnalysis = async () => {
      if (!validateAnalysisForm('histogram')) return;
      
      loading.value = true;
      resetAnalysisResults();
      
      try {
        const { columnName, numBuckets } = histogramForm.value;
        
        const results = await HiveService.generateHistogram(
          currentTable.value,
          columnName,
          numBuckets
        );
        
        analysisResults.value = results;
        
        // 如果有结果，准备可视化
        if (results.length > 0) {
          showVisualization.value = true;
          setTimeout(() => {
            renderChart('histogram', results);
          }, 100);
        }
      } catch (err) {
        console.error('执行直方图分析失败:', err);
        alert('执行直方图分析失败: ' + (err.message || '未知错误'));
      } finally {
        loading.value = false;
      }
    };
    
    // 验证分析表单
    const validateAnalysisForm = (type) => {
      if (!currentTable.value) {
        alert('请先选择一个表');
        return false;
      }
      
      switch (type) {
        case 'aggregate':
          if (!aggregateForm.value.aggregateColumn) {
            alert('请选择聚合列');
            return false;
          }
          break;
        case 'timeseries':
          if (!timeSeriesForm.value.timeColumn) {
            alert('请选择时间列');
            return false;
          }
          if (!timeSeriesForm.value.valueColumn) {
            alert('请选择值列');
            return false;
          }
          break;
        case 'distribution':
          if (!distributionForm.value.columnName) {
            alert('请选择目标列');
            return false;
          }
          break;
        case 'statistics':
          if (!statisticsForm.value.columnName) {
            alert('请选择目标列');
            return false;
          }
          break;
        case 'correlation':
          if (!correlationForm.value.column1) {
            alert('请选择列1');
            return false;
          }
          if (!correlationForm.value.column2) {
            alert('请选择列2');
            return false;
          }
          break;
        case 'histogram':
          if (!histogramForm.value.columnName) {
            alert('请选择目标列');
            return false;
          }
          break;
      }
      
      return true;
    };
    
    // 渲染图表 (基本实现)
    const renderChart = (type, data) => {
      if (!chartContainer.value) return;
      
      // 清空图表容器
      chartContainer.value.innerHTML = '';
      
      // 实际项目中可以使用Chart.js, Echarts等专业图表库
      const chartDiv = document.createElement('div');
      chartDiv.className = 'chart-placeholder';
      chartDiv.textContent = `已生成${getAnalysisTypeLabel()}图表 - 数据点: ${data.length}`;
      
      chartContainer.value.appendChild(chartDiv);
    };
    
    // 导出结果
    const exportResults = (format) => {
      if (analysisResults.value.length === 0 && Object.keys(analysisResult.value).length === 0) {
        alert('没有可导出的结果');
        return;
      }
      
      let data;
      let filename = `${currentTable.value}_${currentAnalysisType.value}_analysis`;
      
      if (format === 'csv') {
        if (analysisResults.value.length > 0) {
          // 表格结果转CSV
          const headers = Object.keys(analysisResults.value[0]);
          const csvContent = [
            headers.join(','),
            ...analysisResults.value.map(row => 
              headers.map(header => JSON.stringify(row[header] || '')).join(',')
            )
          ].join('\n');
          
          data = new Blob([csvContent], { type: 'text/csv' });
          filename += '.csv';
        } else {
          // 对象结果转CSV
          const csvContent = [
            'key,value',
            ...Object.entries(analysisResult.value).map(([key, value]) => 
              `${JSON.stringify(key)},${JSON.stringify(value)}`
            )
          ].join('\n');
          
          data = new Blob([csvContent], { type: 'text/csv' });
          filename += '.csv';
        }
      } else if (format === 'json') {
        // JSON格式
        const jsonContent = JSON.stringify(
          analysisResults.value.length > 0 ? analysisResults.value : analysisResult.value, 
          null, 2
        );
        
        data = new Blob([jsonContent], { type: 'application/json' });
        filename += '.json';
      }
      
      // 创建下载链接
      const url = URL.createObjectURL(data);
      const link = document.createElement('a');
      link.href = url;
      link.download = filename;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      URL.revokeObjectURL(url);
    };
    
    return {
      connected,
      hiveUrl,
      databases,
      tables,
      currentDatabase,
      currentTable,
      tableSchema,
      numericColumns,
      dateColumns,
      analysisTypes,
      currentAnalysisType,
      loading,
      analysisResults,
      analysisResult,
      showVisualization,
      chartContainer,
      
      // 表单
      aggregateForm,
      timeSeriesForm,
      distributionForm,
      statisticsForm,
      correlationForm,
      histogramForm,
      
      // 方法
      refreshStatus,
      refreshDatabases,
      loadTables,
      loadTableSchema,
      showAnalyticsMenu,
      toggleAnalyticsMenu,
      selectAnalysisType,
      getAnalysisTypeLabel,
      getAnalysisDescription,
      
      // 分析执行方法
      executeAggregateAnalysis,
      executeTimeSeriesAnalysis,
      executeDistributionAnalysis,
      executeStatisticsAnalysis,
      executeCorrelationAnalysis,
      executeHistogramAnalysis,
      
      // 导出方法
      exportResults,
      
      // 重置分析结果
      resetAnalysisResults
    };
  }
};
</script>

<style scoped>
.hive-analytics {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.status-bar {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 10px;
}

.connected {
  background-color: #42b983;
}

.disconnected {
  background-color: #f56c6c;
}

.status-text {
  font-size: 14px;
  color: #606266;
}

.refresh-btn {
  margin-left: auto;
  background-color: #409eff;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 5px 10px;
  cursor: pointer;
}

.refresh-btn:hover {
  background-color: #66b1ff;
}

.refresh-btn.small {
  font-size: 12px;
  padding: 3px 8px;
}

.database-selection {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.selection-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.selection-item label {
  font-weight: bold;
  color: #606266;
}

.selection-item select {
  padding: 5px 10px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  background-color: #fff;
}

.analytics-selection {
  margin-top: 20px;
}

.analytics-types-container {
  width: 100%;
  margin-bottom: 20px;
}

.analytics-dropdown {
  position: relative;
  width: 100%;
  margin-bottom: 10px;
}

.analytics-dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background-color: #f0f2f5;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
}

.analytics-dropdown-header:hover {
  background-color: #e6f7ff;
  border-color: #91d5ff;
}

.dropdown-icon {
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
}

.dropdown-icon.down {
  border-top: 6px solid #666;
  border-bottom: 0;
}

.dropdown-icon.up {
  border-bottom: 6px solid #666;
  border-top: 0;
}

.analytics-dropdown-content {
  position: absolute;
  top: 100%;
  left: 0;
  width: 100%;
  max-height: 300px;
  overflow-y: auto;
  background-color: white;
  border: 1px solid #d9d9d9;
  border-top: none;
  border-radius: 0 0 4px 4px;
  z-index: 10;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.analytics-dropdown-item {
  display: flex;
  align-items: center;
  padding: 10px 15px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.analytics-dropdown-item:hover {
  background-color: #f5f5f5;
}

.analytics-dropdown-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}

.analytics-icon {
  margin-right: 10px;
  font-size: 18px;
}

.selected-analysis-type {
  margin-top: 10px;
  padding: 10px;
  background-color: #f9f9f9;
  border-radius: 4px;
  border-left: 4px solid #1890ff;
}

.selected-type-header {
  display: flex;
  flex-direction: column;
}

.selected-type-header h4 {
  margin: 0 0 5px 0;
  font-size: 16px;
  color: #1890ff;
}

.analysis-description {
  font-size: 14px;
  color: #666;
  margin-bottom: 5px;
}

.empty-selection {
  margin-top: 40px;
  text-align: center;
  color: #909399;
  font-size: 16px;
}

/* Analytics Icons */
.aggregate-icon::before { content: '📊'; }
.timeseries-icon::before { content: '📈'; }
.distribution-icon::before { content: '📋'; }
.statistics-icon::before { content: '📉'; }
.correlation-icon::before { content: '🔄'; }
.histogram-icon::before { content: '📊'; }

/* 原有的分析类型网格样式隐藏 */
.analytics-types {
  display: none;
}

/* 新增样式 */
.analysis-section {
  margin-top: 30px;
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h3 {
  margin: 0;
  color: #303133;
}

.back-btn {
  background-color: #909399;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 5px 10px;
  cursor: pointer;
  font-size: 12px;
}

.back-btn:hover {
  background-color: #a6a9ad;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 30px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(64, 158, 255, 0.3);
  border-radius: 50%;
  border-top-color: #409eff;
  animation: spin 1s ease-in-out infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.config-form {
  background-color: #f5f7fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.form-group label {
  font-weight: bold;
  color: #606266;
  font-size: 14px;
}

.form-group select,
.form-group input {
  padding: 8px 10px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  background-color: #fff;
  font-size: 14px;
}

.execute-btn {
  background-color: #67c23a;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 10px 15px;
  cursor: pointer;
  font-weight: bold;
  align-self: flex-start;
  margin-top: 10px;
}

.execute-btn:hover {
  background-color: #85ce61;
}

.results-section {
  margin-top: 30px;
  border-top: 1px solid #ebeef5;
  padding-top: 20px;
}

.results-section h3 {
  margin-bottom: 15px;
  color: #303133;
}

.result-table {
  overflow-x: auto;
  margin-bottom: 20px;
}

.result-table table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid #ebeef5;
}

.result-table th,
.result-table td {
  padding: 12px 10px;
  text-align: left;
  border-bottom: 1px solid #ebeef5;
}

.result-table th {
  background-color: #f5f7fa;
  font-weight: bold;
  color: #606266;
  text-transform: uppercase;
  font-size: 12px;
}

.result-table tr:hover {
  background-color: #f5f7fa;
}

.result-object {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 15px;
  margin-bottom: 20px;
}

.result-item {
  background-color: #f5f7fa;
  padding: 15px;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
}

.result-key {
  font-weight: bold;
  color: #606266;
  margin-bottom: 5px;
  font-size: 14px;
}

.result-value {
  color: #303133;
  font-size: 18px;
}

.visualization {
  margin: 20px 0;
  padding: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.chart-container {
  width: 100%;
  height: 300px;
  overflow: hidden;
}

.chart {
  width: 100%;
  height: 100%;
}

.chart-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  border: 1px dashed #dcdfe6;
  color: #909399;
  font-size: 16px;
}

.export-options {
  display: flex;
  gap: 10px;
  margin-top: 20px;
}

.export-btn {
  background-color: #909399;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 5px 10px;
  cursor: pointer;
}

.export-btn:hover {
  background-color: #a6a9ad;
}

@media (max-width: 768px) {
  .database-selection {
    flex-direction: column;
    gap: 10px;
  }
  
  .result-object {
    grid-template-columns: 1fr;
  }
}
</style> 