<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <h2>个人资料</h2>
        </div>
      </template>
      
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-width="100px"
        label-position="right"
        status-icon
      >
        <div class="avatar-section">
          <div class="avatar-wrapper">
            <avatar-upload v-model:value="form.avatar" @change="handleAvatarChange" />
            <div class="avatar-actions">
              <el-button type="primary" size="small" @click="refreshAvatar" :loading="refreshing">
                <el-icon><Refresh /></el-icon> 刷新头像
              </el-button>
              <el-button v-if="form.avatar" type="info" size="small" @click="openAvatarInNewTab">
                <el-icon><Link /></el-icon> 查看原图
              </el-button>
            </div>
          </div>
        </div>
        
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" disabled />
        </el-form-item>
        
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" placeholder="请输入您的真实姓名" />
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入您的邮箱" />
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入您的手机号" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="loading">保存修改</el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button type="warning" @click="goToChangePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, onMounted, defineComponent } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import { Refresh, Link } from '@element-plus/icons-vue';
import authService from '../services/auth';
import AvatarUpload from '../components/AvatarUpload.vue';
import axios from 'axios';

export default defineComponent({
  name: 'UserProfile',
  components: {
    AvatarUpload,
    Refresh,
    Link
  },
  setup() {
    const router = useRouter();
    const formRef = ref(null);
    const loading = ref(false);
    const refreshing = ref(false);
    const originalData = ref({});
    
    // 表单数据
    const form = reactive({
      username: '',
      name: '',
      email: '',
      phone: '',
      avatar: ''
    });
    
    // 表单验证规则
    const rules = {
      email: [
        { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
      ],
      phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
      ]
    };
    
    // 加载用户数据
    const loadUserData = async () => {
      try {
        // 首先尝试从服务器获取最新的用户信息
        console.log('尝试从服务器获取最新用户信息...');
        const response = await axios.get('/auth/current', {
          headers: {
            'Authorization': `Bearer ${authService.getToken()}`
          }
        });
        
        console.log('从服务器获取的用户信息:', response.data);
        
        if (response.data && response.data.code === 200 && response.data.data) {
          const serverUserData = response.data.data;
          
          // 更新本地存储的用户信息
          localStorage.setItem('user', JSON.stringify(serverUserData));
          console.log('已更新本地存储的用户信息');
          
          // 使用服务器返回的数据
          form.username = serverUserData.username || '';
          form.name = serverUserData.name || serverUserData.fullName || '';
          form.email = serverUserData.email || '';
          form.phone = serverUserData.phone || '';
          
          // 设置头像
          if (serverUserData.avatar) {
            console.log('服务器返回的头像URL:', serverUserData.avatar);
            form.avatar = serverUserData.avatar;
          } else {
            console.log('服务器返回的用户没有头像');
            form.avatar = '';
          }
          
          // 保存原始数据用于重置
          originalData.value = { ...form };
          console.log('已保存原始数据:', originalData.value);
          return;
        }
      } catch (error) {
        console.error('获取最新用户信息失败:', error);
      }
      
      // 如果从服务器获取失败，则使用本地缓存的数据
      const currentUser = authService.getCurrentUser();
      console.log('使用本地缓存的用户数据:', currentUser);
      
      if (currentUser) {
        form.username = currentUser.username || '';
        form.name = currentUser.name || currentUser.fullName || '';
        form.email = currentUser.email || '';
        form.phone = currentUser.phone || '';
        
        // 设置头像
        if (currentUser.avatar) {
          console.log('本地缓存的头像URL:', currentUser.avatar);
          form.avatar = currentUser.avatar;
        } else {
          console.log('本地缓存的用户没有头像');
          form.avatar = '';
        }
        
        // 保存原始数据用于重置
        originalData.value = { ...form };
        console.log('已保存原始数据:', originalData.value);
      } else {
        ElMessage.error('未获取到用户信息，请重新登录');
        router.push('/login');
      }
    };
    
    // 提交表单
    const submitForm = async () => {
      if (!formRef.value) return;
      
      try {
        await formRef.value.validate();
        
        loading.value = true;
        const profileData = {
          name: form.name,
          email: form.email,
          phone: form.phone,
          avatar: form.avatar
        };
        
        console.log('提交个人资料更新:', profileData);
        
        const response = await authService.updateProfile(profileData);
        
        console.log('更新个人资料响应:', response);
        
        if (response && response.code === 200) {
          ElMessage.success('个人资料更新成功');
          // 更新原始数据
          originalData.value = { ...form };
          
          // 确认头像已更新到本地缓存
          const currentUser = authService.getCurrentUser();
          if (currentUser && currentUser.avatar !== form.avatar) {
            console.log('更新本地用户头像:', form.avatar);
            currentUser.avatar = form.avatar;
            localStorage.setItem('user', JSON.stringify(currentUser));
          }
        } else {
          ElMessage.error(response?.message || '个人资料更新失败');
        }
      } catch (error) {
        console.error('更新个人资料失败:', error);
        ElMessage.error(error.message || '表单验证失败');
      } finally {
        loading.value = false;
      }
    };
    
    // 重置表单
    const resetForm = () => {
      Object.assign(form, originalData.value);
    };
    
    // 头像变更处理
    const handleAvatarChange = (url) => {
      if (!url) return;
      
      console.log('头像已更新，URL:', url);
      form.avatar = url;
      
      // 立即提交头像更新
      saveAvatarToServer(url);
    };
    
    // 保存头像到服务器
    const saveAvatarToServer = async (avatarUrl) => {
      try {
        if (!avatarUrl) return;
        
        // 创建只包含头像的数据对象
        const profileData = {
          avatar: avatarUrl
        };
        
        console.log('准备保存头像到服务器:', avatarUrl);
        
        const response = await authService.updateProfile(profileData);
        
        if (response && response.code === 200) {
          console.log('头像保存成功:', response);
          
          // 确认头像已更新到本地缓存
          const currentUser = authService.getCurrentUser();
          if (currentUser) {
            currentUser.avatar = avatarUrl;
            localStorage.setItem('user', JSON.stringify(currentUser));
            console.log('本地用户头像已更新');
          }
        } else {
          console.error('头像保存失败:', response);
          ElMessage.error('头像保存失败');
        }
      } catch (error) {
        console.error('保存头像失败:', error);
        ElMessage.error('保存头像失败: ' + (error.message || '未知错误'));
      }
    };
    
    // 跳转到修改密码页面
    const goToChangePassword = () => {
      router.push('/change-password');
    };
    
    // 刷新头像
    const refreshAvatar = async () => {
      refreshing.value = true;
      try {
        console.log('正在刷新头像...');
        
        // 先尝试直接访问当前头像URL以检查可访问性
        if (form.avatar) {
          console.log('测试当前头像URL是否可访问:', form.avatar);
          
          const testImage = new Image();
          testImage.onload = () => {
            console.log('当前头像URL可以正常访问');
            ElMessage.success('头像URL有效，正在重新加载用户信息');
          };
          testImage.onerror = () => {
            console.error('当前头像URL无法访问');
            ElMessage.warning('当前头像URL无法访问，将尝试获取新的头像信息');
          };
          testImage.src = form.avatar;
        }
        
        // 重新获取用户信息
        await loadUserData();
        ElMessage.success('用户信息已刷新');
      } catch (error) {
        console.error('刷新头像失败:', error);
        ElMessage.error('刷新头像失败');
      } finally {
        refreshing.value = false;
      }
    };
    
    // 打开头像原图
    const openAvatarInNewTab = () => {
      if (form.avatar) {
        window.open(form.avatar, '_blank');
      } else {
        ElMessage.warning('用户没有头像，无法打开原图');
      }
    };
    
    // 组件挂载时加载用户数据
    onMounted(() => {
      loadUserData();
    });
    
    return {
      formRef,
      form,
      rules,
      loading,
      refreshing,
      submitForm,
      resetForm,
      handleAvatarChange,
      goToChangePassword,
      refreshAvatar,
      openAvatarInNewTab
    };
  }
});
</script>

<style scoped>
.profile-container {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.profile-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.avatar-section {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
  position: relative;
}

.avatar-wrapper {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar-actions {
  margin-top: 10px;
  display: flex;
  justify-content: center;
}

.avatar-url {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  word-break: break-all;
  text-align: center;
  background-color: #f5f7fa;
  padding: 4px;
  border-radius: 4px;
}
</style> 