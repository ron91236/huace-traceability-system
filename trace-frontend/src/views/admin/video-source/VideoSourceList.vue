<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="摄像头名称 / 企业名称" clearable style="width:240px" @clear="loadData" @keyup.enter="loadData" />
          <el-button type="primary" @click="loadData">搜索</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="enterpriseName" label="企业" min-width="140" />
        <el-table-column prop="cameraName" label="摄像头名称" min-width="140" />
        <el-table-column prop="streamType" label="流类型" width="80">
          <template #default="{ row }"><el-tag size="small">{{ row.streamType || 'HLS' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="streamUrl" label="流地址" min-width="200" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminVideoSources } from '@/api/admin'

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try { const res = await getAdminVideoSources({ keyword: keyword.value, page: 1, size: 200 }); list.value = res.data?.list || res.data || [] } finally { loading.value = false }
}
</script>
