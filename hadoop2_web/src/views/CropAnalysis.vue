<template>
  <div class="crop-analysis-container">
    <header>
      <div class="container">
        <div class="header-content">
          <h1>农作物产量预测分析</h1>
          <p class="subtitle">基于城市、温度和降水量数据的Spark机器学习预测结果</p>
        </div>
      </div>
    </header>
    
    <div class="container">
      <div class="tabs">
        <button class="tab-btn" :class="{ active: activeTab === 'overview' }" @click="setActiveTab('overview')">概览</button>
        <button class="tab-btn" :class="{ active: activeTab === 'climate' }" @click="setActiveTab('climate')">气候影响</button>
        <button class="tab-btn" :class="{ active: activeTab === 'regional' }" @click="setActiveTab('regional')">区域分析</button>
        <button class="tab-btn" :class="{ active: activeTab === 'seasonal' }" @click="setActiveTab('seasonal')">季节模式</button>
      </div>
      
      <div id="overview" class="tab-content" :class="{ active: activeTab === 'overview' }">
        <h2 class="section-title">概览分析</h2>
        <div class="dashboard">
          <div class="card card-large" @click="showImagePreview('/images/city_avg_prediction.png')">
            <img src="/images/city_avg_prediction.png" alt="各城市平均预测产量" class="card-img">
            <div class="card-content">
              <h2>各城市平均预测产量</h2>
              <p class="card-desc">展示了不同城市的平均预测产量，帮助识别最适合农作物生长的地区。</p>
            </div>
          </div>
          <div class="card" @click="showImagePreview('/images/month_avg_prediction.png')">
            <img src="/images/month_avg_prediction.png" alt="月度平均预测产量变化" class="card-img">
            <div class="card-content">
              <h2>月度产量变化趋势</h2>
              <p class="card-desc">显示全年各月份的产量预测变化，帮助了解最佳播种和收获时间。</p>
            </div>
          </div>
          <div class="card" @click="showImagePreview('/images/city_month_heatmap.png')">
            <img src="/images/city_month_heatmap.png" alt="城市-月份产量预测热力图" class="card-img">
            <div class="card-content">
              <h2>城市-月份热力图</h2>
              <p class="card-desc">通过热力图直观展示不同城市在各月份的产量预测情况。</p>
            </div>
          </div>
        </div>
      </div>
      
      <div id="climate" class="tab-content" :class="{ active: activeTab === 'climate' }">
        <h2 class="section-title">气候因素分析</h2>
        <div class="dashboard">
          <div class="card" @click="showImagePreview('/images/temp_prediction_scatter.png')">
            <img src="/images/temp_prediction_scatter.png" alt="温度与预测产量的关系" class="card-img">
            <div class="card-content">
              <h2>温度影响分析</h2>
              <p class="card-desc">散点图展示了温度与作物产量的相关性，揭示最适宜的生长温度范围。</p>
            </div>
          </div>
          <div class="card" @click="showImagePreview('/images/rainfall_prediction_scatter.png')">
            <img src="/images/rainfall_prediction_scatter.png" alt="降水量与预测产量的关系" class="card-img">
            <div class="card-content">
              <h2>降水量影响分析</h2>
              <p class="card-desc">散点图展示了降水量与作物产量的相关性，帮助了解水分需求。</p>
            </div>
          </div>
          <div class="card card-large" @click="showImagePreview('/images/selected_cities_comparison.png')">
            <img src="/images/selected_cities_comparison.png" alt="主要城市月度产量预测对比" class="card-img">
            <div class="card-content">
              <h2>气候区域对比</h2>
              <p class="card-desc">比较不同气候区域城市的月度产量预测，展示气候差异对产量的影响。</p>
            </div>
          </div>
        </div>
      </div>
      
      <div id="regional" class="tab-content" :class="{ active: activeTab === 'regional' }">
        <h2 class="section-title">区域分析</h2>
        <div class="dashboard">
          <div class="card card-large" @click="showImagePreview('/images/state_prediction_boxplot.png')">
            <img src="/images/state_prediction_boxplot.png" alt="各地区产量预测分布" class="card-img">
            <div class="card-content">
              <h2>地区产量分布</h2>
              <p class="card-desc">箱型图展示了不同地区的产量预测分布情况，包括中位数、四分位数和异常值。</p>
            </div>
          </div>
          <div class="card" @click="showImagePreview('/images/city_avg_prediction.png')">
            <img src="/images/city_avg_prediction.png" alt="各城市平均预测产量" class="card-img">
            <div class="card-content">
              <h2>城市产量排名</h2>
              <p class="card-desc">按平均预测产量对城市进行排名，帮助发现最佳种植区域。</p>
            </div>
          </div>
          <div class="card" @click="showImagePreview('/images/city_month_heatmap.png')">
            <img src="/images/city_month_heatmap.png" alt="城市-月份产量预测热力图" class="card-img">
            <div class="card-content">
              <h2>区域季节性模式</h2>
              <p class="card-desc">热力图展示各地区全年的产量模式，揭示区域气候特点。</p>
            </div>
          </div>
        </div>
      </div>
      
      <div id="seasonal" class="tab-content" :class="{ active: activeTab === 'seasonal' }">
        <h2 class="section-title">季节性分析</h2>
        <div class="dashboard">
          <div class="card" @click="showImagePreview('/images/month_avg_prediction.png')">
            <img src="/images/month_avg_prediction.png" alt="月度平均预测产量变化" class="card-img">
            <div class="card-content">
              <h2>季节性变化趋势</h2>
              <p class="card-desc">展示全年各月份的产量变化趋势，帮助制定农业种植计划。</p>
            </div>
          </div>
          <div class="card card-large" @click="showImagePreview('/images/selected_cities_comparison.png')">
            <img src="/images/selected_cities_comparison.png" alt="主要城市月度产量预测对比" class="card-img">
            <div class="card-content">
              <h2>典型城市季节对比</h2>
              <p class="card-desc">对比不同地区城市的季节性产量变化，展示气候差异。</p>
            </div>
          </div>
          <div class="card" @click="showImagePreview('/images/city_month_heatmap.png')">
            <img src="/images/city_month_heatmap.png" alt="城市-月份产量预测热力图" class="card-img">
            <div class="card-content">
              <h2>季节热力图</h2>
              <p class="card-desc">热力图展示不同月份城市产量预测的差异，帮助识别最佳种植时机。</p>
            </div>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 图片预览组件 -->
    <div v-if="previewImage" class="preview-overlay" @click="closeImagePreview">
      <img :src="previewImage" class="preview-image">
    </div>
  </div>
