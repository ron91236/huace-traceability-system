<template>
  <div class="dl-scan-page" :class="[themeClass, { 'big-font': bigFont }]">
    <!-- 顶部 -->
    <div class="scan-header">
      <div class="header-title">食品数字标签</div>
      <div class="header-slogan">扫一扫，食品信息一目了然</div>
      <div class="header-tools">
        <button class="tool-btn" :class="{ active: speaking }" @click="toggleSpeech">
          🔊 {{ speaking ? '停止播报' : '语音播报' }}
        </button>
        <button class="tool-btn" :class="{ active: bigFont }" @click="bigFont = !bigFont">
          字 {{ bigFont ? '标准' : '大号' }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="scan-loading">加载中...</div>

    <div v-else-if="error" class="scan-error">
      <div class="error-icon">😥</div>
      <div class="error-text">{{ error }}</div>
    </div>

    <template v-else>
      <!-- 食品数字身份证 -->
      <div class="scan-card id-card">
        <div class="id-label">食品数字身份证</div>
        <div class="id-barcode">条码编号：{{ data.version?.barcode }}</div>
      </div>

      <!-- 商品信息 -->
      <div class="scan-card">
        <div class="food-name">{{ data.version?.foodName }}</div>
        <div v-if="foodImages.length" class="food-images">
          <img v-for="(img, i) in foodImages" :key="i" :src="img" @click="previewImage(img)" />
        </div>
        <div class="info-row" v-if="data.version?.spec"><span class="info-label">规格</span><span>{{ data.version.spec }}</span></div>
        <div class="info-row" v-if="data.version?.netContent"><span class="info-label">净含量</span><span>{{ data.version.netContent }}</span></div>
      </div>

      <!-- 配料表 -->
      <div v-if="data.version?.ingredients" class="scan-card">
        <div class="card-title">配料表</div>
        <div class="ingredients" :class="{ collapsed: !ingredientsExpanded }">{{ data.version.ingredients }}</div>
        <div class="expand-link" @click="ingredientsExpanded = !ingredientsExpanded">
          {{ ingredientsExpanded ? '收起' : '展开' }}
        </div>
      </div>

      <!-- 标签详情 -->
      <div v-if="labelFields.length" class="scan-card">
        <div class="card-title">标签详情</div>
        <div v-for="f in labelFields" :key="f.label" class="detail-row">
          <div class="detail-label" @click="f.open = !f.open">
            {{ f.label }} <span class="arrow">{{ f.open ? '▲' : '▼' }}</span>
          </div>
          <div v-if="f.open" class="detail-value">{{ f.value }}</div>
        </div>
        <div v-for="(cf, i) in customFields" :key="'cf' + i" class="detail-row">
          <div class="detail-label" @click="cf.open = !cf.open">
            {{ cf.name }} <span class="arrow">{{ cf.open ? '▲' : '▼' }}</span>
          </div>
          <div v-if="cf.open" class="detail-value">{{ cf.value }}</div>
        </div>
      </div>

      <!-- 营养成分表 -->
      <div v-if="data.version?.nutritionImage" class="scan-card">
        <div class="card-title">营养成分表</div>
        <img class="nutrition-img" :src="data.version.nutritionImage" @click="previewImage(data.version.nutritionImage)" />
      </div>

      <!-- 生产信息 -->
      <div v-if="productionInfo.length" class="scan-card">
        <div class="card-title">生产信息</div>
        <div v-for="(p, i) in productionInfo" :key="i" class="production-item">
          <div class="production-type">{{ p.type }}</div>
          <div class="info-row" v-if="p.name"><span class="info-label">名称</span><span>{{ p.name }}</span></div>
          <div class="info-row" v-if="p.address"><span class="info-label">地址</span><span>{{ p.address }}</span></div>
          <div class="info-row" v-if="p.contacts?.length"><span class="info-label">联系方式</span><span>{{ p.contacts.join('、') }}</span></div>
        </div>
      </div>

      <!-- 扩展信息 -->
      <div v-if="data.version?.introVideo || certificates.length" class="scan-card">
        <div class="card-title">扩展信息</div>
        <video v-if="data.version.introVideo" :src="data.version.introVideo" controls class="intro-video"></video>
        <div v-if="certificates.length" class="cert-list">
          <div class="cert-label">资质证书</div>
          <img v-for="(c, i) in certificates" :key="i" :src="c" @click="previewImage(c)" />
        </div>
      </div>

      <div class="scan-footer">本页面信息由食品数字标签平台提供</div>
    </template>

    <!-- 图片预览 -->
    <div v-if="previewVisible" class="image-preview" @click="previewVisible = false">
      <img :src="previewUrl" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute } from 'vue-router'
