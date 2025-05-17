<template>
  <div class="login-container">
    <div class="login-panel">
      <div class="login-left">
        <div class="login-content">
          <div class="login-header">
            <div class="logo-container">
              <img src="../assets/logo.svg" alt="Logo" v-if="false">
              <div class="placeholder-logo">🌱</div>
            </div>
            <h2>农业大数据平台</h2>
            <p class="login-subheader">以科技驱动农业发展</p>
          </div>
          
          <div v-if="errorMessage" class="error-message">
            <i class="error-icon"></i>
            {{ errorMessage }}
          </div>
          
          <form @submit.prevent="login" class="login-form">
            <div class="form-group">
              <label for="username" class="form-label">用户名</label>
              <div class="input-wrapper">
                <i class="input-icon user-icon"></i>
                <input 
                  type="text" 
                  id="username" 
                  v-model="username" 
                  class="form-control" 
                  placeholder="请输入用户名"
                  required
                />
              </div>
            </div>
            
            <div class="form-group">
              <label for="password" class="form-label">密码</label>
              <div class="input-wrapper">
                <i class="input-icon password-icon"></i>
                <input 
                  type="password" 
                  id="password" 
                  v-model="password" 
                  class="form-control" 
                  placeholder="请输入密码"
                  required
                />
              </div>
            </div>
            
            <div class="form-group remember-me">
              <label class="checkbox-container">
                <input type="checkbox" v-model="rememberMe" />
                <span class="checkmark"></span>
                记住我
              </label>
            </div>
            
            <button type="submit" class="login-btn" :disabled="loading">
              <span class="btn-text">{{ loading ? '登录中...' : '登录' }}</span>
              <i class="btn-icon" v-if="loading"></i>
            </button>
          </form>
        </div>
      </div>
      
      <div class="login-right">
        <div class="login-illustration">
          <div class="illustration-content">
            <h3>欢迎使用农业大数据平台</h3>
            <p>结合现代数据技术，推动农业信息化和现代化发展</p>
            <ul class="feature-list">
              <li><i class="feature-icon"></i> 高效的数据采集与存储</li>
              <li><i class="feature-icon"></i> 强大的数据处理与分析能力</li>
              <li><i class="feature-icon"></i> 直观的数据可视化展示</li>
            </ul>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import AuthService from '../services/auth'

export default {
  name: 'Login',
  data() {
    return {
      username: '',
      password: '',
      rememberMe: false,
      loading: false,
      errorMessage: ''
    }
  },
  created() {
    // 检查是否有存储的用户信息
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      try {
        const user = JSON.parse(storedUser);
        this.username = user.username || '';
        this.rememberMe = true;
      } catch (e) {
        localStorage.removeItem('user');
      }
    }
    
    // 如果已经登录，直接跳转到首页
    if (AuthService.isLoggedIn()) {
      this.$router.push('/');
    }
  },
  methods: {
    async login() {
      this.loading = true;
      this.errorMessage = '';
      
      try {
        console.log('尝试登录', {
          username: this.username,
          withCredentials: true
        });
        
        // 使用AuthService进行登录
        const response = await AuthService.login(this.username, this.password);
        
        console.log('登录响应:', response);
        
        // 重定向到首页
        this.$router.push('/');
      } catch (error) {
        console.error('登录失败:', error);
        if (error.response) {
          console.error('错误详情:', {
            status: error.response.status,
            statusText: error.response.statusText,
            data: error.response.data,
            headers: error.response.headers
          });
        }
        
        this.errorMessage = error.response?.data?.message || 
                            error.response?.data?.error || 
                            '登录失败，请检查网络连接和服务器状态';
      } finally {
        this.loading = false;
      }
    }
  }
}
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #f9f9f9 !important;
  padding: 1rem;
  width: 100%;
}

