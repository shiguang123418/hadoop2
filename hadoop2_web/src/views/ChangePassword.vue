<template>
  <div class="change-password-container">
    <div class="page-header">
      <h1 class="page-title">修改密码</h1>
      <p class="page-subtitle">请设置一个安全的新密码</p>
    </div>
    
    <el-card class="password-card">
      <el-form 
        :model="passwordForm" 
        :rules="rules" 
        ref="passwordFormRef" 
        label-width="120px"
        @submit.prevent="submitForm"
      >
        <el-form-item label="当前密码" prop="currentPassword">
          <el-input 
            v-model="passwordForm.currentPassword" 
            type="password" 
            placeholder="请输入当前密码"
            show-password
          ></el-input>
        </el-form-item>
        
        <el-form-item label="新密码" prop="newPassword">
          <el-input 
            v-model="passwordForm.newPassword" 
            type="password" 
            placeholder="请输入新密码"
            show-password
          ></el-input>
          <div class="password-strength" v-if="passwordForm.newPassword">
            <div class="strength-label">密码强度:</div>
            <div class="strength-bar">
              <div 
                class="strength-indicator" 
                :style="{ width: passwordStrength.percent + '%' }"
                :class="passwordStrength.level"
              ></div>
            </div>
            <div class="strength-text" :class="passwordStrength.level">
              {{ passwordStrength.text }}
            </div>
          </div>
          <div class="password-tips">
            <p>密码要求（至少满足2项）：</p>
            <ul>
              <li :class="{ 'valid': passwordLength }">至少6个字符</li>
              <li :class="{ 'valid': passwordHasNumber }">包含至少一个数字</li>
              <li :class="{ 'valid': passwordHasLetter }">包含字母</li>
              <li :class="{ 'valid': passwordHasSpecial }">包含特殊字符</li>
            </ul>
          </div>
        </el-form-item>
        
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input 
            v-model="passwordForm.confirmPassword" 
            type="password" 
            placeholder="请再次输入新密码"
            show-password
          ></el-input>
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" native-type="submit" :loading="loading">修改密码</el-button>
          <el-button @click="goBack">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { ref, reactive, computed, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AuthService from '../services/auth'

export default {
  name: 'ChangePassword',
  setup() {
    const router = useRouter();
    const passwordFormRef = ref(null);
    const loading = ref(false);
    
    // 密码表单
    const passwordForm = reactive({
      currentPassword: '',
      newPassword: '',
      confirmPassword: ''
    });
    
    // 密码强度校验标志
    const passwordLength = ref(false);
    const passwordHasNumber = ref(false);
    const passwordHasLetter = ref(false);
    const passwordHasSpecial = ref(false);
    
    // 监听密码变化，实时更新密码强度指标
    watch(() => passwordForm.newPassword, (newVal) => {
      passwordLength.value = newVal.length >= 6;
      passwordHasNumber.value = /\d/.test(newVal);
      passwordHasLetter.value = /[a-zA-Z]/.test(newVal);
      passwordHasSpecial.value = /[!@#$%^&*(),.?":{}|<>]/.test(newVal);
    });
    
    // 计算密码强度
    const passwordStrength = computed(() => {
      if (!passwordForm.newPassword) {
        return { level: 'none', percent: 0, text: '无' };
      }
      
      // 计算满足的条件数量
      let score = 0;
      if (passwordLength.value) score++;
      if (passwordHasNumber.value) score++;
      if (passwordHasLetter.value) score++;
      if (passwordHasSpecial.value) score++;
      
      // 根据得分返回强度
      if (score === 0) return { level: 'none', percent: 0, text: '无' };
      if (score === 1) return { level: 'weak', percent: 25, text: '弱' };
      if (score === 2) return { level: 'medium', percent: 50, text: '中' };
      if (score === 3) return { level: 'strong', percent: 75, text: '强' };
      return { level: 'very-strong', percent: 100, text: '非常强' };
    });
    
    // 表单验证规则
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'));
      } else {
        callback();
      }
    };
    
    const rules = {
      currentPassword: [
        { required: true, message: '请输入当前密码', trigger: 'blur' }
      ],
      newPassword: [
        { required: true, message: '请输入新密码', trigger: 'blur' },
        { min: 6, message: '密码长度至少为6个字符', trigger: 'blur' },
        { 
          validator: (rule, value, callback) => {
            if (value === passwordForm.currentPassword) {
              callback(new Error('新密码不能与当前密码相同'));
            } else {
              callback();
            }
          }, 
          trigger: 'blur' 
        },
        { 
          validator: (rule, value, callback) => {
            const score = [
              passwordLength.value,
              passwordHasNumber.value,
              passwordHasLetter.value,
              passwordHasSpecial.value
            ].filter(Boolean).length;
            
            if (score < 2) {
              callback(new Error('密码强度不足，请满足至少2项密码要求'));
            } else {
              callback();
            }
          }, 
          trigger: 'blur' 
        }
      ],
      confirmPassword: [
        { required: true, message: '请再次输入新密码', trigger: 'blur' },
        { validator: validateConfirmPassword, trigger: 'blur' }
      ]
    };
    
    // 提交表单
    const submitForm = async () => {
      try {
        await passwordFormRef.value.validate();
        
        loading.value = true;
        await AuthService.changePassword(
          passwordForm.currentPassword,
          passwordForm.newPassword
        );
        
        ElMessage.success('密码修改成功');
        
        // 清空表单
        passwordForm.currentPassword = '';
        passwordForm.newPassword = '';
        passwordForm.confirmPassword = '';
        
        // 返回个人信息页面
        setTimeout(() => {
          router.push('/profile');
        }, 1500);
      } catch (error) {
        if (error?.name !== 'ValidationError') {
          console.error('修改密码失败:', error);
          ElMessage.error('修改密码失败: ' + (error.message || '未知错误'));
        }
      } finally {
        loading.value = false;
      }
    };
    
    // 返回上一页
    const goBack = () => {
      router.push('/profile');
    };
    
    return {
      passwordFormRef,
      passwordForm,
      loading,
      rules,
      passwordStrength,
      passwordLength,
      passwordHasNumber,
      passwordHasLetter,
      passwordHasSpecial,
      submitForm,
      goBack
    };
  }
}
</script>

