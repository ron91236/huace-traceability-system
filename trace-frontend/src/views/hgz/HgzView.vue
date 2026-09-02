<template>
  <div class="hgz-public-page">
    <div v-if="loading" class="state-wrap"><el-icon class="is-loading" :size="30"><Loading /></el-icon></div>

    <div v-else-if="error" class="state-wrap error-state">
      <el-icon :size="44" color="#f56c6c"><CircleClose /></el-icon>
      <p>{{ error }}</p>
      <el-button @click="load">重新加载</el-button>
    </div>

    <template v-else-if="cert">
      <div class="cert-toolbar no-print">
        <el-button size="small" @click="handlePrint"><el-icon><Printer /></el-icon> 打印合格证</el-button>
        <el-button v-if="cert.queryUrl" size="small" type="primary" @click="goTrace"><el-icon><Search /></el-icon> 溯源查询</el-button>
      </div>

      <!-- 作废标识 -->
      <div v-if="cert.status !== 1" class="void-banner">
        <el-icon :size="30"><WarningFilled /></el-icon>
        <span>该承诺达标合格证已作废，仅供参考，不作有效凭证使用</span>
      </div>

      <div class="hgz-cert-card" :class="{ 'is-void': cert.status !== 1 }">
        <div class="cert-header">
          <div class="cert-title">承诺达标合格证</div>
          <div class="cert-code">证号：{{ cert.code }}</div>
        </div>

        <div class="promise-speech">
          我承诺{{ cert.userType === 2 ? '销售' : '生产销售' }}的食用农产品，符合以下承诺事项：
        </div>

        <div class="cert-section">
          <div class="section-label">承诺事项</div>
          <ul class="promise-list">
            <li v-for="(item, i) in promiseChecked" :key="i">
              <span class="check-box">✓</span>{{ item.title }}
            </li>
          </ul>
        </div>

        <div class="cert-section">
          <div class="section-label">承诺依据</div>
          <ul class="basis-list">
            <li v-for="(item, i) in basisChecked" :key="i" class="basis-li">
              <span class="check-box">✓</span>{{ item.title }}
              <el-link v-if="item.image" type="primary" :href="item.image" target="_blank" class="basis-img-link">查看检测报告</el-link>
            </li>
          </ul>
        </div>

        <div class="cert-details">
          <div class="detail-row"><span class="d-label">产品名称</span><span class="d-value">{{ cert.productName }}</span></div>
          <div class="detail-row"><span class="d-label">重量或数量</span><span class="d-value">{{ cert.number || '-' }}</span></div>
          <div class="detail-row"><span class="d-label">产地</span><span class="d-value">{{ cert.placeOfOrigin || '-' }}</span></div>
          <div class="detail-row"><span class="d-label">承诺主体</span><span class="d-value">{{ cert.promiseUser }}</span></div>
          <div class="detail-row"><span class="d-label">联系方式</span><span class="d-value">{{ cert.contact || '-' }}</span></div>
          <div class="detail-row"><span class="d-label">开具日期</span><span class="d-value">{{ cert.useTime || '-' }}</span></div>
        </div>

        <div class="cert-footer-row">
          <div class="sign-area">
            <img v-if="cert.signature" :src="cert.signature" class="sign-img" alt="签名/盖章" />
            <span v-else class="sign-placeholder">（承诺主体签名/盖章）</span>
            <div class="sign-caption">承诺主体</div>
          </div>
          <div class="qr-area">
            <img :src="qrDataUrl" class="qr-img" alt="合格证二维码" />
            <div class="qr-caption">扫码查看本合格证</div>
          </div>
        </div>

        <div v-if="cert.isShowEnterprise === 1 && cert.enterpriseName" class="enterprise-strip">
          <img v-if="cert.enterpriseImage" :src="cert.enterpriseImage" class="ent-img" alt="企业形象" />
          <div class="ent-info">
            <div class="ent-name">{{ cert.enterpriseName }}</div>
            <div v-if="cert.enterpriseIntroduction" class="ent-intro">{{ cert.enterpriseIntroduction }}</div>
          </div>
        </div>

        <div class="cert-footnote">
          本合格证由生产经营者依据《中华人民共和国农产品质量安全法》《中华人民共和国食品安全法》及《农产品质量安全承诺达标合格证管理办法》自行开具，生产经营者对其真实性负责。
        </div>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Loading, CircleClose, Printer, Search, WarningFilled } from '@element-plus/icons-vue'
import QRCode from 'qrcode'
import { getHgzPublic } from '@/api/common'

const route = useRoute()
const loading = ref(true)
const error = ref('')
const cert = ref<any>(null)
const qrDataUrl = ref('')

const promiseChecked = computed(() => (cert.value?.promiseList || []).filter((x: any) => x.isSelect))
const basisChecked = computed(() => (cert.value?.basisList || []).filter((x: any) => x.isSelect))

