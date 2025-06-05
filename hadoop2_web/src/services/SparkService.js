import LazyApiService from './LazyApiService';

/**
 * Spark服务 - 提供与Spark集群交互的功能
 */
class SparkServiceClass extends LazyApiService {
  constructor() {
    // 使用服务名称
    super('spark');
    // console.log('Spark服务初始化');
  }
  
  /**
   * 获取Spark连接状态
   * @returns {Promise} 连接状态信息
   */
  async getStatus() {
    await this.ensureInitialized();
    return this.get('/status');
  }
  
  /**
   * 获取运行中的Spark应用列表
   * @returns {Promise} 应用列表
   */
  async getApplications() {
    await this.ensureInitialized();
    return this.get('/applications');
  }
  
  /**
   * 获取特定应用的详细信息
   * @param {string} appId 应用ID
   * @returns {Promise} 应用详情
   */
  async getApplicationInfo(appId) {
    await this.ensureInitialized();
    return this.get(`/applications/${appId}`);
  }
  
  /**
   * 获取应用的执行器信息
   * @param {string} appId 应用ID
   * @returns {Promise} 执行器列表
   */
  async getApplicationExecutors(appId) {
    await this.ensureInitialized();
    return this.get(`/applications/${appId}/executors`);
  }
  
  /**
   * 获取应用的作业信息
   * @param {string} appId 应用ID
   * @returns {Promise} 作业列表
   */
  async getApplicationJobs(appId) {
    await this.ensureInitialized();
    return this.get(`/applications/${appId}/jobs`);
  }
  
  /**
   * 获取Spark环境信息
   * @returns {Promise} 环境信息
   */
  async getEnvironment() {
    await this.ensureInitialized();
    return this.get('/environment');
  }
  
  /**
   * 提交Spark作业
   * @param {Object} jobConfig 作业配置
   * @returns {Promise} 提交结果
   */
  async submitJob(jobConfig) {
    await this.ensureInitialized();
    return this.post('/jobs/submit', jobConfig);
  }
  
  /**
   * 取消正在运行的作业
   * @param {string} jobId 作业ID
   * @returns {Promise} 取消结果
   */
  async cancelJob(jobId) {
    await this.ensureInitialized();
    return this.post(`/jobs/${jobId}/cancel`);
  }
}

// 创建单例
const SparkService = new SparkServiceClass();

// 导出服务单例
export default SparkService;

// 命名导出，用于从index.js中导入
export { SparkService }; 