<style scoped>
.change-password-container {
  max-width: 600px;
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

.password-card {
  border-radius: 8px;
}

.password-strength {
  margin-top: 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}

.strength-label {
  margin-right: 10px;
  color: #606266;
  font-size: 13px;
}

.strength-bar {
  flex: 1;
  height: 6px;
  background-color: #e4e7ed;
  border-radius: 3px;
  overflow: hidden;
  margin-right: 10px;
}

.strength-indicator {
  height: 100%;
  transition: all 0.3s;
}

.strength-indicator.none {
  width: 0;
}

.strength-indicator.weak {
  background-color: #f56c6c;
}

.strength-indicator.medium {
  background-color: #e6a23c;
}

.strength-indicator.strong {
  background-color: #67c23a;
}

.strength-indicator.very-strong {
  background-color: #409eff;
}

.strength-text {
  font-size: 13px;
  width: 50px;
  text-align: center;
}

.strength-text.weak {
  color: #f56c6c;
}

.strength-text.medium {
  color: #e6a23c;
}

.strength-text.strong {
  color: #67c23a;
}

.strength-text.very-strong {
  color: #409eff;
}

.password-tips {
  margin-top: 12px;
  padding: 8px 12px;
  background-color: #f8f9fa;
  border-radius: 4px;
}

.password-tips p {
  margin: 0 0 5px 0;
  font-size: 13px;
  color: #606266;
}

.password-tips ul {
  margin: 0;
  padding-left: 20px;
  font-size: 12px;
  color: #909399;
}

.password-tips li {
  margin-bottom: 3px;
}

.password-tips li.valid {
  color: #67c23a;
}

.password-tips li.valid::marker {
  color: #67c23a;
}
</style> 