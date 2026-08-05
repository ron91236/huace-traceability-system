<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="商品名称" clearable style="width:200px" @clear="loadData" @keyup.enter="loadData" />
          <el-button type="primary" @click="openForm()">新增商品</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="productName" label="产品名称" width="120" />
        <el-table-column prop="name" label="商品名称" min-width="140" />
        <el-table-column label="样品图" width="120" align="center">
          <template #default="{ row }">
            <template v-if="row.sampleImage">
              <el-image v-for="(url, idx) in row.sampleImage.split(',').filter(Boolean)" :key="idx" :src="url" fit="cover" style="width:36px;height:36px;border-radius:4px;margin:2px" :on-preview="() => previewImage(url)" />
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="宣传图" width="120" align="center">
          <template #default="{ row }">
            <template v-if="row.promoImage">
              <el-image v-for="(url, idx) in row.promoImage.split(',').filter(Boolean)" :key="idx" :src="url" fit="cover" style="width:36px;height:36px;border-radius:4px;margin:2px" :on-preview="() => previewImage(url)" />
            </template>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="traceTemplateName" label="溯源模板" width="140">
          <template #default="{ row }">{{ row.traceTemplateName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="packageSpec" label="包装规格" width="100" />
        <el-table-column prop="weightSpec" label="重量规格" width="100" />
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="180">
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑商品' : '新增商品'" width="600px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="{ name: [{ required: true, message: '请输入', trigger: 'blur' }], productId: [{ required: true, message: '请选择', trigger: 'change' }] }" label-width="100px">
        <el-form-item label="产品名称" prop="productId">
          <el-select v-model="form.productId" filterable style="width:100%"><el-option v-for="p in products" :key="p.id" :label="p.name" :value="p.id" /></el-select>
        </el-form-item>
        <el-form-item label="商品名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="溯源模板">
          <el-select v-model="form.traceTemplateId" placeholder="请选择溯源模板" clearable filterable style="width:100%">
            <el-option v-for="t in traceTemplates" :key="t.id" :label="t.templateName" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="包装规格">
          <el-input v-model="form.packageSpec" placeholder="如 500g/袋、1kg/盒">
            <template #append>
              <el-select v-model="form.packageSpecUnit" style="width:90px">
                <el-option label="g/袋" value="g/袋" />
                <el-option label="kg/袋" value="kg/袋" />
                <el-option label="g/盒" value="g/盒" />
                <el-option label="kg/盒" value="kg/盒" />
                <el-option label="g/包" value="g/包" />
                <el-option label="kg/箱" value="kg/箱" />
                <el-option label="自定义" value="__custom__" />
              </el-select>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="重量规格">
          <el-input v-model="form.weightSpec" placeholder="请输入重量，单位为kg">
            <template #append>kg</template>
          </el-input>
        </el-form-item>
        <el-form-item label="储存方式"><el-input v-model="form.storageMethod" /></el-form-item>
        <el-form-item label="食用方式"><el-input v-model="form.eatingMethod" /></el-form-item>
        <el-form-item label="商品介绍"><el-input v-model="form.introduction" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="样品图">
          <el-upload
            :file-list="sampleFileList"
            list-type="picture-card"
            accept="image/*"
            :http-request="(opt: any) => handleMultiImageUpload(opt, 'sampleImage', 'sampleFileList')"
            :on-remove="(file: any) => handleImageRemove(file, 'sampleImage', 'sampleFileList')"
            :on-preview="(file: any) => previewImage(file.url)"
          >
            <el-icon :size="28" color="#8c939d"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
        <el-form-item label="宣传图">
          <el-upload
            :file-list="promoFileList"
            list-type="picture-card"
            accept="image/*"
            :http-request="(opt: any) => handleMultiImageUpload(opt, 'promoImage', 'promoFileList')"
            :on-remove="(file: any) => handleImageRemove(file, 'promoImage', 'promoFileList')"
            :on-preview="(file: any) => previewImage(file.url)"
          >
            <el-icon :size="28" color="#8c939d"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getGoods, createGoods, updateGoods, deleteGoods, getAssignedTemplates } from '@/api/enterprise'
import { getProductOptions, uploadFile } from '@/api/common'

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
const products = ref<any[]>([])
const traceTemplates = ref<any[]>([])
const sampleFileList = ref<any[]>([])
const promoFileList = ref<any[]>([])

const form = reactive<any>({ productId: null, name: '', packageSpec: '', packageSpecUnit: '', weightSpec: '', storageMethod: '', eatingMethod: '', introduction: '', sampleImage: '', promoImage: '', traceTemplateId: null })

onMounted(async () => { const [prodRes, tplRes] = await Promise.all([getProductOptions(), getAssignedTemplates()]); products.value = prodRes.data || []; traceTemplates.value = tplRes.data || []; loadData() })

async function loadData() {
  loading.value = true
  try { const res = await getGoods({ page: page.value, size: size.value, keyword: keyword.value }); list.value = res.data?.list || []; total.value = res.data?.total || 0 } finally { loading.value = false }
}

function urlsToFileList(urls: string) {
  if (!urls) return []
  return urls.split(',').filter(Boolean).map((url, i) => ({ name: `image-${i}`, url }))
}

function openForm(row?: any) {
  editId.value = row?.id || null
  Object.keys(form).forEach(k => {
    if (k === 'packageSpecUnit') form[k] = row?.packageSpecUnit || ''
    else form[k] = row?.[k] ?? null
  })
  // Parse package spec unit if it matches a known unit
  if (form.packageSpec) {
    const unitMatch = form.packageSpec.match(/(g\/袋|kg\/袋|g\/盒|kg\/盒|g\/包|kg\/箱)$/)
    if (unitMatch) {
      form.packageSpecUnit = unitMatch[1]
      form.packageSpec = form.packageSpec.replace(unitMatch[1], '').trim()
    } else {
      // No matching unit means it was custom
      form.packageSpecUnit = '__custom__'
    }
  }
  sampleFileList.value = urlsToFileList(form.sampleImage || '')
  promoFileList.value = urlsToFileList(form.promoImage || '')
  dialogVisible.value = true
}

async function handleMultiImageUpload(options: any, field: string, listName: string) {
  try {
    const res = await uploadFile(options.file)
    const url = res.data?.url || res.data || ''
    const fileListRef = listName === 'sampleFileList' ? sampleFileList : promoFileList
    fileListRef.value.push({ name: options.file.name, url })
    form[field] = fileListRef.value.map((f: any) => f.url).join(',')
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

function handleImageRemove(file: any, field: string, listName: string) {
  const fileListRef = listName === 'sampleFileList' ? sampleFileList : promoFileList
  fileListRef.value = fileListRef.value.filter((f: any) => f.url !== file.url)
  form[field] = fileListRef.value.map((f: any) => f.url).join(',')
}

function previewImage(url: string) {
  window.open(url, '_blank')
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const submitData = { ...form }
    // Append unit to packageSpec if selected (not custom)
    if (submitData.packageSpecUnit && submitData.packageSpecUnit !== '__custom__') {
      submitData.packageSpec = (submitData.packageSpec || '') + submitData.packageSpecUnit
    }
    delete submitData.packageSpecUnit
    if (editId.value) await updateGoods(editId.value, submitData)
    else await createGoods(submitData)
    ElMessage.success(editId.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) { await deleteGoods(id); ElMessage.success('删除成功'); loadData() }
</script>
