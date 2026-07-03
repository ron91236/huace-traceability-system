<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-input v-model="search.orderNo" placeholder="订单编号" clearable style="width:140px" />
            <el-select v-model="search.status" placeholder="状态" clearable style="width:120px">
              <el-option label="待审核" value="PENDING" /><el-option label="已通过" value="APPROVED" /><el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>搜索</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单编号" width="160" />
        <el-table-column prop="enterpriseName" label="所属企业" width="140" />
        <el-table-column prop="certName" label="关联证书" width="140" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="170">
          <template #default="{ row }">{{ row.submitTime ? row.submitTime.replace('T',' ').substring(0,19) : '' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,19) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="$router.push(`/admin/order/${row.id}`)">查看</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger" link @click="openReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="rejectVisible" title="驳回原因" width="400px">
      <el-input v-model="rejectNote" type="textarea" :rows="3" placeholder="请输入驳回原因" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getAdminOrders, approveOrder, rejectOrder } from '@/api/admin'
import { orderStatusMap } from '@/utils/constants'

const statusMap = orderStatusMap
const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const search = reactive({ orderNo: '', status: '' })
const rejectVisible = ref(false)
const rejectNote = ref('')
const rejectId = ref(0)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminOrders({ page: page.value, size: size.value, ...search })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function handleApprove(row: any) {
  submitting.value = true
  try {
    await approveOrder(row.id)
    ElMessage.success('审核通过')
    loadData()
  } finally { submitting.value = false }
}

function openReject(row: any) {
  rejectId.value = row.id
  rejectNote.value = ''
  rejectVisible.value = true
}

async function handleReject() {
  if (!rejectNote.value.trim()) return ElMessage.warning('请输入驳回原因')
  submitting.value = true
  try {
    await rejectOrder(rejectId.value, { reviewNote: rejectNote.value })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadData()
  } finally { submitting.value = false }
}
</script>

<style scoped lang="scss">
.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
