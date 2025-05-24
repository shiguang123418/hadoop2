<template>
  <el-container class="app-wrapper">
    <el-aside width="220px" class="sidebar">
      <div class="logo-container">
        <!-- <img src="/logo.png" alt="Logo" class="logo" v-if="false"> -->
        <h2 class="logo-text">农业数据平台</h2>
      </div>
      <el-scrollbar>
        <el-menu
          :router="true"
          :default-active="$route.path"
          class="el-menu-vertical"
          background-color="#f9fafc"
          text-color="#2c3e50"
          active-text-color="#409EFF"
        >
          <el-menu-item index="/" class="menu-item">
            <el-icon><HomeFilled /></el-icon>
            <span>首页</span>
          </el-menu-item>
          
          <el-sub-menu index="1" class="submenu">
            <template #title>
              <el-icon><FolderOpened /></el-icon>
              <span>核心功能</span>
            </template>
            <el-menu-item index="/hdfs-explorer" class="menu-item">
              <el-icon><Files /></el-icon>
              <span>HDFS浏览器</span>
            </el-menu-item>
            <el-menu-item index="/hive-explorer" class="menu-item">
              <el-icon><Document /></el-icon>
              <span>Hive查询</span>
            </el-menu-item>
            <el-menu-item index="/hive-analytics" class="menu-item">
              <el-icon><DataAnalysis /></el-icon>
              <span>Hive分析</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-sub-menu index="2" class="submenu">
            <template #title>
              <el-icon><Grid /></el-icon>
              <span>应用功能</span>
            </template>
            <el-menu-item index="/crop-analysis" class="menu-item">
              <el-icon><Crop /></el-icon>
              <span>产量预测</span>
            </el-menu-item>
            <el-menu-item index="/sensor-monitor" class="menu-item">
              <el-icon><Monitor /></el-icon>
              <span>传感器监控</span>
            </el-menu-item>
            <el-menu-item index="/sensor-dashboard" class="menu-item">
              <el-icon><TrendCharts /></el-icon>
              <span>传感器仪表盘</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="/datasource" class="menu-item">
            <el-icon><DataLine /></el-icon>
            <span>数据源管理</span>
          </el-menu-item>
          
          <el-sub-menu index="3" class="submenu">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/user-management" class="menu-item">
              <el-icon><User /></el-icon>
              <span>用户管理</span>
            </el-menu-item>
            <el-menu-item index="/system-settings" class="menu-item">
              <el-icon><Tools /></el-icon>
              <span>系统设置</span>
            </el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>
    <el-container class="main-container">
      <el-header class="header">
        <div class="header-left">
          <h2 class="header-title">农业数据可视化平台</h2>
        </div>
        <div class="header-right">
          <el-dropdown trigger="click">
            <span class="user-dropdown">
              <el-avatar size="small" icon="el-icon-user"></el-avatar>
              <span class="username">管理员</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item>个人信息</el-dropdown-item>
                <el-dropdown-item>修改密码</el-dropdown-item>
                <el-dropdown-item divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script>
import { 
  DataLine, 
  HomeFilled, 
  FolderOpened, 
  Files, 
  Document, 
  DataAnalysis, 
  Grid, 
  Monitor, 
  TrendCharts, 
  Setting, 
  User, 
  Tools,
  Crop,
  ArrowDown
} from '@element-plus/icons-vue'

export default {
  name: 'App',
  components: {
    DataLine,
    HomeFilled,
    FolderOpened,
    Files,
    Document,
    DataAnalysis,
    Grid,
    Monitor,
    TrendCharts,
    Setting,
    User,
    Tools,
    Crop,
    ArrowDown
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  background-color: #f5f7fa;
}

.app-wrapper {
  height: 100vh;
  overflow: hidden;
}

/* 侧边栏样式 */
.sidebar {
  background-color: #f9fafc;
  box-shadow: 1px 0 6px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 10;
  transition: all 0.3s;
  height: 100%;
  overflow: hidden;
}

.logo-container {
  height: 60px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  background-color: #f9fafc;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 10px;
}

.logo {
  height: 30px;
  margin-right: 10px;
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  color: #409EFF;
  letter-spacing: 1px;
  white-space: nowrap;
}

.el-menu-vertical {
  border-right: none;
  padding: 10px 0;
}

.menu-item {
  height: 50px;
  line-height: 50px;
  margin: 4px 0;
  border-radius: 4px;
  padding-left: 18px !important;
}

.submenu {
  margin: 4px 0;
}

.el-menu-item.is-active {
  background-color: #ecf5ff !important;
  border-left: 3px solid #409EFF;
}

.el-menu-item:hover {
  background-color: #f0f2f5 !important;
}

.el-sub-menu__title {
  padding-left: 18px !important;
  border-radius: 4px;
  margin: 4px 0;
}

.el-sub-menu__title:hover {
  background-color: #f0f2f5 !important;
}

/* 主体内容区域样式 */
.main-container {
  background-color: #f5f7fa;
  height: 100%;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.header {
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 60px;
  position: relative;
  z-index: 9;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
  letter-spacing: 0.5px;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: all 0.3s;
}

.user-dropdown:hover {
  background-color: #f5f7fa;
}

.username {
  margin: 0 8px;
  font-size: 14px;
}

.main-content {
  padding: 20px;
  overflow-y: auto;
  height: calc(100% - 60px);
}

/* 页面过渡动画 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 自定义滚动条 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}

::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 4px;
}

::-webkit-scrollbar-track {
  background: #f5f7fa;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .sidebar {
    width: 64px !important;
  }
  
  .logo-text {
    display: none;
  }
  
  .menu-item span,
  .el-sub-menu__title span {
    display: none;
  }
  
  .el-sub-menu__icon-arrow {
    display: none;
  }
}
</style>