<template>
  <div class="app-container">
    <!-- 头部 -->
    <header>
      <nav class="navbar">
        <div class="logo">农业大数据平台</div>
        <div class="nav-links">
          <router-link to="/" :class="{ 'active': $route.path === '/' }">首页</router-link>
          <router-link to="/data-upload" v-if="isLoggedIn" :class="{ 'active': $route.path === '/data-upload' }">上传数据</router-link>
          <router-link to="/data-analysis" v-if="isLoggedIn" :class="{ 'active': $route.path === '/data-analysis' }">数据分析</router-link>
          <router-link to="/spark-dashboard" v-if="isLoggedIn" :class="['highlight-link', { 'active': $route.path === '/spark-dashboard' }]">农业传感器监控</router-link>
          <router-link to="/spark-analysis" v-if="isLoggedIn" :class="{ 'active': $route.path === '/spark-analysis' }">Spark分析</router-link>
          <router-link to="/spark-streaming" v-if="isLoggedIn" :class="{ 'active': $route.path === '/spark-streaming' }">实时监控</router-link>
          <router-link to="/data-visualization" v-if="isLoggedIn" :class="{ 'active': $route.path === '/data-visualization' }">数据可视化</router-link>
          <router-link to="/crop-analysis" v-if="isLoggedIn" :class="{ 'active': $route.path === '/crop-analysis' }">产量预测</router-link>
          <router-link to="/login" v-if="!isLoggedIn" :class="{ 'active': $route.path === '/login' }">登录</router-link>
          <a href="#" @click.prevent="logout" v-if="isLoggedIn">退出</a>
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
import { ref, onMounted, onBeforeUnmount, computed } from 'vue'
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
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  width: 100%;
  background-color: #f9f9f9;
}

/* 头部样式 */
header {
  background-color: #4CAF50;
  color: white;
  padding: 1rem 0;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.navbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 1rem;
}

.logo {
  font-size: 1.5rem;
  font-weight: bold;
}

.nav-links {
  display: flex;
  gap: 1.5rem;
}

.nav-links a {
  color: white;
  text-decoration: none;
  padding: 0.5rem;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.nav-links a:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.highlight-link {
  background-color: rgba(255, 255, 255, 0.2);
  font-weight: bold;
  position: relative;
}

.highlight-link::after,
.nav-links a.active::after {
  content: '';
  position: absolute;
  bottom: 2px;
  left: 50%;
  transform: translateX(-50%);
  width: 70%;
  height: 2px;
  background-color: white;
}

.nav-links a.active {
  background-color: rgba(255, 255, 255, 0.3);
  font-weight: bold;
  position: relative;
}

/* 主要内容区域样式 */
.main-content {
  flex: 1;
  width: 100%;
  max-width: 100%;
  margin: 0 auto;
  padding: 0;
}

/* 页面切换过渡效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 页脚样式 */
.footer {
  background-color: #333;
  color: white;
  text-align: center;
  padding: 1.5rem 0;
  margin-top: auto;
}
</style>