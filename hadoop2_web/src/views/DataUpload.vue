<template>
    <div class="data-upload">
      <h2>数据上传</h2>
      
      <div class="card">
        <h3>上传农业数据</h3>
        
        <div class="form-group">
          <label for="file-upload">选择文件</label>
          <input 
            type="file" 
            id="file-upload" 
            class="form-control" 
            @change="handleFileChange"
          />
          <small class="file-info" v-if="selectedFile">
            文件大小: {{ formatFileSize(selectedFile.size) }}
          </small>
        </div>
        
        <div class="form-group">
          <label for="hdfs-path">HDFS 路径</label>
          <input 
            type="text" 
            id="hdfs-path" 
            class="form-control" 
            v-model="hdfsPath" 
            placeholder="/user/data/agriculture/crops.csv"
          />
        </div>
        
        <button class="btn" @click="uploadFile" :disabled="!selectedFile || uploading">
          {{ uploading ? '上传中...' : '上传到 HDFS' }}
        </button>
        
        <div v-if="uploading" class="progress-container">
          <div class="progress-bar">
            <div class="progress-fill" :style="{ width: uploadProgress + '%' }"></div>
          </div>
          <div class="progress-text">{{ uploadProgress }}%</div>
        </div>
        
        <div v-if="uploadStatus" class="upload-status" :class="{ success: uploadSuccess, error: !uploadSuccess }">
          {{ uploadStatus }}
        </div>
      </div>
      
      <div class="card">
        <h3>创建 Hive 表</h3>
        
        <div class="form-group">
          <label for="table-name">表名</label>
          <input 
            type="text" 
            id="table-name" 
            class="form-control" 
            v-model="tableName" 
            placeholder="crop_data"
          />
        </div>
        
        <div class="form-group">
          <label for="table-schema">表结构</label>
          <textarea 
            id="table-schema" 
            class="form-control" 
            v-model="tableSchema" 
            rows="5" 
            placeholder="(id INT, crop_type STRING, region STRING, area DOUBLE, yield DOUBLE, harvest_date DATE, temperature DOUBLE, rainfall DOUBLE, soil_moisture DOUBLE, soil_type STRING)"
          ></textarea>
        </div>
        
        <button class="btn" @click="createTable" :disabled="!tableName || !tableSchema || creatingTable">
          {{ creatingTable ? '创建中...' : '创建表' }}
        </button>
        
        <div v-if="tableStatus" class="upload-status" :class="{ success: tableSuccess, error: !tableSuccess }">
          {{ tableStatus }}
        </div>
      </div>
    </div>
  </template>
  
  <script>
  export default {
    name: 'DataUpload',
    data() {
      return {
        selectedFile: null,
        hdfsPath: '',
        uploading: false,
        uploadStatus: '',
        uploadSuccess: false,
        
        tableName: '',
        tableSchema: '',
        creatingTable: false,
        tableStatus: '',
        tableSuccess: false,
        
        // 分片上传配置
        chunkSize: 5 * 1024 * 1024, // 5MB
        currentChunk: 0,
        totalChunks: 0,
        uploadProgress: 0
      }
    },
    methods: {
      handleFileChange(event) {
        this.selectedFile = event.target.files[0]
        this.uploadSuccess = false
        this.uploadStatus = ''
        this.uploadProgress = 0
        
        // Auto-generate HDFS path based on file name
        if (this.selectedFile) {
          this.hdfsPath = `/user/data/agriculture/${this.selectedFile.name}`
          // 计算分片数量
          this.totalChunks = Math.ceil(this.selectedFile.size / this.chunkSize)
          this.currentChunk = 0
        }
      },
      
      formatFileSize(size) {
        if (size < 1024) {
          return size + ' bytes'
        } else if (size < 1024 * 1024) {
          return (size / 1024).toFixed(2) + ' KB'
        } else {
          return (size / (1024 * 1024)).toFixed(2) + ' MB'
        }
      },
      
      async uploadFile() {
        if (!this.selectedFile) return
        
        this.uploading = true
        this.uploadStatus = '准备上传文件...'
        this.currentChunk = 0
        this.uploadProgress = 0
        
        try {
          // 创建目标文件夹
          await this.createFolder()
          
          // 开始分片上传
          await this.uploadNextChunk()
        } catch (error) {
          this.uploadSuccess = false
          this.uploadStatus = `上传失败: ${error.response?.data?.error || error.message}`
          console.error('上传文件错误:', error)
          this.uploading = false
        }
      },
      
      async createFolder() {
        try {
          // 获取目标路径的目录部分
          const pathParts = this.hdfsPath.split('/')
          pathParts.pop() // 移除文件名
          const dirPath = pathParts.join('/')
          
          if (dirPath) {
            await this.axios.post('/agriculture/directory', null, {
              params: { path: dirPath }
            })
          }
        } catch (error) {
          console.warn('创建目录失败，可能已经存在:', error)
          // 继续上传，不中断流程
        }
      },
      
      async uploadNextChunk() {
        if (this.currentChunk >= this.totalChunks) {
          // 所有分片上传完成
          this.uploadSuccess = true
          this.uploadStatus = '文件上传成功！'
          this.uploading = false
          return
        }
        
        // 计算当前分片的起始位置和结束位置
        const start = this.currentChunk * this.chunkSize
        const end = Math.min(start + this.chunkSize, this.selectedFile.size)
        const chunk = this.selectedFile.slice(start, end)
        
        // 更新状态
        this.uploadProgress = Math.floor((this.currentChunk / this.totalChunks) * 100)
        this.uploadStatus = `正在上传分片 ${this.currentChunk + 1}/${this.totalChunks} (${this.uploadProgress}%)`
        
        try {
          const formData = new FormData()
          formData.append('file', chunk, this.selectedFile.name + '.part' + this.currentChunk)
          
          // 构建带有分片信息的路径
          const chunkPath = `${this.hdfsPath}.part${this.currentChunk}`
          
          // 上传当前分片
          await this.axios.post('/agriculture/upload-file', formData, {
            params: { 
              path: chunkPath,
              totalChunks: this.totalChunks,
              currentChunk: this.currentChunk,
              fileName: this.selectedFile.name
            },
            headers: { 
              'Content-Type': 'multipart/form-data'
            }
          })
          
          // 上传下一个分片
          this.currentChunk++
          
          // 如果是最后一个分片，发送合并请求
          if (this.currentChunk >= this.totalChunks) {
            this.uploadStatus = '正在合并文件...'
            await this.mergeChunks()
          } else {
            // 继续上传下一个分片
            await this.uploadNextChunk()
          }
        } catch (error) {
          this.uploadSuccess = false
          this.uploadStatus = `上传分片失败: ${error.response?.data?.error || error.message}`
          console.error('上传分片错误:', error)
          this.uploading = false
        }
      },
      
      async mergeChunks() {
        try {
          await this.axios.post('/agriculture/merge-chunks', null, {
            params: {
              targetPath: this.hdfsPath,
              totalChunks: this.totalChunks,
              fileName: this.selectedFile.name
            }
          })
          
          this.uploadSuccess = true
          this.uploadStatus = '文件上传并合并成功！'
        } catch (error) {
          this.uploadSuccess = false
          this.uploadStatus = `合并文件失败: ${error.response?.data?.error || error.message}`
          console.error('合并文件错误:', error)
        } finally {
          this.uploading = false
        }
      },
      
      async createTable() {
        if (!this.tableName || !this.tableSchema) return
        
        this.creatingTable = true
        this.tableStatus = '正在创建表...'
        
        try {
          const response = await this.axios.post('/agriculture/create-table', null, {
            params: {
              tableName: this.tableName,
              schema: this.tableSchema
            }
          })
          
          this.tableSuccess = true
          this.tableStatus = '表创建成功！'
        } catch (error) {
          this.tableSuccess = false
          this.tableStatus = `创建失败: ${error.response?.data?.error || error.message}`
          console.error('创建表错误:', error)
        } finally {
          this.creatingTable = false
        }
      }
    }
  }
  </script>
  
  <style scoped>
  .data-upload h2 {
    margin-bottom: 1.5rem;
  }
  
  .upload-status {
    margin-top: 1rem;
    padding: 0.5rem;
    border-radius: 4px;
  }
  
  .success {
    background-color: #d4edda;
    color: #155724;
  }
  
  .error {
    background-color: #f8d7da;
    color: #721c24;
  }
  
  .file-info {
    display: block;
    margin-top: 0.5rem;
    color: #6c757d;
  }
  
  textarea.form-control {
    font-family: monospace;
  }
  
  .progress-container {
    margin-top: 1rem;
    display: flex;
    align-items: center;
  }
  
  .progress-bar {
    flex: 1;
    height: 20px;
    background-color: #e9ecef;
    border-radius: 4px;
    overflow: hidden;
  }
  
  .progress-fill {
    height: 100%;
    background-color: #4caf50;
    transition: width 0.3s;
  }
  
  .progress-text {
    margin-left: 10px;
    min-width: 40px;
    text-align: right;
  }
  </style>