<template>
  <div class="hdfs-explorer-view">
    <h2>HDFS文件浏览器</h2>
    <p class="description">浏览并管理HDFS文件系统中的文件和目录</p>
    
    <HDFSExplorer />
  </div>
</template>

<script>
import HDFSExplorer from '@/components/HDFSExplorer.vue';

export default {
  name: 'HDFSExplorerView',
  components: {
    HDFSExplorer
  }
};
</script>

<style scoped>
.hdfs-explorer-view {
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