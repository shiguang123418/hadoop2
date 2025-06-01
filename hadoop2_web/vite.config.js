import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'


const host = '192.168.1.192'
// 多后端API服务器配置
const apiServers = {
  // API服务1配置 - 保留原有API
  api1: {
    target: `http://${host}:8000`,
    pathRewrite: path => path.replace(/^\/api1/, '/api')
  },
  // API服务2配置 - 新增API
  api2: {
    target: `http://${host}:8001`, 
    pathRewrite: path => path.replace(/^\/api2/, '/api')
  },
  // WebSocket服务配置 - 使用IP地址确保更好的兼容性
  ws: {
    target: `http://${host}:8001`,
    pathRewrite: path => path.replace(/^\/api_ws/, '/api/ws')
  }
};

// 默认API服务器配置
// 注意：此处的配置应与api.config.js中保持一致
// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue()
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  define: {
    global: 'window',
    // 将API服务器配置暴露给前端代码
    'process.env.API_SERVERS': JSON.stringify(apiServers)
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    cors: true, // 设置没有访问限制
    proxy: {
      // API1代理配置
      '/api1': {
        target: apiServers.api1.target,
        changeOrigin: true,
        secure: false,
        ws: false,
        rewrite: apiServers.api1.pathRewrite,
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('API1代理错误:', err);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('API1代理请求:', req.method, req.url, '->',
                         options.target + proxyReq.path);
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('API1代理响应:', proxyRes.statusCode, req.url);
          });
        }
      },
      // API2代理配置
      '/api2': {
        target: apiServers.api2.target,
        changeOrigin: true,
        secure: false,
        ws: false,
        rewrite: apiServers.api2.pathRewrite,
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.log('API2代理错误:', err);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('API2代理请求:', req.method, req.url, '->',
                         options.target + proxyReq.path);
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('API2代理响应:', proxyRes.statusCode, req.url);
          });
        }
      },
      
      // WebSocket代理配置
      '/api_ws': {
        target: apiServers.ws.target,
        changeOrigin: true,
        secure: false,
        ws: true, // 启用WebSocket代理
        rewrite: apiServers.ws.pathRewrite,
        configure: (proxy, options) => {
          proxy.on('error', (err, req, res) => {
            console.error('WebSocket代理错误:', err);
          });
          proxy.on('proxyReq', (proxyReq, req, res) => {
            console.log('WebSocket代理请求:', req.method, req.url, '->',
                         options.target + proxyReq.path);
          });
          proxy.on('proxyRes', (proxyRes, req, res) => {
            console.log('WebSocket代理响应:', proxyRes.statusCode, req.url);
          });
        }
      },

    }
  },
  build: {
    outDir: 'dist', // 输出目录
    assetsDir: 'assets', // 静态资源目录
    assetsInlineLimit: 4096, // 小于此大小的资源将内联为base64
    minify: 'terser', // 压缩方式
    terserOptions: {
      compress: {
        drop_console: true, // 移除console
        drop_debugger: true // 移除debugger
      }
    },
    chunkSizeWarningLimit: 500 // 块大小警告限制
  },
  base: '/' // 部署应用的基本URL路径
});
