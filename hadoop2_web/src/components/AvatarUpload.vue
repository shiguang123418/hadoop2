<template>
  <div class="avatar-upload">
    <div 
      class="avatar-container" 
      :class="{ loading }"
      @click="triggerFileInput"
    >
      <img 
        v-if="previewUrl" 
        :src="previewUrl" 
        alt="用户头像" 
        class="avatar-image"
        @error="handleImageError"
      />
      <el-icon v-else class="avatar-placeholder">
        <User />
      </el-icon>
      
      <div class="avatar-overlay">
        <el-icon><Upload /></el-icon>
        <span>{{ loading ? '上传中...' : '更换头像' }}</span>
      </div>
    </div>
    
    <input 
      ref="fileInput"
      type="file" 
      accept="image/*" 
      class="file-input" 
      @change="handleFileChange"
    />
    
    <div class="avatar-tips">
      <p>支持 JPG、PNG、GIF 格式，小于 5MB</p>
    </div>
  </div>
</template>

<script>
import { ref, defineComponent, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { User, Upload } from '@element-plus/icons-vue';
import fileService from '../services/file';
import avatarUtils from '../utils/avatar';

export default defineComponent({
  name: 'AvatarUpload',
  components: {
    User,
    Upload
  },
  props: {
    value: {
      type: String,
      default: ''
    },
    size: {
      type: Number,
      default: 120
    }
  },
  emits: ['update:value', 'change'],
  setup(props, { emit }) {
    const fileInput = ref(null);
    const loading = ref(false);
    const previewUrl = ref(props.value || '');
    
    // 初始化和更新
    const updatePreview = () => {
      console.log('更新头像预览，原始URL:', props.value);
      
      if (!props.value) {
        console.log('头像URL为空，使用默认头像');
        previewUrl.value = '';
        return;
      }
      
      // 使用avatar工具处理URL
      previewUrl.value = avatarUtils.formatAvatarUrl(props.value);
      console.log('设置头像预览URL:', previewUrl.value);
    };
    
    onMounted(() => {
      console.log('AvatarUpload组件挂载，初始头像:', props.value);
      updatePreview();
    });
    
    // 监听props.value变化
    watch(() => props.value, (newValue) => {
      console.log('头像value属性变化:', newValue);
      updatePreview();
    });
    
    // 触发文件选择
    const triggerFileInput = () => {
      if (loading.value) return;
      fileInput.value.click();
    };
    
    // 文件选择变化 - 直接上传文件
    const handleFileChange = async (e) => {
      const file = e.target.files[0];
      if (!file) return;
      
      // 检查文件类型
      if (!file.type.startsWith('image/')) {
        ElMessage.error('请选择图片文件');
        return;
      }
      
      // 检查文件大小
      if (file.size > 5 * 1024 * 1024) {
        ElMessage.error('图片大小不能超过5MB');
        return;
      }
      
      // 直接上传文件
      try {
        loading.value = true;
        console.log('开始上传头像文件:', file.name, file.size + '字节');
        
        const result = await fileService.uploadAvatar(file);
        console.log('头像上传结果:', result);
        
        if (result && result.code === 200) {
          let imageUrl = '';
          
          if (typeof result.data === 'string') {
            imageUrl = result.data;
          } else if (result.data && typeof result.data === 'object') {
            // 尝试从不同字段获取URL或文件名
            if (result.data.url) {
              imageUrl = result.data.url;
            } else if (result.data.path) {
              imageUrl = result.data.path;
            } else if (result.data.filename) {
              // 使用工具类根据文件名构建URL
              imageUrl = avatarUtils.buildOssAvatarUrl(result.data.filename);
            }
          }
          
          if (imageUrl) {
            console.log('获取到头像URL:', imageUrl);
            
            // 使用工具类处理URL
            const formattedUrl = avatarUtils.formatAvatarUrl(imageUrl);
            console.log('格式化后的URL:', formattedUrl);
            
            // 更新显示和发送事件
            previewUrl.value = formattedUrl;
            emit('update:value', imageUrl); // 发送原始URL给父组件
            emit('change', imageUrl);
            
            ElMessage.success('头像上传成功');
          } else {
            console.error('未能从响应中提取URL:', result);
            ElMessage.warning('头像已上传，但未能获取URL');
            
            // 尝试从完整响应中提取文件名
            if (result.data && result.data.filename) {
              const filename = result.data.filename;
              const fallbackUrl = avatarUtils.buildOssAvatarUrl(filename);
              console.log('尝试使用文件名构建URL:', fallbackUrl);
              
              previewUrl.value = fallbackUrl;
              emit('update:value', fallbackUrl);
              emit('change', fallbackUrl);
              
              ElMessage.success('头像上传成功');
            }
          }
        } else {
          console.error('头像上传失败:', result);
          ElMessage.error(result?.message || '头像上传失败');
        }
      } catch (error) {
        console.error('头像上传过程出错:', error);
        ElMessage.error('上传失败: ' + (error.message || '未知错误'));
      } finally {
        loading.value = false;
        // 重置文件输入以允许重新选择同一文件
        e.target.value = '';
      }
    };
    
    // 处理图片加载错误
    const handleImageError = () => {
      console.error('头像加载失败，地址:', previewUrl.value);
      
      // 使用工具类刷新URL
      const refreshedUrl = avatarUtils.refreshAvatarUrl(previewUrl.value);
      console.log('尝试使用刷新后的URL加载头像:', refreshedUrl);
      previewUrl.value = refreshedUrl;
    };
    
    return {
      fileInput,
      loading,
      previewUrl,
      triggerFileInput,
      handleFileChange,
      handleImageError
    };
  }
});
</script>

<style scoped>
.avatar-upload {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-container {
  position: relative;
  width: v-bind(size + 'px');
  height: v-bind(size + 'px');
  border-radius: 50%;
  overflow: hidden;
  background-color: #f0f2f5;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2px solid #ebeef5;
  transition: all 0.3s;
}

.avatar-container:hover .avatar-overlay {
  opacity: 1;
}

.avatar-container.loading {
  opacity: 0.7;
  cursor: not-allowed;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.avatar-placeholder {
  font-size: 40px;
  color: #c0c4cc;
}

.avatar-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
  color: white;
}

.avatar-overlay .el-icon {
  font-size: 24px;
  margin-bottom: 8px;
}

.file-input {
  display: none;
}

.avatar-tips {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}
</style> 