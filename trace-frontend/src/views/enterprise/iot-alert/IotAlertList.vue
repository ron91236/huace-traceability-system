<template>
  <div class="page-container">
    <el-card>
      <el-tabs v-model="activeTab">
        <!-- 告警记录 -->
        <el-tab-pane label="告警记录" name="records">
          <div class="table-toolbar" style="margin-bottom:12px">
            <el-select v-model="filterLevel" placeholder="告警级别" clearable style="width:140px" @change="loadAlerts">
              <el-option label="警告" value="WARNING" /><el-option label="严重" value="CRITICAL" />
            </el-select>
            <el-select v-model="filterStatus" placeholder="处理状态" clearable style="width:140px" @change="loadAlerts">
              <el-option label="未处理" :value="0" /><el-option label="已处理" :value="1" />
            </el-select>
            <el-button type="primary" @click="loadAlerts">搜索</el-button>
          </div>
          <el-table :data="alertList" v-loading="alertLoading" border stripe>
            <el-table-column prop="deviceName" label="设备" min-width="120" />
            <el-table-column prop="metricName" label="指标" width="100">
              <template #default="{ row }">{{ METRIC_LABELS[row.metricName] || row.metricName }}</template>
            </el-table-column>
            <el-table-column prop="alertLevel" label="级别" width="80">
              <template #default="{ row }"><el-tag :type="row.alertLevel === 'CRITICAL' ? 'danger' : 'warning'" size="small">{{ row.alertLevel === 'CRITICAL' ? '严重' : '警告' }}</el-tag></template>
            </el-table-column>
            <el-table-column label="触发值 / 阈值" width="140">
              <template #default="{ row }">{{ typeof row.metricValue === 'number' ? row.metricValue.toFixed(1) : row.metricValue }} / {{ row.threshold }}</template>
            </el-table-column>
            <el-table-column prop="alertMessage" label="告警信息" min-width="160" show-overflow-tooltip />
            <el-table-column prop="handleStatus" label="状态" width="80">
              <template #default="{ row }"><el-tag :type="row.handleStatus === 1 ? 'success' : 'danger'" size="small">{{ row.handleStatus === 1 ? '已处理' : '未处理' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="createdAt" label="时间" width="160" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button v-if="row.handleStatus !== 1" size="small" type="primary" @click="openHandle(row)">处理</el-button>
                <span v-else style="color:#999;font-size:12px">{{ row.handleNote || '已处理' }}</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 告警规则 -->
        <el-tab-pane label="告警规则" name="rules">
          <div class="table-toolbar" style="margin-bottom:12px">
            <el-button type="success" @click="openAddRule">新增规则</el-button>
          </div>
          <el-table :data="ruleList" v-loading="ruleLoading" border stripe>
            <el-table-column prop="deviceName" label="设备" min-width="120" />
            <el-table-column prop="metricName" label="指标" width="120">
              <template #default="{ row }">{{ METRIC_LABELS[row.metricName] || row.metricName }}</template>
            </el-table-column>
            <el-table-column label="条件" width="120">
              <template #default="{ row }">{{ row.operator }} {{ row.threshold }}</template>
            </el-table-column>
            <el-table-column prop="alertLevel" label="级别" width="80">
              <template #default="{ row }"><el-tag :type="row.alertLevel === 'CRITICAL' ? 'danger' : 'warning'" size="small">{{ row.alertLevel === 'CRITICAL' ? '严重' : '警告' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="alertMessage" label="告警信息" min-width="160" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 处理弹窗 -->
    <el-dialog v-model="handleVisible" title="处理告警" width="480px">
      <el-form label-width="80px">
        <el-form-item label="告警信息">{{ handleRow?.alertMessage }}</el-form-item>
        <el-form-item label="处理备注"><el-input v-model="handleNote" type="textarea" :rows="3" placeholder="请输入处理说明" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="handleVisible = false">取消</el-button>
        <el-button type="primary" @click="doHandle">确认处理</el-button>
      </template>
    </el-dialog>

    <!-- 新增规则弹窗 -->
    <el-dialog v-model="ruleDialogVisible" title="新增告警规则" width="560px">
      <el-form :model="ruleForm" label-width="100px">
        <el-form-item label="关联设备">
          <el-select v-model="ruleForm.deviceId" style="width:100%" filterable placeholder="选择设备">
            <el-option v-for="d in deviceOptions" :key="d.id" :label="d.deviceName" :value="d.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="监控指标">
              <el-select v-model="ruleForm.metricName" style="width:100%">
                <el-option v-for="(l, k) in METRIC_LABELS" :key="k" :label="l" :value="k" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="运算符">
              <el-select v-model="ruleForm.operator" style="width:100%">
                <el-option label=">" value=">" /><el-option label="<" value="<" />
                <el-option label=">=" value=">=" /><el-option label="<=" value="<=" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6"><el-form-item label="阈值"><el-input-number v-model="ruleForm.threshold" :precision="1" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="告警级别">
              <el-select v-model="ruleForm.alertLevel" style="width:100%">
                <el-option label="警告" value="WARNING" /><el-option label="严重" value="CRITICAL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="告警信息"><el-input v-model="ruleForm.alertMessage" placeholder="例如：温度过高" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getIotAlerts, handleIotAlert, getIotAlertRules, createIotAlertRule, getIotDevices } from '@/api/enterprise'

const METRIC_LABELS: Record<string, string> = { temperature: '温度', humidity: '湿度', soilMoisture: '土壤湿度', ph: 'pH值', co2: 'CO₂' }

const activeTab = ref('records')
const alertLoading = ref(false)
const alertList = ref<any[]>([])
const filterLevel = ref('')
const filterStatus = ref<number | ''>('')

const ruleLoading = ref(false)
const ruleList = ref<any[]>([])
const ruleDialogVisible = ref(false)
const deviceOptions = ref<any[]>([])
const ruleForm = reactive<any>({ deviceId: null, metricName: 'temperature', operator: '>', threshold: 35, alertLevel: 'WARNING', alertMessage: '' })

const handleVisible = ref(false)
const handleRow = ref<any>(null)
const handleNote = ref('')

onMounted(() => { loadAlerts(); loadRules(); loadDeviceOptions() })

async function loadAlerts() {
  alertLoading.value = true
  try { const res = await getIotAlerts({ alertLevel: filterLevel.value, handleStatus: filterStatus.value, page: 1, size: 100 }); alertList.value = res.data?.list || res.data || [] } finally { alertLoading.value = false }
}

async function loadRules() {
  ruleLoading.value = true
  try { const res = await getIotAlertRules({ page: 1, size: 100 }); ruleList.value = res.data?.list || res.data || [] } finally { ruleLoading.value = false }
}

async function loadDeviceOptions() {
  try { const res = await getIotDevices({ page: 1, size: 200 }); deviceOptions.value = res.data?.list || res.data || [] } catch {}
}

function openHandle(row: any) { handleRow.value = row; handleNote.value = ''; handleVisible.value = true }

async function doHandle() {
  await handleIotAlert(handleRow.value.id, { handleNote: handleNote.value })
  ElMessage.success('处理成功')
  handleVisible.value = false
  loadAlerts()
}

function openAddRule() {
  Object.assign(ruleForm, { deviceId: null, metricName: 'temperature', operator: '>', threshold: 35, alertLevel: 'WARNING', alertMessage: '' })
  ruleDialogVisible.value = true
}

async function saveRule() {
  await createIotAlertRule(ruleForm)
  ElMessage.success('规则已创建')
  ruleDialogVisible.value = false
  loadRules()
}
</script>
