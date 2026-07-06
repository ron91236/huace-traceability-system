<template>
  <div class="vehicle-track-element">
    <div class="track-header">
      <span class="track-icon">🚛</span>
      <span class="track-title">{{ label || '运输轨迹' }}</span>
      <span v-if="trackInfo.vehiclePlate" class="track-plate">{{ trackInfo.vehiclePlate }}</span>
    </div>
    <div v-if="!points.length" class="no-data">暂无轨迹数据</div>
    <template v-else>
      <div ref="mapRef" class="track-map"></div>
      <div class="track-info-bar">
        <div v-if="trackInfo.vehiclePlate" class="track-info-item">
          <span class="info-label">车牌</span>
          <span class="info-value">{{ trackInfo.vehiclePlate }}</span>
        </div>
        <div v-if="trackInfo.speed !== undefined" class="track-info-item">
          <span class="info-label">速度</span>
          <span class="info-value">{{ trackInfo.speed }} km/h</span>
        </div>
        <div v-if="trackInfo.temperature !== undefined" class="track-info-item">
          <span class="info-label">车厢温度</span>
          <span class="info-value" :class="{ 'temp-warn': trackInfo.temperature > -10 }">{{ trackInfo.temperature }}°C</span>
        </div>
        <div v-if="trackInfo.status" class="track-info-item">
          <span class="info-label">状态</span>
          <span class="info-value status-active">{{ trackInfo.status }}</span>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'

const props = withDefaults(defineProps<{
  label?: string
  points?: Array<{ lng: number; lat: number; time?: string; speed?: number; temperature?: number }>
  trackInfo?: { vehiclePlate?: string; speed?: number; temperature?: number; status?: string; mapKey?: string }
}>(), {
  points: () => [],
  trackInfo: () => ({}),
})

const mapRef = ref<HTMLElement | null>(null)
let map: any = null
let tencentMapLoaded = false

async function loadTencentMap(key: string): Promise<void> {
  if ((window as any).T?.Map) return
  if (tencentMapLoaded) return
  tencentMapLoaded = true
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://map.qq.com/api/js?v=2.exp&key=${key}`
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('腾讯地图加载失败'))
    document.head.appendChild(script)
  })
}

function renderMap() {
  if (!mapRef.value || !props.points?.length) return
  const T = (window as any).T
  if (!T?.Map) return

  const mapKey = props.trackInfo?.mapKey || 'YOUR_TENCENT_MAP_KEY'

  if (!map) {
    map = new T.Map(mapRef.value)
  }

  // Clear previous overlays
  try { map.clearOverLays?.() } catch {}

  const TLngLat = T.LngLat
  const path: any[] = []

  for (const p of props.points) {
    path.push(new TLngLat(p.lng, p.lat))
  }

  // Draw polyline
  if (path.length > 1) {
    const polyline = new T.Polyline(path, {
      color: '#059669',
      weight: 4,
      opacity: 0.85,
      lineJoin: 'round',
    })
    map.addOverLay(polyline)
  }

  // Start marker (green)
  if (path.length > 0) {
    const startMarker = new T.Marker(path[0])
    map.addOverLay(startMarker)
    const startLabel = new T.Label({ text: '起点', offset: new T.Point(10, -20) })
    startMarker.bindLabel(startLabel)
  }

  // End / current marker (red)
  if (path.length > 1) {
    const endMarker = new T.Marker(path[path.length - 1])
    map.addOverLay(endMarker)
    const endLabel = new T.Label({ text: '当前位置', offset: new T.Point(10, -20) })
    endMarker.bindLabel(endLabel)
  }

  // Fit bounds
  if (path.length > 1) {
    const bounds = new T.Bounds(
      new TLngLat(
        Math.min(...props.points.map(p => p.lng)),
        Math.min(...props.points.map(p => p.lat))
      ),
      new TLngLat(
        Math.max(...props.points.map(p => p.lng)),
        Math.max(...props.points.map(p => p.lat))
      )
    )
    map.setViewport(bounds, { padding: [30, 30, 30, 30] })
  } else if (path.length === 1) {
    map.centerAndZoom(path[0], 14)
  }
}

const points = ref(props.points || [])

onMounted(async () => {
  points.value = props.points || []
  if (!points.value.length) return
  const mapKey = props.trackInfo?.mapKey || 'YOUR_TENCENT_MAP_KEY'
  try {
    await loadTencentMap(mapKey)
    await nextTick()
    renderMap()
  } catch (e) {
    if (mapRef.value) {
      mapRef.value.innerHTML = '<div style="display:flex;align-items:center;justify-content:center;height:100%;color:#f56c6c;font-size:13px">地图加载失败</div>'
    }
  }
})

onUnmounted(() => {
  try { map?.destroy?.() } catch {}
  map = null
})

watch(() => props.points, async (newVal) => {
  points.value = newVal || []
  if (!points.value.length) return
  const mapKey = props.trackInfo?.mapKey || 'YOUR_TENCENT_MAP_KEY'
  if (!(window as any).T?.Map) {
    try {
      await loadTencentMap(mapKey)
    } catch { return }
  }
  await nextTick()
  renderMap()
}, { deep: true })
</script>

<style scoped>
.vehicle-track-element {
  background: var(--trace-section-bg, #fff);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.track-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--trace-border-color, #eee);
}
.track-icon { font-size: 18px; }
.track-title { font-size: 16px; font-weight: 600; color: #333; flex: 1; }
.track-plate {
  background: #1a73e8;
  color: #fff;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
  letter-spacing: 1px;
}
.no-data {
  text-align: center;
  color: #999;
  padding: 40px;
  font-size: 13px;
}
.track-map {
  width: 100%;
  height: 220px;
  border-radius: 8px;
  overflow: hidden;
  background: #f0f0f0;
}
.track-info-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  padding: 12px 0 0;
}
.track-info-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
}
.info-label {
  color: #999;
}
.info-value {
  font-weight: 600;
  color: #333;
}
.temp-warn {
  color: #e74c3c;
}
.status-active {
  color: #059669;
}
</style>
