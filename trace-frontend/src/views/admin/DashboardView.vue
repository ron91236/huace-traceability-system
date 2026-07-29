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

    <!-- 数据中心 -->
    <div class="data-center-section" v-loading="screenLoading">
      <div class="section-title-bar">
        <h3>数据中心</h3>
        <span class="refresh-info">自动刷新 60s</span>
      </div>

      <!-- KPI -->
      <el-row :gutter="16" class="kpi-row">
        <el-col :span="6" v-for="(kpi, i) in kpis" :key="i">
          <div class="kpi-card">
            <div class="kpi-value">{{ kpi.value }}</div>
            <div class="kpi-label">{{ kpi.label }}</div>
          </div>
        </el-col>
      </el-row>

      <!-- 图表 -->
      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="8">
          <el-card class="chart-card">
            <template #header><span class="chart-title">扫码地域分布</span></template>
            <div ref="mapChartRef" class="chart-body"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="chart-card">
            <template #header><span class="chart-title">扫码趋势（近3月）</span></template>
            <div ref="trendChartRef" class="chart-body"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="chart-card">
            <template #header><span class="chart-title">城市排行 TOP10</span></template>
            <div ref="cityChartRef" class="chart-body"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" style="margin-top: 16px">
        <el-col :span="8">
          <el-card class="chart-card">
            <template #header><span class="chart-title">扫码率</span></template>
            <div ref="scanRateChartRef" class="chart-body"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="chart-card">
            <template #header><span class="chart-title">标签发放趋势（近半年）</span></template>
            <div ref="labelChartRef" class="chart-body"></div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card class="chart-card">
            <template #header><span class="chart-title">产品类别分布</span></template>
            <div ref="categoryChartRef" class="chart-body"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 企业扫码排行 -->
      <el-card style="margin-top: 16px" v-if="enterpriseRanking.length > 0">
        <template #header><span class="chart-title">企业扫码排行</span></template>
        <el-table :data="enterpriseRanking" size="small" stripe>
          <el-table-column prop="rank" label="排名" width="60" />
          <el-table-column prop="name" label="企业名称" />
          <el-table-column prop="scanCount" label="扫码次数" width="120" />
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getAdminDashboard, getAdminDataScreen } from '@/api/admin'
import { useUserStore } from '@/stores/user'
import {
  OfficeBuilding, Medal, Document, Box,
  Promotion, Goods, Bell, DataBoard
} from '@element-plus/icons-vue'

const userStore = useUserStore()
const stats = ref<any>({})
const screenLoading = ref(false)

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

// ====== 数据中心（图表） ======
const mapChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
const cityChartRef = ref<HTMLElement>()
const scanRateChartRef = ref<HTMLElement>()
const labelChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()

let charts: echarts.ECharts[] = []
let refreshTimer: ReturnType<typeof setInterval> | null = null

const kpis = ref([
  { label: '总库存', value: '-' },
  { label: '累计扫码', value: '-' },
  { label: '商家数', value: '-' },
  { label: '产品种类', value: '-' },
])

const enterpriseRanking = ref<any[]>([])

function initCharts() {
  charts.forEach(c => c.dispose())
  charts = []
  const refs = [mapChartRef, trendChartRef, cityChartRef, scanRateChartRef, labelChartRef, categoryChartRef]
  refs.forEach(r => {
    if (r.value) {
      charts.push(echarts.init(r.value))
    }
  })
}

