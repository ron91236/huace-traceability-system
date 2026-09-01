<template>
  <div class="data-screen" v-loading="loading">
    <div class="screen-header">
      <div class="header-left">
        <el-button text style="color:#fff" @click="$router.push('/admin/dashboard')">
          <el-icon><Back /></el-icon>返回
        </el-button>
      </div>
      <div class="header-center">
        <h1>认证标签大数据中心</h1>
        <p>{{ currentTime }}</p>
      </div>
      <div class="header-right">
        <span class="refresh-info">自动刷新 60s</span>
      </div>
    </div>

    <div class="kpi-row">
      <div class="kpi-card" v-for="(kpi, i) in kpis" :key="i">
        <div class="kpi-value">{{ kpi.value }}</div>
        <div class="kpi-label">{{ kpi.label }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="chart-card">
        <div class="chart-title">扫码地域分布</div>
        <div ref="mapChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">扫码趋势（近3月）</div>
        <div ref="trendChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">城市排行 TOP10</div>
        <div ref="cityChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">扫码率</div>
        <div ref="scanRateChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">标签发放趋势（近半年）</div>
        <div ref="labelChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-title">产品类别分布</div>
        <div ref="categoryChartRef" class="chart-body"></div>
      </div>
    </div>

    <div class="ranking-section" v-if="enterpriseRanking.length > 0">
      <div class="chart-title">企业扫码排行</div>
      <el-table :data="enterpriseRanking" size="small" :header-cell-style="{ background: '#0d2b4a', color: '#7ec8e3' }"
        :row-style="{ background: 'transparent', color: '#c0d8f0' }" border style="width:100%">
        <el-table-column prop="rank" label="排名" width="60" />
        <el-table-column prop="name" label="企业名称" />
        <el-table-column prop="scanCount" label="扫码次数" width="120" />
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from '@/utils/echarts'
import { getAdminDataScreen } from '@/api/admin'

const loading = ref(false)
const currentTime = ref('')
let timer: ReturnType<typeof setInterval> | null = null
let refreshTimer: ReturnType<typeof setInterval> | null = null

const mapChartRef = ref<HTMLElement>()
const trendChartRef = ref<HTMLElement>()
const cityChartRef = ref<HTMLElement>()
const scanRateChartRef = ref<HTMLElement>()
const labelChartRef = ref<HTMLElement>()
const categoryChartRef = ref<HTMLElement>()

let charts: echarts.ECharts[] = []

const kpis = ref([
  { label: '总库存', value: '-' },
  { label: '累计扫码', value: '-' },
  { label: '商家数', value: '-' },
  { label: '产品种类', value: '-' },
])

const enterpriseRanking = ref<any[]>([])

function updateTime() {
  currentTime.value = new Date().toLocaleString('zh-CN')
}

const darkTextColor = '#7ec8e3'
const darkAxisColor = '#1a4a6e'

function initCharts() {
  charts.forEach(c => c.dispose())
  charts = []
  const refs = [mapChartRef, trendChartRef, cityChartRef, scanRateChartRef, labelChartRef, categoryChartRef]
  refs.forEach(r => {
    if (r.value) {
      const chart = echarts.init(r.value)
      charts.push(chart)
    }
  })
}

function renderData(data: any) {
  if (!data) return

  // KPI
  const kpi = data.kpi || {}
  kpis.value = [
    { label: '总库存', value: kpi.totalInventory?.toLocaleString() || '0' },
    { label: '累计扫码', value: kpi.totalScans?.toLocaleString() || '0' },
    { label: '商家数', value: kpi.merchantCount?.toLocaleString() || '0' },
    { label: '产品种类', value: kpi.productCount?.toLocaleString() || '0' },
  ]

  // Map chart (fallback to bar if no geo)
  const provinceData = data.provinceScan || []
  if (charts[0]) {
    charts[0].setOption({
      tooltip: { trigger: 'item' },
      xAxis: { type: 'category', data: provinceData.map((d: any) => d.province), axisLabel: { color: darkTextColor, fontSize: 10, rotate: 30 }, axisLine: { lineStyle: { color: darkAxisColor } } },
      yAxis: { type: 'value', axisLabel: { color: darkTextColor }, splitLine: { lineStyle: { color: darkAxisColor } } },
      series: [{ type: 'bar', data: provinceData.map((d: any) => d.count), itemStyle: { color: '#1890ff' } }],
      grid: { left: 40, right: 10, top: 10, bottom: 40 },
    })
  }

  // Trend chart
  const trendData = data.scanTrend || []
  if (charts[1]) {
    charts[1].setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: trendData.map((d: any) => d.date), axisLabel: { color: darkTextColor, fontSize: 10 }, axisLine: { lineStyle: { color: darkAxisColor } } },
      yAxis: { type: 'value', axisLabel: { color: darkTextColor }, splitLine: { lineStyle: { color: darkAxisColor } } },
      series: [{ type: 'line', data: trendData.map((d: any) => d.count), smooth: true, areaStyle: { color: 'rgba(24,144,255,0.2)' }, lineStyle: { color: '#1890ff' }, itemStyle: { color: '#1890ff' } }],
      grid: { left: 40, right: 10, top: 10, bottom: 30 },
    })
  }

  // City ranking
  const cityData = (data.cityRanking || []).slice(0, 10)
  if (charts[2]) {
    charts[2].setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'value', axisLabel: { color: darkTextColor }, splitLine: { lineStyle: { color: darkAxisColor } } },
      yAxis: { type: 'category', data: cityData.map((d: any) => d.city).reverse(), axisLabel: { color: darkTextColor, fontSize: 10 }, axisLine: { lineStyle: { color: darkAxisColor } } },
      series: [{ type: 'bar', data: cityData.map((d: any) => d.count).reverse(), itemStyle: { color: '#36cfc9' } }],
      grid: { left: 80, right: 10, top: 10, bottom: 10 },
    })
  }

  // Scan rate
  const scanRate = data.scanRate || {}
  const scanTotal = scanRate.total || 0
  const scanScanned = scanRate.scanned || 0
  const ratePercent = scanTotal > 0 ? Math.min(100, Math.round(scanScanned / scanTotal * 100)) : 0
  if (charts[3]) {
    charts[3].setOption({
      series: [{
        type: 'gauge',
        startAngle: 180, endAngle: 0,
        min: 0, max: 100,
        pointer: { show: false },
        progress: { show: true, width: 18, itemStyle: { color: '#1890ff' } },
        axisLine: { lineStyle: { width: 18, color: [[1, darkAxisColor]] } },
        axisTick: { show: false },
        splitLine: { show: false },
        axisLabel: { show: false },
        detail: { valueAnimation: true, fontSize: 24, color: '#fff', offsetCenter: [0, '10%'], formatter: '{value}%' },
        data: [{ value: ratePercent, name: '扫码率' }],
        title: { color: darkTextColor, fontSize: 12, offsetCenter: [0, '40%'] },
      }],
    })
  }

  // Label trend
  const labelData = data.labelDistribution || []
  if (charts[4]) {
    charts[4].setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: labelData.map((d: any) => d.month), axisLabel: { color: darkTextColor, fontSize: 10 }, axisLine: { lineStyle: { color: darkAxisColor } } },
      yAxis: { type: 'value', axisLabel: { color: darkTextColor }, splitLine: { lineStyle: { color: darkAxisColor } } },
      series: [{ type: 'line', data: labelData.map((d: any) => d.count), smooth: true, areaStyle: { color: 'rgba(54,207,201,0.2)' }, lineStyle: { color: '#36cfc9' }, itemStyle: { color: '#36cfc9' } }],
      grid: { left: 40, right: 10, top: 10, bottom: 30 },
    })
  }

  // Category pie
  const categoryData = data.productCategory || []
  if (charts[5]) {
    charts[5].setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie', radius: ['40%', '65%'],
        label: { color: darkTextColor, fontSize: 11 },
        data: categoryData.map((d: any) => ({ name: d.name, value: d.value })),
        itemStyle: { borderColor: '#0a1929', borderWidth: 2 },
      }],
    })
  }

  // Enterprise ranking
  enterpriseRanking.value = (data.enterpriseRanking || []).map((d: any, i: number) => ({
    rank: i + 1, name: d.enterpriseName, scanCount: d.scanCount,
  }))
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getAdminDataScreen()
    renderData(res.data)
  } catch {
    // request 拦截器已处理错误提示
  } finally {
    loading.value = false
  }
}

