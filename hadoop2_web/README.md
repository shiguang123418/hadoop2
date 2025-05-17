# Hadoop2 Web应用

一个基于Vite构建的现代Vue 3数据分析、可视化和农作物分析Web应用。

## 功能特点

- **用户认证**：安全的登录系统
- **数据上传**：上传和管理数据集
- **数据分析**：分析农业数据
- **数据可视化**：使用Chart.js和ECharts的交互式图表
- **农作物分析**：专业的农作物数据分析工具
- **实时更新**：通过SockJS和STOMP实现WebSocket通信

## 技术栈

- **前端**：Vue 3, Vue Router
- **构建工具**：Vite
- **HTTP客户端**：Axios
- **可视化**：Chart.js, ECharts
- **WebSocket**：SockJS, STOMP
- **API代理**：配置无缝后端集成

## 开发设置

### 前提条件

- Node.js（推荐最新LTS版本）
- npm或yarn

### 安装

```sh
npm install
```

### 开发服务器

```sh
npm run dev
```

这将在`http://localhost:5173/`启动开发服务器

### 生产构建

```sh
npm run build
```

## 项目结构

- `/src/assets` - 静态资源
- `/src/components` - 可重用Vue组件
- `/src/router` - Vue Router配置
- `/src/services` - API服务和WebSocket集成
- `/src/views` - 页面组件
  - HomePage - 仪表盘视图
  - Login - 身份验证
  - DataUpload - 文件上传界面
  - DataAnalysis - 数据分析工具
  - DataVisualization - 图表和可视化
  - CropAnalysis - 农业分析

## API配置

该应用配置为与后端API通信。API代理在`vite.config.js`中设置。

## 部署

### 生产环境构建

```sh
npm run build
```

### Nginx配置

使用Nginx部署此应用：

1. 构建项目
2. 将`dist`目录复制到Nginx网站根目录
3. 为单页应用配置Nginx：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    root /path/to/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://your-backend-server:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

4. 重启Nginx
```sh
sudo systemctl restart nginx
```