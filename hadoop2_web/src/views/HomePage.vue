<template>
  <div class="home">
    <!-- 主横幅 -->
    <div class="hero">
      <div class="hero-content">
        <h1 class="hero-title">欢迎使用农业大数据平台</h1>
        <p class="hero-description">本平台整合了HDFS、Hadoop、Hive、Spark和Kafka等大数据技术，为农业数据提供全面的存储、处理和分析能力。</p>
        <div class="hero-actions">
          <router-link to="/hdfs-explorer" class="btn btn-highlight" v-if="isLoggedIn">
            <i class="action-icon file-icon"></i>
            HDFS文件浏览器
          </router-link>
          <router-link to="/hive-explorer" class="btn btn-primary" v-if="isLoggedIn">
            <i class="action-icon chart-icon"></i>
            Hive数据查询
          </router-link>
          <router-link to="/crop-analysis" class="btn btn-outline" v-if="isLoggedIn">
            <i class="action-icon chart-icon"></i>
            产量预测分析
          </router-link>
          <router-link to="/sensor-dashboard" class="btn btn-outline" v-if="isLoggedIn">
            <i class="action-icon sensor-icon"></i>
            实时传感器监控
          </router-link>
          <router-link to="/login" class="btn btn-login" v-if="!isLoggedIn">
            <i class="action-icon login-icon"></i>
            登录系统
          </router-link>
        </div>
      </div>
    </div>
    
    <!-- 服务状态 -->
    <div class="status-section" v-if="isLoggedIn">
      <ServiceStatus/>
    </div>
    <div v-else class="login-prompt">
      <div class="login-content">
        <div class="login-icon">🔐</div>
        <h3>访问更多功能</h3>
        <p>登录后可查看服务状态和使用高级功能</p>
        <router-link to="/login" class="login-btn">立即登录</router-link>
      </div>
    </div>
    
    <!-- 平台功能 -->
    <div class="section-title">
      <h2>平台功能</h2>
      <div class="title-underline"></div>
    </div>
    
    <div class="features">
      <div class="feature-card">
        <div class="feature-icon-wrapper">
          <i class="feature-icon upload-feature-icon"></i>
        </div>
        <h3>HDFS文件管理</h3>
        <p>使用HDFS进行分布式存储，确保数据安全可靠，便于分析和处理。</p>
        <router-link to="/hdfs-explorer" class="feature-link">访问文件浏览器</router-link>
      </div>
      
      <div class="feature-card">
        <div class="feature-icon-wrapper">
          <i class="feature-icon sensor-feature-icon"></i>
        </div>
        <h3>实时数据监控</h3>
        <p>结合Kafka和Spark Streaming，实时监控和分析农业传感器数据，提供直观的可视化界面。</p>
        <router-link to="/sensor-dashboard" class="feature-link">查看实时监控</router-link>
      </div>
      
      <div class="feature-card">
        <div class="feature-icon-wrapper">
          <i class="feature-icon process-feature-icon"></i>
        </div>
        <h3>Hive数据查询</h3>
        <p>利用Hive进行大规模数据查询和分析，支持SQL语法，轻松处理农业大数据。</p>
        <router-link to="/hive-explorer" class="feature-link">开始使用Hive</router-link>
      </div>
      
      <div class="feature-card">
        <div class="feature-icon-wrapper">
          <i class="feature-icon chart-feature-icon"></i>
        </div>
        <h3>产量预测分析</h3>
        <p>基于历史数据的智能算法，预测未来作物产量，辅助农业决策。</p>
        <router-link to="/crop-analysis" class="feature-link">进行产量预测</router-link>
      </div>
    </div>
    
    <!-- 新增数据洞察区 -->
    <div class="insights-section">
      <div class="section-title">
        <h2>农业大数据价值</h2>
        <div class="title-underline"></div>
      </div>
      
      <div class="insights-container">
        <div class="insight-card">
          <div class="insight-icon">📊</div>
          <h3>精准农业</h3>
          <p>通过传感器数据和气象信息，实现精准灌溉、施肥和农药喷洒，提高资源利用效率。</p>
        </div>
        
        <div class="insight-card">
          <div class="insight-icon">🌱</div>
          <h3>产量预测</h3>
          <p>利用历史数据和机器学习模型，预测作物产量，辅助决策和资源规划。</p>
        </div>
        
        <div class="insight-card">
          <div class="insight-icon">🌡️</div>
          <h3>风险管理</h3>
          <p>监控环境变化和作物健康状况，及早发现潜在风险，减少损失。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import ServiceStatus from '@/components/ServiceStatus.vue';

