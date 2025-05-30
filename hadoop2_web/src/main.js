import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'
import VueAxios from 'vue-axios'
import AuthService from './services/auth'
import router from './router'
import apiConfig from './config/api.config'
import { setupApiInterceptor } from './utils/service-helper'

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

console.log('API基础路径:', apiConfig.baseUrl)
console.log('WebSocket客户端加载:', SockJS ? '成功' : '失败', Stomp ? '成功' : '失败')

// 设置API请求拦截器，统一添加/api前缀
setupApiInterceptor();

// 添加请求拦截器
axios.interceptors.request.use(
  config => {
    console.log(`发送请求: ${config.method.toUpperCase()} ${config.baseURL}${config.url}`)
    return config
  },
  error => {
    console.error('请求拦截器错误:', error)
    return Promise.reject(error)
  }
)

// 添加响应拦截器
axios.interceptors.response.use(
  response => {
    console.log(`接收响应: ${response.config.url}`, response.status)
    return response
  },
  error => {
    if (error.response) {
      // 服务器返回错误状态码
      console.error(`服务器错误: ${error.response.status}`, error.response.data)
    } else if (error.request) {
      // 请求发送但没收到响应
      console.error('网络错误: 没有收到响应', error.request)
    } else {
      // 请求配置出错
      console.error('请求错误:', error.message)
    }
    return Promise.reject(error)
  }
)

// 设置认证头
AuthService.setupAuthHeader();

// 创建Vue应用实例
const app = createApp(App)

// 全局错误处理器
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue Error:', err);
  console.log('Component:', instance);
  console.log('Error Info:', info);
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