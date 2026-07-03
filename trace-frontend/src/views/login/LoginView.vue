<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-overlay"></div>
    </div>
    <div class="login-box">
      <div class="login-header">
        <div class="logo-icon">
          <img :src="logoFull" alt="CTi" class="logo-img" />
        </div>
        <h1>食品农产品溯源系统</h1>
        <p class="subtitle">Food & Agriculture Traceability Platform</p>
        <p class="tagline">一物一码 · 全程可追溯 · 安全有保障</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            native-type="submit"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <span>华测检测认证 · 品质保障</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/stores/user'
import logoFull from '@/assets/logo-full.png'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({
      username: form.username,
      password: form.password,
    })

    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.user)

    ElMessage.success('登录成功')

    const redirect = route.query.redirect as string
    // 防止开放重定向：只允许站内路径
    if (redirect && redirect.startsWith('/') && !redirect.startsWith('//')) {
      router.push(redirect)
    } else {
      const userType = res.data.user?.userType
      router.push(userType === 'enterprise' ? '/enterprise' : '/admin')
    }
  } catch {
    // request 拦截器已处理错误提示，此处不重复弹出
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.login-bg {
  position: absolute;
  inset: 0;
  background: linear-gradient(135deg, #064e3b 0%, #065f46 25%, #047857 50%, #059669 75%, #10b981 100%);
  background: url('@/assets/login-bg.png') center/cover no-repeat,
    linear-gradient(135deg, #064e3b 0%, #047857 50%, #10b981 100%);

  .bg-overlay {
    position: absolute;
    inset: 0;
    background: rgba(0, 0, 0, 0.25);
  }
}

.login-box {
  width: 440px;
  padding: 44px 40px 32px;
  background: rgba(255, 255, 255, 0.98);
  border-radius: 16px;
  box-shadow: 0 25px 60px rgba(0, 0, 0, 0.2), 0 0 0 1px rgba(255,255,255,0.1);
  position: relative;
  z-index: 1;
  backdrop-filter: blur(20px);
}

.login-header {
  text-align: center;
  margin-bottom: 28px;

  .logo-icon {
    margin-bottom: 12px;
  }

  .logo-img {
    height: 48px;
    width: auto;
    object-fit: contain;
  }

  h1 {
    font-size: 22px;
    font-weight: 700;
    color: #064e3b;
    margin-bottom: 4px;
    letter-spacing: 2px;
  }

  .subtitle {
    font-size: 11px;
    color: #9ca3af;
    letter-spacing: 1px;
    margin-bottom: 8px;
  }

  .tagline {
    font-size: 13px;
    color: #059669;
    letter-spacing: 1px;
    font-weight: 500;
  }
}

:deep(.el-input__wrapper) {
  box-shadow: 0 0 0 1px #d1d5db inset;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 0 0 1px #059669 inset;
  }

  &.is-focus {
    box-shadow: 0 0 0 1px #059669 inset;
  }
}

.login-btn {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 4px;
  background: linear-gradient(135deg, #059669, #047857) !important;
  border: none !important;
  border-radius: 8px !important;

  &:hover {
    background: linear-gradient(135deg, #047857, #065f46) !important;
  }
}

.login-footer {
  text-align: center;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f3f4f6;
  font-size: 12px;
  color: #9ca3af;
}
</style>
