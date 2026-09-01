<template>
  <div class="page-container">
    <div v-if="isAdmin" style="margin-bottom:12px">
      <el-select v-model="entFilter" placeholder="全部企业" clearable style="width:220px" @change="reloadAll">
        <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
      </el-select>
    </div>
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 扫码分析 -->
      <el-tab-pane label="扫码分析" name="scan">
        <div class="stat-row">
          <div class="stat-item"><div class="num">{{ stats.today || 0 }}</div><div class="lbl">今天</div></div>
          <div class="stat-item"><div class="num">{{ stats.yesterday || 0 }}</div><div class="lbl">昨天</div></div>
          <div class="stat-item"><div class="num">{{ stats.last7d || 0 }}</div><div class="lbl">近7天</div></div>
          <div class="stat-item"><div class="num">{{ stats.last30d || 0 }}</div><div class="lbl">近30天</div></div>
          <div class="stat-item"><div class="num">{{ stats.total || 0 }}</div><div class="lbl">全部</div></div>
        </div>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="商品名称/条码/版本号" clearable style="width:240px" />
        </div>
        <el-table :data="filteredList" v-loading="loading" border stripe row-key="versionId" @expand-change="onExpandChange">
          <el-table-column type="expand">
            <template #default="{ row }">
              <div class="expand-detail" v-loading="row._detailLoading">
                <el-table v-if="row._detail?.length" :data="row._detail" size="small" border>
                  <el-table-column type="index" label="序号" width="60" />
                  <el-table-column label="扫码地点">
                    <template #default="{ row: d }">
                      {{ [d.locationProvince, d.locationCity].filter(Boolean).join(' ') || '未知' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="scanTime" label="扫码时间" width="180" />
                </el-table>
                <div v-else-if="!row._detailLoading" style="padding:12px;color:#9ca3af">暂无扫码明细</div>
              </div>
            </template>
          </el-table-column>
          <el-table-column type="index" label="序号" width="60" />
          <el-table-column prop="versionNo" label="版本号" width="150" />
          <el-table-column prop="foodName" label="商品名称" min-width="160" />
          <el-table-column prop="barcode" label="商品条码" width="150" />
          <el-table-column prop="scanCount" label="扫码次数" width="100" align="center" />
        </el-table>
      </el-tab-pane>

      <!-- 地域分析 -->
      <el-tab-pane label="地域分析" name="geo">
        <div class="geo-wrap">
          <div ref="mapChartRef" class="geo-map"></div>
          <div class="geo-tables">
            <el-card>
              <template #header><span class="card-title">省份扫码次数 TOP10</span></template>
              <el-table :data="geoList.slice(0, 10)" size="small" border>
                <el-table-column type="index" label="排名" width="60" />
                <el-table-column prop="province" label="省份" />
                <el-table-column prop="cnt" label="扫码次数" width="100" align="center" />
              </el-table>
            </el-card>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from '@/utils/echarts'
import chinaGeoJson from '@/assets/china.geo.json'
import { getDlScanAnalysis, getDlScanDetail, getDlGeoAnalysis } from '@/api/digital-label'
import { useDlAdmin } from '@/composables/useDlAdmin'

const { isAdmin, entFilter, enterprises } = useDlAdmin()
const activeTab = ref('scan')
const loading = ref(false)
const keyword = ref('')
const stats = ref<any>({})
const list = ref<any[]>([])
const geoList = ref<any[]>([])
const mapChartRef = ref<HTMLElement>()
let mapChart: echarts.ECharts | null = null
let mapRegistered = false

const filteredList = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(r =>
    (r.foodName || '').toLowerCase().includes(kw) ||
    (r.barcode || '').toLowerCase().includes(kw) ||
    (r.versionNo || '').toLowerCase().includes(kw))
})

async function loadData() {
  loading.value = true
  try {
    const res = await getDlScanAnalysis(entFilter.value)
    stats.value = res.data?.stats || {}
    list.value = (res.data?.list || []).map((r: any) => ({ ...r, _detail: null, _detailLoading: false }))
    // 展开行时懒加载明细
    list.value.forEach(r => {
      Object.defineProperty(r, '_loaded', { value: false, writable: true, enumerable: false })
    })
  } finally {
    loading.value = false
  }
}

async function loadDetail(row: any) {
  if (row._loaded) return
  row._loaded = true
  row._detailLoading = true
  try {
    const res = await getDlScanDetail(row.versionId)
    row._detail = res.data || []
  } finally {
    row._detailLoading = false
  }
}

// 展开行触发加载
function onExpandChange(row: any, expandedRows: any[]) {
  if (expandedRows.includes(row)) loadDetail(row)
}

async function loadGeo() {
  try {
    const res = await getDlGeoAnalysis(entFilter.value)
    geoList.value = res.data || []
    await nextTick()
    renderMap()
  } catch (e) {}
}

async function renderMap() {
  if (!mapChartRef.value) return
  if (!mapRegistered) {
    try {
      echarts.registerMap('china', chinaGeoJson as any)
      mapRegistered = true
    } catch (e) {
      // 地图注册失败，退化为柱状图
      renderBarFallback()
      return
    }
  }
  if (!mapChart) mapChart = echarts.init(mapChartRef.value)
  const mapData = geoList.value
    .filter(g => g.province && g.province !== '未知')
    .map(g => ({ name: g.province, value: g.cnt }))
  mapChart.setOption({
    tooltip: { trigger: 'item' },
    visualMap: {
      min: 0, max: Math.max(1, ...mapData.map(d => d.value)),
      left: 10, bottom: 10,
      text: ['多', '少'],
      inRange: { color: ['#d1fae5', '#059669'] },
      calculable: true,
    },
    series: [{
      name: '扫码次数', type: 'map', map: 'china', roam: false,
      label: { show: false },
      data: mapData,
    }],
  })
}

function renderBarFallback() {
  if (!mapChartRef.value) return
  if (!mapChart) mapChart = echarts.init(mapChartRef.value)
  const top = geoList.value.slice(0, 10).slice().reverse()
  mapChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 80, right: 30, top: 20, bottom: 30 },
    xAxis: { type: 'value', minInterval: 1 },
    yAxis: { type: 'category', data: top.map(g => g.province) },
    series: [{ type: 'bar', data: top.map(g => g.cnt), itemStyle: { color: '#059669' }, barMaxWidth: 16 }],
  }, true)
}

watch(activeTab, tab => {
  if (tab === 'geo') loadGeo()
})

function reloadAll() {
  loadData()
  if (activeTab.value === 'geo') loadGeo()
}

onMounted(() => {
  loadData()
})

onBeforeUnmount(() => mapChart?.dispose())
</script>

<style scoped lang="scss">
.stat-row {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
  .stat-item {
    flex: 1;
    text-align: center;
    background: #f8faf9;
    border-radius: 8px;
    padding: 14px 0;
    .num { font-size: 24px; font-weight: 700; color: #059669; }
    .lbl { font-size: 12px; color: #9ca3af; margin-top: 4px; }
  }
}
.table-toolbar { margin-bottom: 12px; }
.expand-detail { padding: 10px 20px; }
.card-title { font-weight: 600; font-size: 14px; }
.geo-wrap {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
  .geo-map { height: 420px; background: #fafafa; border-radius: 8px; }
}
</style>
