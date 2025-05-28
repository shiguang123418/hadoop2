<template>
  <div class="agriculture-monitor-page">
    <h1>农业数据实时监控系统</h1>
    <AgricultureSensorMonitor />
  </div>
</template>

<script>
import AgricultureSensorMonitor from '@/components/AgricultureSensorMonitor.vue'

export default {
  name: 'AgricultureMonitorView',
  components: {
    AgricultureSensorMonitor
  }
}
</script>

<style scoped>
.agriculture-monitor-page {
  padding: 20px;
}

h1 {
  margin-bottom: 20px;
  text-align: center;
  color: #409EFF;
}
</style> 