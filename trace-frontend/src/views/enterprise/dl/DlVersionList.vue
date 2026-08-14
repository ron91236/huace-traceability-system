<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="version-header">
          <div class="product-info">
            <el-button text @click="$router.push('/enterprise/dl/products')">
              <el-icon><ArrowLeft /></el-icon>返回
            </el-button>
            <span class="food-name">{{ route.query.foodName || firstVersion?.foodName || '商品' }}</span>
            <el-tag size="small">条码：{{ route.query.barcode || firstVersion?.barcode || '-' }}</el-tag>
          </div>
          <el-button type="primary" @click="handleCreate">
            <el-icon><Plus /></el-icon>新建
          </el-button>
        </div>
        <div class="table-toolbar" style="margin-top:12px">
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-input v-model="search.versionNo" placeholder="版本号" clearable style="width:160px" @keyup.enter="loadData" />
            <el-select v-model="search.status" placeholder="发布状态" clearable style="width:130px">
              <el-option label="草稿" value="draft" />
              <el-option label="已发布" value="published" />
              <el-option label="已下架" value="offline" />
            </el-select>
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期"
              value-format="YYYY-MM-DD" style="width:260px" />
            <el-button type="primary" @click="loadData">搜索</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="versionNo" label="版本号" width="140" />
        <el-table-column prop="foodName" label="食品名称" min-width="140" />
        <el-table-column prop="barcode" label="商品条码" width="140" />
        <el-table-column label="二维码" width="140" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showQr(row)">预览</el-button>
            <el-button size="small" type="primary" link @click="downloadQr(row)">下载</el-button>
          </template>
        </el-table-column>
        <el-table-column label="发布状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'published'" type="success" size="small">已发布</el-tag>
            <el-tag v-else-if="row.status === 'offline'" type="warning" size="small">已下架</el-tag>
            <el-tag v-else type="info" size="small">草稿</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="165">
          <template #default="{ row }">{{ row.publishedAt || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="165" />
        <el-table-column prop="updatedAt" label="修改时间" width="165" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'published'" size="small" type="primary" link @click="goEdit(row)">编辑</el-button>
            <el-button v-if="row.status !== 'published'" size="small" type="success" link @click="handlePublish(row)">发布</el-button>
            <el-button v-else size="small" type="warning" link @click="handleOffline(row)">下架</el-button>
            <el-popconfirm v-if="row.status !== 'published'" title="确认删除该版本?" @confirm="handleDelete(row)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 二维码预览 -->
    <el-dialog v-model="qrVisible" title="二维码预览" width="360px">
      <div class="qr-wrap">
        <img v-if="qrRow" :src="qrRow.qrCode" class="qr-img" />
        <div class="qr-url">{{ qrRow?.scanUrl }}</div>
        <el-button type="primary" size="small" @click="downloadQr(qrRow)">下载二维码</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getDlVersions, createDlVersion, deleteDlVersion, publishDlVersion, offlineDlVersion } from '@/api/digital-label'

const route = useRoute()
const router = useRouter()
const productId = Number(route.params.id)
const list = ref<any[]>([])
const loading = ref(false)
const search = reactive({ versionNo: '', status: '' })
const dateRange = ref<string[]>([])
const qrVisible = ref(false)
const qrRow = ref<any>(null)

const firstVersion = computed(() => list.value[0] || null)

async function loadData() {
  loading.value = true
  try {
    const res = await getDlVersions(productId, {
      versionNo: search.versionNo || undefined,
      status: search.status || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
    })
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleCreate() {
  const latest = list.value.find(v => v.status !== 'published') || list.value[0]
  let copyFromId: number | undefined
  if (latest) {
    try {
      await ElMessageBox.confirm(
        '是否自动填入最新一次保存的标签信息？',
        '新建标签版本',
        { confirmButtonText: '自动填入', cancelButtonText: '从空白创建', distinguishCancelAndClose: true }
      )
      copyFromId = latest.id
    } catch (action) {
      if (action === 'close') return
      // cancel = 从空白创建
    }
  }
  try {
    const res = await createDlVersion(productId, copyFromId)
    ElMessage.success('版本已创建')
    router.push(`/enterprise/dl/versions/${res.data.id}/edit`)
  } catch (e) {}
}

function goEdit(row: any) {
  router.push(`/enterprise/dl/versions/${row.id}/edit`)
}

async function handlePublish(row: any) {
  try {
    await ElMessageBox.confirm('发布后消费者扫码可见该标签内容，确认发布？', '发布确认', { type: 'warning' })
    await publishDlVersion(row.id)
    ElMessage.success('发布成功')
    loadData()
  } catch (e) {}
}

async function handleOffline(row: any) {
  try {
    await ElMessageBox.confirm('下架后消费者扫码将无法查看该标签，确认下架？', '下架确认', { type: 'warning' })
    await offlineDlVersion(row.id)
    ElMessage.success('已下架')
    loadData()
  } catch (e) {}
}

async function handleDelete(row: any) {
  await deleteDlVersion(row.id)
  ElMessage.success('删除成功')
  loadData()
}

function showQr(row: any) {
  qrRow.value = row
  qrVisible.value = true
}

function downloadQr(row: any) {
  if (!row?.qrCode) return
  const a = document.createElement('a')
  a.href = row.qrCode
  a.download = `digital-label-${row.versionNo}.png`
  a.click()
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.version-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  .product-info {
    display: flex;
    align-items: center;
    gap: 10px;
    .food-name { font-size: 16px; font-weight: 600; color: #1f2937; }
  }
}
.qr-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  .qr-img { width: 220px; height: 220px; }
  .qr-url { font-size: 12px; color: #6b7280; word-break: break-all; text-align: center; }
}
</style>