// 状态变量
const isLoggedIn = ref(false);

// 初始化时检查登录状态
onMounted(() => {
  // 通过检查localStorage判断登录状态
  isLoggedIn.value = localStorage.getItem('token') != null;
});
</script>

<style scoped>
.home {
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 0;
  background-color: var(--bg-gray);
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
}

/* 英雄区域 */
.hero {
  background: linear-gradient(135deg, var(--primary-color), var(--primary-dark));
  border-radius: var(--border-radius);
  padding: 3.5rem 2rem;
  margin-bottom: 2.5rem;
  color: white;
  text-align: center;
  position: relative;
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.hero::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url('data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiPgogIDxkZWZzPgogICAgPHBhdHRlcm4gaWQ9InBhdHRlcm4iIHg9IjAiIHk9IjAiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+CiAgICAgIDxwYXRoIGQ9Ik0xMCAwTDIwIDEwTDEwIDIwTDAgMTBaIiBmaWxsPSJub25lIiBzdHJva2U9InJnYmEoMjU1LDI1NSwyNTUsMC4xKSIgc3Ryb2tlLXdpZHRoPSIxIj48L3BhdGg+CiAgICA8L3BhdHRlcm4+CiAgPC9kZWZzPgogIDxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjcGF0dGVybikiPjwvcmVjdD4KPC9zdmc+') center center;
  opacity: 0.2;
}

.hero-content {
  width: 100%;
  max-width: 900px;
  margin: 0 auto;
  z-index: 1;
  position: relative;
}

.hero-title {
  font-size: 2.8rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
  animation: fadeInDown 0.8s ease;
}

.hero-description {
  font-size: 1.2rem;
  line-height: 1.6;
  margin-bottom: 2.5rem;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
  text-shadow: 1px 1px 2px rgba(0, 0, 0, 0.2);
  animation: fadeIn 1s ease 0.3s both;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
  animation: fadeInUp 1s ease 0.5s both;
}

.btn {
  padding: 0.9rem 1.7rem;
  border-radius: var(--border-radius);
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  transition: all 0.3s ease;
  text-decoration: none;
  box-shadow: var(--shadow-md);
}

.btn-primary {
  background-color: white;
  color: var(--primary-color);
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.15);
  background-color: #f8f8f8;
}

.btn-highlight {
  background-color: var(--accent-color);
  color: var(--text-color);
  font-weight: 600;
  padding: 1rem 1.8rem;
}

.btn-highlight:hover {
  background-color: #fff176;
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.2);
}

.btn-outline {
  border: 2px solid rgba(255, 255, 255, 0.8);
  color: white;
  background-color: rgba(255, 255, 255, 0.1);
}

.btn-outline:hover {
  background-color: rgba(255, 255, 255, 0.2);
  transform: translateY(-3px);
  box-shadow: 0 6px 15px rgba(0, 0, 0, 0.15);
}

.btn-login {
  background-color: #2196F3;
  color: white;
}

.btn-login:hover {
  background-color: #1976D2;
  transform: translateY(-3px);
}

.action-icon {
  display: inline-block;
  width: 1.2rem;
  height: 1.2rem;
  margin-right: 0.6rem;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.upload-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%234CAF50'%3E%3Cpath d='M5 20h14v-2H5v2zm0-10h4v6h6v-6h4l-7-7-7 7z'/%3E%3C/svg%3E");
}

