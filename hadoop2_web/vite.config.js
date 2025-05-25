import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

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
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    cors: true, // 设置没有访问限制
    proxy: {
      '/api': {
        target: 'http://shiguang:8000',
        changeOrigin: true,
        secure: false,
        ws: true
      },
      '/api_ws': {
        target: 'ws://shiguang:8000',
        ws: true,
        changeOrigin: true,
        secure: false,
        rewrite: (path) => path.replace(/^\/api_ws/, '/ws')
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
