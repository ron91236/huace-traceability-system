<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <span style="font-weight:600;font-size:15px">溯源码作废</span>
          </div>
          <div style="display:flex;gap:8px">
            <el-button type="primary" @click="showAddDialog = true">+ 添加</el-button>
            <el-upload :show-file-list="false" accept=".xlsx,.xls" :before-upload="handleExcelImport">
              <el-button type="success">↑ 导入</el-button>
            </el-upload>
            <el-button @click="downloadTemplate">下载模板</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="packageNo" label="条码库" min-width="140" />
        <el-table-column prop="serialStart" label="开始身份码" width="140" />
        <el-table-column prop="serialEnd" label="结束身份码" width="140" />
        <el-table-column prop="count" label="作废数量" width="100" />
        <el-table-column prop="remark" label="备注" min-width="120" />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,19) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确认删除?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <!-- 手动添加对话框 -->
    <el-dialog v-model="showAddDialog" title="添加作废码" width="480px" :close-on-click-modal="false">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="流水号位数" required>
          <el-input-number v-model="addForm.serialDigits" :min="6" :max="20" style="width:100%" />
        </el-form-item>
        <el-form-item label="开始身份码" required>
          <el-input v-model="addForm.serialStart" placeholder="请输入开始身份码" />
        </el-form-item>
        <el-form-item label="结束身份码" required>
          <el-input v-model="addForm.serialEnd" placeholder="请输入结束身份码" />
        </el-form-item>
        <el-form-item label="作废数量" v-if="computedVoidCount > 0">
          <el-input :model-value="computedVoidCount.toLocaleString()" disabled />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAdd" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getVoidedCodeRanges, batchImportVoidedCodeRanges, deleteVoidedCodeRange } from '@/api/admin'

const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const showAddDialog = ref(false)

const addForm = reactive({
  serialDigits: 10,
  serialStart: '',
  serialEnd: '',
  remark: ''
})

onMounted(() => loadData())

const computedVoidCount = computed(() => {
  const start = addForm.serialStart
  const end = addForm.serialEnd
  if (!start || !end || !/^\d+$/.test(start) || !/^\d+$/.test(end)) return 0
  const s = parseInt(start)
  const e = parseInt(end)
  return e >= s ? e - s + 1 : 0
})

async function loadData() {
  loading.value = true
  try {
    const res = await getVoidedCodeRanges({ page: page.value, size: size.value })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function handleAdd() {
  if (!addForm.serialStart || !addForm.serialEnd) return ElMessage.warning('请输入开始和结束身份码')
  submitting.value = true
  try {
    await batchImportVoidedCodeRanges([addForm])
    ElMessage.success('添加成功')
    showAddDialog.value = false
    addForm.serialStart = ''
    addForm.serialEnd = ''
    addForm.remark = ''
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '添加失败')
  } finally { submitting.value = false }
}

async function handleExcelImport(file: File) {
  try {
    const XLSX = await import('xlsx')
    const buffer = await file.arrayBuffer()
    const workbook = XLSX.read(buffer)
    const sheet = workbook.Sheets[workbook.SheetNames[0]]
    const rows = XLSX.utils.sheet_to_json(sheet) as any[]
    if (rows.length === 0) return ElMessage.warning('Excel文件中无数据')

    const ranges = rows.map((row: any) => ({
      serialDigits: Number(row['流水号的位数'] || row['流水号位数'] || 10),
      serialStart: String(row['开始流水号'] || row['开始身份码'] || ''),
      serialEnd: String(row['结束流水号'] || row['结束身份码'] || ''),
      remark: row['备注'] || ''
    })).filter((r: any) => r.serialStart && r.serialEnd)

    if (ranges.length === 0) return ElMessage.warning('未找到有效数据行')

    await batchImportVoidedCodeRanges(ranges)
    ElMessage.success(`导入成功，共 ${ranges.length} 条`)
    loadData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.msg || '导入失败')
  }
  return false
}

async function downloadTemplate() {
  const XLSX = await import('xlsx')
  const wb = XLSX.utils.book_new()
  const data = [['流水号的位数', '开始流水号', '结束流水号', '备注'], [10, 54934651, 54941150, '']]
  const ws = XLSX.utils.aoa_to_sheet(data)
  ws['!cols'] = [{ wch: 14 }, { wch: 16 }, { wch: 16 }, { wch: 20 }]
  XLSX.utils.book_append_sheet(wb, ws, '作废码导入')
  XLSX.writeFile(wb, '溯源码作废导入模板.xlsx')
}

async function handleDelete(id: number) {
  await deleteVoidedCodeRange(id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped lang="scss">
.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrap {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>
