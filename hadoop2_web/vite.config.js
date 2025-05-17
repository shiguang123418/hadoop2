import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

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
        target: 'http://192.168.1.192:8080/api', // 后端服务器地址
        rewrite: (path) => path.replace(/^\/api/, ''),
        changeOrigin: true,
        secure: false,
        ws: true
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
})
