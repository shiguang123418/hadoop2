<template>
  <div class="system-settings">
    <div class="page-header">
      <h1>系统设置</h1>
    </div>

    <div class="settings-tabs">
      <div class="tab-nav">
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'general' }"
          @click="activeTab = 'general'"
        >
          <i class="tab-icon">⚙️</i>
          基本设置
        </div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'services' }"
          @click="activeTab = 'services'"
        >
          <i class="tab-icon">🖥️</i>
          服务配置
        </div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'maintenance' }"
          @click="activeTab = 'maintenance'"
        >
          <i class="tab-icon">🔧</i>
          系统维护
        </div>
      </div>

      <div class="tab-content">
        <!-- 基本设置 -->
        <div v-if="activeTab === 'general'" class="tab-panel">
          <h2>基本设置</h2>
          
          <div class="settings-group">
            <h3>系统信息</h3>
            <div class="form-group">
              <label>系统名称</label>
              <input type="text" v-model="settings.systemName" />
            </div>
            <div class="form-group">
              <label>系统版本</label>
              <input type="text" v-model="settings.systemVersion" disabled />
            </div>
            <div class="form-group">
              <label>系统描述</label>
              <textarea v-model="settings.systemDescription" rows="3"></textarea>
            </div>
          </div>

          <div class="settings-group">
            <h3>网站设置</h3>
            <div class="form-group">
              <label>网站标题</label>
              <input type="text" v-model="settings.siteTitle" />
            </div>
            <div class="form-group">
              <label>网站Logo</label>
              <div class="file-upload">
                <input type="file" id="logo-upload" accept="image/*" />
                <label for="logo-upload" class="upload-btn">选择图片</label>
                <span class="file-name">{{ settings.siteLogo || '未选择图片' }}</span>
              </div>
            </div>
            <div class="form-group">
              <label>主题颜色</label>
              <div class="color-selector">
                <div 
                  v-for="color in themeColors" 
                  :key="color.value"
                  class="color-option"
                  :class="{ active: settings.themeColor === color.value }"
                  :style="{ backgroundColor: color.value }"
                  @click="settings.themeColor = color.value"
                ></div>
              </div>
            </div>
          </div>
          
          <div class="settings-group">
            <h3>邮件设置</h3>
            <div class="form-group">
              <label>SMTP服务器</label>
              <input type="text" v-model="settings.smtpServer" />
            </div>
            <div class="form-group">
              <label>SMTP端口</label>
              <input type="number" v-model="settings.smtpPort" />
            </div>
            <div class="form-group">
              <label>发件人邮箱</label>
              <input type="email" v-model="settings.smtpEmail" />
            </div>
            <div class="form-group">
              <label>SMTP密码</label>
              <input type="password" v-model="settings.smtpPassword" />
            </div>
            <button class="btn btn-secondary" @click="testEmailSettings">
              测试邮件设置
            </button>
          </div>
          
          <div class="form-actions">
            <button class="btn btn-primary" @click="saveGeneralSettings">保存设置</button>
          </div>
        </div>
        
        <!-- 服务配置 -->
        <div v-if="activeTab === 'services'" class="tab-panel">
          <h2>服务配置</h2>
          
          <div class="settings-group">
            <h3>HDFS配置</h3>
            <div class="form-group">
              <label>HDFS URI</label>
              <input type="text" v-model="serviceSettings.hdfs.uri" />
            </div>
            <div class="form-group">
              <label>默认副本数</label>
              <input type="number" v-model="serviceSettings.hdfs.replication" min="1" max="10" />
            </div>
            <div class="form-group">
              <label>块大小 (MB)</label>
              <input type="number" v-model="serviceSettings.hdfs.blockSize" min="16" max="256" />
            </div>
          </div>
          
          <div class="settings-group">
            <h3>Hive配置</h3>
            <div class="form-group">
              <label>Hive URL</label>
              <input type="text" v-model="serviceSettings.hive.url" />
            </div>
            <div class="form-group">
              <label>默认数据库</label>
              <input type="text" v-model="serviceSettings.hive.defaultDatabase" />
            </div>
            <div class="form-group">
              <label>认证方式</label>
              <select v-model="serviceSettings.hive.auth">
                <option value="none">无认证</option>
                <option value="ldap">LDAP</option>
                <option value="kerberos">Kerberos</option>
              </select>
            </div>
          </div>
          
          <div class="settings-group">
            <h3>Kafka配置</h3>
            <div class="form-group">
              <label>Kafka引导服务器</label>
              <input type="text" v-model="serviceSettings.kafka.bootstrapServers" />
            </div>
            <div class="form-group">
              <label>默认消费者组</label>
              <input type="text" v-model="serviceSettings.kafka.consumerGroup" />
            </div>
          </div>
          
          <div class="form-actions">
            <button class="btn btn-primary" @click="saveServiceSettings">保存服务设置</button>
          </div>
        </div>
        
        <!-- 系统维护 -->
        <div v-if="activeTab === 'maintenance'" class="tab-panel">
          <h2>系统维护</h2>
          
          <div class="settings-group">
            <h3>系统日志</h3>
            <div class="form-group">
              <label>日志级别</label>
              <select v-model="maintenanceSettings.logLevel">
                <option value="debug">Debug</option>
                <option value="info">Info</option>
                <option value="warn">Warning</option>
                <option value="error">Error</option>
              </select>
            </div>
            <div class="form-group">
              <label>日志保留天数</label>
              <input type="number" v-model="maintenanceSettings.logRetentionDays" min="1" max="365" />
            </div>
            <button class="btn btn-secondary" @click="viewSystemLogs">
              查看系统日志
            </button>
          </div>
          
          <div class="settings-group">
            <h3>数据备份</h3>
            <div class="form-group">
              <label>自动备份</label>
              <div class="toggle-switch">
                <input type="checkbox" id="auto-backup" v-model="maintenanceSettings.autoBackup" />
                <label for="auto-backup"></label>
              </div>
            </div>
            <div class="form-group" v-if="maintenanceSettings.autoBackup">
              <label>备份频率</label>
              <select v-model="maintenanceSettings.backupFrequency">
                <option value="daily">每天</option>
                <option value="weekly">每周</option>
                <option value="monthly">每月</option>
              </select>
            </div>
            <div class="form-group" v-if="maintenanceSettings.autoBackup">
              <label>备份保留数</label>
              <input type="number" v-model="maintenanceSettings.backupRetention" min="1" max="30" />
            </div>
            <button class="btn btn-secondary" @click="createBackup">
              立即创建备份
            </button>
          </div>
          
          <div class="settings-group danger-zone">
            <h3>危险操作</h3>
            <p class="warning-text">
              以下操作可能会导致系统数据丢失，请谨慎操作。
            </p>
            <div class="danger-actions">
              <button class="btn btn-danger" @click="confirmClearCache">
                清除系统缓存
              </button>
              <button class="btn btn-danger" @click="confirmResetSystem">
                重置系统设置
              </button>
            </div>
          </div>
          
          <div class="form-actions">
            <button class="btn btn-primary" @click="saveMaintenanceSettings">保存维护设置</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 确认对话框 -->
    <div v-if="showConfirmDialog" class="modal-overlay" @click.self="showConfirmDialog = false">
      <div class="modal">
        <div class="modal-header">
          <h3>{{ confirmDialog.title }}</h3>
          <button class="close-btn" @click="showConfirmDialog = false">×</button>
        </div>
        <div class="modal-body">
          <p>{{ confirmDialog.message }}</p>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showConfirmDialog = false">取消</button>
          <button 
            class="confirm-btn" 
            :class="confirmDialog.type === 'danger' ? 'btn-danger' : 'btn-primary'"
            @click="confirmAction"
          >
            确认
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';

