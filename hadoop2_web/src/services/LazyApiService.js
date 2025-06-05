/**
 * LazyApiService - 延迟加载ApiService的基类
 * 用于解决循环依赖问题，所有服务可以继承此类而非直接继承ApiService
 */
class LazyApiService {
  /**
   * 创建延迟加载服务实例
   * @param {string} serviceName 服务名称
   */
  constructor(serviceName) {
    this.serviceName = serviceName;
    this.initialized = false;
    
    // 延迟初始化
    setTimeout(() => {
      this.initialize();
    }, 0);
    
    // console.log(`${serviceName}服务开始初始化`);
  }
  
  /**
   * 初始化服务，动态导入ApiService和serviceHelper
   */
  async initialize() {
    try {
      // 动态导入modules
      const [helperModule, ApiModule] = await Promise.all([
        import('../utils/service-helper'),
        import('./api.service')
      ]);
      
      const serviceHelper = helperModule.default;
      const ApiService = ApiModule.default;
      
      // 获取服务配置
      const serviceConfig = serviceHelper.getServiceConfig(this.serviceName);
      this.servicePath = serviceConfig.path;
      this.serverPrefix = serviceConfig.server;
      
      // 创建API实例或直接使用prototype
      const apiInstance = new ApiService(this.serviceName);
      this.api = apiInstance.api;
      
      // 设置API方法
      this.get = apiInstance.get.bind(this);
      this.post = apiInstance.post.bind(this);
      this.put = apiInstance.put.bind(this);
      this.delete = apiInstance.delete.bind(this);
      this.buildUrl = apiInstance.buildUrl.bind(this);
      
      this.initialized = true;
      // console.log(`${this.serviceName}服务完全初始化完成`);
    } catch (error) {
      // console.error(`${this.serviceName}服务初始化失败:`, error);
    }
  }
  
  /**
   * 确保服务已初始化
   * @returns {Promise} 初始化完成的Promise
   */
  async ensureInitialized() {
    if (!this.initialized) {
      // console.log(`${this.serviceName}服务尚未完全初始化，等待初始化完成...`);
      await new Promise(resolve => {
        const checkInterval = setInterval(() => {
          if (this.initialized) {
            clearInterval(checkInterval);
            resolve();
          }
        }, 100);
      });
      // console.log(`${this.serviceName}服务初始化完成，继续操作`);
    }
  }
}

export default LazyApiService; 