</template>

<script>
export default {
  name: 'CropAnalysis',
  data() {
    return {
      activeTab: 'overview',
      previewImage: null
    }
  },
  methods: {
    setActiveTab(tabName) {
      this.activeTab = tabName;
    },
    showImagePreview(imageSrc) {
      this.previewImage = imageSrc;
    },
    closeImagePreview() {
      this.previewImage = null;
    },
    handleKeyDown(e) {
      if (e.key === 'Escape' && this.previewImage) {
        this.closeImagePreview();
      }
    }
  },
  mounted() {
    document.addEventListener('keydown', this.handleKeyDown);
  },
  beforeUnmount() {
    document.removeEventListener('keydown', this.handleKeyDown);
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+SC:wght@300;400;500;700&display=swap');

:root {
  --primary-color: #3498db;
  --secondary-color: #2ecc71;
  --text-color: #333;
  --light-bg: #f9f9f9;
  --card-shadow: 0 4px 8px rgba(0,0,0,0.1);
  --transition: all 0.3s ease;
}

.crop-analysis-container {
  font-family: 'Noto Sans SC', sans-serif;
  color: var(--text-color);
  background-color: var(--light-bg);
  line-height: 1.6;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

header {
  background: linear-gradient(135deg, #3498db, #2ecc71);
  color: white;
  padding: 40px 0;
  margin-bottom: 30px;
  border-radius: 0 0 20px 20px;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.header-content {
  text-align: center;
  max-width: 800px;
  margin: 0 auto;
}

h1 {
  font-size: 2.5rem;
  margin-bottom: 15px;
}

.subtitle {
  font-size: 1.2rem;
  opacity: 0.9;
}

.dashboard {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 25px;
  margin-top: 20px;
}

.card {
  background: white;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
  transition: all 0.3s ease;
  cursor: pointer;
}

.card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0,0,0,0.15);
}

.card-large {
  grid-column: span 2;
}

.card-img {
  width: 100%;
  height: auto;
  display: block;
  border-bottom: 1px solid #eee;
}

.card-content {
  padding: 20px;
}

h2 {
  font-size: 1.25rem;
  margin-bottom: 10px;
  color: #3498db;
}

.card-desc {
  font-size: 0.95rem;
  color: #666;
}

.tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 30px;
}

.tab-btn {
  padding: 10px 20px;
  margin: 0 5px;
  border: none;
  background-color: white;
  border-radius: 30px;
  cursor: pointer;
  font-family: inherit;
  font-size: 1rem;
  transition: all 0.3s ease;
  box-shadow: 0 4px 8px rgba(0,0,0,0.1);
}

.tab-btn.active {
  background-color: #3498db;
  color: white;
}

.tab-content {
  display: none;
}

.tab-content.active {
  display: block;
}

/* 图片预览样式 */
.preview-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0,0,0,0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  cursor: pointer;
}

.preview-image {
  max-width: 90%;
  max-height: 90%;
  border-radius: 5px;
}

@media (max-width: 768px) {
  .card-large {
    grid-column: span 1;
  }
  
  h1 {
    font-size: 2rem;
  }
  
  .tab-btn {
    padding: 8px 15px;
    font-size: 0.9rem;
  }
  
  .tabs {
    flex-wrap: wrap;
  }
}
</style> 