<template>
  <div class="home">
    <!-- 主横幅 -->
    <div class="hero">
      <div class="hero-content">
        <h2 class="hero-title">欢迎使用农业大数据平台</h2>
        <p class="hero-description">本平台整合了HDFS、Hadoop、Hive、Spark和Kafka等大数据技术，为农业数据提供全面的存储、处理和分析能力。</p>
        <div class="hero-actions">
          <router-link to="/data-upload" class="btn btn-primary">
            <i class="action-icon upload-icon"></i>
            开始上传数据
          </router-link>
          <router-link to="/spark-dashboard" class="btn btn-highlight">
            <i class="action-icon dashboard-icon"></i>
            农业传感器监控平台
          </router-link>
          <router-link to="/data-analysis" class="btn btn-outline">
            <i class="action-icon chart-icon"></i>
            查看数据分析
          </router-link>
        </div>
      </div>
    </div>
    
    <!-- 平台功能 -->
    <div class="section-title">
      <h3>平台功能</h3>
      <div class="title-underline"></div>
    </div>
    
    <div class="features">
      <div class="feature-card">
        <div class="feature-icon-wrapper">
          <i class="feature-icon upload-feature-icon"></i>
        </div>
        <h3>数据采集与存储</h3>
        <p>使用Flume进行数据采集，HDFS进行分布式存储，确保数据安全可靠。</p>
        <router-link to="/data-upload" class="feature-link">了解更多</router-link>
      </div>
      
      <div class="feature-card">
        <div class="feature-icon-wrapper">
          <i class="feature-icon sensor-feature-icon"></i>
        </div>
        <h3>农业传感器监控</h3>
        <p>结合Kafka和Spark Streaming，实时监控和分析农业传感器数据，提供直观的可视化界面。</p>
        <router-link to="/spark-dashboard" class="feature-link">查看监控平台</router-link>
      </div>
      
      <div class="feature-card">
        <div class="feature-icon-wrapper">
          <i class="feature-icon process-feature-icon"></i>
        </div>
        <h3>数据处理与分析</h3>
        <p>利用Hadoop MapReduce进行大规模数据处理，Hive进行数据查询和分析。</p>
        <router-link to="/data-analysis" class="feature-link">了解更多</router-link>
      </div>
      
      <div class="feature-card">
        <div class="feature-icon-wrapper">
          <i class="feature-icon chart-feature-icon"></i>
        </div>
        <h3>数据可视化</h3>
        <p>直观的图表和仪表盘，帮助您理解农业数据趋势和模式。</p>
        <router-link to="/data-visualization" class="feature-link">了解更多</router-link>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HomePage'
}
</script>

<style scoped>
.home {
  width: 100%;
  max-width: 100%;
  margin: 0;
  padding: 0;
  background-color: #f9f9f9;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
}

/* 英雄区域 */
.hero {
  background: linear-gradient(135deg, #4CAF50, #2E7D32);
  border-radius: 8px;
  padding: 3rem;
  margin-bottom: 3rem;
  color: white;
  display: flex;
  position: relative;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.hero-content {
  width: 100%;
  z-index: 1;
  text-align: center;
}

.hero-title {
  font-size: 2.5rem;
  font-weight: 700;
  margin-bottom: 1.5rem;
}

.hero-description {
  font-size: 1.1rem;
  line-height: 1.6;
  margin-bottom: 2rem;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.hero-actions {
  display: flex;
  gap: 1rem;
  justify-content: center;
  flex-wrap: wrap;
}

.btn {
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  transition: all 0.3s ease;
  text-decoration: none;
}

.btn-primary {
  background-color: white;
  color: #4CAF50;
}

.btn-highlight {
  background-color: #ffeb3b;
  color: #333;
  font-weight: bold;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  padding: 0.85rem 1.7rem;
}

.btn-highlight:hover {
  background-color: #fff176;
  transform: translateY(-3px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}

.btn-primary:hover {
  transform: translateY(-3px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.btn-outline {
  border: 2px solid rgba(255, 255, 255, 0.7);
  color: white;
}

.btn-outline:hover {
  background-color: rgba(255, 255, 255, 0.1);
  transform: translateY(-3px);
}

.action-icon {
  display: inline-block;
  width: 1.2rem;
  height: 1.2rem;
  margin-right: 0.5rem;
  background-size: contain;
  background-repeat: no-repeat;
  background-position: center;
}

.upload-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%234CAF50'%3E%3Cpath d='M5 20h14v-2H5v2zm0-10h4v6h6v-6h4l-7-7-7 7z'/%3E%3C/svg%3E");
}

.dashboard-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='%23333333'%3E%3Cpath d='M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 17h-2v-2h2v2zm2.07-7.75l-.9.92C13.45 12.9 13 13.5 13 15h-2v-.5c0-1.1.45-2.1 1.17-2.83l1.24-1.26c.37-.36.59-.86.59-1.41 0-1.1-.9-2-2-2s-2 .9-2 2H8c0-2.21 1.79-4 4-4s4 1.79 4 4c0 .88-.36 1.68-.93 2.25z'/%3E%3C/svg%3E");
}

.chart-icon {
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='white'%3E%3Cpath d='M3.5 18.49l6-6.01 4 4L22 6.92l-1.41-1.41-7.09 7.97-4-4L2 16.99z'/%3E%3C/svg%3E");
}

/* 标题部分 */
.section-title {
  text-align: center;
  margin: 3rem 0 2rem;
  position: relative;
}

.section-title h3 {
  font-size: 1.8rem;
  color: #333;
  margin-bottom: 0.5rem;
}

.title-underline {
  width: 80px;
  height: 4px;
  background: linear-gradient(to right, #4CAF50, #8BC34A);
  margin: 0 auto;
  border-radius: 2px;
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
  background-color: white;
  border-radius: 8px;
  padding: 2rem;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.feature-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.1);
}

.feature-icon-wrapper {
  width: 70px;
  height: 70px;
  border-radius: 50%;
  background-color: rgba(76, 175, 80, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 1.5rem;
}

.feature-icon {
  width: 35px;
  height: 35px;
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
  margin-bottom: 1rem;
  color: #333;
}

.feature-card p {
  color: #666;
  margin-bottom: 1.5rem;
  line-height: 1.6;
}

.feature-link {
  color: #4CAF50;
  text-decoration: none;
  font-weight: 500;
  position: relative;
  transition: all 0.2s;
}

.feature-link::after {
  content: '';
  position: absolute;
  width: 100%;
  height: 2px;
  bottom: -2px;
  left: 0;
  background-color: #4CAF50;
  transform: scaleX(0);
  transition: transform 0.3s;
}

.feature-link:hover::after {
  transform: scaleX(1);
}

@media (max-width: 768px) {
  .hero {
    padding: 2rem;
  }
  
  .hero-title {
    font-size: 2rem;
  }
  
  .hero-actions {
    flex-direction: column;
    gap: 1rem;
  }
  
  .btn {
    width: 100%;
    justify-content: center;
  }
}
</style> 