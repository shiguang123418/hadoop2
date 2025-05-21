# API 配置说明

## 概述

本项目采用集中式API配置管理，便于在不同环境中切换API服务器地址。所有API相关配置均在 `src/config/api.config.js` 文件中统一管理。

## 如何修改API服务器地址

当需要更改API服务器地址时，只需修改以下两个文件中的相关配置：

### 1. 前端API配置文件

打开文件: `src/config/api.config.js`

找到以下配置部分:

```javascript
// ======================================================
// 【重要】修改此处可统一更改所有API服务地址
// ======================================================
const API_SERVER_HOST = 'localhost'; // 修改此处设置API服务器主机名/IP
const API_SERVER_PORT = '8080';      // 修改此处设置API服务器端口
// ======================================================
```

将 `API_SERVER_HOST` 修改为实际API服务器的IP地址或主机名，将 `API_SERVER_PORT` 修改为实际API服务器的端口号。

### 2. 开发服务器代理配置文件

打开文件: `vite.config.js`

找到以下配置部分:

```javascript
// 默认API服务器配置
// 注意：此处的配置应与api.config.js中保持一致
const API_SERVER_HOST = 'localhost';
const API_SERVER_PORT = '8080';
```

确保这里的配置与 `api.config.js` 中的配置保持一致。

## 配置优先级

1. 开发环境下使用本地代理，所有API请求会通过Vite的开发服务器代理转发到实际的API服务器
2. 生产环境下直接请求API服务器

## 配置结构

API配置文件中包含以下主要配置项：

- `apiServer`: API服务器的完整地址
- `apiBasePath`: API的基础路径
- `useProxy`: 是否使用本地代理
- `services`: 各个服务的路径配置
  - `hdfs`: HDFS相关API的路径
  - `hive`: Hive相关API的路径
  - `spark`: Spark相关API的路径
  - `auth`: 认证相关API的路径

## 重要提示

1. 修改API配置后，需要重启开发服务器才能使新配置生效
2. 对于生产环境部署，修改配置后需要重新构建前端应用
3. 请确保vite.config.js和api.config.js中的API服务器配置保持一致 