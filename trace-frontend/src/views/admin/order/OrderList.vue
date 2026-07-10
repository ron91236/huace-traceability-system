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
            <el-button type="success" :loading="exporting" @click="handleExport"><el-icon><Download /></el-icon>导出</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe row-key="id">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding:12px 24px">
              <div v-if="row._expanding" style="text-align:center;color:#999">加载中...</div>
              <div v-else-if="row._expanded">
                <h4 style="margin:0 0 8px">商品明细</h4>
                <el-table :data="row._orderItems" border stripe size="small" style="margin-bottom:16px">
                  <el-table-column prop="batchName" label="批次" min-width="80" />
                  <el-table-column prop="goodsName" label="商品名称" min-width="100" />
                  <el-table-column prop="goodsSpec" label="规格" width="80" />
                  <el-table-column prop="goodsWeight" label="重量" width="80" />
                  <el-table-column prop="labelSpecName" label="标签规格" width="100" />
                  <el-table-column prop="price" label="单价(元)" width="80">
                    <template #default="{ row: ri }">{{ ri.price != null ? Number(ri.price).toFixed(4) : '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="quantity" label="数量" width="70" />
                  <el-table-column prop="totalPrice" label="总价(元)" width="90">
                    <template #default="{ row: ri }">{{ ri.totalPrice != null ? Number(ri.totalPrice).toFixed(2) : '-' }}</template>
                  </el-table-column>
                </el-table>
                <h4 style="margin:0 0 8px">条码信息</h4>
                <el-table :data="row._orderCodes" border stripe size="small">
                  <el-table-column prop="productName" label="产品名称" min-width="100" />
                  <el-table-column prop="productDescription" label="产品描述" min-width="100">
                    <template #default="{ row: oc }">{{ oc.productDescription || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="goodsName" label="商品名称" min-width="80">
                    <template #default="{ row: oc }">{{ oc.goodsName || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="labelSpecName" label="标签规格" width="100" />
                  <el-table-column prop="serialStart" label="开始码" width="100" />
                  <el-table-column prop="serialEnd" label="结束码" width="100" />
                  <el-table-column prop="quantity" label="数量" width="70" />
                  <el-table-column prop="wasteCount" label="作废" width="60" />
                  <el-table-column prop="bindCount" label="绑定数" width="70" />
                  <el-table-column prop="productionTime" label="生产时间" width="160">
                    <template #default="{ row: oc }">{{ oc.productionTime || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="traceTemplate" label="溯源模板" width="100" />
                </el-table>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="enterpriseName" label="所属企业" width="120" />
        <el-table-column prop="certName" label="关联证书" width="120" />
        <el-table-column prop="totalBarcodeCount" label="订购条码数" width="100">
          <template #default="{ row }">{{ row.totalBarcodeCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="allocatedBarcodeCount" label="分配条码数" width="100">
          <template #default="{ row }">{{ row.allocatedBarcodeCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="订单总价" width="100">
          <template #default="{ row }">{{ row.totalPrice != null ? Number(row.totalPrice).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,19) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="toggleExpand(row)">{{ row._expanded ? '收起' : '查看' }}</el-button>
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
import { Search, Download } from '@element-plus/icons-vue'
import { getAdminOrders, approveOrder, rejectOrder, exportOrders, getAdminOrderDetail } from '@/api/admin'
import { orderStatusMap } from '@/utils/constants'

const statusMap = orderStatusMap
const loading = ref(false)
const submitting = ref(false)
const exporting = ref(false)
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
    list.value = (res.data?.list || []).map((item: any) => ({ ...item, _expanded: false, _expanding: false, _orderItems: [], _orderCodes: [] }))
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function toggleExpand(row: any) {
  if (row._expanded) {
    row._expanded = false
    return
  }
  row._expanding = true
  try {
    const res = await getAdminOrderDetail(row.id)
    const d = res.data
    row._orderItems = d?.orderItems || []
    row._orderCodes = d?.orderCodes || []
    row._expanded = true
  } catch (e) {
    ElMessage.error('加载明细失败')
  } finally {
    row._expanding = false
  }
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

async function handleExport() {
  exporting.value = true
  try {
    const params: any = {}
    if (search.orderNo) params.keyword = search.orderNo
    if (search.status) params.status = search.status
    await exportOrders(params)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally { exporting.value = false }
}
</script>

<style scoped lang="scss">
.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
}
</style>