// 当前激活的标签页
const activeTab = ref('general');

// 确认对话框
const showConfirmDialog = ref(false);
const confirmDialog = reactive({
  title: '',
  message: '',
  type: 'default',
  action: null
});

// 基本设置
const settings = reactive({
  systemName: '农业大数据平台',
  systemVersion: 'v1.0.0',
  systemDescription: '基于大数据技术的农业数据分析平台，整合HDFS、Hive、Spark和Kafka等组件。',
  siteTitle: '农业大数据平台',
  siteLogo: 'logo.png',
  themeColor: '#4CAF50',
  smtpServer: 'smtp.example.com',
  smtpPort: 587,
  smtpEmail: 'admin@example.com',
  smtpPassword: ''
});

// 服务配置
const serviceSettings = reactive({
  hdfs: {
    uri: 'hdfs://localhost:9000',
    replication: 3,
    blockSize: 64
  },
  hive: {
    url: 'jdbc:hive2://localhost:10000',
    defaultDatabase: 'default',
    auth: 'none'
  },
  kafka: {
    bootstrapServers: 'localhost:9092',
    consumerGroup: 'hadoop-consumer-group'
  }
});

// 维护设置
const maintenanceSettings = reactive({
  logLevel: 'info',
  logRetentionDays: 30,
  autoBackup: true,
  backupFrequency: 'daily',
  backupRetention: 7
});

