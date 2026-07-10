<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-select v-model="search.enterpriseId" placeholder="所属企业" clearable filterable style="width:160px">
              <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
            </el-select>
            <el-input v-model="search.serialNo" placeholder="流水号" clearable style="width:140px" />
            <el-date-picker v-model="search.dateRange" type="daterange" range-separator="~" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" style="width:240px" />
            <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>搜索</el-button>
            <el-button @click="resetSearch">重置</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe size="small">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="enterpriseName" label="所属企业" min-width="140" />
        <el-table-column prop="orderNo" label="订单号" width="160" />
        <el-table-column prop="serialNo" label="流水号" width="120" />
        <el-table-column prop="goodsName" label="商品名称" min-width="120" />
        <el-table-column prop="certName" label="证书名称" width="120" />
        <el-table-column prop="batchName" label="批次" width="100" />
        <el-table-column prop="bindTime" label="绑定时间" width="170">
          <template #default="{ row }">{{ row.bindTime || '-' }}</template>
        </el-table-column>
        <el-table-column prop="url" label="溯源链接" width="100">
          <template #default="{ row }">
            <el-link v-if="row.url" :href="row.url" target="_blank" type="primary">查看</el-link>
            <span v-else>-</span>
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
import { Search } from '@element-plus/icons-vue'
import { getCodeDistribution, getAllEnterprises } from '@/api/admin'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const enterprises = ref<any[]>([])

const search = reactive({
  enterpriseId: null as number | null,
  serialNo: '',
  dateRange: null as string[] | null,
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
    if (search.serialNo) params.serialNo = search.serialNo
    const res = await getCodeDistribution(params)
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function resetSearch() {
  search.enterpriseId = null
  search.serialNo = ''
  search.dateRange = null
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
