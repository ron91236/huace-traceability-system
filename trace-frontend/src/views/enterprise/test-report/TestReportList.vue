<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="报告名称" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData" />
          <el-button type="primary" @click="openForm()">
            <el-icon><Plus /></el-icon>新增报告
          </el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="reportName" label="报告名称" min-width="160" />
        <el-table-column prop="testCode" label="检测编号" width="120" />
        <el-table-column prop="testOrg" label="检测机构" width="140" />
        <el-table-column prop="testResult" label="检测结果" width="100">
          <template #default="{ row }">
            <el-tag :type="row.testResult === '合格' ? 'success' : 'danger'" size="small">{{ row.testResult || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="报告文件" width="120">
          <template #default="{ row }">
            <span v-if="row.reportPdf">📄 PDF</span>
            <span v-else-if="row.reportImage">🖼️ {{ (row.reportImage || '').split(',').filter(Boolean).length }}张</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="testTime" label="检测时间" width="170" />
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button size="small" @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑检测报告' : '新增检测报告'" width="640px" top="5vh">
      <el-form ref="formRef" :model="form" :rules="{ reportName: [{ required: true, message: '请输入报告名称', trigger: 'blur' }] }" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="报告名称" prop="reportName"><el-input v-model="form.reportName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="检测编号"><el-input v-model="form.testCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="检测机构"><el-input v-model="form.testOrg" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="检测时间"><el-date-picker v-model="form.testTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="检测方式"><el-input v-model="form.testMethod" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="检测依据"><el-input v-model="form.testBasis" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="检测类型"><el-input v-model="form.testType" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="检测结果"><el-input v-model="form.testResult" /></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="检测报告">
              <el-upload
                :file-list="imageFileList"
                list-type="picture-card"
                :http-request="handleImageUpload"
                accept="image/*"
                multiple
                :limit="10"
                :on-remove="handleImageRemove"
                :on-preview="handleImagePreview"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
              <div v-if="form.reportPdf" class="pdf-priority-hint">
                ⚠️ 已上传PDF，图片信息将不在溯源页展示
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="检测PDF">
              <el-upload
                :show-file-list="false"
                :http-request="handlePdfUpload"
                accept=".pdf"
              >
                <el-button type="primary" plain>
                  <el-icon><Upload /></el-icon> 上传PDF
                </el-button>
              </el-upload>
              <div v-if="form.reportPdf" class="pdf-preview-row">
                <el-link :href="form.reportPdf" target="_blank" type="primary">📄 {{ pdfFileName }}</el-link>
                <el-button link type="danger" @click="form.reportPdf = ''">移除</el-button>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-image-viewer v-if="previewVisible" :url-list="[previewUrl]" @close="previewVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Upload } from '@element-plus/icons-vue'
import { getTestReports, createTestReport, updateTestReport, deleteTestReport } from '@/api/enterprise'
import { uploadFile } from '@/api/common'

const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()
const previewVisible = ref(false)
const previewUrl = ref('')

const form = reactive<any>({
  reportName: '', testCode: '', testOrg: '', testTime: '',
  testMethod: '', testBasis: '', testType: '', testResult: '',
  reportImage: '', reportPdf: ''
})

// 图片文件列表（用于el-upload回显）
const imageFileList = computed(() => {
  if (!form.reportImage) return []
  return form.reportImage.split(',').filter(Boolean).map((url: string, i: number) => ({
    name: `image_${i + 1}`,
    url: url
  }))
})

const pdfFileName = computed(() => {
  if (!form.reportPdf) return ''
  const parts = form.reportPdf.split('/')
  return parts[parts.length - 1] || 'report.pdf'
})

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getTestReports({ page: page.value, size: size.value, keyword: keyword.value })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function openForm(row?: any) {
  editId.value = row?.id || null
  Object.keys(form).forEach(k => { form[k] = row?.[k] || '' })
  dialogVisible.value = true
}

async function handleImageUpload(options: any) {
  try {
    const res = await uploadFile(options.file)
    const url = res.data?.url || res.data || ''
    // 追加到逗号分隔列表
    if (form.reportImage) {
      form.reportImage = form.reportImage + ',' + url
    } else {
      form.reportImage = url
    }
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

function handleImageRemove(file: any) {
  const urls = form.reportImage.split(',').filter(Boolean)
  const idx = urls.indexOf(file.url)
  if (idx >= 0) urls.splice(idx, 1)
  form.reportImage = urls.join(',')
}

function handleImagePreview(file: any) {
  previewUrl.value = file.url
  previewVisible.value = true
}

async function handlePdfUpload(options: any) {
  try {
    const res = await uploadFile(options.file)
    form.reportPdf = res.data?.url || res.data || ''
    ElMessage.success('PDF上传成功')
  } catch (e) {
    ElMessage.error('PDF上传失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) await updateTestReport(editId.value, form)
    else await createTestReport(form)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await deleteTestReport(id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped>
.table-toolbar { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.pdf-priority-hint { color: #e6a23c; font-size: 12px; margin-top: 6px; }
.pdf-preview-row { display: flex; align-items: center; gap: 8px; margin-top: 6px; }
</style>
