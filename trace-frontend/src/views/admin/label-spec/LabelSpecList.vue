<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-input v-model="keyword" placeholder="搜索标签规格" style="width:240px" clearable @clear="loadData" @keyup.enter="loadData">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </div>
          <el-button type="primary" @click="openForm()">
            <el-icon><Plus /></el-icon>新增标签规格
          </el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="specName" label="规格名称" width="140" />
        <el-table-column prop="material" label="材质" width="100" />
        <el-table-column prop="price" label="单价(元)" width="100">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.price?.toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="usageMethod" label="使用方式" width="100">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.usageMethod || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="手动指定流水号" width="120">
          <template #default="{ row }">
            <el-tag :type="row.supportManualAssign ? 'success' : 'info'" size="small">{{ row.supportManualAssign ? '支持' : '不支持' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属证书" width="120">
          <template #default="{ row }">
            <el-tag v-if="row.certTypeId" size="small" type="warning">{{ certTypes.find((c: any) => c.id === row.certTypeId)?.name || '-' }}</el-tag>
            <el-tag v-else size="small" type="info">通用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isVoid ? 'danger' : 'success'" size="small">{{ row.isVoid ? '作废' : '有效' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openForm(row)">编辑</el-button>
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

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑标签规格' : '新增标签规格'" width="520px">
      <el-form ref="formRef" :model="form" :rules="{ specName: [{ required: true, message: '请输入规格名称', trigger: 'blur' }] }" label-width="110px">
        <el-form-item label="规格名称" prop="name"><el-input v-model="form.specName" placeholder="如：食品标签A4" /></el-form-item>
        <el-form-item label="材质"><el-input v-model="form.material" placeholder="如：铜版纸" /></el-form-item>
        <el-form-item label="单价(元)"><el-input-number v-model="form.price" :min="0" :precision="2" style="width:100%" /></el-form-item>
        <el-form-item label="使用方式">
          <el-select v-model="form.usageMethod" style="width:100%">
            <el-option label="贴标" value="贴标" />
            <el-option label="挂牌" value="挂牌" />
            <el-option label="喷码" value="喷码" />
          </el-select>
        </el-form-item>
        <el-form-item label="支持手动流水号"><el-switch v-model="form.supportManualAssign" :active-value="1" :inactive-value="0" /></el-form-item>
        <el-form-item label="所属证书类型">
          <el-select v-model="form.certTypeId" placeholder="不选则为通用" clearable style="width:100%">
            <el-option v-for="c in certTypes" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否作废"><el-switch v-model="form.isVoid" :active-value="1" :inactive-value="0" /></el-form-item>
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
import { getLabelSpecs, createLabelSpec, updateLabelSpec, deleteLabelSpec } from '@/api/admin'
import { getCertTypeOptions } from '@/api/common'

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
const certTypes = ref<any[]>([])
const defaultForm: Record<string, any> = { specName: '', material: '', price: 0, usageMethod: '贴标', supportManualAssign: 0, isVoid: 0, certTypeId: null }
const form = reactive<Record<string, any>>({ ...defaultForm })

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

onMounted(async () => {
  loadData()
  try { const res = await getCertTypeOptions(); certTypes.value = res.data || [] } catch {}
})

async function loadData() {
  loading.value = true
  try {
    const res = await getLabelSpecs({ page: page.value, size: size.value, keyword: keyword.value })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function openForm(row?: any) {
  editId.value = row?.id || null
  if (row) {
    Object.keys(form).forEach(k => { form[k] = row[k] ?? defaultForm[k] })
  } else {
    Object.assign(form, defaultForm)
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (editId.value) await updateLabelSpec(editId.value, form)
    else await createLabelSpec(form)
    ElMessage.success(editId.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function handleDelete(id: number) {
  await deleteLabelSpec(id)
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

.price-text {
  color: #d97706;
  font-weight: 600;
}
</style>
