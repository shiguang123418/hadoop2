<template>
  <el-container class="app-wrapper">
    <el-aside width="220px" class="sidebar">
      <div class="logo-container">
        <img src="/logo.svg" alt="Logo" class="logo" v-if="false">
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
            <el-menu-item index="/realtime-visualization" class="menu-item">
              <el-icon><DataAnalysis /></el-icon>
              <span>实时数据可视化</span>
            </el-menu-item>
          </el-sub-menu>
          
          <el-menu-item index="/datasource" class="menu-item">
            <el-icon><DataLine /></el-icon>
            <span>数据源管理</span>
          </el-menu-item>
          
          <el-menu-item index="/notification" class="menu-item">
            <el-icon><Bell /></el-icon>
            <span>通知中心</span>
            <el-badge :value="unreadCount" :max="99" class="notification-badge" v-if="unreadCount > 0" />
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
            <el-menu-item index="/system-config" class="menu-item" v-if="isAdmin">
              <el-icon><Setting /></el-icon>
              <span>系统配置</span>
            </el-menu-item>
            <el-menu-item index="/audit-logs" class="menu-item" v-if="isAdmin">
              <el-icon><List /></el-icon>
              <span>审计日志</span>
            </el-menu-item>
            <el-menu-item index="/notification-manage" class="menu-item" v-if="isAdmin">
              <el-icon><Message /></el-icon>
              <span>通知管理</span>
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
          <el-badge :value="unreadCount" :max="99" class="notification-icon" v-if="unreadCount > 0">
            <el-button 
              type="primary" 
              :icon="Bell" 
              circle 
              @click="goToNotification"
              size="small"
            ></el-button>
          </el-badge>
          <el-button 
            v-else
            type="info" 
            :icon="Bell" 
            circle 
            @click="goToNotification"
            size="small"
          ></el-button>
          
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-dropdown">
              <el-avatar 
                size="small" 
                :src="userAvatar" 
                @error="avatarLoadError">
              </el-avatar>
              <span class="username">{{ username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                <el-dropdown-item command="changePassword">修改密码</el-dropdown-item>
                <el-dropdown-item command="notification">消息通知</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
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
  ArrowDown,
  List,
  Bell,
  Message
} from '@element-plus/icons-vue'
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AuthService from './services/auth'
import axios from 'axios'

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
    ArrowDown,
    List,
    Message
  },
  setup() {
    const router = useRouter();
    const username = ref('管理员');
    const userAvatar = ref('');
    const unreadCount = ref(0);
    let notificationTimer = null;
    
    // 判断是否为管理员
    const isAdmin = computed(() => {
      return AuthService.isAdmin();
    });
    
    // 从服务器获取最新用户信息
    const fetchCurrentUser = async () => {
      try {
        // 如果未登录则不请求
        if (!AuthService.isLoggedIn()) {
          console.log('用户未登录，跳过获取用户信息');
          return;
        }
        
        console.log('正在从服务器获取最新用户信息...');
        const response = await axios.get('/auth/current', {
          headers: {
            'Authorization': `Bearer ${AuthService.getToken()}`
          }
        });
        
        console.log('获取用户信息响应:', response.data);
        
        if (response.data && response.data.code === 200 && response.data.data) {
          const userData = response.data.data;
          console.log('服务器返回的用户信息:', userData);
          
          // 更新本地存储
          localStorage.setItem('user', JSON.stringify(userData));
          
          // 更新页面显示
          username.value = userData.username || '管理员';
          
          // 处理头像URL
          if (userData.avatar) {
            console.log('用户头像URL存在:', userData.avatar);
            // 确保URL是有效的
            if (userData.avatar.startsWith('http')) {
              console.log('头像URL格式正确，直接使用');
              userAvatar.value = userData.avatar;
            } else {
              console.log('头像URL格式可能不正确，尝试修复:', userData.avatar);
              // 尝试修复URL格式
              if (userData.avatar.startsWith('/')) {
                userAvatar.value = `${window.location.origin}${userData.avatar}`;
              } else {
                userAvatar.value = `${window.location.origin}/${userData.avatar}`;
              }
              console.log('修复后的头像URL:', userAvatar.value);
            }
          } else {
            console.log('用户没有头像');
            userAvatar.value = ''; // 清空URL
          }
          
          return userData;
        }
      } catch (error) {
        console.error('获取用户信息失败:', error);
      }
      return null;
    };
    
    // 获取未读通知数量
    const fetchUnreadCount = async () => {
      if (!AuthService.isLoggedIn()) return;
      
      try {
        const response = await axios.get('/notifications/unread-count');
        if (response.data.success) {
          unreadCount.value = response.data.data;
        }
      } catch (error) {
        console.error('获取未读通知数量失败:', error);
      }
    };
    
    const startNotificationTimer = () => {
      // 每隔一分钟检查一次未读通知数量
      notificationTimer = setInterval(() => {
        fetchUnreadCount();
      }, 60000); // 60秒
    };
    
    const goToNotification = () => {
      router.push('/notification');
    };
    
    onMounted(async () => {
      // 尝试从服务器获取最新用户信息
      const serverUser = await fetchCurrentUser();
      
      if (!serverUser) {
        // 如果服务器获取失败，则使用本地缓存
        const localUser = AuthService.getCurrentUser();
        console.log('使用本地缓存的用户信息:', localUser);
        
        if (localUser) {
          username.value = localUser.username || '管理员';
          
          // 处理头像URL
          if (localUser.avatar) {
            console.log('用户头像URL:', localUser.avatar);
            userAvatar.value = localUser.avatar;
          } else {
            console.log('用户没有头像');
            userAvatar.value = ''; // El-avatar组件会在src为空时显示默认图标
          }
        }
      }
      
      // 获取未读通知数量
      await fetchUnreadCount();
      // 启动定时器
      startNotificationTimer();
    });
    
    onUnmounted(() => {
      // 清除定时器
      if (notificationTimer) {
        clearInterval(notificationTimer);
      }
    });
    
    // 处理下拉菜单命令
    const handleCommand = (command) => {
      if (command === 'logout') {
        ElMessageBox.confirm('确定要退出登录吗?', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(() => {
          AuthService.logout();
          router.push('/login');
          ElMessage.success('已成功退出登录');
        }).catch(() => {});
      } else if (command === 'profile') {
        router.push('/profile');
      } else if (command === 'changePassword') {
        router.push('/change-password');
      } else if (command === 'notification') {
        router.push('/notification');
      }
    };
    
    // 处理头像加载错误
    const avatarLoadError = () => {
      console.error('头像加载失败');
      userAvatar.value = ''; // 清空src，使用默认图标
    };
    
    return {
      username,
      userAvatar,
      isAdmin,
      unreadCount,
      handleCommand,
      avatarLoadError,
      goToNotification,
      Bell
    };
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
  margin-left: 15px;
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

/* 通知样式 */
.notification-badge {
  margin-left: 8px;
}

.notification-icon {
  margin-right: 10px;
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
}
</style>