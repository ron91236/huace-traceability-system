<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="批次名称" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData" />
          <el-button type="primary" @click="openForm()">新增批次</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="name" label="批次名称" min-width="140" />
        <el-table-column prop="goodsName" label="商品名称" width="120" />
        <el-table-column prop="goodsSpec" label="规格" width="80" />
        <el-table-column prop="baseName" label="所属基地" width="120" />
        <el-table-column prop="testReportName" label="检测报告" width="140">
          <template #default="{ row }">{{ row.testReportName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button size="small" @click="openForm(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="handleCopy(row)">复制</el-button>
            <el-button size="small" type="success" @click="showQrcode(row)">二维码</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>
    <el-dialog v-model="dialogVisible" :title="editId ? '编辑批次' : '新增批次'" width="560px" top="5vh">
      <el-form ref="formRef" :model="form" :rules="{ name: [{ required: true, message: '请输入', trigger: 'blur' }] }" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="批次名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="商品"><el-select v-model="form.goodsId" filterable style="width:100%"><el-option v-for="g in goodsList" :key="g.id" :label="g.name" :value="g.id" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="商品规格"><el-input v-model="form.goodsSpec" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属基地"><el-select v-model="form.baseId" filterable style="width:100%"><el-option v-for="b in bases" :key="b.id" :label="b.name" :value="b.id" /></el-select></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="检测报告">
              <el-select v-model="form.testReportIds" multiple filterable clearable placeholder="可多选检测报告" style="width:100%">
                <el-option v-for="r in testReports" :key="r.id" :label="r.reportName" :value="r.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
    <el-dialog v-model="qrVisible" title="批次二维码" width="320px" align-center>
      <div style="text-align:center"><img v-if="qrUrl" :src="qrUrl" style="width:200px;height:200px" /></div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getBatches, createBatch, updateBatch, getBatchQrcode, copyBatch, getGoods, getBases, getAllTestReports } from '@/api/enterprise'

const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const dialogVisible = ref(false)
const qrVisible = ref(false)
const qrUrl = ref('')
const editId = ref<number | null>(null)
const formRef = ref()
const goodsList = ref<any[]>([])
const bases = ref<any[]>([])
const testReports = ref<any[]>([])

const form = reactive<any>({ name: '', goodsId: null, goodsSpec: '', baseId: null, testReportIds: [] })

onMounted(async () => {
  const [gRes, bRes, rRes] = await Promise.all([getGoods({ page: 1, size: 200 }), getBases({ page: 1, size: 200 }), getAllTestReports()])
  goodsList.value = gRes.data?.list || []; bases.value = bRes.data?.list || []; testReports.value = rRes.data || []
  loadData()
})

async function loadData() { loading.value = true; try { const res = await getBatches({ page: page.value, size: size.value, keyword: keyword.value }); list.value = res.data?.list || []; total.value = res.data?.total || 0 } finally { loading.value = false } }
function openForm(row?: any) {
  editId.value = row?.id || null
  form.name = row?.name || ''
  form.goodsId = row?.goodsId || null
  form.goodsSpec = row?.goodsSpec || ''
  form.baseId = row?.baseId || null
  form.testReportIds = row?.testReportIds || (row?.testReportId ? [row.testReportId] : [])
  dialogVisible.value = true
}
async function handleCopy(row: any) {
  await copyBatch(row.id)
  ElMessage.success('复制成功')
  loadData()
}
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false); if (!valid) return; submitting.value = true
  try { if (editId.value) await updateBatch(editId.value, form); else await createBatch(form); ElMessage.success('保存成功'); dialogVisible.value = false; loadData() } finally { submitting.value = false }
}
async function showQrcode(row: any) { try { const res = await getBatchQrcode(row.id); qrUrl.value = typeof res.data === 'string' ? res.data : (res.data?.qrUrl || ''); qrVisible.value = true } catch (e) { ElMessage.error('获取二维码失败') } }
</script>
