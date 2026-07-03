<template>
  <div class="page-container">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-text">
        <h2>欢迎回来，{{ userStore.userInfo?.nickname || '管理员' }}</h2>
        <p>食品农产品溯源管理系统 · 一物一码，全程可追溯</p>
      </div>
      <div class="welcome-date">{{ currentDate }}</div>
    </div>

    <!-- 统计卡片 -->
    <el-row :gutter="16" class="stats-row">
      <el-col :span="6">
        <div class="stat-card" style="--accent: #059669">
          <div class="stat-icon">
            <el-icon :size="26"><OfficeBuilding /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.enterpriseCount || 0 }}</div>
            <div class="stat-label">入驻企业</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="--accent: #0891b2">
          <div class="stat-icon">
            <el-icon :size="26"><Medal /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.certCount || 0 }}</div>
            <div class="stat-label">认证数量</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="--accent: #7c3aed">
          <div class="stat-icon">
            <el-icon :size="26"><Document /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.orderCount || 0 }}</div>
            <div class="stat-label">订单总数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" style="--accent: #dc2626">
          <div class="stat-icon">
            <el-icon :size="26"><Box /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ stats.pendingOrders || 0 }}</div>
            <div class="stat-label">待审核订单</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 快捷操作 -->
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="16">
        <el-card class="quick-actions-card">
          <template #header><span>快捷操作</span></template>
          <el-row :gutter="12">
            <el-col :span="4" v-for="item in quickActions" :key="item.path">
              <div class="quick-action" @click="$router.push(item.path)">
                <div class="action-icon" :style="{ background: item.color }">
                  <el-icon :size="22"><component :is="item.icon" /></el-icon>
                </div>
                <span class="action-label">{{ item.label }}</span>
              </div>
            </el-col>
          </el-row>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="system-info-card">
          <template #header><span>系统信息</span></template>
          <div class="info-row">
            <span class="info-label">系统版本</span>
            <span class="info-value">v1.0.0</span>
          </div>
          <div class="info-row">
            <span class="info-label">溯源模式</span>
            <span class="info-value">一物一码</span>
          </div>
          <div class="info-row">
            <span class="info-label">管理机构</span>
            <span class="info-value">华测检测认证</span>
          </div>
          <div class="info-row">
            <span class="info-label">技术支撑</span>
            <span class="info-value">区块链 + IoT</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getAdminDashboard } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import {
  OfficeBuilding, Medal, Document, Box,
  Promotion, Goods, Bell, DataBoard
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const stats = ref<any>({})

const currentDate = computed(() => {
  const d = new Date()
  const days = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日 星期${days[d.getDay()]}`
})

const quickActions = [
  { label: '企业管理', path: '/admin/enterprise', icon: OfficeBuilding, color: '#059669' },
  { label: '产品管理', path: '/admin/product', icon: Goods, color: '#0891b2' },
  { label: '订单管理', path: '/admin/order', icon: Document, color: '#7c3aed' },
  { label: '码中台', path: '/admin/code-platform', icon: Promotion, color: '#d97706' },
  { label: '码包管理', path: '/admin/code-package', icon: Box, color: '#dc2626' },
  { label: '公告管理', path: '/admin/notice', icon: Bell, color: '#4f46e5' },
]

onMounted(async () => {
  try {
    const res = await getAdminDashboard()
    stats.value = res.data || {}
  } catch (e) {}
})
</script>

<style scoped lang="scss">
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  background: linear-gradient(135deg, #064e3b 0%, #047857 100%);
  border-radius: 12px;
  margin-bottom: 16px;
  color: #fff;

  .welcome-text {
    h2 {
      font-size: 20px;
      font-weight: 700;
      margin-bottom: 4px;
    }
    p {
      font-size: 13px;
      color: #a7f3d0;
    }
  }

  .welcome-date {
    font-size: 13px;
    color: #6ee7b7;
    white-space: nowrap;
  }
}

.stats-row {
  .stat-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 20px 22px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 1px 3px rgba(0,0,0,0.05);
    transition: transform 0.2s, box-shadow 0.2s;
    cursor: default;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0,0,0,0.08);
    }

    .stat-icon {
      width: 52px;
      height: 52px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--accent);
      color: #fff;
      flex-shrink: 0;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #1f2937;
      line-height: 1;
    }

    .stat-label {
      font-size: 13px;
      color: #9ca3af;
      margin-top: 4px;
    }
  }
}

.quick-actions-card {
  .quick-action {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 8px;
    padding: 16px 8px;
    border-radius: 8px;
    cursor: pointer;
    transition: background 0.2s;

    &:hover {
      background: #f9fafb;
    }

    .action-icon {
      width: 44px;
      height: 44px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
    }

    .action-label {
      font-size: 12px;
      color: #6b7280;
      font-weight: 500;
    }
  }
}

.system-info-card {
  .info-row {
    display: flex;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid #f5f5f5;
    font-size: 13px;

    &:last-child {
      border-bottom: none;
    }

    .info-label {
      color: #9ca3af;
    }

    .info-value {
      color: #374151;
      font-weight: 500;
    }
  }
}
</style>
