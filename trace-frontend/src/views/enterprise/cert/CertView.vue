<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div style="display:flex;gap:8px">
            <el-input v-model="search.certName" placeholder="证书名称" clearable style="width:140px" />
            <el-button type="primary" @click="loadData">搜索</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="certTypeName" label="证书类型" width="120" />
        <el-table-column prop="certName" label="证书名称" min-width="160" />
        <el-table-column prop="productName" label="产品名称" width="120" />
        <el-table-column label="有效期" width="200"><template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template></el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.isVoid ? 'danger' : 'success'">{{ row.isVoid ? '已作废' : '有效' }}</el-tag></template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getEnterpriseCerts } from '@/api/enterprise'
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const search = reactive({ certName: '' })
onMounted(() => loadData())
async function loadData() {
  loading.value = true
  try { const res = await getEnterpriseCerts({ page: page.value, size: size.value, ...search }); list.value = res.data?.list || []; total.value = res.data?.total || 0 } finally { loading.value = false }
}
</script>
