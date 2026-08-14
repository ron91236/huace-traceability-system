<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="sync-tip">
            平台会自动为企业同步商品基础信息，企业也可手动触发同步。
          </div>
          <el-button type="primary" @click="syncVisible = true">手动同步</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column label="同步类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.syncType === 'auto' ? 'info' : 'primary'" size="small">
              {{ row.syncType === 'auto' ? '自动' : '手动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="同步结果" min-width="260">
          <template #default="{ row }">
            总条数 {{ row.totalCount }} · 相同 {{ row.sameCount }} · 新增 {{ row.newCount }} · 更新 {{ row.updateCount }}
          </template>
        </el-table-column>
        <el-table-column label="同步条件" width="150">
          <template #default="{ row }">{{ row.syncCondition || '全部商品' }}</template>
        </el-table-column>
        <el-table-column label="时间范围" width="100">
          <template #default="{ row }">{{ rangeText(row.timeRange) }}</template>
        </el-table-column>
        <el-table-column label="同步状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'success'" type="success" size="small">成功</el-tag>
            <el-tag v-else-if="row.status === 'failed'" type="danger" size="small">失败</el-tag>
            <el-tag v-else type="warning" size="small">进行中</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="同步时间" width="170" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <!-- 手动同步弹窗 -->
    <el-dialog v-model="syncVisible" title="手动同步" width="440px" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="时间范围">
          <el-select v-model="syncForm.timeRange" style="width:100%">
            <el-option label="近7天" value="7d" />
            <el-option label="近14天" value="14d" />
            <el-option label="近30天" value="30d" />
            <el-option label="近60天" value="60d" />
            <el-option label="全部" value="all" />
          </el-select>
        </el-form-item>
        <el-form-item label="同步条件">
          <el-input v-model="syncForm.condition" placeholder="商品名称或条码（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="syncVisible = false">取消</el-button>
        <el-button type="primary" :loading="syncing" @click="handleSync">开始同步</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDlSyncRecords, manualDlSync } from '@/api/digital-label'

const list = ref<any[]>([])
const loading = ref(false)
const syncing = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const syncVisible = ref(false)
const syncForm = reactive({ timeRange: '30d', condition: '' })

function rangeText(range: string) {
  const map: Record<string, string> = { '7d': '近7天', '14d': '近14天', '30d': '近30天', '60d': '近60天', all: '全部' }
  return map[range] || '全部'
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDlSyncRecords({ page: page.value, size: size.value })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function handleSync() {
  syncing.value = true
  try {
    const res = await manualDlSync(syncForm)
    const r = res.data || {}
    ElMessage.success(`同步完成：共 ${r.totalCount} 条，相同 ${r.sameCount} 条，新增 ${r.newCount} 条，更新 ${r.updateCount} 条`)
    syncVisible.value = false
    syncForm.condition = ''
    loadData()
  } finally {
    syncing.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.sync-tip { color: #6b7280; font-size: 13px; }
</style>
