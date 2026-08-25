<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)"
    title="模板画廊" width="900px" top="5vh" destroy-on-close :close-on-click-modal="false">
    <div class="gallery-filter">
      <el-radio-group v-model="filterLayout" size="small">
        <el-radio-button label="">全部</el-radio-button>
        <el-radio-button v-for="p in LAYOUT_PRESETS" :key="p.key" :label="p.key">{{ p.label }}</el-radio-button>
      </el-radio-group>
    </div>
    <div class="gallery-grid">
      <div v-for="item in filteredItems" :key="item.key" class="gallery-card" @click="select(item)">
        <div class="gallery-thumb" :data-theme="item.themeKey || 'standard-green'">
          <div class="thumb-page">
            <div class="thumb-block" v-for="n in item.blocks" :key="n" :style="{ width: n % 3 === 0 ? '100%' : '48%' }"></div>
          </div>
        </div>
        <div class="gallery-info">
          <div class="gallery-title">{{ item.title }}</div>
          <div class="gallery-tags">
            <el-tag size="small" type="info">{{ getLayoutLabel(item.layout) }}</el-tag>
            <el-tag size="small" :type="item.themeKey === 'premium-gold' ? 'warning' : item.themeKey === 'tech-blue' ? 'primary' : 'success'">
              {{ getThemeLabel(item.themeKey) }}
            </el-tag>
          </div>
          <div class="gallery-desc">{{ item.description }}</div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { LAYOUT_PRESETS, getLayoutLabel, type LayoutType } from '@/constants/layout-presets'

interface GalleryItem {
  key: string
  title: string
  description: string
  layout: LayoutType
  themeKey: string
  blocks: number[]
}

const props = defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [val: boolean]; select: [item: GalleryItem] }>()

const filterLayout = ref('')

const themeLabels: Record<string, string> = {
  'standard-green': '标准绿',
  'tech-blue': '科技蓝',
  'premium-gold': '品质金',
}

const galleryItems: GalleryItem[] = [
  { key: 'pig-free', title: '生猪溯源（自由）', description: '传统纵向排列，适合完整展示养殖全流程', layout: 'free', themeKey: 'premium-gold', blocks: [1, 2, 3, 4, 5, 6] },
  { key: 'pig-one-screen', title: '生猪溯源（一屏到底）', description: '单页连贯滚动，用户体验流畅', layout: 'one-screen', themeKey: 'standard-green', blocks: [1, 2, 3, 4, 5, 6] },
  { key: 'pig-modular', title: '生猪溯源（卡片）', description: '两列卡片网格，信息模块独立展示', layout: 'modular-cards', themeKey: 'tech-blue', blocks: [3, 3, 3, 3, 3, 3] },
  { key: 'pig-split', title: '产品详情（左图右文）', description: '左侧主图，右侧关键信息，适合产品展示', layout: 'split-left-right', themeKey: 'standard-green', blocks: [1, 2, 2, 3] },
  { key: 'pig-tabs', title: '多维度（标签页）', description: '顶部标签切换，适合多维度信息溯源', layout: 'tabs', themeKey: 'tech-blue', blocks: [1, 2, 2, 3] },
  { key: 'pig-timeline', title: '全生命周期（时间轴）', description: '纵向时间线串联各阶段，直观展示流程', layout: 'timeline', themeKey: 'premium-gold', blocks: [3, 3, 3, 3, 3, 3] },
]

const filteredItems = computed(() => {
  if (!filterLayout.value) return galleryItems
  return galleryItems.filter(i => i.layout === filterLayout.value)
})

function getThemeLabel(key: string) {
  return themeLabels[key] || key
}

function select(item: GalleryItem) {
  emit('select', item)
  emit('update:modelValue', false)
}
</script>

<style scoped lang="scss">
.gallery-filter {
  margin-bottom: 16px;
  text-align: center;
}
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}
.gallery-card {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
  &:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.1); transform: translateY(-2px); }
}
.gallery-thumb {
  height: 120px;
  background: var(--trace-bg, #f5f5f5);
  padding: 12px;
  .thumb-page {
    height: 100%;
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    align-content: flex-start;
  }
  .thumb-block {
    height: 22px;
    border-radius: 4px;
    background: var(--trace-section-bg, #fff);
    box-shadow: var(--trace-section-shadow, 0 1px 4px rgba(0,0,0,0.06));
  }
}
.gallery-info {
  padding: 12px;
}
.gallery-title { font-size: 14px; font-weight: 600; margin-bottom: 6px; color: #333; }
.gallery-tags { display: flex; gap: 6px; margin-bottom: 6px; }
.gallery-desc { font-size: 12px; color: #999; line-height: 1.4; }
</style>
