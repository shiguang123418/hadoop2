/**
 * 服务状态API - 提供统一的服务状态查询和管理接口
 */
import ApiService from '../services/api.service';
import axios from 'axios';
import apiConfig from '../config/api.config';

// 服务状态API服务类
class ServiceStatusApi extends ApiService {
  constructor() {
    super('/services');
    
    // 在开发环境中模拟API响应
    this.mockResponses = {
      // 获取所有服务状态的模拟响应
      getAllStatus: {
        services: [
          {
            id: 1,
            name: 'HDFS',
            status: 'running',
            url: 'hdfs://shiguang:9000',
            health: 98,
            lastChecked: new Date().toISOString(),
            details: {
              version: '3.3.1',
              mode: 'HA',
              totalSpace: '10.5TB',
              usedSpace: '4.2TB',
              availableSpace: '6.3TB',
              dataNodes: 5,
              liveDataNodes: 5
            }
          },
          {
            id: 2,
            name: 'Hive',
            status: 'running',
            url: 'jdbc:hive2://localhost:10000',
            health: 92,
            lastChecked: new Date().toISOString(),
            details: {
              version: '3.1.2',
              metastoreStatus: 'connected',
              warehouseDir: '/user/hive/warehouse',
              totalTables: 156,
              totalDatabases: 8
            }
          },
          {
            id: 3,
            name: 'Spark',
            status: 'running',
            url: 'local[*]',
            health: 95,
            lastChecked: new Date().toISOString(),
            details: {
              version: '3.2.0',
              mode: 'YARN',
              availableExecutors: 12,
              totalApplications: 8,
              runningApplications: 3
            }
          },
          {
            id: 4,
            name: 'Kafka',
            status: 'running',
            url: 'shiguang:9092',
            health: 90,
            lastChecked: new Date().toISOString(),
            details: {
              version: '2.8.0',
              brokers: 3,
              topics: 24,
              partitions: 96,
              controllers: 1
            }
          }
        ]
      },
      
      // 获取单个服务状态的模拟响应
      getStatus: {
        HDFS: {
          id: 1,
          name: 'HDFS',
          status: 'running',
          url: 'hdfs://shiguang:9000',
          health: 98,
          lastChecked: new Date().toISOString(),
          details: {
            version: '3.3.1',
            mode: 'HA',
            totalSpace: '10.5TB',
            usedSpace: '4.2TB',
            availableSpace: '6.3TB',
            dataNodes: 5,
            liveDataNodes: 5
          }
        },
        Hive: {
          id: 2,
          name: 'Hive',
          status: 'running',
          url: 'jdbc:hive2://localhost:10000',
          health: 92,
          lastChecked: new Date().toISOString(),
          details: {
            version: '3.1.2',
            metastoreStatus: 'connected',
            warehouseDir: '/user/hive/warehouse',
            totalTables: 156,
            totalDatabases: 8
          }
        },
        Spark: {
          id: 3,
          name: 'Spark',
          status: 'running',
          url: 'local[*]',
          health: 95,
          lastChecked: new Date().toISOString(),
          details: {
            version: '3.2.0',
            mode: 'YARN',
            availableExecutors: 12,
            totalApplications: 8,
            runningApplications: 3
          }
        },
        Kafka: {
          id: 4,
          name: 'Kafka',
          status: 'running',
          url: 'shiguang:9092',
          health: 90,
          lastChecked: new Date().toISOString(),
          details: {
            version: '2.8.0',
            brokers: 3,
            topics: 24,
            partitions: 96,
            controllers: 1
          }
        }
      }
    };
  }
  
  /**
   * 获取所有服务状态
   * @returns {Promise} 所有服务的状态信息
   */
  getAllStatus() {
    if (process.env.NODE_ENV === 'development') {
      // 开发环境返回模拟数据
      return new Promise(resolve => {
        setTimeout(() => {
          resolve(this.mockResponses.getAllStatus);
        }, 500);  // 模拟网络延迟
      });
    }
    
    // 生产环境使用实际API
    return this.get('/status/all');
  }
  