// 主题颜色选项
const themeColors = [
  { name: '绿色', value: '#4CAF50' },
  { name: '蓝色', value: '#2196F3' },
  { name: '红色', value: '#F44336' },
  { name: '紫色', value: '#9C27B0' },
  { name: '橙色', value: '#FF9800' }
];

// 初始化
onMounted(() => {
  // 从服务器加载设置
  loadSettings();
});

// 加载设置
const loadSettings = async () => {
  try {
    // 这里应该调用API获取系统设置
    // const response = await SettingsService.getSettings();
    // 更新settings对象
    
    // 模拟从服务器获取设置
    console.log('从服务器加载设置');
  } catch (err) {
    console.error('加载设置失败:', err);
  }
};

// 保存基本设置
const saveGeneralSettings = async () => {
  try {
    // 这里应该调用API保存系统设置
    // await SettingsService.saveGeneralSettings(settings);
    
    // 模拟保存设置
    console.log('保存基本设置:', settings);
    alert('基本设置已保存');
  } catch (err) {
    console.error('保存设置失败:', err);
    alert('保存设置失败: ' + err.message);
  }
};

// 测试邮件设置
const testEmailSettings = async () => {
  try {
    // 这里应该调用API测试邮件设置
    // await SettingsService.testEmailSettings(settings);
    
    // 模拟测试邮件
    console.log('测试邮件设置:', settings);
    alert('测试邮件已发送');
  } catch (err) {
    console.error('测试邮件失败:', err);
    alert('测试邮件失败: ' + err.message);
  }
};

// 保存服务设置
const saveServiceSettings = async () => {
  try {
    // 这里应该调用API保存服务设置
    // await SettingsService.saveServiceSettings(serviceSettings);
    
    // 模拟保存设置
    console.log('保存服务设置:', serviceSettings);
    alert('服务设置已保存');
  } catch (err) {
    console.error('保存服务设置失败:', err);
    alert('保存服务设置失败: ' + err.message);
  }
};

// 保存维护设置
const saveMaintenanceSettings = async () => {
  try {
    // 这里应该调用API保存维护设置
    // await SettingsService.saveMaintenanceSettings(maintenanceSettings);
    
    // 模拟保存设置
    console.log('保存维护设置:', maintenanceSettings);
    alert('维护设置已保存');
  } catch (err) {
    console.error('保存维护设置失败:', err);
    alert('保存维护设置失败: ' + err.message);
  }
};

// 查看系统日志
const viewSystemLogs = () => {
  // 这里应该打开日志查看界面或下载日志
  console.log('查看系统日志');
  alert('系统日志功能尚未实现');
};

