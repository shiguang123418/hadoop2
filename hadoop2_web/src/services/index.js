/**
 * 服务模块索引文件 - 统一导出所有服务
 * 
 * 为避免循环依赖问题，使用异步导入
 * 这个文件只是为了兼容性而存在，推荐直接从各个服务文件导入服务实例
 */

// 基础服务
import ApiService from './api.service';

// 为了向后兼容直接导入服务
import _AuthService from './auth';
import _HDFSService from './HDFSService';
import _HiveService from './HiveService';
import _SparkService from './SparkService';
import _KafkaService from './KafkaService';

// 惰性加载其他服务
let services = {
  api: ApiService,
  auth: _AuthService,
  hdfs: _HDFSService,
  hive: _HiveService,
  spark: _SparkService,
  kafka: _KafkaService
};

// 获取服务的函数
async function getService(name) {
  if (!services[name]) {
    console.log(`延迟加载服务: ${name}`);
    try {
      switch (name) {
        case 'auth':
          const { default: authService } = await import('./auth');
          services.auth = authService;
          break;
        case 'hdfs':
          const { default: hdfsService } = await import('./HDFSService');
          services.hdfs = hdfsService;
          break;
        case 'hive':
          const { default: hiveService } = await import('./HiveService');
          services.hive = hiveService;
          break;
        case 'spark':
          const { default: sparkService } = await import('./SparkService');
          services.spark = sparkService;
          break;
        case 'kafka':
          const { default: kafkaService } = await import('./KafkaService');
          services.kafka = kafkaService;
          break;
        case 'sensor':
          const { default: sensorService } = await import('../api/sensor');
          services.sensor = sensorService;
          break;
        default:
          console.warn(`未知的服务: ${name}`);
          return null;
      }
      console.log(`服务 ${name} 已加载`);
      return services[name];
    } catch (error) {
      console.error(`加载服务 ${name} 时出错:`, error);
      return null;
    }
  }
  return services[name];
}

// 提供与原来相同的导出方式，但采用惰性加载
export {
  ApiService,
  _AuthService as AuthService,
  _HDFSService as HDFSService,
  _HiveService as HiveService,
  _SparkService as SparkService,
  _KafkaService as KafkaService
};

// 为保证向后兼容，创建代理对象
const servicesProxy = new Proxy({}, {
  get: function(target, prop) {
    if (prop in services) {
      return services[prop];
    }
    
    console.log(`通过代理请求服务: ${prop}`);
    // 返回一个Promise，以便异步加载
    return new Promise((resolve, reject) => {
      getService(prop).then(service => {
        if (service) resolve(service);
        else reject(new Error(`无法加载服务: ${prop}`));
      });
    });
  }
});

export default servicesProxy; 