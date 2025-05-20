<template>
  <div class="hive-analytics-view">
    <h2>Hive 数据分析中心</h2>
    <p class="description">强大的数据分析和可视化工具，深入挖掘农业数据价值</p>
    
    <HiveAnalytics />
  </div>
</template>

<script>
import HiveAnalytics from '../components/HiveAnalytics.vue';

export default {
  name: 'HiveAnalyticsView',
  components: {
    HiveAnalytics
  }
}
</script>

<style scoped>
.hive-analytics-view {
  padding: 20px;
}

h2 {
  color: #2c3e50;
  font-size: 24px;
  margin-bottom: 5px;
}

.description {
  color: #666;
  margin-bottom: 20px;
}
</style> 