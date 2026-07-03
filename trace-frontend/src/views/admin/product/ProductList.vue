<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-input v-model="keyword" placeholder="搜索产品名称" style="width:240px" clearable @clear="loadData" @keyup.enter="loadData">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </div>
          <el-button type="primary" @click="openForm()">
            <el-icon><Plus /></el-icon>新增产品
          </el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="产品名称" min-width="160" />
        <el-table-column prop="category" label="产品分类" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.category" size="small" type="success">{{ row.category }}</el-tag>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="产品编码" width="120">
          <template #default="{ row }">
            <span class="code-text">{{ row.code || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除该产品?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑产品' : '新增产品'" width="520px">
      <el-form ref="formRef" :model="form" :rules="{ name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }] }" label-width="90px">
        <el-form-item label="产品名称" prop="name"><el-input v-model="form.name" placeholder="如：有机大米" /></el-form-item>
        <el-form-item label="产品分类">
          <el-select v-model="form.category" placeholder="选择分类" clearable style="width:100%">
            <el-option label="谷物类" value="谷物类" />
            <el-option label="蔬菜类" value="蔬菜类" />
            <el-option label="水果类" value="水果类" />
            <el-option label="肉禽类" value="肉禽类" />
            <el-option label="水产类" value="水产类" />
            <el-option label="乳品类" value="乳品类" />
            <el-option label="茶叶类" value="茶叶类" />
            <el-option label="其他" value="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="产品编码"><el-input v-model="form.code" placeholder="如：P001" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="产品描述信息" /></el-form-item>
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
import { Search, Plus } from '@element-plus/icons-vue'
import { getProducts, createProduct, updateProduct, deleteProduct } from '@/api/admin'

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
const form = reactive({ name: '', description: '', category: '', code: '' })

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getProducts({ page: page.value, size: size.value, keyword: keyword.value })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function openForm(row?: any) {
  editId.value = row?.id || null
  form.name = row?.name || ''
  form.description = row?.description || ''
  form.category = row?.category || ''
  form.code = row?.code || ''
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) await updateProduct(editId.value, form)
    else await createProduct(form)
    ElMessage.success(editId.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await deleteProduct(id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<style scoped lang="scss">
.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
}

.text-muted {
  color: #c0c4cc;
}

.code-text {
  font-family: 'SF Mono', 'Monaco', 'Menlo', monospace;
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>
