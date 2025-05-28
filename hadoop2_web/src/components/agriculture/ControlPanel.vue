<template>
  <el-card class="control-panel">
    <div class="row">
      <div class="connection-status">
        <h4>
          WebSocket连接状态: 
          <el-tag :type="isConnected ? 'success' : 'danger'" size="small">
            {{ isConnected ? '已连接' : '未连接' }}
          </el-tag>
        </h4>
        <div class="mt-2">
          <el-button type="primary" @click="connect" :disabled="isConnected">连接</el-button>
          <el-button type="danger" @click="disconnect" :disabled="!isConnected">断开</el-button>
        </div>
      </div>
      
      <div class="topics">
        <h4>订阅主题</h4>
        <div class="topic-selection">
          <el-checkbox-group v-model="topicsModel">
            <el-checkbox v-for="topic in availableTopics" :key="topic.value" :label="topic.value">
              {{ topic.label }}
            </el-checkbox>
          </el-checkbox-group>
        </div>
      </div>
      
      <div class="actions">
        <el-button type="success" @click="sendTest" :disabled="!isConnected">发送测试数据</el-button>
      </div>
    </div>
  </el-card>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'ControlPanel',
  props: {
    isConnected: {
      type: Boolean,
      default: false
    },
    availableTopics: {
      type: Array,
      required: true
    },
    subscribedTopics: {
      type: Array,
      required: true
    }
  },
  emits: ['connect', 'disconnect', 'update:subscribedTopics', 'send-test'],
  setup(props, { emit }) {
    // 使用computed属性实现v-model绑定
    const topicsModel = computed({
      get: () => props.subscribedTopics,
      set: (val) => {
        emit('update:subscribedTopics', val)
      }
    })
    
    // 连接WebSocket
    const connect = () => {
      emit('connect')
    }
    
    // 断开WebSocket连接
    const disconnect = () => {
      emit('disconnect')
    }
    
    // 发送测试数据
    const sendTest = () => {
      emit('send-test')
    }
    
    return {
      topicsModel,
      connect,
      disconnect,
      sendTest
    }
  }
}
</script>

<style scoped>
.control-panel {
  margin-bottom: 20px;
}

.row {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 20px;
}

.topic-selection {
  margin-top: 10px;
}

.mt-2 {
  margin-top: 8px;
}
</style> 