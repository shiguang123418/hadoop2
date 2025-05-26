<template>
  <div class="user-management">
    <div class="page-header">
      <h1>用户管理</h1>
      <button class="btn btn-primary" @click="showAddUserModal = true">
        <i class="add-icon">➕</i> 添加用户
      </button>
    </div>

    <div class="search-bar">
      <div class="search-input">
        <i class="search-icon">🔍</i>
        <input 
          type="text" 
          v-model="searchQuery" 
          placeholder="搜索用户..." 
          @input="searchUsers"
        />
      </div>
      <div class="filter-controls">
        <select v-model="roleFilter" @change="applyFilters">
          <option value="">所有角色</option>
          <option value="admin">管理员</option>
          <option value="user">普通用户</option>
          <option value="guest">访客</option>
        </select>
        <select v-model="statusFilter" @change="applyFilters">
          <option value="">所有状态</option>
          <option value="active">激活</option>
          <option value="inactive">未激活</option>
          <option value="locked">已锁定</option>
        </select>
      </div>
    </div>

    <div v-if="loading" class="loading-indicator">
      <div class="spinner"></div>
      <span>加载中...</span>
    </div>

    <div v-else-if="error" class="error-message">
      <i class="error-icon">❌</i>
      <span>{{ error }}</span>
      <button @click="fetchUsers" class="retry-button">重试</button>
    </div>

    <div v-else-if="filteredUsers.length === 0" class="empty-state">
      <div class="empty-icon">👥</div>
      <h3>暂无用户数据</h3>
      <p v-if="searchQuery || roleFilter || statusFilter">没有符合条件的用户，请尝试更改筛选条件</p>
      <p v-else>系统中还没有用户，点击"添加用户"创建第一个用户</p>
    </div>

    <div v-else class="user-table-wrapper">
      <table class="user-table">
        <thead>
          <tr>
            <th>用户名</th>
            <th>姓名</th>
            <th>邮箱</th>
            <th>角色</th>
            <th>状态</th>
            <th>上次登录</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in filteredUsers" :key="user.id" :class="{ 'inactive-row': user.status !== 'active' }">
            <td>{{ user.username }}</td>
            <td>{{ user.name || '-' }}</td>
            <td>{{ user.email }}</td>
            <td>
              <span class="badge" :class="`role-${user.role}`">
                {{ getRoleName(user.role) }}
              </span>
            </td>
            <td>
              <span class="badge" :class="`status-${user.status}`">
                {{ getStatusName(user.status) }}
              </span>
            </td>
            <td>{{ formatDate(user.lastLogin) }}</td>
            <td>{{ formatDate(user.createdAt) }}</td>
            <td class="actions">
              <button class="action-btn edit-btn" @click="editUser(user)">
                <i class="edit-icon">✏️</i>
              </button>
              <button class="action-btn delete-btn" @click="confirmDeleteUser(user)" :disabled="user.username === currentUser?.username">
                <i class="delete-icon">🗑️</i>
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 添加用户模态框 -->
    <div v-if="showAddUserModal" class="modal-overlay" @click.self="showAddUserModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3>添加用户</h3>
          <button class="close-btn" @click="showAddUserModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label for="username">用户名 <span class="required">*</span></label>
            <input type="text" id="username" v-model="newUser.username" required />
          </div>
          <div class="form-group">
            <label for="name">姓名</label>
            <input type="text" id="name" v-model="newUser.name" />
          </div>
          <div class="form-group">
            <label for="email">邮箱 <span class="required">*</span></label>
            <input type="email" id="email" v-model="newUser.email" required />
          </div>
          <div class="form-group">
            <label for="password">密码 <span class="required">*</span></label>
            <input type="password" id="password" v-model="newUser.password" required />
          </div>
          <div class="form-group">
            <label for="role">角色 <span class="required">*</span></label>
            <select id="role" v-model="newUser.role" required>
              <option value="admin">管理员</option>
              <option value="user">普通用户</option>
              <option value="guest">访客</option>
            </select>
          </div>
          <div class="form-group">
            <label for="status">状态</label>
            <select id="status" v-model="newUser.status">
              <option value="active">激活</option>
              <option value="inactive">未激活</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showAddUserModal = false">取消</button>
          <button class="submit-btn" @click="addUser" :disabled="isSubmitting">
            {{ isSubmitting ? '添加中...' : '添加用户' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 编辑用户模态框 -->
    <div v-if="showEditUserModal" class="modal-overlay" @click.self="showEditUserModal = false">
      <div class="modal">
        <div class="modal-header">
          <h3>编辑用户</h3>
          <button class="close-btn" @click="showEditUserModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label for="edit-username">用户名</label>
            <input type="text" id="edit-username" v-model="editingUser.username" disabled />
          </div>
          <div class="form-group">
            <label for="edit-name">姓名</label>
            <input type="text" id="edit-name" v-model="editingUser.name" />
          </div>
          <div class="form-group">
            <label for="edit-email">邮箱 <span class="required">*</span></label>
            <input type="email" id="edit-email" v-model="editingUser.email" required />
          </div>
          <div class="form-group">
            <label for="edit-password">密码 <span class="optional">(留空表示不修改)</span></label>
            <input type="password" id="edit-password" v-model="editingUser.password" />
          </div>
          <div class="form-group">
            <label for="edit-role">角色 <span class="required">*</span></label>
            <select id="edit-role" v-model="editingUser.role" required :disabled="editingUser.username === currentUser?.username">
              <option value="admin">管理员</option>
              <option value="user">普通用户</option>
              <option value="guest">访客</option>
            </select>
          </div>
          <div class="form-group">
            <label for="edit-status">状态</label>
            <select id="edit-status" v-model="editingUser.status" :disabled="editingUser.username === currentUser?.username">
              <option value="active">激活</option>
              <option value="inactive">未激活</option>
              <option value="locked">已锁定</option>
            </select>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showEditUserModal = false">取消</button>
          <button class="submit-btn" @click="updateUser" :disabled="isSubmitting">
            {{ isSubmitting ? '保存中...' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 删除确认模态框 -->
    <div v-if="showDeleteModal" class="modal-overlay" @click.self="showDeleteModal = false">
      <div class="modal delete-modal">
        <div class="modal-header">
          <h3>确认删除</h3>
          <button class="close-btn" @click="showDeleteModal = false">×</button>
        </div>
        <div class="modal-body">
          <div class="delete-warning">
            <i class="warning-icon">⚠️</i>
            <p>您确定要删除用户 <strong>{{ userToDelete?.username }}</strong> 吗？</p>
            <p class="warning-text">此操作不可撤销，用户的所有数据将被永久删除。</p>
          </div>
        </div>
        <div class="modal-footer">
          <button class="cancel-btn" @click="showDeleteModal = false">取消</button>
          <button class="delete-confirm-btn" @click="deleteUser" :disabled="isSubmitting">
            {{ isSubmitting ? '删除中...' : '确认删除' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import AuthService from '@/services/auth';

// 状态变量
const users = ref([]);
const filteredUsers = ref([]);
const loading = ref(true);
const error = ref(null);
const searchQuery = ref('');
const roleFilter = ref('');
const statusFilter = ref('');
const isSubmitting = ref(false);

// 模态框状态
const showAddUserModal = ref(false);
const showEditUserModal = ref(false);
const showDeleteModal = ref(false);

// 表单数据
const newUser = ref({
  username: '',
  name: '',
  email: '',
  password: '',
  role: 'user',
  status: 'active'
});

const editingUser = ref({});
const userToDelete = ref(null);
const currentUser = ref(AuthService.getCurrentUser());

// 获取用户数据
const fetchUsers = async () => {
  loading.value = true;
  error.value = null;
  
  try {
    // 调用实际的API获取用户列表
    const response = await AuthService.getUsers();
    if (response && response.data) {
      users.value = response.data;
      filteredUsers.value = [...users.value];
    } else {
      users.value = [];
      filteredUsers.value = [];
    }
    loading.value = false;
  } catch (err) {
    console.error('获取用户列表失败:', err);
    error.value = '获取用户数据失败，请稍后重试';
    loading.value = false;
  }
};

// 搜索用户
const searchUsers = () => {
  applyFilters();
};

// 应用筛选条件
const applyFilters = () => {
  const query = searchQuery.value.toLowerCase().trim();
  
  filteredUsers.value = users.value.filter(user => {
    // 搜索条件
    const matchesSearch = !query || 
      user.username.toLowerCase().includes(query) || 
      (user.name && user.name.toLowerCase().includes(query)) || 
      user.email.toLowerCase().includes(query);
    
    // 角色筛选
    const matchesRole = !roleFilter.value || user.role === roleFilter.value;
    
    // 状态筛选
    const matchesStatus = !statusFilter.value || user.status === statusFilter.value;
    
    return matchesSearch && matchesRole && matchesStatus;
  });
};

// 添加用户
const addUser = async () => {
  // 简单表单验证
  if (!newUser.value.username || !newUser.value.email || !newUser.value.password) {
    alert('请填写必填字段');
    return;
  }
  
  isSubmitting.value = true;
  
  try {
    // 调用实际的API创建用户
    const response = await AuthService.createUser(newUser.value);
    if (response && response.data) {
      // 将新用户添加到列表
      users.value.push(response.data);
      applyFilters();
    }
    
    showAddUserModal.value = false;
    
    // 重置表单
    newUser.value = {
      username: '',
      name: '',
      email: '',
      password: '',
      role: 'user',
      status: 'active'
    };
    
    isSubmitting.value = false;
  } catch (err) {
    console.error('创建用户失败:', err);
    alert('创建用户失败: ' + (err.response?.data?.message || '请稍后重试'));
    isSubmitting.value = false;
  }
};

// 编辑用户
const editUser = (user) => {
  // 创建副本以避免直接修改原始数据
  editingUser.value = { ...user, password: '' };
  showEditUserModal.value = true;
};

// 更新用户
const updateUser = async () => {
  // 简单表单验证
  if (!editingUser.value.email) {
    alert('请填写必填字段');
    return;
  }
  
  isSubmitting.value = true;
  
  try {
    // 创建一个不包含id的数据对象
    const userData = { ...editingUser.value };
    delete userData.id;
    delete userData.createdAt;
    delete userData.lastLogin;
    
    // 如果密码为空，不更新密码字段
    if (!userData.password) {
      delete userData.password;
    }
    
    // 调用实际的API更新用户
    const response = await AuthService.updateUser(editingUser.value.id, userData);
    
    if (response && response.data) {
      // 更新本地用户数据
      const index = users.value.findIndex(u => u.id === editingUser.value.id);
      if (index !== -1) {
        users.value[index] = response.data;
      }
    }
    
    applyFilters();
    showEditUserModal.value = false;
    isSubmitting.value = false;
  } catch (err) {
    console.error('更新用户失败:', err);
    alert('更新用户失败: ' + (err.response?.data?.message || '请稍后重试'));
    isSubmitting.value = false;
  }
};

// 确认删除用户
const confirmDeleteUser = (user) => {
  userToDelete.value = user;
  showDeleteModal.value = true;
};

// 删除用户
const deleteUser = async () => {
  if (!userToDelete.value) return;
  
  isSubmitting.value = true;
  
  try {
    // 调用实际的API删除用户
    await AuthService.deleteUser(userToDelete.value.id);
    
    // 从本地列表中移除
    users.value = users.value.filter(u => u.id !== userToDelete.value.id);
    
    applyFilters();
    showDeleteModal.value = false;
    userToDelete.value = null;
    isSubmitting.value = false;
  } catch (err) {
    console.error('删除用户失败:', err);
    alert('删除用户失败: ' + (err.response?.data?.message || '请稍后重试'));
    isSubmitting.value = false;
  }
};

// 格式化日期
const formatDate = (date) => {
  if (!date) return '-';
  
  if (typeof date === 'string') {
    date = new Date(date);
  }
  
  return date.toLocaleString('zh-CN', { 
    year: 'numeric', 
    month: '2-digit', 
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 获取角色名称
const getRoleName = (role) => {
  // 如果角色为空或非字符串类型，返回空字符串
  if (!role || typeof role !== 'string') {
    return '';
  }
  
  // 标准化角色名称
  const normalizedRole = role.toLowerCase();
  
  // 角色映射表
  const roleMap = {
    'admin': '管理员',
    'user': '普通用户',
    'guest': '访客',
    'role_admin': '管理员',
    'role_user': '普通用户',
    'role_guest': '访客',
    'role_hdfs': 'HDFS管理员',
    'role_hive': 'HIVE管理员'
  };
  
  // 先检查完整映射
  if (roleMap[normalizedRole]) {
    return roleMap[normalizedRole];
  }
  
  // 检查是否带有ROLE_前缀
  if (normalizedRole.startsWith('role_')) {
    // 尝试提取前缀后的部分
    const roleSuffix = normalizedRole.substring(5); // 'role_'长度为5
    return roleMap[roleSuffix] || role;
  } else {
    // 尝试加上前缀匹配
    return roleMap['role_' + normalizedRole] || role;
  }
};

// 获取状态名称
const getStatusName = (status) => {
  const statusMap = {
    'active': '已激活',
    'inactive': '未激活',
    'locked': '已锁定'
  };
  
  return statusMap[status] || status;
};

// 初始化
onMounted(() => {
  fetchUsers();
});
</script>

<style scoped>
.user-management {
  width: 100%;
  max-width: 100%;
  background-color: white;
  border-radius: var(--border-radius);
  box-shadow: var(--shadow-sm);
  padding: 1.5rem;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1.5rem;
}

.page-header h1 {
  font-size: 1.8rem;
  margin: 0;
  color: var(--text-color);
}

.btn {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.6rem 1.2rem;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  font-weight: 600;
  transition: all 0.3s ease;
}

.btn:hover {
  background-color: var(--primary-dark);
  transform: translateY(-2px);
}

.add-icon {
  font-size: 1rem;
}

.search-bar {
  display: flex;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.search-input {
  position: relative;
  flex: 1;
}

.search-icon {
  position: absolute;
  left: 10px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 1rem;
  color: var(--text-light);
}

.search-input input {
  width: 100%;
  padding: 0.6rem 0.6rem 0.6rem 2.2rem;
  border: 1px solid #ddd;
  border-radius: var(--border-radius);
  transition: border-color 0.3s;
}

.search-input input:focus {
  outline: none;
  border-color: var(--primary-color);
}

.filter-controls {
  display: flex;
  gap: 0.8rem;
}

.filter-controls select {
  padding: 0.6rem;
  border: 1px solid #ddd;
  border-radius: var(--border-radius);
  background-color: white;
  cursor: pointer;
}

.loading-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: var(--text-light);
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid rgba(0, 0, 0, 0.1);
  border-radius: 50%;
  border-top-color: var(--primary-color);
  animation: spin 1s infinite linear;
  margin-bottom: 1rem;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.error-message {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
  color: #d32f2f;
  text-align: center;
}

.error-icon {
  font-size: 2rem;
  margin-bottom: 1rem;
}

.retry-button {
  margin-top: 1rem;
  padding: 0.5rem 1.5rem;
  background-color: #f5f5f5;
  border: 1px solid #ddd;
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.3s;
}

.retry-button:hover {
  background-color: #e0e0e0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 3rem;
  color: var(--text-light);
  text-align: center;
}

.empty-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  opacity: 0.3;
}

.user-table-wrapper {
  overflow-x: auto;
  margin-bottom: 1.5rem;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  border-spacing: 0;
}

.user-table th,
.user-table td {
  padding: 0.8rem;
  text-align: left;
  border-bottom: 1px solid #e0e0e0;
}

.user-table th {
  background-color: #f5f5f5;
  font-weight: 600;
  color: var(--text-color);
}

.user-table tr:hover {
  background-color: #f9f9f9;
}

.inactive-row {
  opacity: 0.7;
}

.badge {
  display: inline-block;
  padding: 0.25rem 0.6rem;
  border-radius: 20px;
  font-size: 0.75rem;
  font-weight: 600;
  text-transform: uppercase;
}

.role-admin {
  background-color: #e3f2fd;
  color: #1976d2;
}

.role-user {
  background-color: #e8f5e9;
  color: #388e3c;
}

.role-guest {
  background-color: #f5f5f5;
  color: #757575;
}

.status-active {
  background-color: #e8f5e9;
  color: #388e3c;
}

.status-inactive {
  background-color: #fff8e1;
  color: #ff8f00;
}

.status-locked {
  background-color: #fbe9e7;
  color: #d32f2f;
}

.actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-start;
}

.action-btn {
  width: 32px;
  height: 32px;
  border: none;
  background-color: transparent;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s;
}

.edit-btn:hover {
  background-color: #e3f2fd;
}

.delete-btn:hover {
  background-color: #fbe9e7;
}

.action-btn:disabled {
  opacity: 0.3;
  cursor: not-allowed;
}

/* 模态框样式 */
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
  max-width: 550px;
  max-height: 90vh;
  overflow-y: auto;
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

.form-group {
  margin-bottom: 1.2rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 0.7rem;
  border: 1px solid #ddd;
  border-radius: var(--border-radius);
  transition: border-color 0.3s;
}

.form-group input:focus,
.form-group select:focus {
  outline: none;
  border-color: var(--primary-color);
}

.required {
  color: #d32f2f;
}

.optional {
  color: var(--text-light);
  font-size: 0.85rem;
  font-weight: normal;
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

.submit-btn {
  padding: 0.6rem 1.2rem;
  background-color: var(--primary-color);
  color: white;
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.3s;
}

.submit-btn:hover {
  background-color: var(--primary-dark);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.delete-modal .modal-body {
  padding: 2rem 1.5rem;
}

.delete-warning {
  text-align: center;
}

.warning-icon {
  font-size: 3rem;
  margin-bottom: 1rem;
  display: block;
}

.warning-text {
  color: var(--text-light);
  margin-top: 0.5rem;
}

.delete-confirm-btn {
  padding: 0.6rem 1.2rem;
  background-color: #f44336;
  color: white;
  border: none;
  border-radius: var(--border-radius);
  cursor: pointer;
  transition: all 0.3s;
}

.delete-confirm-btn:hover {
  background-color: #d32f2f;
}

.delete-confirm-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .search-bar {
    flex-direction: column;
  }
  
  .filter-controls {
    justify-content: space-between;
  }
  
  .filter-controls select {
    flex: 1;
  }
  
  .user-table th,
  .user-table td {
    padding: 0.6rem;
  }
  
  .action-btn {
    width: 28px;
    height: 28px;
  }
}
</style> 