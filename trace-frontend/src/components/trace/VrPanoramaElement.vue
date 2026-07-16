<template>
  <div class="vr-panorama-section">
    <div class="section-title">VR全景导览</div>
    <div v-if="loading" class="vr-loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载VR全景中...</span>
    </div>
    <div v-else-if="!scenes.length" class="vr-empty">暂无VR全景数据</div>
    <template v-else>
      <!-- 场景缩略图导航 -->
      <div class="scene-nav" v-if="scenes.length > 1">
        <div
          v-for="(scene, idx) in scenes"
          :key="scene.id"
          class="scene-thumb"
          :class="{ active: currentSceneIdx === idx }"
          @click="switchScene(idx)"
        >
          <img :src="scene.panoramaUrl" :alt="scene.name" />
          <span class="scene-label">{{ scene.name || `场景${idx + 1}` }}</span>
        </div>
      </div>
      <!-- 全景渲染容器 -->
      <div class="vr-viewer-wrap">
        <div :id="viewerId" class="vr-viewer"></div>
        <div class="scene-info" v-if="scenes[currentSceneIdx]">
          {{ scenes[currentSceneIdx].name || `场景${currentSceneIdx + 1}` }}
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { Loading } from '@element-plus/icons-vue'

declare global {
  interface Window { pannellum: any }
}

interface VrHotspot {
  id: number
  type: string
  targetSceneId?: number
  label?: string
  tooltip?: string
  hYaw: number
  vPitch: number
}

interface VrScene {
  id: number
  name: string
  panoramaUrl: string
  hfov: number
  vfov: number
  isDefault: number
  hotspots: VrHotspot[]
}

let pannellumLoaded = false
let pannellumLoading: Promise<void> | null = null

function loadPannellum(): Promise<void> {
  if (pannellumLoaded || window.pannellum) {
    pannellumLoaded = true
    return Promise.resolve()
  }
  if (pannellumLoading) return pannellumLoading
  pannellumLoading = new Promise<void>((resolve, reject) => {
    // Load CSS
    if (!document.querySelector('link[href*="pannellum"]')) {
      const link = document.createElement('link')
      link.rel = 'stylesheet'
      link.href = '/vendor/pannellum/pannellum.css'
      document.head.appendChild(link)
    }
    // Load JS
    const script = document.createElement('script')
    script.src = '/vendor/pannellum/pannellum.js'
    script.onload = () => { pannellumLoaded = true; resolve() }
    script.onerror = () => reject(new Error('Pannellum加载失败'))
    document.head.appendChild(script)
  })
  return pannellumLoading
}

const props = defineProps<{
  config?: any
  traceData?: any
}>()

const route = useRoute()
const loading = ref(true)
const scenes = ref<VrScene[]>([])
const currentSceneIdx = ref(0)
const viewerId = `vr-viewer-${Date.now()}`
let viewer: any = null

onMounted(async () => {
  await loadPannellum()
  await loadVrData()
})

onBeforeUnmount(() => {
  if (viewer) {
    try { viewer.destroy() } catch (e) { /* ignore */ }
    viewer = null
  }
})

async function loadVrData() {
  loading.value = true
  try {
    const serialNo = route.params.serialNo as string
    const batchId = route.params.batchId as string

    let url: string
    if (batchId) {
      url = `/api/trace/batch/${batchId}/vr`
    } else if (serialNo) {
      url = `/api/trace/${serialNo}/vr`
    } else {
      loading.value = false
      return
    }

    const res = await fetch(url)
    if (!res.ok) throw new Error('获取VR数据失败')
    const result = await res.json()
    const data = result.data || []
    scenes.value = data

    if (data.length > 0) {
      // 找到默认场景索引
      const defaultIdx = data.findIndex((s: VrScene) => s.isDefault === 1)
      currentSceneIdx.value = defaultIdx >= 0 ? defaultIdx : 0
      await nextTickRender()
      initViewer()
    }
  } catch (e) {
    console.error('VR数据加载失败:', e)
  } finally {
    loading.value = false
  }
}

