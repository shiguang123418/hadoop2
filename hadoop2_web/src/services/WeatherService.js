/**
 * 天气数据服务 - 用于获取天气数据并进行可视化
 */

class WeatherService {
  constructor() {
    // API基础URL
    this.apiBaseUrl = '/api';
  }

  /**
   * 获取天气数据
   * @param {string} city 城市名称
   * @param {string} metric 指标类型（maxtemp/mintemp/rainfall）
   * @returns {Promise} 包含months和values的数据对象
   */
  async getWeatherData(city, metric) {
    try {
      const url = `${this.apiBaseUrl}/weather?city=${encodeURIComponent(city)}&metric=${encodeURIComponent(metric)}`;
      console.log(`[WeatherService] 请求天气数据: ${url}`);
      
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`HTTP错误 ${response.status}`);
      }
      
      const data = await response.json();
      console.log('[WeatherService] 获取到天气数据:', data);
      return data;
    } catch (error) {
      console.error('[WeatherService] 获取天气数据失败:', error);
      throw error;
    }
  }

  /**
   * 获取城市的所有气象指标数据
   * @param {string} city 城市名称
   * @returns {Promise} 包含所有指标数据的对象
   */
  async getAllWeatherData(city) {
    try {
      const url = `${this.apiBaseUrl}/weather/all?city=${encodeURIComponent(city)}`;
      console.log(`[WeatherService] 请求所有天气数据: ${url}`);
      
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`HTTP错误 ${response.status}`);
      }
      
      const data = await response.json();
      console.log('[WeatherService] 获取到所有天气数据:', data);
      return data;
    } catch (error) {
      console.error('[WeatherService] 获取所有天气数据失败:', error);
      throw error;
    }
  }

  /**
   * 获取可用城市列表
   * @returns {Promise} 城市列表
   */
  async getCities() {
    try {
      const url = `${this.apiBaseUrl}/weather/cities`;
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error(`HTTP错误 ${response.status}`);
      }
      
      const cities = await response.json();
      return cities;
    } catch (error) {
      console.error('[WeatherService] 获取城市列表失败:', error);
      // 返回默认城市列表，实际应用中应由后端提供
      return [
        { city: 'Bangalore', state: 'Karnataka' },
        { city: 'Bareilly', state: 'Uttar Pradesh' }
      ];
    }
  }
}

// 导出单例实例
const weatherServiceInstance = new WeatherService();
export default weatherServiceInstance; 