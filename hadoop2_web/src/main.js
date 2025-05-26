import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'
import VueAxios from 'vue-axios'
import AuthService from './services/auth'
import router from './router'
import apiConfig from './config/api.config'

// 导入Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'

// 导入样式文件
import './assets/base.css'
import './assets/main.css'
import './assets/background-fix.css'
import './assets/vue-cropper.css'  // 引入vue-cropper样式

// 尝试导入vue-cropper样式，如果安装了的话
try {
  import('vue-cropper/dist/index.css').catch(err => {
    console.warn('未能加载vue-cropper样式:', err);
  });
} catch (e) {
  console.warn('未能导入vue-cropper样式');
}

// 配置axios - 使用apiConfig中的baseUrl作为默认路径
// apiConfig.baseUrl 已经包含了 /api 前缀
axios.defaults.baseURL = apiConfig.baseUrl
axios.defaults.withCredentials = true

console.log('API基础路径:', apiConfig.baseUrl)
console.log('API代理模式:', apiConfig.useProxy ? '启用' : '禁用')

// 添加请求拦截器，记录每个请求
axios.interceptors.request.use(config => {
  const fullUrl = (config.baseURL || '') + config.url;
  console.log(`发送请求: ${config.method.toUpperCase()} ${fullUrl}`);
  return config;
}, error => {
  return Promise.reject(error);
});

// 添加响应拦截器
axios.interceptors.response.use(response => {
  console.log(`收到响应: ${response.config.method.toUpperCase()} ${response.config.url} -> ${response.status}`);
  return response;
}, error => {
  console.error('请求错误:', error);
  return Promise.reject(error);
});

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

// 挂载应用
app.mount('#app')