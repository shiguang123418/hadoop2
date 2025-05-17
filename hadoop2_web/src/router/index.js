import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../views/HomePage.vue'
import DataUpload from '../views/DataUpload.vue'
import DataAnalysis from '../views/DataAnalysis.vue'
import DataVisualization from '../views/DataVisualization.vue'
import Login from '../views/Login.vue'
import WeatherChart from '../components/WeatherChart.vue'
import SparkAnalysis from '../views/SparkAnalysis.vue'
import SparkDashboard from '../views/SparkDashboard.vue'
import SparkStreaming from '../views/SparkStreaming.vue'
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
      path: '/data-upload', 
      component: DataUpload, 
      meta: { requiresAuth: true }
    },
    { 
      path: '/data-analysis', 
      component: DataAnalysis, 
      name: 'hive-console',
      meta: { requiresAuth: true }
    },
    { 
      path: '/data-visualization', 
      component: DataVisualization,
      meta: { requiresAuth: true }
    },
    { 
      path: '/weather-chart', 
      component: WeatherChart,
      meta: { requiresAuth: true }
    },
    { 
      path: '/crop-analysis', 
      component: () => import('../views/CropAnalysis.vue'),
      meta: { requiresAuth: true }
    },
    { 
      path: '/spark-analysis', 
      component: SparkAnalysis,
      name: 'spark-analysis',
      meta: { requiresAuth: true }
    },
    {
      path: '/spark-dashboard',
      component: SparkDashboard,
      name: 'spark-dashboard',
      meta: { requiresAuth: true }
    },
    {
      path: '/spark-streaming',
      component: SparkStreaming,
      name: 'spark-streaming',
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