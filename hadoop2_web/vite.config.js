import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 默认API服务器配置
// 注意：此处的配置应与api.config.js中保持一致
const API_SERVER_HOST = '192.168.1.192';
const API_SERVER_PORT = '8080';

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
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    cors: true, // 设置没有访问限制
    proxy: {
      '/api': {
        target: `http://${API_SERVER_HOST}:${API_SERVER_PORT}`,
        changeOrigin: true,
        secure: false,
        ws: true, // 启用WebSocket代理
        // 调试时可以查看代理的请求
        configure: (proxy, options) => {
          console.log(`代理配置已加载: /api -> ${options.target}`);
          
          // 可以添加代理事件监听以调试
          proxy.on('proxyReq', function(proxyReq, req, res, options) {
            console.log('代理请求:', req.method, req.url, '-> 转发到:', options.target + req.url);
          });
          
          proxy.on('proxyRes', function(proxyRes, req, res) {
            console.log('代理响应:', proxyRes.statusCode, req.url);
          });
          
          proxy.on('error', function(err, req, res) {
            console.error('代理错误:', err, req.url);
          });
        }
      }
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
