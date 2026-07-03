<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-input v-model="search.name" placeholder="基地名称" clearable style="width:140px" />
            <el-select v-model="search.enterpriseId" placeholder="所属企业" clearable filterable style="width:160px">
              <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
            </el-select>
            <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>搜索</el-button>
          </div>
          <el-button type="primary" @click="openForm()"><el-icon><Plus /></el-icon>新增基地</el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="基地名称" min-width="140" />
        <el-table-column prop="code" label="基地编号" width="100">
          <template #default="{ row }"><span class="code-text">{{ row.code || '-' }}</span></template>
        </el-table-column>
        <el-table-column prop="enterpriseName" label="所属企业" width="140" />
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column label="面积" width="100"><template #default="{ row }">{{ row.area }}{{ row.unit }}</template></el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,19) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openForm(row)">编辑</el-button>
            <el-popconfirm title="确认删除该基地?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑基地' : '新增基地'" width="620px">
      <el-form ref="formRef" :model="form" :rules="{ name: [{ required: true, message: '请输入基地名称', trigger: 'blur' }], enterpriseId: [{ required: true, message: '请选择所属企业', trigger: 'change' }] }" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="所属企业" prop="enterpriseId">
            <el-select v-model="form.enterpriseId" filterable style="width:100%"><el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" /></el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="基地名称" prop="name"><el-input v-model="form.name" placeholder="如：有机稻基地" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="基地编号"><el-input v-model="form.code" placeholder="如：BASE001" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="负责人"><el-input v-model="form.manager" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="面积"><el-input-number v-model="form.area" :min="0" style="width:100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="面积单位"><el-select v-model="form.unit" style="width:100%"><el-option label="亩" value="亩" /><el-option label="公顷" value="公顷" /></el-select></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="省"><el-input v-model="form.province" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="市"><el-input v-model="form.city" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="区"><el-input v-model="form.district" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="认证情况"><el-input v-model="form.certification" /></el-form-item></el-col>
        </el-row>
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
import { getBases, createBase, updateBase, deleteBase, getAllEnterprises } from '@/api/admin'

const loading = ref(false)
const submitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const dialogVisible = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()
const search = reactive({ name: '', enterpriseId: null as number | null })
const enterprises = ref<any[]>([])

const form = reactive<any>({
  enterpriseId: null, name: '', code: '', manager: '', phone: '',
  area: 0, unit: '亩', province: '', city: '', district: '', certification: '',
})

onMounted(async () => {
  const res = await getAllEnterprises()
  enterprises.value = res.data || []
  loadData()
})

async function loadData() {
  loading.value = true
  try {
    const res = await getBases({ page: page.value, size: size.value, ...search })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function openForm(row?: any) {
  editId.value = row?.id || null
  Object.keys(form).forEach(k => { form[k] = row?.[k] ?? (k === 'unit' ? '亩' : (k === 'area' ? 0 : null)) })
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) await updateBase(editId.value, form)
    else await createBase(form)
    ElMessage.success(editId.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await deleteBase(id)
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

.code-text {
  font-family: 'SF Mono', 'Monaco', monospace;
  font-size: 12px;
  color: #6b7280;
  background: #f3f4f6;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>