.login-panel {
  display: flex;
  width: 900px;
  height: 600px;
  border-radius: var(--border-radius);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.login-left {
  flex: 1;
  background-color: var(--bg-light);
  padding: 2.5rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-content {
  max-width: 400px;
  margin: 0 auto;
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.logo-container {
  margin-bottom: 1rem;
}

.placeholder-logo {
  font-size: 3rem;
  width: 80px;
  height: 80px;
  line-height: 80px;
  margin: 0 auto;
  background-color: var(--primary-light);
  border-radius: 50%;
  color: var(--primary-color);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: var(--shadow);
}

.login-header h2 {
  color: var(--text-color);
  font-size: 1.8rem;
  margin-bottom: 0.5rem;
}

.login-subheader {
  color: var(--text-light);
  font-size: 1rem;
}

.login-form {
  margin-top: 1rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: var(--text-color);
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  width: 1.2rem;
  height: 1.2rem;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.user-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%233498db'%3E%3Cpath d='M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z'/%3E%3C/svg%3E");
}

.password-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%233498db'%3E%3Cpath d='M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z'/%3E%3C/svg%3E");
}

.form-control {
  width: 100%;
  padding: 0.875rem 1rem 0.875rem 3rem;
  border: 1px solid var(--border-color);
  border-radius: var(--border-radius);
  background-color: var(--bg-light);
  transition: var(--transition);
  font-size: 1rem;
}

.form-control:focus {
  outline: none;
  border-color: var(--primary-color);
  box-shadow: 0 0 0 3px rgba(52, 152, 219, 0.15);
}

.remember-me {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
}

.checkbox-container {
  display: inline-flex;
  align-items: center;
  position: relative;
  padding-left: 28px;
  cursor: pointer;
  user-select: none;
}

.checkbox-container input {
  position: absolute;
  opacity: 0;
  cursor: pointer;
  height: 0;
  width: 0;
}

.checkmark {
  position: absolute;
  top: 0;
  left: 0;
  height: 18px;
  width: 18px;
  background-color: var(--bg-light);
  border: 1px solid var(--border-color);
  border-radius: 3px;
  transition: var(--transition);
}

.checkbox-container:hover input ~ .checkmark {
  border-color: var(--primary-color);
}

.checkbox-container input:checked ~ .checkmark {
  background-color: var(--primary-color);
  border-color: var(--primary-color);
}

.checkmark:after {
  content: "";
  position: absolute;
  display: none;
}

.checkbox-container input:checked ~ .checkmark:after {
  display: block;
}

.checkbox-container .checkmark:after {
  left: 6px;
  top: 2px;
  width: 5px;
  height: 10px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.login-btn {
  width: 100%;
  padding: 0.875rem 1rem;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  font-size: 1rem;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.login-btn:hover {
  background-color: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.login-btn:active {
  transform: translateY(0);
}

.login-btn:disabled {
  background-color: rgba(52, 152, 219, 0.7);
  cursor: not-allowed;
}

.btn-icon {
  width: 1.2rem;
  height: 1.2rem;
  margin-left: 0.5rem;
  animation: rotate 1s infinite linear;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' stroke='white' fill='none' stroke-width='2'%3E%3Cpath d='M12 2v4m0 12v4M4.93 4.93l2.83 2.83m8.48 8.48 2.83 2.83M2 12h4m12 0h4M4.93 19.07l2.83-2.83m8.48-8.48 2.83-2.83'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

@keyframes rotate {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(360deg);
  }
}

.login-right {
  flex: 1;
  background-color: var(--primary-color);
  padding: 2.5rem;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  color: white;
}

.login-right::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='100' height='100' viewBox='0 0 100 100'%3E%3Cpath fill='%23ffffff' fill-opacity='0.05' d='M11 18c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm48 25c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm-43-7c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm63 31c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM34 90c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm56-76c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM12 86c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm28-65c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm23-11c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-6 60c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm29 22c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zM32 63c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm57-13c2.76 0 5-2.24 5-5s-2.24-5-5-5-5 2.24-5 5 2.24 5 5 5zm-9-21c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM60 91c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM35 41c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM12 60c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2z'%3E%3C/path%3E%3C/svg%3E");
  z-index: 0;
}

.login-illustration {
  position: relative;
  z-index: 1;
  text-align: center;
}

.illustration-content h3 {
  font-size: 1.5rem;
  margin-bottom: 1rem;
  color: white;
}

.illustration-content p {
  margin-bottom: 2rem;
  line-height: 1.6;
  opacity: 0.9;
}

.feature-list {
  list-style: none;
  padding: 0;
  text-align: left;
  max-width: 300px;
  margin: 0 auto;
}

.feature-list li {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
}

.feature-icon {
  display: inline-block;
  width: 1.2rem;
  height: 1.2rem;
  margin-right: 0.75rem;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='white'%3E%3Cpath d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.error-message {
  background-color: rgba(231, 76, 60, 0.1);
  color: #e74c3c;
  padding: 1rem;
  border-radius: var(--border-radius);
  margin-bottom: 1.5rem;
  display: flex;
  align-items: center;
  border-left: 3px solid #e74c3c;
}

.error-icon {
  width: 1.2rem;
  height: 1.2rem;
  margin-right: 0.75rem;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23e74c3c'%3E%3Cpath d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z'/%3E%3C/svg%3E");
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .login-panel {
    flex-direction: column-reverse;
    height: auto;
    width: 100%;
    max-width: 450px;
  }
  
  .login-right {
    padding: 2rem;
    min-height: 200px;
  }
  
  .login-left {
    padding: 2rem;
  }
  
  .login-content {
    max-width: 100%;
  }
}
</style> 