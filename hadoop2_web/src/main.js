import { createApp } from 'vue'
import App from './App.vue'
import axios from 'axios'
import VueAxios from 'vue-axios'
import AuthService from './services/auth'
import router from './router'

// 导入样式文件
import './assets/base.css'
import './assets/main.css'
import './assets/background-fix.css'

// 配置axios - 移除默认的/api前缀，因为各服务类已经包含了完整路径
axios.defaults.baseURL = ''
axios.defaults.withCredentials = true

// 设置认证头
AuthService.setupAuthHeader();

// 创建Vue应用实例
const app = createApp(App)

// 使用插件
app.use(router)
app.use(VueAxios, axios)

// 挂载应用
app.mount('#app')