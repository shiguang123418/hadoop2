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
    
    <el-dialog
      v-model="cropperVisible"
      title="裁剪头像"
      width="500px"
      :close-on-click-modal="false"
      :append-to-body="true"
    >
      <div class="cropper-container">
        <div v-if="cropperSrc" class="cropper-img-container">
          <vue-cropper
            ref="cropper"
            :img="cropperSrc"
            :outputSize="1"
            :outputType="'png'"
            :info="true"
            :full="false"
            :canMove="true"
            :canMoveBox="true"
            :fixedBox="true"
            :autoCrop="true"
            :autoCropWidth="200"
            :autoCropHeight="200"
            :centerBox="true"
            :high="true"
            :infoTrue="true"
          />
        </div>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="cropperVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmCrop">确认</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, defineComponent, computed, onMounted, watch } from 'vue';
import { ElMessage } from 'element-plus';
import { VueCropper } from 'vue-cropper';
import { User, Upload } from '@element-plus/icons-vue';
import fileService from '../services/file';

export default defineComponent({
  name: 'AvatarUpload',
  components: {
    VueCropper,
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
    const cropperVisible = ref(false);
    const cropperSrc = ref('');
    const cropper = ref(null);
    const currentFile = ref(null);
    
    // 当props.value改变时更新预览
    const updatePreview = () => {
      console.log('更新头像预览，原始URL:', props.value);
      
      if (!props.value) {
        console.log('头像URL为空，使用默认头像');
        previewUrl.value = '';
        return;
      }
      
      // 直接使用原始URL，不尝试修复格式
      previewUrl.value = props.value;
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
    
    // 文件选择变化
    const handleFileChange = (e) => {
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
      
      currentFile.value = file;
      
      // 显示图片裁剪器
      const reader = new FileReader();
      reader.onload = (event) => {
        cropperSrc.value = event.target.result;
        cropperVisible.value = true;
      };
      reader.readAsDataURL(file);
      
      // 重置input，允许选择相同文件
      e.target.value = '';
    };
    
    // 确认裁剪
    const confirmCrop = () => {
      if (!cropper.value) return;
      
      cropper.value.getCropBlob(async (blob) => {
        try {
          cropperVisible.value = false;
          loading.value = true;
          
          // 创建File对象
          const file = new File([blob], currentFile.value.name || "avatar.png", { 
            type: 'image/png' 
          });
          
          console.log('准备上传头像文件:', file.name, file.size + '字节');
          
          // 使用专用的头像上传API
          const result = await fileService.uploadAvatar(file);
          
          console.log('头像上传结果:', result);
          
          if (result && result.code === 200) {
            let imageUrl = '';
            
            // 处理不同的返回格式
            if (result.data && typeof result.data === 'object') {
              if (result.data.url) {
                imageUrl = result.data.url;
              } else if (result.data.data && result.data.data.url) {
                imageUrl = result.data.data.url;
              }
            } else if (typeof result.data === 'string') {
              imageUrl = result.data;
            }
            
            if (imageUrl) {
              console.log('获取到头像URL:', imageUrl);
              
              // 更新头像URL
              previewUrl.value = imageUrl;
              emit('update:value', imageUrl);
              emit('change', imageUrl);
              
              // 立即更新个人资料中的头像
              try {
                console.log('正在更新用户资料中的头像...');
                
                // 使用authService统一处理头像更新
                const updateResponse = await fileService.updateUserAvatar(imageUrl);
                
                if (updateResponse && updateResponse.code === 200) {
                  console.log('用户头像已成功更新到个人资料');
                  ElMessage.success('头像上传并保存成功');
                } else {
                  console.warn('更新个人资料失败:', updateResponse);
                  ElMessage.warning('头像已上传，但未能更新到个人资料');
                }
              } catch (error) {
                console.error('更新头像到个人资料失败:', error);
                ElMessage.error('头像上传成功，但更新个人资料失败: ' + (error.message || '未知错误'));
              } finally {
                loading.value = false;
              }
            } else {
              console.error('未找到图片URL', result);
              ElMessage.error('头像上传失败: 返回数据格式错误');
            }
          } else {
            console.error('头像上传失败:', result);
            ElMessage.error(result?.message || '头像上传失败');
          }
        } catch (error) {
          console.error('头像上传失败', error);
          ElMessage.error('头像上传失败: ' + (error.message || '未知错误'));
        }
      });
    };
    
    // 处理图片加载错误
    const handleImageError = () => {
      console.error('头像加载失败，地址:', previewUrl.value);
      ElMessage.warning('头像图片加载失败，请尝试重新上传');
      previewUrl.value = '';
    };
    
    return {
      fileInput,
      loading,
      previewUrl,
      cropperVisible,
      cropperSrc,
      cropper,
      triggerFileInput,
      handleFileChange,
      confirmCrop,
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

.cropper-container {
  width: 100%;
  height: 350px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cropper-img-container {
  width: 100%;
  height: 100%;
}

.dialog-footer {
  padding: 10px 0 0;
  text-align: right;
}

/* 添加vue-cropper的基本样式 */
.vue-cropper {
  position: relative;
  width: 100%;
  height: 100%;
  box-sizing: border-box;
  user-select: none;
  direction: ltr;
  touch-action: none;
  text-align: left;
  background-image: url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQAQMAAAAlPW0iAAAAA3NCSVQICAjb4U/gAAAABlBMVEXMzMz////TjRV2AAAACXBIWXMAAArrAAAK6wGCiw1aAAAAHHRFWHRTb2Z0d2FyZQBBZG9iZSBGaXJld29ya3MgQ1M26LyyjAAAABFJREFUCJlj+M/AgBVhF/0PAH6/D/HkDxOGAAAAAElFTkSuQmCC");
}

.cropper-box, .cropper-box-canvas, .cropper-drag-box, .cropper-crop-box, .cropper-face {
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  user-select: none;
}

.cropper-view-box {
  display: block;
  overflow: hidden;
  width: 100%;
  height: 100%;
  outline: 1px solid #39f;
  outline-color: rgba(51, 153, 255, 0.75);
}
</style> 