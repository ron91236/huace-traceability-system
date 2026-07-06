<template>
  <div class="live-video-element">
    <div class="live-video-header">
      <span class="live-icon">📹</span>
      <span class="live-title">{{ label || '实时监控' }}</span>
      <span v-if="sources.length" class="live-badge">直播中</span>
    </div>
    <div v-if="!sources.length" class="no-video">暂无视频源</div>
    <div v-else class="live-video-grid" :class="'grid-' + (gridColumns || 1)">
      <div v-for="(vs, idx) in sources" :key="idx" class="live-video-item">
        <video
          :ref="(el) => setVideoRef(idx, el as HTMLVideoElement | null)"
          :poster="vs.coverImage"
          controls
          playsinline
          muted
          class="live-video-player"
        />
        <div class="video-label">{{ vs.cameraName }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  label?: string
  sources: Array<{ cameraName: string; streamUrl: string; streamType?: string; coverImage?: string }>
  gridColumns?: number
  autoPlay?: boolean
}>()

const videoRefs: Record<number, HTMLVideoElement | null> = {}
const hlsInstances: Record<number, any> = {}

function setVideoRef(idx: number, el: HTMLVideoElement | null) {
  videoRefs[idx] = el
}

async function initPlayer(idx: number, url: string) {
  const video = videoRefs[idx]
  if (!video || !url) return

  if (url.includes('.m3u8')) {
    // HLS stream
    try {
      const Hls = (await import('hls.js')).default
      if (Hls.isSupported()) {
        const hls = new Hls({ enableWorker: true, lowLatencyMode: true })
        hls.loadSource(url)
        hls.attachMedia(video)
        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          if (props.autoPlay) video.play().catch(() => {})
        })
        hlsInstances[idx] = hls
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        // Safari native HLS
        video.src = url
        if (props.autoPlay) video.play().catch(() => {})
      }
    } catch (e) {
      console.warn('HLS player init failed', e)
    }
  } else {
    // Direct video URL (MP4, etc.)
    video.src = url
    if (props.autoPlay) video.play().catch(() => {})
  }
}

onMounted(() => {
  props.sources?.forEach((vs, idx) => {
    setTimeout(() => initPlayer(idx, vs.streamUrl), idx * 500)
  })
})

onUnmounted(() => {
  Object.values(hlsInstances).forEach(hls => hls?.destroy?.())
})
</script>

<style scoped>
.live-video-element {
  background: var(--trace-section-bg, #fff);
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
}
.live-video-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--trace-border-color, #eee);
}
.live-icon { font-size: 18px; }
.live-title { font-size: 16px; font-weight: 600; color: #333; }
.live-badge {
  margin-left: auto;
  background: #e74c3c;
  color: #fff;
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.6; }
}
.no-video {
  text-align: center;
  color: #999;
  padding: 24px;
  font-size: 13px;
}
.live-video-grid {
  display: grid;
  gap: 8px;
}
.grid-1 { grid-template-columns: 1fr; }
.grid-2 { grid-template-columns: 1fr 1fr; }
.grid-3 { grid-template-columns: 1fr 1fr 1fr; }
.grid-4 { grid-template-columns: 1fr 1fr; }
.live-video-item {
  border-radius: 8px;
  overflow: hidden;
  background: #000;
}
.live-video-player {
  width: 100%;
  display: block;
  aspect-ratio: 16/9;
  object-fit: cover;
}
.video-label {
  padding: 6px 8px;
  font-size: 12px;
  color: #666;
  background: #f8f8f8;
  text-align: center;
}
</style>
