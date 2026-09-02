<template>
  <div class="hgz-print-page">
    <component :is="'style'" type="text/css">{{ dynamicPrintCss }}</component>

    <div class="print-toolbar no-print">
      <el-button size="small" @click="$router.back()"><el-icon><Back /></el-icon> 返回</el-button>
      <el-radio-group v-model="preset" size="small">
        <el-radio-button value="a5">A5 证书版</el-radio-button>
        <el-radio-button value="100x70">100×70mm 标签</el-radio-button>
        <el-radio-button value="80x60">80×60mm 标签</el-radio-button>
        <el-radio-button value="50x30">50×30mm 标签</el-radio-button>
      </el-radio-group>
      <el-checkbox v-model="showQr" size="small">二维码</el-checkbox>
      <el-checkbox v-if="preset === 'a5'" v-model="showEnterprise" size="small">企业信息</el-checkbox>
      <el-checkbox v-if="preset === 'a5'" v-model="showBasisImages" size="small">检测报告链接</el-checkbox>
      <div class="toolbar-right">
        <el-tag v-if="cert && cert.status !== 1" type="danger" size="small">已作废</el-tag>
        <el-button size="small" type="primary" :loading="qrLoading" @click="handlePrint"><el-icon><Printer /></el-icon> 打印</el-button>
      </div>
    </div>

    <div v-loading="loading" class="print-stage">
      <!-- A5 证书版 -->
      <div v-if="preset === 'a5'" class="label label-a5">
        <div class="a5-header">
          <div class="a5-title">承诺达标合格证</div>
          <div class="a5-code">证号：{{ cert?.code }}</div>
        </div>
        <div class="a5-speech">我承诺{{ cert?.userType === 2 ? '销售' : '生产销售' }}的食用农产品，符合以下承诺事项：</div>
        <ul class="a5-promises">
          <li v-for="(item, i) in promiseChecked" :key="i"><span class="cb">✓</span>{{ item.title }}</li>
        </ul>
        <div class="a5-basis" v-if="basisChecked.length">
          <span class="a5-basis-label">承诺依据：</span>
          <template v-for="(item, i) in basisChecked" :key="i">
            <span class="cb">✓</span>{{ item.title }}<span v-if="i < basisChecked.length - 1">、</span>
          </template>
        </div>
        <div class="a5-details">
          <div class="a5-row"><span>产品名称</span><b>{{ cert?.productName }}</b></div>
          <div class="a5-row"><span>重量或数量</span><b>{{ cert?.number || '-' }}</b></div>
          <div class="a5-row"><span>产地</span><b>{{ cert?.placeOfOrigin || '-' }}</b></div>
          <div class="a5-row"><span>承诺主体</span><b>{{ cert?.promiseUser }}</b></div>
          <div class="a5-row"><span>联系方式</span><b>{{ cert?.contact || '-' }}</b></div>
          <div class="a5-row"><span>开具日期</span><b>{{ cert?.useTime || '-' }}</b></div>
        </div>
        <div class="a5-foot">
          <div class="a5-sign">
            <img v-if="cert?.signature" :src="cert.signature" class="a5-sign-img" />
            <span v-else class="a5-sign-ph">（承诺主体签名/盖章）</span>
            <div class="a5-sign-cap">承诺主体</div>
          </div>
          <div v-if="showQr" class="a5-qr">
            <img v-if="qrData" :src="qrData" class="a5-qr-img" />
            <div class="a5-qr-cap">扫码查看本合格证</div>
          </div>
        </div>
        <div v-if="showEnterprise && cert?.enterpriseName" class="a5-ent">
          <img v-if="cert?.enterpriseImage" :src="cert.enterpriseImage" class="a5-ent-img" />
          <span class="a5-ent-name">{{ cert.enterpriseName }}</span>
        </div>
        <div class="a5-note">本合格证由生产经营者依据《中华人民共和国农产品质量安全法》《中华人民共和国食品安全法》及《农产品质量安全承诺达标合格证管理办法》自行开具，对其真实性负责。</div>
      </div>

      <!-- 标签版 -->
      <div v-else class="label label-tag" :class="'label-' + preset">
        <div class="tag-head">
          <span class="tag-title">承诺达标合格证</span>
          <img v-if="showQr && qrData" :src="qrData" class="tag-qr" />
        </div>
        <div class="tag-rows">
          <div class="tag-row"><span>产品名称</span><b>{{ cert?.productName }}</b></div>
          <div class="tag-row" v-if="preset !== '50x30'"><span>重量/数量</span><b>{{ cert?.number || '-' }}</b></div>
          <div class="tag-row" v-if="preset !== '50x30'"><span>产地</span><b>{{ cert?.placeOfOrigin || '-' }}</b></div>
          <div class="tag-row" v-if="preset !== '50x30'"><span>承诺主体</span><b>{{ cert?.promiseUser }}</b></div>
          <div class="tag-row" v-if="preset === '100x70'"><span>联系方式</span><b>{{ cert?.contact || '-' }}</b></div>
          <div class="tag-row"><span>开具日期</span><b>{{ cert?.useTime || '-' }}</b></div>
        </div>
        <div class="tag-code">证号：{{ cert?.code }}</div>
        <div class="tag-note" v-if="preset !== '50x30'">承诺事项：{{ promiseChecked.map((x: any) => x.title).join('；') }}</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Back, Printer } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getHgzDetail, getHgzQrcode } from '@/api/enterprise'