async function load() {
  loading.value = true
  error.value = ''
  try {
    const code = route.params.code as string
    const res = await getHgzPublic(code)
    cert.value = res.data
    const target = cert.value?.qrUrl || window.location.href
    qrDataUrl.value = await QRCode.toDataURL(target, { width: 260, margin: 1 })
  } catch (e: any) {
    error.value = e?.response?.data?.msg || '合格证信息加载失败'
  } finally {
    loading.value = false
  }
}

function goTrace() {
  if (cert.value?.queryUrl) window.location.href = cert.value.queryUrl
}

function handlePrint() {
  window.print()
}

onMounted(load)
</script>

<style scoped lang="scss">
.hgz-public-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 20px 12px 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
  box-sizing: border-box;
}

.state-wrap {
  padding: 80px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
  color: #606266;
}

.cert-toolbar {
  width: 100%;
  max-width: 720px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-bottom: 12px;
}

.void-banner {
  width: 100%;
  max-width: 720px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  background: #fef0f0;
  border: 1px solid #fbc4c4;
  color: #c45656;
  font-size: 14px;
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 12px;
}

.hgz-cert-card {
  width: 100%;
  max-width: 720px;
  box-sizing: border-box;
  background: #fff;
  border: 2px solid #1e7e3c;
  border-radius: 10px;
  padding: 28px 32px 20px;
  position: relative;

  &.is-void {
    opacity: 0.72;
    filter: grayscale(0.5);
  }

  .cert-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    border-bottom: 2px solid #1e7e3c;
    padding-bottom: 10px;
    margin-bottom: 16px;

    .cert-title {
      font-size: 26px;
      font-weight: 700;
      color: #1e7e3c;
      letter-spacing: 4px;
    }

    .cert-code {
      font-size: 12px;
      color: #909399;
    }
  }

  .promise-speech {
    font-size: 15px;
    color: #303133;
    line-height: 1.7;
    margin-bottom: 14px;
  }

  .cert-section {
    margin-bottom: 12px;

    .section-label {
      font-size: 13px;
      color: #909399;
      margin-bottom: 6px;
    }

    .promise-list, .basis-list {
      list-style: none;
      margin: 0;
      padding: 0;

      li {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: #303133;
        line-height: 2;
      }

      .basis-li {
        flex-wrap: wrap;

        .basis-img-link {
          font-size: 12px;
        }
      }

      .check-box {
        width: 16px;
        height: 16px;
        border: 1px solid #1e7e3c;
        border-radius: 3px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
        color: #1e7e3c;
        flex-shrink: 0;
      }
    }
  }

  .cert-details {
    border: 1px solid #dcdfe6;
    border-radius: 6px;
    overflow: hidden;
    margin: 14px 0;

    .detail-row {
      display: flex;
      border-bottom: 1px solid #f0f0f0;

      &:last-child { border-bottom: none; }

      .d-label {
        width: 110px;
        flex-shrink: 0;
        background: #f7faf7;
        padding: 8px 12px;
        font-size: 13px;
        color: #606266;
        border-right: 1px solid #f0f0f0;
      }

      .d-value {
        flex: 1;
        padding: 8px 12px;
        font-size: 14px;
        color: #303133;
        word-break: break-all;
      }
    }
  }

  .cert-footer-row {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin: 18px 0 12px;

    .sign-area {
      text-align: center;

      .sign-img {
        max-width: 150px;
        max-height: 70px;
      }

      .sign-placeholder {
        display: inline-block;
        color: #c0c4cc;
        font-size: 13px;
        padding: 20px 30px;
        border: 1px dashed #dcdfe6;
        border-radius: 6px;
      }

      .sign-caption {
        font-size: 12px;
        color: #909399;
        margin-top: 6px;
      }
    }

    .qr-area {
      text-align: center;

      .qr-img {
        width: 110px;
        height: 110px;
      }

      .qr-caption {
        font-size: 12px;
        color: #909399;
        margin-top: 4px;
      }
    }
  }

  .enterprise-strip {
    display: flex;
    gap: 12px;
    align-items: center;
    background: #f7faf7;
    border: 1px solid #e3efe6;
    border-radius: 8px;
    padding: 10px 14px;
    margin-bottom: 12px;

    .ent-img {
      width: 52px;
      height: 52px;
      object-fit: cover;
      border-radius: 6px;
      flex-shrink: 0;
    }

    .ent-info {
      .ent-name {
        font-size: 14px;
        font-weight: 600;
        color: #1e7e3c;
      }

      .ent-intro {
        font-size: 12px;
        color: #909399;
        margin-top: 3px;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }
    }
  }

  .cert-footnote {
    font-size: 11px;
    color: #b0b3b8;
    line-height: 1.6;
    border-top: 1px dashed #e4e7ed;
    padding-top: 10px;
  }
}

@media print {
  .hgz-public-page {
    background: #fff;
    padding: 0;
  }

  .no-print, .void-banner, .state-wrap {
    display: none !important;
  }

  .hgz-cert-card {
    border: none;
    border-radius: 0;
    max-width: 100%;
    padding: 10px 0;
  }

  @page {
    size: A5 portrait;
    margin: 8mm;
  }
}
</style>
