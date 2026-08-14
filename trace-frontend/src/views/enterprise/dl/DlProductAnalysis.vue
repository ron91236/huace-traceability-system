<template>
  <div class="page-container" v-loading="loading">
    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-item"><div class="num">{{ data.productCount || 0 }}</div><div class="lbl">商品总数</div></div>
      <div class="stat-item"><div class="num">{{ data.newYesterday || 0 }}</div><div class="lbl">昨天新增</div></div>
      <div class="stat-item"><div class="num">{{ data.new7d || 0 }}</div><div class="lbl">近7天新增</div></div>
      <div class="stat-item"><div class="num">{{ data.new14d || 0 }}</div><div class="lbl">近14天新增</div></div>
      <div class="stat-item"><div class="num">{{ data.new30d || 0 }}</div><div class="lbl">近30天新增</div></div>
    </div>

    <el-card class="mb-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-title">新增商品数量趋势</span>
          <el-radio-group v-model="days" size="small" @change="loadData">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="14">近14天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
            <el-radio-button :value="60">近60天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendChartRef" class="chart"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDlProductAnalysis } from '@/api/digital-label'

const loading = ref(false)
const days = ref(30)
const data = ref<any>({})
const trendChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null

async function loadData() {
  loading.value = true
  try {
    const res = await getDlProductAnalysis(days.value)
    data.value = res.data || {}
    await nextTick()
    renderChart()
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!trendChartRef.value) return
  if (!trendChart) trendChart = echarts.init(trendChartRef.value)
  const trend = data.value.trend || []
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    xAxis: { type: 'category', data: trend.map((t: any) => t.date.slice(5)) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ name: '新增商品数量', type: 'line', smooth: true, data: trend.map((t: any) => t.count), areaStyle: { opacity: 0.15 }, itemStyle: { color: '#059669' } }],
  }, true)
}

function handleResize() { trendChart?.resize() }

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped lang="scss">
.mb-card { margin-bottom: 16px; }
.card-title { font-weight: 600; font-size: 15px; }
.card-header-row { display: flex; align-items: center; justify-content: space-between; }
.stat-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  .stat-item {
    flex: 1;
    text-align: center;
    background: #fff;
    border-radius: 8px;
    padding: 18px 0;
    box-shadow: 0 1px 3px rgba(0,0,0,0.06);
    .num { font-size: 26px; font-weight: 700; color: #059669; }
    .lbl { font-size: 12px; color: #9ca3af; margin-top: 4px; }
  }
}
.chart { height: 320px; }
</style>
