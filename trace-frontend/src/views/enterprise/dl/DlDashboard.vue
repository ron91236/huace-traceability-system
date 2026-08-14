<template>
  <div class="page-container dl-dashboard">
    <!-- 顶部操作 -->
    <el-card class="mb-card">
      <div class="dash-actions">
        <el-button type="primary" @click="$router.push('/enterprise/dl/products')">
          <el-icon><Plus /></el-icon>创建数字标签
        </el-button>
        <el-button @click="$router.push('/enterprise/dl/products')">查看版本</el-button>
        <el-button @click="$router.push('/enterprise/dl/analysis/scan')">扫码分析</el-button>
      </div>
    </el-card>

    <!-- 统计卡片 -->
    <div class="stat-row">
      <el-card class="stat-card">
        <div class="stat-label">商品总数</div>
        <div class="stat-value">{{ data.productCount || 0 }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">标签总数</div>
        <div class="stat-value">{{ data.versionCount || 0 }}</div>
      </el-card>
      <el-card class="stat-card">
        <div class="stat-label">发布标签总数</div>
        <div class="stat-value">{{ data.publishedCount || 0 }}</div>
      </el-card>
      <el-card class="stat-card chart-card">
        <div class="stat-label">标签发布状态分布</div>
        <div ref="statusChartRef" class="mini-chart"></div>
      </el-card>
    </div>

    <!-- 扫码统计 -->
    <el-card class="mb-card">
      <template #header><span class="card-title">扫码统计</span></template>
      <div class="scan-stats">
        <div class="scan-item"><div class="scan-num">{{ data.scanStats?.today || 0 }}</div><div class="scan-label">今天</div></div>
        <div class="scan-item"><div class="scan-num">{{ data.scanStats?.yesterday || 0 }}</div><div class="scan-label">昨天</div></div>
        <div class="scan-item"><div class="scan-num">{{ data.scanStats?.last7d || 0 }}</div><div class="scan-label">近7天</div></div>
        <div class="scan-item"><div class="scan-num">{{ data.scanStats?.last14d || 0 }}</div><div class="scan-label">近14天</div></div>
        <div class="scan-item"><div class="scan-num">{{ data.scanStats?.last30d || 0 }}</div><div class="scan-label">近30天</div></div>
      </div>
    </el-card>

    <!-- 扫码趋势 -->
    <el-card class="mb-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-title">扫码数据趋势</span>
          <el-radio-group v-model="trendDays" size="small" @change="loadData">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="14">近14天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
            <el-radio-button :value="60">近60天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendChartRef" class="trend-chart"></div>
    </el-card>

    <!-- TOP10 -->
    <el-card class="mb-card">
      <template #header><span class="card-title">商品扫码 TOP10</span></template>
      <div ref="topChartRef" class="trend-chart"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDlDashboard, recordDlLogin } from '@/api/digital-label'

const data = ref<any>({})
const trendDays = ref(7)
const statusChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
const topChartRef = ref<HTMLElement>()
let statusChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null
let topChart: echarts.ECharts | null = null

async function loadData() {
  try {
    const res = await getDlDashboard(trendDays.value)
    data.value = res.data || {}
    await nextTick()
    renderCharts()
  } catch (e) {}
}

function renderCharts() {
  // 状态分布环形图
  if (statusChartRef.value) {
    if (!statusChart) statusChart = echarts.init(statusChartRef.value)
    const dist = data.value.statusDistribution || {}
    statusChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0, textStyle: { fontSize: 11 } },
      series: [{
        type: 'pie', radius: ['45%', '70%'], center: ['50%', '42%'],
        label: { show: false },
        data: [
          { name: '草稿', value: dist.draft || 0, itemStyle: { color: '#909399' } },
          { name: '已发布', value: dist.published || 0, itemStyle: { color: '#67c23a' } },
          { name: '已下架', value: dist.offline || 0, itemStyle: { color: '#e6a23c' } },
        ],
      }],
    })
  }
  // 扫码趋势折线图
  if (trendChartRef.value) {
    if (!trendChart) trendChart = echarts.init(trendChartRef.value)
    const trend = data.value.scanTrend || []
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: trend.map((t: any) => t.date.slice(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ name: '扫码次数', type: 'line', smooth: true, data: trend.map((t: any) => t.count), areaStyle: { opacity: 0.15 }, itemStyle: { color: '#059669' } }],
    }, true)
  }
  // TOP10 柱状图
  if (topChartRef.value) {
    if (!topChart) topChart = echarts.init(topChartRef.value)
    const top = (data.value.topProducts || []).slice().reverse()
    topChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 140, right: 30, top: 20, bottom: 30 },
      xAxis: { type: 'value', minInterval: 1 },
      yAxis: { type: 'category', data: top.map((t: any) => t.foodName || t.barcode) },
      series: [{ name: '扫码次数', type: 'bar', data: top.map((t: any) => t.scanCount), itemStyle: { color: '#059669', borderRadius: [0, 4, 4, 0] }, barMaxWidth: 18 }],
    }, true)
  }
}

function handleResize() {
  statusChart?.resize(); trendChart?.resize(); topChart?.resize()
}

onMounted(() => {
  recordDlLogin().catch(() => {})
  loadData()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  statusChart?.dispose(); trendChart?.dispose(); topChart?.dispose()
})
</script>

<style scoped lang="scss">
.dl-dashboard {
  .mb-card { margin-bottom: 16px; }
  .dash-actions { display: flex; gap: 10px; }
  .card-title { font-weight: 600; font-size: 15px; }
  .card-header-row { display: flex; align-items: center; justify-content: space-between; }
}
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
  .stat-card {
    .stat-label { font-size: 13px; color: #6b7280; margin-bottom: 8px; }
    .stat-value { font-size: 30px; font-weight: 700; color: #059669; }
    &.chart-card .mini-chart { height: 110px; }
  }
}
.scan-stats {
  display: flex;
  gap: 24px;
  .scan-item {
    flex: 1;
    text-align: center;
    padding: 12px 0;
    background: #f8faf9;
    border-radius: 8px;
    .scan-num { font-size: 24px; font-weight: 700; color: #1f2937; }
    .scan-label { font-size: 12px; color: #9ca3af; margin-top: 4px; }
  }
}
.trend-chart { height: 300px; }
</style>
