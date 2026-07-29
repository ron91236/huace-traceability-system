<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-select v-model="search.enterpriseId" placeholder="所属企业" clearable filterable style="width:160px">
              <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
            </el-select>
            <el-input v-model="search.orderNo" placeholder="订单号" clearable style="width:160px" />
            <el-input v-model="search.certNo" placeholder="证书编号" clearable style="width:140px" />
            <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe size="small" row-key="id" ref="tableRef">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding:12px 24px">
              <el-descriptions :column="3" border size="small">
                <el-descriptions-item label="生产时间">{{ row.productionTime || '-' }}</el-descriptions-item>
                <el-descriptions-item label="溯源码模板">{{ row.traceTemplate || '-' }}</el-descriptions-item>
                <el-descriptions-item label="包装规格">{{ row.goodsPackageSpec || '-' }}</el-descriptions-item>
                <el-descriptions-item label="重量规格">{{ row.goodsWeightSpec || '-' }}</el-descriptions-item>
                <el-descriptions-item label="作废数">{{ row.wasteCount || 0 }}</el-descriptions-item>
                <el-descriptions-item label="绑定时间">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,19) : '-' }}</el-descriptions-item>
              </el-descriptions>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="enterpriseName" label="所属企业" min-width="120">
          <template #default="{ row }">{{ row.enterpriseName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单号" width="180">
          <template #default="{ row }">{{ row.orderNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="certNo" label="证书编号" width="130">
          <template #default="{ row }">{{ row.certNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="productName" label="产品名称" min-width="120">
          <template #default="{ row }">{{ row.productName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="productDescription" label="产品描述" min-width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.productDescription || '-' }}</template>
        </el-table-column>
        <el-table-column prop="goodsName" label="商品名称" min-width="100">
          <template #default="{ row }">{{ row.goodsName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="labelSpecName" label="标签规格" width="110">
          <template #default="{ row }">{{ row.labelSpecName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="goodsPackageSpec" label="包装规格" width="90">
          <template #default="{ row }">{{ row.goodsPackageSpec || '-' }}</template>
        </el-table-column>
        <el-table-column prop="goodsWeightSpec" label="重量规格" width="90">
          <template #default="{ row }">{{ row.goodsWeightSpec || '-' }}</template>
        </el-table-column>
        <el-table-column prop="serialStart" label="起始码" width="100" />
        <el-table-column prop="serialEnd" label="结束码" width="100" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="wasteCount" label="作废数" width="70">
          <template #default="{ row }">{{ row.wasteCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="绑定时间" width="170">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,19) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="toggleDetail(row)">
              <el-icon style="margin-right:2px"><ArrowRight v-if="!row._showDetail" /><ArrowDown v-else /></el-icon>{{ row._showDetail ? '收起' : '详情' }}
            </el-button>
            <el-button v-if="row.orderId" size="small" type="success" link @click="$router.push(`/admin/orders/${row.orderId}`)">分配条码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, sizes, prev, pager, next" :page-sizes="[20, 50, 100]" @change="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ArrowRight, ArrowDown } from '@element-plus/icons-vue'
import { getCodeDistribution, getAllEnterprises } from '@/api/admin'

const router = useRouter()

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const enterprises = ref<any[]>([])

const search = reactive({
  enterpriseId: null as number | null,
  orderNo: '',
  certNo: '',
})

onMounted(async () => {
  try { const res = await getAllEnterprises(); enterprises.value = res.data || [] } catch {}
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const params: any = { page: page.value, size: size.value }
    if (search.enterpriseId) params.enterpriseId = search.enterpriseId
    if (search.orderNo) params.orderNo = search.orderNo
    if (search.certNo) params.certNo = search.certNo
    const res = await getCodeDistribution(params)
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

const tableRef = ref()

function toggleDetail(row: any) {
  tableRef.value?.toggleRowExpansion(row)
}

function resetSearch() {
  search.enterpriseId = null
  search.orderNo = ''
  search.certNo = ''
  page.value = 1
  loadData()
}
</script>

<style scoped lang="scss">
.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}
</style>
