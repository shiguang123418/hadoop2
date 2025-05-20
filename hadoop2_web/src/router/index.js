import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import Login from '../views/Login.vue'
import AuthService from '../services/auth'

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
      meta: { requiresAuth: false }
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
    }
  ]
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  // 检查路由是否需要认证
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth === true);
  const isLoggedIn = AuthService.isLoggedIn();
  
  console.log('路由守卫检查:', { 
    path: to.path, 
    requiresAuth, 
    isLoggedIn,
    matched: to.matched.map(r => r.path)
  });

  if (requiresAuth && !isLoggedIn) {
    // 需要认证但未登录，重定向到登录页
    console.log('未登录，重定向到登录页');
    next('/login');
  } else if (to.path === '/login' && isLoggedIn) {
    // 已登录但访问登录页，重定向到首页
    console.log('已登录，重定向到首页');
    next('/');
  } else {
    // 其他情况正常导航
    next();
  }
});

export default router; 