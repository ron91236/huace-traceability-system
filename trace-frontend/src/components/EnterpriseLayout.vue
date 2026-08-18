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
          background-color="#1e3a2f"
          text-color="#a7f3d0"
          active-text-color="#fff"
        >
          <el-menu-item index="/enterprise/dashboard">
            <el-icon><DataBoard /></el-icon>
            <span>控制台</span>
          </el-menu-item>
          <el-menu-item index="/enterprise/cert">
            <el-icon><Medal /></el-icon>
            <span>企业认证</span>
          </el-menu-item>
          <el-menu-item index="/enterprise/profile">
            <el-icon><OfficeBuilding /></el-icon>
            <span>企业信息</span>
          </el-menu-item>
          <el-menu-item index="/enterprise/base">
            <el-icon><Location /></el-icon>
            <span>基地管理</span>
          </el-menu-item>
          <el-menu-item index="/enterprise/goods">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/enterprise/address">
            <el-icon><MapLocation /></el-icon>
            <span>收货地址</span>
          </el-menu-item>
          <el-menu-item index="/enterprise/test-report">
            <el-icon><Document /></el-icon>
            <span>检测报告</span>
          </el-menu-item>
          <el-menu-item index="/enterprise/batch">
            <el-icon><Tickets /></el-icon>
            <span>批次管理</span>
          </el-menu-item>
          <el-sub-menu index="order-mgmt">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>订单管理</span>
            </template>
            <el-menu-item index="/enterprise/order">订单列表</el-menu-item>
            <el-menu-item index="/enterprise/order-code">订单条码</el-menu-item>
            <el-menu-item index="/enterprise/code-usage">条码使用</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/enterprise/notice">
            <el-icon><Bell /></el-icon>
            <span>公告</span>
          </el-menu-item>
          <el-sub-menu index="iot-video-mgmt">
            <template #title>
              <el-icon><Monitor /></el-icon>
              <span>视频/IoT</span>
            </template>
            <el-menu-item index="/enterprise/video-source">视频源管理</el-menu-item>
            <el-menu-item index="/enterprise/iot-device">IoT设备</el-menu-item>
            <el-menu-item index="/enterprise/iot-alert">IoT告警</el-menu-item>
          </el-sub-menu>
          <el-menu-item index="/dl/dashboard">
            <el-icon><Collection /></el-icon>
            <span>数字标签系统</span>
          </el-menu-item>
          <el-menu-item index="/screen/enterprise">
            <el-icon><TrendCharts /></el-icon>
            <span>数据大屏</span>
          </el-menu-item>
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
          <!-- 母账号企业切换 -->
          <el-select v-if="userStore.isMasterAccount && children.length > 0"
            v-model="selectedChildId" placeholder="全部企业" clearable
            size="small" style="width: 180px" @change="onChildChange">
            <el-option label="全部企业（聚合）" :value="null" />
            <el-option v-for="c in children" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <span class="enterprise-name" v-if="userStore.userInfo?.enterpriseName">
            {{ userStore.userInfo.enterpriseName }}
          </span>
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-avatar :size="32" class="user-avatar">
                {{ userStore.userInfo?.nickname?.charAt(0) || '企' }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.nickname || '企业用户' }}</span>
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
import { getGroupChildren } from '@/api/enterprise'
import logoIcon from '@/assets/logo-icon.png'
import logoFull from '@/assets/logo-full.png'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const collapsed = ref(false)
const children = ref<{ id: number; name: string }[]>([])
const selectedChildId = ref<number | null>(null)

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => (route.meta.title as string) || '控制台')

onMounted(async () => {
  // Load child enterprises for master accounts
  if (userStore.isMasterAccount) {
    try {
      const res = await getGroupChildren()
      children.value = res.data || []
    } catch (e) {}
  }
})

function onChildChange(childId: number | null) {
  userStore.setCurrentViewEnterpriseId(childId)
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