const route = useRoute()
const loading = ref(true)
const qrLoading = ref(false)
const cert = ref<any>(null)
const qrData = ref('')
const preset = ref('a5')
const showQr = ref(true)
const showEnterprise = ref(true)
const showBasisImages = ref(true)

const promiseChecked = computed(() => (cert.value?.promiseItems || []).filter((x: any) => x.isSelect))
const basisChecked = computed(() => (cert.value?.basisItems || []).filter((x: any) => x.isSelect))

const dynamicPrintCss = computed(() => {
  const sizeMap: Record<string, string> = {
    a5: '148mm 210mm',
    '100x70': '100mm 70mm',
    '80x60': '80mm 60mm',
    '50x30': '50mm 30mm',
  }
  return `@media print {
    @page { size: ${sizeMap[preset.value] || '148mm 210mm'}; margin: 0; }
    body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
  }`
})

async function load() {
  loading.value = true
  try {
    const id = Number(route.params.id)
    const res = await getHgzDetail(id)
    cert.value = res.data
    qrLoading.value = true
    try {
      const qr = await getHgzQrcode(id)
      qrData.value = qr.data
    } finally {
      qrLoading.value = false
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '合格证信息加载失败')
  } finally {
    loading.value = false
  }
}

function handlePrint() {
  window.print()
}

onMounted(load)
</script>

<style scoped lang="scss">
.hgz-print-page {
  min-height: 100vh;
  background: #f0f2f5;
  padding: 16px;
  box-sizing: border-box;
}

.print-toolbar {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  background: #fff;
  border-radius: 8px;
  padding: 10px 16px;
  margin-bottom: 16px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);

  .toolbar-right {
    margin-left: auto;
    display: flex;
    align-items: center;
    gap: 10px;
  }
}

