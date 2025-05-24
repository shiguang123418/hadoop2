<template>
  <div class="login-container">
    <div class="login-panel">
      <div class="login-left">
        <div class="login-content">
          <div class="login-header">
            <div class="logo-container">
              <div class="logo-icon">🌱</div>
            </div>
            <h1>农业大数据平台</h1>
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
                  autocomplete="username"
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
                  autocomplete="current-password"
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
              <span class="spinner" v-if="loading"></span>
            </button>
          </form>
          
          <div class="login-footer">
            <router-link to="/">返回首页</router-link>
          </div>
        </div>
      </div>
      
      <div class="login-right">
        <div class="login-illustration">
          <div class="illustration-content">
            <h2>欢迎使用农业大数据平台</h2>
            <p>结合现代数据技术，推动农业信息化和现代化发展</p>
            <ul class="feature-list">
              <li><span class="check-icon"></span>高效的数据采集与存储</li>
              <li><span class="check-icon"></span>强大的数据处理与分析能力</li>
              <li><span class="check-icon"></span>直观的数据可视化展示</li>
              <li><span class="check-icon"></span>精准的农业产量预测</li>
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
        console.log('当前Token:', localStorage.getItem('token'));
        console.log('当前User:', localStorage.getItem('user'));
        
        // 确保登录状态更新后再跳转
        if (AuthService.isLoggedIn()) {
          console.log('登录成功，跳转到首页');
          // 延迟100ms确保状态更新
          setTimeout(() => {
            this.$router.push('/');
          }, 100);
        } else {
          console.error('登录成功但token未保存');
          this.errorMessage = '登录状态保存失败，请重试';
        }
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
  background-color: var(--bg-gray);
  padding: 1rem;
  width: 100%;
}

.login-panel {
  display: flex;
  width: 900px;
  overflow: hidden;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-lg);
  background-color: var(--bg-light);
}

.login-left {
  flex: 1;
  padding: 3rem 2.5rem;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-content {
  max-width: 400px;
  margin: 0 auto;
  width: 100%;
}

.login-header {
  text-align: center;
  margin-bottom: 2.5rem;
}

.logo-container {
  margin-bottom: 1.2rem;
  display: flex;
  justify-content: center;
}

.logo-icon {
  font-size: 3.5rem;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.login-header h1 {
  font-size: 1.8rem;
  color: var(--text-color);
  margin: 0 0 0.5rem 0;
  font-weight: 600;
}

.login-subheader {
  color: var(--text-light);
  margin: 0;
  font-size: 1rem;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background-color: rgba(244, 67, 54, 0.1);
  border-radius: var(--border-radius);
  color: #d32f2f;
  font-size: 0.9rem;
  margin-bottom: 1.5rem;
}

.error-icon {
  width: 20px;
  height: 20px;
  display: inline-block;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23d32f2f'%3E%3Cpath d='M11 15h2v2h-2zm0-8h2v6h-2zm.99-5C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: center;
  flex-shrink: 0;
}

.login-form {
  margin-bottom: 2rem;
}

.form-group {
  margin-bottom: 1.5rem;
}

.form-label {
  display: block;
  margin-bottom: 0.5rem;
  color: var(--text-color);
  font-weight: 500;
  font-size: 0.95rem;
}

.input-wrapper {
  position: relative;
}

.input-icon {
  position: absolute;
  left: 1rem;
  top: 50%;
  transform: translateY(-50%);
  width: 20px;
  height: 20px;
  background-repeat: no-repeat;
  background-position: center;
  opacity: 0.7;
}

.user-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%234CAF50'%3E%3Cpath d='M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z'/%3E%3C/svg%3E");
}

.password-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%234CAF50'%3E%3Cpath d='M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z'/%3E%3C/svg%3E");
}

.form-control {
  width: 100%;
  padding: 0.8rem 1rem 0.8rem 2.8rem;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: var(--border-radius);
  font-size: 1rem;
  transition: all 0.3s ease;
  background-color: #f9f9f9;
}

.form-control:focus {
  outline: none;
  border-color: var(--primary-color);
  background-color: white;
  box-shadow: 0 0 0 3px rgba(76, 175, 80, 0.15);
}

.form-control::placeholder {
  color: #aaa;
}

.remember-me {
  display: flex;
  align-items: center;
}