function renderScreenData(data: any) {
  if (!data) return

  const kpi = data.kpi || {}
  kpis.value = [
    { label: '总库存', value: kpi.totalStock?.toLocaleString() || '0' },
    { label: '累计扫码', value: kpi.totalScans?.toLocaleString() || '0' },
    { label: '商家数', value: kpi.enterpriseCount?.toLocaleString() || '0' },
    { label: '产品种类', value: kpi.productTypes?.toLocaleString() || '0' },
  ]

  const axisColor = '#d1d5db'
  const splitColor = '#f0f0f0'

  // Province bar
  const provinceData = data.provinceScanData || []
  if (charts[0]) {
    charts[0].setOption({
      tooltip: { trigger: 'item' },
      xAxis: { type: 'category', data: provinceData.map((d: any) => d.name), axisLabel: { color: axisColor, fontSize: 10, rotate: 30 }, axisLine: { lineStyle: { color: splitColor } } },
      yAxis: { type: 'value', axisLabel: { color: axisColor }, splitLine: { lineStyle: { color: splitColor } } },
      series: [{ type: 'bar', data: provinceData.map((d: any) => d.value), itemStyle: { color: '#1890ff' } }],
      grid: { left: 40, right: 10, top: 10, bottom: 40 },
    })
  }

  // Trend line
  const trendData = data.scanTrend || []
  if (charts[1]) {
    charts[1].setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trendData.map((d: any) => d.date), axisLabel: { color: axisColor, fontSize: 10 }, axisLine: { lineStyle: { color: splitColor } } },
      yAxis: { type: 'value', axisLabel: { color: axisColor }, splitLine: { lineStyle: { color: splitColor } } },
      series: [{ type: 'line', data: trendData.map((d: any) => d.value), smooth: true, areaStyle: { color: 'rgba(24,144,255,0.15)' }, lineStyle: { color: '#1890ff' }, itemStyle: { color: '#1890ff' } }],
      grid: { left: 40, right: 10, top: 10, bottom: 30 },
    })
  }

  // City ranking
  const cityData = (data.cityRanking || []).slice(0, 10)
  if (charts[2]) {
    charts[2].setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'value', axisLabel: { color: axisColor }, splitLine: { lineStyle: { color: splitColor } } },
      yAxis: { type: 'category', data: cityData.map((d: any) => d.name).reverse(), axisLabel: { color: axisColor, fontSize: 10 }, axisLine: { lineStyle: { color: splitColor } } },
      series: [{ type: 'bar', data: cityData.map((d: any) => d.value).reverse(), itemStyle: { color: '#36cfc9' } }],
      grid: { left: 80, right: 10, top: 10, bottom: 10 },
    })
  }

  // Scan rate gauge
  const scanRate = data.scanRate || {}
  if (charts[3]) {
    charts[3].setOption({
      series: [{
        type: 'gauge',
        startAngle: 180, endAngle: 0,
        min: 0, max: 100,
        pointer: { show: false },
        progress: { show: true, width: 18, itemStyle: { color: '#1890ff' } },
        axisLine: { lineStyle: { width: 18, color: [[1, '#e5e7eb']] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        detail: { valueAnimation: true, fontSize: 24, color: '#1f2937', offsetCenter: [0, '10%'], formatter: '{value}%' },
        data: [{ value: scanRate.rate || 0, name: '扫码率' }],
        title: { color: axisColor, fontSize: 12, offsetCenter: [0, '40%'] },
      }],
    })
  }

  // Label trend
  const labelData = data.labelDistribution || []
  if (charts[4]) {
    charts[4].setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: labelData.map((d: any) => d.date), axisLabel: { color: axisColor, fontSize: 10 }, axisLine: { lineStyle: { color: splitColor } } },
      yAxis: { type: 'value', axisLabel: { color: axisColor }, splitLine: { lineStyle: { color: splitColor } } },
      series: [{ type: 'line', data: labelData.map((d: any) => d.value), smooth: true, areaStyle: { color: 'rgba(54,207,201,0.15)' }, lineStyle: { color: '#36cfc9' }, itemStyle: { color: '#36cfc9' } }],
      grid: { left: 40, right: 10, top: 10, bottom: 30 },
    })
  }

  // Category pie
  const categoryData = data.productCategoryRatio || []
  if (charts[5]) {
    charts[5].setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '65%'],
        label: { color: '#6b7280', fontSize: 11 },
        data: categoryData.map((d: any) => ({ name: d.name, value: d.value })),
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
      }],
    })
  }

  // Enterprise ranking
  enterpriseRanking.value = (data.enterpriseScanRanking || []).map((d: any, i: number) => ({
    rank: i + 1, name: d.name, scanCount: d.value,
  }))
}

async function fetchScreenData() {
  screenLoading.value = true
  try {
    const res = await getAdminDataScreen()
    renderScreenData(res.data)
  } catch {
    // 已处理
  } finally {
    screenLoading.value = false
  }
}

function handleResize() {
  charts.forEach(c => c.resize())
}

onMounted(async () => {
  // Load dashboard stats
  try {
    const res = await getAdminDashboard()
    stats.value = res.data || {}
  } catch (e) {}

  // Load data screen charts
  await nextTick()
  initCharts()
  fetchScreenData()
  refreshTimer = setInterval(fetchScreenData, 60000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
  charts.forEach(c => c.dispose())
  window.removeEventListener('resize', handleResize)
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

.data-center-section {
  margin-top: 24px;

  .section-title-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;

    h3 {
      font-size: 17px;
      font-weight: 600;
      color: #1f2937;
      margin: 0;
      padding-left: 12px;
      border-left: 4px solid #059669;
    }

    .refresh-info {
      font-size: 12px;
      color: #9ca3af;
    }
  }
}

.kpi-row {
  .kpi-card {
    background: #fff;
    border: 1px solid #f0f0f0;
    border-radius: 12px;
    padding: 20px;
    text-align: center;

    .kpi-value {
      font-size: 26px;
      font-weight: 700;
      color: #1890ff;
      font-family: 'DIN Alternate', monospace;
    }

    .kpi-label {
      font-size: 13px;
      color: #9ca3af;
      margin-top: 4px;
    }
  }
}

.chart-card {
  .chart-title {
    font-size: 14px;
    font-weight: 600;
    color: #374151;
  }

  .chart-body {
    width: 100%;
    height: 260px;
  }
}
</style>
