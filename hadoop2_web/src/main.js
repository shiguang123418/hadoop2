import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'
import VueAxios from 'vue-axios'
import AuthService from './services/auth'
import KafkaServiceInstance from './services/kafka'
import router from './router'

// 导入样式文件
import './assets/base.css'
import './assets/main.css'
import './assets/background-fix.css'

// 配置axios
axios.defaults.baseURL = '/api'
axios.defaults.withCredentials = true

// 设置认证头
AuthService.setupAuthHeader();

// Kafka连接初始化函数
const initKafka = () => {
  if (AuthService.isLoggedIn()) {
    console.log('用户已登录，初始化Kafka连接...');
    KafkaServiceInstance.connect()
      .then(() => {
        console.log('Kafka WebSocket连接成功');
      })
      .catch(error => {
        console.error('Kafka WebSocket连接失败:', error);
      });
  } else {
    console.log('用户未登录，不初始化Kafka连接');
  }
};

// 创建Vue应用实例
const app = createApp(App)

// 使用插件
app.use(router)
app.use(VueAxios, axios)

// 将Kafka服务实例添加到全局属性
app.config.globalProperties.$kafka = KafkaServiceInstance;

// 挂载应用
app.mount('#app')

// 如需在用户登录后自动初始化Kafka，取消下行注释
// initKafka();

// 页面关闭时断开Kafka连接
window.addEventListener('beforeunload', () => {
  KafkaServiceInstance.disconnect();
});