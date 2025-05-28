/**
 * 传感器工具函数
 */

/**
 * 获取传感器类型显示名
 * @param {string} type 传感器类型
 * @returns {string} 显示名称
 */
export const getSensorTypeDisplay = (type) => {
  switch (type) {
    case 'temperature': return '温度'
    case 'humidity': return '湿度'
    case 'soilMoisture': return '土壤湿度'
    case 'light': return '光照强度'
    case 'co2': return 'CO₂浓度'
    default: return type
  }
}

/**
 * 获取传感器单位
 * @param {string} type 传感器类型
 * @returns {string} 单位
 */
export const getSensorUnit = (type) => {
  switch (type) {
    case 'temperature': return '°C'
    case 'humidity': return '%'
    case 'soilMoisture': return '%'
    case 'light': return 'lux'
    case 'co2': return 'ppm'
    default: return ''
  }
}

/**
 * 获取趋势图标
 * @param {string} trend 趋势（rising|falling|stable）
 * @returns {string} 图标
 */
export const getTrendIcon = (trend) => {
  switch (trend) {
    case 'rising': return '↑'
    case 'falling': return '↓'
    case 'stable': return '→'
    default: return ''
  }
}

/**
 * 获取趋势样式类
 * @param {string} trend 趋势
 * @returns {string} 样式类
 */
export const getTrendClass = (trend) => {
  switch (trend) {
    case 'rising': return 'trend-up'
    case 'falling': return 'trend-down'
    case 'stable': return 'trend-stable'
    default: return ''
  }
}

/**
 * 获取图表颜色
 * @param {string} type 传感器类型
 * @param {number} alpha 透明度
 * @returns {string} 颜色
 */
export const getChartColor = (type, alpha = 1) => {
  let color
  switch (type) {
    case 'temperature': color = 'rgb(255, 99, 71)'; break; // 红色
    case 'humidity': color = 'rgb(0, 123, 255)'; break; // 蓝色
    case 'soilMoisture': color = 'rgb(111, 66, 193)'; break; // 紫色
    case 'light': color = 'rgb(255, 193, 7)'; break; // 黄色
    case 'co2': color = 'rgb(32, 201, 151)'; break; // 青绿色
    default: color = 'rgb(108, 117, 125)'; // 灰色
  }
  
  if (alpha < 1) {
    return color.replace('rgb', 'rgba').replace(')', `, ${alpha})`)
  }
  
  return color
}

/**
 * 计算趋势
 * @param {Array<number>} values 数值数组
 * @returns {string} 趋势（rising|falling|stable）
 */
export const calculateTrend = (values) => {
  if (!values || values.length < 3) return 'stable'
  
  const last = values[values.length - 1]
  const secondLast = values[values.length - 2]
  const thirdLast = values[values.length - 3]
  
  if (last > secondLast && secondLast > thirdLast) {
    return 'rising'
  } else if (last < secondLast && secondLast < thirdLast) {
    return 'falling'
  } else {
    return 'stable'
  }
} 