.checkbox-container {
  display: flex;
  align-items: center;
  position: relative;
  padding-left: 30px;
  cursor: pointer;
  font-size: 0.95rem;
  color: var(--text-light);
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
  height: 20px;
  width: 20px;
  background-color: #f1f1f1;
  border-radius: 4px;
}

.checkbox-container:hover input ~ .checkmark {
  background-color: #e9e9e9;
}

.checkbox-container input:checked ~ .checkmark {
  background-color: var(--primary-color);
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
  left: 7px;
  top: 3px;
  width: 5px;
  height: 10px;
  border: solid white;
  border-width: 0 2px 2px 0;
  transform: rotate(45deg);
}

.login-btn {
  width: 100%;
  padding: 0.9rem;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
}

.login-btn:hover {
  background-color: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.15);
}

.login-btn:disabled {
  background-color: #aaa;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.spinner {
  width: 20px;
  height: 20px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  border-top-color: white;
  animation: spin 0.8s linear infinite;
  position: absolute;
  right: 1rem;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.login-footer {
  text-align: center;
  margin-top: 1.5rem;
}

.login-footer a {
  color: var(--primary-color);
  text-decoration: none;
  font-size: 0.9rem;
  transition: color 0.3s;
}

.login-footer a:hover {
  color: var(--primary-dark);
  text-decoration: underline;
}

.login-right {
  flex: 1.1;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
}

.login-illustration {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%;
  padding: 2rem;
  color: white;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 1440 320'%3E%3Cpath fill='rgba(255, 255, 255, 0.1)' fill-opacity='1' d='M0,224L48,213.3C96,203,192,181,288,154.7C384,128,480,96,576,117.3C672,139,768,213,864,218.7C960,224,1056,160,1152,128C1248,96,1344,96,1392,96L1440,96L1440,320L1392,320C1344,320,1248,320,1152,320C1056,320,960,320,864,320C768,320,672,320,576,320C480,320,384,320,288,320C192,320,96,320,48,320L0,320Z'%3E%3C/path%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: bottom;
  background-size: cover;
  position: relative;
}

.login-illustration::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgdmlld0JveD0iMCAwIDYwIDYwIj48ZyBmaWxsLXJ1bGU9ImV2ZW5vZGQiPjxnIGZpbGwtb3BhY2l0eT0iLjE1IiBmaWxsPSIjZmZmIj48cGF0aCBkPSJNNTAgNEg0MmE2IDYgMCAwIDEgMCAxMmg4YTYgNiAwIDAgMSAwLTEyek01MCAyNkg0MmE2IDYgMCAwIDEgMCAxMmg4YTYgNiAwIDAgMSAwLTEyek00MiAxNmg4YTYgNiAwIDAgMSAwIDEyaC04YTYgNiAwIDAgMSAwLTEyek00MiAzNmg4YTYgNiAwIDAgMSAwIDEyaC04YTYgNiAwIDAgMSAwLTEyeiI+PC9wYXRoPjwvZz48L2c+PC9zdmc+');
  opacity: 0.1;
}

.illustration-content {
  z-index: 1;
  max-width: 400px;
  text-align: center;
}

.illustration-content h2 {
  font-size: 1.8rem;
  margin-bottom: 1.5rem;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  font-weight: 600;
}

.illustration-content p {
  margin-bottom: 2rem;
  line-height: 1.6;
  font-size: 1rem;
  opacity: 0.9;
}

.feature-list {
  list-style: none;
  padding: 0;
  margin: 0;
  text-align: left;
}

.feature-list li {
  margin-bottom: 1rem;
  display: flex;
  align-items: center;
  gap: 0.7rem;
}

.check-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background-color: rgba(255, 255, 255, 0.2);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  position: relative;
}

.check-icon::before {
  content: '';
  width: 12px;
  height: 6px;
  border: 2px solid white;
  border-top: none;
  border-right: none;
  transform: rotate(-45deg);
  position: absolute;
  top: 7px;
  left: 5px;
}

@media (max-width: 880px) {
  .login-panel {
    flex-direction: column;
    width: 95%;
    max-width: 500px;
    height: auto;
  }
  
  .login-right {
    display: none;
  }
  
  .login-left {
    padding: 2rem;
  }
}

@media (max-width: 400px) {
  .login-left {
    padding: 1.5rem;
  }
}
</style> 