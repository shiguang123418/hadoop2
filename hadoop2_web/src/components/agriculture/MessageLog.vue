<template>
  <el-card class="message-panel">
    <template #header>
      <div class="message-header">
        <span>消息日志</span>
        <el-button type="primary" size="small" @click="clearMessages">清空</el-button>
      </div>
    </template>
    
    <div class="message-list" ref="messageContainer">
      <div v-for="(message, index) in messages" :key="index" 
           :class="['message', message.type]">
        <strong>{{ message.time }}</strong>: {{ message.content }}
      </div>
    </div>
  </el-card>
</template>

<script>
import { ref, onMounted, watch } from 'vue'

export default {
  name: 'MessageLog',
  props: {
    messages: {
      type: Array,
      default: () => []
    }
  },
  emits: ['clear'],
  setup(props, { emit }) {
    const messageContainer = ref(null)
    
    // 清空消息列表
    const clearMessages = () => {
      emit('clear')
    }
    
    // 滚动到底部
    const scrollToBottom = () => {
      if (messageContainer.value) {
        messageContainer.value.scrollTop = messageContainer.value.scrollHeight
      }
    }
    
    // 监听消息变化，自动滚动到底部
    watch(() => props.messages.length, () => {
      setTimeout(() => {
        scrollToBottom()
      }, 100)
    })
    
    // 组件挂载后滚动到底部
    onMounted(() => {
      scrollToBottom()
    })
    
    return {
      messageContainer,
      clearMessages
    }
  }
}
</script>

<style scoped>
.message-panel {
  margin-bottom: 20px;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-list {
  max-height: 300px;
  overflow-y: auto;
  padding: 10px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.message {
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 4px;
  font-size: 14px;
}

.message.system {
  background-color: #e1f3d8;
  border-left: 3px solid #67c23a;
}

.message.error {
  background-color: #fef0f0;
  border-left: 3px solid #f56c6c;
}

.message.received {
  background-color: #ecf5ff;
  border-left: 3px solid #409eff;
}
</style> 