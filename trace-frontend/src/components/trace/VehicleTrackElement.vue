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
let amapLoaded = false

async function loadAmap(key: string): Promise<void> {
  if ((window as any).AMap) return
  if (amapLoaded) return
  amapLoaded = true
  return new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${key}`
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('高德地图加载失败'))
    document.head.appendChild(script)
  })
}

function renderMap() {
  if (!mapRef.value || !props.points?.length) return
  const AMap = (window as any).AMap
  if (!AMap) return

  if (!map) {
    map = new AMap.Map(mapRef.value, { zoom: 14 })
  }

  // 清除已有覆盖物
  try { map.clearMap?.() } catch {}

  const path = props.points.map(p => [p.lng, p.lat])

  // 轨迹线
  if (path.length > 1) {
    const polyline = new AMap.Polyline({
      path,
      strokeColor: '#059669',
      strokeWeight: 4,
      strokeOpacity: 0.85,
      lineJoin: 'round',
    })
    map.add(polyline)
  }

  // 起点标记
  if (path.length > 0) {
    map.add(new AMap.Marker({
      position: path[0],
      label: { content: '起点', offset: new AMap.Pixel(10, -20) },
    }))
  }

  // 终点/当前位置标记
  if (path.length > 1) {
    map.add(new AMap.Marker({
      position: path[path.length - 1],
      label: { content: '当前位置', offset: new AMap.Pixel(10, -20) },
    }))
  }

  // 自适应视野
  if (path.length > 0) {
    map.setFitView()
  }
}

const points = ref(props.points || [])

onMounted(async () => {
  points.value = props.points || []
  if (!points.value.length) return
  const mapKey = props.trackInfo?.mapKey || ''
  if (!mapKey) return
  try {
    await loadAmap(mapKey)
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
  const mapKey = props.trackInfo?.mapKey || ''
  if (!mapKey) return
  if (!(window as any).AMap) {
    try {
      await loadAmap(mapKey)
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
