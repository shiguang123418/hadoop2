<template>
  <div class="quick-links-section">
    <h2 class="section-title">快速链接</h2>
    <el-row :gutter="20">
      <el-col :span="8" v-for="link in links" :key="link.title">
        <el-card class="quick-link-card" shadow="hover" @click="handleLinkClick(link)">
          <div class="quick-link-content">
            <div class="quick-link-icon">
              <el-icon>
                <component :is="link.icon"></component>
              </el-icon>
            </div>
            <div class="quick-link-info">
              <h3 class="quick-link-title">{{ link.title }}</h3>
              <p class="quick-link-description">{{ link.description }}</p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { ref } from 'vue'
import { DataLine, User, Setting, DataBoard } from '@element-plus/icons-vue'

export default {
  name: 'QuickLinksPanel',
  components: {
    DataLine,
    User,
    Setting,
    DataBoard
  },
  props: {
    // 可以传入自定义链接数据
    customLinks: {
      type: Array,
      default: null
    }
  },
  emits: ['link-click'],
  setup(props, { emit }) {
    // 默认链接数据
    const defaultLinks = [
      { 
        title: '数据源管理', 
        description: '管理系统数据源配置', 
        icon: 'DataLine',
        route: '/datasource'
      },
      { 
        title: '用户管理', 
        description: '管理系统用户和权限', 
        icon: 'User',
        route: '/user-management'
      },
      { 
        title: '系统设置', 
        description: '配置系统参数和服务', 
        icon: 'Setting',
        route: '/system-settings'
      },
      { 
        title: '农业大数据平台', 
        description: '高科技风格的大数据监控大屏', 
        icon: 'DataBoard',
        route: '/agriculture-dashboard'
      }
    ]
    
    // 如果传入了自定义数据则使用，否则使用默认数据
    const links = ref(props.customLinks || defaultLinks)
    
    // 处理链接点击
    const handleLinkClick = (link) => {
      // 触发点击事件，将链接数据传给父组件
      emit('link-click', link)
    }
    
    return {
      links,
      handleLinkClick
    }
  }
}
</script>

<style scoped>
.section-title {
  font-size: 18px;
  color: #303133;
  margin: 0 0 20px;
  position: relative;
  padding-left: 12px;
}

.section-title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 18px;
  background-color: #409EFF;
  border-radius: 2px;
}

/* 快速链接区域样式 */
.quick-link-card {
  border-radius: 8px;
  margin-bottom: 20px;
  cursor: pointer;
  transition: all 0.3s;
  height: 100px;
}

.quick-link-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 15px rgba(0, 0, 0, 0.1);
}

.quick-link-content {
  display: flex;
  align-items: center;
  height: 100%;
}

.quick-link-icon {
  width: 50px;
  height: 50px;
  background-color: #ecf5ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 15px;
}

.quick-link-icon .el-icon {
  font-size: 24px;
  color: #409EFF;
}

.quick-link-info {
  flex: 1;
}

.quick-link-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 5px;
}

.quick-link-description {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 响应式调整 */
@media (max-width: 1200px) {
  .el-col {
    width: 50% !important;
  }
}

@media (max-width: 768px) {
  .el-col {
    width: 100% !important;
  }
}
</style> 