import ApiService from './api.service';

/**
 * Hive服务 - 提供与Hive数据库交互的功能
 */
class HiveService extends ApiService {
  constructor() {
    super('/hive');
  }
  
  /**
   * 获取Hive连接状态
   * @returns {Promise} 连接状态信息
   */
  getStatus() {
    return this.get('/status');
  }
  
  /**
   * 获取所有数据库列表
   * @returns {Promise} 数据库列表
   */
  getDatabases() {
    return this.get('/databases');
  }
  
  /**
   * 获取表列表
   * @param {string} database 可选，数据库名称
   * @returns {Promise} 表列表
   */
  getTables(database) {
    const params = {};
    if (database) {
      params.database = database;
    }
    return this.get('/tables', params);
  }
  
  /**
   * 执行查询
   * @param {string} sql SQL查询语句
   * @returns {Promise} 查询结果
   */
  executeQuery(sql) {
    return this.post('/query', { sql });
  }
  
  /**
   * 执行更新操作（DDL语句）
   * @param {string} sql SQL更新语句
   * @returns {Promise} 更新结果
   */
  executeUpdate(sql) {
    return this.post('/update', { sql });
  }
  
  /**
   * 获取表结构
   * @param {string} table 表名
   * @param {string} database 可选，数据库名称
   * @returns {Promise} 表结构信息
   */
  getTableSchema(table, database) {
    const params = { table };
    if (database) {
      params.database = database;
    }
    return this.get('/schema', params);
  }
  
  /**
   * 创建表
   * @param {object} tableDefinition 表定义对象
   * @param {string} tableDefinition.name 表名
   * @param {Array} tableDefinition.columns 列定义数组
   * @param {string} tableDefinition.comment 表注释
   * @param {string} tableDefinition.fileFormat 文件格式
   * @returns {Promise} 创建结果
   */
  createTable(tableDefinition) {
    return this.post('/table', tableDefinition);
  }
  
  /**
   * 删除表
   * @param {string} tableName 表名
   * @returns {Promise} 删除结果
   */
  dropTable(tableName) {
    return this.delete('/table', { name: tableName });
  }
  
  /**
   * 格式化SQL查询
   * @param {string} sql 原始SQL
   * @returns {string} 格式化后的SQL
   */
  formatSQL(sql) {
    // 简单的SQL格式化
    return sql
      .replace(/\s+/g, ' ')
      .replace(/\s*([(),])\s*/g, '$1 ')
      .replace(/\s+AND\s+/gi, '\nAND ')
      .replace(/\s+OR\s+/gi, '\nOR ')
      .replace(/\s+FROM\s+/gi, '\nFROM ')
      .replace(/\s+WHERE\s+/gi, '\nWHERE ')
      .replace(/\s+GROUP\s+BY\s+/gi, '\nGROUP BY ')
      .replace(/\s+HAVING\s+/gi, '\nHAVING ')
      .replace(/\s+ORDER\s+BY\s+/gi, '\nORDER BY ')
      .replace(/\s+LIMIT\s+/gi, '\nLIMIT ')
      .replace(/\s+UNION\s+/gi, '\nUNION\n')
      .replace(/\s+ON\s+/gi, '\n  ON ')
      .replace(/\s+USING\s+/gi, '\n  USING ')
      .replace(/\s+INNER\s+JOIN\s+/gi, '\nINNER JOIN ')
      .replace(/\s+LEFT\s+JOIN\s+/gi, '\nLEFT JOIN ')
      .replace(/\s+RIGHT\s+JOIN\s+/gi, '\nRIGHT JOIN ')
      .replace(/\s+FULL\s+JOIN\s+/gi, '\nFULL JOIN ')
      .replace(/\s+CROSS\s+JOIN\s+/gi, '\nCROSS JOIN ')
      .replace(/\s+JOIN\s+/gi, '\nJOIN ')
      .trim();
  }
  