// 创建备份
const createBackup = async () => {
  try {
    // 这里应该调用API创建系统备份
    // await SettingsService.createBackup();
    
    // 模拟创建备份
    console.log('创建系统备份');
    alert('系统备份已创建');
  } catch (err) {
    console.error('创建备份失败:', err);
    alert('创建备份失败: ' + err.message);
  }
};

// 确认清除缓存
const confirmClearCache = () => {
  confirmDialog.title = '确认清除缓存';
  confirmDialog.message = '您确定要清除系统缓存吗？这可能会导致系统暂时变慢。';
  confirmDialog.type = 'danger';
  confirmDialog.action = clearCache;
  showConfirmDialog.value = true;
};

// 清除缓存
const clearCache = async () => {
  try {
    // 这里应该调用API清除系统缓存
    // await SettingsService.clearCache();
    
    // 模拟清除缓存
    console.log('清除系统缓存');
    showConfirmDialog.value = false;
    alert('系统缓存已清除');
  } catch (err) {
    console.error('清除缓存失败:', err);
    showConfirmDialog.value = false;
    alert('清除缓存失败: ' + err.message);
  }
};

// 确认重置系统
const confirmResetSystem = () => {
  confirmDialog.title = '确认重置系统';
  confirmDialog.message = '您确定要重置系统设置吗？这将删除所有自定义设置并恢复默认值。';
  confirmDialog.type = 'danger';
  confirmDialog.action = resetSystem;
  showConfirmDialog.value = true;
};

// 重置系统
const resetSystem = async () => {
  try {
    // 这里应该调用API重置系统
    // await SettingsService.resetSystem();
    
    // 模拟重置系统
    console.log('重置系统设置');
    showConfirmDialog.value = false;
    
    // 重置本地设置
    Object.assign(settings, {
      systemName: '农业大数据平台',
      systemDescription: '基于大数据技术的农业数据分析平台',
      siteTitle: '农业大数据平台',
      siteLogo: 'logo.png',
      themeColor: '#4CAF50',
      smtpServer: '',
      smtpPort: 587,
      smtpEmail: '',
      smtpPassword: ''
    });
    
    alert('系统设置已重置');
  } catch (err) {
    console.error('重置系统失败:', err);
    showConfirmDialog.value = false;
    alert('重置系统失败: ' + err.message);
  }
};

// 确认对话框的确认操作
const confirmAction = () => {
  if (typeof confirmDialog.action === 'function') {
    confirmDialog.action();
  }
};
</script>

<style scoped>
.system-settings {
  width: 100%;
  max-width: 100%;
  background-color: white;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
  padding: 1.5rem;
}

.page-header {
  margin-bottom: 1.5rem;
}

.page-header h1 {
  font-size: 1.8rem;
  margin: 0;
  color: var(--text-color);
}

/* 标签页样式 */
.settings-tabs {
  display: flex;
  border: 1px solid #e0e0e0;
  border-radius: var(--border-radius);
  overflow: hidden;
}

.tab-nav {
  width: 200px;
  background-color: #f5f5f5;
  border-right: 1px solid #e0e0e0;
}

.tab-item {
  padding: 1rem;
  cursor: pointer;
  transition: all 0.3s;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  align-items: center;
}

.tab-item:hover {
  background-color: #e9ecef;
}

.tab-item.active {
  background-color: var(--primary-light);
  color: var(--primary-dark);
  font-weight: 500;
  border-left: 3px solid var(--primary-color);
}

.tab-icon {
  margin-right: 0.8rem;
  font-size: 1.2rem;
}

.tab-content {
  flex: 1;
  padding: 1.5rem;
  min-height: 500px;
}

.tab-panel h2 {
  margin-top: 0;
  margin-bottom: 1.5rem;
  font-size: 1.5rem;
  color: var(--text-color);
}

