/**
 * 服务模块索引文件 - 统一导出所有服务
 */

// 基础服务
import ApiService from './api.service';

// 身份验证服务
import AuthService from './auth';

// 大数据服务
import HDFSService from './HDFSService';
import HiveService from './HiveService';
import SparkService from './SparkService';
import KafkaService from './KafkaService';

// 导出所有服务
export {
  ApiService,
  AuthService,
  HDFSService,
  HiveService,
  SparkService,
  KafkaService
};

// 导出服务单例
export default {
  api: ApiService,
  auth: AuthService,
  hdfs: HDFSService,
  hive: HiveService,
  spark: SparkService,
  kafka: KafkaService
}; 