import { getDlScanData } from '@/api/digital-label'

const route = useRoute()
const barcode = route.params.barcode as string

const loading = ref(true)
const error = ref('')
const data = ref<any>({})
const bigFont = ref(false)
const speaking = ref(false)
const ingredientsExpanded = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')
let utterance: SpeechSynthesisUtterance | null = null

// 按食品分类匹配模板颜色
const themeClass = computed(() => {
  const cat = data.value.version?.foodCategory || ''
  if (cat.startsWith('乳制品')) return 'theme-dairy'
  if (cat.startsWith('谷物豆类')) return 'theme-grain'
  if (cat.startsWith('肉制品')) return 'theme-meat'
  if (cat.startsWith('饮品')) return 'theme-drink'
  return 'theme-default'
})

const foodImages = computed(() => data.value.foodImages || [])
const certificates = computed(() => data.value.certificates || [])
const customFields = computed(() =>
  (Array.isArray(data.value.customFields) ? data.value.customFields : [])
    .map((f: any) => ({ ...f, open: false })))
const productionInfo = computed(() =>
  Array.isArray(data.value.productionInfo) ? data.value.productionInfo : [])

const labelFields = computed(() => {
  const v = data.value.version || {}
  const fields: { label: string; value: string; open: boolean }[] = []
  const map: [string, string][] = [
    ['食品分类', v.foodCategory], ['保质期', v.shelfLife],
    ['生产日期标示', v.productionDateLabel], ['保质期到期日标示', v.expiryDateLabel],
    ['食品生产许可证编号', v.licenseNo], ['产品标准代号', v.standardCode],
    ['质量等级', v.qualityGrade], ['贮存条件', v.storageCondition],
    ['转基因食品', v.gmoFood], ['辐照食品', v.irradiatedFood],
    ['定量标识', v.quantityLabel], ['批号标示', v.batchNoLabel],
    ['致敏物质', v.allergens], ['食用方法', v.consumptionMethod],
  ]
  map.forEach(([label, value]) => {
    if (value) fields.push({ label, value, open: false })
  })
  return fields
})

function toggleSpeech() {
  if (speaking.value) {
    window.speechSynthesis?.cancel()
    speaking.value = false
    return
  }
  const v = data.value.version || {}
  const text = [
    `${v.foodName}。`,
    v.spec ? `规格：${v.spec}。` : '',
    v.netContent ? `净含量：${v.netContent}。` : '',
    v.ingredients ? `配料表：${v.ingredients}。` : '',
    v.shelfLife ? `保质期：${v.shelfLife}。` : '',
    v.storageCondition ? `贮存条件：${v.storageCondition}。` : '',
  ].join('')
  if (!window.speechSynthesis || !text) return
  utterance = new SpeechSynthesisUtterance(text)
  utterance.lang = 'zh-CN'
  utterance.onend = () => { speaking.value = false }
  speaking.value = true
  window.speechSynthesis.speak(utterance)
}

function previewImage(url: string) {
  previewUrl.value = url
  previewVisible.value = true
}

