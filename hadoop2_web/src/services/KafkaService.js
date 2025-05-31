import ApiService from './api.service';

/**
 * Kafka服务 - 提供与Kafka消息系统交互的功能
 */
class KafkaServiceClass extends ApiService {
  constructor() {
    // 使用服务名称
    super('kafka');
    console.log('Kafka服务初始化');
  }
  
  /**
   * 获取Kafka连接状态
   * @returns {Promise} 连接状态信息
   */
  getStatus() {
    return this.get('/status');
  }
  
  /**
   * 获取所有主题列表
   * @returns {Promise} 主题列表
   */
  getTopics() {
    return this.get('/topics');
  }
  
  /**
   * 创建新主题
   * @param {string} topicName 主题名称
   * @param {number} partitions 分区数
   * @param {number} replicationFactor 复制因子
   * @returns {Promise} 创建结果
   */
  createTopic(topicName, partitions = 1, replicationFactor = 1) {
    return this.post('/topics', {
      name: topicName,
      partitions,
      replicationFactor
    });
  }
  
  /**
   * 删除主题
   * @param {string} topicName 主题名称
   * @returns {Promise} 删除结果
   */
  deleteTopic(topicName) {
    return this.delete(`/topics/${topicName}`);
  }
  
  /**
   * 获取主题详情
   * @param {string} topicName 主题名称
   * @returns {Promise} 主题详细信息
   */
  getTopicInfo(topicName) {
    return this.get(`/topics/${topicName}`);
  }
  
  /**
   * 获取主题的消费者组列表
   * @param {string} topicName 主题名称
   * @returns {Promise} 消费者组列表
   */
  getTopicConsumerGroups(topicName) {
    return this.get(`/topics/${topicName}/consumer-groups`);
  }
  
  /**
   * 向主题生产消息
   * @param {string} topicName 主题名称
   * @param {Object} message 消息对象
   * @returns {Promise} 发送结果
   */
  produceMessage(topicName, message) {
    return this.post(`/topics/${topicName}/messages`, message);
  }
  
  /**
   * 消费主题消息
   * @param {string} topicName 主题名称
   * @param {string} groupId 消费者组ID
   * @param {number} maxMessages 最大消息数
   * @returns {Promise} 消息列表
   */
  consumeMessages(topicName, groupId, maxMessages = 100) {
    return this.get(`/topics/${topicName}/messages`, {
      groupId,
      maxMessages
    });
  }
  
  /**
   * 获取所有消费者组列表
   * @returns {Promise} 消费者组列表
   */
  getConsumerGroups() {
    return this.get('/consumer-groups');
  }
  
  /**
   * 获取消费者组详情
   * @param {string} groupId 消费者组ID
   * @returns {Promise} 消费者组详情
   */
  getConsumerGroupInfo(groupId) {
    return this.get(`/consumer-groups/${groupId}`);
  }
  
  /**
   * 获取Kafka集群节点信息
   * @returns {Promise} 集群节点列表
   */
  getClusterInfo() {
    return this.get('/cluster/info');
  }
}

// 创建单例
const KafkaService = new KafkaServiceClass();

// 导出服务单例
export default KafkaService;

// 命名导出，用于从index.js中导入
export { KafkaService }; 