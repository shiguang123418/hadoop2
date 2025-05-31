# Hadoop2 Web 前端应用

## 项目介绍

Hadoop2 Web是一个基于Vue 3和Element Plus构建的大数据平台前端应用，支持连接多个后端API服务。

## 多后端API支持

本项目支持将不同的服务模块路由到不同的后端服务器，主要配置如下：

### 后端服务器配置

项目中定义了多个API服务器端点，配置在`vite.config.js`和`src/config/api.config.js`中：

- `/api1` - 默认服务器，用于HDFS和Hive服务
- `/api2` - 第二服务器，用于Kafka、Spark和实时数据服务

### 代理配置

`vite.config.js`中设置了开发环境的代理规则，将请求路由到不同的后端服务：

```js
server: {
  proxy: {
    // API1代理配置
    '/api1': {
      target: 'http://shiguang:8000',
      changeOrigin: true,
      rewrite: path => path.replace(/^\/api1/, '/api')
    },
    // API2代理配置
    '/api2': {
      target: 'http://shiguang:8001',
      changeOrigin: true,
      rewrite: path => path.replace(/^\/api2/, '/api')
    },
    // WebSocket代理配置
    '/api_ws': {
      target: 'http://shiguang:8001',
      ws: true,
      rewrite: path => path.replace(/^\/api_ws/, '/api/ws')
    }
  }
}
```

### 服务配置

服务配置在`src/config/api.config.js`中定义，将每个服务映射到对应的后端服务器：

```js
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
  // Spark服务 - 使用服务器2
  spark: {
    path: '/spark',
    server: servers.server2
  },
  // Kafka服务 - 使用服务器2
  kafka: {
    path: '/kafka',
    server: servers.server2
  },
  // 认证服务 - 使用默认服务器
  auth: {
    path: '/auth',
    server: servers.default
  },
  // 实时数据服务 - 使用服务器2
  realtime: {
    path: '/realtime',
    server: servers.server2
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
  
  // API方法...
}
```

## 项目启动

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev

# 构建生产版本
npm run build
``` 