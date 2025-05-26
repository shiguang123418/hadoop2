<template>
  <div class="change-password-container">
    <el-card class="password-card">
      <template #header>
        <div class="card-header">
          <h2>修改密码</h2>
        </div>
      </template>
      
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-width="120px"
        label-position="right"
        status-icon
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input 
            v-model="form.currentPassword" 
            type="password" 
            placeholder="请输入当前密码" 
            show-password
          />
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="form.newPassword" 
            type="password" 
            placeholder="请输入新密码" 
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input 
            v-model="form.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码" 
            show-password
          />
        </el-form-item>
        
        <div class="password-tips">
          <p>密码要求：</p>
          <ul>
            <li>至少6个字符</li>
            <li>包含字母和数字</li>
            <li>区分大小写</li>
          </ul>
        </div>
        
        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="loading">确认修改</el-button>
          <el-button @click="resetForm">重置</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, defineComponent } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import authService from '../services/auth';

export default defineComponent({
  name: 'ChangePassword',
  setup() {
    const router = useRouter();
    const formRef = ref(null);
    const loading = ref(false);
    
    // 表单数据
    const form = reactive({
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    });
    
    // 自定义验证器：确认密码
    const validateConfirmPassword = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请再次输入新密码'));
      } else if (value !== form.newPassword) {
        callback(new Error('两次输入的密码不一致'));
      } else {
        callback();
      }
    };
    
    // 密码强度验证
    const validatePasswordStrength = (rule, value, callback) => {
      if (value === '') {
        callback(new Error('请输入新密码'));
      } else if (value.length < 6) {
        callback(new Error('密码长度不能少于6个字符'));
      } else if (!/(?=.*[A-Za-z])(?=.*\d)/.test(value)) {
        callback(new Error('密码必须包含字母和数字'));
      } else {
        callback();
      }
    };
    
    // 表单验证规则
    const rules = {
      currentPassword: [
        { required: true, message: '请输入当前密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { validator: validatePasswordStrength, trigger: 'blur' }
      ],
      confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
      ]
    };
    
    // 提交表单
    const submitForm = async () => {
      if (!formRef.value) return;
      
      try {
        await formRef.value.validate();
        
        // 检查新旧密码是否相同
        if (form.currentPassword === form.newPassword) {
          ElMessage.warning('新密码不能与当前密码相同');
          return;
        }
        
        loading.value = true;
        
        const response = await authService.changePassword(
          form.currentPassword,
          form.newPassword
        );
        
        if (response && response.code === 200) {
          ElMessage.success('密码修改成功，请使用新密码重新登录');
          // 退出登录，返回登录页
          authService.logout();
          // 清除页面上的用户信息
          window.dispatchEvent(new Event('user-info-updated'));
          router.push('/login');
        } else {
          ElMessage.error(response?.message || '密码修改失败');
        }
      } catch (error) {
        console.error('修改密码失败:', error);
        ElMessage.error(error.message || '表单验证失败');
      } finally {
        loading.value = false;
      }
    };
    
    // 重置表单
    const resetForm = () => {
      formRef.value.resetFields();
    };
    
    // 返回上一页
    const goBack = () => {
      router.push('/profile');
    };
    
    return {
      formRef,
      form,
      rules,
      loading,
      submitForm,
      resetForm,
      goBack
    };
  }
});
</script>

<style scoped>
.change-password-container {
  padding: 20px;
  max-width: 600px;
  margin: 0 auto;
}

.password-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.password-tips {
  background-color: #f8f9fa;
  padding: 10px 15px;
  border-radius: 4px;
  margin: 10px 0 20px;
  font-size: 14px;
  color: #606266;
}

.password-tips p {
  margin-bottom: 5px;
  font-weight: bold;
}

.password-tips ul {
  margin: 0;
  padding-left: 20px;
}
</style> 