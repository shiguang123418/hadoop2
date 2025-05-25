<template>
  <div class="profile-container">
    <div class="page-header">
      <h1 class="page-title">个人信息</h1>
      <p class="page-subtitle">查看和编辑您的个人信息</p>
    </div>
    
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <h2>基本信息</h2>
          <el-button type="primary" @click="startEdit" v-if="!editing">编辑</el-button>
          <div v-else>
            <el-button type="success" @click="saveProfile">保存</el-button>
            <el-button @click="cancelEdit">取消</el-button>
          </div>
        </div>
      </template>
      
      <el-form :model="userForm" label-width="100px" :disabled="!editing" ref="profileForm" :rules="rules">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" disabled></el-input>
        </el-form-item>
        
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="userForm.nickname" placeholder="请输入昵称"></el-input>
        </el-form-item>
        
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="userForm.email" placeholder="请输入邮箱"></el-input>
        </el-form-item>
        
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="userForm.phone" placeholder="请输入手机号"></el-input>
        </el-form-item>
        
        <el-form-item label="角色">
          <el-tag type="success" v-if="isAdmin">管理员</el-tag>
          <el-tag v-else>普通用户</el-tag>
        </el-form-item>
        
        <el-form-item label="最后登录">
          <span>{{ lastLogin || '暂无记录' }}</span>
        </el-form-item>
      </el-form>
    </el-card>
    
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <h2>安全设置</h2>
        </div>
      </template>
      
      <div class="security-items">
        <div class="security-item">
          <div class="security-info">
            <h3>密码</h3>
            <p>上次修改时间: {{ passwordLastChanged || '未知' }}</p>
          </div>
          <el-button @click="goToChangePassword">修改密码</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthService from '../services/auth'

export default {
  name: 'Profile',
  setup() {
    const router = useRouter();
    const profileForm = ref(null);
    const editing = ref(false);
    const loading = ref(false);
    
    // 用户表单数据
    const userForm = reactive({
      username: '',
      nickname: '',
      email: '',
      phone: ''
    });
    
    // 原始用户数据备份
    const originalUserData = ref({});
    
    // 最后登录和密码修改时间
    const lastLogin = ref('');
    const passwordLastChanged = ref('');
    
    // 表单验证规则
    const rules = {
      email: [
        { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
      ],
      phone: [
        { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
      ]
    };
    
    // 判断是否是管理员
    const isAdmin = computed(() => {
      return AuthService.isAdmin();
    });
    
    // 加载用户信息
    const loadUserInfo = () => {
      const user = AuthService.getCurrentUser();
      if (user) {
        userForm.username = user.username || '';
        userForm.nickname = user.nickname || '';
        userForm.email = user.email || '';
        userForm.phone = user.phone || '';
        
        // 记录最后登录时间，如果有的话
        if (user.lastLogin) {
          lastLogin.value = new Date(user.lastLogin).toLocaleString();
        }
        
        // 记录密码修改时间，如果有的话
        if (user.passwordLastChanged) {
          passwordLastChanged.value = new Date(user.passwordLastChanged).toLocaleString();
        }
        
        // 备份原始数据
        originalUserData.value = { ...userForm };
      } else {
        // 如果没有用户信息，返回登录页
        ElMessage.warning('您需要先登录');
        router.push('/login');
      }
    };
    
    // 开始编辑
    const startEdit = () => {
      editing.value = true;
    };
    
    // 取消编辑
    const cancelEdit = () => {
      // 恢复原始数据
      Object.assign(userForm, originalUserData.value);
      editing.value = false;
    };
    
    // 保存个人资料
    const saveProfile = async () => {
      try {
        // 表单验证
        await profileForm.value.validate();
        
        loading.value = true;
        
        // 调用更新个人资料API
        const result = await AuthService.updateProfile({
          nickname: userForm.nickname,
          email: userForm.email,
          phone: userForm.phone
        });
        
        // 更新备份的原始数据
        originalUserData.value = { ...userForm };
        
        editing.value = false;
        ElMessage.success('个人信息更新成功');
      } catch (error) {
        if (error && error.name !== 'ValidationError') {
          console.error('更新个人资料失败:', error);
          ElMessage.error('更新失败: ' + (error.message || '未知错误'));
        }
      } finally {
        loading.value = false;
      }
    };
    
    // 跳转到修改密码页面
    const goToChangePassword = () => {
      router.push('/change-password');
    };
    
    // 组件挂载时加载用户信息
    onMounted(() => {
      loadUserInfo();
    });
    
    return {
      profileForm,
      userForm,
      editing,
      loading,
      rules,
      isAdmin,
      lastLogin,
      passwordLastChanged,
      startEdit,
      cancelEdit,
      saveProfile,
      goToChangePassword
    };
  }
}
</script>

<style scoped>
.profile-container {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.page-subtitle {
  color: #606266;
  font-size: 14px;
}

.profile-card {
  margin-bottom: 24px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.security-items {
  padding: 12px 0;
}

.security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #ebeef5;
}

.security-item:last-child {
  border-bottom: none;
}

.security-info h3 {
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 8px 0;
}

.security-info p {
  color: #909399;
  font-size: 13px;
  margin: 0;
}
</style> 