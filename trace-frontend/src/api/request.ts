import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 30000,
})

// 防重入锁：避免多个 401 响应同时触发多次 logout/跳转
let isLoggingOut = false

function handle401() {
  if (isLoggingOut) return
  isLoggingOut = true
  const userStore = useUserStore()
  userStore.logout()
  router.push('/login')
  ElMessage.error('登录已过期，请重新登录')
  // 短暂延迟后重置，防止极端情况下同一会话无法再次处理
  setTimeout(() => { isLoggingOut = false }, 3000)
}

// 请求拦截器 - 添加 JWT token
request.interceptors.request.use(
  (config) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器 - 统一错误处理
request.interceptors.response.use(
  (response) => {
    // Blob 类型响应（文件下载）直接返回，不解析 code
    if (response.config.responseType === 'blob') {
      return response
    }
    const res = response.data
    if (res.code !== 200 && res.code !== 0) {
      ElMessage.error(res.msg || '请求失败')
      if (res.code === 401) {
        handle401()
      }
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      handle401()
    } else {
      ElMessage.error(error.response?.data?.msg || error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

export default request
