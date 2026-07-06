<template>
  <div class="iot-sensor-element">
    <div class="sensor-header">
      <span class="sensor-icon">🌡️</span>
      <span class="sensor-title">{{ label || '环境监测' }}</span>
      <span v-if="updatedAt" class="sensor-time">{{ updatedAt }}</span>
    </div>
    <div v-if="!readings.length" class="no-data">暂无传感器数据</div>
    <div v-else class="sensor-grid" :class="'cols-' + columns">
      <div v-for="r in readings" :key="r.key" class="sensor-card" :style="{ borderColor: r.color }">
        <div class="sensor-card-icon" :style="{ background: r.bgColor }">{{ r.icon }}</div>
        <div class="sensor-card-body">
          <div class="sensor-card-label">{{ r.label }}</div>
          <div class="sensor-card-value" :style="{ color: r.color }">
            {{ r.value }}<span class="sensor-card-unit">{{ r.unit }}</span>
          </div>
        </div>
        <div v-if="r.status" class="sensor-status" :class="'status-' + r.status">
          {{ r.status === 'normal' ? '正常' : r.status === 'warning' ? '偏高' : '异常' }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  label?: string
  data?: Record<string, number | null>
  updatedAt?: string
  columns?: number
}>()

const METRIC_CONFIG: Record<string, { icon: string; label: string; unit: string; color: string; min?: number; max?: number }> = {
  temperature:    { icon: '🌡️', label: '温度',     unit: '°C',  color: '#e74c3c', min: -10, max: 45 },
  humidity:       { icon: '💧', label: '湿度',     unit: '%',   color: '#3498db', min: 0, max: 100 },
  soilMoisture:   { icon: '🌱', label: '土壤湿度', unit: '%',   color: '#27ae60', min: 0, max: 100 },
  ph:             { icon: '⚗️', label: 'pH值',     unit: '',    color: '#9b59b6', min: 0, max: 14 },
  lightIntensity: { icon: '☀️', label: '光照强度', unit: 'lux', color: '#f39c12', min: 0, max: 100000 },
  co2:            { icon: '🌬️', label: 'CO₂',     unit: 'ppm', color: '#1abc9c', min: 0, max: 5000 },
  windSpeed:      { icon: '🌪️', label: '风速',    unit: 'm/s', color: '#2980b9', min: 0, max: 60 },
  rainfall:       { icon: '🌧️', label: '降雨量',  unit: 'mm',  color: '#34495e', min: 0, max: 300 },
}

function getStatus(key: string, val: number): string {
  const cfg = METRIC_CONFIG[key]
  if (!cfg) return 'normal'
  if (key === 'ph') {
    if (val < 5.5 || val > 8.5) return 'danger'
    if (val < 6.0 || val > 8.0) return 'warning'
    return 'normal'
  }
  if (key === 'temperature') {
    if (val > 40 || val < -5) return 'danger'
    if (val > 35 || val < 0) return 'warning'
    return 'normal'
  }
  return 'normal'
}

const readings = computed(() => {
  if (!props.data) return []
  return Object.entries(props.data)
    .filter(([, v]) => v !== null && v !== undefined)
    .map(([key, val]) => {
      const cfg = METRIC_CONFIG[key] || { icon: '📊', label: key, unit: '', color: '#607d8b' }
      return {
        key,
        icon: cfg.icon,
        label: cfg.label,
        value: typeof val === 'number' ? (Number.isInteger(val) ? val : val.toFixed(1)) : val,
        unit: cfg.unit,
        color: cfg.color,
        bgColor: cfg.color + '18',
        status: typeof val === 'number' ? getStatus(key, val) : null,
      }
    })
})

const columns = computed(() => props.columns || (readings.value.length <= 2 ? readings.value.length : readings.value.length <= 4 ? 2 : 3))
</script>

<style scoped>
.iot-sensor-element {
  background: var(--trace-section-bg, #fff);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.sensor-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--trace-border-color, #eee);
}
.sensor-icon { font-size: 18px; }
.sensor-title { font-size: 16px; font-weight: 600; color: #333; }
.sensor-time {
  margin-left: auto;
  font-size: 11px;
  color: #999;
}
.no-data {
  text-align: center;
  color: #999;
  padding: 24px;
  font-size: 13px;
}
.sensor-grid {
  display: grid;
  gap: 10px;
}
.cols-1 { grid-template-columns: 1fr; }
.cols-2 { grid-template-columns: 1fr 1fr; }
.cols-3 { grid-template-columns: 1fr 1fr 1fr; }
.cols-4 { grid-template-columns: 1fr 1fr; }
.sensor-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px;
  border-radius: 10px;
  background: #f8faf9;
  border-left: 3px solid #ccc;
  position: relative;
}
.sensor-card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}
.sensor-card-body { flex: 1; min-width: 0; }
.sensor-card-label {
  font-size: 12px;
  color: #888;
  margin-bottom: 2px;
}
.sensor-card-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}
.sensor-card-unit {
  font-size: 12px;
  font-weight: 400;
  color: #999;
  margin-left: 2px;
}
.sensor-status {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 8px;
  position: absolute;
  top: 6px;
  right: 8px;
}
.status-normal { background: #e8f5e9; color: #2e7d32; }
.status-warning { background: #fff3e0; color: #e65100; }
.status-danger { background: #fce4ec; color: #c62828; }
</style>