.file-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23333333'%3E%3Cpath d='M20 6h-8l-2-2H4c-1.1 0-1.99.9-1.99 2L2 18c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 12H4V8h16v10z'/%3E%3C/svg%3E");
}

.chart-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='white'%3E%3Cpath d='M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z'/%3E%3C/svg%3E");
}

.login-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='white'%3E%3Cpath d='M11 7L9.6 8.4l2.6 2.6H2v2h10.2l-2.6 2.6L11 17l5-5-5-5zm9 12h-8v2h8c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2h-8v2h8v14z'/%3E%3C/svg%3E");
}

/* 标题部分 */
.section-title {
  text-align: center;
  margin: 3rem 0 2rem;
  position: relative;
}

.section-title h2 {
  font-size: 2rem;
  color: var(--text-color);
  margin-bottom: 0.5rem;
  font-weight: 600;
}

.title-underline {
  width: 80px;
  height: 4px;
  background: linear-gradient(to right, var(--primary-color), var(--primary-light));
  margin: 0 auto;
  border-radius: 2px;
}

/* 服务状态部分 */
.status-section {
  margin: 2rem auto;
  max-width: 1200px;
  padding: 0 1rem;
}

.login-prompt {
  margin: 2rem auto;
  max-width: 800px;
  padding: 0 1rem;
}

.login-content {
  background: linear-gradient(135deg, #ffffff, #f8f8f8);
  border-radius: var(--border-radius);
  padding: 2.5rem;
  text-align: center;
  box-shadow: var(--shadow-md);
  border: 1px solid #eaeaea;
}

.login-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  color: var(--primary-color);
}

.login-content h3 {
  font-size: 1.5rem;
  color: var(--text-color);
  margin-bottom: 1rem;
}

.login-content p {
  color: var(--text-light);
  margin-bottom: 1.5rem;
}

.login-btn {
  display: inline-block;
  padding: 0.75rem 2rem;
  background-color: var(--primary-color);
  color: white;
  border-radius: var(--border-radius);
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: var(--shadow-sm);
}

.login-btn:hover {
  background-color: var(--primary-dark);
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

/* 功能卡片 */
.features {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 2rem;
  margin-bottom: 3rem;
  padding: 0 1rem;
}

.feature-card {
  background-color: var(--bg-light);
  border-radius: var(--border-radius);
  padding: 2.5rem 2rem;
  box-shadow: var(--shadow-md);
  transition: all 0.4s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  border: 1px solid rgba(0,0,0,0.05);
}

.feature-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 15px 30px rgba(0, 0, 0, 0.1);
}

.feature-icon-wrapper {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background-color: rgba(76, 175, 80, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
  transition: all 0.3s ease;
}

.feature-card:hover .feature-icon-wrapper {
  background-color: rgba(76, 175, 80, 0.2);
  transform: scale(1.1);
}

.feature-icon {
  width: 40px;
  height: 40px;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.upload-feature-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%234CAF50'%3E%3Cpath d='M19.35 10.04C18.67 6.59 15.64 4 12 4 9.11 4 6.6 5.64 5.35 8.04 2.34 8.36 0 10.91 0 14c0 3.31 2.69 6 6 6h13c2.76 0 5-2.24 5-5 0-2.64-2.05-4.78-4.65-4.96zM14 13v4h-4v-4H7l5-5 5 5h-3z'/%3E%3C/svg%3E");
}

.sensor-feature-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%234CAF50'%3E%3Cpath d='M15.54 5.54L13.77 7.3C14.55 8.16 15 9.29 15 10.5c0 2.48-2.02 4.5-4.5 4.5-2.48 0-4.5-2.02-4.5-4.5 0-2.48 2.02-4.5 4.5-4.5 1.21 0 2.34.46 3.2 1.22l1.76-1.77C14.23 4.13 12.68 3.5 11 3.5c-3.86 0-7 3.14-7 7 0 3.86 3.14 7 7 7 3.86 0 7-3.14 7-7 0-1.68-.62-3.23-1.46-4.46zM15 12c0-1.38-1.12-2.5-2.5-2.5S10 10.62 10 12s1.12 2.5 2.5 2.5c.17 0 .34-.02.5-.05V8.92c-.16-.03-.33-.05-.5-.05-1.68 0-3.1 1.09-3.63 2.6-.26.73-.37 1.52-.29 2.31.26 2.7 2.6 4.72 5.3 4.72 2.94 0 5.32-2.38 5.32-5.32 0-1.1-.33-2.12-.9-2.97V12z'/%3E%3C/svg%3E");
}

