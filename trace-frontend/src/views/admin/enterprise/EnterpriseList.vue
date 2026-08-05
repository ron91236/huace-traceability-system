<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-input v-model="search.name" placeholder="企业名称" clearable style="width:160px" @keyup.enter="loadData" />
            <el-input v-model="search.contact" placeholder="联系人" clearable style="width:120px" @keyup.enter="loadData" />
            <el-button type="primary" @click="loadData">
              <el-icon><Search /></el-icon>搜索
            </el-button>
          </div>
          <div style="display:flex;gap:8px">
            <el-button type="success" @click="openMasterForm">
              <el-icon><Plus /></el-icon>创建母账号
            </el-button>
            <el-button type="primary" @click="openForm()">
              <el-icon><Plus /></el-icon>新增企业
            </el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="name" label="企业名称" min-width="160" />
        <el-table-column prop="nature" label="企业性质" width="100" />
        <el-table-column prop="accountLevel" label="账号类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.accountLevel === 'master'" type="warning" size="small">母账号</el-tag>
            <el-tag v-else-if="row.accountLevel === 'child'" type="info" size="small">子账号</el-tag>
            <el-tag v-else size="small">独立</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="loginAccount" label="登录账号" width="120" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openForm(row)">编辑</el-button>
            <el-button v-if="row.accountLevel === 'master'" size="small" type="warning" link @click="openGroupDialog(row)">管理子企业</el-button>
            <el-popconfirm title="确认删除该企业?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editId ? '编辑企业' : '新增企业'" width="680px" top="5vh" :close-on-click-modal="false">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="企业名称" prop="name"><el-input v-model="form.name" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="企业性质"><el-input v-model="form.nature" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系人" prop="contact"><el-input v-model="form.contact" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话" prop="phone"><el-input v-model="form.phone" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="所属行业"><el-input v-model="form.industry" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="登录账号" prop="loginAccount"><el-input v-model="form.loginAccount" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="登录密码" :prop="editId ? '' : 'loginPassword'"><el-input v-model="form.loginPassword" type="password" show-password :placeholder="editId ? '留空不修改' : '请输入'" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="统一社会信用代码"><el-input v-model="form.creditCode" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="主营类型"><el-input v-model="form.mainType" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="省"><el-input v-model="form.province" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="市"><el-input v-model="form.city" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="区"><el-input v-model="form.district" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="邮编"><el-input v-model="form.zipcode" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="详细地址"><el-input v-model="form.address" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="企业介绍"><el-input v-model="form.introduction" type="textarea" :rows="3" /></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="溯源模板">
              <el-select v-model="form.assignedTemplateIdsList" multiple placeholder="请选择分配的溯源模板" style="width:100%" filterable>
                <el-option v-for="t in traceTemplates" :key="t.id" :label="t.templateName" :value="t.id" />
              </el-select>
              <div style="font-size:12px;color:#909399;margin-top:4px">未选择时企业可使用所有模板</div>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 创建母账号弹窗 -->
    <el-dialog v-model="masterDialogVisible" title="创建母账号" width="520px" :close-on-click-modal="false">
      <el-form ref="masterFormRef" :model="masterForm" :rules="masterRules" label-width="100px">
        <el-form-item label="企业名称" prop="name"><el-input v-model="masterForm.name" /></el-form-item>
        <el-form-item label="联系人" prop="contact"><el-input v-model="masterForm.contact" /></el-form-item>
        <el-form-item label="联系电话" prop="phone"><el-input v-model="masterForm.phone" /></el-form-item>
        <el-form-item label="登录账号" prop="loginAccount"><el-input v-model="masterForm.loginAccount" /></el-form-item>
        <el-form-item label="登录密码" prop="loginPassword"><el-input v-model="masterForm.loginPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="masterDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="masterSubmitting" @click="handleCreateMaster">确定</el-button>
      </template>
    </el-dialog>

    <!-- 子企业管理弹窗 -->
    <EnterpriseGroupDialog v-model="groupDialogVisible" :parent-enterprise="currentParent" @saved="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'
import { getEnterprises, createEnterprise, updateEnterprise, deleteEnterprise, createMasterEnterprise } from '@/api/admin'
import { getTraceTemplateOptions } from '@/api/common'
import EnterpriseGroupDialog from './EnterpriseGroupDialog.vue'

const loading = ref(false)
const submitting = ref(false)
const masterSubmitting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const dialogVisible = ref(false)
const masterDialogVisible = ref(false)
const groupDialogVisible = ref(false)
const editId = ref<number | null>(null)
const formRef = ref()
const masterFormRef = ref()
const search = reactive({ name: '', contact: '' })
const currentParent = ref<any>(null)

const form = reactive<any>({
  name: '', nature: '', contact: '', phone: '', email: '', industry: '',
  loginAccount: '', loginPassword: '', creditCode: '', mainType: '',
  province: '', city: '', district: '', zipcode: '', address: '', introduction: '',
  assignedTemplateIds: '',
  assignedTemplateIdsList: [],
})

const traceTemplates = ref<any[]>([])

const masterForm = reactive<any>({
  name: '', contact: '', phone: '', loginAccount: '', loginPassword: '',
})

const rules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  loginAccount: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  loginPassword: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

const masterRules = {
  name: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  loginAccount: [{ required: true, message: '请输入登录账号', trigger: 'blur' }],
  loginPassword: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

onMounted(async () => {
  loadData()
  try { const res = await getTraceTemplateOptions(); traceTemplates.value = res.data || [] } catch {}
})

async function loadData() {
  loading.value = true
  try {
    const res = await getEnterprises({ page: page.value, size: size.value, ...search })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

function openForm(row?: any) {
  editId.value = row?.id || null
  Object.keys(form).forEach(k => {
    if (k === 'assignedTemplateIdsList') {
      form[k] = row?.assignedTemplateIds ? row.assignedTemplateIds.split(',').map(Number) : []
    } else if (k === 'assignedTemplateIds') {
      form[k] = row?.assignedTemplateIds || ''
    } else {
      form[k] = row?.[k] || ''
    }
  })
  dialogVisible.value = true
}

function openMasterForm() {
  Object.keys(masterForm).forEach(k => { masterForm[k] = '' })
  masterDialogVisible.value = true
}

function openGroupDialog(row: any) {
  currentParent.value = row
  groupDialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const submitData = { ...form }
    // Convert selected template IDs array to comma-separated string
    submitData.assignedTemplateIds = (form.assignedTemplateIdsList || []).join(',')
    delete submitData.assignedTemplateIdsList
    if (editId.value) await updateEnterprise(editId.value, submitData)
    else await createEnterprise(submitData)
    ElMessage.success(editId.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function handleCreateMaster() {
  const valid = await masterFormRef.value?.validate().catch(() => false)
  if (!valid) return
  masterSubmitting.value = true
  try {
    await createMasterEnterprise(masterForm)
    ElMessage.success('母账号创建成功')
    masterDialogVisible.value = false
    loadData()
  } finally { masterSubmitting.value = false }
}

async function handleDelete(id: number) {
  await deleteEnterprise(id)
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
</style>
