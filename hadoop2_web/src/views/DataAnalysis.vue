<template>
    <div class="data-analysis">
      <h2>Hive 控制台</h2>
      
      <div class="tab-container">
        <div class="tabs">
          <div class="tab" :class="{ active: activeTab === 'console' }" @click="activeTab = 'console'">命令控制台</div>
          <div class="tab" :class="{ active: activeTab === 'tables' }" @click="activeTab = 'tables'">表管理</div>
          <div class="tab" :class="{ active: activeTab === 'schema' }" @click="activeTab = 'schema'">查看表结构</div>
        </div>
        
        <!-- 命令控制台 -->
        <div v-show="activeTab === 'console'" class="tab-content">
          <div class="card">
            <h3>Hive 命令</h3>
            
            <div class="form-group">
              <label for="hive-command">输入Hive命令</label>
              <textarea 
                id="hive-command" 
                class="form-control" 
                v-model="command" 
                rows="5" 
                placeholder="输入任何Hive命令，例如：SELECT * FROM crop_data LIMIT 10 或 CREATE TABLE new_table (id INT, name STRING)"
              ></textarea>
            </div>
            
            <div class="command-type">
              <label>命令类型：</label>
              <select v-model="commandType">
                <option value="QUERY">查询(QUERY)</option>
                <option value="UPDATE">更新(UPDATE)</option>
                <option value="DESCRIBE">表结构(DESCRIBE)</option>
                <option value="CREATE_TABLE">创建表(CREATE_TABLE)</option>
                <option value="DROP_TABLE">删除表(DROP_TABLE)</option>
              </select>
              
              <div v-if="commandType === 'DESCRIBE' || commandType === 'DROP_TABLE'" class="table-name-input">
                <label>表名：</label>
                <input type="text" v-model="tableName" placeholder="输入表名" />
              </div>
            </div>
            
            <button class="btn" @click="executeStandardCommand" :disabled="!isCommandValid || executing">
              {{ executing ? '执行中...' : '执行命令' }}
            </button>
            
            <div v-if="commandStatus" class="command-status" :class="{ success: commandSuccess, error: !commandSuccess }">
              {{ commandStatus }}
            </div>
            
            <div v-if="Array.isArray(commandResults) && commandResults.length > 0" class="results-container">
              <h4>查询结果</h4>
              <div class="table-responsive">
                <table class="results-table">
                  <thead>
                    <tr>
                      <th v-for="(_, key) in commandResults[0]" :key="key">{{ key }}</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(row, index) in commandResults" :key="index">
                      <td v-for="(value, key) in row" :key="key">{{ value }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
            
            <div v-else-if="!Array.isArray(commandResults) && commandResults && commandSuccess" class="message-container">
              <div class="command-message success">
                <p>{{ commandResults.message }}</p>
              </div>
            </div>
          </div>
          
          <div class="card">
            <h3>常用命令</h3>
            
            <div class="common-commands">
              <button class="command-btn" @click="runCommonCommand('SHOW DATABASES')">
                显示所有数据库
              </button>
              
              <button class="command-btn" @click="runCommonCommand('SHOW TABLES')">
                显示所有表
              </button>
              
              <button class="command-btn" @click="describeTable('crop_data')">
                查看crop_data表结构
              </button>
              
              <button class="command-btn" @click="createTestTable()">
                创建测试表
              </button>
              
              <button class="command-btn" @click="dropTable('test_table')">
                删除测试表
              </button>
            </div>
          </div>
        </div>
        
        <!-- 表管理 -->
        <div v-show="activeTab === 'tables'" class="tab-content">
          <div class="card">
            <h3>创建表</h3>
            
            <div class="form-group">
              <label for="table-name">表名</label>
              <input type="text" id="table-name" v-model="newTable.name" class="form-control" placeholder="输入表名">
            </div>
            
            <h4 class="mt-3">列定义</h4>
            <div v-for="(column, index) in newTable.columns" :key="index" class="column-definition">
              <div class="form-group">
                <label>列名</label>
                <input type="text" v-model="column.name" class="form-control" placeholder="列名">
              </div>
              <div class="form-group">
                <label>数据类型</label>
                <select v-model="column.type" class="form-control">
                  <option value="INT">INT</option>
                  <option value="BIGINT">BIGINT</option>
                  <option value="STRING">STRING</option>
                  <option value="DOUBLE">DOUBLE</option>
                  <option value="FLOAT">FLOAT</option>
                  <option value="BOOLEAN">BOOLEAN</option>
                  <option value="DATE">DATE</option>
                  <option value="TIMESTAMP">TIMESTAMP</option>
                </select>
              </div>
              <div class="form-group">
                <label>注释</label>
                <input type="text" v-model="column.comment" class="form-control" placeholder="列注释">
              </div>
              <button class="btn btn-danger" @click="removeColumn(index)">删除</button>
            </div>
            
            <div class="buttons-container">
              <button class="btn btn-secondary" @click="addColumn">添加列</button>
              <button class="btn btn-primary" @click="createTable" :disabled="!isTableValid || creating">
                {{ creating ? '创建中...' : '创建表' }}
              </button>
            </div>
            
            <div v-if="tableStatus" class="command-status" :class="{ success: tableSuccess, error: !tableSuccess }">
              {{ tableStatus }}
            </div>
          </div>
        </div>
        
        <!-- 查看表结构 -->
        <div v-show="activeTab === 'schema'" class="tab-content">
          <div class="card">
            <h3>查看表结构</h3>
            
            <div class="form-group">
              <label for="schema-table-name">表名</label>
              <input type="text" id="schema-table-name" v-model="schemaTableName" class="form-control" placeholder="输入表名">
              <button class="btn mt-2" @click="getTableSchema" :disabled="!schemaTableName || loadingSchema">
                {{ loadingSchema ? '加载中...' : '获取表结构' }}
              </button>
            </div>
            
            <div v-if="schemaStatus" class="command-status" :class="{ success: schemaSuccess, error: !schemaSuccess }">
              {{ schemaStatus }}
            </div>
            
            <div v-if="tableSchema && tableSchema.length > 0" class="schema-container">
              <h4>表 {{ schemaTableName }} 的结构</h4>
              <div class="table-responsive">
                <table class="results-table">
                  <thead>
                    <tr>
                      <th>列名</th>
                      <th>数据类型</th>
                      <th>注释</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="(column, index) in tableSchema" :key="index">
                      <td>{{ column.col_name }}</td>
                      <td>{{ column.data_type }}</td>
                      <td>{{ column.comment || '' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'HiveConsole',
    data() {
      return {
        activeTab: 'console',
        // 命令控制台
        command: '',
        commandType: 'QUERY',
        tableName: '',
        executing: false,
        commandStatus: '',
        commandSuccess: false,
        commandResults: null,
        
        // 表管理
        newTable: {
          name: '',
          columns: [
            { name: 'id', type: 'INT', comment: '唯一标识符' },
            { name: 'name', type: 'STRING', comment: '名称' }
          ]
        },
        creating: false,
        tableStatus: '',
        tableSuccess: false,
        
        // 查看表结构
        schemaTableName: '',
        tableSchema: null,
        loadingSchema: false,
        schemaStatus: '',
        schemaSuccess: false
      }
    },
    computed: {
      isCommandValid() {
        if (this.commandType === 'QUERY' || this.commandType === 'UPDATE') {
          return !!this.command;
        } else if (this.commandType === 'DESCRIBE' || this.commandType === 'DROP_TABLE') {
          return !!this.tableName;
        }
        return false;
      },
      isTableValid() {
        return this.newTable.name && this.newTable.columns.length > 0 &&
          this.newTable.columns.every(col => col.name && col.type);
      }
    },
    methods: {
      // 通用方法 - 设置命令
      setCommand(command) {
        this.command = command;
        // 自动判断命令类型
        if (command.toUpperCase().startsWith('SELECT') || command.toUpperCase().startsWith('SHOW')) {
          this.commandType = 'QUERY';
        } else if (command.toUpperCase().startsWith('DESCRIBE') || command.toUpperCase().startsWith('DESC')) {
          this.commandType = 'DESCRIBE';
          // 提取表名
          const parts = command.trim().split(/\s+/);
          if (parts.length > 1) {
            this.tableName = parts[1];
          }
        } else if (command.toUpperCase().startsWith('CREATE')) {
          this.commandType = 'CREATE_TABLE';
        } else if (command.toUpperCase().startsWith('DROP')) {
          this.commandType = 'DROP_TABLE';
          // 提取表名
          const parts = command.trim().split(/\s+/);
          if (parts.length > 2) {
            this.tableName = parts[2];
          }
        } else {
          this.commandType = 'UPDATE';
        }
      },
      
      // 执行标准化命令
      async executeStandardCommand() {
        if (!this.isCommandValid) return;
        
        this.executing = true;
        this.commandStatus = '正在执行命令...';
        this.commandResults = null;
        
        try {
          // 构建标准化命令对象
          const commandObj = {
            type: this.commandType
          };
          
          if (this.commandType === 'QUERY' || this.commandType === 'UPDATE') {
            commandObj.sql = this.command;
          } else if (this.commandType === 'DESCRIBE' || this.commandType === 'DROP_TABLE') {
            commandObj.tableName = this.tableName;
          }
          
          const response = await this.axios.post('/agriculture/hive/standard', commandObj);
          
          this.commandResults = response.data;
          this.commandSuccess = true;
          
          if (Array.isArray(this.commandResults)) {
            this.commandStatus = `命令执行成功，返回 ${this.commandResults.length} 条结果`;
          } else {
            this.commandStatus = '命令执行成功';
          }
        } catch (error) {
          this.commandSuccess = false;
          this.commandStatus = `命令执行失败: ${error.response?.data?.error || error.message}`;
        } finally {
          this.executing = false;
        }
      },
      
      // 执行常用命令
      runCommonCommand(command) {
        this.setCommand(command);
        this.executeStandardCommand();
      },
      
      // 查看表结构
      describeTable(tableName) {
        this.commandType = 'DESCRIBE';
        this.tableName = tableName;
        this.command = `DESCRIBE ${tableName}`;
        this.executeStandardCommand();
      },
      
      // 删除表
      dropTable(tableName) {
        this.commandType = 'DROP_TABLE';
        this.tableName = tableName;
        this.command = `DROP TABLE ${tableName}`;
        this.executeStandardCommand();
      },
      
      // 创建测试表
      createTestTable() {
        const columns = [
          { name: 'id', type: 'INT', comment: '唯一标识符' },
          { name: 'name', type: 'STRING', comment: '名称' },
          { name: 'value', type: 'DOUBLE', comment: '数值' }
        ];
        
        this.createTableWithColumns('test_table', columns);
      },
      
      // 使用标准化接口创建表
      async createTableWithColumns(tableName, columns) {
        this.executing = true;
        this.commandStatus = '正在创建表...';
        this.commandResults = null;
        
        try {
          const commandObj = {
            type: 'CREATE_TABLE',
            tableName: tableName,
            columns: columns
          };
          
          const response = await this.axios.post('/agriculture/hive/standard', commandObj);
          
          this.commandResults = response.data;
          this.commandSuccess = true;
          this.commandStatus = '表创建成功';
        } catch (error) {
          this.commandSuccess = false;
          this.commandStatus = `表创建失败: ${error.response?.data?.error || error.message}`;
        } finally {
          this.executing = false;
        }
      },
      
      // 新表管理 - 添加列
      addColumn() {
        this.newTable.columns.push({ name: '', type: 'STRING', comment: '' });
      },
      
      // 新表管理 - 删除列
      removeColumn(index) {
        this.newTable.columns.splice(index, 1);
      },
      
      // 新表管理 - 创建表
      async createTable() {
        if (!this.isTableValid) return;
        
        this.creating = true;
        this.tableStatus = '正在创建表...';
        
        try {
          const commandObj = {
            type: 'CREATE_TABLE',
            tableName: this.newTable.name,
            columns: this.newTable.columns
          };
          
          const response = await this.axios.post('/agriculture/hive/standard', commandObj);
          
          this.tableSuccess = true;
          this.tableStatus = `表 ${this.newTable.name} 创建成功`;
          
          // 重置表单，保留结构
          this.newTable.name = '';
          this.newTable.columns = [
            { name: 'id', type: 'INT', comment: '唯一标识符' },
            { name: 'name', type: 'STRING', comment: '名称' }
          ];
        } catch (error) {
          this.tableSuccess = false;
          this.tableStatus = `表创建失败: ${error.response?.data?.error || error.message}`;
        } finally {
          this.creating = false;
        }
      },
      
      // 查看表结构 - 获取表结构
      async getTableSchema() {
        if (!this.schemaTableName) return;
        
        this.loadingSchema = true;
        this.schemaStatus = '正在获取表结构...';
        this.tableSchema = null;
        
        try {
          const response = await this.axios.get(`/agriculture/hive/tables/${this.schemaTableName}/schema`);
          
          this.tableSchema = response.data;
          this.schemaSuccess = true;
          
          if (this.tableSchema.length > 0) {
            this.schemaStatus = `成功获取表 ${this.schemaTableName} 的结构`;
          } else {
            this.schemaStatus = '未找到表结构';
          }
        } catch (error) {
          this.schemaSuccess = false;
          this.schemaStatus = `获取表结构失败: ${error.response?.data?.error || error.message}`;
        } finally {
          this.loadingSchema = false;
        }
      }
    }
  }
  </script>
  
  <style scoped>
  .data-analysis h2 {
    margin-bottom: 1.5rem;
  }
  
  .tab-container {
    margin-top: 1rem;
  }
  
  .tabs {
    display: flex;
    border-bottom: 1px solid #ddd;
    margin-bottom: 1rem;
  }
  
  .tab {
    padding: 0.5rem 1rem;
    cursor: pointer;
    border: 1px solid transparent;
    border-bottom: none;
    border-radius: 4px 4px 0 0;
    margin-right: 0.5rem;
  }
  
  .tab.active {
    background-color: #f8f9fa;
    border-color: #ddd;
    border-bottom-color: #f8f9fa;
    margin-bottom: -1px;
  }
  
  .tab-content {
    padding: 1rem 0;
  }
  
  .command-status {
    margin-top: 1rem;
    padding: 0.5rem;
    border-radius: 4px;
  }
  
  .success {
    background-color: #d4edda;
    color: #155724;
  }
  
  .error {
    background-color: #f8d7da;
    color: #721c24;
  }
  
  .results-container, .message-container, .schema-container {
    margin-top: 1.5rem;
  }
  
  .results-container h4, .schema-container h4 {
    margin-bottom: 0.5rem;
  }
  
  .command-message {
    padding: 1rem;
    border-radius: 4px;
  }
  
  .table-responsive {
    overflow-x: auto;
  }
  
  .results-table {
    width: 100%;
    border-collapse: collapse;
  }
  
  .results-table th,
  .results-table td {
    padding: 0.5rem;
    text-align: left;
    border: 1px solid #ddd;
  }
  
  .results-table th {
    background-color: #f2f2f2;
    font-weight: bold;
  }
  
  .results-table tr:nth-child(even) {
    background-color: #f9f9f9;
  }
  
  .common-commands {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 1rem;
    margin-top: 1rem;
  }
  
  .command-btn {
    background-color: #f2f2f2;
    border: 1px solid #ddd;
    border-radius: 4px;
    padding: 0.5rem;
    cursor: pointer;
    transition: background-color 0.3s;
  }
  
  .command-btn:hover {
    background-color: #e0e0e0;
  }
  
  .command-type {
    margin: 1rem 0;
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }
  
  .command-type select {
    margin: 0 1rem;
    padding: 0.3rem;
    border-radius: 4px;
    border: 1px solid #ddd;
  }
  
  .table-name-input {
    margin-left: 1rem;
    display: flex;
    align-items: center;
  }
  
  .table-name-input input {
    margin-left: 0.5rem;
    padding: 0.3rem;
    border-radius: 4px;
    border: 1px solid #ddd;
  }
  
  .column-definition {
    display: grid;
    grid-template-columns: 2fr 2fr 3fr 1fr;
    gap: 0.5rem;
    margin-bottom: 1rem;
    padding: 0.5rem;
    background-color: #f8f9fa;
    border-radius: 4px;
    align-items: end;
  }
  
  .buttons-container {
    display: flex;
    gap: 1rem;
    margin-top: 1rem;
  }
  
  .btn {
    padding: 0.5rem 1rem;
    border: none;
    border-radius: 4px;
    cursor: pointer;
    background-color: #4CAF50;
    color: white;
  }
  
  .btn:disabled {
    background-color: #cccccc;
    cursor: not-allowed;
  }
  
  .btn-secondary {
    background-color: #6c757d;
  }
  
  .btn-danger {
    background-color: #dc3545;
  }
  
  .form-group {
    margin-bottom: 1rem;
  }
  
  .form-control {
    width: 100%;
    padding: 0.5rem;
    border: 1px solid #ddd;
    border-radius: 4px;
  }
  
  .mt-2 {
    margin-top: 0.5rem;
  }
  
  .mt-3 {
    margin-top: 1rem;
  }
  </style>