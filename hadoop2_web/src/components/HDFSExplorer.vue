<template>
  <div class="hdfs-explorer">
    <div class="status-bar">
      <span :class="['status-indicator', connected ? 'connected' : 'disconnected']"></span>
      <span class="status-text">
        {{ connected ? '已连接' : '未连接' }} {{ hdfsUri }}
      </span>
      <button @click="refreshStatus" class="refresh-btn">刷新</button>
    </div>
    
    <div class="path-navigator">
      <input 
        type="text" 
        v-model="currentPath" 
        class="path-input" 
        @keyup.enter="browseToPath" 
        placeholder="HDFS路径，如 /user/data"
      />
      <button @click="browseToPath" class="browse-btn">浏览</button>
      <button @click="goToParentDir" class="parent-dir-btn" :disabled="isRootDir">上级目录</button>
    </div>
    
    <div class="explorer-toolbar">
      <button @click="showNewFolderDialog = true" class="toolbar-btn">
        <i class="folder-icon">📁</i> 新建文件夹
      </button>
      <button @click="showUploadDialog = true" class="toolbar-btn">
        <i class="upload-icon">⬆️</i> 上传文件
      </button>
      <button @click="refreshCurrentPath" class="toolbar-btn">
        <i class="refresh-icon">🔄</i> 刷新
      </button>
    </div>
    
    <div v-if="loading" class="loading">
      加载中...
    </div>
    
    <div v-else-if="error" class="error-message">
      <p>加载失败: {{ error }}</p>
      <button @click="refreshCurrentPath" class="retry-btn">重试</button>
    </div>
    
    <div v-else-if="files.length === 0" class="empty-directory">
      <p>当前目录为空</p>
    </div>
    
    <div v-else class="file-list">
      <table>
        <thead>
          <tr>
            <th>名称</th>
            <th>类型</th>
            <th>大小</th>
            <th>权限</th>
            <th>修改时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(file, index) in files" :key="index">
            <td>
              <div class="file-name" @click="handleFileClick(file)">
                <i v-if="file.isDirectory" class="folder-icon">📁</i>
                <i v-else class="file-icon">📄</i>
                {{ file.name }}
              </div>
            </td>
            <td>{{ file.isDirectory ? '目录' : '文件' }}</td>
            <td>{{ formatFileSize(file.length) }}</td>
            <td>{{ file.permission }}</td>
            <td>{{ formatDate(file.modificationTime) }}</td>
            <td class="actions">
              <button @click="downloadFile(file)" v-if="!file.isDirectory" class="action-btn download-btn">
                下载
              </button>
              <button @click="deleteFile(file)" class="action-btn delete-btn">
                删除
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 新建文件夹对话框 -->
    <div v-if="showNewFolderDialog" class="dialog-overlay" @click.self="showNewFolderDialog = false">
      <div class="dialog">
        <h3>新建文件夹</h3>
        <div class="form-group">
          <label>当前路径: {{ currentPath }}</label>
        </div>
        <div class="form-group">
          <label for="folder-name">文件夹名称:</label>
          <input type="text" id="folder-name" v-model="newFolderName" />
        </div>
        <div class="dialog-actions">
          <button @click="showNewFolderDialog = false" class="cancel-btn">取消</button>
          <button @click="createFolder" class="create-btn" :disabled="!newFolderName">创建</button>
        </div>
      </div>
    </div>
    
    <!-- 上传文件对话框 -->
    <div v-if="showUploadDialog" class="dialog-overlay" @click.self="showUploadDialog = false">
      <div class="dialog">
        <h3>上传文件</h3>
        <div class="form-group">
          <label>上传到: {{ currentPath }}</label>
        </div>
        <div class="form-group">
          <label for="file-upload">选择文件:</label>
          <input type="file" id="file-upload" @change="handleFileChange" />
        </div>
        <div v-if="uploadProgress > 0" class="upload-progress">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
          <div class="progress-text">{{ uploadProgress }}%</div>
        </div>
        <div class="dialog-actions">
          <button @click="showUploadDialog = false" class="cancel-btn">取消</button>
          <button @click="uploadFile" class="upload-btn" :disabled="!selectedFile || uploading">
            {{ uploading ? '上传中...' : '上传' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { HDFSService } from '@/services';

export default {
  name: 'HDFSExplorer',
  
  props: {
    initialPath: {
      type: String,
      default: '/'
    }
  },
  
  setup(props) {
    // 状态变量
    const connected = ref(false);
    const hdfsUri = ref('');
    const currentPath = ref(props.initialPath);
    const files = ref([]);
    const loading = ref(false);
    const error = ref(null);
    
    // 对话框状态
    const showNewFolderDialog = ref(false);
    const showUploadDialog = ref(false);
    
    // 表单值
    const newFolderName = ref('');
    const selectedFile = ref(null);
    const uploading = ref(false);
    const uploadProgress = ref(0);
    
    // 计算属性
    const isRootDir = computed(() => {
      return currentPath.value === '/' || currentPath.value === '';
    });
    
    // 初始化
    onMounted(async () => {
      try {
        await refreshStatus();
        if (connected.value) {
          await browseToPath();
        }
      } catch (e) {
        console.error("初始化HDFS浏览器失败:", e);
      }
    });
    
    // 刷新HDFS连接状态
    const refreshStatus = async () => {
      try {
        const response = await HDFSService.getStatus();
        console.log("HDFS状态响应:", response);
        
        // 处理可能的嵌套数据结构
        if (response && response.data) {
          connected.value = response.data.connected;
          hdfsUri.value = response.data.uri || '';
        } else {
          connected.value = response.connected;
          hdfsUri.value = response.uri || '';
        }
      } catch (err) {
        console.error('获取HDFS状态失败:', err);
        connected.value = false;
        hdfsUri.value = '';
      }
    };
    
    // 浏览到指定路径
    const browseToPath = async () => {
      if (!currentPath.value) {
        currentPath.value = '/';
      }
      
      // 处理路径，移除HDFS URL前缀
      const cleanPath = cleanHdfsPath(currentPath.value);
      currentPath.value = cleanPath; // 更新为干净的路径
      
      if (!connected.value) {
        error.value = 'HDFS服务未连接';
        return;
      }
      
      loading.value = true;
      error.value = null;
      
      try {
        console.log("正在请求HDFS目录:", cleanPath);
        const response = await HDFSService.listFiles(cleanPath);
        console.log("HDFS列表响应:", response);
        
        // 处理可能的嵌套数据结构
        let fileList = response;
        if (response && response.data) {
          fileList = response.data;
        }
        
        if (Array.isArray(fileList)) {
          files.value = fileList;
        } else {
          files.value = [];
        }
      } catch (err) {
        console.error('获取文件列表失败:', err);
        error.value = err.response?.data?.error || err.message;
        files.value = [];
      } finally {
        loading.value = false;
      }
    };
    
    // 刷新当前路径
    const refreshCurrentPath = () => {
      browseToPath();
    };
    
    // 处理文件/目录点击
    const handleFileClick = (file) => {
      if (file.isDirectory) {
        // 如果是目录，进入该目录
        // 使用相对路径而不是完整的HDFS路径
        currentPath.value = cleanHdfsPath(file.path);
        browseToPath();
      }
    };
    
    // 到上级目录
    const goToParentDir = () => {
      if (isRootDir.value) return;
      
      // 处理路径，确保使用干净的相对路径
      const cleanPath = cleanHdfsPath(currentPath.value);
      const pathParts = cleanPath.split('/').filter(p => p);
      pathParts.pop();
      currentPath.value = pathParts.length > 0 ? '/' + pathParts.join('/') : '/';
      browseToPath();
    };
    
    // 清理HDFS路径，移除URL前缀
    const cleanHdfsPath = (path) => {
      // 检查路径是否包含hdfs:// URL前缀
      if (path.includes('hdfs:')) {
        // 提取URL后面的实际路径部分
        const match = path.match(/^hdfs:\/\/[^\/]+(.*)$/);
        if (match && match[1]) {
          return match[1] || '/';
        }
      }
      
      // 如果路径包含完整主机名，也提取路径部分
      if (path.includes('://')) {
        const match = path.match(/^.*:\/\/[^\/]+(.*)$/);
        if (match && match[1]) {
          return match[1] || '/';
        }
      }
      
      // 确保路径至少以/开头
      if (!path.startsWith('/')) {
        return '/' + path;
      }
      
      return path;
    };
    
    // 创建文件夹
    const createFolder = async () => {
      if (!newFolderName.value) return;
      
      try {
        // 使用干净的路径
        const cleanPath = cleanHdfsPath(currentPath.value); 
        const path = `${cleanPath}/${newFolderName.value}`.replace(/\/\//g, '/');
        await HDFSService.createDirectory(path);
        showNewFolderDialog.value = false;
        newFolderName.value = '';
        refreshCurrentPath();
      } catch (err) {
        console.error('创建文件夹失败:', err);
        alert(`创建文件夹失败: ${err.response?.data?.error || err.message}`);
      }
    };
    
    // 处理文件选择
    const handleFileChange = (event) => {
      selectedFile.value = event.target.files[0];
      uploadProgress.value = 0;
    };
    
    // 上传文件
    const uploadFile = async () => {
      if (!selectedFile.value) return;
      
      uploading.value = true;
      uploadProgress.value = 0;
      
      try {
        // 使用清理后的路径 - 确保只提供目录路径，不包含文件名
        const cleanPath = cleanHdfsPath(currentPath.value);
        // 不再将文件名添加到路径中，而是让后端处理
        await HDFSService.uploadFile(
          selectedFile.value, 
          cleanPath,  // 只传递目录路径
          (progress) => {
            uploadProgress.value = progress;
          }
        );
        showUploadDialog.value = false;
        selectedFile.value = null;
        refreshCurrentPath();
      } catch (err) {
        console.error('上传文件失败:', err);
        alert(`上传文件失败: ${err.response?.data?.error || err.message}`);
      } finally {
        uploading.value = false;
      }
    };
    
    // 下载文件
    const downloadFile = async (file) => {
      try {
        // 使用清理后的路径
        const cleanPath = cleanHdfsPath(file.path);
        console.log("正在下载文件:", cleanPath);
        const response = await HDFSService.downloadFile(cleanPath);
        console.log("下载文件响应:", response);
        HDFSService.saveFile(response, file.name);
      } catch (err) {
        console.error('下载文件失败:', err);
        alert(`下载文件失败: ${err.response?.data?.error || err.message}`);
      }
    };
    
    // 删除文件或目录
    const deleteFile = async (file) => {
      if (!confirm(`确定要删除 ${file.name} 吗？${file.isDirectory ? '这将删除目录中的所有内容！' : ''}`)) {
        return;
      }
      
      try {
        // 使用清理后的路径
        const cleanPath = cleanHdfsPath(file.path);
        await HDFSService.deleteFile(cleanPath, file.isDirectory);
        refreshCurrentPath();
      } catch (err) {
        console.error('删除失败:', err);
        alert(`删除失败: ${err.response?.data?.error || err.message}`);
      }
    };
    
    // 格式化文件大小
    const formatFileSize = (size) => {
      if (size === 0) return '0 B';
      if (size === undefined || size === null) return '-';
      
      const units = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(size) / Math.log(1024));
      return (size / Math.pow(1024, i)).toFixed(2) + ' ' + units[i];
    };
    
    // 格式化日期
    const formatDate = (timestamp) => {
      if (!timestamp) return '-';
      const date = new Date(timestamp);
      return date.toLocaleString();
    };
    
    return {
      connected,
      hdfsUri,
      currentPath,
      files,
      loading,
      error,
      showNewFolderDialog,
      showUploadDialog,
      newFolderName,
      selectedFile,
      uploading,
      uploadProgress,
      isRootDir,
      refreshStatus,
      browseToPath,
      refreshCurrentPath,
      handleFileClick,
      goToParentDir,
      createFolder,
      handleFileChange,
      uploadFile,
      downloadFile,
      deleteFile,
      formatFileSize,
      formatDate
    };
  }
};
</script>

<style scoped>
.hdfs-explorer {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 1rem;
}

.status-bar {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
  padding: 0.5rem;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.status-indicator {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 0.5rem;
}

.connected {
  background-color: #4CAF50;
}

.disconnected {
  background-color: #f44336;
}

.refresh-btn {
  margin-left: auto;
  padding: 0.25rem 0.5rem;
  background-color: #e7e7e7;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.path-navigator {
  display: flex;
  margin-bottom: 1rem;
}

.path-input {
  flex: 1;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  margin-right: 0.5rem;
}

.browse-btn,
.parent-dir-btn {
  padding: 0.5rem 1rem;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-left: 0.5rem;
}

.parent-dir-btn {
  background-color: #2196F3;
}

.parent-dir-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.explorer-toolbar {
  display: flex;
  margin-bottom: 1rem;
}

.toolbar-btn {
  padding: 0.5rem 1rem;
  background-color: #f1f1f1;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 0.5rem;
  display: flex;
  align-items: center;
}

.toolbar-btn i {
  margin-right: 0.25rem;
}

.loading,
.error-message,
.empty-directory {
  padding: 2rem;
  text-align: center;
  background-color: #f9f9f9;
  border-radius: 4px;
}

.error-message {
  color: #f44336;
}

.retry-btn {
  padding: 0.5rem 1rem;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 1rem;
}

.file-list {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th, td {
  padding: 0.75rem;
  text-align: left;
  border-bottom: 1px solid #ddd;
}

th {
  background-color: #f1f1f1;
}

.file-name {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.file-name:hover {
  text-decoration: underline;
}

.file-icon,
.folder-icon {
  margin-right: 0.5rem;
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.action-btn {
  padding: 0.25rem 0.5rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.download-btn {
  background-color: #2196F3;
  color: white;
}

.delete-btn {
  background-color: #f44336;
  color: white;
}

.dialog-overlay {
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

.dialog {
  background-color: white;
  padding: 1.5rem;
  border-radius: 4px;
  width: 100%;
  max-width: 400px;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
}

.form-group input {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}

.cancel-btn {
  padding: 0.5rem 1rem;
  background-color: #f1f1f1;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.create-btn,
.upload-btn {
  padding: 0.5rem 1rem;
  background-color: #4CAF50;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.create-btn:disabled,
.upload-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.upload-progress {
  margin-top: 1rem;
}

.progress-bar {
  height: 20px;
  background-color: #f1f1f1;
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background-color: #4CAF50;
  transition: width 0.3s;
}

.progress-text {
  text-align: center;
  margin-top: 0.25rem;
}
</style> 