<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="设备名称" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData" />
          <el-select v-model="filterType" placeholder="设备类型" clearable style="width:140px" @change="loadData">
            <el-option label="土壤传感器" value="soil_sensor" /><el-option label="温湿度传感器" value="temp_sensor" />
            <el-option label="GPS追踪器" value="gps_tracker" /><el-option label="冷链温度计" value="cold_chain" />
          </el-select>
          <el-button type="primary" @click="loadData">搜索</el-button>
          <el-button type="success" @click="openAdd">新增设备</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="deviceName" label="设备名称" min-width="140" />
        <el-table-column prop="deviceType" label="类型" width="120">
          <template #default="{ row }"><el-tag size="small" :type="TYPE_COLORS[row.deviceType]">{{ TYPE_LABELS[row.deviceType] || row.deviceType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="locationDesc" label="位置" min-width="120" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '在线' : '离线' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="lastOnlineAt" label="最后在线" width="160" />
        <el-table-column label="操作" width="200">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑设备' : '新增设备'" width="620px">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="设备名称"><el-input v-model="form.deviceName" /></el-form-item></el-col>
          <el-col :span="12">
            <el-form-item label="设备类型">
              <el-select v-model="form.deviceType" style="width:100%">
                <el-option v-for="(l, k) in TYPE_LABELS" :key="k" :label="l" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="位置描述"><el-input v-model="form.locationDesc" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="经度"><el-input-number v-model="form.longitude" :precision="6" :step="0.001" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="纬度"><el-input-number v-model="form.latitude" :precision="6" :step="0.001" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="关联基地"><el-input-number v-model="form.baseId" :min="0" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="ProductKey"><el-input v-model="form.productKey" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="DeviceKey"><el-input v-model="form.deviceKey" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 设备详情抽屉 -->
    <el-drawer v-model="drawerVisible" title="设备详情" size="480px">
      <template v-if="detailDevice">
        <h4>{{ detailDevice.deviceName }} <el-tag size="small">{{ TYPE_LABELS[detailDevice.deviceType] }}</el-tag></h4>
        <el-descriptions :column="1" border size="small" style="margin:12px 0">
          <el-descriptions-item label="位置">{{ detailDevice.locationDesc }}</el-descriptions-item>
          <el-descriptions-item label="状态"><el-tag :type="detailDevice.status === 1 ? 'success' : 'danger'" size="small">{{ detailDevice.status === 1 ? '在线' : '离线' }}</el-tag></el-descriptions-item>
          <el-descriptions-item label="最后在线">{{ detailDevice.lastOnlineAt || '-' }}</el-descriptions-item>
        </el-descriptions>
        <h4>最新读数</h4>
        <div v-if="latestData" class="latest-grid">
          <div v-for="(v, k) in latestData.metrics" :key="k" class="latest-item">
            <span class="latest-label">{{ METRIC_LABELS[String(k)] || k }}</span>
            <span class="latest-value">{{ typeof v === 'number' ? v.toFixed(1) : v }}</span>
          </div>
        </div>
        <div v-else style="color:#999;font-size:13px">暂无数据</div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getIotDevices, createIotDevice, updateIotDevice, deleteIotDevice, getDeviceLatest } from '@/api/enterprise'

const TYPE_LABELS: Record<string, string> = { soil_sensor: '土壤传感器', temp_sensor: '温湿度传感器', gps_tracker: 'GPS追踪器', cold_chain: '冷链温度计' }
const TYPE_COLORS: Record<string, string> = { soil_sensor: 'success', temp_sensor: 'warning', gps_tracker: '', cold_chain: 'danger' }
const METRIC_LABELS: Record<string, string> = { temperature: '温度(°C)', humidity: '湿度(%)', soilMoisture: '土壤湿度(%)', ph: 'pH值', lightIntensity: '光照(lux)', co2: 'CO₂(ppm)' }

const loading = ref(false)
const saving = ref(false)
const list = ref<any[]>([])
const keyword = ref('')
const filterType = ref('')
const dialogVisible = ref(false)
const drawerVisible = ref(false)
const form = reactive<any>({ deviceName: '', deviceType: 'soil_sensor', locationDesc: '', longitude: null, latitude: null, baseId: null, productKey: '', deviceKey: '' })
const editId = ref(0)
const detailDevice = ref<any>(null)
const latestData = ref<any>(null)

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try { const res = await getIotDevices({ keyword: keyword.value, deviceType: filterType.value, page: 1, size: 100 }); list.value = res.data?.list || res.data || [] } finally { loading.value = false }
}

function openAdd() {
  editId.value = 0
  Object.assign(form, { deviceName: '', deviceType: 'soil_sensor', locationDesc: '', longitude: null, latitude: null, baseId: null, productKey: '', deviceKey: '' })
  dialogVisible.value = true
}

function openEdit(row: any) {
  editId.value = row.id
  Object.keys(form).forEach(k => { form[k] = row[k] ?? form[k] })
  dialogVisible.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editId.value) await updateIotDevice(editId.value, form)
    else await createIotDevice(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { saving.value = false }
}

async function handleDelete(id: number) {
  await ElMessageBox.confirm('确认删除该设备？', '提示', { type: 'warning' })
  await deleteIotDevice(id)
  ElMessage.success('已删除')
  loadData()
}

async function openDetail(row: any) {
  detailDevice.value = row
  latestData.value = null
  drawerVisible.value = true
  try { const res = await getDeviceLatest(row.id); latestData.value = res.data } catch {}
}
</script>

<style scoped>
.latest-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin: 8px 0; }
.latest-item { padding: 10px; background: #f5f7fa; border-radius: 8px; display: flex; justify-content: space-between; }
.latest-label { color: #666; font-size: 13px; }
.latest-value { font-weight: 700; color: #059669; font-size: 15px; }
</style>
