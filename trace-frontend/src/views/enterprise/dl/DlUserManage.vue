<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-select v-if="isAdmin" v-model="entFilter" placeholder="全部企业" clearable style="width:180px" @change="loadData">
            <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
          <el-input v-model="keyword" placeholder="用户名/手机号" clearable style="width:200px"
            @keyup.enter="loadData" @clear="loadData" />
          <el-button type="primary" @click="loadData">搜索</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="180">
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column v-if="isAdmin" label="所属企业" width="150">
          <template #default="{ row }">{{ entName(row.enterpriseId) }}</template>
        </el-table-column>
        <el-table-column label="账号级别" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.accountLevel === 'master' ? 'warning' : 'info'" size="small">
              {{ row.accountLevel === 'master' ? '母账号' : '子账号' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="showDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="用户详情" width="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ detail?.username }}</el-descriptions-item>
        <el-descriptions-item label="昵称">{{ detail?.nickname || '-' }}</el-descriptions-item>
        <el-descriptions-item label="手机号">{{ detail?.phone || '-' }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ detail?.email || '-' }}</el-descriptions-item>
        <el-descriptions-item label="账号级别">{{ detail?.accountLevel === 'master' ? '母账号' : '子账号' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail?.status === 1 ? '正常' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail?.createdAt }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDlUsers } from '@/api/digital-label'
import { useDlAdmin } from '@/composables/useDlAdmin'

const { isAdmin, entFilter, enterprises, entName } = useDlAdmin()
const list = ref<any[]>([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const size = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const detail = ref<any>(null)

async function loadData() {
  loading.value = true
  try {
    const res = await getDlUsers({ page: page.value, size: size.value, keyword: keyword.value || undefined, enterpriseId: entFilter.value })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function showDetail(row: any) {
  detail.value = row
  detailVisible.value = true
}

onMounted(loadData)
</script>