/* 表单样式 */
.settings-group {
  margin-bottom: 2rem;
  padding-bottom: 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.settings-group h3 {
  margin-top: 0;
  margin-bottom: 1rem;
  font-size: 1.2rem;
  color: var(--text-color);
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-group input[type="text"],
.form-group input[type="number"],
.form-group input[type="email"],
.form-group input[type="password"],
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 0.7rem;
  border: 1px solid #ddd;
  border-radius: var(--border-radius);
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group select:focus,
.form-group textarea:focus {
  outline: none;
  border-color: var(--primary-color);
}

.form-group input[disabled] {
  background-color: #f5f5f5;
  cursor: not-allowed;
}

.form-actions {
  margin-top: 1.5rem;
  display: flex;
  justify-content: flex-end;
}

/* 按钮样式 */
.btn {
  padding: 0.6rem 1.2rem;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s ease;
  border: none;
}

.btn-primary {
  background-color: var(--primary-color);
  color: white;
}

.btn-primary:hover {
  background-color: var(--primary-dark);
}

.btn-secondary {
  background-color: #f5f5f5;
  color: var(--text-color);
  border: 1px solid #ddd;
}

.btn-secondary:hover {
  background-color: #e0e0e0;
}

.btn-danger {
  background-color: #f44336;
  color: white;
}

.btn-danger:hover {
  background-color: #d32f2f;
}

/* 文件上传样式 */
.file-upload {
  display: flex;
  align-items: center;
}

.file-upload input[type="file"] {
  display: none;
}

.upload-btn {
  padding: 0.5rem 1rem;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: var(--border-radius);
  cursor: pointer;
  margin-right: 1rem;
  transition: all 0.3s;
}

.upload-btn:hover {
  background-color: #e0e0e0;
}

.file-name {
  color: var(--text-light);
}

/* 颜色选择器 */
.color-selector {
  display: flex;
  gap: 1rem;
}

.color-option {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.color-option.active {
  transform: scale(1.1);
  border-color: #333;
}

/* 开关样式 */
.toggle-switch {
  position: relative;
  display: inline-block;
  width: 50px;
  height: 24px;
}

.toggle-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.toggle-switch label {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  transition: .4s;
  border-radius: 24px;
}

.toggle-switch label:before {
  position: absolute;
  content: "";
  height: 16px;
  width: 16px;
  left: 4px;
  bottom: 4px;
  background-color: white;
  transition: .4s;
  border-radius: 50%;
}

.toggle-switch input:checked + label {
  background-color: var(--primary-color);
}

.toggle-switch input:checked + label:before {
  transform: translateX(26px);
}

/* 危险区域样式 */
.danger-zone {
  background-color: #fbe9e7;
  border-radius: var(--border-radius);
  padding: 1rem;
  border: 1px solid #ffccbc;
}

.warning-text {
  color: #d32f2f;
  margin-bottom: 1rem;
}

.danger-actions {
  display: flex;
  gap: 1rem;
}

/* 确认对话框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background-color: white;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-lg);
  width: 100%;
  max-width: 450px;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1.2rem 1.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.5rem;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: var(--text-light);
}

.modal-body {
  padding: 1.5rem;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  padding: 1.2rem 1.5rem;
  border-top: 1px solid #e0e0e0;
}

.cancel-btn {
  padding: 0.6rem 1.2rem;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn:hover {
  background-color: #e0e0e0;
}

.confirm-btn {
  padding: 0.6rem 1.2rem;
  border-radius: var(--border-radius);
  cursor: pointer;
  border: none;
  transition: all 0.3s;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .settings-tabs {
    flex-direction: column;
  }
  
  .tab-nav {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid #e0e0e0;
    display: flex;
    overflow-x: auto;
  }
  
  .tab-item {
    border-bottom: none;
    flex: 1;
    justify-content: center;
    white-space: nowrap;
    padding: 0.8rem;
  }
  
  .tab-icon {
    margin-right: 0.3rem;
  }
  
  .danger-actions {
    flex-direction: column;
  }
}
</style> 