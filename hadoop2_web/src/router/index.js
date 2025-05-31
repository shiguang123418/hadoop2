import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import Login from '../views/Login.vue'
import AuthService from '../services/auth'
import { ElMessage } from 'element-plus'

// 创建路由实例
const router = createRouter({
  history: createWebHistory(),
  routes: [
    { 
      path: '/login', 
      component: Login,
      meta: { requiresAuth: false }
    },
    { 
      path: '/', 
      component: HomePage,
      meta: { requiresAuth: false, title: '首页' }
    },
    { 
      path: '/hdfs-explorer', 
      component: () => import('../views/HDFSExplorerView.vue'),
      name: 'hdfs-explorer',
      meta: { requiresAuth: true }
    },
    { 
      path: '/hive-explorer', 
      component: () => import('../views/HiveExplorerView.vue'),
      name: 'hive-explorer',
      meta: { requiresAuth: true }
    },
    { 
      path: '/hive-analytics', 
      component: () => import('../views/HiveAnalyticsView.vue'),
      name: 'hive-analytics',
      meta: { requiresAuth: true }
    },
    { 
      path: '/crop-analysis', 
      component: () => import('../views/CropAnalysis.vue'),
      meta: { requiresAuth: true }
    },
    { 
      path: '/user-management', 
      component: () => import('../views/UserManagement.vue'),
      name: 'user-management',
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    { 
      path: '/system-settings', 
      component: () => import('../views/SystemSettings.vue'),
      name: 'system-settings',
      meta: { requiresAuth: true, requiresAdmin: true }
    },
    {
      path: '/datasource',
      name: 'DataSource',
      component: () => import('../views/datasource/DataSourceList.vue'),
      meta: { title: '数据源管理' }
    },
    // 新增审计日志路由
    { 
      path: '/audit-logs', 
      component: () => import('../views/AuditLogView.vue'),
      name: 'audit-logs',
      meta: { 
        requiresAuth: true, 
        requiresAdmin: true, 
        title: '系统操作审计日志' 
      }
    },
    // 系统配置管理
    {
      path: '/system-config',
      name: 'system-config',
      component: () => import('../views/SystemConfigView.vue'),
      meta: { 
        requiresAuth: true, 
        requiresAdmin: true, 
        title: '系统配置管理' 
      }
    },
    // 农业传感器监控
    {
      path: '/agriculture-monitor',
      name: 'agriculture-monitor',
      component: () => import('../views/AgricultureMonitor.vue'),
      meta: { requiresAuth: false, title: '农业传感器监控' }
    },
    // 新增路由 - 个人信息
    {
      path: '/profile',
      name: 'profile',
      component: () => import('../views/Profile.vue'),
      meta: { requiresAuth: true, title: '个人信息' }
    },
    // 新增路由 - 修改密码
    {
      path: '/change-password',
      name: 'change-password',
      component: () => import('../views/ChangePassword.vue'),
      meta: { requiresAuth: true, title: '修改密码' }
    },
    // 新增路由 - 消息通知中心
    {
      path: '/notification',
      name: 'notification',
      component: () => import('../views/notification/NotificationList.vue'),
      meta: { requiresAuth: true, title: '消息通知中心' }
    },
    // 新增路由 - 通知管理(管理员)
    {
      path: '/notification-manage',
      name: 'notification-manage',
      component: () => import('../views/notification/NotificationManage.vue'),
      meta: { 
        requiresAuth: true, 
        requiresAdmin: true, 
        title: '通知管理' 
      }
    },
    // 新增大屏展示页面路由
    {
      path: '/agriculture-dashboard',
      name: 'agriculture-dashboard',
      component: () => import('../views/AgricultureDataDashboard.vue'),
      meta: { 
        requiresAuth: false, 
        title: '农业大数据监控平台',
        fullScreen: true // 添加全屏标记
      }
    },
    // WebSocket调试器
    {
      path: '/ws-debug',
      name: 'ws-debug',
      component: () => import('../components/WebSocketDebugger.vue'),
      meta: {
        requiresAuth: false,
        title: 'WebSocket连接调试器'
      }
    }
  ]
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  document.title = to.meta.title ? `${to.meta.title} - 农业数据可视化平台` : '农业数据可视化平台'

  // 检查路由是否需要认证
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth === true);
  const requiresAdmin = to.matched.some(record => record.meta.requiresAdmin === true);
  const isLoggedIn = AuthService.isLoggedIn();
  
  // 检查用户是否为管理员
  const isAdmin = AuthService.isAdmin();
  
  console.log('路由守卫检查:', { 
    path: to.path, 
    requiresAuth, 
    requiresAdmin,
    isLoggedIn,
    isAdmin,
    matched: to.matched.map(r => r.path)
  });

  if (requiresAuth && !isLoggedIn) {
    // 需要认证但未登录，重定向到登录页
    console.log('未登录，重定向到登录页');
    ElMessage.warning('请先登录后再访问此页面');
    next('/login');
  } else if (requiresAdmin && !isAdmin) {
    // 需要管理员权限但不是管理员，重定向到首页
    console.log('需要管理员权限，重定向到首页');
    ElMessage.error('您没有管理员权限，无法访问此页面');
    next('/');
  } else if (to.path === '/login' && isLoggedIn) {
    // 已登录但访问登录页，重定向到首页
    console.log('已登录，重定向到首页');
    next('/');
  } else {
    // 其他情况正常导航
    console.log('允许导航到:', to.path);
    next();
  }
});

export default router; 