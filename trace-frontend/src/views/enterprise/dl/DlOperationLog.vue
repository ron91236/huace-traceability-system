<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-select v-if="isAdmin" v-model="entFilter" placeholder="全部企业" clearable style="width:180px" @change="loadData">
              <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
            </el-select>
            <el-input v-model="search.productName" placeholder="商品名称" clearable style="width:160px" @keyup.enter="loadData" />
            <el-select v-model="search.operationType" placeholder="操作类型" clearable style="width:130px">
              <el-option label="创建版本" value="创建版本" />
              <el-option label="更新版本" value="更新版本" />
              <el-option label="删除版本" value="删除版本" />
              <el-option label="发布" value="发布" />
              <el-option label="下架" value="下架" />
            </el-select>
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期"
              value-format="YYYY-MM-DD" style="width:260px" />
            <el-button type="primary" @click="loadData">搜索</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column v-if="isAdmin" label="所属企业" width="140">
          <template #default="{ row }">{{ row.enterpriseName || entName(row.enterpriseId) }}</template>
        </el-table-column>
        <el-table-column prop="productName" label="商品名称" min-width="150" />
        <el-table-column prop="versionCode" label="版本编码" width="150" />
        <el-table-column prop="operationType" label="操作类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="opTagType(row.operationType)">{{ row.operationType }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creator" label="创建人" width="120" />
        <el-table-column prop="createdAt" label="时间" width="170" />
        <el-table-column label="详情" width="90">
          <template #default="{ row }">
            <el-button v-if="row.beforeData || row.afterData" size="small" type="primary" link @click="showDetail(row)">详情</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <!-- 修改对比弹窗 -->
    <el-dialog v-model="detailVisible" title="操作详情" width="700px">
      <el-table :data="diffRows" border size="small" max-height="460">
        <el-table-column prop="field" label="字段" width="150" />
        <el-table-column prop="before" label="修改前">
          <template #default="{ row }">{{ formatVal(row.before) }}</template>
        </el-table-column>
        <el-table-column prop="after" label="修改后">
          <template #default="{ row }">
            <span :class="{ changed: formatVal(row.before) !== formatVal(row.after) }">{{ formatVal(row.after) }}</span>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { getDlOperationLogs } from '@/api/digital-label'
import { useDlAdmin } from '@/composables/useDlAdmin'

const { isAdmin, entFilter, enterprises, entName } = useDlAdmin()
const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const search = reactive({ productName: '', operationType: '' })
const dateRange = ref<string[]>([])
const detailVisible = ref(false)
const currentLog = ref<any>(null)

const FIELD_LABELS: Record<string, string> = {
  foodName: '食品名称', barcode: '商品条码', ingredients: '配料表', spec: '规格',
  netContent: '净含量', foodImages: '食品图片', nutritionImage: '营养成分表',
  foodCategory: '食品分类', shelfLife: '保质期', productionDateLabel: '生产日期标示',
  expiryDateLabel: '保质期到期日标示', licenseNo: '生产许可证编号', standardCode: '产品标准代号',
  qualityGrade: '质量等级', storageCondition: '贮存条件', gmoFood: '转基因食品',
  irradiatedFood: '辐照食品', quantityLabel: '定量标识', batchNoLabel: '批号标示',
  allergens: '致敏物质', consumptionMethod: '食用方法', introVideo: '介绍视频',
  certificates: '资质证书', customFields: '自定义字段', productionInfo: '生产信息',
  versionDesc: '版本描述', status: '状态',
}

const diffRows = computed(() => {
  const log = currentLog.value
  if (!log) return []
  const before = safeParse(log.beforeData)
  const after = safeParse(log.afterData)
  const keys = Array.from(new Set([...Object.keys(before || {}), ...Object.keys(after || {})]))
  return keys
    .filter(k => !['id', 'productId', 'versionNo', 'createdAt', 'updatedAt', 'publishedAt', 'qrCode', 'scanUrl', 'passwordHash'].includes(k))
    .map(k => ({ field: FIELD_LABELS[k] || k, before: before?.[k], after: after?.[k] }))
})

function safeParse(json: string) {
  if (!json) return null
  try { return JSON.parse(json) } catch (e) { return null }
}

function formatVal(v: any) {
  if (v === null || v === undefined || v === '') return '-'
  if (typeof v === 'object') return JSON.stringify(v)
  return String(v)
}

function opTagType(type: string) {
  if (type === '发布') return 'success'
  if (type === '下架') return 'warning'
  if (type === '删除版本') return 'danger'
  return 'info'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDlOperationLogs({
      page: page.value, size: size.value,
      productName: search.productName || undefined,
      operationType: search.operationType || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      enterpriseId: entFilter.value,
    })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function showDetail(row: any) {
  currentLog.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.changed { color: #e6a23c; font-weight: 600; }
</style>