  /**
   * 检查SQL语法
   * @param {string} sql SQL语句
   * @returns {Object} 检查结果 {valid: boolean, message: string}
   */
  validateSQL(sql) {
    // 简单的SQL语法检查
    const result = { valid: true, message: '语法正确' };
    
    // SELECT语句检查
    if (/^SELECT\s+.*\s+FROM\s+/i.test(sql)) {
      // 检查是否有未闭合的括号
      const openParens = (sql.match(/\(/g) || []).length;
      const closeParens = (sql.match(/\)/g) || []).length;
      
      if (openParens !== closeParens) {
        result.valid = false;
        result.message = '括号不匹配';
      }
    } 
    // 其他语句类型检查
    else if (/^(CREATE|ALTER|DROP|INSERT|UPDATE|DELETE)\s+/i.test(sql)) {
      // 简单检查语法
      if (sql.trim().endsWith(';')) {
        // 移除分号再检查
        sql = sql.trim().slice(0, -1);
      }
      
      // CREATE TABLE检查
      if (/^CREATE\s+TABLE\s+\w+\s*\(/i.test(sql)) {
        const openParens = (sql.match(/\(/g) || []).length;
        const closeParens = (sql.match(/\)/g) || []).length;
        
        if (openParens !== closeParens) {
          result.valid = false;
          result.message = '创建表语句中括号不匹配';
        }
      }
    } else {
      result.valid = false;
      result.message = '不支持的SQL语句类型';
    }
    
    return result;
  }
  
  /**
   * 执行聚合分析
   * @param {string} tableName 表名
   * @param {string} aggregateColumn 聚合列
   * @param {string} aggregateFunction 聚合函数
   * @param {string} groupByColumn 分组列
   * @param {string} whereClause WHERE条件
   * @param {number} limit 限制数
   * @returns {Promise} 分析结果
   */
  executeAggregateAnalysis(tableName, aggregateColumn, aggregateFunction, groupByColumn, whereClause, limit) {
    return this.post('/analyze/aggregate', {
      tableName,
      aggregateColumn,
      aggregateFunction,
      groupByColumn,
      whereClause,
      limit
    });
  }
  
  /**
   * 执行时间序列分析
   * @param {string} tableName 表名
   * @param {string} timeColumn 时间列
   * @param {string} valueColumn 值列
   * @param {string} interval 时间间隔
   * @param {string} aggregateFunction 聚合函数
   * @param {string} whereClause WHERE条件
   * @param {number} limit 限制数
   * @returns {Promise} 分析结果
   */
  executeTimeSeriesAnalysis(tableName, timeColumn, valueColumn, interval, aggregateFunction, whereClause, limit) {
    return this.post('/analyze/timeseries', {
      tableName,
      timeColumn,
      valueColumn,
      interval,
      aggregateFunction,
      whereClause,
      limit
    });
  }
  
  /**
   * 分析列值分布
   * @param {string} tableName 表名
   * @param {string} columnName 列名
   * @param {number} limit 限制数
   * @returns {Promise} 分析结果
   */
  analyzeColumnDistribution(tableName, columnName, limit) {
    return this.post('/analyze/distribution', {
      tableName,
      columnName,
      limit
    });
  }
  
  /**
   * 计算列统计信息
   * @param {string} tableName 表名
   * @param {string} columnName 列名
   * @returns {Promise} 统计结果
   */
  calculateColumnStatistics(tableName, columnName) {
    return this.post('/analyze/statistics', {
      tableName,
      columnName
    });
  }
  
  /**
   * 计算相关性
   * @param {string} tableName 表名
   * @param {string} column1 列1
   * @param {string} column2 列2
   * @returns {Promise} 相关性结果
   */
  calculateCorrelation(tableName, column1, column2) {
    return this.post('/analyze/correlation', {
      tableName,
      column1,
      column2
    });
  }
  
  /**
   * 生成直方图数据
   * @param {string} tableName 表名
   * @param {string} columnName 列名
   * @param {number} numBuckets 桶数
   * @returns {Promise} 直方图数据
   */
  generateHistogram(tableName, columnName, numBuckets = 10) {
    return this.post('/analyze/histogram', {
      tableName,
      columnName,
      numBuckets
    });
  }
  
  /**
   * 执行透视分析
   * @param {string} tableName 表名
   * @param {string} rowDimension 行维度
   * @param {string} colDimension 列维度
   * @param {string} aggregateColumn 聚合列
   * @param {string} aggregateFunction 聚合函数
   * @param {number} limit 限制数
   * @returns {Promise} 透视表数据
   */
  executePivotAnalysis(tableName, rowDimension, colDimension, aggregateColumn, aggregateFunction, limit) {
    return this.post('/analyze/pivot', {
      tableName,
      rowDimension,
      colDimension,
      aggregateColumn,
      aggregateFunction,
      limit
    });
  }
}

// 导出单例
export default new HiveService(); 