  /**
   * 获取指定服务的状态
   * @param {string} serviceName 服务名称（HDFS、Hive、Spark、Kafka等）
   * @returns {Promise} 服务状态信息
   */
  getServiceStatus(serviceName) {
    if (process.env.NODE_ENV === 'development') {
      // 开发环境返回模拟数据
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          if (this.mockResponses.getStatus[serviceName]) {
            // 随机模拟偶发故障
            if (Math.random() > 0.9) {
              this.mockResponses.getStatus[serviceName].status = 'stopped';
              this.mockResponses.getStatus[serviceName].health = 0;
            } else {
              this.mockResponses.getStatus[serviceName].status = 'running';
              this.mockResponses.getStatus[serviceName].health = 
                70 + Math.floor(Math.random() * 30);
            }
            
            // 更新检查时间
            this.mockResponses.getStatus[serviceName].lastChecked = 
              new Date().toISOString();
              
            resolve(this.mockResponses.getStatus[serviceName]);
          } else {
            reject(new Error(`未知服务: ${serviceName}`));
          }
        }, 300 + Math.random() * 500);  // 模拟网络延迟
      });
    }
    
    // 生产环境使用实际API
    return this.get(`/status/${serviceName}`);
  }
  
  /**
   * 启动服务
   * @param {string} serviceName 服务名称
   * @returns {Promise} 启动结果
   */
  startService(serviceName) {
    if (process.env.NODE_ENV === 'development') {
      // 开发环境模拟启动服务
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          // 95%概率启动成功
          if (Math.random() > 0.05) {
            if (this.mockResponses.getStatus[serviceName]) {
              this.mockResponses.getStatus[serviceName].status = 'running';
              this.mockResponses.getStatus[serviceName].health = 85;
              this.mockResponses.getStatus[serviceName].lastChecked = 
                new Date().toISOString();
                
              resolve({
                success: true,
                message: `${serviceName}服务已成功启动`,
                service: this.mockResponses.getStatus[serviceName]
              });
            } else {
              reject(new Error(`未知服务: ${serviceName}`));
            }
          } else {
            reject(new Error(`启动${serviceName}服务失败: 连接超时`));
          }
        }, 2000);  // 启动服务通常需要更长时间
      });
    }
    
    // 生产环境使用实际API
    return this.post(`/control/${serviceName}/start`);
  }
  
  /**
   * 停止服务
   * @param {string} serviceName 服务名称
   * @returns {Promise} 停止结果
   */
  stopService(serviceName) {
    if (process.env.NODE_ENV === 'development') {
      // 开发环境模拟停止服务
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          // 95%概率停止成功
          if (Math.random() > 0.05) {
            if (this.mockResponses.getStatus[serviceName]) {
              this.mockResponses.getStatus[serviceName].status = 'stopped';
              this.mockResponses.getStatus[serviceName].health = 0;
              this.mockResponses.getStatus[serviceName].lastChecked = 
                new Date().toISOString();
                
              resolve({
                success: true,
                message: `${serviceName}服务已成功停止`,
                service: this.mockResponses.getStatus[serviceName]
              });
            } else {
              reject(new Error(`未知服务: ${serviceName}`));
            }
          } else {
            reject(new Error(`停止${serviceName}服务失败: 操作超时`));
          }
        }, 1500);  // 停止服务通常需要一些时间
      });
    }
    
    // 生产环境使用实际API
    return this.post(`/control/${serviceName}/stop`);
  }
  
  /**
   * 重启服务
   * @param {string} serviceName 服务名称
   * @returns {Promise} 重启结果
   */
  restartService(serviceName) {
    if (process.env.NODE_ENV === 'development') {
      // 开发环境模拟重启服务
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          // 90%概率重启成功
          if (Math.random() > 0.1) {
            if (this.mockResponses.getStatus[serviceName]) {
              // 先设置为停止状态
              this.mockResponses.getStatus[serviceName].status = 'stopped';
              this.mockResponses.getStatus[serviceName].health = 0;
              
              // 延迟后设置为运行状态
              setTimeout(() => {
                this.mockResponses.getStatus[serviceName].status = 'running';
                this.mockResponses.getStatus[serviceName].health = 95;
                this.mockResponses.getStatus[serviceName].lastChecked = 
                  new Date().toISOString();
                  
                resolve({
                  success: true,
                  message: `${serviceName}服务已成功重启`,
                  service: this.mockResponses.getStatus[serviceName]
                });
              }, 1500);
            } else {
              reject(new Error(`未知服务: ${serviceName}`));
            }
          } else {
            reject(new Error(`重启${serviceName}服务失败: 服务启动异常`));
          }
        }, 1000);
      });
    }
    
    // 生产环境使用实际API
    return this.post(`/control/${serviceName}/restart`);
  }
  
  /**
   * 获取服务健康报告
   * @param {string} serviceName 服务名称
   * @returns {Promise} 健康报告详情
   */
  getServiceHealthReport(serviceName) {
    if (process.env.NODE_ENV === 'development') {
      // 开发环境返回模拟健康报告
      return new Promise((resolve, reject) => {
        setTimeout(() => {
          if (this.mockResponses.getStatus[serviceName]) {
            const baseService = this.mockResponses.getStatus[serviceName];
            
            // 根据不同服务生成详细健康报告
            let healthReport = {
              name: baseService.name,
              status: baseService.status,
              health: baseService.health,
              lastChecked: new Date().toISOString(),
              checks: []
            };
            
            if (serviceName === 'HDFS') {
              healthReport.checks = [
                { name: '存储空间', status: 'passed', value: '60% 可用' },
                { name: 'NameNode状态', status: 'passed', value: '正常' },
                { name: 'DataNode状态', status: 'passed', value: '5/5 正常' },
                { name: '副本状态', status: 'passed', value: '所有块副本健康' },
                { name: '文件系统检查', status: 'passed', value: '最近检查通过' }
              ];
            } else if (serviceName === 'Hive') {
              healthReport.checks = [
                { name: 'Metastore连接', status: 'passed', value: '已连接' },
                { name: 'JDBC连接', status: 'passed', value: '响应正常' },
                { name: '查询执行', status: 'passed', value: '执行时间正常' },
                { name: '表状态检查', status: 'warning', value: '3个表需要优化' },
                { name: '权限检查', status: 'passed', value: '权限配置正确' }
              ];
            } else if (serviceName === 'Spark') {
              healthReport.checks = [
                { name: 'Spark Context', status: 'passed', value: '运行中' },
                { name: '资源管理', status: 'passed', value: '资源充足' },
                { name: '应用状态', status: 'passed', value: '3个运行中, 0个失败' },
                { name: '执行性能', status: 'warning', value: '部分任务延迟' },
                { name: '内存使用', status: 'passed', value: '70% 已使用' }
              ];
            } else if (serviceName === 'Kafka') {
              healthReport.checks = [
                { name: 'Broker状态', status: 'passed', value: '3/3 正常' },
                { name: 'Controller状态', status: 'passed', value: '主控制器健康' },
                { name: '主题状态', status: 'passed', value: '所有主题可用' },
                { name: '消息处理', status: 'passed', value: '吞吐量正常' },
                { name: '副本同步', status: 'warning', value: '2个分区同步延迟' }
              ];
            }
            
            resolve(healthReport);
          } else {
            reject(new Error(`未知服务: ${serviceName}`));
          }
        }, 800);
      });
    }
    
    // 生产环境使用实际API
    return this.get(`/health/${serviceName}`);
  }
}

// 导出单例
export default new ServiceStatusApi(); 