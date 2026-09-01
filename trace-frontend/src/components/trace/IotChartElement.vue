<template>
  <div class="iot-chart-element">
    <div class="chart-header">
      <span class="chart-icon">📈</span>
      <span class="chart-title">{{ label || '温湿度曲线' }}</span>
      <div class="chart-range-btns">
        <button v-for="r in ranges" :key="r.value" :class="{ active: activeRange === r.value }" @click="switchRange(r.value)">{{ r.label }}</button>
      </div>
    </div>
    <div v-if="!series?.length" class="no-data">暂无历史数据</div>
    <div v-else ref="chartRef" class="chart-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from '@/utils/echarts'

const props = defineProps<{
  label?: string
  series?: Array<{ name: string; data: Array<{ time: string; value: number }> }>
  thresholdLine?: number
}>()

const ranges = [
  { label: '24h', value: '24h' },
  { label: '7天', value: '7d' },
  { label: '30天', value: '30d' },
]
const activeRange = ref('24h')
const chartRef = ref<HTMLElement | null>(null)
let chart: echarts.ECharts | null = null
let resizeObs: ResizeObserver | null = null

const COLORS = ['#e74c3c', '#3498db', '#27ae60', '#9b59b6', '#f39c12']

function switchRange(val: string) {
  activeRange.value = val
  // The parent should watch this and re-fetch data; here we just re-render with existing data
  renderChart()
}

function renderChart() {
  if (!chartRef.value || !props.series?.length) return
  if (!chart) {
    chart = echarts.init(chartRef.value, undefined, { renderer: 'canvas' })
  }

  const gridColors = COLORS.slice(0, props.series.length)

  const seriesOpts = props.series.map((s, i) => ({
    name: s.name,
    type: 'line' as const,
    smooth: true,
    symbol: 'circle',
    symbolSize: 4,
    sampling: 'lttb' as const,
    itemStyle: { color: gridColors[i] },
    lineStyle: { width: 2 },
    areaStyle: {
      color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: gridColors[i] + '40' },
        { offset: 1, color: gridColors[i] + '05' },
      ]),
    },
    data: s.data.map(d => [d.time, d.value]),
  }))

  const markLineData: any[] = []
  if (props.thresholdLine !== undefined) {
    markLineData.push({
      yAxis: props.thresholdLine,
      label: { formatter: '阈值 ' + props.thresholdLine, position: 'insideEndTop' },
      lineStyle: { color: '#e74c3c', type: 'dashed', width: 1.5 },
    })
  }
  if (markLineData.length && seriesOpts.length) {
    (seriesOpts[0] as any).markLine = { data: markLineData, silent: true }
  }

  chart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(255,255,255,0.96)',
      borderColor: '#eee',
      textStyle: { color: '#333', fontSize: 12 },
    },
    legend: {
      bottom: 0,
      textStyle: { fontSize: 11, color: '#666' },
      itemWidth: 12,
      itemHeight: 8,
    },
    grid: { top: 12, right: 12, bottom: 32, left: 46 },
    xAxis: {
      type: 'time',
      axisLabel: { fontSize: 10, color: '#999' },
      axisLine: { lineStyle: { color: '#e0e0e0' } },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      axisLabel: { fontSize: 10, color: '#999' },
      splitLine: { lineStyle: { color: '#f0f0f0' } },
    },
    series: seriesOpts,
  }, true)
}

function switchRange2(val: string) {
  activeRange.value = val
}

onMounted(() => {
  nextTick(() => {
    renderChart()
    if (chartRef.value) {
      resizeObs = new ResizeObserver(() => chart?.resize())
      resizeObs.observe(chartRef.value)
    }
  })
})

onUnmounted(() => {
  resizeObs?.disconnect()
  chart?.dispose()
  chart = null
})

watch(() => props.series, () => nextTick(renderChart), { deep: true })
</script>

<style scoped>
.iot-chart-element {
  background: var(--trace-section-bg, #fff);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.chart-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--trace-border-color, #eee);
}
.chart-icon { font-size: 18px; }
.chart-title { font-size: 16px; font-weight: 600; color: #333; flex: 1; }
.chart-range-btns {
  display: flex;
  gap: 4px;
}
.chart-range-btns button {
  padding: 3px 10px;
  font-size: 11px;
  border: 1px solid #ddd;
  border-radius: 12px;
  background: #fff;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}
.chart-range-btns button.active {
  background: var(--trace-accent, #059669);
  color: #fff;
  border-color: var(--trace-accent, #059669);
}
.no-data {
  text-align: center;
  color: #999;
  padding: 40px;
  font-size: 13px;
}
.chart-container {
  width: 100%;
  height: 240px;
}
</style>
