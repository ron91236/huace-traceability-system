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
        <el-table-column label="样品图" width="80" align="center">
          <template #default="{ row }">
            <el-image v-if="row.sampleImage" :src="row.sampleImage" fit="cover" style="width:48px;height:48px;border-radius:4px" :preview-src-list="[row.sampleImage]" />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="宣传图" width="80" align="center">
          <template #default="{ row }">
            <el-image v-if="row.promoImage" :src="row.promoImage" fit="cover" style="width:48px;height:48px;border-radius:4px" :preview-src-list="[row.promoImage]" />
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑商品' : '新增商品'" width="600px">
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
        <el-form-item label="包装规格"><el-input v-model="form.packageSpec" /></el-form-item>
        <el-form-item label="重量规格"><el-input v-model="form.weightSpec" /></el-form-item>
        <el-form-item label="储存方式"><el-input v-model="form.storageMethod" /></el-form-item>
        <el-form-item label="食用方式"><el-input v-model="form.eatingMethod" /></el-form-item>
        <el-form-item label="商品介绍"><el-input v-model="form.introduction" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="样品图">
          <el-upload :show-file-list="false" :http-request="(opt: any) => handleImageUpload(opt, 'sampleImage')" accept="image/*">
            <el-image v-if="form.sampleImage" :src="form.sampleImage" fit="cover" style="width:100px;height:100px;border-radius:8px" />
            <el-icon v-else :size="28" color="#8c939d"><Plus /></el-icon>
          </el-upload>
          <el-button v-if="form.sampleImage" link type="danger" style="margin-left:8px" @click="form.sampleImage = ''">移除</el-button>
        </el-form-item>
        <el-form-item label="宣传图">
          <el-upload :show-file-list="false" :http-request="(opt: any) => handleImageUpload(opt, 'promoImage')" accept="image/*">
            <el-image v-if="form.promoImage" :src="form.promoImage" fit="cover" style="width:100px;height:100px;border-radius:8px" />
            <el-icon v-else :size="28" color="#8c939d"><Plus /></el-icon>
          </el-upload>
          <el-button v-if="form.promoImage" link type="danger" style="margin-left:8px" @click="form.promoImage = ''">移除</el-button>
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
import { getGoods, createGoods, updateGoods, deleteGoods } from '@/api/enterprise'
import { getProductOptions, uploadFile, getTraceTemplateOptions } from '@/api/common'

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

const form = reactive<any>({ productId: null, name: '', packageSpec: '', weightSpec: '', storageMethod: '', eatingMethod: '', introduction: '', sampleImage: '', promoImage: '', traceTemplateId: null })

onMounted(async () => { const [prodRes, tplRes] = await Promise.all([getProductOptions(), getTraceTemplateOptions()]); products.value = prodRes.data || []; traceTemplates.value = tplRes.data || []; loadData() })

async function loadData() {
  loading.value = true
  try { const res = await getGoods({ page: page.value, size: size.value, keyword: keyword.value }); list.value = res.data?.list || []; total.value = res.data?.total || 0 } finally { loading.value = false }
}

function openForm(row?: any) { editId.value = row?.id || null; Object.keys(form).forEach(k => { form[k] = row?.[k] ?? null }); dialogVisible.value = true }

async function handleImageUpload(options: any, field: string) {
  try {
    const res = await uploadFile(options.file)
    const url = res.data?.url || res.data || ''
    form[field] = url
    ElMessage.success('图片上传成功')
  } catch (e) {
    ElMessage.error('图片上传失败')
  }
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try { if (editId.value) await updateGoods(editId.value, form); else await createGoods(form); ElMessage.success(editId.value ? '编辑成功' : '新增成功'); dialogVisible.value = false; loadData() } finally { submitting.value = false }
}

async function handleDelete(id: number) { await deleteGoods(id); ElMessage.success('删除成功'); loadData() }
</script>
