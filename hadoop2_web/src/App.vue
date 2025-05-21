<template>
  <div class="app-container">
    <!-- 头部 -->
    <header>
      <nav class="navbar">
        <div class="logo">农业大数据平台</div>
        <div class="nav-links">
          <router-link to="/" :class="{ 'active': $route.path === '/' }">首页</router-link>
          <router-link to="/hdfs-explorer" v-if="isLoggedIn" :class="{ 'active': $route.path === '/hdfs-explorer' }">HDFS浏览器</router-link>
          <router-link to="/hive-explorer" v-if="isLoggedIn" :class="{ 'active': $route.path === '/hive-explorer' }">Hive查询</router-link>
          <router-link to="/hive-analytics" v-if="isLoggedIn" :class="{ 'active': $route.path === '/hive-analytics' }">Hive分析</router-link>
          <router-link to="/crop-analysis" v-if="isLoggedIn" :class="{ 'active': $route.path === '/crop-analysis' }">产量预测</router-link>
          <router-link to="/sensor-monitor" v-if="isLoggedIn" :class="{ 'active': $route.path === '/sensor-monitor' }">传感器监控</router-link>
          <router-link to="/login" v-if="!isLoggedIn" :class="{ 'active': $route.path === '/login' }">登录</router-link>
          <a href="#" @click.prevent="logout" v-if="isLoggedIn" class="logout-link">退出</a>
        </div>
      </nav>
    </header>

    <!-- 主要内容区域 -->
    <main class="main-content">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <!-- 页脚 -->
    <footer class="footer">
      <div class="container">
        <p>&copy; 2025 农业大数据平台 - 版权所有</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import AuthService from './services/auth'

const router = useRouter()
const route = useRoute()
const isLoggedIn = ref(false)

// 更新登录状态
const updateLoginStatus = () => {
  isLoggedIn.value = AuthService.isLoggedIn()
}

// 退出登录
const logout = () => {
  AuthService.logout()
  isLoggedIn.value = false
  router.push('/login')
}

// 初始化检查登录状态
onMounted(() => {
  updateLoginStatus()
  
  // 添加路由变化监听
  router.beforeEach((to, from, next) => {
    updateLoginStatus()
    next()
  })
})

// 清理路由监听
onBeforeUnmount(() => {
  // 路由监听会自动清理，无需手动操作
})
</script>

<style>
:root {
  --primary-color: #4CAF50;
  --primary-dark: #388E3C;
  --primary-light: #C8E6C9;
  --accent-color: #FFEB3B;
  --text-color: #333333;
  --text-light: #666666;
  --bg-light: #ffffff;
  --bg-gray: #f5f5f5;
  --border-radius: 8px;
  --shadow-sm: 0 1px 3px rgba(0, 0, 0, 0.1);
  --shadow-md: 0 4px 6px rgba(0, 0, 0, 0.1);
  --shadow-lg: 0 10px 15px rgba(0, 0, 0, 0.1);
  --transition-speed: 0.3s;
}

.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background-color: var(--bg-gray);
}

/* 头部样式 */
header {
  background-color: var(--primary-color);
  color: white;
  padding: 0.8rem 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  position: sticky;
  top: 0;
  z-index: 100;
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 1.5rem;
}

.logo {
  font-size: 1.6rem;
  font-weight: bold;
  letter-spacing: 0.5px;
  background: linear-gradient(135deg, #ffffff, #e0e0e0);
  -webkit-background-clip: text;
  color: transparent;
  text-shadow: 1px 1px 1px rgba(0,0,0,0.2);
}

.nav-links {
  display: flex;
  gap: 1.2rem;
  align-items: center;
}

.nav-links a {
  color: white;
  text-decoration: none;
  padding: 0.6rem 0.8rem;
  border-radius: 4px;
  transition: all 0.3s;
  font-weight: 500;
  position: relative;
}

.nav-links a:hover {
  background-color: rgba(255, 255, 255, 0.15);
  transform: translateY(-2px);
}

.nav-links a.active {
  background-color: rgba(255, 255, 255, 0.25);
  font-weight: 600;
}

.nav-links a.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 70%;
  height: 3px;
  background-color: var(--accent-color);
  border-radius: 2px;
}

.logout-link {
  color: white;
  font-weight: 500;
  background-color: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.3);
  padding: 0.5rem 1rem !important;
  border-radius: 4px;
  transition: all 0.3s;
}

.logout-link:hover {
  background-color: rgba(255, 0, 0, 0.15);
}

/* 主要内容区域样式 */
.main-content {
  flex: 1;
  width: 100%;
  max-width: 1280px;
  margin: 1.5rem auto;
  padding: 0 1.5rem;
  padding-bottom: 2rem;
}

/* 页面切换过渡效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 页脚样式 */
.footer {
  background-color: #333;
  color: rgba(255, 255, 255, 0.8);
  text-align: center;
  padding: 1.8rem 0;
  margin-top: auto;
  font-size: 0.9rem;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.1);
}

/* 响应式设计 */
@media (max-width: 768px) {
  .navbar {
    flex-direction: column;
    padding: 1rem 0;
  }
  
  .logo {
    margin-bottom: 1rem;
  }
  
  .nav-links {
    width: 100%;
    flex-wrap: wrap;
    justify-content: center;
    gap: 0.8rem;
  }
  
  .nav-links a {
    padding: 0.5rem 0.7rem;
    font-size: 0.9rem;
  }
}
</style>