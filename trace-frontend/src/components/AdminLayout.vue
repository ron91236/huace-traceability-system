<template>
  <el-container class="layout-container">
    <el-aside :width="collapsed ? '64px' : '220px'" class="aside">
      <div class="logo">
        <img :src="collapsed ? logoIcon : logoFull" :alt="collapsed ? 'CTi' : 'CTi华测检测'" class="logo-img" :class="{ 'logo-collapsed': collapsed }" />
      </div>
      <el-scrollbar class="menu-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="collapsed"
          router
          background-color="#064e3b"
          text-color="#a7f3d0"
          active-text-color="#fff"
        >
          <el-menu-item index="/admin/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>数据中心</span>
          </el-menu-item>
          <el-menu-item index="/admin/notice">
            <el-icon><Bell /></el-icon>
            <span>公告管理</span>
          </el-menu-item>
          <el-sub-menu index="enterprise-mgmt">
            <template #title>
              <el-icon><OfficeBuilding /></el-icon>
              <span>企业管理</span>
            </template>
            <el-menu-item index="/admin/enterprise">企业列表</el-menu-item>
            <el-menu-item index="/admin/enterprise-cert">企业认证</el-menu-item>
            <el-menu-item index="/admin/cert-type">证书类型</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="product-mgmt">
            <template #title>
              <el-icon><Goods /></el-icon>
              <span>产品管理</span>
            </template>
            <el-menu-item index="/admin/product">产品列表</el-menu-item>
            <el-menu-item index="/admin/label-spec">标签规格</el-menu-item>
            <el-menu-item index="/admin/base-manage">企业基地</el-menu-item>
            <el-menu-item index="/admin/goods">商品管理</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="order-mgmt">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>订单管理</span>
            </template>
            <el-menu-item index="/admin/order">订单列表</el-menu-item>
          </el-sub-menu>
          <el-sub-menu index="code-mgmt">
            <template #title>
              <el-icon><Promotion /></el-icon>
              <span>溯源码管理</span>
            </template>
            <el-menu-item index="/admin/code-platform">码中台</el-menu-item>
            <el-menu-item index="/admin/code-package">码包管理</el-menu-item>
            <el-menu-item index="/admin/code-void">溯源码作废</el-menu-item>
            <el-menu-item index="/admin/code-distribution">发放管理</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/admin/trace-template">
            <el-icon><PictureFilled /></el-icon>
            <span>溯源模板</span>
          </el-menu-item>
          <el-sub-menu index="iot-video-mgmt">
            <template #title>
              <el-icon><Monitor /></el-icon>
              <span>视频/IoT</span>
            </template>
            <el-menu-item index="/admin/video-source">视频源总览</el-menu-item>
            <el-menu-item index="/admin/iot-device">IoT设备总览</el-menu-item>
            <el-menu-item index="/admin/vr-manage">VR全景管理</el-menu-item>
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
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || '管' }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || '管理员' }}</span>
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
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import logoIcon from '@/assets/logo-icon.png'
import logoFull from '@/assets/logo-full.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => (route.meta.title as string) || '控制台')

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
  background: #064e3b;
  transition: width 0.3s;
  overflow: hidden;
  box-shadow: 2px 0 8px rgba(0,0,0,0.15);

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    color: #333;
    background: #fff;
    border-bottom: 1px solid #e5e7eb;

    .logo-img {
      height: 32px;
      width: auto;
      object-fit: contain;
      &.logo-collapsed {
        height: 28px;
      }
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
