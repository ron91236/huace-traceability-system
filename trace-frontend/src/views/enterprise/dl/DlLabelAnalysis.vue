<template>
  <div class="page-container" v-loading="loading">
    <!-- 管理员企业筛选 -->
    <div v-if="isAdmin" style="margin-bottom:12px">
      <el-select v-model="entFilter" placeholder="全部企业" clearable style="width:220px" @change="loadData">
        <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
      </el-select>
    </div>
    <!-- 统计卡片 -->
    <div class="stat-row">
      <div class="stat-item"><div class="num">{{ data.versionCount || 0 }}</div><div class="lbl">标签版本总数</div></div>
      <div class="stat-item"><div class="num">{{ data.publishedCount || 0 }}</div><div class="lbl">发布标签总数</div></div>
      <div class="stat-item"><div class="num">{{ data.newYesterday || 0 }}</div><div class="lbl">昨天新增</div></div>
      <div class="stat-item"><div class="num">{{ data.new7d || 0 }}</div><div class="lbl">近7天新增</div></div>
      <div class="stat-item"><div class="num">{{ data.new30d || 0 }}</div><div class="lbl">近30天新增</div></div>
    </div>

    <el-card class="mb-card">
      <template #header>
        <div class="card-header-row">
          <span class="card-title">新增标签趋势</span>
          <el-radio-group v-model="days" size="small" @change="loadData">
            <el-radio-button :value="7">近7天</el-radio-button>
            <el-radio-button :value="14">近14天</el-radio-button>
            <el-radio-button :value="30">近30天</el-radio-button>
          </el-radio-group>
        </div>
      </template>
      <div ref="trendChartRef" class="chart"></div>
    </el-card>

    <el-card class="mb-card">
      <template #header><span class="card-title">标签发布状态分布</span></template>
      <div ref="statusChartRef" class="chart"></div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from '@/utils/echarts'
import { getDlLabelAnalysis } from '@/api/digital-label'
import { useDlAdmin } from '@/composables/useDlAdmin'

const { isAdmin, entFilter, enterprises } = useDlAdmin()
const loading = ref(false)
const days = ref(30)
const data = ref<any>({})
const trendChartRef = ref<HTMLElement>()
const statusChartRef = ref<HTMLElement>()
let trendChart: echarts.ECharts | null = null
let statusChart: echarts.ECharts | null = null

async function loadData() {
  loading.value = true
  try {
    const res = await getDlLabelAnalysis(days.value, entFilter.value)
    data.value = res.data || {}
    await nextTick()
    renderCharts()
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  if (trendChartRef.value) {
    if (!trendChart) trendChart = echarts.init(trendChartRef.value)
    const trend = data.value.trend || []
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      grid: { left: 40, right: 20, top: 30, bottom: 30 },
      xAxis: { type: 'category', data: trend.map((t: any) => t.date.slice(5)) },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ name: '新增标签数量', type: 'line', smooth: true, data: trend.map((t: any) => t.count), areaStyle: { opacity: 0.15 }, itemStyle: { color: '#059669' } }],
    }, true)
  }
  if (statusChartRef.value) {
    if (!statusChart) statusChart = echarts.init(statusChartRef.value)
    const dist = data.value.statusDistribution || {}
    statusChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie', radius: ['40%', '65%'],
        data: [
          { name: '草稿', value: dist.draft || 0, itemStyle: { color: '#909399' } },
          { name: '已发布', value: dist.published || 0, itemStyle: { color: '#67c23a' } },
          { name: '已下架', value: dist.offline || 0, itemStyle: { color: '#e6a23c' } },
        ],
      }],
    }, true)
  }
}

function handleResize() { trendChart?.resize(); statusChart?.resize() }

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose(); statusChart?.dispose()
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
.chart { height: 300px; }
</style>