.print-stage {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.label {
  background: #fff;
  box-shadow: 0 2px 10px rgba(0,0,0,0.12);
  box-sizing: border-box;
  overflow: hidden;
}

/* ============ A5 证书版 ============ */
.label-a5 {
  width: 148mm;
  min-height: 210mm;
  padding: 12mm 10mm 8mm;
  display: flex;
  flex-direction: column;

  .a5-header {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
    border-bottom: 3px solid #1e7e3c;
    padding-bottom: 3mm;

    .a5-title {
      font-size: 26px;
      font-weight: 700;
      color: #1e7e3c;
      letter-spacing: 5px;
    }

    .a5-code { font-size: 11px; color: #909399; }
  }

  .a5-speech {
    font-size: 13px;
    color: #303133;
    line-height: 1.7;
    margin: 4mm 0 2mm;
  }

  .a5-promises {
    list-style: none;
    margin: 0 0 3mm;
    padding: 0;

    li {
      display: flex;
      align-items: center;
      gap: 2mm;
      font-size: 12px;
      line-height: 2;
      color: #303133;
    }
  }

  .a5-basis {
    font-size: 12px;
    color: #606266;
    line-height: 1.8;
    margin-bottom: 3mm;

    .a5-basis-label { font-weight: 600; }
  }

  .cb {
    width: 14px;
    height: 14px;
    border: 1px solid #1e7e3c;
    border-radius: 2px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 10px;
    color: #1e7e3c;
    flex-shrink: 0;
  }

  .a5-details {
    border: 1px solid #dcdfe6;
    border-radius: 2mm;
    overflow: hidden;

    .a5-row {
      display: flex;
      border-bottom: 1px solid #f0f0f0;

      &:last-child { border-bottom: none; }

      span {
        width: 28mm;
        flex-shrink: 0;
        background: #f7faf7;
        padding: 2.2mm 3mm;
        font-size: 11px;
        color: #606266;
        border-right: 1px solid #f0f0f0;
      }

      b {
        flex: 1;
        padding: 2.2mm 3mm;
        font-size: 12px;
        font-weight: 600;
        color: #303133;
        word-break: break-all;
      }
    }
  }

  .a5-foot {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin: 6mm 0 4mm;

    .a5-sign {
      text-align: center;

      .a5-sign-img { max-width: 40mm; max-height: 18mm; }

      .a5-sign-ph {
        display: inline-block;
        color: #c0c4cc;
        font-size: 11px;
        padding: 6mm 10mm;
        border: 1px dashed #dcdfe6;
        border-radius: 2mm;
      }

      .a5-sign-cap { font-size: 10px; color: #909399; margin-top: 1mm; }
    }

    .a5-qr {
      text-align: center;

      .a5-qr-img { width: 28mm; height: 28mm; }

      .a5-qr-cap { font-size: 10px; color: #909399; }
    }
  }

  .a5-ent {
    display: flex;
    align-items: center;
    gap: 3mm;
    background: #f7faf7;
    border: 1px solid #e3efe6;
    border-radius: 2mm;
    padding: 2.5mm 3mm;
    margin-bottom: 3mm;

    .a5-ent-img { width: 12mm; height: 12mm; object-fit: cover; border-radius: 1.5mm; }

    .a5-ent-name { font-size: 12px; font-weight: 600; color: #1e7e3c; }
  }

  .a5-note {
    margin-top: auto;
    font-size: 9px;
    color: #b0b3b8;
    line-height: 1.6;
    border-top: 1px dashed #e4e7ed;
    padding-top: 2mm;
  }
}

/* ============ 标签版 ============ */
.label-100x70 { width: 100mm; height: 70mm; padding: 3mm; }
.label-80x60 { width: 80mm; height: 60mm; padding: 2.5mm; }
.label-50x30 { width: 50mm; height: 30mm; padding: 1.5mm; }

.label-tag {
  display: flex;
  flex-direction: column;
  border: 1px solid #1e7e3c;
  border-radius: 1.5mm;

  .tag-head {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #1e7e3c;
    padding-bottom: 1mm;
    margin-bottom: 1.5mm;

    .tag-title {
      color: #1e7e3c;
      font-weight: 700;
    }

    .tag-qr { flex-shrink: 0; }
  }

  .tag-rows {
    flex: 1;

    .tag-row {
      display: flex;
      gap: 1.5mm;
      line-height: 1.6;

      span { color: #606266; flex-shrink: 0; }

      b { color: #303133; font-weight: 600; word-break: break-all; }
    }
  }

  .tag-code { color: #909399; }

  .tag-note {
    color: #606266;
    line-height: 1.5;
    overflow: hidden;
  }
}

.label-100x70 {
  .tag-title { font-size: 15px; }
  .tag-qr { width: 16mm; height: 16mm; }
  .tag-rows { font-size: 10.5px; }
  .tag-code { font-size: 9px; margin-top: 1mm; }
  .tag-note { font-size: 9px; margin-top: 1mm; }
}

.label-80x60 {
  .tag-title { font-size: 13px; }
  .tag-qr { width: 13mm; height: 13mm; }
  .tag-rows { font-size: 9.5px; }
  .tag-code { font-size: 8px; }
  .tag-note { font-size: 8px; }
}

.label-50x30 {
  .tag-title { font-size: 9px; }
  .tag-qr { width: 11mm; height: 11mm; }
  .tag-rows { font-size: 7.5px; }
  .tag-code { font-size: 6.5px; }
}

@media print {
  .hgz-print-page {
    background: #fff;
    padding: 0;
    min-height: 0;
  }

  .no-print {
    display: none !important;
  }

  .print-stage {
    padding: 0;
  }

  .label {
    box-shadow: none;
    margin: 0;
  }

  .label-a5 {
    width: auto;
    min-height: auto;
  }
}
</style>
