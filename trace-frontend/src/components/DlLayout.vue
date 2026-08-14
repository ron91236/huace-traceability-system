<template>
  <el-container class="layout-container">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo">
        <img :src="logoIcon" alt="CTi" class="logo-img" />
        <span v-if="!collapsed" class="logo-title">数字标签管理系统</span>
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          router
          background-color="#1e3a2f"
          text-color="#a7f3d0"
          active-text-color="#fff"
        >
          <el-sub-menu index="dl-mgmt">
            <template #title>
              <el-icon><Collection /></el-icon>
              <span>数字标签</span>
            </template>
            <el-menu-item index="/dl/dashboard">工作台</el-menu-item>
            <el-menu-item index="/dl/products">商品管理</el-menu-item>
            <el-menu-item v-if="!isAdmin" index="/dl/sync">商品同步</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="dl-analysis">
            <template #title>
              <el-icon><PieChart /></el-icon>
              <span>数据分析</span>
            </template>
            <el-menu-item index="/dl/analysis/scan">扫码分析</el-menu-item>
            <el-menu-item index="/dl/analysis/label">标签分析</el-menu-item>
            <el-menu-item index="/dl/analysis/product">商品分析</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="dl-enterprise">
            <template #title>
              <el-icon><User /></el-icon>
              <span>企业管理</span>
            </template>
            <el-menu-item index="/dl/users">用户管理</el-menu-item>
            <el-menu-item index="/dl/logs/operation">操作日志</el-menu-item>
            <el-menu-item index="/dl/logs/login">登录日志</el-menu-item>
          </el-sub-menu>
        </el-menu>
      </el-scrollbar>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="collapsed = !collapsed" :size="20">
            <Fold v-if="!collapsed" />
            <Expand v-else />
          </el-icon>
          <span class="page-title">{{ pageTitle }}</span>
          <el-tag v-if="isAdmin" type="warning" size="small" effect="dark">管理员 · 全局只读</el-tag>
        </div>
        <div class="header-right">
          <span v-if="!isAdmin && userStore.userInfo?.enterpriseName" class="enterprise-name">
            {{ userStore.userInfo.enterpriseName }}
          </span>
          <el-button size="small" @click="goBack">
            <el-icon style="margin-right:4px"><Back /></el-icon>返回溯源系统
          </el-button>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || (isAdmin ? '管' : '企') }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || (isAdmin ? '管理员' : '企业用户') }}</span>
              <el-icon class="arrow-icon"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { recordDlLogin } from '@/api/digital-label'
import logoIcon from '@/assets/logo-icon.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)

const isAdmin = computed(() => userStore.isAdmin)
const activeMenu = computed(() => route.path)
const pageTitle = computed(() => (route.meta.title as string) || '数字标签')

onMounted(() => {
  // 企业用户进入数字标签模块时记录登录日志
  if (!isAdmin.value) recordDlLogin().catch(() => {})
})

function goBack() {
  router.push(isAdmin.value ? '/admin/dashboard' : '/enterprise/dashboard')
}

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.aside {
  background: #1e3a2f;
  transition: width 0.3s;
  overflow: hidden;
  box-shadow: 2px 0 8px rgba(0,0,0,0.15);

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    background: #fff;
    border-bottom: 1px solid #e5e7eb;
    overflow: hidden;

    .logo-img {
      height: 30px;
      width: auto;
      object-fit: contain;
    }

    .logo-title {
      font-size: 15px;
      font-weight: 700;
      color: #065f46;
      white-space: nowrap;
    }
  }

  .menu-scroll {
    height: calc(100vh - 60px);
  }

  .el-menu {
    border-right: none;
    --el-menu-hover-bg-color: rgba(255,255,255,0.08);

    .el-menu-item.is-active {
      background: rgba(255,255,255,0.12) !important;
      border-right: 3px solid #34d399;
    }

    .el-sub-menu .el-menu-item {
      background: rgba(0,0,0,0.15) !important;
      min-width: auto;
    }
  }
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
  padding: 0 24px;
  height: 60px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);

  .header-left {
    display: flex;
    align-items: center;
    gap: 14px;
  }

  .collapse-btn {
    cursor: pointer;
    color: #6b7280;
    transition: color 0.2s;
    &:hover { color: #059669; }
  }

  .page-title {
    font-size: 17px;
    font-weight: 600;
    color: #1f2937;
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 16px;
  }

  .enterprise-name {
    font-size: 13px;
    color: #065f46;
    padding: 4px 14px;
    background: #ecfdf5;
    border-radius: 20px;
    border: 1px solid #a7f3d0;
    font-weight: 500;
  }

  .user-info {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
    padding: 6px 12px;
    border-radius: 8px;
    transition: background 0.2s;

    &:hover { background: #f9fafb; }

    .user-avatar {
      background: linear-gradient(135deg, #059669, #047857);
      color: #fff;
      font-size: 14px;
      font-weight: 600;
    }

    .username {
      font-size: 14px;
      color: #374151;
      font-weight: 500;
    }

    .arrow-icon {
      font-size: 12px;
      color: #9ca3af;
    }
  }
}

.main {
  background: #f5f7f5;
  padding: 20px;
  overflow-y: auto;
}
</style>
