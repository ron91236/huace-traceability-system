<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="搜索公告标题" clearable style="width:240px" @clear="loadData" @keyup.enter="loadData" />
          <el-button type="primary" @click="loadData">搜索</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="title" label="标题" min-width="200" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getNotices } from '@/api/enterprise'
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
onMounted(() => loadData())
async function loadData() { loading.value = true; try { const res = await getNotices({ page: page.value, size: size.value, keyword: keyword.value }); list.value = res.data?.list || []; total.value = res.data?.total || 0 } finally { loading.value = false } }
</script>
