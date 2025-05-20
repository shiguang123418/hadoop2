<template>
  <div class="hive-explorer-view">
    <h2>Hive 查询管理器</h2>
    <p class="description">浏览 Hive 数据库和表，执行 HiveQL 查询</p>
    
    <HiveExplorer />
  </div>
</template>

<script>
import HiveExplorer from '../components/HiveExplorer.vue';

export default {
  name: 'HiveExplorerView',
  components: {
    HiveExplorer
  }
};
</script>

<style scoped>
.hive-explorer-view {
  padding: 2rem;
}

h2 {
  color: #333;
  margin-bottom: 0.5rem;
}

.description {
  color: #666;
  margin-bottom: 2rem;
}
</style> 