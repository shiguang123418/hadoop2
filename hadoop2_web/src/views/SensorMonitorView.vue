<template>
  <div class="sensor-monitor-view">
    <sensor-dashboard />
  </div>
</template>

<script>
import SensorDashboard from '@/components/SensorDashboard.vue'

export default {
  name: 'SensorMonitorView',
  components: {
    SensorDashboard
  }
}
</script>

<style scoped>
.sensor-monitor-view {
  width: 100%;
  height: 100%;
  padding: 20px;
  box-sizing: border-box;
}
</style>