.process-feature-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%234CAF50'%3E%3Cpath d='M20 3h-5.5v2h5v2.5h2V5c0-1.1-.9-2-2-2zm0 18h-5.5v2h5c1.1 0 2-.9 2-2v-2.5h-2V21zm-17-2h5.5v2h-5c-1.1 0-2-.9-2-2v-2.5h2V19zM3 5v2.5h2V5h5.5V3h-5c-1.1 0-2 .9-2 2zm8 4c-2.21 0-4 1.79-4 4s1.79 4 4 4 4-1.79 4-4-1.79-4-4-4z'/%3E%3C/svg%3E");
}

.chart-feature-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%234CAF50'%3E%3Cpath d='M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM9 17H7v-7h2v7zm4 0h-2V7h2v10zm4 0h-2v-4h2v4z'/%3E%3C/svg%3E");
}

.feature-card h3 {
  font-size: 1.5rem;
  margin: 0.5rem 0 1rem;
  color: var(--text-color);
  font-weight: 600;
}

.feature-card p {
  color: var(--text-light);
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

.feature-link {
  color: var(--primary-color);
  text-decoration: none;
  font-weight: 600;
  position: relative;
  transition: all 0.3s;
  padding-bottom: 3px;
  margin-top: auto;
}

.feature-link::after {
  content: '';
  position: absolute;
  width: 100%;
  height: 2px;
  bottom: 0;
  left: 0;
  background-color: var(--primary-color);
  transform: scaleX(0);
  transform-origin: center;
  transition: transform 0.3s ease;
}

.feature-link:hover {
  color: var(--primary-dark);
}

.feature-link:hover::after {
  transform: scaleX(1);
}

/* 新增数据洞察区域 */
.insights-section {
  padding: 2rem 1rem 4rem;
  background-color: #f8f8f8;
  border-radius: var(--border-radius);
  margin: 1rem 1rem 3rem;
  box-shadow: var(--shadow-sm);
}

.insights-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 1.5rem;
  margin-top: 2rem;
}

.insight-card {
  background-color: var(--bg-light);
  border-radius: var(--border-radius);
  padding: 1.5rem;
  transition: transform 0.3s ease;
  box-shadow: var(--shadow-sm);
  text-align: center;
}

.insight-card:hover {
  transform: translateY(-5px);
  box-shadow: var(--shadow-md);
}

.insight-icon {
  font-size: 2.5rem;
  margin-bottom: 1rem;
}

.insight-card h3 {
  font-size: 1.3rem;
  margin-bottom: 1rem;
  color: var(--text-color);
}

.insight-card p {
  color: var(--text-light);
  line-height: 1.6;
}

/* 添加动画 */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInDown {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .hero {
    padding: 2rem 1rem;
  }
  
  .hero-title {
    font-size: 2rem;
  }
  
  .hero-actions {
    flex-direction: column;
    gap: 0.8rem;
  }
  
  .btn {
    width: 100%;
    justify-content: center;
  }
  
  .feature-card {
    padding: 1.5rem;
  }
  
  .insight-card {
    padding: 1.2rem;
  }
}
</style> 