<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-input v-model="search.certName" placeholder="证书名称" clearable style="width:140px" />
            <el-select v-model="search.enterpriseId" placeholder="所属企业" clearable filterable style="width:160px">
              <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
            </el-select>
            <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>搜索</el-button>
          </div>
          <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon>新增认证</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="certTypeName" label="证书类型" width="120">
          <template #default="{ row }"><el-tag size="small">{{ row.certTypeName || '-' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="certName" label="证书名称" min-width="140" />
        <el-table-column prop="certNo" label="证书编号" width="140">
          <template #default="{ row }">{{ row.certNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="enterpriseName" label="所属企业" width="140" />
        <el-table-column prop="labelSpecName" label="标签规格" width="120">
          <template #default="{ row }">{{ row.labelSpecName || '-' }}</template>
        </el-table-column>
        <el-table-column label="有效期" width="200">
          <template #default="{ row }"><span class="date-range">{{ row.startDate }} ~ {{ row.endDate }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isVoid ? 'danger' : 'success'" size="small">{{ row.isVoid ? '已作废' : '有效' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openForm(row)">编辑</el-button>
            <el-button size="small" type="warning" link @click="openCertProducts(row)">证书产品</el-button>
            <el-button size="small" type="success" link @click="showQrcode(row)">二维码</el-button>
            <el-popconfirm title="确认删除该认证?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑认证' : '新增认证'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="证书类型" prop="certTypeId">
          <el-select v-model="form.certTypeId" placeholder="请选择" filterable style="width:100%">
            <el-option v-for="c in certTypes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属企业" prop="enterpriseId">
          <el-select v-model="form.enterpriseId" placeholder="请选择" filterable style="width:100%">
            <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="证书名称" prop="certName"><el-input v-model="form.certName" placeholder="如：有机认证证书" /></el-form-item>
        <el-form-item label="证书编号"><el-input v-model="form.certNo" placeholder="如：150KZS2600010" /></el-form-item>
        <el-form-item label="标签规格">
          <el-select v-model="form.labelSpecId" placeholder="请选择标签规格" clearable filterable style="width:100%">
            <el-option v-for="ls in labelSpecs" :key="ls.id" :label="ls.specName" :value="ls.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="有效期开始"><el-date-picker v-model="form.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="有效期结束"><el-date-picker v-model="form.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
        <el-form-item label="是否作废"><el-switch v-model="form.isVoid" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="证书图片">
          <el-upload
            :key="imageFileListKey"
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
          <div v-if="form.certPdf" class="pdf-priority-hint">
            ⚠️ 已上传PDF，图片信息将不在溯源页展示
          </div>
        </el-form-item>
        <el-form-item label="证书PDF">
          <el-upload
            :show-file-list="false"
            :http-request="handlePdfUpload"
            accept=".pdf"
          >
            <el-button type="primary" plain>
              <el-icon><Upload /></el-icon> 上传PDF
            </el-button>
          </el-upload>
          <div v-if="form.certPdf" class="pdf-preview-row">
            <el-link :href="form.certPdf" target="_blank" type="primary">📄 {{ pdfFileName }}</el-link>
            <el-button link type="danger" @click="form.certPdf = ''">移除</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="qrVisible" title="证书二维码" width="340px" align-center>
      <div style="text-align:center">
        <img v-if="qrUrl" :src="qrUrl" style="width:200px;height:200px" />
        <div v-if="qrCertUrl" style="margin-top:12px;font-size:13px;color:#6b7280;word-break:break-all;">
          {{ qrCertUrl }}
        </div>
      </div>
    </el-dialog>

    <!-- 证书产品列表弹窗 -->
    <el-dialog v-model="certProductVisible" :title="`证书产品 - ${currentCert?.certName || ''}`" width="720px">
      <div style="margin-bottom:12px;text-align:right">
        <el-button type="primary" size="small" @click="openAddCertProduct"><el-icon><Plus /></el-icon>添加产品</el-button>
      </div>
      <el-table :data="certProducts" v-loading="certProductsLoading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="productName" label="产品名称" min-width="120" />
        <el-table-column prop="productDescription" label="产品描述" min-width="120">
          <template #default="{ row }">{{ row.productDescription || '-' }}</template>
        </el-table-column>
        <el-table-column prop="totalProduction" label="总产量(吨)" width="110" />
        <el-table-column prop="remainingProduction" label="剩余产量(吨)" width="120">
          <template #default="{ row }">
            <el-tag :type="row.remainingProduction > 0 ? 'success' : 'danger'" size="small">{{ row.remainingProduction }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-popconfirm title="确认移除该产品?" @confirm="handleRemoveCertProduct(row.id)">
              <template #reference><el-button size="small" type="danger" link>移除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 添加证书产品弹窗 -->
    <el-dialog v-model="addCertProductVisible" title="添加证书产品" width="460px">
      <el-form :model="certProductForm" label-width="100px">
        <el-form-item label="选择产品">
          <el-select v-model="certProductForm.productId" placeholder="请选择" filterable style="width:100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="总产量(吨)">
          <el-input-number v-model="certProductForm.totalProduction" :min="0" :precision="4" :step="1" style="width:100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addCertProductVisible = false">取消</el-button>
        <el-button type="primary" :loading="addCertProductSubmitting" @click="handleAddCertProduct">确定</el-button>
      </template>
    </el-dialog>

    <!-- 图片预览 -->
    <el-image-viewer v-if="previewVisible" :url-list="[previewUrl]" @close="previewVisible = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus, Upload } from '@element-plus/icons-vue'
import { getEnterpriseCerts, createEnterpriseCert, updateEnterpriseCert, deleteEnterpriseCert, getCertQrcode, getAllEnterprises, getCertProducts, addCertProduct, removeCertProduct, getLabelSpecs } from '@/api/admin'
import { getCertTypeOptions, getProductOptions, uploadFile } from '@/api/common'

const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const dialogVisible = ref(false)
const qrVisible = ref(false)
const qrUrl = ref('')
const qrCertUrl = ref('')
const editId = ref<number | null>(null)
const formRef = ref()
const search = reactive({ certName: '', enterpriseId: null as number | null })
const certTypes = ref<any[]>([])
const enterprises = ref<any[]>([])
const productOptions = ref<any[]>([])
const labelSpecs = ref<any[]>([])

// 证书产品相关
const certProductVisible = ref(false)
const certProducts = ref<any[]>([])
const certProductsLoading = ref(false)
const currentCert = ref<any>(null)
const addCertProductVisible = ref(false)
const addCertProductSubmitting = ref(false)
const certProductForm = reactive({ productId: null as number | null, totalProduction: 0 })

const previewVisible = ref(false)
const previewUrl = ref('')
const imageFileListKey = ref(0)

const form = reactive<any>({
  certTypeId: null, enterpriseId: null, certName: '', certNo: '', labelSpecId: null,
  startDate: '', endDate: '', isVoid: 0,
  certImage: '', certPdf: '',
})

const imageFileList = computed(() => {
  if (!form.certImage) return []
  return form.certImage.split(',').filter(Boolean).map((url: string, i: number) => ({
    name: `image_${i + 1}`,
    url: url
  }))
})

const pdfFileName = computed(() => {
  if (!form.certPdf) return ''
  const parts = form.certPdf.split('/')
  return parts[parts.length - 1] || 'cert.pdf'
})

const rules = {
  certTypeId: [{ required: true, message: '请选择', trigger: 'change' }],
  enterpriseId: [{ required: true, message: '请选择', trigger: 'change' }],
  certName: [{ required: true, message: '请输入', trigger: 'blur' }],
}

onMounted(async () => {
  const [ct, ent, prods, ls] = await Promise.all([getCertTypeOptions(), getAllEnterprises(), getProductOptions(), getLabelSpecs({ page: 1, size: 200 })])
  certTypes.value = ct.data || []
  enterprises.value = ent.data || []
  productOptions.value = prods.data || []
  labelSpecs.value = ls.data?.list || ls.data || []
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getEnterpriseCerts({ page: page.value, size: size.value, ...search })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function openForm(row?: any) {
  editId.value = row?.id || null
  Object.keys(form).forEach(k => { form[k] = row?.[k] ?? (k === 'isVoid' ? 0 : null) })
  imageFileListKey.value++
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) await updateEnterpriseCert(editId.value, form)
    else await createEnterpriseCert(form)
    ElMessage.success(editId.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await deleteEnterpriseCert(id)
  ElMessage.success('删除成功')
  loadData()
}

async function showQrcode(row: any) {
  try {
    const res = await getCertQrcode(row.id)
    qrUrl.value = res.data?.qrUrl || res.data
    qrCertUrl.value = res.data?.certUrl || ''
    qrVisible.value = true
  } catch (e) { ElMessage.error('获取二维码失败') }
}

async function handleImageUpload(options: any) {
  try {
    const res = await uploadFile(options.file)
    const url = res.data?.url || res.data || ''
    if (form.certImage) {
      form.certImage = form.certImage + ',' + url
    } else {
      form.certImage = url
    }
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

function handleImageRemove(file: any) {
  const urls = form.certImage.split(',').filter(Boolean)
  const idx = urls.indexOf(file.url)
  if (idx >= 0) urls.splice(idx, 1)
  form.certImage = urls.join(',')
}

function handleImagePreview(file: any) {
  previewUrl.value = file.url
  previewVisible.value = true
}

async function handlePdfUpload(options: any) {
  try {
    const res = await uploadFile(options.file)
    form.certPdf = res.data?.url || res.data || ''
    ElMessage.success('PDF上传成功')
  } catch (e) {
    ElMessage.error('PDF上传失败')
  }
}

async function openCertProducts(row: any) {
  currentCert.value = row
  certProductVisible.value = true
  await loadCertProducts(row.id)
}

async function loadCertProducts(certId: number) {
  certProductsLoading.value = true
  try {
    const res = await getCertProducts(certId)
    certProducts.value = res.data || []
  } finally { certProductsLoading.value = false }
}

function openAddCertProduct() {
  certProductForm.productId = null
  certProductForm.totalProduction = 0
  addCertProductVisible.value = true
}

async function handleAddCertProduct() {
  if (!certProductForm.productId) { ElMessage.warning('请选择产品'); return }
  if (!certProductForm.totalProduction || certProductForm.totalProduction <= 0) { ElMessage.warning('请输入总产量'); return }
  addCertProductSubmitting.value = true
  try {
    await addCertProduct(currentCert.value.id, certProductForm)
    ElMessage.success('添加成功')
    addCertProductVisible.value = false
    await loadCertProducts(currentCert.value.id)
  } finally { addCertProductSubmitting.value = false }
}

async function handleRemoveCertProduct(id: number) {
  await removeCertProduct(id)
  ElMessage.success('移除成功')
  await loadCertProducts(currentCert.value.id)
}
</script>

<style scoped lang="scss">
.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.date-range {
  font-size: 12px;
  color: #6b7280;
}

.pdf-priority-hint {
  color: #e6a23c;
  font-size: 12px;
  margin-top: 6px;
}

.pdf-preview-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 6px;
}
</style>
