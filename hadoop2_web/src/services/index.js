/**
 * 服务模块索引文件 - 统一导出所有服务
 */

// 基础服务
import ApiService from './api.service';

// 身份验证服务
import AuthService, { AuthService as AuthServiceNamed } from './auth';

// 大数据服务
import HDFSService, { HDFSService as HDFSServiceNamed } from './HDFSService';
import HiveService, { HiveService as HiveServiceNamed } from './HiveService';
import SparkService, { SparkService as SparkServiceNamed } from './SparkService';
import KafkaService, { KafkaService as KafkaServiceNamed } from './KafkaService';

// 传感器和实时数据服务
import SensorApi, { SensorApi as SensorApiNamed } from '../api/sensor';

// 命名导出所有服务
export {
  ApiService,
  AuthServiceNamed as AuthService,
  HDFSServiceNamed as HDFSService,
  HiveServiceNamed as HiveService,
  SparkServiceNamed as SparkService,
  KafkaServiceNamed as KafkaService,
  SensorApiNamed as SensorApi
};

// 导出服务单例
export default {
  api: ApiService,
  auth: AuthService,
  hdfs: HDFSService,
  hive: HiveService,
  spark: SparkService,
  kafka: KafkaService,
  sensor: SensorApi
}; 