function handleResize() {
  charts.forEach(c => c.resize())
}

onMounted(async () => {
  updateTime()
  timer = setInterval(updateTime, 1000)
  await nextTick()
  initCharts()
  fetchData()
  refreshTimer = setInterval(fetchData, 60000)
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (refreshTimer) clearInterval(refreshTimer)
  charts.forEach(c => c.dispose())
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped lang="scss">
.data-screen {
  min-height: 100vh;
  background: #0a1929;
  color: #c0d8f0;
  padding: 16px 24px;
  overflow-y: auto;
}

.screen-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(126, 200, 227, 0.15);

  .header-center {
    text-align: center;
    h1 { font-size: 22px; color: #fff; letter-spacing: 4px; margin: 0; }
    p { font-size: 13px; color: #7ec8e3; margin: 4px 0 0; }
  }

  .header-right {
    .refresh-info { font-size: 12px; color: #4a7aa5; }
  }
}

.kpi-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin: 16px 0;
}

.kpi-card {
  background: linear-gradient(135deg, #0d2b4a 0%, #112e4e 100%);
  border: 1px solid rgba(126, 200, 227, 0.1);
  border-radius: 12px;
  padding: 20px;
  text-align: center;

  .kpi-value {
    font-size: 28px;
    font-weight: 700;
    color: #1890ff;
    font-family: 'DIN Alternate', monospace;
  }

  .kpi-label {
    font-size: 13px;
    color: #7ec8e3;
    margin-top: 4px;
  }
}

.chart-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.chart-card {
  background: linear-gradient(135deg, #0d2b4a 0%, #112e4e 100%);
  border: 1px solid rgba(126, 200, 227, 0.1);
  border-radius: 12px;
  padding: 16px;
}

.chart-title {
  font-size: 14px;
  font-weight: 600;
  color: #7ec8e3;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 3px solid #1890ff;
}

.chart-body {
  width: 100%;
  height: 260px;
}

.ranking-section {
  background: linear-gradient(135deg, #0d2b4a 0%, #112e4e 100%);
  border: 1px solid rgba(126, 200, 227, 0.1);
  border-radius: 12px;
  padding: 16px;
}
</style>
