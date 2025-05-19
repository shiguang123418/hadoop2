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

// 导出所有服务
export {
  ApiService,
  AuthService,
  HDFSService,
  HiveService
};

// 导出服务单例
export default {
  api: ApiService,
  auth: AuthService,
  hdfs: HDFSService,
  hive: HiveService
}; 