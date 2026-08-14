<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-select v-model="loginType" placeholder="登录类型" clearable style="width:130px">
              <el-option label="PC" value="PC" />
              <el-option label="移动端" value="mobile" />
            </el-select>
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="开始日期" end-placeholder="结束日期"
              value-format="YYYY-MM-DD" style="width:260px" />
            <el-button type="primary" @click="loadData">搜索</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="username" label="用户" width="150" />
        <el-table-column label="登录类型" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.loginType === 'mobile' ? 'warning' : 'info'" size="small">
              {{ row.loginType === 'mobile' ? '移动端' : 'PC' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="country" label="国家" width="100">
          <template #default="{ row }">{{ row.country || '-' }}</template>
        </el-table-column>
        <el-table-column prop="province" label="省份" width="120">
          <template #default="{ row }">{{ row.province || '-' }}</template>
        </el-table-column>
        <el-table-column prop="city" label="城市" width="120">
          <template #default="{ row }">{{ row.city || '-' }}</template>
        </el-table-column>
        <el-table-column prop="loginTime" label="登录时间" min-width="170" />
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDlLoginLogs } from '@/api/digital-label'

const list = ref<any[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const loginType = ref('')
const dateRange = ref<string[]>([])

async function loadData() {
  loading.value = true
  try {
    const res = await getDlLoginLogs({
      page: page.value, size: size.value,
      loginType: loginType.value || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
    })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>
