<template>
  <div class="page-container">
    <!-- 码包生成记录列表 -->
    <el-card class="history-card">
      <template #header>
        <div class="table-toolbar">
          <span class="section-title">码包发行记录</span>
          <el-button type="primary" @click="showGenerateForm = !showGenerateForm">
            {{ showGenerateForm ? '收起' : '新增数码发行' }}
          </el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe size="small">
        <el-table-column prop="packageNo" label="码包编号" width="160" />
        <el-table-column prop="importTime" label="发行时间" width="170">
          <template #default="{ row }">{{ formatTime(row.importTime) }}</template>
        </el-table-column>
        <el-table-column prop="startQuantity" label="起始数量" width="100" />
        <el-table-column prop="totalCount" label="码数量" width="90">
          <template #default="{ row }">{{ formatCount(row.totalCount) }}</template>
        </el-table-column>
        <el-table-column prop="serialStart" label="流水号起" width="100" />
        <el-table-column prop="serialEnd" label="流水号止" width="100" />
        <el-table-column prop="serialDigits" label="流水号位数" width="90" />
        <el-table-column prop="antiFakeDigits" label="防伪码位数" width="90" />
        <el-table-column label="验证模式" width="100">
          <template #default="{ row }">
            <el-tag :type="row.verifyMode === 'DIRECT' ? 'success' : 'info'" size="small">
              {{ row.verifyMode === 'DIRECT' ? '扫码即查' : '输入防伪码' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="productCode" label="产品代码" width="90" />
        <el-table-column label="生码类型" width="140">
          <template #default="{ row }">
            <el-tag size="small">{{ codeTypeMap[row.codeType] || row.codeType || '导入' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="handleExport(row.id)">导出</el-button>
            <el-button size="small" type="primary" link @click="showDetail(row)">明细</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <!-- 新增数码发行表单 -->
    <el-card v-if="showGenerateForm" class="generate-card" style="margin-top:16px">
      <template #header>
        <span class="section-title">新增数码发行</span>
      </template>
      <el-form :model="genForm" label-width="110px" size="default">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品代码">
              <el-input v-model="genForm.productCode" placeholder="可选，如 0150" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年份代码">
              <el-input v-model="genForm.yearCode" placeholder="可选，如 26" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="码套数量">
              <el-input-number v-model="genForm.quantity" :min="1" :max="200" :step="1" style="width:160px" />
              <span style="margin-left:8px;color:#dc2626;font-weight:600">万</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生码规则">
              <el-select v-model="codeRule" style="width:280px" @change="onRuleChange">
                <el-option value="A" label="规则A: 8位流水 + 10位防伪码（输入验证）" />
                <el-option value="B" label="规则B: 10位流水 + 12位防伪码（输入验证）" />
                <el-option value="C" label="规则C: 8位流水（扫码即防伪）" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="起始流水号">
              <el-input-number v-model="genForm.serialStart" :min="1" controls-position="right" style="width:200px" disabled />
              <span style="margin-left:8px;color:#909399;font-size:12px">根据历史码段自动生成</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="终止流水号">
              <el-input-number v-model="genForm.serialEnd" :min="1" controls-position="right" style="width:200px" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="起始数量">
              <el-input-number v-model="genForm.startQuantity" :min="1" controls-position="right" style="width:200px" disabled />
              <span style="margin-left:8px;color:#909399;font-size:12px">根据历史码段自动生成</span>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="生码类型">
              <el-select v-model="genForm.codeType" style="width:240px">
                <el-option value="SERIAL_URL_ANTI" label="2-流水号+网址+防伪码" />
                <el-option value="ANTI_ONLY" label="1-单防伪码" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="防伪验证模式">
              <el-radio-group v-model="genForm.verifyMode">
                <el-radio value="INPUT">输入防伪码验证</el-radio>
                <el-radio value="DIRECT">扫码即防伪</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="溯源码前缀">
              <el-input v-model="genForm.urlPrefix" placeholder="https://trace.cti-pit.com/?a=" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注内容">
              <el-input v-model="genForm.remark" type="textarea" :rows="2" placeholder="如: 25乘17无抗左出标55万枚" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item>
          <el-button type="success" size="large" :loading="generating" @click="handleGenerate"
            style="padding:12px 40px;font-size:15px;font-weight:600">
            输出数码
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 码包明细弹窗 -->
    <el-dialog v-model="detailVisible" title="码包明细" width="750px">
      <el-table :data="detailList" stripe max-height="400" size="small">
        <el-table-column prop="serialNo" label="流水号" width="120" />
        <el-table-column prop="antiFakeCode" label="防伪码" width="180" />
        <el-table-column prop="url" label="溯源网址" />
        <el-table-column label="绑定状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.bindStatus === 'BOUND' ? 'success' : 'info'" size="small">
              {{ row.bindStatus === 'BOUND' ? '已绑定' : '未绑定' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getCodePackages, getCodePackageDetail, generateCodePackage, exportCodePackage, getLastSerialByRule } from '@/api/admin'
import { codePackageStatusMap } from '@/utils/constants'

const statusMap = codePackageStatusMap
const codeTypeMap: Record<string, string> = {
  'SERIAL_URL_ANTI': '流水号+网址+防伪码',
  'ANTI_ONLY': '单防伪码'
}

const loading = ref(false)
const generating = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const showGenerateForm = ref(true)
const detailVisible = ref(false)
const detailList = ref<any[]>([])

const codeRule = ref('A')

const genForm = reactive({
  productCode: '',
  yearCode: '',
  quantity: 1,
  serialDigits: 8,
  antiFakeDigits: 10,
  serialStart: 1,
  serialEnd: 10000,
  startQuantity: 1,
  codeType: 'SERIAL_URL_ANTI',
  urlPrefix: 'https://trace.cti-pit.com/?a=',
  verifyMode: 'INPUT',
  remark: ''
})

async function onRuleChange(rule: string) {
  if (rule === 'A') {
    genForm.serialDigits = 8
    genForm.antiFakeDigits = 10
    genForm.verifyMode = 'INPUT'
  } else if (rule === 'B') {
    genForm.serialDigits = 10
    genForm.antiFakeDigits = 12
    genForm.verifyMode = 'INPUT'
  } else if (rule === 'C') {
    genForm.serialDigits = 8
    genForm.antiFakeDigits = 0
    genForm.verifyMode = 'DIRECT'
  }
  // 根据规则获取最后流水号，自动填充起始/终止流水号
  await fetchLastSerial()
}

async function fetchLastSerial() {
  try {
    const res = await getLastSerialByRule(genForm.serialDigits)
    const nextStart = res.data?.nextSerialStart || 1
    genForm.serialStart = nextStart
    genForm.startQuantity = nextStart
    const count = genForm.quantity * 10000
    genForm.serialEnd = nextStart + count - 1
  } catch {
    // request 拦截器已处理错误提示
  }
}

// 码套数量变化时自动更新终止流水号
watch(() => genForm.quantity, (newVal) => {
  const count = newVal * 10000
  genForm.serialEnd = genForm.serialStart + count - 1
})

onMounted(async () => {
  loadData()
  await fetchLastSerial()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getCodePackages({ page: page.value, size: size.value })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

function formatCount(n: number) {
  if (n >= 10000) return (n / 10000).toFixed(0) + '万'
  return String(n)
}

async function handleGenerate() {
  const count = genForm.quantity * 10000
  const serialStart = genForm.serialStart
  const serialEnd = serialStart + count - 1

  try {
    await ElMessageBox.confirm(
      `即将生成 ${formatCount(count)} 条数码，流水号范围 ${serialStart} ~ ${serialEnd}，确认继续？`,
      '确认生成', { type: 'warning' }
    )
  } catch { return }

  generating.value = true
  try {
    await generateCodePackage({
      productCode: genForm.productCode,
      yearCode: genForm.yearCode,
      serialDigits: genForm.serialDigits,
      antiFakeDigits: genForm.antiFakeDigits,
      serialStart: serialStart,
      serialEnd: serialEnd,
      startQuantity: genForm.startQuantity,
      codeType: genForm.codeType,
      urlPrefix: genForm.urlPrefix,
      verifyMode: genForm.verifyMode,
      remark: genForm.remark
    })
    ElMessage.success('码包生成成功！')
    genForm.serialStart = serialEnd + 1
    genForm.serialEnd = genForm.serialStart + count - 1
    genForm.startQuantity = genForm.startQuantity + count
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '生成失败')
  } finally { generating.value = false }
}

async function handleExport(id: number) {
  try {
    await exportCodePackage(id)
    ElMessage.success('导出成功')
  } catch (e: any) {
    ElMessage.error('导出失败，请重试')
  }
}

async function showDetail(row: any) {
  try {
    const res = await getCodePackageDetail(row.id)
    detailList.value = res.data?.items || []
    detailVisible.value = true
  } catch (e: any) {
    ElMessage.error('获取明细失败')
  }
}
</script>

<style scoped lang="scss">
.section-title {
  font-weight: 700;
  font-size: 15px;
  color: #064e3b;
}

.history-card {
  margin-bottom: 0;
}

.generate-card {
  border: 2px solid #059669 !important;
  border-radius: 8px;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 1px solid #f5f5f5;
}
</style>
