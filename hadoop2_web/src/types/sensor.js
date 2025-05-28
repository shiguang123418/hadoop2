/**
 * 传感器数据类型定义
 * 
 * 注意：这是JSDoc风格的类型定义，不是TypeScript
 * 用于提供代码提示和文档
 */

/**
 * 传感器数据
 * @typedef {Object} SensorData
 * @property {string} sensorId - 传感器ID
 * @property {string} sensorType - 传感器类型
 * @property {number} value - 传感器值
 * @property {string} unit - 单位
 * @property {string} readableTime - 可读时间
 * @property {number} timestamp - 时间戳
 * @property {boolean} isAnomaly - 是否异常
 * @property {number} [movingAverage] - 移动平均值
 * @property {string} location - 位置
 * @property {Object} [stats] - 统计信息
 * @property {string} [stats.trend] - 趋势
 */

/**
 * 传感器状态对象
 * @typedef {Object} SensorState
 * @property {string} type - 传感器类型
 * @property {Array<number>} values - 历史值
 * @property {Array<string>} timestamps - 历史时间
 * @property {number} lastValue - 最新值
 * @property {string} lastUnit - 单位
 * @property {string} lastTime - 最新时间
 * @property {boolean} isAnomaly - 是否异常
 * @property {number} [movingAverage] - 移动平均值
 * @property {string} trend - 趋势 (rising|falling|stable)
 * @property {string} location - 位置
 */

/**
 * Spark统计数据
 * @typedef {Object} SparkStats
 * @property {number} min - 最小值
 * @property {number} max - 最大值
 * @property {number} avg - 平均值
 * @property {number} stdDev - 标准差
 * @property {number} count - 样本数
 * @property {number} anomalyCount - 异常数
 * @property {number} anomalyRate - 异常率
 */

/**
 * 消息对象
 * @typedef {Object} Message
 * @property {string} type - 消息类型 (system|error|received)
 * @property {string} content - 消息内容
 * @property {string} time - 时间
 */

/**
 * 主题
 * @typedef {Object} Topic
 * @property {string} label - 显示名称
 * @property {string} value - 主题路径
 */ 