import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'
import VueAxios from 'vue-axios'
import router from './router'
import apiConfig from './config/api.config'
import { setupApiInterceptor } from './utils/service-helper'
import logger from './utils/logger'

// 导入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 导入WebSocket相关依赖
import SockJS from 'sockjs-client'
import { Stomp } from '@stomp/stompjs'
// 导入样式文件
import './assets/base.css'
import './assets/main.css'
import './assets/background-fix.css'
import './assets/vue-cropper.css'  // 引入vue-cropper样式
import './styles/variables.css'  // 引入CSS变量

// 确保正确加载vue-cropper样式
import 'vue-cropper/dist/index.css'

// 配置axios - 使用空的baseURL，由拦截器统一处理API前缀
axios.defaults.baseURL = apiConfig.baseUrl
axios.defaults.withCredentials = true
axios.defaults.timeout = 15000 // 全局15秒超时
axios.defaults.headers.common['X-Requested-With'] = 'XMLHttpRequest'

// 将WebSocket客户端挂载到全局，以便在组件中使用
window.SockJS = SockJS
window.Stomp = Stomp

// 设置API请求拦截器，统一添加/api前缀
setupApiInterceptor();

<<<<<<< HEAD
// 设置认证头，并检查token是否有效
if (AuthService.isLoggedIn()) {
  // 设置认证头
  AuthService.setupAuthHeader();
  
  // 立即检查token是否有效
  if (!AuthService.isTokenValid()) {
    logger.warn('应用启动时发现过期token，执行自动登出');
    AuthService.logout();
    router.push('/login');
  }
=======
// 设置初始认证头 (从localStorage读取token)
const token = localStorage.getItem('token');
if (token) {
  axios.defaults.headers.common['Authorization'] = `Bearer ${token}`;
>>>>>>> b0e0a6943b9c974ef7ad351f9cdd89616f735470
}

// 创建Vue应用实例
const app = createApp(App)

// 全局错误处理器
app.config.errorHandler = (err, instance, info) => {
  logger.error('Vue Error:', err);
  logger.debug('Component:', instance);
  logger.debug('Error Info:', info);
};

// 使用插件
app.use(router)
app.use(VueAxios, axios)
app.use(ElementPlus, {
  locale: zhCn,
  size: 'default'
})

// 注册ElementPlus图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 挂载应用
app.mount('#app')

// 设置token检查 - 使用延迟导入AuthService以避免循环依赖
setTimeout(async () => {
  try {
    // 动态导入AuthService
    const { default: AuthService } = await import('./services/auth');
    
    // 设置定期检查token
    const TOKEN_CHECK_INTERVAL = 60000; // 60秒
    const tokenCheckInterval = setInterval(() => {
      // 只有当用户已登录时才检查
      if (localStorage.getItem('token')) {
        AuthService.getToken(); // 这将自动检查token是否过期并处理登出
      }
    }, TOKEN_CHECK_INTERVAL);
    
    logger.info('Token有效性检查定时器已设置，间隔：', TOKEN_CHECK_INTERVAL, 'ms');
  } catch (error) {
    logger.error('加载AuthService失败，无法设置token检查:', error);
  }
}, 2000); // 延迟2秒启动