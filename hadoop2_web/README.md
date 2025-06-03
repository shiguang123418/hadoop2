# Hadoop2 Web 前端应用

## 项目介绍

Hadoop2 Web是一个基于Vue 3和Element Plus构建的大数据平台前端应用，支持连接多个后端API服务，提供农业大数据可视化和管理功能。

## 技术栈

- Vue 3.2+
- Vite 4.x
- Element Plus 2.x
- ECharts 5.4+
- TailwindCSS
- Pinia 状态管理
- Vue Router 4.x

## 多后端API支持

本项目支持将不同的服务模块路由到不同的后端服务器，以优化系统架构。

### 后端服务器配置

项目中定义了多个API服务器端点，配置在`vite.config.js`和`src/config/api.config.js`中：

- `/api1` - 默认服务器，用于HDFS和Hive服务（端口8000）
- `/api_ws` - WebSocket服务器，用于Kafka和实时数据服务（端口8001）

### 代理配置

`vite.config.js`中设置了开发环境的代理规则，将请求路由到不同的后端服务：

```js
server: {
  proxy: {
    // API1代理配置
    '/api1': {
      target: 'http://localhost:8000',
      changeOrigin: true,
      rewrite: path => path.replace(/^\/api1/, '/api')
    },
    // WebSocket代理配置
    '/api_ws': {
      target: 'http://localhost:8001',
      ws: true,
      changeOrigin: true,
      rewrite: path => path.replace(/^\/api_ws/, '/api/ws')
    }
  }
}
```

### 服务配置

服务配置在`src/config/api.config.js`中定义，将每个服务映射到对应的后端服务器：

```js
// 服务器配置
const servers = {
  default: '/api1',
  server1: '/api1',
  websocket: '/api_ws'
};

// 服务相关配置
const services = {
  // HDFS服务 - 使用服务器1
  hdfs: {
    path: '/hdfs',
    server: servers.server1
  },
  // Hive服务 - 使用服务器1
  hive: {
    path: '/hive',
    server: servers.server1
  },
  // Kafka服务 - 使用WebSocket服务器
  kafka: {
    path: '/kafka',
    server: servers.websocket
  },
  // 认证服务 - 使用默认服务器
  auth: {
    path: '/auth',
    server: servers.default
  },
  // 实时数据服务 - 使用WebSocket服务器
  realtime: {
    path: '/realtime',
    server: servers.websocket
  }
};
```

### 使用多后端API

在服务类中，通过继承`ApiService`基类并指定服务名称，可以自动将请求路由到正确的后端服务器：

```js
import ApiService from './api.service';

class HDFSServiceClass extends ApiService {
  constructor() {
    // 使用服务名称'hdfs'，会自动路由到服务器1
    super('hdfs');
  }
  
  // API方法
  listFiles(path) {
    return this.get('/list', { params: { path } });
  }
  
  downloadFile(path) {
    return this.get('/file', { params: { path }, responseType: 'blob' });
  }
}

export const HDFSService = new HDFSServiceClass();
```

## 项目结构

```
hadoop2_web/
├── public/                 # 静态资源
├── src/
│   ├── api/                # API服务层
│   │   ├── modules/        # 各模块API
│   │   └── api.service.js  # API服务基类
│   │
│   ├── assets/             # 静态资源
│   │
│   ├── components/         # 通用组件
│   │
│   ├── config/             # 配置文件
│   │
│   ├── layouts/            # 布局组件
│   │
│   ├── router/             # 路由配置
│   │
│   ├── stores/             # Pinia状态
│   │
│   ├── styles/             # 样式文件
│   │
│   ├── utils/              # 工具函数
│   │
│   ├── views/              # 页面组件
│   │
│   ├── App.vue             # 根组件
│   └── main.js             # 入口文件
│
├── .env                    # 环境变量
│
├── .env.development        # 开发环境变量
│
├── .env.production         # 生产环境变量
│
├── index.html              # HTML模板
│
├── package.json            # 项目依赖
│
├── vite.config.js          # Vite配置
└── tailwind.config.js      # TailwindCSS配置
```

## 开发指南

### 安装依赖

```bash
npm install
# 或
yarn install
```

### 启动开发服务器

```bash
npm run dev
# 或
yarn dev
```

### 构建生产版本

```bash
npm run build
# 或
yarn build
```

### 代码检查

```bash
npm run lint
# 或
yarn lint
```

## 部署指南

### 构建生产版本

```bash
npm run build
```

构建完成后，生成的文件将位于`dist`目录。

### Nginx配置

将构建后的前端应用部署到Nginx服务器，配置文件示例：

```nginx
server {
    listen 5173;
    server_name your_domain_or_ip;
    index index.html;
    root /path/to/hadoop2/hadoop2_web/dist;
    
    # 处理前端SPA路由，确保直接访问路由不会返回404
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    # API代理设置
    location /api1/ {
        proxy_pass http://localhost:8000/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
    
    # WebSocket代理设置
    location /api_ws/ {
        proxy_pass http://localhost:8001/api/ws/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket支持配置
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
    
    # 静态资源缓存设置
    location ~* \.(js|css)$ {
        expires 12h;
        add_header Cache-Control "public, no-transform";
    }
    
    location ~* \.(jpg|jpeg|png|gif|ico|svg)$ {
        expires 30d;
        add_header Cache-Control "public, no-transform";
    }
}
```

### 配置文件设置

根据不同环境需要修改配置文件，主要包括：

1. `.env.production`：设置生产环境变量
   ```
   VITE_API_BASE_URL=https://your-production-domain.com
   VITE_API_TIMEOUT=30000
   ```

2. 确保在构建前已正确配置`src/config/api.config.js`中的服务器地址

### 部署流程

1. 构建前端应用
   ```bash
   npm run build
   ```

2. 将`dist`目录内容复制到Nginx服务器目录
   ```bash
   cp -r dist/* /path/to/nginx/html/
   ```

3. 配置Nginx，并重新加载配置
   ```bash
   nginx -s reload
   ```

## 常见问题

### 路由跳转404问题

如果直接访问前端路由（如 `/dashboard` 或 `/analytics`）返回404错误，请确保Nginx配置中包含以下设置：

```nginx
location / {
    try_files $uri $uri/ /index.html;
}
```

这会将所有不存在的路径请求重定向到index.html，让前端路由系统处理它们。

### API请求跨域问题

如果遇到跨域问题，确保Nginx配置正确设置了代理，并且在后端服务中启用了CORS支持：

```nginx
location /api1/ {
    # ...
    add_header Access-Control-Allow-Origin * always;
    add_header Access-Control-Allow-Methods 'GET, POST, PUT, DELETE, OPTIONS' always;
    add_header Access-Control-Allow-Headers 'DNT,X-CustomHeader,Keep-Alive,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Authorization' always;
    
    if ($request_method = 'OPTIONS') {
        return 204;
    }
}
```

### WebSocket连接问题

如果WebSocket连接失败，请检查：

1. Nginx配置中是否正确设置了WebSocket代理
2. 后端WebSocket服务是否正常运行
3. 前端连接字符串是否正确 