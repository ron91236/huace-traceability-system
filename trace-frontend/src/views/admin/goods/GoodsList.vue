<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-input v-model="search.goodsName" placeholder="商品名称" clearable style="width:140px" />
            <el-input v-model="search.productName" placeholder="产品名称" clearable style="width:140px" />
            <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>搜索</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="productName" label="产品名称" width="120">
          <template #default="{ row }"><el-tag size="small" type="success">{{ row.productName || '-' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="140" />
        <el-table-column prop="enterpriseName" label="所属企业" width="140" />
        <el-table-column prop="packageSpec" label="包装规格" width="100" />
        <el-table-column prop="weightSpec" label="重量规格" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,19) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="商品详情" width="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="商品名称" :span="2">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="产品名称">{{ detail.productName }}</el-descriptions-item>
        <el-descriptions-item label="所属企业">{{ detail.enterpriseName }}</el-descriptions-item>
        <el-descriptions-item label="包装规格">{{ detail.packageSpec }}</el-descriptions-item>
        <el-descriptions-item label="重量规格">{{ detail.weightSpec }}</el-descriptions-item>
        <el-descriptions-item label="储存方式" :span="2">{{ detail.storageMethod }}</el-descriptions-item>
        <el-descriptions-item label="食用方式" :span="2">{{ detail.eatingMethod }}</el-descriptions-item>
        <el-descriptions-item label="商品介绍" :span="2">
          <div class="intro-text">{{ detail.introduction }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getAdminGoods } from '@/api/admin'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const search = reactive({ goodsName: '', productName: '' })
const detailVisible = ref(false)
const detail = ref<any>({})

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminGoods({ page: page.value, size: size.value, ...search })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function viewDetail(row: any) {
  detail.value = row
  detailVisible.value = true
}
</script>

<style scoped lang="scss">
.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.intro-text {
  word-break: break-all;
  white-space: pre-wrap;
  max-height: 200px;
  overflow-y: auto;
}
</style>