onMounted(async () => {
  try {
    const res = await getDlScanData(barcode)
    data.value = res.data || {}
  } catch (e: any) {
    error.value = e?.response?.data?.msg || '未找到该商品的数字标签信息'
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.speechSynthesis?.cancel()
})
</script>

<style scoped lang="scss">
.dl-scan-page {
  --theme-color: #059669;
  --theme-bg: #ecfdf5;
  min-height: 100vh;
  background: var(--theme-bg);
  font-size: 14px;
  padding-bottom: 30px;

  &.theme-dairy { --theme-color: #16a34a; --theme-bg: #f0fdf4; }
  &.theme-grain { --theme-color: #b45309; --theme-bg: #fef9ec; }
  &.theme-meat { --theme-color: #ea580c; --theme-bg: #fff7ed; }
  &.theme-drink { --theme-color: #2563eb; --theme-bg: #eff6ff; }

  &.big-font {
    font-size: 17px;
    .food-name { font-size: 24px !important; }
    .card-title { font-size: 19px !important; }
  }
}

.scan-header {
  background: var(--theme-color);
  color: #fff;
  padding: 24px 16px 20px;
  text-align: center;
  .header-title { font-size: 20px; font-weight: 700; }
  .header-slogan { font-size: 13px; opacity: 0.85; margin-top: 6px; }
  .header-tools {
    display: flex;
    justify-content: center;
    gap: 12px;
    margin-top: 14px;
    .tool-btn {
      background: rgba(255,255,255,0.2);
      border: 1px solid rgba(255,255,255,0.4);
      color: #fff;
      border-radius: 20px;
      padding: 5px 16px;
      font-size: 13px;
      cursor: pointer;
      &.active { background: #fff; color: var(--theme-color); }
    }
  }
}

.scan-loading, .scan-error {
  text-align: center;
  padding: 60px 20px;
  color: #6b7280;
  .error-icon { font-size: 40px; margin-bottom: 12px; }
}

.scan-card {
  background: #fff;
  border-radius: 12px;
  margin: 12px 14px 0;
  padding: 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.05);
}

.id-card {
  border: 2px solid var(--theme-color);
  .id-label { color: var(--theme-color); font-weight: 700; font-size: 16px; }
  .id-barcode { margin-top: 6px; color: #374151; }
}

.food-name { font-size: 19px; font-weight: 700; color: #111827; margin-bottom: 10px; }
.food-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 10px;
  img { width: 90px; height: 90px; object-fit: cover; border-radius: 8px; }
}

.info-row {
  display: flex;
  gap: 12px;
  padding: 5px 0;
  color: #374151;
  .info-label { color: #9ca3af; min-width: 60px; flex-shrink: 0; }
}

.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--theme-color);
  margin-bottom: 10px;
  padding-left: 8px;
  border-left: 3px solid var(--theme-color);
}

.ingredients {
  color: #374151;
  line-height: 1.6;
  &.collapsed {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }
}
.expand-link { color: var(--theme-color); font-size: 13px; margin-top: 6px; cursor: pointer; }

.detail-row {
  border-bottom: 1px solid #f3f4f6;
  &:last-child { border-bottom: none; }
  .detail-label {
    padding: 10px 0;
    color: #374151;
    display: flex;
    justify-content: space-between;
    cursor: pointer;
    .arrow { color: #9ca3af; font-size: 11px; }
  }
  .detail-value { padding: 0 0 10px; color: #6b7280; line-height: 1.6; }
}

.nutrition-img { width: 100%; border-radius: 8px; }

.production-item {
  padding: 10px 0;
  border-bottom: 1px solid #f3f4f6;
  &:last-child { border-bottom: none; }
  .production-type { font-weight: 600; color: #111827; margin-bottom: 4px; }
}

.intro-video { width: 100%; border-radius: 8px; margin-bottom: 10px; }
.cert-list {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  .cert-label { width: 100%; color: #6b7280; font-size: 13px; }
  img { width: 90px; height: 90px; object-fit: cover; border-radius: 8px; }
}

.scan-footer { text-align: center; color: #9ca3af; font-size: 12px; margin-top: 24px; }

.image-preview {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
  img { max-width: 95%; max-height: 90vh; }
}
</style>
