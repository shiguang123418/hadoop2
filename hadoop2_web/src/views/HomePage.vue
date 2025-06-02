<template>
  <div class="home-page">
    <!-- 使用欢迎组件 -->
    <WelcomeSection @navigate="handleNavigation" />

    <!-- 使用服务状态监控组件 -->
    <ServiceStatusMonitor @service-status-change="handleServiceStatusChange" />

    <ServiceStatus/>
    
    <!-- 使用数据概览组件 -->
    <DataStatsSummary />
    
    <!-- 使用快速链接组件 -->
    <QuickLinksPanel @link-click="goToRoute" />
  </div>
</template>

<script>
import { useRouter } from 'vue-router'
import ServiceStatus from "@/components/home/ServiceStatus.vue"
// 导入home文件夹下的组件
import WelcomeSection from "@/components/home/WelcomeSection.vue"
import ServiceStatusMonitor from "@/components/home/ServiceStatusMonitor.vue"
import DataStatsSummary from "@/components/home/DataStatsSummary.vue"
import QuickLinksPanel from "@/components/home/QuickLinksPanel.vue"

export default {
  name: 'HomePage',
  components: {
    ServiceStatus,
    WelcomeSection,
    ServiceStatusMonitor,
    DataStatsSummary,
    QuickLinksPanel
  },
  setup() {
    const router = useRouter()
    
    // 处理服务状态变更的回调函数
    const handleServiceStatusChange = (servicesData) => {
      console.log('服务状态变更:', servicesData)
    }

    // 处理路由导航
    const goToRoute = (link) => {
      if (link && link.route) {
        router.push(link.route)
      }
    }

    // 处理欢迎区域的导航事件
    const handleNavigation = (destination) => {
      switch (destination) {
        case 'hdfs':
          router.push('/hdfs-explorer')
          break
        case 'hive':
          router.push('/hive-explorer')
          break
        case 'analysis':
          router.push('/crop-analysis')
          break
        case 'monitor':
          router.push('/agriculture-monitor')
          break
        case 'dashboard':
          router.push('/agriculture-dashboard')
          break
        default:
          console.log('未知的导航目标:', destination)
      }
    }

    return {
      handleServiceStatusChange,
      goToRoute,
      handleNavigation
    }
  }
}
</script>

<style scoped>
.home-page {
  padding-bottom: 30px;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  /* 响应式全局调整 */
}

@media (max-width: 768px) {
  /* 移动设备响应式调整 */
}
</style> 