function nextTickRender(): Promise<void> {
  return nextTick()
}

function initViewer() {
  if (scenes.value.length === 0) return

  const firstScene = scenes.value[currentSceneIdx.value]

  // 构建 Pannellum multi-scene 配置
  const pannellumScenes: Record<string, any> = {}
  scenes.value.forEach((scene, idx) => {
    const hotspots = (scene.hotspots || []).map(hs => {
      if (hs.type === 'scene' && hs.targetSceneId) {
        const targetIdx = scenes.value.findIndex(s => s.id === hs.targetSceneId)
        if (targetIdx >= 0) {
          return {
            pitch: Number(hs.vPitch) || 0,
            yaw: Number(hs.hYaw) || 0,
            type: 'scene',
            text: hs.label || '前往下一场景',
            sceneId: `scene_${scenes.value[targetIdx].id}`,
          }
        }
      }
      // info 热点
      return {
        pitch: Number(hs.vPitch) || 0,
        yaw: Number(hs.hYaw) || 0,
        type: 'info',
        text: hs.label || '',
        cssClass: 'vr-info-hotspot',
      }
    })

    pannellumScenes[`scene_${scene.id}`] = {
      title: scene.name || `场景${idx + 1}`,
      hfov: scene.hfov || 120,
      type: 'equirectangular',
      panorama: scene.panoramaUrl,
      hotspots,
    }
  })

  const container = document.getElementById(viewerId)
  if (!container) return

  // 如果已有 viewer 先销毁
  if (viewer) {
    try { viewer.destroy() } catch (e) { /* ignore */ }
  }

  try {
    const p = window.pannellum
    viewer = p.viewer(viewerId, {
      default: {
        firstScene: `scene_${firstScene.id}`,
        sceneFadeDuration: 800,
        autoLoad: true,
        compass: false,
        showFullscreenCtrl: true,
        showZoomCtrl: true,
        mouseZoom: true,
        hfov: firstScene.hfov || 120,
      },
      scenes: pannellumScenes,
    })

    // 监听场景切换
    viewer.on('scenechange', (sceneId: string) => {
      const id = parseInt(sceneId.replace('scene_', ''))
      const idx = scenes.value.findIndex(s => s.id === id)
      if (idx >= 0) currentSceneIdx.value = idx
    })
  } catch (e) {
    console.error('Pannellum初始化失败:', e)
  }
}

function switchScene(idx: number) {
  currentSceneIdx.value = idx
  const scene = scenes.value[idx]
  if (viewer && scene) {
    try {
      viewer.loadScene(`scene_${scene.id}`)
    } catch (e) {
      console.error('场景切换失败:', e)
    }
  }
}
</script>

<style scoped lang="scss">
.vr-panorama-section {
  margin: 12px 0;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid #409eff;
}

.vr-loading, .vr-empty {
  text-align: center;
  padding: 40px 0;
  color: #999;
  font-size: 14px;

  .el-icon { margin-right: 6px; }
}

.vr-viewer-wrap {
  position: relative;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0,0,0,0.15);
}

.vr-viewer {
  width: 100%;
  height: 400px;
  background: #1a1a2e;
}

.scene-info {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  pointer-events: none;
}

.scene-nav {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  overflow-x: auto;
  padding: 4px 0;

  .scene-thumb {
    flex-shrink: 0;
    width: 80px;
    cursor: pointer;
    border-radius: 6px;
    overflow: hidden;
    border: 2px solid transparent;
    transition: border-color 0.2s;
    position: relative;

    &.active { border-color: #409eff; }
    &:hover { border-color: #79bbff; }

    img {
      width: 100%;
      height: 50px;
      object-fit: cover;
      display: block;
    }

    .scene-label {
      display: block;
      font-size: 11px;
      text-align: center;
      padding: 2px 4px;
      background: rgba(0,0,0,0.7);
      color: #fff;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

@media (max-width: 768px) {
  .vr-viewer { height: 260px; }
  .scene-thumb { width: 64px;
    img { height: 40px; }
  }
}
</style>
