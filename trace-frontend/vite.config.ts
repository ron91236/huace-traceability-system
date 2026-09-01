import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// 本地开发时 API 代理目标 — 默认指向服务器后端，本地不运行任何服务
const apiTarget = process.env.VITE_API_TARGET || 'http://8.152.162.118:8080'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: apiTarget,
        changeOrigin: true,
      },
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-element': ['element-plus', '@element-plus/icons-vue'],
          'vendor-echarts': ['echarts/core', 'echarts/charts', 'echarts/components', 'echarts/renderers', 'echarts/features'],
        },
      },
    },
  },
})
