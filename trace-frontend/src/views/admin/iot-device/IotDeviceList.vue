<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="设备名称 / 企业名称" clearable style="width:240px" @clear="loadData" @keyup.enter="loadData" />
          <el-select v-model="filterType" placeholder="设备类型" clearable style="width:140px" @change="loadData">
            <el-option label="土壤传感器" value="soil_sensor" /><el-option label="温湿度传感器" value="temp_sensor" />
            <el-option label="GPS追踪器" value="gps_tracker" /><el-option label="冷链温度计" value="cold_chain" />
          </el-select>
          <el-button type="primary" @click="loadData">搜索</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="enterpriseName" label="企业" min-width="140" />
        <el-table-column prop="deviceName" label="设备名称" min-width="140" />
        <el-table-column prop="deviceType" label="类型" width="120">
          <template #default="{ row }"><el-tag size="small" :type="TYPE_COLORS[row.deviceType]">{{ TYPE_LABELS[row.deviceType] || row.deviceType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="locationDesc" label="位置" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '在线' : '离线' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="lastOnlineAt" label="最后在线" width="160" />
        <el-table-column prop="createdAt" label="注册时间" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getAdminIotDevices } from '@/api/admin'

const TYPE_LABELS: Record<string, string> = { soil_sensor: '土壤传感器', temp_sensor: '温湿度传感器', gps_tracker: 'GPS追踪器', cold_chain: '冷链温度计' }
const TYPE_COLORS: Record<string, string> = { soil_sensor: 'success', temp_sensor: 'warning', gps_tracker: '', cold_chain: 'danger' }

const loading = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const filterType = ref('')

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try { const res = await getAdminIotDevices({ keyword: keyword.value, deviceType: filterType.value, page: 1, size: 200 }); list.value = res.data?.list || res.data || [] } finally { loading.value = false }
}
</script>
