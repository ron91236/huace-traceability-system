<template>
  <div class="cert-page">
    <div class="cert-header">
      <div class="header-logo">
        <span class="brand-text">CTI 华测检测</span>
      </div>
      <h2 class="header-title">证书信息</h2>
    </div>

    <div v-if="loading" class="cert-loading">
      <el-icon class="is-loading" :size="32" color="#059669"><Loading /></el-icon>
      <p>加载中...</p>
    </div>

    <div v-else-if="error" class="cert-error">
      <el-icon :size="48" color="#ef4444"><WarningFilled /></el-icon>
      <p>{{ error }}</p>
    </div>

    <div v-else class="cert-body">
      <!-- 证书状态 -->
      <div class="cert-status" :class="cert.isVoid ? 'void' : 'valid'">
        <el-icon v-if="!cert.isVoid"><SuccessFilled /></el-icon>
        <el-icon v-else><CircleCloseFilled /></el-icon>
        <span>{{ cert.isVoid ? '已作废' : '有效' }}</span>
      </div>

      <!-- 证书信息卡片 -->
      <div class="info-card">
        <div class="info-row">
          <span class="label">证书名称</span>
          <span class="value">{{ cert.certName }}</span>
        </div>
        <div class="info-row" v-if="cert.certTypeName">
          <span class="label">证书类型</span>
          <span class="value">{{ cert.certTypeName }}</span>
        </div>
        <div class="info-row" v-if="cert.enterpriseName">
          <span class="label">企业名称</span>
          <span class="value">{{ cert.enterpriseName }}</span>
        </div>
        <div class="info-row" v-if="cert.productName">
          <span class="label">产品名称</span>
          <span class="value">{{ cert.productName }}</span>
        </div>
        <div class="info-row" v-if="cert.startDate">
          <span class="label">有效期</span>
          <span class="value">{{ cert.startDate }} ~ {{ cert.endDate }}</span>
        </div>
      </div>

      <!-- 证书文件 -->
      <div class="cert-files">
        <template v-if="cert.certPdf">
          <h3 class="section-title">证书文件</h3>
          <div class="pdf-viewer-wrap">
            <iframe :src="cert.certPdf" class="pdf-iframe" />
          </div>
        </template>
        <template v-else-if="cert.certImage">
          <h3 class="section-title">证书图片</h3>
          <div class="image-gallery">
            <el-image
              v-for="(img, idx) in certImages"
              :key="idx"
              :src="img"
              fit="contain"
              class="gallery-img"
              :preview-src-list="certImages"
              :initial-index="idx"
            />
          </div>
        </template>
        <div v-else class="no-file">
          <p>暂无证书文件</p>
        </div>
      </div>

      <!-- 底部 -->
      <div class="cert-footer">
        <p>本信息由华测检测溯源系统提供</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getCertPublicInfo } from '@/api/common'
import { Loading, WarningFilled, SuccessFilled, CircleCloseFilled } from '@element-plus/icons-vue'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const cert = ref<any>({})

const certImages = computed(() => {
  if (!cert.value.certImage) return []
  return cert.value.certImage.split(',').filter(Boolean)
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (!id) {
    error.value = '无效的证书ID'
    loading.value = false
    return
  }
  try {
    const res = await getCertPublicInfo(id)
    cert.value = res.data || {}
  } catch (e: any) {
    error.value = '证书不存在或已失效'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped lang="scss">
.cert-page {
  min-height: 100vh;
  background: #f0fdf4;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

.cert-header {
  background: linear-gradient(135deg, #065f46, #059669);
  color: #fff;
  text-align: center;
  padding: 24px 16px 20px;

  .header-logo {
    margin-bottom: 8px;
    .brand-text {
      font-size: 16px;
      font-weight: 600;
      letter-spacing: 1px;
    }
  }

  .header-title {
    font-size: 20px;
    font-weight: 700;
    margin: 0;
  }
}

.cert-loading, .cert-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;

  p { color: #6b7280; font-size: 14px; }
}

.cert-body {
  padding: 16px;
  max-width: 600px;
  margin: 0 auto;
}

.cert-status {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  border-radius: 12px;
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 16px;

  &.valid {
    background: #ecfdf5;
    color: #059669;
    border: 1px solid #a7f3d0;
  }
  &.void {
    background: #fef2f2;
    color: #ef4444;
    border: 1px solid #fecaca;
  }
}

.info-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  margin-bottom: 16px;

  .info-row {
    display: flex;
    padding: 10px 0;
    border-bottom: 1px solid #f3f4f6;

    &:last-child { border-bottom: none; }

    .label {
      width: 80px;
      flex-shrink: 0;
      color: #6b7280;
      font-size: 14px;
    }

    .value {
      flex: 1;
      color: #1f2937;
      font-size: 14px;
      font-weight: 500;
    }
  }
}

.cert-files {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  margin-bottom: 16px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 12px;
}

.pdf-viewer-wrap {
  border-radius: 8px;
  overflow: hidden;

  .pdf-iframe {
    width: 100%;
    height: 70vh;
    min-height: 500px;
    border: none;
    border-radius: 8px;
  }
}

.image-gallery {
  display: flex;
  flex-direction: column;
  gap: 12px;

  .gallery-img {
    width: 100%;
    max-height: 80vh;
    border-radius: 8px;
  }
}

.no-file {
  text-align: center;
  padding: 40px 0;
  color: #9ca3af;
}

.cert-footer {
  text-align: center;
  padding: 20px;
  color: #9ca3af;
  font-size: 12px;
}
</style>
